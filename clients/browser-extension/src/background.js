// Chrome/Edge use a service worker (no manifest scripts array), so config.js must be imported here.
// Firefox loads config.js via the manifest "scripts" array before this file runs, so importScripts
// is not available and not needed there.
if (typeof importScripts === 'function') {
    importScripts('config.js');
}

var clientVersion = "4.0.3";

// PIN length migration: existing installs without pinLength get pinLength=4
function migratePinLength() {
    chrome.storage.local.get(['pinRegistered', 'pinLength'], function(result) {
        if (result.pinRegistered && !result.pinLength) {
            chrome.storage.local.set({ pinLength: 4 });
        }
    });
}

chrome.runtime.onInstalled.addListener(function() {
    migratePinLength();
});

chrome.runtime.onStartup.addListener(function() {
    migratePinLength();
    
    storageGet(["apiKey", "deviceId", "pinRegistered", "mitIdRegistered"]).then(function(result) {
        fetchStatusFromBackend(result);
    });
});

// Real-time managed policy listener.
// Handles SSO token pushes: when the IdP writes a token+timestamp via policy,
// the extension fetches the session URL to log the user in automatically.
chrome.storage.managed.onChanged.addListener(function(changes) {
    if (!changes || !changes.timestamp || !changes.token) {
        return;
    }
    if (!changes.timestamp.newValue || !changes.token.newValue) {
        return;
    }

    // Reject messages older than 5 minutes
    var oldestAllowed = new Date();
    oldestAllowed.setMinutes(oldestAllowed.getMinutes() - 5);
    if (new Date(changes.timestamp.newValue) < oldestAllowed) {
        return;
    }

    fetch(changes.token.newValue, { method: 'GET', cache: 'default' });
});

// INTERNAL MESSAGES
chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {

    if (request.event == 'validatePin') {
        validatePin(sendResponse, request.pin);
        return true;
    }

    if (request.event == 'acceptChallenge') {
        acceptChallenge(sendResponse, request.pin, request.uuid, request.passwordlessChallenge);
        return true;
    }

    if (request.event == 'challenge') {
        checkForChallenges(sendResponse);
        return true;
    }

    if (request.event == 'rejectChallenge') {
        rejectChallenge(sendResponse, request.uuid);
        return true;
    }

    if (request.event == 'manualChallengeTrigger') {
        chrome.tabs.query({ active: true, currentWindow: true }, function(tabs) {
            if (tabs && tabs.length > 0) {
                chrome.tabs.sendMessage(tabs[0].id, { event: 'manualChallenge' }, function() {
                    void chrome.runtime.lastError; // suppress "no receiver" error on non-.dk pages
                });
            }
        });
        sendResponse();
        return true;
    }

    if (request.runMitIdRegistrationMonitorTask) {
        runMitIdRegistrationMonitorTask(request.runMitIdRegistrationMonitorTask);
        sendResponse();
    }

    if (request.runPinRegistrationMonitorTask) {
        chrome.storage.local.set({ isPaused: true });
        runPinRegistrationMonitorTask(request.runPinRegistrationMonitorTask);
        sendResponse();
    }

    if (request.runSelfServiceMonitorTask) {
        chrome.storage.local.set({ isPaused: true });
        runSelfServiceMonitorTask(request.runSelfServiceMonitorTask);
        sendResponse();
    }

    return true;
});


// ALARM HANDLER
chrome.alarms.onAlarm.addListener(function(alarm) {
    if (alarm.name === "runMitIdRegistrationMonitorTask") {
        monitorMitIdUrl();
    }
    if (alarm.name === "runPinRegistrationMonitorTask") {
        monitorPinUrl();
    }
    if (alarm.name === "runSelfServiceMonitorTask") {
        monitorSelfServiceUrl();
    }
});

// Window removal listener — cleans up tracking state when any managed window closes
chrome.windows.onRemoved.addListener(function(windowId) {
    chrome.storage.local.get(["mitIdWindowId", "pinWindowId", "selfServiceWindowId"], function(result) {
        if (result.mitIdWindowId != null && windowId == result.mitIdWindowId) {
            chrome.alarms.clear("runMitIdRegistrationMonitorTask");
            chrome.storage.local.set({ mitIdWindowId: null, isPaused: false });
        }
        if (result.pinWindowId != null && windowId == result.pinWindowId) {
            chrome.alarms.clear("runPinRegistrationMonitorTask");
            chrome.storage.local.set({ pinWindowId: null, isPaused: false });
        }
        if (result.selfServiceWindowId != null && windowId == result.selfServiceWindowId) {
            chrome.alarms.clear("runSelfServiceMonitorTask");
            chrome.storage.local.set({ selfServiceWindowId: null, isPaused: false });
        }
    });
});

async function fetchStatusFromBackend(dbVariables) {
    var apiKey = dbVariables["apiKey"];
    var deviceId = dbVariables["deviceId"];
    var dbPinRegistered = dbVariables["pinRegistered"];
    var dbMitIdRegistered = dbVariables["mitIdRegistered"];

    if (!apiKey || !deviceId) {
        console.log('2-faktor enhed ikke registreret endnu');
        return;
    }

    try {
        var response = await makeAjaxRequest({
            headers: { 'ApiKey': apiKey, 'deviceId': deviceId },
            url: BACKEND_URL + "/api/client/v2/status"
        });
            
        handleBackendStatus({
            exists: !response.data.disabled,
            pinProtected: response.data.pinProtected,
            mitIdRegistered: response.data.nemIdRegistered,
            lookupFailed: false
        }, dbPinRegistered, dbMitIdRegistered);
    } catch (error) {
        if (error.status === 401) return;
        handleBackendStatus({ exists: false, lookupFailed: true }, dbPinRegistered, dbMitIdRegistered);
    }
}

function handleBackendStatus(result, dbPinRegistered, dbMitIdRegistered) {
    if (result.exists) {
        if (result.pinProtected && !dbPinRegistered) {
            storageSet({ pinRegistered: true });
        }

        if (result.mitIdRegistered && !dbMitIdRegistered) {
            storageSet({ mitIdRegistered: true });
        }
    }
    else if (!result.lookupFailed) {
        console.log('2-faktor enheden er slettet centralt, nulstiller enheden');
        storageRemove(["apiKey", "deviceId", "mitIdRegistered", "pinRegistered", "pinLength"]);
    }
    else {
        console.error('Teknisk fejl ved status-opslag');
    }
}

// MITID WINDOW MONITORING
function monitorMitIdUrl() {
    chrome.storage.local.get(["mitIdWindowId"], function(result) {
        var mitIdWindowId = result.mitIdWindowId;

        if (mitIdWindowId == null) {
            stopMitIdRegistrationMonitorTask();
            return;
        }

        try {
            chrome.windows.get(mitIdWindowId, { populate: true }, function(win) {
                if (!win) {
                    stopMitIdRegistrationMonitorTask();
                    return;
                }
                if (win.tabs[0].url) {
                    var url = new URL(win.tabs[0].url);
                    if (url.searchParams.get("status") === "true") {
                        storageSet({ mitIdRegistered: true });
                    }
                    if (url.searchParams.get("closeWindow")) {
                        chrome.windows.remove(win.id);
                        stopMitIdRegistrationMonitorTask();
                    }
                }
            });
        } catch (error) {
            stopMitIdRegistrationMonitorTask();
        }
    });
}

function runMitIdRegistrationMonitorTask(windowId) {
    chrome.storage.local.set({ mitIdWindowId: windowId });
    chrome.alarms.create("runMitIdRegistrationMonitorTask", { periodInMinutes: 0.004 });
}

function stopMitIdRegistrationMonitorTask() {
    chrome.alarms.clear("runMitIdRegistrationMonitorTask");
    chrome.storage.local.set({ mitIdWindowId: null, isPaused: false });
}

// PIN REGISTRATION WINDOW MONITORING
function monitorPinUrl() {
    chrome.storage.local.get(["pinWindowId"], function(result) {
        var pinWindowId = result.pinWindowId;

        if (pinWindowId == null) {
            stopPinRegistrationMonitorTask();
            return;
        }

        try {
            chrome.windows.get(pinWindowId, { populate: true }, function(win) {
                if (!win) {
                    stopPinRegistrationMonitorTask();
                    return;
                }
                if (win.tabs[0].url) {
                    var url = new URL(win.tabs[0].url);
                    if (url.searchParams.get("status") === "true") {
                        storageSet({ pinRegistered: true });
                    }
                    if (url.searchParams.get("closeWindow")) {
                        chrome.windows.remove(win.id);
                        stopPinRegistrationMonitorTask();
                    }
                }
            });
        } catch (error) {
            stopPinRegistrationMonitorTask();
        }
    });
}

function runPinRegistrationMonitorTask(windowId) {
    chrome.storage.local.set({ pinWindowId: windowId });
    chrome.alarms.create("runPinRegistrationMonitorTask", { periodInMinutes: 0.004 });
}

function stopPinRegistrationMonitorTask() {
    chrome.alarms.clear("runPinRegistrationMonitorTask");
    chrome.storage.local.set({ pinWindowId: null, isPaused: false });
}

// SELF-SERVICE WINDOW MONITORING
function monitorSelfServiceUrl() {
    chrome.storage.local.get(["selfServiceWindowId"], function(result) {
        var selfServiceWindowId = result.selfServiceWindowId;

        if (selfServiceWindowId == null) {
            stopSelfServiceMonitorTask();
            return;
        }

        try {
            chrome.windows.get(selfServiceWindowId, { populate: true }, function(win) {
                if (!win) {
                    stopSelfServiceMonitorTask();
                    return;
                }
                if (win.tabs[0].url) {
                    var url = new URL(win.tabs[0].url);
                    if (url.searchParams.get("closeWindow")) {
                        chrome.windows.remove(win.id);
                        stopSelfServiceMonitorTask();
                    }
                }
            });
        } catch (error) {
            stopSelfServiceMonitorTask();
        }
    });
}

function runSelfServiceMonitorTask(windowId) {
    chrome.storage.local.set({ selfServiceWindowId: windowId });
    chrome.alarms.create("runSelfServiceMonitorTask", { periodInMinutes: 0.004 });
}

function stopSelfServiceMonitorTask() {
    chrome.alarms.clear("runSelfServiceMonitorTask");
    chrome.storage.local.set({ selfServiceWindowId: null, isPaused: false });
}

// CONTENT SCRIPT SUPPORT — used by content.js inline modal flow

async function checkForChallenges(callback) {
    try {
        var result = await storageGet(["apiKey", "deviceId"]);
        var apiKey = result.apiKey;
        var deviceId = result.deviceId;

        if (apiKey && deviceId) {
            var response = await makeAjaxRequest({
                headers: { 'ApiKey': apiKey, 'deviceId': deviceId },
                url: BACKEND_URL + "/api/client"
            });
            if (response.data !== "") {
                callback(response.data);
                return;
            }
        }
    } catch (error) {
        console.error('checkForChallenges error:', error);
    }
    callback(null);
}

async function validatePin(callback, pin) {
    var result = { status: 'WRONG_PIN', lockedUntil: null };
    try {
        var stored = await storageGet(["apiKey", "deviceId"]);
        var apiKey = stored.apiKey;
        var deviceId = stored.deviceId;

        if (apiKey && deviceId) {
            var response = await makeAjaxRequest({
                headers: {
                    'ApiKey': apiKey,
                    'deviceId': deviceId,
                    'clientVersion': clientVersion,
                    'Content-Type': 'application/json'
                },
                url: BACKEND_URL + "/api/client/v2/validatePin",
                type: 'POST',
                data: JSON.stringify({ pincode: pin })
            });
            result.status = response.data.status;
            result.lockedUntil = response.data.lockedUntil || null;
        }
    } catch (error) {
        console.error("validatePin error:", error);
    }
    callback(result);
}

async function acceptChallenge(callback, pin, uuid, passwordlessChallenge) {
    var wrongPin = false;
    var locked = false;
    var lockedUntil = null;
    try {
        var result = await storageGet(["apiKey", "deviceId"]);
        var apiKey = result.apiKey;
        var deviceId = result.deviceId;

        if (apiKey && deviceId) {
            await makeAjaxRequest({
                headers: {
                    'ApiKey': apiKey,
                    'deviceId': deviceId,
                    'clientVersion': clientVersion,
                    'pinCode': pin,
                    'passwordlessChallenge': passwordlessChallenge
                },
                url: BACKEND_URL + "/api/client/" + uuid + "/accept",
                type: 'PUT'
            });
        }
    } catch (error) {
        if (error.responseJSON && error.responseJSON.status === "WRONG_PIN") {
            wrongPin = true;
        } else if (error.responseJSON && error.responseJSON.status === "LOCKED") {
            locked = true;
            lockedUntil = error.responseJSON.lockedUntil || null;
        }
    }
    callback({ wrongPin: wrongPin, locked: locked, lockedUntil: lockedUntil });
}

async function rejectChallenge(callback, uuid) {
    try {
        var result = await storageGet(["apiKey", "deviceId"]);
        var apiKey = result.apiKey;
        var deviceId = result.deviceId;

        if (apiKey && deviceId) {
            await makeAjaxRequest({
                headers: {
                    'ApiKey': apiKey,
                    'deviceId': deviceId,
                    'clientVersion': clientVersion
                },
                url: BACKEND_URL + "/api/client/" + uuid + "/reject",
                type: 'PUT'
            });
        }
    } catch (error) {
        console.error("rejectChallenge error:", error);
    }
    callback();
}

async function makeAjaxRequest(options) {
    try {
        var fetchOptions = {
            method: options.type || 'GET',
            headers: options.headers || {}
        };
        if (options.data) {
            fetchOptions.body = options.data;
        }

        var response = await fetch(options.url, fetchOptions);
        var responseText = await response.text();

        if (response.ok) {
            var responseData = responseText;
            try { responseData = JSON.parse(responseText); } catch (e) {}
            return { data: responseData, status: response.status };
        } else {
            var errorResponse = responseText;
            try { errorResponse = JSON.parse(responseText); } catch (e) {}
            throw { status: response.status, responseJSON: errorResponse };
        }
    } catch (error) {
        if (error.status) throw error;
        throw { status: 0, responseJSON: null, networkError: error };
    }
}

/// handling push notifications start
const VAPID_PUBLIC_KEY = 'BJgHwxgz45mYC9_gGqOF3RiCL97HVwt3tP9RqYz2btuv_r0Ev3bJ4A9PMzwpHVbsXnA715ZJmxhn5DDRDHoBnGI='; 

// trigger method - should be called somewhere
async function registerPushToken() {
    try {
        const registration = self.registration;

        let subscription = await registration.pushManager.getSubscription();
    
        if (!subscription) {
            subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY)
            });
        }

        await updatePushNotificationToken(JSON.stringify(subscription.toJSON(), null, 2));
    }
    catch (err) {
        console.log('Subscription failed:', err);
    }
}

async function updatePushNotificationToken(token) {
    const result = await storageGet(["apiKey", "deviceId", "pushToken"]);

    if (result.apiKey && result.deviceId) {
        if (result.pushToken && token === result.pushToken) {
            log.info("no token changes");
        }
        else {
            await storageSet({ pushToken: token });
            await updatePushTokenForReal(result.apiKey, result.deviceId, token);
        }
    }
}

async function updatePushTokenForReal(apiKey, deviceId, token) {
    if (token && apiKey && deviceId) {
        const response = await fetch(BACKEND_URL + "/api/client/v2/setRegId", {
            method: 'POST',
            headers: {
                'ApiKey': apiKey,
                'deviceId': deviceId, 
                'clientVersion': clientVersion,
                'content-type': 'application/json'
            },
            body: JSON.stringify({
              'token': token
            })
        });
        
        if (response.ok) {
            console.info('Updated push token on backend');
        }
        else {
            console.error('Failed to update push token:', response.status);
        }
    }
}

// trigger registration of token on backend
chrome.runtime.onStartup.addListener(registerPushToken);
chrome.runtime.onInstalled.addListener(registerPushToken);

self.addEventListener('push', (event) => {
    event.waitUntil((async () => {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tab) {
            try {
                await chrome.tabs.sendMessage(tab.id, { event: 'manualChallenge' });
            } catch (err) {
                // Content script not loaded in this tab (e.g. chrome:// page, or non-.dk site)
                console.error('Could not deliver challenge to active tab:', err);
            }
        }
    })());
});

function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = atob(base64);

    return Uint8Array.from([...rawData].map(c => c.charCodeAt(0)));
}

/// handling push notifications end

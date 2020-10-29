var monitorRegisterUrlTask;
var registrationWindowId;

var monitorNemIdUrlTask;
var nemIdWindowId;

var pinWindowId;
var monitorPinUrlTask;

var monitorSelfServiceUrlTask;
var selfServiceWindowId;

var notificationCount = 0;

// challenge window
var challengePopupId;

/** Utility functions */
function guid() {
	function s4() {
		return Math.floor((1 + Math.random()) * 0x10000)
			.toString(16)
			.substring(1);
	}

	return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
}

// listener for events from HTML - for direct AD FS integration
chrome.extension.onMessage.addListener(function(message, sender, sendResponse) {
	showChallengeWindow();

	return true;
});

// listener for events from GCM - for indirect integration
chrome.gcm.onMessage.addListener(function(message) {
	showChallengeWindow();
});

function showChallengeWindow() {
        var w = 600;
        var h = 400;
        var left = Math.round(screen.width/2) - Math.round(w/2);
        var top = Math.round(screen.height/2) - Math.round(h/2);

        if (challengePopupId == null){
                chrome.windows.create({
                        url: "challenge.html",
                        type: "popup",
                        state: "normal",
                        focused: true,
                        width: w,
                        height: h,
                        left: left,
                        top: top
                }, function (win) {
                        challengePopupId = win.id;
                });
        }
}

chrome.gcm.onSendError.addListener(function (err) {
	console.log(err);
})

function closePopup() {
  var windows = chrome.extension.getViews();

  for (var x = 0; x < windows.length; x++) {
    if (windows[x].location.pathname == "/popup.html") {
       windows[x].close();
    }
  }
}

function monitorRegisterUrl() {
	chrome.windows.get(registrationWindowId, { populate: true }, function (win) {
		var url = new URL(win.tabs[0].url);
		var apiKey = url.searchParams.get("apiKey");
		var deviceId = url.searchParams.get("deviceId");
		var closeWindow = url.searchParams.get("closeWindow");

		if (apiKey && deviceId) {
			chrome.storage.local.set({ apiKey: apiKey });
			chrome.storage.local.set({ deviceId: deviceId });
		}
		
		if (closeWindow) {
			chrome.windows.remove(win.id);
		}
	});
}

function runRegistrationMonitorTask(windowId){
	registrationWindowId = windowId;
	monitorRegisterUrlTask = setInterval(monitorRegisterUrl, 200);
}

function monitorNemIdUrl() {
	chrome.windows.get(nemIdWindowId, { populate: true }, function (win) {
		var url = new URL(win.tabs[0].url);
		var status = url.searchParams.get("status");
		var closeWindow = url.searchParams.get("closeWindow");

		if (status && status == "true") {
			chrome.storage.local.set({ nemIdRegistered: true });
		}
		
		if (closeWindow) {
			chrome.windows.remove(win.id);
		}
	});
}

function runNemIdRegistrationMonitorTask(windowId){
	nemIdWindowId = windowId;
	monitorNemIdUrlTask = setInterval(monitorNemIdUrl, 200);
}

function monitorPinUrl() {
	if (pinWindowId == null) {//TODO probably same logic required in other monitor tasks
		clearInterval(monitorPinUrlTask);
		chrome.storage.local.set({ isPaused: false });
		return;
	}
	chrome.windows.get(pinWindowId, { populate: true }, function (win) {
		var url = new URL(win.tabs[0].url);
		var status = url.searchParams.get("status");
		var closeWindow = url.searchParams.get("closeWindow");

		if (status && status == "true") {
			chrome.storage.local.set({ pinRegistered: true });
		}
		
		if (closeWindow) {
			chrome.windows.remove(win.id);
		}
	});
}

function runPinRegistrationMonitorTask(windowId){
	pinWindowId = windowId;
	monitorPinUrlTask = setInterval(monitorPinUrl, 200);
}

function monitorSelfServiceUrl() {
	chrome.windows.get(selfServiceWindowId, { populate: true }, function (win) {
		var url = new URL(win.tabs[0].url);
		var closeWindow = url.searchParams.get("closeWindow");

		if (closeWindow) {
			chrome.windows.remove(win.id);
		}
	});
}

function runSelfServiceMonitorTask(windowId){
	selfServiceWindowId = windowId;
	monitorSelfServiceUrlTask = setInterval(monitorSelfServiceUrl, 200);
}

chrome.windows.onRemoved.addListener(function (windowId) {
	if (registrationWindowId != null && windowId == registrationWindowId) {
		clearInterval(monitorRegisterUrlTask);
		registrationWindowId = null;
	}

	if (nemIdWindowId != null && windowId == nemIdWindowId) {
		clearInterval(monitorNemIdUrlTask);
		nemIdWindowId = null;
	}

	if (pinWindowId != null && windowId == pinWindowId) {
		clearInterval(monitorPinUrlTask);
		pinWindowId = null;
		chrome.storage.local.set({ isPaused: false });
	}

	if (selfServiceWindowId != null && windowId == selfServiceWindowId) {
		clearInterval(monitorSelfServiceUrlTask);
		selfServiceWindowId = null;
	}

	if (challengePopupId != null && windowId == challengePopupId) {
		challengePopupId = null;
	}
});

// can be removed some time in the futere, when all active badges have been removed
function removeBadge() {
	chrome.browserAction.setBadgeText({text: ""});
	chrome.browserAction.setBadgeBackgroundColor({color: "#F00"});
}

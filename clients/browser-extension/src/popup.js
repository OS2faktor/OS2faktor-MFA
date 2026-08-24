document.addEventListener('DOMContentLoaded', function() {
    var registerClientLink     = document.getElementById('register-client');
    var activateClientLink     = document.getElementById('activate-client');
    var pinRegisterLink        = document.getElementById('pin-register');
    var manageClientLink       = document.getElementById('manage-client');
    var fetchChallengesLink    = document.getElementById('fetch-challenges');
    var resetClientLink        = document.getElementById('reset-client');
    var deviceInfoItemValueRef = document.getElementById('deviceInfoItemValue');

    updateUI();

    registerClientLink.addEventListener('click', function(e) {
        e.preventDefault();
        createCenteredWindow('register.html', function() {
            window.close();
        });
    });

    activateClientLink.addEventListener('click', function(e) {
        e.preventDefault();
        storageGet(["apiKey", "deviceId"]).then(function(result) {
            if (result.apiKey && result.deviceId) {
                var params = new URLSearchParams({ deviceId: result.deviceId, apiKey: result.apiKey });
                createCenteredWindow(FRONTEND_URL + '/ui/register2/nemid?' + params.toString(), function(win) {
                    chrome.runtime.sendMessage({ runMitIdRegistrationMonitorTask: win.id }, function() {
                        window.close();
                    });
                });
            }
        });
    });

    pinRegisterLink.addEventListener('click', function(e) {
        e.preventDefault();
        storageGet(["apiKey", "deviceId"]).then(function(result) {
            if (result.apiKey && result.deviceId) {
                var params = new URLSearchParams({ deviceId: result.deviceId, apiKey: result.apiKey });
                createCenteredWindow(FRONTEND_URL + '/ui/pin/register?' + params.toString(), function(win) {
                    chrome.runtime.sendMessage({ runPinRegistrationMonitorTask: win.id });
                    window.close();
                });
            }
        });
    });

    manageClientLink.addEventListener('click', function(e) {
        e.preventDefault();
        storageGet(["apiKey", "deviceId"]).then(function(result) {
            if (result.apiKey && result.deviceId) {
                var params = new URLSearchParams({ deviceId: result.deviceId, apiKey: result.apiKey });
                createCenteredWindow(FRONTEND_URL + '/ui/selfservice?' + params.toString(), function(win) {
                    chrome.runtime.sendMessage({ runSelfServiceMonitorTask: win.id });
                    window.close();
                });
            }
        });
    });

    fetchChallengesLink.addEventListener('click', function(e) {
        e.preventDefault();
        chrome.runtime.sendMessage({ event: 'manualChallengeTrigger' });
        window.close();
    });

    resetClientLink.addEventListener('click', function(e) {
        e.preventDefault();
        createCenteredWindow('reset.html', function() {
            window.close();
        });
    });

    function createCenteredWindow(url, func) {
        chrome.windows.getCurrent(function(currentWindow) {
            var windowWidth = 600;
            var windowHeight = 400;
            var left = Math.round(currentWindow.left + (currentWindow.width - windowWidth) / 2);
            var top  = Math.round(currentWindow.top  + (currentWindow.height - windowHeight) / 2);

            chrome.windows.create({
                url: url,
                type: 'popup',
                width: windowWidth,
                height: windowHeight,
                left: left,
                top: top
            }, function(win) {
                if (func) func(win);
            });
        });
    }

    function updateUI() {
        storageGet(["deviceId", "pinRegistered", "mitIdRegistered", "installedTts"]).then(function(result) {
            var deviceId        = result["deviceId"];
            var pinRegistered   = result["pinRegistered"];
            var mitIdRegistered = result["mitIdRegistered"];
            var installedTts    = result["installedTts"];

            if (!installedTts) {
                var now = new Date();
                installedTts = "installeret " + now.getFullYear() + '/' + (now.getMonth() + 1) + '/' + now.getDate();
                storageSet({ installedTts: installedTts });
            }

            if (deviceId) {
                deviceInfoItemValueRef.textContent = deviceId;

                hide(registerClientLink);
                show(resetClientLink);
                show(fetchChallengesLink);

                if (mitIdRegistered) {
                    hide(activateClientLink);
                    show(manageClientLink);
                } else {
                    show(activateClientLink);
                    hide(manageClientLink);
                }

                if (pinRegistered) {
                    hide(pinRegisterLink);
                } else {
                    show(pinRegisterLink);
                }
            } else {
                deviceInfoItemValueRef.textContent = installedTts;

                show(registerClientLink);
                hide(activateClientLink);
                hide(pinRegisterLink);
                hide(manageClientLink);
                hide(fetchChallengesLink);
                hide(resetClientLink);
            }
        });
    }

    function hide(el) { el.style.display = 'none'; }
    function show(el) { el.style.display = 'block'; }
});

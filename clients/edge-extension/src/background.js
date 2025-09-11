var clientVersion = "2.5.1";
var applicationServerKey = "BJgHwxgz45mYC9_gGqOF3RiCL97HVwt3tP9RqYz2btuv_r0Ev3bJ4A9PMzwpHVbsXnA715ZJmxhn5DDRDHoBnGI=";

/* utility method for generating a Login session */
function obtainSession(sessionUrl) {
	fetch(sessionUrl, { method: 'GET', cache: "default" });
}

/** Utility functions */
function guid() {
	function s4() {
		return Math.floor((1 + Math.random()) * 0x10000)
			.toString(16)
			.substring(1);
	}

	return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
}

// INTERNAL MESSAGES
chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {

	//Pause challanges for dialog windows
	if (request.runPinRegistrationMonitorTask || request.runNemIdRegistrationMonitorTask || request.runSelfServiceMonitorTask) {
		chrome.storage.local.set({ isPaused: true });
	}

	if (request.runPinRegistrationMonitorTask) {
		console.log("Running runPinRegistrationMonitorTask task");
		runPinRegistrationMonitorTask(request.runPinRegistrationMonitorTask);
	}

	if (request.runNemIdRegistrationMonitorTask) {
		console.log("Running runNemIdRegistrationMonitorTask task");
		runNemIdRegistrationMonitorTask(request.runNemIdRegistrationMonitorTask);
		sendResponse(); // we need this to close the registration window
	}

	if (request.runSelfServiceMonitorTask) {
		console.log("Running runSelfServiceMonitorTask task");
		runSelfServiceMonitorTask(request.runSelfServiceMonitorTask);
	}

	//Comes from "shortcut" to open challenge window
	if (request.os2faktorEvent) {
		handleChallenge();
	}

	return true;
});

//EXTERNAL MESSAGES
chrome.runtime.onMessageExternal.addListener(function(request, sender, sendResponse) {
	if (request.nemIdRegistration) {
		chrome.storage.local.set({ nemIdRegistered: true });
	}
});

function handleChallenge() {
	chrome.storage.local.get(["apiKey", "deviceId"], function (result) {
		if (typeof result.apiKey === 'undefined' || typeof result.deviceId === 'undefined') {
			// do nothing
		} else {
			openChallengeWindow();
		}
	});
}

function openChallengeWindow() {
	var now = new Date().getTime();
	var in20sec = new Date().getTime() + (1000 * 20);

	try {
		chrome.system.display.getInfo(function(displayInfo) {
			var w = 600;
			var h = 400;
			var left = Math.round(displayInfo[0].bounds.width/2) - Math.round(w/2);
			var top = Math.round(displayInfo[0].bounds.height/2) - Math.round(h/2);

			chrome.storage.local.get(["challengePopupTts"], function (result) {
				// did we open a popup within the last 20 seconds?
				var allowPopup = true;
				if (result.challengePopupTts && result.challengePopupTts > now) {
					allowPopup = false;
				}

				//check if there are no other challenge windows
				if (allowPopup) {
					chrome.windows.create({
						url: "challenge.html",
						type: "popup",
						state: "normal",
						focused: true,
						width: w,
						height: h,
						top: top,
						left: left
					}, function (win) {
						chrome.storage.local.set({ challengePopupTts: in20sec });
					});
				}
				else {
					console.log("Supressing popup because one was shown within the last 20 seconds, and it is still open");
				}
			});

		});
	}
	catch (err) {
		console.log("failed to open through display.getInfo() - " + err);

		var w = 600;
		var h = 400;
		var left = 340; // 1280 x 720 assumption
		var top = 160;

		chrome.storage.local.get(["challengePopupTts"], function (result) {
			// did we open a popup within the last 20 seconds?
			var allowPopup = true;
			if (result.challengePopupTts && result.challengePopupTts > now) {
				allowPopup = false;
			}

			//check if there are no other challenge windows
			if (allowPopup) {
				chrome.windows.create({
					url: "challenge.html",
					type: "popup",
					state: "normal",
					focused: true,
					width: w,
					height: h,
					top: top,
					left: left
				}, function (win) {
					chrome.storage.local.set({ challengePopupTts: in20sec });
				});
			}
			else {
				console.log("Supressing popup because one was shown within the last 20 seconds, and it is still open");
			}
		});
	}
}

// Handle alarms
chrome.alarms.onAlarm.addListener(function( alarm ) {
	if (alarm.name == "runSelfServiceMonitorTask") {
		monitorSelfServiceUrl();
	}
	if (alarm.name == "runPinRegistrationMonitorTask") {
		monitorPinUrl();
	}
	if (alarm.name == "runNemIdRegistrationMonitorTask") {
		monitorNemIdUrl();
	}
});

function monitorNemIdUrl() {
	chrome.storage.local.get(["nemIdWindowId"], function (result) {

		nemIdWindowId = result["nemIdWindowId"];
		if (nemIdWindowId == null) {
			stopNemIdRegistrationMonitorTask();
			return;
		}
		try {
			chrome.windows.get(nemIdWindowId, { populate: true }, function (win) {
				//sometimes "win.tabs[0].url" is empty probably because the page has not loaded yet
				if (win.tabs[0].url) {
					var url = new URL(win.tabs[0].url);
					var status = url.searchParams.get("status");
					var closeWindow = url.searchParams.get("closeWindow");

					if (status && status == "true") {
						chrome.storage.local.set({ nemIdRegistered: true });
					}

					if (closeWindow) {
						chrome.windows.remove(win.id);
						stopNemIdRegistrationMonitorTask();
					}
				}
			});
		} catch (error) {
			stopNemIdRegistrationMonitorTask();
			return;
		}
	});
}

function runNemIdRegistrationMonitorTask(windowId){
	chrome.storage.local.set({ nemIdWindowId: windowId });
	chrome.alarms.create("runNemIdRegistrationMonitorTask", { periodInMinutes: 0.004 });
}

function stopNemIdRegistrationMonitorTask() {
	chrome.alarms.clear("runNemIdRegistrationMonitorTask");
	chrome.storage.local.set({ nemIdWindowId: null });
	chrome.storage.local.set({ isPaused: false });
}

function monitorPinUrl() {
	chrome.storage.local.get(["pinWindowId"], function (result) {
		var pinWindowId = result["pinWindowId"];
		if (pinWindowId == null) {
			stopPinRegistrationMonitorTask();
			return;
		}

		try {
			chrome.windows.get(pinWindowId, { populate: true }, function (win) {
				if (win.tabs[0].url) {
					var url = new URL(win.tabs[0].url);
					var status = url.searchParams.get("status");
					var closeWindow = url.searchParams.get("closeWindow");

					if (status && status == "true") {
						chrome.storage.local.set({ pinRegistered: true });
					}

					if (closeWindow) {
						chrome.windows.remove(win.id);
						stopPinRegistrationMonitorTask();
					}
				}
			});
		} catch (error) {
			stopPinRegistrationMonitorTask();
			return;
		}
	});
}

function runPinRegistrationMonitorTask(windowId) {
	chrome.storage.local.set({ pinWindowId: windowId });
	chrome.alarms.create("runPinRegistrationMonitorTask", { periodInMinutes: 0.004 });
}

function stopPinRegistrationMonitorTask() {
	chrome.alarms.clear("runPinRegistrationMonitorTask");
	chrome.storage.local.set({ pinWindowId: null });
	chrome.storage.local.set({ isPaused: false });
}

function monitorSelfServiceUrl() {
	chrome.storage.local.get(["selfServiceWindowId"], function (result) {
		selfServiceWindowId = result["selfServiceWindowId"];
		if (selfServiceWindowId == null) {
			stopSelfServiceMonitorTask();
			return;
		}
		try {
			chrome.windows.get(selfServiceWindowId, { populate: true }, function (win) {
				if (win.tabs[0].url) {
					var url = new URL(win.tabs[0].url);
					var closeWindow = url.searchParams.get("closeWindow");

					if (closeWindow) {
						chrome.windows.remove(win.id);
						stopSelfServiceMonitorTask();
					}
				}
			});
		} catch (error) {
			stopSelfServiceMonitorTask();
			return;
		}
	});
}

function runSelfServiceMonitorTask(windowId){
	chrome.storage.local.set({ selfServiceWindowId: windowId });
	chrome.alarms.create("runSelfServiceMonitorTask", { periodInMinutes: 0.004 });
}

function stopSelfServiceMonitorTask() {
	chrome.alarms.clear("runSelfServiceMonitorTask");
	chrome.storage.local.set({ selfServiceWindowId: null });
	chrome.storage.local.set({ isPaused: false });
}

chrome.windows.onRemoved.addListener(function (windowId) {
	chrome.storage.local.get(["nemIdWindowId", "pinWindowId", "selfServiceWindowId", "challengePopupId"], function (result) {
		nemIdWindowId = result["nemIdWindowId"];
		pinWindowId = result["pinWindowId"];
		selfServiceWindowId = result["selfServiceWindowId"];
		challengePopupId = result["challengePopupId"];

		if (nemIdWindowId != null && windowId == nemIdWindowId) {
			chrome.alarms.clear("runNemIdRegistrationMonitorTask");
			chrome.storage.local.set({ nemIdWindowId: null });
			chrome.storage.local.set({ isPaused: false });
		}

		if (pinWindowId != null && windowId == pinWindowId) {
			chrome.alarms.clear("runPinRegistrationMonitorTask");
			chrome.storage.local.set({ pinWindowId: null });
			chrome.storage.local.set({ isPaused: false });
		}

		if (selfServiceWindowId != null && windowId == selfServiceWindowId) {
			chrome.alarms.clear("runSelfServiceMonitorTask");
			chrome.storage.local.set({ selfServiceWindowId: null });
			chrome.storage.local.set({ isPaused: false });
		}

		if (challengePopupId != null) {
			chrome.storage.local.set({ challengePopupId: null });
		}
	});
});

//Startup listner to fetch current state from backend

chrome.runtime.onStartup.addListener(function () {
	var backendUrl = "https://backend.os2faktor.dk";

	chrome.storage.local.get(["apiKey", "deviceId", "pinRegistered", "nemIdRegistered"], function (result) {
		fetchStatusFromBackend(result, backendUrl);
	});
});

function fetchStatusFromBackend(dbVariables, backendUrl) {
	var apiKey = dbVariables["apiKey"];
	var deviceId = dbVariables["deviceId"];
	var dbPinRegistered = dbVariables["pinRegistered"];
	var dbNemIdRegistered = dbVariables["nemIdRegistered"];

	console.log("Getting status from backend");

	var statusResult = {
		'exists': false,
		'lookupFailed': false,
		'pinProtected': false,
		'nemIdRegistered': false
	};

	if (apiKey == null || deviceId == null) {
		console.log('2-faktor enhed ikke registreret endnu, status kan ikke hentes');
		return;
	}

	fetch(backendUrl + "/api/client/v2/status", {
		method: 'GET',
		headers: {
			'ApiKey': apiKey,
			'deviceId': deviceId,
		}
	}).then(response => {
		response.json().then(data => {
			if (response.status == 200) {
				statusResult.exists = !data.disabled;
				statusResult.pinProtected = data.pinProtected;
				statusResult.nemIdRegistered = data.nemIdRegistered;

				handleBackendStatus(statusResult, dbPinRegistered, dbNemIdRegistered);
			} else {
				if (response.status != 401) {
					statusResult.lookupFailed = true;
				}

				// TODO this is an expected error maybe we should change it to warning?
				console.error("getStatusError: " + JSON.stringify(response));
				handleBackendStatus(statusResult, dbPinRegistered, dbNemIdRegistered);
			}
		 });
	})
	.catch((errorMsg) => {
		console.error('fetchStatusFromBackend Error');
		console.error(errorMsg);
	});
}

function handleBackendStatus(result, dbPinRegistered, dbNemIdRegistered) {
	if (result.exists) {
		var changes = false;

		if (result.pinProtected && !dbPinRegistered) {
			changes = true;
			chrome.storage.local.set({ pinRegistered: true });
		}

		if (result.nemIdRegistered && !dbNemIdRegistered) {
			chrome.storage.local.set({ nemIdRegistered: true });
		}

		refreshPushToken();

		if (changes) {
			console.log('Centrale opdateringer tilgængelig til enheden - opdaterer');
		}
	}
	else if (!result.lookupFailed) {
		console.log('2-faktor enheden er slettet centralt, nulstiller enheden');
		chrome.storage.local.remove(["apiKey","deviceId","nemIdRegistered","pinRegistered"]);
	}
	else {
		console.error("teknisk fejl i opslag");
	}
}

function urlB64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');

  const rawData = atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

function refreshPushToken() {
	try {
		console.log("Refreshing push token");

		self.registration.pushManager.subscribe({
			userVisibleOnly: false,
			applicationServerKey: urlB64ToUint8Array(applicationServerKey)
		}).then((subscriptionData) => {
                        const json = JSON.stringify(subscriptionData.toJSON(), null, 2);

			updatePushNotificationToken(json);
		});
	} catch (error) {
		console.error('[Service Worker] Failed to subscribe, error: ', error);
	}
}

function updatePushNotificationToken(token) {
	chrome.storage.local.get(["apiKey", "deviceId"], function (result) {
		if (typeof result.apiKey === 'undefined' || typeof result.deviceId === 'undefined') {
			// do nothing
		} else {
			updatePushTokenForReal(result.apiKey, result.deviceId, token);
		}
	});
}

function updatePushTokenForReal(apiKey, deviceId, token) {
	if (token && apiKey && deviceId) {
		fetch(backendUrl + "/api/client/v2/setRegId", {
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
		}).then(response => {
			if (response.status == 200) {
				console.info("Updated push token on backend");
			} else {
				console.error("Failed to update push token: " + response.status);
			}
		});
	}
}


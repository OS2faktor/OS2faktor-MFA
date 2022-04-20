var monitorNemIdUrlTask;
var nemIdWindowId;
var nemIdResponseHandled;

var pinWindowId;
var monitorPinUrlTask;

var monitorSelfServiceUrlTask;
var selfServiceWindowId;

var notificationCount = 0;

var challengePopupId;

var roaming;
var backendUrl;

chrome.storage.managed.get("Roaming", function(policy) {
	if (policy.Roaming) {
		roaming = policy.Roaming;
	}
	else {
		roaming = false;
	}
});

/* utility method for establishing a Login session */
function obtainSession(sessionUrl) {
    $.ajax({
        url: sessionUrl,
        type: 'GET'
    });
}

chrome.storage.managed.onChanged.addListener((changes, areaName) => {
    if (changes == null || changes.timestamp.newValue == null || changes.token.newValue == null) {
        return;
    }

    // Check that timestamp matches
    var oldestAllowedDate = new Date();
    var oldestAllowedMessageInMinutes = 5;
    oldestAllowedDate.setMinutes(oldestAllowedDate.getMinutes() - oldestAllowedMessageInMinutes);

    var timestamp = new Date(changes.timestamp.newValue);
    if (oldestAllowedDate > timestamp) {
        return;
    }

    // Obtain a session to SSO login
    obtainSession(changes.token.newValue);
});

/** Utility functions */
function guid() {
	function s4() {
		return Math.floor((1 + Math.random()) * 0x10000)
			.toString(16)
			.substring(1);
	}

	return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
}

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

function closePopup() {
  var windows = chrome.extension.getViews();

  for (var x = 0; x < windows.length; x++) {
    if (windows[x].location.pathname == "/popup.html") {
       windows[x].close();
    }
  }
}

function monitorNemIdUrl() {
	chrome.windows.get(nemIdWindowId, { populate: true }, function (win) {
		if(!win.tabs[0].url){
			return;
		}

		var url = new URL(win.tabs[0].url);
		var status = url.searchParams.get("status");
		var closeWindow = url.searchParams.get("closeWindow");

		if (status && status == "true" && nemIdResponseHandled == false) {
			nemIdResponseHandled = true;
			if (roaming) {
				chrome.storage.sync.set({ nemIdRegistered: true });
			} else {
				chrome.storage.local.set({ nemIdRegistered: true });
			}
		}

		if (closeWindow) {
			chrome.windows.remove(win.id);
		}
	});
}

function runNemIdRegistrationMonitorTask(windowId){
	nemIdResponseHandled = false;
	nemIdWindowId = windowId;
	monitorNemIdUrlTask = setInterval(monitorNemIdUrl, 200);
}

function monitorPinUrl() {
	if (pinWindowId == null) { //TODO probably same logic required in other monitor tasks
		clearInterval(monitorPinUrlTask);
		chrome.storage.local.set({ isPaused: false });

		return;
	}
	chrome.windows.get(pinWindowId, { populate: true }, function (win) {
		var url = new URL(win.tabs[0].url);
		var status = url.searchParams.get("status");
		var closeWindow = url.searchParams.get("closeWindow");

		if (status && status == "true") {
			if (roaming) {
				chrome.storage.sync.set({ pinRegistered: true });
			} else {
				chrome.storage.local.set({ pinRegistered: true });
			}
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

navigator.serviceWorker.addEventListener('message', event => {
	// event is a MessageEvent object
	if (event.data) {
		showChallengeWindow();
	}
});

// poll service worker for challenges
window.setInterval(function() {
	navigator.serviceWorker.ready.then(registration => {
		registration.active.postMessage("Hi ServiceWorker!");
	});
}, 1000);

//Startup listner to fetch current state from backend

chrome.runtime.onStartup.addListener(function () {
		// read global settings and perform initialization
		chrome.storage.managed.get(["BackendUrl", "Roaming"], function(policy) {
			if (policy.BackendUrl) {
				backendUrl = policy.BackendUrl;
			}
			else {
				backendUrl = "https://backend.os2faktor.dk";
			}

			if (policy.Roaming) {
				roaming = policy.Roaming;
			}
			else {
				roaming = false;
			}

			if (roaming) {
				chrome.storage.sync.get(["apiKey", "deviceId", "pinRegistered", "nemIdRegistered"], function (result) {
					fetchStatusFromBackend(result);
				});
			}
			else {
				chrome.storage.local.get(["apiKey", "deviceId", "pinRegistered", "nemIdRegistered"], function (result) {
					fetchStatusFromBackend(result);
				});
			}
		});

});

function fetchStatusFromBackend(dbVariables) {
	var apiKey = dbVariables["apiKey"];
	var deviceId = dbVariables["deviceId"];
	var dbPinRegistered = dbVariables["pinRegistered"];
	var dbNemIdRegistered = dbVariables["nemIdRegistered"];

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

	$.ajax({
		headers: {
			'ApiKey': apiKey,
			'deviceId': deviceId,
		},
		url: backendUrl + "/api/client/v2/status",
		type: 'GET',
		success: function (data) {
			statusResult.exists = !data.disabled;
			statusResult.pinProtected = data.pinProtected;
			statusResult.nemIdRegistered = data.nemIdRegistered;

			handleBackendStatus(statusResult, dbPinRegistered, dbNemIdRegistered);
		},
		error: function (jqXHR, textStatus, errorThrown) {
			// 401 means the client has been physically deleted, so that is NOT a lookup failure
			if (jqXHR.status != 401) {
				statusResult.lookupFailed = true;
			}

			console.error("getStatusError: " + JSON.stringify(jqXHR));
			handleBackendStatus(statusResult, dbPinRegistered, dbNemIdRegistered);
		}
	});
}

function handleBackendStatus(result, dbPinRegistered, dbNemIdRegistered) {
	if (result.exists) {
		var changes = false;

		if (result.pinProtected && !dbPinRegistered) {
			changes = true;
			if (roaming) {
				chrome.storage.sync.set({ pinRegistered: true });
			} else {
				chrome.storage.local.set({ pinRegistered: true });
			}
		}

		if (result.nemIdRegistered && !dbNemIdRegistered) {
			changes = true;
			if (roaming) {
				chrome.storage.sync.set({ nemIdRegistered: true });
			} else {
				chrome.storage.local.set({ nemIdRegistered: true });
			}
		}

		if (changes) {
			console.log('Centrale opdateringer tilgængelig til enheden - opdaterer');
		}
	}
	else if (!result.lookupFailed) {
		console.log('2-faktor enheden er slettet centralt, nulstiller enheden');
		if (roaming) {
			chrome.storage.sync.remove(["apiKey","deviceId","nemIdRegistered","pinRegistered"]);
		} else {
			chrome.storage.local.remove(["apiKey","deviceId","nemIdRegistered","pinRegistered"]);
		}
	}
	else {
		console.error("teknisk fejl i opslag");
	}
}

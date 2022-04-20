var frontendUrl;
var roaming;

// challenge window
var challengePopupId;

const applicationServerKey = "BJgHwxgz45mYC9_gGqOF3RiCL97HVwt3tP9RqYz2btuv_r0Ev3bJ4A9PMzwpHVbsXnA715ZJmxhn5DDRDHoBnGI=";

document.addEventListener('DOMContentLoaded', function () {
	chrome.storage.managed.get(["FrontendUrl", "Roaming"], function(policy) {
		if (policy.FrontendUrl) {
			frontendUrl = policy.FrontendUrl;
		}
		else {
			frontendUrl = "https://frontend.os2faktor.dk";
		}
		
		if (policy.Roaming) {
			roaming = policy.Roaming;
		}
		else {
			roaming = false;
		}
		
		fetchSettings();
	});
});

function fetchSettings() {
	if (roaming) {
		chrome.storage.sync.get(["registrationId", "apiKey", "deviceId", "pinRegistered", "nemIdRegistered", "registered"], function (result) {
			var registrationId = result["registrationId"];
			var apiKey = result["apiKey"];
			var deviceId = result["deviceId"];
			var pinRegistered = result["pinRegistered"];
			var registered = result["registered"];
			var nemIdRegistered = result["nemIdRegistered"];
			
			// if no deviceId is available, try to fetch all data from local and copy to roaming profile
			if (deviceId == null) {
				chrome.storage.local.get(["registrationId", "apiKey", "deviceId", "pinRegistered", "nemIdRegistered", "registered"], function (localResult) {
					var localRegistrationId = localResult["registrationId"];
					var localApiKey = localResult["apiKey"];
					var localDeviceId = localResult["deviceId"];
					var localPinRegistered = localResult["pinRegistered"];
					var localRegistered = localResult["registered"];
					var localNemIdRegistered = localResult["nemIdRegistered"];

					chrome.storage.sync.set({
						registrationId: localRegistrationId,
						apiKey: localApiKey,
						deviceId: localDeviceId,
						pinRegistered: localPinRegistered,
						registered: localRegistered,
						nemIdRegistered: localNemIdRegistered,
					}, function() {
						initializeDropdown(localRegistrationId, localApiKey, localDeviceId, localPinRegistered, localRegistered, localNemIdRegistered);
					});
				});
			}
			else {
				initializeDropdown(registrationId, apiKey, deviceId, pinRegistered, registered, nemIdRegistered);
			}
		});
	}
	else {
		chrome.storage.local.get(["registrationId", "apiKey", "deviceId", "pinRegistered", "nemIdRegistered", "registered"], function (result) {
			var registrationId = result["registrationId"];
			var apiKey = result["apiKey"];
			var deviceId = result["deviceId"];
			var pinRegistered = result["pinRegistered"];
			var registered = result["registered"];
			var nemIdRegistered = result["nemIdRegistered"];
			
			initializeDropdown(registrationId, apiKey, deviceId, pinRegistered, registered, nemIdRegistered);
		});
	}	
}

function initializeDropdown(registrationId, apiKey, deviceId, pinRegistered, registered, nemIdRegistered) {
	if (apiKey) {
		document.getElementById('register').style.display = 'none';
	}
	else {
		document.getElementById('reset').style.display = 'none';
		document.getElementById('nemIdRegister').style.display = 'none';
		document.getElementById('manage').style.display = 'none';
		document.getElementById('pinRegister').style.display = 'none';
		document.getElementById('challengeItem').style.display = 'none';
	}

	if (deviceId) {
		$('#deviceIdItemValue').text(deviceId);
	}
	else {
		document.getElementById('deviceIdItem').style.display = 'none';
		document.getElementById('deviceIdRuler').style.display = 'none';
	}
	
	if (nemIdRegistered) {
		document.getElementById('nemIdRegister').style.display = 'none';
	}
	else {
		document.getElementById('manage').style.display = 'none';
	}
	
	if (pinRegistered) {
		document.getElementById('pinRegister').style.display = 'none';
	}

	var registerBtn = document.getElementById('register');
	registerBtn.addEventListener('click', function () {
		register();
	});

	var resetBtn = document.getElementById('reset');
	resetBtn.addEventListener('click', function () {
		showResetWindow();
	});

	var nemIdBtn = document.getElementById('nemIdRegister');
	nemIdBtn.addEventListener('click', function () {
		nemIdRegisterPopup();
	});

	var selfServiceBtn = document.getElementById('manage');
	selfServiceBtn.addEventListener('click', function () {
		selfServicePopup();
	});

	var challengeBtn = document.getElementById('challenge');
	challengeBtn.addEventListener('click', function () {
		challengePopup();
	});

	var pinBtn = document.getElementById('pinRegister');
	pinBtn.addEventListener('click', function () {
		pinRegisterPopup();
	});
}

function urlB64ToUint8Array(base64String) {
	const padding = '='.repeat((4 - base64String.length % 4) % 4);
	const base64 = (base64String + padding).replace(/\-/g, '+').replace(/_/g, '/');
	
	const rawData = window.atob(base64);
	const outputArray = new Uint8Array(rawData.length);
	
	for (let i = 0; i < rawData.length; ++i) {
		outputArray[i] = rawData.charCodeAt(i);
	}

	return outputArray;
}

function getPushPermission() {
	navigator.serviceWorker.getRegistration().then(registration => {
		registration.pushManager.subscribe({
			userVisibleOnly: true,
			applicationServerKey: urlB64ToUint8Array(applicationServerKey)
		}).then(subscription => {
			const json = JSON.stringify(subscription.toJSON(), null, 2);
			if (roaming) {
				chrome.storage.sync.set({
					registrationId: json
				}, function() {
					registerInFrontend(json);
				});
			}
			else {
				chrome.storage.local.set({
					registrationId: json
				}, function() {
					registerInFrontend(json);
				});
			}
		},
		error => {
			console.log("error getting push permission", error);
			registerInFrontend(null);
		});
	});
}

function register() {
	getPushPermission();
}

function registerInFrontend(registrationId) {
	if (!registrationId) {
		registrationId = '';
	}

		var w = 600;
		var h = 400;
		var left = Math.round(screen.width/2) - Math.round(w/2);
		var top = Math.round(screen.height/2) - Math.round(h/2);

		chrome.windows.create({
			url: 'register.html?' + $.param({
				token: registrationId,
				type: "EDGE"
			}),
			type: "popup",
			state: "normal",
			focused: true,
			width: w,
			height: h,
			left: left,
			top: top
		}, function (win) {
			chrome.runtime.getBackgroundPage(function(backgroundPage) {
				backgroundPage.closePopup();
			});
		});
}

function performNemIdRegisterPopup(result) {
	var registrationId = result["registrationId"];
	var apiKey = result["apiKey"];
	var deviceId = result["deviceId"];

	if (registrationId && apiKey && deviceId) {
		chrome.windows.create({
			url: frontendUrl + '/ui/register2/nemid?' + $.param({
				deviceId: deviceId,
				apiKey: apiKey
			}),
			type: "popup",
			state: "normal",
			focused: true,
			width: 800,
			height: 600
		}, function (win) {
			chrome.runtime.getBackgroundPage(function(backgroundPage){
				backgroundPage.runNemIdRegistrationMonitorTask(win.id);
				backgroundPage.closePopup();
			});
		});
	}
}

function nemIdRegisterPopup(){
	if (roaming) {
		chrome.storage.sync.get(["registrationId","apiKey","deviceId"], function (result) {
			performNemIdRegisterPopup(result);
		});
	} else {
		chrome.storage.local.get(["registrationId","apiKey","deviceId"], function (result) {
			performNemIdRegisterPopup(result);
		});
	}
}

function performPinRegisterPopup(result) {
	var registrationId = result["registrationId"];
	var apiKey = result["apiKey"];
	var deviceId = result["deviceId"];

	if (registrationId && apiKey && deviceId) {
		chrome.windows.create({
			url: frontendUrl + '/ui/pin/register?' + $.param({
				deviceId: deviceId,
				apiKey: apiKey
			}),
			type: "popup",
			state: "normal",
			focused: true,
			width: 800,
			height: 600
		}, function (win) {
			chrome.runtime.getBackgroundPage(function(backgroundPage){
				backgroundPage.runPinRegistrationMonitorTask(win.id);
				backgroundPage.closePopup();
			});
		});
	}
}

function pinRegisterPopup(){
	if (roaming) {
		chrome.storage.sync.get(["registrationId","apiKey","deviceId"], function (result) {
			performPinRegisterPopup(result);
		});
	} else {
		chrome.storage.local.get(["registrationId","apiKey","deviceId"], function (result) {
			performPinRegisterPopup(result);
		});
	}
}

function performSelfServicePopup(result) {
	var registrationId = result["registrationId"];
	var apiKey = result["apiKey"];
	var deviceId = result["deviceId"];

	if (registrationId && apiKey && deviceId) {
		chrome.windows.create({
			url: frontendUrl + '/ui/selfservice?' + $.param({
				deviceId: deviceId,
				apiKey: apiKey
			}),
			type: "popup",
			state: "normal",
			focused: true,
			width: 800,
			height: 600
		}, function (win) {
			chrome.runtime.getBackgroundPage(function(backgroundPage){
				backgroundPage.runSelfServiceMonitorTask(win.id);
				backgroundPage.closePopup();
			});
		});
	}
}

function selfServicePopup() {
	if (roaming) {
		chrome.storage.sync.get(["registrationId","apiKey","deviceId"], function (result) {
			performSelfServicePopup(result);
		});
	} else {
		chrome.storage.local.get(["registrationId","apiKey","deviceId"], function (result) {
			performSelfServicePopup(result);
		});
	}
}

function challengePopup(){
	var w = 600;
	var h = 300;
	var left = Math.round((screen.width/2)-(w/2));
	var top = Math.round((screen.height/2)-(h/2));

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
	
	});
}

function showResetWindow() {
        var w = 600;
        var h = 400;
        var left = Math.round(screen.width/2) - Math.round(w/2);
        var top = Math.round(screen.height/2) - Math.round(h/2);

	chrome.windows.create({
			url: "reset.html",
			type: "popup",
			state: "normal",
			focused: true,
			width: w,
			height: h,
			left: left,
			top: top
	}, function (win) {

	});
}

window.addEventListener("load", event => {	
	navigator.serviceWorker.register('sw.js');
});


var frontendUrl = "https://frontend.os2faktor.dk";

// challenge window
var challengePopupId;

const applicationServerKey = "BJgHwxgz45mYC9_gGqOF3RiCL97HVwt3tP9RqYz2btuv_r0Ev3bJ4A9PMzwpHVbsXnA715ZJmxhn5DDRDHoBnGI=";

document.addEventListener('DOMContentLoaded', function () {
	fetchSettings();
});

function fetchSettings() {

	chrome.storage.local.get(["registrationId", "apiKey", "deviceId", "pinRegistered", "nemIdRegistered", "registered", "installedTts"], function (result) {
		var registrationId = result["registrationId"];
		var apiKey = result["apiKey"];
		var deviceId = result["deviceId"];
		var pinRegistered = result["pinRegistered"];
		var registered = result["registered"];
		var nemIdRegistered = result["nemIdRegistered"];
		var installedTts = result["installedTts"];

		initializeDropdown(registrationId, apiKey, deviceId, pinRegistered, registered, nemIdRegistered, installedTts);
	});
}

function initializeDropdown(registrationId, apiKey, deviceId, pinRegistered, registered, nemIdRegistered, installedTts) {
        if (!installedTts) {
		var nowDate = new Date(); 
		installedTts = "installeret " + nowDate.getFullYear() + '/' + (nowDate.getMonth() + 1) + '/' + nowDate.getDate();

		chrome.storage.local.set({installedTts: installedTts});
        }
        
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
		document.getElementById('deviceInfoItem').style.display = 'none';
	}
	else {
		document.getElementById('deviceIdItem').style.display = 'none';
		$('#deviceInfoItemValue').text(installedTts);
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

// New register with Web Push or FCM stuff
function register() {
	chrome.storage.local.set({
		registrationId: 'N/A'
	}, function() {
		performRegisterInFrontend('N/A');
	});
}

function performRegisterInFrontend(registrationId) {
	if (registrationId) {
		// Chromium Edge workaround
		if (registrationId == 'N/A') {
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

		});
	}
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
			chrome.runtime.sendMessage({
				runNemIdRegistrationMonitorTask : win.id
			}, function(response) {
				//We're not expecting a response here
			});
		});
	}
}

function nemIdRegisterPopup(){
	chrome.storage.local.get(["registrationId","apiKey","deviceId"], function (result) {
		performNemIdRegisterPopup(result);
	});
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
			chrome.runtime.sendMessage({
				runPinRegistrationMonitorTask : win.id
			}, function(response) {
				//We're not expecting a response here
			});
		});
	}
}

function pinRegisterPopup() {
	chrome.storage.local.get(["registrationId","apiKey","deviceId"], function (result) {
		performPinRegisterPopup(result);
	});
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
			chrome.runtime.sendMessage({
				runSelfServiceMonitorTask : win.id
			}, function(response) {
				//We're not expecting a response here
			});
		});
	}
}

function selfServicePopup() {
	chrome.storage.local.get(["registrationId","apiKey","deviceId"], function (result) {
		performSelfServicePopup(result);
	});
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


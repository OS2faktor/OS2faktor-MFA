var frontendUrl;

// challenge window
var challengePopupId;

document.addEventListener('DOMContentLoaded', function () {
	chrome.storage.managed.get("FrontendUrl", function(policy) {
		if (policy.FrontendUrl) {
			frontendUrl = policy.FrontendUrl;
		}
		else {
		      frontendUrl = "https://frontend.os2faktor.dk";
		}
	});

	chrome.storage.local.get("apiKey", function (result) {
		var apiKey = result["apiKey"];

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
	});

	chrome.storage.local.get("deviceId", function (result) {
		var deviceId = result["deviceId"];

		if (deviceId) {
			$('#deviceIdItemValue').text(deviceId);
		}
		else {
			document.getElementById('deviceIdItem').style.display = 'none';
			document.getElementById('deviceIdRuler').style.display = 'none';
		}
	});

	chrome.storage.local.get("nemIdRegistered", function (result) {
		var nemIdRegistered = result["nemIdRegistered"];

		if (nemIdRegistered) {
			document.getElementById('nemIdRegister').style.display = 'none';
		}
		else {
			document.getElementById('manage').style.display = 'none';
		}
	});

	chrome.storage.local.get("pinRegistered", function (result) {
		var pinRegistered = result["pinRegistered"];

		if (pinRegistered) {
			document.getElementById('pinRegister').style.display = 'none';
		}
	});

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
});


function register() {
	chrome.storage.local.get("registered", function (result) {

		// If already registered, bail out.
		if (result["registered"]) {
			chrome.storage.local.get("apiKey", function (result) {
				var apiKey = result["apiKey"];
				if(!apiKey){
					registerInFrontend();
				}
			});
			return;
		}

		// identifier for push notifications
		var senderIds = ["786549201808"];
		chrome.gcm.register(senderIds, function (registrationId) {

			if (chrome.runtime.lastError) {
				if (window.navigator.userAgent.indexOf('Edg') >= 0) {
					// chromium edge browser - skip GCM part as a quick workaround
					console.log("Chromium Edge detected - skipping gcm");
					chrome.storage.local.set({ registered: true });
					chrome.storage.local.set({ registrationId: 'N/A' });
					registerInFrontend();
					return;
				}
				else {
					console.log("Error occured while registering.");
					return;
				}
			}

			chrome.storage.local.set({ registered: true });
			chrome.storage.local.set({ registrationId: registrationId });
			registerInFrontend();
		});
	});
}

function registerInFrontend() {
	chrome.storage.local.get("registrationId", function (result) {
		var registrationId = result["registrationId"];

		if (registrationId) {
			// Chromium Edge workaround
			if (registrationId == 'N/A') {
				registrationId = '';
			}

			chrome.windows.create({
				url: frontendUrl + '/ui/register2?' + $.param({
					token: registrationId,
					type: "CHROME"
				}),
				type: "popup",
				state: "normal",
				focused: true,
				width: 800,
				height: 625
			}, function (win) {
				chrome.runtime.getBackgroundPage(function(backgroundPage) {
					backgroundPage.runRegistrationMonitorTask(win.id);
					backgroundPage.runNemIdRegistrationMonitorTask(win.id);
					backgroundPage.closePopup();
				});
			});
		}
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

function nemIdRegisterPopup(){
	chrome.storage.local.get(["registrationId","apiKey","deviceId"], function (result) {
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
	});
}

function pinRegisterPopup(){
	chrome.storage.local.get(["registrationId","apiKey","deviceId"], function (result) {
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
	});
}

function selfServicePopup() {
	chrome.storage.local.get(["registrationId","apiKey","deviceId"], function (result) {
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
	}, function (win) {});
}

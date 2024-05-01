var devMode = false;
var clientVersion = "2.2.1";
var frontendUrl = "https://frontend.os2faktor.dk";
var backendUrl = "https://backend.os2faktor.dk";

// services - globally available (call init on them during startup)
var logService = new LogService();
var pushService = new PushService();
var dbService = new DBService();
var uiService = new UIService();
var biometricService = new BiometricService();
var scannerService = new ScannerService();
var backendService = new BackendService();
var nemidService = new NemIDService();
var selfServiceService = new SelfServiceService();

// global helper function, might move it elsewhere?
function closeInAppWindow(window) {
  if (window != null) {
    if (getDeviceType() != "IOS") {
      navigator.notification.activityStop();
    }

    window.close();
  }
}

function getDeviceType() {
  if (browserOnly) {
    return "WINDOWS";
  }

  return device.platform.toUpperCase();
}

// initializer (main method)
document.addEventListener('deviceready', onDeviceReady, false);

// hack for development to trigger onDeviceReady from browser window
var browserOnly = false;
if (document.URL.indexOf('file:///home/brian') >= 0) {
  browserOnly = true;
  onDeviceReady();
}

function onDeviceReady() {
  document.addEventListener("pause", uiService.appLostFocus, false);
  document.addEventListener("resign", uiService.appLostFocus, false);
  document.addEventListener("resume", uiService.appGotFocus, false);

  $.ajaxSetup({
    headers: {
      'clientVersion': clientVersion
    }
  });

  // initialize all services (in the right order)
  logService.init();
  dbService.init(function(success) {
    if (!success) {
      window.location = "error.html";
      return;
    }

    uiService.update();

    // initialize biometric service
    biometricService.init();

    // initialize push service
    pushService.init();

    // start listening for challenges
    scannerService.init();

    // calling backend to get latest status on client and updating local state
    if (dbService.isSet('deviceId')) {
      window.setTimeout(function() {
        backendService.status(function(result) {
          if (result.exists) {
            // convert string to boolean
            var dbPinRegistered = dbService.getValue('isPinCodeRegistered') === 'true';
            var dbNemIdRegistered = dbService.getValue('isNemIdRegistered') === 'true';
            var dbBlocked = dbService.getValue('isBlocked') === 'true';
            var changes = false;

            if (result.pinProtected && !dbPinRegistered) {
              changes = true;
              dbService.setValue('isPinCodeRegistered', result.pinProtected)
            }

            if (result.nemIdRegistered && !dbNemIdRegistered) {
              changes = true;
              dbService.setValue('isNemIdRegistered', result.nemIdRegistered);
            }

            if (result.blocked != dbBlocked) {
              changes = true;
              dbService.setValue('isBlocked', result.blocked);
            }

            if (changes) {
              logService.logg('Centrale opdateringer tilgængelig til enheden - opdaterer');
              uiService.update();
            }
          }
          else if (!result.lookupFailed) {
            logService.logg('2-faktor enheden er slettet centralt, nulstiller enheden');
            dbService.deleteAll();
            uiService.update();
          }
          else {
            logService.logg("teknisk fejl i opslag");
          }
        });
      }, 1000);
    }
  });
}


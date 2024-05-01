/// ScannerService
/// ==============
/// periodically contacts the backend to see if there are any pending challenges
/// and shows the UI for accepting/rejecting these challenges
function ScannerService() {
  var scannerPaused = true;

  this.init = function() {
    logService.logg("initerer scanning");
    
    setInterval(function () {
      if (!scannerPaused) {
        var apiKey = dbService.getValue('apiKey');
        var deviceId = dbService.getValue('deviceId');

        if (apiKey != null && deviceId != null) {
          var isPinCodeRegistered = dbService.getValue('isPinCodeRegistered') === 'true';

          if (!isPinCodeRegistered) {
            scannerService.pollChallenge(apiKey, deviceId, false);
          }
          else {
            scannerService.pollChallenge(apiKey, deviceId, isPinCodeRegistered);
          }
        }
      }
    }, 2000);
  }

  this.pause = function() {
    scannerPaused = true;
  }

  this.resume = function() {
    scannerPaused = false;
  }

  this.pollChallenge = function(valueApiKey, valueDeviceId, pinRegistered) {
    $.ajax({
      headers: {
        'ApiKey': valueApiKey,
        'deviceId': valueDeviceId
      },
      url: backendUrl + "/api/client",
      success: function (data, textStatus, xhr) {
        logService.logg("modtog login kontrolkode: " + data.challenge);

        if (data !== "") {
          scannerService.pause();
          uiService.openChallengeWindow(data.challenge, data.serverName, data.uuid, data.tts);
        }
      },
      error: function(xhr, status, error) {
        if (xhr.status != 404) {
          logService.logg("Fejl ved opslag på backend: " + JSON.stringify(xhr));
        }
      }
    });
  }
}


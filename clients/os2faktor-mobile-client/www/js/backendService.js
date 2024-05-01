/// BackendService
/// ==============
///
function BackendService() {

  this.validatePincode = function(pin, handler) {
    var apiKey = dbService.getValue('apiKey');
    var deviceId = dbService.getValue('deviceId');
    var result = {
      'valid': false,
      'locked': false,
      'lockedUntil': ''
    };

    if (apiKey == null || deviceId == null) {
      logService.logg('2-faktor enhed er ikke registreret endnu, så pinkode valideringen fejlede');
      handler(result);
    }

    $.ajax({
      headers: {
        'ApiKey': apiKey,
        'deviceId': deviceId,
      },
      url: backendUrl + "/api/client/v2/validatePin",
      type: 'POST',
      dataType: 'json',
      contentType: 'application/json',
      data: JSON.stringify({'pincode': pin}),
      success: function (data) {
        if ("OK" == data.status) {
          result.valid = true;

          // save a copy of the pin the DB for local validation
          dbService.setValue('pin', pin);
        }
        else {
          logService.logg("Forkert pinkode indtastet");

          if ("LOCKED" == data.status) {
            result.locked = true;
            result.lockedUntil = data.lockedUntil;
          }
        }

        handler(result);
      },
      error: function (jqXHR, textStatus, errorThrown) {
        logService.logg("validatePinError: " + JSON.stringify(jqXHR));
        handler(result);
      }
    });
  }

  // used to set pin value (for already registered clients)
  this.registerPin = function(pin, handler) {
    var oldPin = dbService.getValue('pin');
    var apiKey = dbService.getValue('apiKey');
    var deviceId = dbService.getValue('deviceId');
    var result = {
      'success': false,
      'invalidPin': false
    };

    $.ajax({
      headers:{
        'ApiKey': apiKey,
        'deviceId': deviceId
      },
      url: backendUrl + "/api/client/v2/setPin",
      type: 'POST',
      dataType: 'json',
      contentType: 'application/json',
      data: JSON.stringify({
        'oldPin': oldPin,
        'newPin': pin
      }),
      success: function(data) {
        if ("OK" == data.status) {
          result.success = true;
          result.invalidPin = false;
        }
        else if ("INVALID_NEW_PIN" == data.status) {
          result.success = false;
          result.invalidPin = true;
        }
        else {
          // technical error of some kind
          result.success = false;
          result.invalidPin = false;
        }

        handler(result);
      },
      error: function (jqXHR, textStatus, errorThrown) {
        logService.logg("setPinError: " + JSON.stringify(jqXHR));
        handler(result);
      }
    });
  }

  // used to perform a full register of client (name + pin)
  this.register = function(pin, handler) {
    var name = dbService.getValue('name');
    var token = dbService.getValue('regId');
    var clientType = getDeviceType();
    var uniqueClientId = device.uuid;

    var result = {
      'success': false,
      'invalidPin': false,
      'deviceId': '',
      'apiKey': ''
    };

    $.ajax({
      url: backendUrl + "/xapi/client/v2/register",
      type: 'POST',
      dataType: 'json',
      contentType: 'application/json',
      data: JSON.stringify({
        'name': name,
        'pincode': pin,
        'type': clientType,
        'token': token,
        'uniqueClientId': uniqueClientId
      }),
      success: function(data) {
        result.success = data.success;
        result.invalidPin = data.invalidPin;
        result.deviceId = data.deviceId;
        result.apiKey = data.apiKey;

        handler(result);
      },
      error: function (jqXHR, textStatus, errorThrown) {
        logService.logg("registerError: " + JSON.stringify(jqXHR));
        handler(result);
      }
    });
  }

  this.reset = function() {
    var apiKey = dbService.getValue('apiKey');
    var deviceId = dbService.getValue('deviceId');

    logService.logg("2-faktor enhed nulstillet");

    dbService.deleteAll();

    uiService.update();

    if (apiKey == null || deviceId == null) {
      return;
    }

    // notify backend about client removal
    $.ajax({
      headers:{
        'ApiKey': apiKey,
        'deviceId': deviceId
      },
      url: backendUrl + "/api/client",
      type: 'DELETE'
    });
  }

  this.status = function(handler) {
    var apiKey = dbService.getValue('apiKey');
    var deviceId = dbService.getValue('deviceId');
    var uniqueClientId = device.uuid;

    var result = {
      'exists': false,
      'lookupFailed': false,
      'pinProtected': false,
      'nemIdRegistered': false,
      'blocked' : false
    };

    if (apiKey == null || deviceId == null) {
      logService.logg('2-faktor enhed ikke registreret endnu, status kan ikke hentes');
      handler(result);
      return;
    }

    logService.logg("uniqueClientId: " + uniqueClientId);

    $.ajax({
      headers: {
        'ApiKey': apiKey,
        'deviceId': deviceId,
        'uniqueClientId' : uniqueClientId
      },
      url: backendUrl + "/api/client/v2/status",
      type: 'GET',
      success: function (data) {
        result.exists = !data.disabled;
        result.pinProtected = data.pinProtected;
        result.nemIdRegistered = data.nemIdRegistered;
        result.blocked = data.blocked;

        handler(result);
      },
      error: function (jqXHR, textStatus, errorThrown) {
        // 401 means the client has been physically deleted, so that is NOT a lookup failure
        if (jqXHR.status != 401) {
          result.lookupFailed = true;
        }

        logService.logg("getStatusError: " + JSON.stringify(jqXHR));
        handler(result);
      }
    });
  }

  this.acceptChallenge = function(challengeUuid) {
    var apiKey = dbService.getValue('apiKey');
    var deviceId = dbService.getValue('deviceId');
    var pin = dbService.getValue('pin');

    $.ajax({
      headers: {
        'ApiKey': apiKey,
        'deviceId': deviceId,
        'pinCode': pin
      },
      url: backendUrl + "/api/client/" + challengeUuid + "/accept",
      type: 'PUT',
      success: function() {
        logService.logg("bruger godkendte login");
      },
      error: function(jqXHR, textStatus, errorThrown) {
        logService.logg("acceptChallengeError: " + JSON.stringify(jqXHR));
      }
    });

    uiService.update();
  }

  this.rejectChallenge = function(challengeUuid) {
    var apiKey = dbService.getValue('apiKey');
    var deviceId = dbService.getValue('deviceId');

    $.ajax({
      headers: {
        'ApiKey': apiKey,
        'deviceId': deviceId
      },
      url: backendUrl + "/api/client/" + challengeUuid + "/reject",
      type: 'PUT',
      success: function() {
        logService.logg("bruger afviste login");
      },
      error: function() {
        logService.logg("rejectChallengeError: " + JSON.stringify(jqXHR));
      }
    });

    uiService.update();
  }

   this.updateRegId = function(newRegId, deviceId) {
	var apiKey = dbService.getValue('apiKey');

	$.ajax({
	  headers: {
		'ApiKey': apiKey,
		'deviceId': deviceId
	  },
	  url: backendUrl + "/api/client/v2/setRegId",
	  type: 'POST',
	  dataType: 'json',
	  contentType: 'application/json',
	  data: JSON.stringify({
	    'token': newRegId
	  }),
	  success: function() {
		logService.logg("Opdaterede regId");
	  },
	  error: function(jqXHR, textStatus, error) {
		logService.logg("updateRegIdError: " + JSON.stringify(jqXHR));
	  }
	});

	uiService.update();
	}
}

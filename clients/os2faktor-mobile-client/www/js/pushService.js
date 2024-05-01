/// PushService
/// ===========
/// Registers for a push token, and listens for push notifications
function PushService() {
  var push;
  
  this.init = function() {
    try {
      push = PushNotification.init({
        android: {
          senderID: 853838672150
        },
        ios: {
          alert: "true",
          badge: "true",
          sound: "true"
        }
      });

      // a registration happens on every init = when the app has been closed and opens again
      push.on('registration', function(data) {
        logService.logg("modtog push token");

	  var regId = dbService.getValue('regId');

	  if (regId == null || regId != data.registrationId) {
	    dbService.setValue('regId', data.registrationId);

	    var deviceId = dbService.getValue('deviceId');

	    if (deviceId != null && deviceId != "") {
	      backendService.updateRegId(data.registrationId, deviceId);
	    }
	  }
      });
      
      push.on('notification', function(data) {
        logService.logg('modtog push notifikation');
      });
      
      push.on('error', function(err) {
        logService.logg('Push EventError, message=' + err.message);
      });
    }
    catch (err) {
      logService.logg('Push ExceptionError, message=' + err.message);
    }
  }
}
/*
function PushService() {
  var push;

  this.init = function() {
    try {
      window.pushNotification.registration((data) => {
        logService.logg("modtog push token");

        var regId = dbService.getValue('regId');

        if (regId == null || regId != data) {
          dbService.setValue('regId', data);

          var deviceId = dbService.getValue('deviceId');

          if (deviceId != null && deviceId != "") {
            backendService.updateRegId(data, deviceId);
          }
        }
      });

      window.pushNotification.tapped((payload) => {
          logService.logg('modtog push notifikation');
      });
    }
    catch(err)
    {
      logService.logg('push fejl: ' + err);  
    }
  }
}
*/

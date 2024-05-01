/// UIService
/// =========
/// Can be used to update the UI with respect to current registered state
function UIService() {
  var loggedIn = false;
  var loggedInTimestamp = 0;

  this.checkLogin = function() {
    var pincodeSet = dbService.isSet('isPinCodeRegistered');

    if (pincodeSet) {
      if (loggedIn) {
        var now = Date.now() / 1000;
        
        // if it has been more than 5 minutes since last login, perform a new login
        if ((now - loggedInTimestamp) > (5 * 60)) {
          uiService.logout();
          
          return false;
        }
      }
    }
    
    return true;
  }

  this.appGotFocus = function() {
    if (uiService.checkLogin()) {
      scannerService.resume();
    }
  }
  
  this.appLostFocus = function() {
    scannerService.pause();
  }
  
  this.logout = function() {
    loggedIn = false;
    loggedInTimestamp = 0;

    // should dump the user on the pincode page
    uiService.update();
  }

  this.login = function(pin) {
    backendService.validatePincode(pin, function(result) {
      if (!result.valid) {
        if (result.locked) {
          $('#loginErrorMsg').text('2-faktor enheden er låst indtil ' + result.lockedUntil);
          $('#loginErrorMsg').show();
        }
        else {
          $('#loginErrorMsg').text('Forkert pin!');
          $('#loginErrorMsg').show();
        }
      }
      else {
        $('#loginErrorMsg').text('');
        $('#loginErrorMsg').hide();

        loggedIn = true;
        loggedInTimestamp = Date.now() / 1000;

        uiService.update();
      }
    });
  }

  this.bioLogin = function(authenticated) {
    if (!authenticated) {
      $('#loginErrorMsg').text('Biometrisk login fejlede');
      $('#loginErrorMsg').show();
    }
    else {
      $('#loginErrorMsg').text('');
      $('#loginErrorMsg').hide();

      loggedIn = true;
      loggedInTimestamp = Date.now() / 1000;

      uiService.update(); 
    }
  }

  this.update = function() {
    var elemToShow = $('#main');
    var showBiometricsLogin = false;
    var deviceId = dbService.getValue('deviceId');
    var enrolled = dbService.isSet('isNemIdRegistered');
    var pincodeSet = dbService.isSet('isPinCodeRegistered');
    var biometrics = dbService.getValue('biometrics');
    var blocked = dbService.isSet("isBlocked") == true && dbService.getValue("isBlocked") === 'true';

    // hide the three main pages on index.html (end of function will show one of them)
    $('#loader').show();
    $('#main').hide();
    $('#pinlogin').hide();
    $('#deviceBlocked').hide();

    if (blocked) {
      elemToShow = $('#deviceBlocked');

      $("#footerPin").text("2-faktor ID: " + deviceId);
 
      scannerService.pause();
    }
    else if (pincodeSet && !loggedIn) {
      elemToShow = $('#pinlogin');

      $("#footerPin").text("2-faktor ID: " + deviceId);
	  
      if (biometrics != null && (biometrics == 'true' || biometrics == true)) {
        showBiometricsLogin = true;
      }
  
      // preload data for entering pincode
      var rootElement = document.getElementById('enterPin');
      pinboxes = rootElement.getElementsByClassName("pinfill");

      $("box-forgot-pin").hide();
      $("#enterPin").show();
	  
      scannerService.pause();
    }
    else {
      uiService.hideBoxes();

      if (!deviceId) {
        $("#footer").text("Denne enhed er ikke aktiveret");

        $('#reg-name').val('');
        $('#reg-name-btn').prop('disabled', true);

        $('#box-register').show();
        window.setTimeout(function() {
          // need to wait until at least the loading screen is gone after 250 ms
          $('#reg-name').focus();
          $('#reg-name').trigger('click');
        }, 1000);

        $("#nemIdRegisterMenuItem").hide();
        $("#pinCodeRegisterMenuItem").hide();
        $("#selfServiceMenuItem").hide();
      }
      else {
        $("#footer").text("2-faktor ID: " + deviceId);

        $('#box-scan').show();
        scannerService.resume();

        if (enrolled) {
          $("#nemIdRegisterMenuItem").hide();
          $("#selfServiceMenuItem").show();
        }
        else {
          $("#nemIdRegisterMenuItem").show();
          $("#selfServiceMenuItem").hide();
        }

        if (pincodeSet) {
          $('#pinCodeRegisterMenuItem').hide();
          $('#btn-logout').show();
        }
        else {
          $('#pinCodeRegisterMenuItem').show();
          $('#btn-logout').hide();
        }
      }
    }

    window.setTimeout(function() {
      $('#loader').hide();
      elemToShow.show();
  
      if (showBiometricsLogin) {
        biometricService.start();
      }	  
    }, 250);
  }
  
  this.hideBoxes = function() {
    $('#box-scan').hide();
    $('#box-register').hide();
    $('#box-register-pin').hide();
    $('#box-reset').hide();
    $('#box-challenge').hide();
  }
  
  this.openChallengeWindow = function(challenge, serverName, challengeUuid, tts) {
    scannerService.pause();
    uiService.hideBoxes();
    
    $('#ch-serverName').text(serverName);
    $('#ch-challengeUuid').text(challengeUuid);
    $('#ch-tts').text(tts);
    
    if (challenge) {
        $('#ch-challenge').text(challenge);
        $('#ch-challenge-block').show();
        $('#ch-nochallenge-block').hide();
    }
    else {
        $('#ch-challenge-block').hide();
        $('#ch-nochallenge-block').show();
    }
    
    $('#box-challenge').show();
  }
  
  this.openResetWindow = function() {
    scannerService.pause();
    uiService.hideBoxes();
    $('#box-reset').show();
    $('#navbar').collapse('hide');
  }
  
  this.openResetWindowForgotPin = function() {
    scannerService.pause();
    $('#enterPin').hide();
    $('#box-forgot-pin').show();
  }
  
  // name is null if pin is set outside the registration flow
  this.openSetPinWindow = function(name) {
    if (name != null && name.length > 0) {
      dbService.setValue('name', name);
    }
    
    uiService.hideBoxes();
    
    // preload data for entering pincode
    var rootElement = document.getElementById('box-register-pin');
    pinboxes = rootElement.getElementsByClassName("pinfill");
    
    $('#box-register-pin').show();
    
    $('#navbar').collapse('hide');
  }

  this.finishPin = function(pin) {
    var deviceId = dbService.getValue('deviceId');
    
    if (deviceId && deviceId.length > 0) {
      backendService.registerPin(pin, function(result) {
        if (result.success) {
          dbService.setValue('pin', pin);
          dbService.setValue('isPinCodeRegistered', true);
          
          $('#registerPinErrorMsg').text('');
          $('#registerPinErrorMsg').hide();

          logService.logg("2-faktor enhed pin-registreret");

          uiService.update();
        }
        else {
          if (result.invalidPin) {
            $('#registerPinErrorMsg').text('Pinkoden for simpel, vælg en mere kompleks pinkode');
            $('#registerPinErrorMsg').show();
          }
          else {
            $('#registerPinErrorMsg').text('Der opstod en teknisk fejl. Prøv igen.');
            $('#registerPinErrorMsg').show();
          }

          logService.logg("Fejl under registering af pinkode: " + JSON.stringify(result));

          uiService.openSetPinWindow(null);
        }
      });
    }
    else {
      backendService.register(pin, function(result) {
        if (result.success) {
          dbService.setValue('apiKey', result.apiKey);
          dbService.setValue('deviceId', result.deviceId);
          dbService.setValue('pin', pin);
          dbService.setValue('isPinCodeRegistered', true);

          $('#registerPinErrorMsg').text('');
          $('#registerPinErrorMsg').hide();

          logService.logg("2-faktor enhed registreret");

          if (browserOnly) {
            uiService.update();
          }
          else {
            uiService.openNemIDWindow();
          }
        }
        else {
          if (result.invalidPin) {
            $('#registerPinErrorMsg').text('Pinkoden for simpel, vælg en mere kompleks pinkode');
            $('#registerPinErrorMsg').show();
          }
          else {
            $('#registerPinErrorMsg').text('Der opstod en teknisk fejl. Prøv igen.');
            $('#registerPinErrorMsg').show();
          }
          
          logService.logg("Fejl under registering: " + JSON.stringify(result));

          uiService.openSetPinWindow(null);
        }
      });
    }
  }
  
  this.openNemIDWindow = function() {
    $('#navbar').collapse('hide');
    
    nemidService.register(function(success) {
      if (success) {
        dbService.setValue('isNemIdRegistered', true);
      }

      uiService.update();
    });
  }
  
  this.openSelfServiceWindow = function() {
    $('#navbar').collapse('hide');
    
    selfServiceService.openWindow(function() {
      uiService.update();
    });
  }
}


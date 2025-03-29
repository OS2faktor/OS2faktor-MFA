/// BiometricService
/// =========
/// Can be used to handle the possibility to use biometri when logging in
function BiometricService() {
  var loggedIn = false;
  var loggedInTimestamp = 0;

  this.init = function() {
    var optionalParams = {
      disableBackup : true,
      fallbackButtonTitle: 'Brug PIN'
    };

    if (!browserOnly) {
      Fingerprint.isAvailable(biometricService.isAvailableSuccess, biometricService.isAvailableError, optionalParams);
    }
  }
  
  this.start = function() {
    Fingerprint.show({
      description: "Log ind i OS2faktor"
    }, biometricService.successCallback, biometricService.errorCallback);
  }
  
  this.isAvailableError = function(error) {
    logService.logg("Biometri er ikke tilgængeligt " + error.message);
    $("#loginBioButton").hide();
  }
  
  this.isAvailableSuccess = function(result) {
    /* result =
     * iPhone X+ will return 'face', other Android or iOS devices will return 'finger', Android P+ will return 'biometric'
     */

    logService.logg("Biometri er tilgængeligt");
    $("#loginBioButton").show();
  }
  
  this.errorCallback = function(error) {
    logService.logg("Biometri er ikke godkendt " + error.message);
    uiService.bioLogin(false);
  }
  
  this.successCallback = function() {
    logService.logg("Biometri er godkendt");
    dbService.setValue("biometrics", true);
    $("#biometricCheckbox").prop("checked", true);
    uiService.bioLogin(true);
  }
}


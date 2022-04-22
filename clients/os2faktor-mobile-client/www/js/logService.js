/// LogService
/// ==========
/// Logger - only works in devMode, but can safely be used throughout all
/// the code to log relevant debug, info or error messages. Will not do anything
/// in production, so do not use for informing about errors to the end-user
function LogService() {
  this.init = function(str) {
    if (devMode) {
      $("#console").show();
    }
    
    logService.logg("Starting OS2faktor");
  }

  this.logg = function(str) {
    var oldValue = $('#console').val();
    if (oldValue != null && oldValue.length > 2000) {
      oldValue = oldValue.substring(oldValue.length - 2000);
    }

    $('#console').val(oldValue + str + '\n');
  }
}


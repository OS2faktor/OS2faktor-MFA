function SelfServiceService() {
  var browserWindow;

  this.openWindow = function(handler) {
    var deviceIdValue = dbService.getValue('deviceId');
    var apiKeyValue = dbService.getValue('apiKey');

    var browserUrl = frontendUrl + '/ui/selfservice?' + $.param({
      deviceId: deviceIdValue,
      apiKey: apiKeyValue
    });

    // https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-inappbrowser/
    browserWindow = cordova.InAppBrowser.open(browserUrl, '_blank', 'clearsessioncache=yes,clearcache=yes,location=no,toolbar=no,disallowoverscroll=yes,zoom=no,useWideViewPort=no,footer=no');

    browserWindow.addEventListener('loadstop', function() {
      if (getDeviceType() != "IOS") {
        navigator.notification.activityStop();
      }
    });

    if (getDeviceType() != "IOS") {
      navigator.notification.activityStart("Vent venligst", "Indlæser...");

      var spinner ="<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,height=device-height,initial-scale=1'><style>.loader {position: absolute;    margin-left: -2em;    left: 50%;    top: 50%;    margin-top: -2em;    border: 5px solid #f3f3f3;    border-radius: 50%;    border-top: 5px solid #3498db;    width: 50px;    height: 50px;    -webkit-animation: spin 1.5s linear infinite;    animation: spin 1.5s linear infinite;}@-webkit-keyframes spin {  0% { -webkit-transform: rotate(0deg); } 100% { -webkit-transform: rotate(360deg); }}@keyframes spin {  0% { transform: rotate(0deg); }  100% { transform:rotate(360deg); }}</style></head><body><div class='loader'></div></body></html>";

      browserWindow.executeScript({code: "(function() { document.write(\"" + spinner + "\"); window.location.href='" + browserWindow + "';})()"});
    }

    browserWindow.addEventListener('loaderror', function (params) {
      closeInAppWindow(browserWindow);
      browserWindow = undefined;
      window.location = "error.html";
    });

    $(browserWindow).on('loadstart', function (e) {
      var url = new URL(e.originalEvent.url);
      var urlParams = new URLSearchParams(url.search);      

      var closeWindow = urlParams.get('closeWindow');

      if (closeWindow) {
        closeInAppWindow(browserWindow);
        handler();
      }
    });
  }
}


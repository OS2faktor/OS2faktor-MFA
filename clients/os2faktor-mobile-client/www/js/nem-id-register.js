var nemIdWindow;

var nemId = {
    register: function (options) {
        var deferred = $.Deferred();

        var nemIdUrl = frontendUrl + '/ui/register2/nemid?' + $.param({
            deviceId: options.deviceId,
            apiKey: options.apiKey
        });

        //https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-inappbrowser/

        nemIdWindow = cordova.InAppBrowser.open(nemIdUrl, '_blank', 'clearsessioncache=yes,clearcache=yes,location=no,toolbar=yes,closebuttoncaption=done,zoom=no,useWideViewPort=no');
        nemIdWindow.addEventListener('loadstop', function(){ if (getDeviceType() != "IOS") { navigator.notification.activityStop(); } });
        if (getDeviceType() != "IOS") {
            navigator.notification.activityStart("Vent venligst", "Indlæser...");
        }
        
        if (getDeviceType() != "IOS") {
            //spinner html
            var spinner ="<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,height=device-height,initial-scale=1'><style>.loader {position: absolute;    margin-left: -2em;    left: 50%;    top: 50%;    margin-top: -2em;    border: 5px solid #f3f3f3;    border-radius: 50%;    border-top: 5px solid #3498db;    width: 50px;    height: 50px;    -webkit-animation: spin 1.5s linear infinite;    animation: spin 1.5s linear infinite;}@-webkit-keyframes spin {  0% { -webkit-transform: rotate(0deg); } 100% { -webkit-transform: rotate(360deg); }}@keyframes spin {  0% { transform: rotate(0deg); }  100% { transform:rotate(360deg); }}</style></head><body><div class='loader'></div></body></html>";

            //intended webpage is loaded here
            nemIdWindow.executeScript({code: "(function() {document.write(\""+spinner+"\");window.location.href='"+nemIdUrl+"';})()"});
        }

        //TODO we may not need that or we should redo 
        nemIdWindow.addEventListener('loaderror', function (params) {
            nemIdWindow.close();
            nemIdWindow = undefined;
            window.location = "myErrorPage.html";

        });

        $(nemIdWindow).on('loadstart', function (e) {
            var url = new URL(e.originalEvent.url);
            var urlParams = new URLSearchParams(url.search);

            var status = urlParams.get('status');
            var closeWindow = urlParams.get('closeWindow');
            
            //logg("ApiKey:" + apiKey[1]);
            //logg("deviceId:" + deviceId[2]);
            
            if (status !== null) {
                if (status) {
                    deferred.resolve(status);
                } else {
                    deferred.reject();
                }
            }

            //Just close the window (e.g. after success page)
            if (closeWindow) {
                closeInAppWindow(nemIdWindow);
                deferred.resolve();
            }
        });

        return deferred.promise();
    }
};

function registerNemID() {
    $('#navbarSupportedContent').collapse('hide');
    isset('isNemIdRegistered').then((isset) => {
        if (!isset) {
            //Pause the challange scanner so that we can display registration window without interference
            isPaused = true;
            getValue('apiKey').then((apiKey) => {
                getValue('deviceId').then((deviceId) => {
                    var promise = nemId.register({
                        deviceId: deviceId,
                        apiKey: apiKey
                    });
                    promise.done(function (isNemIdRegistered) {
                        if(isNemIdRegistered){
                            setValue('isNemIdRegistered', isNemIdRegistered).then(() => {
                                isPaused = false;

                                $("#nemIdRegisterMenuItem").hide();
                                $("#nemIdPanel").hide();
                            });
                        }
                        
                    });
                    promise.catch(() => {
                        isPaused = false;
                        window.location = "myErrorPage.html";
                    });
                });
            });

        } else {
            $("#nemIdRegisterMenuItem").hide();
            $("#nemIdPanel").hide();
        }
    });
}

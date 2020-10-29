var pinWindow;

var pin = {
    register: function (options) {
        var deferred = $.Deferred();

        var pinRegUrl = frontendUrl + '/ui/pin/register?' + $.param({
            deviceId: options.deviceId,
            apiKey: options.apiKey
        });

        //https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-inappbrowser/

        pinWindow = cordova.InAppBrowser.open(pinRegUrl, '_blank', 'clearsessioncache=yes,clearcache=yes,location=no,toolbar=yes,closebuttoncaption=done,zoom=no,useWideViewPort=no');
        pinWindow.addEventListener('loadstop', function(){ if (getDeviceType() != "IOS") { navigator.notification.activityStop(); } });
        if (getDeviceType() != "IOS") {
            navigator.notification.activityStart("Vent venligst", "Indlæser...");
        }
        
        if (getDeviceType() != "IOS") {
            //spinner html
            var spinner ="<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,height=device-height,initial-scale=1'><style>.loader {position: absolute;    margin-left: -2em;    left: 50%;    top: 50%;    margin-top: -2em;    border: 5px solid #f3f3f3;    border-radius: 50%;    border-top: 5px solid #3498db;    width: 50px;    height: 50px;    -webkit-animation: spin 1.5s linear infinite;    animation: spin 1.5s linear infinite;}@-webkit-keyframes spin {  0% { -webkit-transform: rotate(0deg); } 100% { -webkit-transform: rotate(360deg); }}@keyframes spin {  0% { transform: rotate(0deg); }  100% { transform:rotate(360deg); }}</style></head><body><div class='loader'></div></body></html>";

            //intended webpage is loaded here
            pinWindow.executeScript({code: "(function() {document.write(\""+spinner+"\");window.location.href='"+pinRegUrl+"';})()"});
        }

        //TODO we may not need that or we should redo 
        pinWindow.addEventListener('loaderror', function (params) {
            pinWindow.close();
            pinWindow = undefined;
            deferred.reject();
        });

        $(pinWindow).on('loadstart', function (e) {
            var url = new URL(e.originalEvent.url);
            var urlParams = new URLSearchParams(url.search);

            var status = urlParams.get('status');
            var closeWindow = urlParams.get('closeWindow');

            if (status !== null) {
                if (status) {
                    deferred.resolve(status);
                } else {
                    deferred.reject();
                }
            }

            //Just close the window (e.g. after success page)
            if (closeWindow) {
                closeInAppWindow(pinWindow);
                deferred.resolve();
            }
        });

        return deferred.promise();
    }
};

function registerPinCode() {
    $('#navbarSupportedContent').collapse('hide');
    isset('isPinCodeRegistered').then((isset) => {
        if (!isset) {
            //Pause the challange scanner so that we can display registration window without interference
            isPaused = true;

            getValue('apiKey').then((apiKey) => {
                getValue('deviceId').then((deviceId) => {
                    var promise = pin.register({
                        deviceId: deviceId,
                        apiKey: apiKey
                    });
                    promise.done(function (isPinCodeRegistered) {
                        if(isPinCodeRegistered){
                            setValue('isPinCodeRegistered', isPinCodeRegistered).then(() => {
                                isPaused = false;

                                $("#pinCodeRegisterMenuItem").hide();
                            });
                        }
                        
                    });
                    promise.catch(() => {
                        isPaused = false;
                        //window.location = "myErrorPage.html";
                    });
                });
            });

        } else {
            $("#pinCodeRegisterMenuItem").hide();
        }
    });
}

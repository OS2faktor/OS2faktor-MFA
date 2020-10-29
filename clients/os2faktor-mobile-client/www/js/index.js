var db;
var tableName = "settings";
var frontendUrl = "https://frontend.os2faktor.dk"; // without slash at the end
var backendUrl = "https://backend.os2faktor.dk";
//var frontendUrl = "http://192.168.1.136:9121";
//var backendUrl = "https://192.168.1.136:9088";
var isPaused = false;
var defaultFooterText = "Denne enhed er ikke registreret";
var authWindow;
var clientVersion = "1.3.4";
var devMode = false;


const swalWithBootstrapButtons = swal.mixin({
    confirmButtonClass: 'btn',
    cancelButtonClass: 'btn btn-danger',
    buttonsStyling: false,
});

// code for dealing with the app running in the background (pausing it)
document.addEventListener("pause", onPause, false);
document.addEventListener("resign", onPause, false); // handles IOS when app in foreground but user locks the screen

function onPause() {
    isPaused = true;
}

document.addEventListener("resume", onResume, false);

function onResume() {
    setTimeout(function () {
        swal.close();
        isPaused = false;
    }, 0);
}

// end of code for dealing with the app running in the background

document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
    //Setup ajax so that all our calls will contain clientVersion
    $.ajaxSetup({
        headers: { 'clientVersion': clientVersion }
    });

    if (devMode) {
        $("#console").show();
    }

    logg("Starting OS2faktor");

    StatusBar.overlaysWebView(false);

    // initialize the database if needed
    if (window.cordova.platformId === 'browser') {
        db = window.openDatabase('os2f', '1.0', 'Data', 2 * 1024 * 1024);
    }
    else {
        db = window.sqlitePlugin.openDatabase({
            name: 'os2f',
            location: 'default',
            androidDatabaseImplementation: 2
        });
    }

    // initialize push notification if needed
    var push = PushNotification.init({
        android: {
            // Add your Google Mobile App SenderId here
            senderID: 853838672150
        },
        ios: {
            alert: "true",
            badge: "true",
            sound: "true"
        }
    });

    push.on('registration', function (data) {
        initDB().then(() => {
            setValue('regId', data.registrationId);
        });
    });

    push.on('notification', function (data) {
        isset('apiKey').then((isset) => {
            if (isset) {
                isPaused = false;
            }
        });
    });

    push.on('error', function (err) {
        logg('Event=error, message=' + err.message);
    });

    initDB().then(() => {
        isset('apiKey').then((issetApiKey) => {
            if (!issetApiKey) {
                $('#registerPanel').show();
                $('#awaitPanel').hide();
                $("#nemIdRegisterMenuItem").hide();
                $("#nemIdPanel").hide();
                $("#pinCodeRegisterMenuItem").hide();
                $("#selfServiceMenuItem").hide();
            } else {
                getValue('apiKey').then((apiKey) => {
                    $('#registerPanel').hide();
                    $('#awaitPanel').show();
                });

                getValue('isNemIdRegistered').then((isNemIdRegistered) => {
                    if (isNemIdRegistered) {
                        $("#nemIdRegisterMenuItem").hide();
                        $("#nemIdPanel").hide();
                    } else {
                        $("#nemIdRegisterMenuItem").show();
                        $("#nemIdPanel").show();
                    }
                });

                getValue('isPinCodeRegistered').then((isPinCodeRegistered) => {
                    if (isPinCodeRegistered) {
                        $('#pinCodeRegisterMenuItem').hide();
                    } else {
                        $('#pinCodeRegisterMenuItem').show();
                    }
                });
            }
        });

        isset('deviceId').then((issetDeviceId) => {
            if (issetDeviceId) {
                getValue('deviceId').then((deviceId) => {
                    $("#footer").text("OS2faktor ID: " + deviceId);
                });
            } else {
                $("#footer").text(defaultFooterText);
            }
        });
    });

    setUpChallengeScanner();
}

function initDB() {
    return new Promise((resolve, reject) => {
        db.transaction(function (tx) {
            tx.executeSql('CREATE TABLE IF NOT EXISTS ' + tableName + ' (key unique, value)');
        }, function (error) {
            logg("failed to create database");
            reject();
        }, function () {
            resolve();
        });
    });
}

function setValue(key, value) {
    return new Promise((resolve, reject) => {
        if (db == null) {
            navigator.notification.confirm(
                'Database not initialized.',
                function () { navigator.app.exitApp(); },
                'Critical Error',
                ['OK']
            );
        }

        isset(key).then((isset) => {
            if (isset) {
                db.transaction(function (tx) {
                    var query = 'UPDATE ' + tableName + ' SET value = ?1 WHERE key = ?2';

                    tx.executeSql(query, [value, key], function (tx, rs) {
                        resolve();
                    }, function (tx, error) {
                        reject();
                    });
                }, function (error) {
                    reject();
                });
            } else {
                db.transaction(function (tx) {
                    var query = 'INSERT INTO ' + tableName + ' VALUES (?1,?2)';
                    tx.executeSql(query, [key, value], function (tx, rs) {
                        resolve();
                    }, function (tx, error) {
                        reject();
                    });
                }, function (error) {
                    reject();
                });
            }
        }, reason => {
            reject();
        });
    });
}

function getValue(key) {
    return new Promise(function (resolve, reject) {
        if (db == null) {
            navigator.notification.confirm(
                'Database not initialized.',
                function () { navigator.app.exitApp(); },
                'Critical Error',
                ['OK']
            );
        }

        db.transaction(function (tx) {
            var query = 'SELECT * FROM ' + tableName + ' WHERE key = ?';
            tx.executeSql(query, [key], function (tx, rs) {
                var result = rs.rows.item(0);
                if(!result){
                    resolve("");
                }else{
                    resolve(result.value);
                }
            }, function (tx, error) {
                logg(error);
                reject();
            });
        }, function (error) {
            logg(error);
            reject();
        });
    });
}

function deleteValue(key) {
    return new Promise(function (resolve, reject) {
        if (db == null) {
            navigator.notification.confirm(
                'Database not initialized.',
                function () { navigator.app.exitApp(); },
                'Critical Error',
                ['OK']
            );
        }

        db.transaction(function (tx) {
            var query = 'DELETE FROM ' + tableName + ' WHERE key = ?';
            tx.executeSql(query, [key], function (tx, rs) {
                resolve();
            }, function (tx, error) {
                reject();
            });
        }, function (error) {
            reject();
        });
    });
}

function isset(key) {
    return new Promise(function (resolve, reject) {
        db.transaction(function (tx) {
            tx.executeSql('SELECT count(*) AS mycount FROM ' + tableName + " WHERE key=?", [key], function (tx, rs) {
                if (rs.rows.item(0).mycount == 1) {
                    resolve(true);
                } else {
                    resolve(false);
                }
            }, function (tx, error) {
                reject();
            });
        }, function (error) {
            logg('transaction error: ' + error.message);
        });
    });
}

var registration = {
    register: function (options) {
        var deferred = $.Deferred();

        var authUrl = frontendUrl + '/ui/register2?' + $.param({
            token: options.token,
            type: options.type
        });

        //https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-inappbrowser/

        authWindow = cordova.InAppBrowser.open(authUrl, '_blank', 'clearsessioncache=yes,clearcache=yes,location=no,toolbar=yes,closebuttoncaption=done,zoom=no,useWideViewPort=no');
        authWindow.addEventListener('loadstop', function () { if (getDeviceType() != "IOS") { navigator.notification.activityStop(); } });
        if (getDeviceType() != "IOS") {
            navigator.notification.activityStart("Vent venligst", "Indlæser...");
        }

        if (getDeviceType() != "IOS") {
            //spinner html
            var spinner = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,height=device-height,initial-scale=1'><style>.loader {position: absolute;    margin-left: -2em;    left: 50%;    top: 50%;    margin-top: -2em;    border: 5px solid #f3f3f3;    border-radius: 50%;    border-top: 5px solid #3498db;    width: 50px;    height: 50px;    -webkit-animation: spin 1.5s linear infinite;    animation: spin 1.5s linear infinite;}@-webkit-keyframes spin {  0% { -webkit-transform: rotate(0deg); } 100% { -webkit-transform: rotate(360deg); }}@keyframes spin {  0% { transform: rotate(0deg); }  100% { transform:rotate(360deg); }}</style></head><body><div class='loader'></div></body></html>";

            //intended webpage is loaded here
            authWindow.executeScript({ code: "(function() {document.write(\"" + spinner + "\");window.location.href='" + authUrl + "';})()" });
        }

        authWindow.addEventListener('loaderror', function (params) {
            logg("Load error: "+ JSON.stringify(params));
            authWindow.close();
            authWindow = undefined;
            deferred.reject();
        });

        $(authWindow).on('loadstart', function (e) {
            var url = new URL(e.originalEvent.url);
            var urlParams = new URLSearchParams(url.search);

            var apiKey = urlParams.get('apiKey');
            var deviceId = urlParams.get('deviceId');
            var closeWindow = urlParams.get('closeWindow');
			var status = urlParams.get('status');
			
            // If we got data to finish registration
            if (deviceId !== null && apiKey !== null) {

				isset('apiKey').then((isset) => {
					if (!isset) {
						setValue('deviceId', deviceId).then(() => {
							$("#footer").text("OS2faktor ID: " + deviceId);
						});

						setValue('apiKey', apiKey).then(() => {
							isPaused = false;

							$("#registerPanel").hide();
							$("#awaitPanel").show();
							$("#nemIdRegisterMenuItem").show();
							$("#nemIdPanel").show();
							$("#selfServiceMenuItem").show();
							$('#pinCodeRegisterMenuItem').show();
						});
					}
				});
            }

			if (status === "true") {
				$("#nemIdRegisterMenuItem").hide();
				$("#nemIdPanel").hide();
            }

            if (closeWindow) {
                closeInAppWindow(authWindow);
                deferred.resolve();
            }			
        });

        return deferred.promise();
    }
};

function closeInAppWindow(window) {
    if (window != null) {
        if (getDeviceType() != "IOS") {
            navigator.notification.activityStop();
        }
        window.close();
    }
}

function register() {
    $('#navbarSupportedContent').collapse('hide');
    isset('apiKey').then((isset) => {
        if (!isset) {
            getValue('regId').then((registrationId) => {
                logg("xxx");
                var promise = registration.register({
                    token: registrationId,
                    type: getDeviceType()
                });
                promise.done(function () {

                });
                promise.catch((err) => {
                    window.location = "myErrorPage.html";
                });
            });
        } else {
            $('#registerPanel').hide();
            $('#awaitPanel').show();
        }
    });
	
}

function reset() {
    swalWithBootstrapButtons({
        title: '',
        html: 'Er du sikker på at du vil nulstille klienten?',
        type: null,
        showCancelButton: true,
        reverseButtons: true,
        confirmButtonText: 'Ja',
        cancelButtonText: 'Nej',
        allowOutsideClick: false
    }).then((result) => {
        if (result.value) {
            //TODO if getValue apiKey fails none of the reset code will be executed. maybe we should modify this code.
            getValue('apiKey').then((valueApiKey) => {
                getValue('deviceId').then((valueDeviceId) => {
                    isset('apiKey').then((isset) => {
                        if (isset) {
                            deleteValue('apiKey');
                        }
                
                        $('#registerPanel').show();
                        $('#awaitPanel').hide();
                        $("#selfServiceMenuItem").hide();
                    });
                
                    isset('deviceId').then((isset) => {
                        if (isset) {
                            deleteValue('deviceId');
                        }
                
                        $("#footer").text(defaultFooterText);
                    });
                
                    isset('isNemIdRegistered').then((isset) => {
                        if (isset) {
                            deleteValue('isNemIdRegistered');
                        }
                
                        $('#nemIdRegisterMenuItem').hide();
                        $('#nemIdPanel').hide();
                    });

                    isset('isPinCodeRegistered').then((isset) => {
                        if (isset) {
                            deleteValue('isPinCodeRegistered');
                        }
                
                        $('#pinCodeRegisterMenuItem').hide();
                    });
        
                    //Notify backend about client removal
                    $.ajax({
                        headers:{ 'ApiKey': valueApiKey , 'deviceId': valueDeviceId },
                        url: backendUrl + "/api/client",
                        type: 'DELETE'
                    });
                });
            });
        }
    });
    $('#navbarSupportedContent').collapse('hide');
}

function getDeviceType() {
    return device.platform.toUpperCase();
}

function logg(str) {
    if (devMode) {
        var field = document.getElementById('console');
		
        var stack = new Error().stack;
        if (typeof field.value === 'undefined') {
            field.value = "";
        }
        field.value += str + "\n" + stack + "\n---\n";
    }
}

function setUpChallengeScanner() {
    setInterval(function () {
        if (!isPaused) {
            getValue('apiKey').then((valueApiKey) => {
                getValue('deviceId').then((valueDeviceId) => {
                    isset('isPinCodeRegistered').then((issetPinCode) => {
                        if (!issetPinCode) {
                            pollChallenge(valueApiKey, valueDeviceId, false);
                        } else {
                            getValue('isPinCodeRegistered').then((pinRegistered) => {
                                pollChallenge(valueApiKey, valueDeviceId, pinRegistered);
                            });
                        }
                    });
                });
            });
        }
    }, 2000);
}

function pollChallenge(valueApiKey, valueDeviceId, pinRegistered) {
    $.ajax({
        headers: { 'ApiKey': valueApiKey, 'deviceId': valueDeviceId },
        url: backendUrl + "/api/client",
        success: function (data, textStatus, xhr) {
            if (data !== "") {
                isPaused = true;
                showChallenge(data, valueApiKey, valueDeviceId, pinRegistered, false);
            }
        },
        error: function(xhr, status, error){
            //We should keep it for debug and ignore 404 - which means not challanges
            //logg(JSON.stringify(xhr));
        }
    }); 
}

function showChallenge(data, valueApiKey, valueDeviceId, pinRegistered, responseJSON) {
    var wrongPinCode = false;
    if (responseJSON.status == "WRONG_PIN") {
        wrongPinCode = true;
    }
    if (responseJSON.status == "LOCKED") {
        Swal.fire({
            title: 'Din klient er låst!',
            html: 'Låst indtil: <span style="color:red;">' + responseJSON.lockedUntil + '</span>',
            type: 'error'
        });
        return;
    }

    swalWithBootstrapButtons({
        title: data.serverName,
        html: 
            'Vil du godkende nedenstående kontrolkode?<h2 class="os2faktorcode">' + data.challenge + '</h2>' +
            ((wrongPinCode) ? '<p style="color:red;">Forkert pinkode!</p>' : '')  +
            ((pinRegistered) ? '<p>Indtast PIN-kode</p>' + '<input id="swal-input-pin" class="form-control" type="number" style="-webkit-text-security:disc;" maxlength="4" placeholder="PIN" oninput="javascript: if (this.value.length > this.maxLength) this.value = this.value.slice(0, this.maxLength);" required="required" oninvalid="this.setCustomValidity(\'Invalid PIN code\')" autofocus="autofocus">' : ''),
        type: null,
        showCancelButton: true,
        reverseButtons: true,
        confirmButtonText: 'Godkend',
        cancelButtonText: 'Afvis',
        allowOutsideClick: false,
        preConfirm: () => {
            if (pinRegistered) {
                return [
                    document.getElementById('swal-input-pin').value,
                ]
            }
        }
    }).then((result) => {
        isPaused = true;
        if (result.value) {
            $.ajax({
                headers: { 'ApiKey': valueApiKey, 'deviceId': valueDeviceId, 'pinCode': ((pinRegistered) ? result.value[0] : null) },
                url: backendUrl + "/api/client/" + data.uuid + "/accept",
                type: 'PUT',
                success: function () {
                    isPaused = false;
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    showChallenge(data,valueApiKey,valueDeviceId,pinRegistered,jqXHR.responseJSON);
                }
            });
        }
        else if (result.dismiss === swal.DismissReason.cancel || result.dismiss === swal.DismissReason.close || result.dismiss === swal.DismissReason.esc) {
            $.ajax({
                headers: { 'ApiKey': valueApiKey, 'deviceId': valueDeviceId },
                url: backendUrl + "/api/client/" + data.uuid + "/reject",
                type: 'PUT',
                success: function () {
                    isPaused = false;
                },
                error: function () {
                    isPaused = false;
                }
            });
        }
    });
}


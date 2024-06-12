var backendUrl;
var roaming;
var clientVersion = "2.3.0";

$(document).ready(function() {

    // read global settings and perform initialization
    chrome.storage.managed.get(["BackendUrl", "FrontendUrl", "Roaming"], function(policy) {
        if (policy.BackendUrl) {
            backendUrl = policy.BackendUrl;
        }
        else {
            backendUrl = "https://backend.os2faktor.dk";
        }

        if (policy.FrontendUrl) {
            frontendUrl = policy.FrontendUrl;
        }
        else {
            frontendUrl = "https://frontend.os2faktor.dk";
        }

        if (policy.Roaming) {
            roaming = policy.Roaming;
        }
        else {
            roaming = false;
        }
    });

    // Setup ajax so that all our calls will contain clientVersion
    $.ajaxSetup({
        headers: { 'clientVersion': clientVersion }
    });

    var inputPin = document.getElementById("inputPin");
    var inputConfirm = document.getElementById("inputConfirm");
    var style = window.getComputedStyle(inputPin);

    if (style.webkitTextSecurity) {
        ;
    }
    else {
        inputPin.setAttribute("type", "password");
        inputConfirm.setAttribute("type", "password");
    }

    // Add validation to device name field
    $('#registerNameBtn').prop('disabled', true);
    $('#inputName').on('oninvalid', function () {
        this.setCustomValidity('Angiv et navn på din klient');
    });
    $('#inputName').on('oninput', function () {
        this.setCustomValidity('');
    });

    $('#inputName').on('keyup', function (e) {
        var name = $('#inputName').val();
        if (!name || name.length < 2) {
            $('#registerNameBtn').prop('disabled', true);
            return;
        }

        $('#registerNameBtn').prop('disabled', false);

        if (e.key === 'Enter' || e.keyCode === 13) {
            moveToPinRegistration();
        }
    });

    $('#inputPin').on('keyup', function (e) {
        if (e.key === 'Enter' || e.keyCode === 13) {
            $('#registerPinBtn').click();
        }
    });

    $('#registerNameBtn').on('click', function () {
        var name = $('#inputName').val();

        moveToPinRegistration();
    });

    $('#registerPinBtn').on('click', function () {
        tryToRegister(function (result) {
            if (result.success == true ) {
                if (roaming) {
                    chrome.storage.sync.set({ apiKey: result.apiKey });
                    chrome.storage.sync.set({ deviceId: result.deviceId });
                    chrome.storage.sync.set({ pinRegistered: true });
                } else {
                    chrome.storage.local.set({ apiKey: result.apiKey });
                    chrome.storage.local.set({ deviceId: result.deviceId });
                    chrome.storage.local.set({ pinRegistered: true });
                }

                $('#registerPinErrorMsg').text('');
                $('#registerPinErrorMsg').hide();

                nemIdRegisterPopup();
            } else {
                if (result.invalidPin) {
                    $('#registerPinErrorMsg').text('Ugyldig pinkode. Pinkoden skal være 4 tal, og må ikke være for simpel');
                    $('#registerPinErrorMsg').show();
                }
                else {
                    $('#registerPinErrorMsg').text('Der opstod en teknisk fejl. Prøv igen.');
                    $('#registerPinErrorMsg').show();
                }

                console.log("Fejl under registering: " + JSON.stringify(result));
            }
        })
    });

    // focus on input field    
    $('#inputName').click().focus();
});


function moveToPinRegistration() {
    $('a[data-toggle="tab"][href="#pinRegisterStep"]').click();
    
    window.setTimeout(function() {
      $('#inputPin').click().focus();
    }, 250);
}

function tryToRegister(handler) {
    let searchParams = new URLSearchParams(window.location.search);
    var name = $('#inputName').val();
    var token = searchParams.get('token');
    var clientType = searchParams.get('type');
    var pin = $('#inputPin').val();

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
            'token': token
        }),
        success: function(data) {
            result.success = data.success;
            result.invalidPin = data.invalidPin;
            result.deviceId = data.deviceId;
            result.apiKey = data.apiKey;

            handler(result);
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.log("registerError: " + JSON.stringify(jqXHR));
            handler(result);
        }
    });
}

// NemId Registration
function performNemIdRegisterPopup(result) {
    var apiKey = result["apiKey"];
    var deviceId = result["deviceId"];

    if (apiKey && deviceId) {
        chrome.windows.create({
            url: frontendUrl + '/ui/register2/nemid?' + $.param({
                deviceId: deviceId,
                apiKey: apiKey
            }),
            type: "popup",
            state: "normal",
            focused: true,
            width: 800,
            height: 1000
        }, function (win) {
            window.close();

            chrome.runtime.getBackgroundPage(function(backgroundPage) {
                backgroundPage.runNemIdRegistrationMonitorTask(win.id);
                backgroundPage.closePopup();
            });
        });
    }
}

function nemIdRegisterPopup() {
    if (roaming) {
        chrome.storage.sync.get(["apiKey", "deviceId"], function (result) {
            performNemIdRegisterPopup(result);
        });
    }
    else {
        chrome.storage.local.get(["apiKey", "deviceId"], function (result) {
            performNemIdRegisterPopup(result);
        });
    }
}


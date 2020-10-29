var backendUrl;
var clientVersion = "1.5.0";

const swalWithBootstrapButtons = swal.mixin({
    confirmButtonClass: 'btn',
    cancelButtonClass: 'btn btn-danger',
    buttonsStyling: false,
});

(function () {
    chrome.storage.managed.get(["BackendUrl"], function(policy) {
		if (policy.BackendUrl) {
			backendUrl = policy.BackendUrl;
		}
		else {
			backendUrl = "https://backend.os2faktor.dk";
        }
    });

    chrome.storage.local.set({ isPaused: false });

    //Setup ajax so that all our calls will contain clientVersion
    $.ajaxSetup({
        headers: { 'clientVersion': clientVersion }
    });

    var foundChallenge = false;

    // check immediately
    setTimeout(checkForChallenges, 200);

    // display a waiting page if nothing happens for a while
    setTimeout(showWaiting, 1800);
    
    // re-check every 2 seconds
    setInterval(checkForChallenges, 2000);
    
    function showWaiting() {
    	if (!foundChallenge) {
    		$("#noChallenge").show();
    	}
    }

    function checkForChallenges() {
        chrome.storage.local.get(["registrationId", "apiKey", "deviceId", "pinRegistered", "isPaused"], function (result) {
            var isPaused = result["isPaused"];
            if (!isPaused) {
                var registrationId = result["registrationId"];
                var apiKey = result["apiKey"];
                var deviceId = result["deviceId"];
                var pinRegistered = result["pinRegistered"];

                if (registrationId && apiKey && deviceId) {
                    $.ajax({
                        headers: { 'ApiKey': apiKey, 'deviceId': deviceId },
                        url: backendUrl + "/api/client",
                        success: function (data, textStatus, xhr) {
                            if (data !== "") {
                                chrome.storage.local.set({ isPaused: true });
                                foundChallenge = true;
                                
                                showChallenge(data,apiKey,deviceId,pinRegistered,false);
                            }
                        },
                        error: function() {
                            chrome.storage.local.set({ isPaused: false });
                        }
                    });
                }
            }
        });
    }

    function showChallenge(data, apiKey, deviceId, pinRegistered, responseJSON){
        var wrongPinCode = false;
        if (responseJSON.status == "WRONG_PIN") {
            wrongPinCode = true;
        }
        if (responseJSON.status == "LOCKED") {
            Swal.fire({
                title: 'Din klient at låst!',
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
            onOpen: (model) => {
                window.resizeTo(512,$(model).height()+74+(window.outerHeight - window.innerHeight));
            },
            preConfirm: () => {
                if (pinRegistered) {
                    return [
                        document.getElementById('swal-input-pin').value,
                    ]
                }
            }
        }).then((result) => {
            if (result.value) {
                $.ajax({
                    headers: { 'ApiKey': apiKey, 'deviceId': deviceId, 'pinCode': ((pinRegistered) ? result.value[0] : null) },
                    url: backendUrl + "/api/client/" + data.uuid + "/accept",
                    type: 'PUT',
                    success: function () {
                        chrome.runtime.getBackgroundPage(function(backgroundPage) {
                            backgroundPage.removeBadge();
                        });
                        window.close();
                    },
                    error: function (jqXHR, textStatus, errorThrown) {
                        //chrome.storage.local.set({ isPaused: false });
                        
                        showChallenge(data,apiKey,deviceId,pinRegistered,jqXHR.responseJSON);
                        
                    }
                });
            } else if (result.dismiss === swal.DismissReason.cancel || result.dismiss === swal.DismissReason.close || result.dismiss === swal.DismissReason.esc) {
                $.ajax({
                    headers: { 'ApiKey': apiKey, 'deviceId': deviceId },
                    url: backendUrl + "/api/client/" + data.uuid + "/reject",
                    type: 'PUT',
                    success: function () {
                        chrome.runtime.getBackgroundPage(function(backgroundPage) {
                            backgroundPage.removeBadge();
                        });
                        window.close();
                    },
                    error: function(){
                        chrome.storage.local.set({ isPaused: false });
                    }
                });
            }
        });
    }

})();

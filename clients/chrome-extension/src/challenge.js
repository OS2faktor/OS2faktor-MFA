var backendUrl;
var roaming;
var clientVersion = "2.5.0";

const swalWithBootstrapButtons = swal.mixin({
    confirmButtonClass: 'btn',
    cancelButtonClass: 'btn btn-danger',
    buttonsStyling: false,
});

(function () {
	var foundChallenge = false;

	// five minutes timeout for popup window
	setTimeout(() => {
		window.close();
	}, 5 * 60 * 1000);

	function showWaiting() {
		if (!foundChallenge) {
			$("#noChallenge").show();
		}
	}

	function checkForChallenges(apiKey, deviceId, pinRegistered) {
		chrome.storage.local.get(["isPaused"], function (result) {
			var isPaused = result["isPaused"];

			if (!isPaused) {
				if (apiKey && deviceId) {
					$.ajax({
						headers: { 'ApiKey': apiKey, 'deviceId': deviceId },
						url: backendUrl + "/api/client",
						success: function (data, textStatus, xhr) {
							if (data !== "") {
								chrome.storage.local.set({ isPaused: true });
								foundChallenge = true;

								showChallenge(data, apiKey, deviceId, pinRegistered, false);
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

	function showChallenge(data, apiKey, deviceId, pinRegistered, responseJSON) {
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
			title: 'Godkend login forespørgsel',
			html: 'Der er ankommet en login forespørgsel ' + ((data.tts != null) ? data.tts : '') + ' fra <h4 style="margin-top: 15px; margin-bottom: 15px;">' + data.serverName + '</h4>' + ((data.challenge != null && data.challenge.length > 0)
                           ? 'Vil du godkende nedenstående kontrolkode og gennemføre login?<h2 style="margin-top: 15px; margin-bottom: 15px;">' + data.challenge + '</h2>'
                           : 'Ønsker du at godkende denne login forespørgsel?<br/><br/>'
                         ) + ((wrongPinCode) ? '<p style="color:red;">Forkert pinkode!</p>' : '') +
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
					return [ document.getElementById('swal-input-pin').value ]
				}
			}
		}).then((result) => {
			if (result.value) {
				$.ajax({
					headers: { 'ApiKey': apiKey, 'deviceId': deviceId, 'pinCode': ((pinRegistered) ? result.value[0] : null), 'roaming': roaming},
					url: backendUrl + "/api/client/" + data.uuid + "/accept",
					type: 'PUT',
					success: function () {
						window.close();
					},
					error: function (jqXHR, textStatus, errorThrown) {
						//chrome.storage.local.set({ isPaused: false });
						showChallenge(data, apiKey, deviceId, pinRegistered, jqXHR.responseJSON);
					}
				});
			}
			else if (result.dismiss === swal.DismissReason.cancel || result.dismiss === swal.DismissReason.close || result.dismiss === swal.DismissReason.esc) {
				$.ajax({
					headers: { 'ApiKey': apiKey, 'deviceId': deviceId },
					url: backendUrl + "/api/client/" + data.uuid + "/reject",
					type: 'PUT',
					success: function () {
						window.close();
					},
					error: function(){
						chrome.storage.local.set({ isPaused: false });
					}
				});
			}
		});

		setTimeout(function() {
			$('#swal-input-pin').focus();

			$("#swal-input-pin").on('keyup', function (e) {
				if (e.key === 'Enter' || e.keyCode === 13) {
					$(".swal2-confirm").click();
				}
			});
		}, 200);
	}

	function initializePage(result) {
		var apiKey = result["apiKey"];
		var deviceId = result["deviceId"];
		var pinRegistered = result["pinRegistered"];

		// check immediately
		setTimeout(function() {
			checkForChallenges(apiKey, deviceId, pinRegistered);
		}, 200);

		// re-check every 2 seconds
		setInterval(function() {
			checkForChallenges(apiKey, deviceId, pinRegistered);
		}, 2000);

		// display a waiting page if nothing happens for a while
		setTimeout(showWaiting, 1800);
	}

	// read global settings and perform initialization
	chrome.storage.managed.get(["BackendUrl", "Roaming"], function(policy) {
		if (policy.BackendUrl) {
			backendUrl = policy.BackendUrl;
		}
		else {
			backendUrl = "https://backend.os2faktor.dk";
		}

		if (policy.Roaming) {
			roaming = policy.Roaming;
		}
		else {
			roaming = false;
		}

		// Setup ajax so that all our calls will contain clientVersion
		$.ajaxSetup({
			headers: { 'clientVersion': clientVersion }
		});

		// turn off any residual pause flags
		chrome.storage.local.set({ isPaused: false });

		if (roaming) {
			chrome.storage.sync.get(["apiKey", "deviceId", "pinRegistered"], function (result) {
				initializePage(result);
			});
		} else {
			chrome.storage.local.get(["apiKey", "deviceId", "pinRegistered"], function (result) {
				initializePage(result);
			});
		}
	});
})();

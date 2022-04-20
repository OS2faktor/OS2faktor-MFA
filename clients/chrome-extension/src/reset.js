const swalWithBootstrapButtons = swal.mixin({
    confirmButtonClass: 'btn',
    cancelButtonClass: 'btn btn-danger',
    buttonsStyling: false,
});
var roaming;

showReset();

function showReset() {

	chrome.storage.managed.get("Roaming", function(policy) {
		if (policy.Roaming) {
			roaming = policy.Roaming;
		}
		else {
		      roaming = false;
		}
	});
	
	var swalText = 'Er du sikker på at du vil nulstille din klient?';
	if (roaming) {
		swalText += ' Bemærk at 2-faktor klienten vil blive slettet fra alle de maskiner den er installeret på';
	}
	

	swalWithBootstrapButtons({
		title: 'Nulstil klient',
		text: swalText,
		type: 'warning',
		showCancelButton: true,
		reverseButtons: true,
		confirmButtonText: 'Ja',
		cancelButtonText: 'Nej',
		allowOutsideClick: false,
		onOpen: (model) => {
			window.resizeTo(512,$(model).height()+74+(window.outerHeight - window.innerHeight));
		},
	}).then((result) => {
		if (result.value) {
			if (roaming) {
				chrome.storage.sync.remove(["apiKey","deviceId","nemIdRegistered","pinRegistered"]);
			} else {
				chrome.storage.local.remove(["apiKey","deviceId","nemIdRegistered","pinRegistered"]);
			}

			chrome.runtime.getBackgroundPage(function(backgroundPage) {
				backgroundPage.closePopup();
			});
		}

		window.close();
	});
}

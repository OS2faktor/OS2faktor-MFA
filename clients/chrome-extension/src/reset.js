const swalWithBootstrapButtons = swal.mixin({
    confirmButtonClass: 'btn',
    cancelButtonClass: 'btn btn-danger',
    buttonsStyling: false,
});

showReset();

function showReset() {
	swalWithBootstrapButtons({
		title: 'Nulstil klient',
		text: 'Er du sikker på at du vil nulstille din klient?',
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
			chrome.storage.local.remove(["apiKey","deviceId","nemIdRegistered","pinRegistered"]);

			chrome.runtime.getBackgroundPage(function(backgroundPage) {
				backgroundPage.closePopup();
				backgroundPage.removeBadge();
			});
		}

		window.close();
	});
}

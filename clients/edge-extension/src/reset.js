const swalWithBootstrapButtons = swal.mixin({
    customClass: {
      confirmButton: "btn btn-primary",
      cancelButton: "btn btn-danger"
    }
});

var backendUrl;
showReset();

const getObjectFromLocalStorage = async function(key) {
	return new Promise((resolve, reject) => {
		try {
			chrome.storage.local.get(key, function(value) {
				resolve(value[key]);
			});
		} catch (ex) {
			reject(ex);
		}
	});
};

function showReset() {
	var backendUrl = "https://backend.os2faktor.dk";

	var swalText = 'Er du sikker på at du vil nulstille din klient?';

        swalWithBootstrapButtons.fire({
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
	}).then(async (result) => {
		if (result.value) {
			let apiKey;
			let deviceId;
			apiKey = await getObjectFromLocalStorage("apiKey");
			deviceId = await getObjectFromLocalStorage("deviceId");
			chrome.storage.local.remove(["apiKey", "deviceId", "nemIdRegistered", "pinRegistered"]);

			if (deviceId != null && apiKey != null) {
				await $.ajax({
					headers: {
						'ApiKey': apiKey,
						'DeviceId': deviceId
					},
					url: backendUrl + "/api/client",
					type: 'DELETE'
				});
			}
		}
		window.close();
	});
}

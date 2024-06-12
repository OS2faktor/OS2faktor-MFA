const swalWithBootstrapButtons = swal.mixin({
    confirmButtonClass: 'btn',
    cancelButtonClass: 'btn btn-danger',
    buttonsStyling: false,
});
var roaming;
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

const getObjectFromSyncStorage = async function(key) {
	return new Promise((resolve, reject) => {
		try {
			chrome.storage.sync.get(key, function(value) {
				resolve(value[key]);
			});
		} catch (ex) {
			reject(ex);
		}
	});
};

function showReset() {

	chrome.storage.managed.get(["Roaming", "BackendUrl"], function(policy) {
		if (policy.Roaming) {
			roaming = policy.Roaming;
		}
		else {
		      roaming = false;
		}
		if (policy.BackendUrl) {
			backendUrl = policy.BackendUrl;
		}
		else {
			backendUrl = "https://backend.os2faktor.dk";
		}
	});
	
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
	}).then(async (result) => {
		if (result.value) {
			let apiKey;
			let deviceId;
			if (roaming) {
				apiKey = await getObjectFromSyncStorage("apiKey");
				deviceId = await getObjectFromSyncStorage("deviceId");
				chrome.storage.sync.remove(["apiKey", "deviceId", "nemIdRegistered", "pinRegistered"]);
			} else {
				apiKey = await getObjectFromLocalStorage("apiKey");
				deviceId = await getObjectFromLocalStorage("deviceId");
				chrome.storage.local.remove(["apiKey", "deviceId", "nemIdRegistered", "pinRegistered"]);
			}

			if (apiKey != null && deviceId != null) {
				await $.ajax({
					headers: {
						'ApiKey': apiKey,
						'DeviceId': deviceId
					},
					url: backendUrl + "/api/client",
					type: 'DELETE'
				});
			}

			await chrome.runtime.getBackgroundPage(function(backgroundPage) {
				backgroundPage.closePopup();
			});
		}
		window.close();
	});
}

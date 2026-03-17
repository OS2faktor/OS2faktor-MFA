var BACKEND_URL = 'https://backend.os2faktor.dk';
var FRONTEND_URL = 'https://frontend.os2faktor.dk';

function storageGet(keys) {
    return new Promise(function(resolve) {
        chrome.storage.local.get(keys, resolve);
    });
}

function storageSet(items) {
    return new Promise(function(resolve) {
        chrome.storage.local.set(items, resolve);
    });
}

function storageRemove(keys) {
    return new Promise(function(resolve) {
        chrome.storage.local.remove(keys, resolve);
    });
}

// CONNECTION HANDLING

document.addEventListener('deviceready', networkEventHandlers, false);

function networkEventHandlers() {
    document.addEventListener("offline", onOffline, false);
    document.addEventListener("online", onOnline, false);
};

function onOffline() {
    if (window.location.href.indexOf("offline.html") < 0) {
        window.location = "offline.html";
    }
}

function onOnline() {
    if (window.location.href.indexOf("offline.html") >= 0) {
        window.location = "index.html";
    }
}

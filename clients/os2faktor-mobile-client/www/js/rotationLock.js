document.addEventListener('deviceready', setRotationLock, false);

function setRotationLock() {
    window.plugins.screensize.get(successCallback, errorCallback);
};

function successCallback(result) {
    if (result) {
        try {
            if (result.diameter < 7) {
                screen.orientation.lock('portrait');
            }
        }
        catch (error) {
            screen.orientation.lock('portrait');
        }
    }
    else {
        screen.orientation.lock('portrait');
    }
}

function errorCallback(result) {
    screen.orientation.lock('portrait');
}

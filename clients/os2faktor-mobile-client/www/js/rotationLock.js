document.addEventListener('deviceready', setRotationLock, false);

function setRotationLock() {
    screen.orientation.lock('portrait');
};

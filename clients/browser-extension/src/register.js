var clientVersion = "4.0.2";

document.addEventListener('DOMContentLoaded', function() {
    const inputPin = document.getElementById("inputPin");
    const inputName = document.getElementById("inputName");
    const registerNameBtn = document.getElementById("registerNameBtn");
    const registerPinBtn = document.getElementById("registerPinBtn");
    const registerPinErrorMsg = document.getElementById("registerPinErrorMsg");

    // check for webkit text security, otherwise use password type
    const style = window.getComputedStyle(inputPin);
    if (!style.webkitTextSecurity) {
        inputPin.setAttribute("type", "password");
    }

    // make sure register button is disabled until input validation passes
    registerNameBtn.disabled = true;

    inputName.addEventListener('invalid', function() {
        this.setCustomValidity('Angiv et navn på din klient');
    });

    inputName.addEventListener('input', function() {
        this.setCustomValidity('');
    });

    inputName.addEventListener('keyup', function(e) {
        const name = inputName.value;
        if (!name || name.length < 2) {
            registerNameBtn.disabled = true;
            return;
        }

        registerNameBtn.disabled = false;

        if (e.key === 'Enter') {
            moveToPinRegistration();
        }
    });

    inputPin.addEventListener('keyup', function(e) {
        if (e.key === 'Enter') {
            registerPinBtn.click();
        }
    });

    registerNameBtn.addEventListener('click', function() {
        moveToPinRegistration();
    });

    registerPinBtn.addEventListener('click', function() {
        tryToRegister(function(result) {
            if (result.success == true) {
                storageSet({ apiKey: result.apiKey, deviceId: result.deviceId, pinRegistered: true, pinLength: 6 });

                registerPinErrorMsg.textContent = '';
                registerPinErrorMsg.style.display = 'none';

                mitIDRegisterPopup();
            } else {
                if (result.invalidPin) {
                    registerPinErrorMsg.textContent = 'Ugyldig pinkode. Pinkoden skal være 6 tal, og må ikke være for simpel';
                    registerPinErrorMsg.style.display = 'block';
                } else {
                    registerPinErrorMsg.textContent = 'Der opstod en teknisk fejl. Prøv igen.';
                    registerPinErrorMsg.style.display = 'block';
                }

                console.log("Fejl under registering: " + JSON.stringify(result));
            }
        });
    });

    // add click handlers for tabs
    const tabLinks = document.querySelectorAll('a[data-toggle="tab"]');
    tabLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');
            switchTab(targetId);
        });
    });

    inputName.click();
    inputName.focus();
});

function switchTab(targetId) {
    const tabPanes = document.querySelectorAll('.tab-pane');
    tabPanes.forEach(pane => pane.classList.remove('active'));

    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => link.classList.remove('active'));

    const targetPane = document.querySelector(targetId);
    if (targetPane) {
        targetPane.classList.add('active');
    }

    const targetLink = document.querySelector(`a[href="${targetId}"]`);
    if (targetLink) {
        targetLink.classList.add('active');
    }
}

function moveToPinRegistration() {
    const tabLink = document.querySelector('a[data-toggle="tab"][href="#pinRegisterStep"]');
    if (tabLink) {
        tabLink.click();
    }

    setTimeout(function() {
        const inputPin = document.getElementById("inputPin");
        inputPin.click();
        inputPin.focus();
    }, 250);
}

function tryToRegister(handler) {
    const name = document.getElementById("inputName").value;
    const pin = document.getElementById("inputPin").value;
    const clientType = chrome.runtime.getManifest().browser_specific_settings?.client_type;

    const sanitizedName = sanitizeName(name);

    const result = {
        'success': false,
        'invalidPin': false,
        'deviceId': '',
        'apiKey': ''
    };

    fetch(BACKEND_URL + "/xapi/client/v2/register", {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'clientVersion': clientVersion
        },
        body: JSON.stringify({
            'name': sanitizedName,
            'pincode': pin,
            'type': clientType,
            'token': ''
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        result.success = data.success;
        result.invalidPin = data.invalidPin;
        result.deviceId = data.deviceId;
        result.apiKey = data.apiKey;
        handler(result);
    })
    .catch(error => {
        console.log("registerError: " + JSON.stringify(error));
        handler(result);
    });
}

function mitIDRegisterPopup() {
    storageGet(["apiKey", "deviceId"]).then(function(result) {
        performMitIDRegisterPopup(result);
    });
}

function performMitIDRegisterPopup(result) {
    const apiKey = result["apiKey"];
    const deviceId = result["deviceId"];

    if (apiKey && deviceId) {
        const params = new URLSearchParams({
            deviceId: deviceId,
            apiKey: apiKey
        });

        chrome.windows.create({
            url: FRONTEND_URL + '/ui/register2/nemid?' + params.toString(),
            type: "popup",
            state: "normal",
            focused: true,
            width: 800,
            height: 1000
        }, function(win) {
            chrome.runtime.sendMessage({
                runMitIdRegistrationMonitorTask: win.id
            }, function(response) {
                window.close();
            });
        });
    }
}

function sanitizeName(name) {
    return name.trim().replace(/[<>&"']/g, 'x');
}

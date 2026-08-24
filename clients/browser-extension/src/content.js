// content.js - Injected into every .dk page. Listens for the 'os2faktorEvent' custom event
// fired by the IdP to trigger the MFA flow, then fetches the pending challenge from the
// background script and shows the inline authentication modal.
(function() {
    'use strict';

    // only run on .dk domains
    if (!window.location.hostname.endsWith('.dk')) {
        return;
    }

    function handleChallenge(response) {
        chrome.storage.local.get(['pinLength'], function(stored) {
            const pinLength = stored.pinLength || 6;
            const modal = new OS2faktorModal();

            if (response.challenge && response.challenge !== '??') {
                // Visual approval flow (challenge code present)
                modal.showApprovalUI({
                    challenge: response.challenge,
                    name: response.serverName,
                    timestamp: 'Ankommet ' + response.tts,
                    uuid: response.uuid,
                    pinLength: pinLength,
                    onApprove: function(pin) {
                        chrome.runtime.sendMessage({
                            event: 'acceptChallenge',
                            uuid: response.uuid,
                            pin: pin,
                            passwordlessChallenge: null
                        }, function(response2) {
                            if (response2 && response2.locked) {
                                modal.showLockedError(response2.lockedUntil);
                            } else if (response2 && response2.wrongPin) {
                                modal.showPinError();
                            } else {
                                modal.close();
                            }
                        });
                    },
                    onReject: function() {
                        chrome.runtime.sendMessage({ event: 'rejectChallenge', uuid: response.uuid });
                        modal.close();
                    }
                });
            } else {
                // Passwordless flow
                modal.open({
                    name: response.serverName,
                    timestamp: 'Ankommet ' + response.tts,
                    uuid: response.uuid,
                    pinLength: pinLength,
                    onPinComplete: function(pin) {
                        chrome.runtime.sendMessage({ event: 'validatePin', pin: pin }, function(pinResult) {
                            if (pinResult && pinResult.status === 'LOCKED') {
                                modal.showLockedError(pinResult.lockedUntil);
                            } else if (!pinResult || pinResult.status !== 'OK') {
                                modal.showPinError();
                            } else {
                                modal.showPasswordlessEntry(function(code) {
                                    chrome.runtime.sendMessage({
                                        event: 'acceptChallenge',
                                        uuid: response.uuid,
                                        pin: pin,
                                        passwordlessChallenge: code
                                    }, function(response2) {
                                        if (response2 && response2.locked) {
                                            modal.showLockedError(response2.lockedUntil);
                                        } else if (response2 && response2.wrongPin) {
                                            modal.showCodeError();
                                        } else {
                                            modal.close();
                                        }
                                    });
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    document.addEventListener('os2faktorEvent', function(e) {
        chrome.runtime.sendMessage({ event: 'challenge' }, function(response) {
            if (!response) return;
            handleChallenge(response);
        });
    }, false);

    chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
        if (request.event === 'manualChallenge') {
            var spinnerModal = new OS2faktorModal();
            spinnerModal.showSpinner();

            chrome.runtime.sendMessage({ event: 'challenge' }, function(response) {
                if (!response) {
                    setTimeout(function() { spinnerModal.close(); }, 2500);
                    return;
                }
                spinnerModal.close();
                handleChallenge(response);
            });

            sendResponse();
        }
    });

})();

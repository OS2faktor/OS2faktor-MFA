// OS2faktor inline modal — handles both passwordless and visual approval flows
class OS2faktorModal {
    constructor() {
        document.addEventListener('keydown', (e) => this.handleKeydown(e));

        document.addEventListener('keyup', (e) => {
            if (this.modal && e.key === 'Escape' && this.modal.classList.contains('active')) {
                this.closeAndReject();
            }
        });
    }

    injectOS2FaktorCSS(cssFilePath) {
        const existingLink = document.querySelector(`link[href="${cssFilePath}"]`);
        if (existingLink) {
            return;
        }
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.type = 'text/css';
        link.href = cssFilePath;
        document.head.appendChild(link);
    }

    _buildPinCirclesHTML(count) {
        let html = '';
        for (let i = 0; i < count; i++) {
            html += `<div class="os2faktor-pin-circle" data-index="${i}"></div>`;
        }
        return html;
    }

    // This modal is shown for the passwordless flow
    open(options = {}) {
        if (chrome && chrome.runtime && chrome.runtime.getURL) {
            this.injectOS2FaktorCSS(chrome.runtime.getURL("css/modal.css"));
        } else {
            this.injectOS2FaktorCSS("css/modal.css");
        }

        this.pinLength = options.pinLength || 6;

        this.modalOverlay = document.createElement('div');
        this.modalOverlay.id = 'os2faktorModal';
        this.modalOverlay.className = 'os2faktor-modal-overlay';
        this.modalOverlay.innerHTML = `
            <div class="os2faktor-modal-content">
                <button class="os2faktor-close-button" id="os2faktor-closeModal">×</button>

                <div class="os2faktor-modal-header">
                    <div class="os2faktor-logo-container">
                        <div class="os2faktor-logo-logo">
                            <img class="img-fluid" src="${chrome.runtime.getURL('images/os2faktor-logo.png')}" style="display: inline; max-width: 30vw; max-height: 40px;">
                        </div>
                    </div>
                </div>

                <div class="os2faktor-modal-body">
                    <div class="os2faktor-content-row">
                        <div class="os2faktor-message">
                            <span class="os2faktor-name" id="os2faktorModalName"></span><br>
                            <span class="os2faktor-timestamp" id="os2faktorModalTimestamp"></span>
                        </div>
                    </div>
                    <div class="os2faktor-pin-section">
                        <div class="os2faktor-pin-label">Indtast PIN-kode</div>
                        <div class="os2faktor-pin-circles">
                            ${this._buildPinCirclesHTML(this.pinLength)}
                        </div>
                        <div class="os2faktor-pin-error"></div>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(this.modalOverlay);

        this.modal = document.getElementById('os2faktorModal');
        this.modalContent = this.modal.querySelector('.os2faktor-modal-content');
        this.closeButton = document.getElementById('os2faktor-closeModal');
        this.pinCircles = document.querySelectorAll('.os2faktor-pin-circle');
        this.pinError = document.querySelector('.os2faktor-pin-error');
        this.os2faktorName = document.getElementById('os2faktorModalName');
        this.os2faktorModalTimestamp = document.getElementById('os2faktorModalTimestamp');

        this.closeButton.addEventListener('click', () => this.closeAndReject());

        if (options.name) {
            this.os2faktorName.textContent = options.name;
        }
        if (options.timestamp) {
            this.os2faktorModalTimestamp.textContent = options.timestamp;
        }

        this.onPinComplete = options.onPinComplete || null;
        this.uuid = options.uuid || null;

        this.resetPin();

        this.modal.classList.add('active');
        document.body.style.overflow = 'hidden';

        setTimeout(() => {
            this.modalContent.focus();
        }, 100);
    }

    // this modal is shown for the Token flow
    showApprovalUI(options = {}) {
        if (chrome && chrome.runtime && chrome.runtime.getURL) {
            this.injectOS2FaktorCSS(chrome.runtime.getURL("css/modal.css"));
        } else {
            this.injectOS2FaktorCSS("css/modal.css");
        }

        this.uuid = options.uuid || null;
        this.pinLength = options.pinLength || 6;
        this.onApprove = options.onApprove || null;
        this.onReject = options.onReject || null;
        this.approvalMode = true;

        this.modalOverlay = document.createElement('div');
        this.modalOverlay.id = 'os2faktorModal';
        this.modalOverlay.className = 'os2faktor-modal-overlay';
        this.modalOverlay.innerHTML = `
            <div class="os2faktor-modal-content">
                <button class="os2faktor-close-button" id="os2faktor-closeModal">×</button>

                <div class="os2faktor-modal-header">
                    <div class="os2faktor-logo-container">
                        <div class="os2faktor-logo-logo">
                            <img class="img-fluid" src="${chrome.runtime.getURL('images/os2faktor-logo.png')}" style="display: inline; max-width: 30vw; max-height: 40px;">
                        </div>
                    </div>
                </div>

                <div class="os2faktor-modal-body">
                    <div class="os2faktor-content-row">
                        <div class="os2faktor-message">
                            <span class="os2faktor-name" id="os2faktorModalName"></span><br>
                            <span class="os2faktor-timestamp" id="os2faktorModalTimestamp"></span>
                        </div>
                        <div class="os2faktor-challenge-section" style="text-align: center;">
                            <div class="os2faktor-challenge-code" style="font-size: 32px; font-weight: bold; letter-spacing: 6px; margin: 8px 0;">${options.challenge || ''}</div>
                        </div>
                    </div>
                    <div class="os2faktor-pin-section">
                        <div class="os2faktor-pin-label">Indtast PIN-kode</div>
                        <div class="os2faktor-pin-circles">
                            ${this._buildPinCirclesHTML(this.pinLength)}
                        </div>
                        <div class="os2faktor-pin-error"></div>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(this.modalOverlay);

        this.modal = document.getElementById('os2faktorModal');
        this.modalContent = this.modal.querySelector('.os2faktor-modal-content');
        this.closeButton = document.getElementById('os2faktor-closeModal');
        this.pinCircles = this.modalOverlay.querySelectorAll('.os2faktor-pin-circle');
        this.pinError = this.modalOverlay.querySelector('.os2faktor-pin-error');

        const nameEl = document.getElementById('os2faktorModalName');
        const tsEl = document.getElementById('os2faktorModalTimestamp');
        if (nameEl && options.name) {
            nameEl.textContent = options.name;
        }
        if (tsEl && options.timestamp) {
            tsEl.textContent = options.timestamp;
        }

        this.closeButton.addEventListener('click', () => {
            if (this.onReject) {
                this.onReject();
            } else {
                this.close();
            }
        });

        // Override onPinComplete to call onApprove
        this.onPinComplete = (pin) => {
            if (this.onApprove) {
                this.onApprove(pin);
            }
        };

        this.resetPin();

        this.modal.classList.add('active');
        document.body.style.overflow = 'hidden';

        setTimeout(() => {
            this.modalContent.focus();
        }, 100);
    }

    close() {
        if (this.modal) {
            this.modal.classList.remove('active');
            document.body.style.overflow = '';
            this.resetPin();
            this.modalOverlay.remove();
        }
    }

    closeAndReject() {
        if (this.uuid) {
            chrome.runtime.sendMessage({ event: 'rejectChallenge', uuid: this.uuid });
        }
        this.close();
    }

    resetPin() {
        this.resetPinInput();
        this.clearPinError();
    }

    resetPinInput() {
        this.locked = false;
        this.currentPinIndex = 0;
        this.pinCode = '';

        if (this.pinCircles) {
            this.pinCircles.forEach((circle) => {
                circle.classList.remove('filled', 'active');
            });

            if (this.pinCircles[0]) {
                this.pinCircles[0].classList.add('active');
            }
        }
    }

    clearPinError() {
        if (this.pinError) {
            this.pinError.textContent = '';
            this.pinError.classList.remove('visible');
        }
    }

    handleKeydown(e) {
        if (!this.modal || !this.modal.classList.contains('active')) {
            return;
        }

        if (e.key >= '0' && e.key <= '9') {
            e.preventDefault();
            this.addDigit(e.key);
        } else if (e.key === 'Backspace' || e.key === 'Delete') {
            e.preventDefault();
            this.removeDigit();
        }
    }

    addDigit(digit) {
        if (this.locked) {
            return;
        }
        const maxLength = this.passwordlessMode ? 2 : this.pinLength;
        if (this.currentPinIndex < maxLength) {
            this.pinCode += digit;

            this.pinCircles[this.currentPinIndex].classList.add('filled');
            this.pinCircles[this.currentPinIndex].classList.remove('active');

            this.currentPinIndex++;

            if (this.currentPinIndex < maxLength) {
                this.pinCircles[this.currentPinIndex].classList.add('active');
            }

            if (this.currentPinIndex === maxLength) {
                setTimeout(() => {
                    this.completePIN();
                }, 150);
            }
        }
    }

    removeDigit() {
        if (this.currentPinIndex > 0) {
            this.currentPinIndex--;
            this.pinCode = this.pinCode.slice(0, -1);

            this.pinCircles[this.currentPinIndex].classList.remove('filled');

            this.pinCircles.forEach(circle => circle.classList.remove('active'));
            this.pinCircles[this.currentPinIndex].classList.add('active');
        }
    }

    completePIN() {
        if (this.passwordlessMode && this.onCodeComplete) {
            this.onCodeComplete(this.pinCode);
        } else if (this.onPinComplete) {
            this.onPinComplete(this.pinCode);
        }
    }

    showPasswordlessEntry(onCodeComplete) {
        this.passwordlessMode = true;
        this.onCodeComplete = onCodeComplete;

        const label = this.modalOverlay.querySelector('.os2faktor-pin-section .os2faktor-pin-label');
        if (label) {
            label.textContent = 'Indtast kontrolkode';
        }

        const circlesContainer = this.modalOverlay.querySelector('.os2faktor-pin-circles');
        circlesContainer.innerHTML = `
            <div class="os2faktor-pin-circle" data-index="0"></div>
            <div class="os2faktor-pin-circle" data-index="1"></div>
        `;
        this.pinCircles = circlesContainer.querySelectorAll('.os2faktor-pin-circle');
        this.pinError = this.modalOverlay.querySelector('.os2faktor-pin-error');

        this.resetPin();
    }

    exitPasswordlessMode() {
        this.passwordlessMode = false;
        this.onCodeComplete = null;

        const circlesContainer = this.modalOverlay.querySelector('.os2faktor-pin-circles');
        circlesContainer.innerHTML = this._buildPinCirclesHTML(this.pinLength);
        this.pinCircles = circlesContainer.querySelectorAll('.os2faktor-pin-circle');
        this.pinError = this.modalOverlay.querySelector('.os2faktor-pin-error');

        const label = this.modalOverlay.querySelector('.os2faktor-pin-section .os2faktor-pin-label');
        if (label) {
            label.textContent = 'Indtast PIN-kode';
        }
    }

    showCodeError() {
        const circles = this.modalOverlay.querySelector('.os2faktor-pin-circles');
        circles.classList.add('shake');
        circles.addEventListener('animationend', () => {
            circles.classList.remove('shake');
            this.resetPinInput();
            setTimeout(() => this.clearPinError(), 4200);
        }, { once: true });

        if (this.pinError) {
            this.pinError.textContent = 'Forkert kontrolkode. Prøv igen.';
            this.pinError.classList.add('visible');
        }
    }

    showPinError() {
        this.exitPasswordlessMode();

        const circles = this.modalOverlay.querySelector('.os2faktor-pin-circles');
        circles.classList.add('shake');
        circles.addEventListener('animationend', () => {
            circles.classList.remove('shake');
            this.resetPinInput();
            setTimeout(() => this.clearPinError(), 4200);
        }, { once: true });

        if (this.pinError) {
            this.pinError.textContent = 'Forkert PIN-kode. Prøv igen.';
            this.pinError.classList.add('visible');
        }
    }

    showLockedError(lockedUntil) {
        this.locked = true;

        const circles = this.modalOverlay.querySelector('.os2faktor-pin-circles');
        circles.classList.add('shake');
        circles.addEventListener('animationend', () => {
            circles.classList.remove('shake');
        }, { once: true });

        if (this.pinError) {
            this.pinError.textContent = lockedUntil
                ? 'Din klient er låst indtil ' + lockedUntil + '.'
                : 'Din klient er låst.';
            this.pinError.classList.add('visible', 'locked');
        }
    }
}

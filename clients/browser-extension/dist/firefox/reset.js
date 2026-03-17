function createConfirmationModal() {
    const modalOverlay = document.createElement('div');
    modalOverlay.id = 'reset-modal-overlay';
    modalOverlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 10000;
        font-family: Arial, sans-serif;
    `;

    const modal = document.createElement('div');
    modal.id = 'reset-modal';
    modal.style.cssText = `
        background: white;
        border-radius: 8px;
        padding: 30px;
        max-width: 450px;
        width: 90%;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        text-align: center;
    `;

    const modalContent = document.createElement('div');
    modalContent.id = 'reset-modal-content';

    modal.appendChild(modalContent);
    modalOverlay.appendChild(modal);
    document.body.appendChild(modalOverlay);

    return { modalOverlay, modal, modalContent };
}

function showConfirmationDialog(title, message) {
    return new Promise((resolve) => {
        const { modalOverlay, modal, modalContent } = createConfirmationModal();

        modalContent.innerHTML = `
            <h2 style="color: #333; margin-bottom: 15px; font-size: 24px;">${title}</h2>
            <p style="color: #666; margin-bottom: 30px; font-size: 16px; line-height: 1.4;">${message}</p>
            <div style="margin-top: 30px;">
                <button id="reset-confirm-btn" style="
                        padding: 12px 24px;
                        border-radius: 4px;
                        cursor: pointer;
                        font-size: 16px;
                        margin-right: 10px;
                        background-color: #0069d9;
                        background-image: none;
                        border: 1px solid #0062cc;
                        color: #fff;
                        width: 120px;
                ">Ja</button>
                <button id="reset-cancel-btn" style="
                        padding: 12px 24px;
                        border-radius: 4px;
                        cursor: pointer;
                        font-size: 16px;
                        background-color: #dc3545;
                        background-image: none;
                        border: 1px solid #dc2e22;
                        color: #fff;
                        width: 120px;
                ">Nej</button>
            </div>
        `;

        const confirmBtn = document.getElementById('reset-confirm-btn');
        const cancelBtn = document.getElementById('reset-cancel-btn');

        setTimeout(() => {
            const modalHeight = modal.offsetHeight;
            window.resizeTo(512, modalHeight + 74 + (window.outerHeight - window.innerHeight));
        }, 100);

        confirmBtn.addEventListener('mouseenter', () => {
            confirmBtn.style.backgroundColor = '#0056b3';
        });
        confirmBtn.addEventListener('mouseleave', () => {
            confirmBtn.style.backgroundColor = '#007bff';
        });

        cancelBtn.addEventListener('mouseenter', () => {
            cancelBtn.style.backgroundColor = '#c82333';
        });
        cancelBtn.addEventListener('mouseleave', () => {
            cancelBtn.style.backgroundColor = '#dc3545';
        });

        confirmBtn.addEventListener('click', () => {
            document.body.removeChild(modalOverlay);
            resolve(true);
        });

        cancelBtn.addEventListener('click', () => {
            document.body.removeChild(modalOverlay);
            resolve(false);
        });

        document.addEventListener('keydown', function escHandler(e) {
            if (e.key === 'Escape') {
                document.body.removeChild(modalOverlay);
                document.removeEventListener('keydown', escHandler);
                resolve(false);
            }
        });

        setTimeout(() => {
            confirmBtn.focus();
        }, 100);
    });
}

async function showReset() {
    const result = await showConfirmationDialog('Nulstil klient', 'Er du sikker på at du vil nulstille din klient?');

    if (result) {
        try {
            const stored = await storageGet(["apiKey", "deviceId"]);
            const apiKey = stored["apiKey"];
            const deviceId = stored["deviceId"];

            await storageRemove(["apiKey", "deviceId", "mitIdRegistered", "pinRegistered", "pinLength"]);

            if (deviceId != null && apiKey != null) {
                try {
                    await fetch(BACKEND_URL + "/api/client", {
                        method: 'DELETE',
                        headers: { 'ApiKey': apiKey, 'deviceId': deviceId }
                    });
                } catch (error) {
                    console.error('Error deleting client from backend:', error);
                }
            }
        } catch (error) {
            console.error('Error during reset:', error);
        }
    }

    window.close();
}

showReset();

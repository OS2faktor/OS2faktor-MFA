# OS2faktor Chrome Extension

## Installation

### Manual installation (unpacked)(development)
1. Run `./build.sh chrome` from the `browser-extension/` directory
2. Open Chrome and go to `chrome://extensions`
3. Enable **Developer mode** (top right toggle)
4. Click **Load unpacked** and select `dist/chrome/`

### Installation in Store
```bash
./build.sh chrome
```

The build produces `releases/os2faktor-chrome-<version>.zip`.
Upload this file to the [Chrome Web Store Developer Dashboard](https://chrome.google.com/webstore/devconsole).

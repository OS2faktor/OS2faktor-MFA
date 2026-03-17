# OS2faktor Firefox Extension

## Installation

### Manual installation (unpacked)(development)
1. Run `./build.sh firefox` from the `browser-extension/` directory
2. Open Firefox and go to `about:debugging#/runtime/this-firefox`
3. Click **Load Temporary Add-on** and select any file inside `dist/firefox/`

Note: Temporary add-ons are removed when Firefox restarts.
For persistent installation without signing, use Firefox Developer Edition
and set xpinstall.signatures.required = false in about:config.

### Installing the signed XPI
1. Download `releases/os2faktor-firefox-<version>.xpi`
2. In Firefox go to `about:addons`
3. Click the gear icon → **Install Add-on From File** and select the `.xpi`

### Submitting to AMO
```bash
./build.sh firefox
```

Upload `releases/os2faktor-firefox-<version>.xpi` to
[addons.mozilla.org/developers](https://addons.mozilla.org/developers/).
Mozilla requires source code submission for extensions containing minified code.

## Minimum Firefox version

Requires Firefox 109 or later (Manifest V3 support).


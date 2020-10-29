Install in browser (dev)
========================
1. Open Chrome
2. Go to chrome://extensions/
3. Enable developer mode
4. Load Unpacked button, navigate to folder and open

Schema/Policy configuration of URLs
===================================
1. Default settings in code, so no reason to do this unless needed
2. If needed, it is OS-specific, read more here:
3. On Linux, modify /etc/opt/chrome/policies/managed/policy.json and fill in this (id might differ)

{
  "ShowHomeButton": true,

  "3rdparty": {
    "extensions": {
      "clnpgeifaldjjkfdhmhmblnnodnomgak": {
        "BackendUrl": "http://192.168.1.111:9088",
        "FrontendUrl": "http://192.168.1.111:9121"
      }
    }
  }
}

4. On Windows, make a chrome.reg file an fill in this (id might differ):
	Windows Registry Editor Version 5.00
	; chrome version: 85.0.4183.102

	[HKEY_LOCAL_MACHINE\Software\Policies\Google\Chrome\3rdparty\extensions\clnpgeifaldjjkfdhmhmblnnodnomgak\policy]
	"FrontendUrl"= "http://192.168.1.111:9121"
	"BackendUrl"= "http://192.168.1.111:9088"
	
	4.1 Find your file and double click it
	4.2 accept


Install in Web Store
====================
1. modify manifest.json (outside src folder), and bump version
2. run build.sh
3. goto https://chrome.google.com/webstore/devconsole/
4. upload app.zip as new version

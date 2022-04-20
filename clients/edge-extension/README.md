Modifications made to manifest.json to make web store accept it
===============================================================
changed version to 2 (3 is required for service workers, so wonder how that will work)
changed the content_security_policy to a simple string, again, wonder how that will work

Install in Web Store
====================
1. modify manifest.json (outside src folder), and bump version
2. run build.sh
3. https://partner.microsoft.com/en-us/dashboard/microsoftedge/
4. upload app.zip as new version


TEST INFO FOR MS
================

The application is a 2-faktor login application, so it has the following features

* registration of device
* use of device as a login factor

To showcase the applications use, a demo website has been build, that showcases how to tie a OS2faktor client device to your account, and then use it during login. The following steps will show how this works

1) Open the app and click on the registration button - and pick a name for the device. The name is not important, it is used for managing multiple clients
2) Visit https://demo.os2faktor.dk/ in an ordinary desktop browser
3) Perform a login using the supplied credentials (ms001/ms001 and ms002/ms002 are additional test accounts if needed)
4) Visit the "Settings" menu, and enter the DeviceID found at the bottom of the running app (after it has been registered, the app is given a DeviceID)
5) Logout from the website and attempt to login again

During the login process, the username/password is no longer enough - it will not require approval from within the app. The app should show a prompt, asking to approve the login. If approved, login will succeed. If rejected, the login will fail.

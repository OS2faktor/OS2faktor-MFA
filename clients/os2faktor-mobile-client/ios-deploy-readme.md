Close Repo
Install npm and cordova
Remove platform/ios folder
Execute "cordova platform add ios@7.1.1"
Open project in XCode, note that it is the workspace file you need to open!
Update IOS version to iOS15 under "Build settings" tab and make sure the swift version is correct, aka 4.0
Select OS2faktor "target" and "Signing & Capabilities" tab.
Fix Team and profile so the tab has no errors. (select Digital identity for signing)
Add push notification capability if it is not already present (it most likely already is)
Choose "Generic IOS Device" in top menu bar.
Choose Product->Archive menu and follow instructions (manually sign)



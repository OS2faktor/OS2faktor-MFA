1) update code with whatever changes are needed

2) run ./build.sh

3) in the output folder the APK is now located, it needs to be signed

/home/brian/Android/Sdk/build-tools/28.0.3/zipalign -v -p 4 output/app-release-unsigned.apk app-unsigned-aligned.apk
/home/brian/Android/Sdk/build-tools/28.0.3/apksigner sign --ks ~/projects/os2faktor/keys/prod/android/android-prod.keystore --out app-release.apk app-unsigned-aligned.apk 

4) deploy to app store

https://play.google.com/apps/publish/?account=7829721744116569915#AppListPlace

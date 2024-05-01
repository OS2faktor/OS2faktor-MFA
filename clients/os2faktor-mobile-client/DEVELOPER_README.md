## Cordova Instalation guide
**==🔴❗Use java 11==**

``` bash
sudo apt install npm
sudo npm install -g cordova@11.0.0
mkdir -p sdk/cmdline-tools

unzip commandlinetools-linux-8512546_latest.zip -d sdk/cmdline-tools
cd sdk
sdkmanager "build-tools;30.0.3" "platform-tools" "platforms;android-31"

```

10.  Accept Android Licenses:
``` bash
sdkmanager --licenses
```
11.  Update Android Packages when necessary:
``` bash
sdkmanager --update
```


## Running on android device
``` bash
cordova run android
```
## Building and signing without docker
Generated file can be found in ./platforms/android/app/build/outputs/apk/release/

0) Generate keystore for test puropses
``` bash
keytool -genkey -v -keystore android.keystore -alias android -keyalg RSA -keysize 2048 -validity 10000
```

1) APK
``` bash
cordova build android --release -- --keystore="android.keystore" --storePassword=Test1234 --alias=android --password=Test1234 --packageType=apk
```
2) AAB
``` bash
cordova build android --release -- --keystore="android.keystore" --storePassword=Test1234 --alias=android --password=Test1234 --packageType=bundle
```
## Dealing with AAB
*The **jar** file is located inside **docker-build** folder so run those commands from there*
1) How to convert aab to apks

``` bash
java -jar bundletool-all-1.11.2.jar build-apks --bundle=/home/piotr/projects/os2faktor-mobile-client/docker-build/output/app-release.aab --output=/home/piotr/projects/os2faktor-mobile-client/docker-build/output/app-release.apks
```
	
2) Deploy apks to android device
``` bash
java -jar bundletool-all-1.11.2.jar install-apks --apks=/home/piotr/projects/os2faktor-mobile-client/docker-build/output/app-release.apks
```

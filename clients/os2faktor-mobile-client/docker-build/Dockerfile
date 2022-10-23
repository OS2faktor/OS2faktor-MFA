FROM ubuntu:18.04

WORKDIR /app

# install node 10.15.0 and cordova 9.0.0
RUN \
apt-get update && \
apt-get install -y software-properties-common curl python git unzip wget apt-transport-https dirmngr && \
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys D7CC6F019D06AF36 && \
add-apt-repository ppa:cwchien/gradle && \
apt-get update -y && \
apt-get install gradle -y && \
curl https://nodejs.org/dist/v16.17.1/node-v16.17.1-linux-x64.tar.xz | tar xJ -C /usr/local/ --strip=1 && \
npm install -g cordova@11.0.0

# install java 11 for building android package
RUN \
apt-get update && \
apt-get install -y openjdk-11-jdk

# install android SDK tools
COPY commandlinetools-linux-8512546_latest.zip .
RUN mkdir -p sdk/cmdline-tools
RUN unzip commandlinetools-linux-8512546_latest.zip -d sdk/cmdline-tools

ENV ANDROID_SDK_ROOT /app/sdk
ENV ANDROID_HOME /app/sdk
ENV PATH $PATH:$ANDROID_HOME/cmdline-tools/cmdline-tools/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/31.0.0

RUN gradle --version

# accept license and update android-sdk to API 31
# for some reason (probably gradle) we need specific 30.0.3 version of build-tools
# I found quotes online related to this bug https://github.com/apache/cordova-android/issues/1391
# "Build tools 30.0.3 is the only version supported by Android Studio that works"
# versions 31.x and 32.x lead to "Installed Build Tools revision X is corrupted."
RUN ( sleep 5 && while [ 1 ]; do sleep 1; echo y; done ) | (cd /app/sdk/ && sdkmanager "build-tools;30.0.3" "platform-tools" "platforms;android-31")

# Copy dependency configuration files
COPY tmp/ /app/

# Install dependencies
RUN npm install

# Install android platform
RUN cordova platform add android

# get all we need to build
RUN cordova prepare android

# should fail if requirements are not met
RUN cordova requirements

# build the APK
# APK
RUN cordova build android --release -- --keystore="android-prod.keystore" --storePassword=Test1234 --alias=android --password=Test1234 --packageType=apk
# AAB
#RUN cordova build android --release -- --keystore="android-prod.keystore" --storePassword=Test1234 --alias=android --password=Test1234 --packageType=bundle

RUN ls -l /app/sdk

# run the shell script that copies to the file to the mounted volume
ENTRYPOINT ["/bin/bash", "run.sh"]

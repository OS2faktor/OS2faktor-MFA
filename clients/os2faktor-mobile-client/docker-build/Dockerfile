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
curl https://nodejs.org/dist/v10.15.0/node-v10.15.0-linux-x64.tar.gz | tar xz -C /usr/local/ --strip=1 && \
npm install -g cordova@9.0.0

# install java 8 for building android package
RUN apt-get remove -y openjdk-11-jre-headless
RUN \
apt-get update && \
apt-get install -y openjdk-8-jdk && \
rm -rf /var/lib/apt/lists/*

# install android SDK tools
COPY commandlinetools-linux-6609375_latest.zip .
RUN mkdir -p sdk/cmdline-tools
RUN unzip commandlinetools-linux-6609375_latest.zip -d sdk/cmdline-tools

ENV ANDROID_SDK_ROOT /app/sdk
ENV ANDROID_HOME /app/sdk
ENV PATH $PATH:$ANDROID_HOME/cmdline-tools/tools/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/29.0.2

RUN gradle --version

# accept license and update android-sdk to API 29
RUN ( sleep 5 && while [ 1 ]; do sleep 1; echo y; done ) | (cd /app/sdk/ && sdkmanager "build-tools;29.0.2" "platform-tools" "platforms;android-29")

# Copy dependency configuration files
COPY tmp/ /app/

# Install dependencies
RUN npm install

# Install android platform
RUN cordova platform add android@9.0.0

# get all we need to build
RUN cordova prepare android

# does it work?
RUN cordova requirements

# if it does, then build the APK (signing happens outside Docker)
RUN cordova build --release android

RUN ls -l /app/sdk

# finally sign the binary
RUN zipalign -v -p 4 /app/platforms/android/app/build/outputs/apk/release/app-release-unsigned.apk aligned.apk
RUN apksigner sign --ks /app/android-prod.keystore --ks-pass pass:Test1234 --out release.apk aligned.apk

# run the shell script that copies to the file to the mounted volume
ENTRYPOINT ["/bin/bash", "run.sh"]

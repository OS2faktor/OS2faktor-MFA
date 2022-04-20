#!/bin/bash

# clean build folder
rm -Rf tmp
rm -Rf output
mkdir tmp
mkdir output

# gather relevant files
cp ~/projects/os2faktor-deploy/keys/prod/android/android-prod.keystore tmp
cp ../config.xml tmp
cp ../google-services.json tmp
cp ../package.json tmp
cp -R ../res tmp
cp -R ../www tmp
cp run.sh tmp

# build and run
docker build -t os2faktor-mobile:build . && \
docker run -v $PWD/output:/mnt/host os2faktor-mobile:build

# cleanup
rm -Rf tmp

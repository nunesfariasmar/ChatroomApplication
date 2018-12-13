#!/bin/bash
set -x
set -e
./gradlew assembleDebug
firebase login
cd firebase_node/typescript_clone_images_proj/functions
npm install
cd ..
firebase deploy --only functions
adb install ../../app/build/outputs/apk/debug/app-debug.apk

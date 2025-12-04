#!/bin/bash
# Quick install script for NanoMInd

echo "Building NanoMInd..."
./gradlew :app:assembleDebug

echo ""
echo "Installing on device..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo ""
echo "✅ Installation complete!"
echo "Make sure your model file 'nanomind_model.gguf' is in the Downloads folder"

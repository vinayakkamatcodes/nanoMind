#!/bin/bash
echo "=== Verifying llamacpp-kotlin library ==="
echo ""
echo "1. Checking if library is in dependencies..."
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -i llamacpp || echo "NOT FOUND in dependencies"
echo ""
echo "2. Checking if library JAR/AAR is downloaded..."
find ~/.gradle/caches -name "*llamacpp*" -type f 2>/dev/null | head -3
echo ""
echo "3. After building, check if library is in APK:"
echo "   unzip -l app/build/outputs/apk/debug/app-debug.apk | grep llamacpp"
echo ""
echo "4. To rebuild cleanly:"
echo "   ./gradlew clean assembleDebug"

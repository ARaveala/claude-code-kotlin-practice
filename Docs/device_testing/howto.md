**sony j9110** (computer ubuntu)
1. Turn on USB debugging on the phone
- Settings → About phone → tap Build number 7 times until it says you're a developer.
- Settings → System → Developer options → turn on USB debugging.
- Optional: turn on Stay awake in the same menu so the screen doesn't lock mid-test.

2. Plug it in and authorise it
- Use a USB cable that carries data. Some cheap ones only charge.
- If the phone asks for a USB mode, pick File transfer. Some phones won't show up for ADB without it.
- Accept the "Allow USB debugging?" prompt and tick "Always allow from this computer".
- Check it worked: ! adb devices. (apt install adb) You should see a serial number followed by device.
  - unauthorized: the prompt wasn't accepted. Unplug, plug back in and accept it.
  - no permissions (common on Linux): your user needs udev access to the phone. sudo apt install android-sdk-platform-tools-common adds the rules, then replug. If that doesn't fix it, check that you're in the plugdev group.

3. Install and launch
- In Android Studio, pick the phone in the device dropdown (it's listed next to the emulator) and press Run. This installs the debug build, which is signed automatically with the debug key, so no signing setup is needed.
- From a terminal, ! ./gradlew installDebug does the same with one device connected. With the emulator running too, set ANDROID_SERIAL=<phone serial> first so it doesn't install to the wrong one.
- Don't run connectedAndroidTest with the phone plugged in. It wipes the app data on whatever device it targets.

4. Logcat for the checklist
In Android Studio's Logcat, pick the phone and filter with tag:AreaCanvasScreen | tag:GrowZoneViewModel.

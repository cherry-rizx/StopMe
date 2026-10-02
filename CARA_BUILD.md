# Cara build StopMe (Linux, tanpa Android Studio)

1. Ekstrak zip ini, lalu masuk ke foldernya:
   cd ~/StopMe

2. Taruh file rahasia:
   cp ~/StopMe-rahasia/google-services.json app/
   echo "sdk.dir=$HOME/Android/Sdk" > local.properties
   echo "WEB_CLIENT_ID=ISI_WEB_CLIENT_ID.apps.googleusercontent.com" >> local.properties

3. Colok HP (cek: adb devices -> status "device"), lalu:
   ./gradlew installDebug

   Build pertama lama (mengunduh Gradle, plugin, dan SDK). Kalau error,
   salin pesan error-nya (bagian "What went wrong") untuk diperbaiki.

4. Lihat log saat uji coba:
   adb logcat | grep -i -E "stopme|AndroidRuntime"

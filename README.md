# Team X Reader

A small Android reader app for [olympustaff.com](https://olympustaff.com) (Team X, Arabic manga).

It talks to the site directly — no Aidoku runtime involved. Aidoku `.aix` extensions are WebAssembly
modules that only the Aidoku iOS app can execute, so this app reimplements the same scraping logic
(`sources/ar.teamx` in the Aidoku sources repo) in plain Java.

## What it does

- **Popular list** (`/series`, paginated with *Load more*)
- **Search** (`/search?keyword=…`)
- **Details page**: cover, title, description, tags, full chapter list across all pages
- **Reader**: one page at a time with Previous / Next (low memory, works for long webtoon strips)

## Build

CI builds and signs the APK on every push to `main`:

**Actions → Build APK → Artifacts → TeamXReader**

The workflow uses the Android SDK already installed on the runner (`aapt2`, `javac`, `d8`,
`zipalign`, `apksigner`) — there is no Gradle project.

Local build (same steps as CI) on a machine with JDK 17 and an Android SDK:

```sh
AJ="$ANDROID_SDK_ROOT/platforms/android-33/android.jar"
BT="$ANDROID_SDK_ROOT/build-tools/34.0.0"

mkdir -p build/classes build/dex
"$BT/aapt2" compile --dir app/res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$AJ" \
  --manifest app/AndroidManifest.xml -R build/res.zip \
  --min-sdk-version 26 --target-sdk-version 33

javac --release 11 -encoding UTF-8 -classpath "$AJ:libs/jsoup-1.17.2.jar" \
  -d build/classes app/src/dev/teamx/reader/*.java

"$BT/d8" --lib "$AJ" --release --min-api 26 --output build/dex \
  $(find build/classes -name '*.class') libs/jsoup-1.17.2.jar

cd build && cp base.apk unsigned.apk && zip -q -j unsigned.apk dex/classes.dex && cd ..

keytool -genkeypair -keystore build/debug.keystore -alias androiddebugkey \
  -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US" \
  -keyalg RSA -keysize 2048 -validity 10000

"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks build/debug.keystore \
  --ks-pass pass:android --key-pass pass:android \
  --out TeamXReader.apk build/aligned.apk
```

Requires **JDK 17**: javac 21+ writes `MethodParameters` entries for synthetic bridges that older
`d8` versions crash on.

## Layout

```
app/AndroidManifest.xml
app/src/dev/teamx/reader/Site.java          # HTTP + Jsoup scraping
app/src/dev/teamx/reader/MainActivity.java  # popular + search
app/src/dev/teamx/reader/DetailsActivity.java
app/src/dev/teamx/reader/ReaderActivity.java
app/src/dev/teamx/reader/MangaAdapter.java
app/src/dev/teamx/reader/ImageLoader.java
libs/jsoup-1.17.2.jar
```

Package `dev.teamx.reader`, min Android 8.0 (API 26), target 33. Debug-signed; install manually
(the APK is not on any store).

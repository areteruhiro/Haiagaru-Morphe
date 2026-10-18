param([Parameter(Mandatory=$true)][string]$Serial)
$ErrorActionPreference='Stop'
$java='C:/Users/narum/jdk-21.0.9+10/bin'
$sdk='C:/Users/narum/AppData/Local/Android/Sdk'
$android="$sdk/platforms/android-36/android.jar"
$classes='extensions/chmate/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes'
$output='build/visibility-switch-tests'
$null=New-Item -ItemType Directory -Force -Path $output
& "$java/javac.exe" -encoding UTF-8 -cp "$android;$classes" -d $output scripts/VerifyVisibilitySwitches.java scripts/VerifyResponseActionDefaults.java scripts/VerifyFeatureSwitchCatalog.java
if($LASTEXITCODE -ne 0){throw 'Compilation failed'}
& "$java/jar.exe" cf "$output/fixture.jar" -C $output app
if($LASTEXITCODE -ne 0){throw 'Packaging failed'}
& "$java/java.exe" -cp "$sdk/build-tools/36.0.0/lib/d8.jar" com.android.tools.r8.D8 --min-api 26 --lib $android --output $output "$output/fixture.jar"
if($LASTEXITCODE -ne 0){throw 'DEX failed'}
$adb="$sdk/platform-tools/adb.exe"
& $adb -s $Serial push "$output/classes.dex" /data/local/tmp/haiagaru-visibility-tests.dex
if($LASTEXITCODE -ne 0){throw 'Test transfer failed'}
& $adb -s $Serial push extensions/chmate/build/intermediates/dex/release/minifyReleaseWithR8/classes.dex /data/local/tmp/haiagaru-visibility-production.dex
if($LASTEXITCODE -ne 0){throw 'Production transfer failed'}
foreach($test in @('VerifyVisibilitySwitches','VerifyResponseActionDefaults','VerifyFeatureSwitchCatalog')){
    & $adb -s $Serial shell 'CLASSPATH=/data/local/tmp/haiagaru-visibility-tests.dex:/data/local/tmp/haiagaru-visibility-production.dex' app_process /system/bin "app.morphe.extension.chmate.$test"
    if($LASTEXITCODE -ne 0){throw "$test failed"}
}

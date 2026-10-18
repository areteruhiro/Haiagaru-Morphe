param([string]$Serial = 'emulator-5554',
      [string]$Sdk = 'C:/Users/narum/AppData/Local/Android/Sdk',
      [string]$Java = 'C:/Users/narum/jdk-21.0.9+10')
$ErrorActionPreference = 'Stop'
$output = Join-Path ([IO.Path]::GetTempPath()) ('haiagaru-x-performance-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $output
$android = Join-Path $Sdk 'platforms/android-36/android.jar'
$extension = 'extensions/chmate/src/main/java/app/morphe/extension/chmate'
& "$Java/bin/javac.exe" -encoding UTF-8 -cp $android -d $output `
    scripts/x-post-preview/Haiagaru.java scripts/x-post-preview/XPostMetadata.java `
    scripts/x-post-preview/MediaTestStubs.java scripts/x-post-preview/PreviewTest.java `
    scripts/x-post-preview/PerformanceTest.java scripts/x-post-preview/RetryTest.java scripts/x-post-preview/CardRenderTest.java `
    scripts/x-post-preview/PositionTest.java `
    "$extension/XPostPreview.java" "$extension/XPostPreviewTouch.java" `
    "$extension/XPostPreviewPosition.java" "$extension/XPostPreviewCard.java" "$extension/XMediaAttachments.java" "$extension/TwimgImageUrls.java" "$extension/DirectVideoUrls.java"
if ($LASTEXITCODE -ne 0) { throw 'Fixture compilation failed' }
& "$Java/bin/jar.exe" cf "$output/fixture.jar" -C $output app
if ($LASTEXITCODE -ne 0) { throw 'Fixture packaging failed' }
& "$Java/bin/java.exe" -cp "$Sdk/build-tools/36.0.0/lib/d8.jar" com.android.tools.r8.D8 `
    --min-api 21 --lib $android --output $output "$output/fixture.jar"
if ($LASTEXITCODE -ne 0) { throw 'DEX compilation failed' }
& "$Sdk/platform-tools/adb.exe" -s $Serial push "$output/classes.dex" /data/local/tmp/haiagaru-x-preview-performance.dex
if ($LASTEXITCODE -ne 0) { throw 'Fixture transfer failed' }
Write-Output "Fixture retained at $output"

param([string]$Version = '191')
$ErrorActionPreference='Stop'
$aapt='C:/Users/narum/AppData/Local/Android/Sdk/build-tools/36.0.0/aapt2.exe'
$apk=Join-Path $PSScriptRoot "../build/verification/board-editor$Version.apk"
$expected="jp.co.airfront.android.a2chMate.haiagaru.boardedit$Version"
$resources=(& $aapt dump resources $apk) -join "`n"
$xmls=@([regex]::Matches($resources,'resource 0x[0-9a-f]+ xml/[^\r\n]+\r?\n\s+\(\) "([^"]+\.xml)"') | ForEach-Object {$_.Groups[1].Value})
$found=0
foreach($xml in $xmls){
    $tree=(& $aapt dump xmltree $apk --file $xml) -join "`n"
    if($tree -notmatch 'haiagaruExternalBoardEditor'){continue}
    if($tree -notmatch 'updateBbsMenu'){throw 'Editor not in Board/BBSMENU preferences'}
    if(!$tree.Contains('android:targetPackage') -or !$tree.Contains($expected)){throw 'Editor package intent not rewritten'}
    if(!$tree.Contains('app.morphe.extension.chmate.ExternalBoardEditorActivity')){throw 'Editor activity missing'}
    $found++
}
if($found -ne 1){throw "Expected one native board preference entry, found $found"}
$manifest=(& $aapt dump xmltree $apk --file AndroidManifest.xml) -join "`n"
if(!$manifest.Contains('app.morphe.extension.chmate.ExternalBoardEditorActivity')){throw 'Editor manifest entry missing'}
Write-Output "PASS $Version native Board/BBSMENU entry, explicit package intent and activity registration"

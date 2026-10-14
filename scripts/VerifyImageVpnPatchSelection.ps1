$ErrorActionPreference = 'Stop'
$taskRepo = Split-Path -Parent $PSScriptRoot
$taskPatch = Get-Content -Raw -LiteralPath (Join-Path $taskRepo 'patches/src/main/kotlin/app/morphe/patches/chmate/ImageVpnResourcesPatch.kt')
$taskHaiagaru = Get-Content -Raw -LiteralPath (Join-Path $taskRepo 'patches/src/main/kotlin/app/morphe/patches/chmate/HaiagaruPatch.kt')
if ($taskPatch -notmatch 'val imageVpnResourcesPatch = resourcePatch\(') { throw 'VPN patch is not separately selectable' }
if ($taskPatch -notmatch 'default = false') { throw 'VPN patch must be opt-in' }
if ($taskPatch -notmatch 'dependsOn\(imageVpnTransportPatch\)') { throw 'Image VPN native transport dependency is missing' }
if ($taskHaiagaru -match 'dependsOn\([^)]*imageVpnResourcesPatch') { throw 'Ordinary Haiagaru must not force VPN packaging' }
if ($taskPatch -notmatch '未完了' -or $taskPatch -notmatch '有効化が必要') { throw 'Staging limitations must be visible' }
Write-Output 'PASS image VPN patch selection: separately selectable, default-off, no forced inclusion and explicit staging notice'

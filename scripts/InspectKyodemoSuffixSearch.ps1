param(
    [string]$Board = 'e_e_liveedge',
    [string]$Id = 'nr7x80QOM',
    [string]$Wacchoi = '',
    [string]$Suffix = '-Kuom',
    [string]$Prefix = '',
    [string]$ThreadKey = '',
    [string]$PostDate = '',
    [ValidateRange(10, 300)][int]$RequestIntervalSeconds = 10,
    [switch]$IncludeAlternateQueries,
    [string]$OutputDirectory = ''
)
# Read-only public-page diagnostics. Does not log in, request analysis, or post.
$ErrorActionPreference = 'Stop'
if ($Board -notmatch '^[A-Za-z0-9_-]+$') { throw 'Invalid board slug' }
if ($Suffix -notmatch '^-[^\s()<>]{4}$') { throw 'Suffix must be a hyphen plus four characters' }
if ($Prefix -and $Prefix -notmatch '^[A-Za-z0-9]{4}$') { throw 'Prefix must contain four alphanumeric characters' }
if ($ThreadKey -and $ThreadKey -notmatch '^[0-9]{9,}$') { throw 'Invalid thread key' }
if ($PostDate -and $PostDate -notmatch '^[0-9]{8}$') { throw 'PostDate must be YYYYMMDD' }
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $PSScriptRoot ('../build/verification/kyodemo-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
}
$null = New-Item -ItemType Directory -Path $OutputDirectory -Force
$OutputDirectory = (Resolve-Path -LiteralPath $OutputDirectory).Path
try { [Text.Encoding]::RegisterProvider([Text.CodePagesEncodingProvider]::Instance) } catch { }
$encoding = [Text.Encoding]::GetEncoding(932)
$base = 'https://www.kyodemo.net'
$tests = @(
    @{ Name = 'id'; Path = "/sdemo/b/$Board/?bs=hi&k=" + [uri]::EscapeDataString("ID:$Id") },
    @{ Name = 'suffix'; Path = "/sdemo/b/$Board/?bs=hi&k=" + [uri]::EscapeDataString($Suffix) },
    @{ Name = 'search-form'; Path = '/sdemo/s' },
    @{ Name = 'public-script'; Path = '/sdemo/com/c.js?a=91' }
)
if ($Wacchoi) {
    # Use the site's legacy form encoding where lossless; otherwise UTF-8.
    $strict = [Text.Encoding]::GetEncoding(932, [Text.EncoderFallback]::ExceptionFallback, [Text.DecoderFallback]::ExceptionFallback)
    try { $bytes = $strict.GetBytes($Wacchoi) } catch { $bytes = [Text.Encoding]::UTF8.GetBytes($Wacchoi) }
    $escaped = ($bytes | ForEach-Object { '%' + $_.ToString('X2') }) -join ''
    $tests += @{ Name = 'full-wacchoi'; Path = "/sdemo/b/$Board/?bs=hi&k=$escaped" }
    if ($ThreadKey -and $PostDate) {
        $tests += @{ Name = 'wc-full'; Path = "/sdemo/b/$Board/?hi=&key=$ThreadKey&date=$PostDate&wc=$escaped" }
    }
}
if ($IncludeAlternateQueries) {
    # A small fixed comparison set, not a crawl or exhaustive parameter scan.
    foreach ($query in @("*$Suffix", "ﾜｯﾁｮｲ $Suffix", "L20 $Suffix")) {
        $tests += @{ Name = 'alternate-' + $tests.Count; Path = "/sdemo/b/$Board/?bs=hi&k=" + [uri]::EscapeDataString($query) }
    }
}
if ($Prefix) {
    foreach ($query in @("$Prefix-", "L20 $Prefix-")) {
        $tests += @{ Name = 'prefix-' + $tests.Count; Path = "/sdemo/b/$Board/?bs=hi&k=" + [uri]::EscapeDataString($query) }
    }
}
$results = @()
$lastRequest = [DateTime]::MinValue
foreach ($test in $tests) {
    $remaining = $RequestIntervalSeconds - ([DateTime]::UtcNow - $lastRequest).TotalSeconds
    if ($remaining -gt 0) { Start-Sleep -Milliseconds ([int][Math]::Ceiling($remaining * 1000)) }
    $url = $base + $test.Path
    $file = Join-Path $OutputDirectory ($test.Name + '.response')
    # curl enforces per-request limits and HTTPS-only redirects. Cookies are not supplied.
    $lastRequest = [DateTime]::UtcNow
    $status = & curl.exe --silent --show-error --max-time 20 `
        --proto '=https' --proto-redir '=https' --output $file --write-out '%{http_code}' $url
    $exitCode = $LASTEXITCODE
    $text = if (Test-Path -LiteralPath $file) { $encoding.GetString([IO.File]::ReadAllBytes($file)) } else { '' }
    $headerCount = [regex]::Matches($text, 'class="r-head"').Count
    $posterCount = [regex]::Matches($text, '(?:ID:[A-Za-z0-9+/!._-]+|class="clname")').Count
    $externalOnly = $headerCount -le 1 -and $posterCount -eq 0 -and $text.Contains('スレッドを表示する(外部サイト)')
    $outcome = if ($exitCode -ne 0) { 'transport-error' }
        elseif ($text.Contains('IDを指定してください')) { 'query-rejected' }
        elseif ($status -ne '200') { 'http-error' }
        elseif ($text.Contains('ID検索エラー')) { 'search-error' }
        elseif ($text.Contains('IDが見つかりませんでした')) { 'no-results' }
        elseif ($externalOnly) { 'external-link-only' }
        elseif ($headerCount -gt 0 -and $posterCount -gt 0) { 'post-headers-returned' }
        else { 'inspect-response' }
    $results += [pscustomobject]@{
        Name = $test.Name; Status = "$status"; Outcome = $outcome; Url = $url; File = $file
        HeaderCount = $headerCount; PosterMarkerCount = $posterCount
    }
    Write-Output "$($test.Name): HTTP $status / $outcome"
    if ($status -in @('403', '429', '503') -or $exitCode -ne 0 -or $outcome -eq 'search-error') {
        Write-Output 'Stopping on access restriction, overload, or transport failure; no automatic retry.'
        break
    }
}
$results | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'results.json') -Encoding UTF8
Write-Output "Saved to $OutputDirectory"
Write-Output 'post-headers-returned alone does not prove matching. Compare actual returned poster names with a known full Wacchoi.'

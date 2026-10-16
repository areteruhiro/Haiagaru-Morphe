param([string]$ResultsDirectory = 'build/verification')
$ErrorActionPreference = 'Stop'
foreach ($version in @('191', '226')) {
    $logPath = Join-Path $ResultsDirectory "network$version-failures-final.log"
    $rows = @(Get-Content -LiteralPath $logPath | Where-Object { $_.StartsWith('{') } |
        ForEach-Object { $_ | ConvertFrom-Json })
    if ($rows.Count -ne 9) { throw "$version expected 9 results, got $($rows.Count)" }
    if (@($rows | Where-Object { -not $_.pass -or $_.version -ne $version }).Count) {
        throw "$version failed a native transport check"
    }
    foreach ($label in @('tls-handshake-no-response', 'http-headers-no-response', 'http-body-stalled')) {
        $row = @($rows | Where-Object { $_.test -eq $label })
        if ($row.Count -ne 1 -or $row[0].exception[0] -ne 'java.net.SocketTimeoutException' -or
                $row[0].elapsedMs -lt 14000 -or $row[0].elapsedMs -ge 22000) {
            throw "$version invalid timeout result: $label"
        }
    }
    $cert = @($rows | Where-Object { $_.test -eq 'untrusted-certificate' })
    if ($cert.Count -ne 1 -or $cert[0].exception[0] -ne 'javax.net.ssl.SSLHandshakeException') {
        throw "$version missing certificate rejection"
    }
    $recovery = @($rows | Where-Object { $_.test.StartsWith('recovery-after-') })
    if ($recovery.Count -ne 4 -or @($recovery | Where-Object { $null -ne $_.exception }).Count) {
        throw "$version failed recovery checks"
    }
    Write-Output "PASS $version native failure/recovery comparison: 9 results"
    $rows | Select-Object version, test, elapsedMs, headersMs | Format-Table -AutoSize
}

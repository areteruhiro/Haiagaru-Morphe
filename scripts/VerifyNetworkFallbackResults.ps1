param([string]$ResultsDirectory = 'build/verification')
$ErrorActionPreference = 'Stop'
foreach ($version in @('191', '226')) {
    $logPath = Join-Path $ResultsDirectory "network$version-fallback-final.log"
    $rows = @(Get-Content -LiteralPath $logPath | Where-Object { $_.StartsWith('{') } |
        ForEach-Object { $_ | ConvertFrom-Json })
    if ($rows.Count -ne 5 -or @($rows | Where-Object { -not $_.pass }).Count) {
        throw "$version requires five successful fixture requests"
    }
    foreach ($row in $rows) {
        if (($row.order -join ',') -ne ($row.dnsOrder -join ',')) {
            throw "$version DNS order was not verified: $($row.test)"
        }
        if (-not $row.test.StartsWith('unresponsive-first-')) { continue }
        $bad = @($row.attempts | Where-Object { $_.ip -eq '127.0.0.2' })
        $good = @($row.attempts | Where-Object { $_.ip -eq '127.0.0.1' })
        if ($bad.Count -ne 1 -or $good.Count -ne 1) { throw "$version missing both connect attempts" }
        if ($version -eq '191') {
            if ($row.elapsedMs -lt 14000 -or $good[0].startMs -lt $bad[0].endMs) {
                throw '191 did not wait for the first candidate to finish'
            }
        } elseif ($row.elapsedMs -ge 2000 -or $good[0].startMs -ge $bad[0].endMs) {
            throw '226 did not start the second candidate before the first finished'
        }
    }
    Write-Output "PASS $version DNS ordering, connection attempts, completion and recovery"
    $rows | Select-Object version, test, elapsedMs | Format-Table -AutoSize
}

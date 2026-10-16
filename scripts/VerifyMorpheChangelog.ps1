param([string]$Path = 'CHANGELOG.md')
$ErrorActionPreference = 'Stop'
$source = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $Path))
# Exact VERSION_HEADING grammar in Morphe Manager's ChangelogParser.kt.
$heading = '^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)'
$entries = @()
$current = $null
foreach ($line in ($source -split '\r?\n')) {
    $match = [regex]::Match($line, $heading, 'IgnoreCase')
    if ($match.Success) {
        $version = $match.Groups[1].Value
        if (-not $version) { $version = $match.Groups[2].Value }
        $current = @{ Version = $version; Date = $match.Groups[3].Value; Scoped = 0 }
        $null = [datetime]::ParseExact($current.Date, 'yyyy-MM-dd', [cultureinfo]::InvariantCulture)
        $entries += $current
    } elseif ($line -match '^## ') {
        throw "Morphe cannot parse heading: $line"
    } elseif ($line -match '^\* ') {
        if ($null -eq $current -or $line -notmatch '^\* \*\*ChMate:\*\*') {
            throw "Morphe ChMate filter cannot recognize bullet: $line"
        }
        $current.Scoped++
    }
}
if ($entries.Count -lt 80 -or $entries[0].Version -ne '1.8.8') { throw 'Missing changelog history/latest version' }
if (@($entries | Where-Object { $_.Scoped -eq 0 }).Count) { throw 'Empty ChMate entries' }
if (@($entries.Version | Select-Object -Unique).Count -ne $entries.Count) { throw 'Duplicate versions' }
if ($source -match '\]\(docs/') { throw 'Relative links will not resolve inside Morphe' }
Write-Output "PASS: Morphe heading/date parsing and ChMate filtering for $($entries.Count) release entries; latest $($entries[0].Version)."

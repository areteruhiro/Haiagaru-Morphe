param([string]$Path = 'CHANGELOG.md')
$ErrorActionPreference = 'Stop'

# Formatting only: preserve versions, channels and the original release text.
# Dates are UTC publication dates; deleted releases fall back to their local tag.
$releaseJson = & gh api 'repos/areteruhiro/Haiagaru-Morphe/releases?per_page=100'
if ($LASTEXITCODE -ne 0) { throw 'Cannot read release dates' }
$dates = @{}
foreach ($release in ($releaseJson | ConvertFrom-Json)) {
    $version = $release.tag_name -replace '^v', ''
    if (-not $dates.ContainsKey($version)) {
        $dates[$version] = ([datetime]$release.published_at).ToUniversalTime().ToString('yyyy-MM-dd')
    }
}
$dates['1.2.0'] = $dates['1.2']
$dates['1.1.0'] = $dates['1.1']
# The 1.3.4 release/tag was removed; use its actual release commit date.
$dates['1.3.4'] = (& git show -s --format=%cs ef12ca45).Trim()
$source = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $Path))
$result = [regex]::Replace($source, '(?m)^## (?:\\?\[([^\]]+)\]|([0-9][^\s]*)(?: \([^\r\n]*\))?)\r?$', {
    param($match)
    $version = $match.Groups[1].Value
    if (-not $version) { $version = $match.Groups[2].Value }
    if (-not $dates.ContainsKey($version)) {
        $date = & git log -1 --format=%cs $version --
        if ($LASTEXITCODE -ne 0 -or $date -notmatch '^\d{4}-\d{2}-\d{2}$') {
            throw "No verified date for $version"
        }
        $dates[$version] = $date.Trim()
    }
    "## $version ($($dates[$version]))"
})
$result = [regex]::Replace($result, '(?m)^\* (?!\*\*ChMate:\*\*)', '* **ChMate:** ')
$result = $result.Replace('](docs/', '](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/')
[IO.File]::WriteAllText((Resolve-Path -LiteralPath $Path), $result, [Text.UTF8Encoding]::new($false))
Write-Output 'Formatted Morphe version/date headings and ChMate scopes without changing release versions.'

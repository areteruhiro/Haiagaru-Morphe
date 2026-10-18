param(
    [Parameter(Mandatory=$true)][ValidateSet('master','main','dev')][string]$Branch,
    [Parameter(Mandatory=$true)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version,
    [Parameter(Mandatory=$true)][string]$ExpectedParent,
    [Parameter(Mandatory=$true)][string[]]$Paths
)
$ErrorActionPreference='Stop'
$repo='areteruhiro/Haiagaru-Morphe'
function Api([string]$endpoint,$body=$null){
    if($null -eq $body){$result=& gh api "repos/$repo/$endpoint"}
    else {$result=$body | ConvertTo-Json -Depth 15 -Compress | & gh api "repos/$repo/$endpoint" --method POST --input -}
    if($LASTEXITCODE -ne 0){throw "GitHub API failed: $endpoint"}
    return ($result | ConvertFrom-Json)
}
$ref=Api "git/ref/heads/$Branch"
if($ref.object.sha -ne $ExpectedParent){throw 'Remote branch advanced. Review before publishing.'}
$parent=Api "git/commits/$ExpectedParent"
$entries=@()
foreach($path in $Paths){
    if($path -notmatch '^(docs/[^\\]+\.md|README\.md|CHANGELOG\.md|release-notes-\d+\.\d+\.\d+\.md|patches-bundle(?:-pre|-\d+\.\d+\.\d+)?\.json)$'){throw "Not a release documentation/metadata path: $path"}
    $content=[IO.File]::ReadAllText((Join-Path (Get-Location) $path))
    if($path -like 'patches-bundle*.json'){
        # PowerShell 7.5+ otherwise converts ISO text to DateTime before regex validation.
        try{$metadata=$content | ConvertFrom-Json -DateKind String}catch{throw "Invalid Morphe metadata JSON: $path"}
        if($metadata.version -ne $Version){throw "Metadata version '$($metadata.version)' does not match requested version '$Version': $path"}
        if($metadata.created_at -notmatch '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$'){
            throw "Morphe created_at must be timezone-free YYYY-MM-DDTHH:mm:ss (no Z or offset): $path"
        }
        $parsed=[datetime]::MinValue
        $valid=[datetime]::TryParseExact($metadata.created_at,'yyyy-MM-ddTHH:mm:ss',[Globalization.CultureInfo]::InvariantCulture,[Globalization.DateTimeStyles]::None,[ref]$parsed)
        if(-not $valid){throw "Invalid Morphe created_at date/time: $path"}
    }
    $entries+=@{path=$path;mode='100644';type='blob';content=$content}
}
$tree=Api 'git/trees' @{base_tree=$parent.tree.sha;tree=$entries}
$commit=Api 'git/commits' @{message="release: update stable and preview sources to Haiagaru $Version";tree=$tree.sha;parents=@($ExpectedParent)}
$body=@{sha=$commit.sha;force=$false}|ConvertTo-Json -Compress
$result=$body | & gh api "repos/$repo/git/refs/heads/$Branch" --method PATCH --input -
if($LASTEXITCODE -ne 0){throw "Failed to advance $Branch"}
Write-Output "Updated $Branch -> $($commit.sha) (existing history preserved)"

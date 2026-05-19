$root = "C:\Users\Justin\AndroidStudioProjects\InvoiceApp"
$target = Join-Path $root "PLAYSTORE_SUBMISSION_DOSSIER.md"
$placeholder = "[CODE_PLACEHOLDER]"

$template = Get-Content $target -Raw
$codebase = New-Object System.Text.StringBuilder

function Append-File($relPath, $lang) {
    $fullPath = Join-Path $root $relPath
    if (Test-Path $fullPath) {
        [void]$codebase.AppendLine("### $relPath")
        [void]$codebase.AppendLine("```$lang")
        [void]$codebase.AppendLine((Get-Content $fullPath -Raw))
        [void]$codebase.AppendLine("```")
        [void]$codebase.AppendLine()
    }
}

# Root and App files
Append-File "build.gradle.kts" "kotlin-dsl"
Append-File "settings.gradle.kts" "kotlin-dsl"
Append-File "gradle\libs.versions.toml" "toml"
Append-File "app\build.gradle.kts" "kotlin-dsl"
Append-File "shared\build.gradle.kts" "kotlin-dsl"
Append-File "composeApp\build.gradle.kts" "kotlin-dsl"
Append-File "app\src\main\AndroidManifest.xml" "xml"

# Source directories to scan
$sourceDirs = @(
    (Join-Path $root "app\src\main\java\com\fordham\toolbelt"),
    (Join-Path $root "shared\src"),
    (Join-Path $root "composeApp\src"),
    (Join-Path $root "iosApp\iosApp")
)

foreach ($dir in $sourceDirs) {
    if (Test-Path $dir) {
        $files = Get-ChildItem -Path $dir -Include *.kt,*.swift,*.plist -Recurse
        foreach ($f in $files) {
            $relPath = $f.FullName.Substring($root.Length + 1)
            $lang = "kotlin"
            if ($f.Extension -eq ".swift") { $lang = "swift" }
            elseif ($f.Extension -eq ".plist") { $lang = "xml" }
            
            Append-File $relPath $lang
        }
    }
}

$finalContent = $template.Replace($placeholder, $codebase.ToString())
Set-Content $target $finalContent
Write-Output "Successfully aggregated files into $target"

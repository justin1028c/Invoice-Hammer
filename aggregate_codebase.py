import os

root = r"C:\Users\Justin\AndroidStudioProjects\InvoiceApp"
target = os.path.join(root, "CROSS_PLATFORM_AUDIT.md")
# Use a marker system instead of a single-use placeholder
START_MARKER = "## 🏗️ CODEBASE AGGREGATE"
END_MARKER = "<!-- END_AGGREGATE -->"

if not os.path.exists(target):
    with open(target, 'w', encoding='utf-8') as f:
        f.write(f"# CROSS-PLATFORM ARCHITECTURAL AUDIT - INVOICE HAMMER\n\n{START_MARKER}\n[CODE_PLACEHOLDER]\n{END_MARKER}\n")

with open(target, 'r', encoding='utf-8') as f:
    content = f.read()

codebase = []

def append_file(rel_path, lang):
    full_path = os.path.join(root, rel_path)
    if os.path.exists(full_path):
        print(f"Appending: {rel_path}")
        codebase.append(f"### {rel_path}")
        codebase.append(f"```{lang}")
        with open(full_path, 'r', encoding='utf-8') as f:
            codebase.append(f.read())
        codebase.append("```")
        codebase.append("")

# Root and App files
append_file("build.gradle.kts", "kotlin-dsl")
append_file("settings.gradle.kts", "kotlin-dsl")
append_file("gradle/libs.versions.toml", "toml")
append_file("app/build.gradle.kts", "kotlin-dsl")
append_file("shared/build.gradle.kts", "kotlin-dsl")
append_file("composeApp/build.gradle.kts", "kotlin-dsl")
append_file("app/src/main/AndroidManifest.xml", "xml")
append_file(".gitignore", "gitignore")

# iOS build handoff + bootstrap artifacts (XcodeGen-driven one-command build for James)
append_file("iosApp/project.yml", "yaml")
append_file("iosApp/bootstrap_ios.sh", "bash")
append_file("iosApp/READ_ME_FIRST_JAMES.md", "markdown")
append_file("iosApp/IOS_BUILD_HANDOFF.md", "markdown")

# Directories to scan for source code
source_dirs = [
    os.path.join(root, "app", "src", "main", "java", "com", "fordham", "toolbelt"),
    os.path.join(root, "shared", "src"),
    os.path.join(root, "composeApp", "src"),
    os.path.join(root, "iosApp", "iosApp")
]

for d in source_dirs:
    for dirpath, dirnames, filenames in os.walk(d):
        for filename in filenames:
            if (filename.endswith(".kt")
                or filename.endswith(".swift")
                or filename.endswith(".plist")
                or filename.endswith(".entitlements")):
                full_path = os.path.join(dirpath, filename)
                rel_path = os.path.relpath(full_path, root)

                lang = "kotlin"
                if filename.endswith(".swift"): lang = "swift"
                elif filename.endswith(".plist") or filename.endswith(".entitlements"): lang = "xml"

                append_file(rel_path, lang)

new_code_section = "\n".join(codebase)

if START_MARKER in content:
    # If the file already has the aggregate section, replace it
    header = content.split(START_MARKER)[0]
    # We assume the file might have stuff after the aggregate too, but for this audit it's usually the end
    footer = content.split(END_MARKER)[1] if END_MARKER in content else ""
    final_content = f"{header}{START_MARKER}\n\n{new_code_section}\n{END_MARKER}{footer}"
else:
    # Fallback to the old placeholder if markers aren't there
    final_content = content.replace("[CODE_PLACEHOLDER]", new_code_section)

with open(target, 'w', encoding='utf-8') as f:
    f.write(final_content)

print(f"Successfully aggregated files into {target}")

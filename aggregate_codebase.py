"""
Regenerate CROSS_PLATFORM_AUDIT.md with the full KMP codebase aggregate.
Run: python aggregate_codebase.py
Deploy: adb push CROSS_PLATFORM_AUDIT.md /storage/emulated/0/
"""

from __future__ import annotations

import os
from datetime import datetime, timezone

ROOT = os.path.dirname(os.path.abspath(__file__))
TARGET = os.path.join(ROOT, "CROSS_PLATFORM_AUDIT.md")
START_MARKER = "## 🏗️ CODEBASE AGGREGATE"
END_MARKER = "<!-- END_AGGREGATE -->"

SKIP_DIR_NAMES = {
    "build",
    ".gradle",
    ".kotlin",
    "intermediates",
    "generated",
    "schemas",
    "node_modules",
    ".git",
    ".idea",
    ".cursor",
    "bundletool",
    "tools",
}

SOURCE_EXTENSIONS = {
    ".kt": "kotlin",
    ".swift": "swift",
    ".plist": "xml",
    ".entitlements": "xml",
    ".mdc": "markdown",
}

ROOT_FILES = [
    ("build.gradle.kts", "kotlin-dsl"),
    ("settings.gradle.kts", "kotlin-dsl"),
    ("gradle/libs.versions.toml", "toml"),
    ("app/build.gradle.kts", "kotlin-dsl"),
    ("shared/build.gradle.kts", "kotlin-dsl"),
    ("composeApp/build.gradle.kts", "kotlin-dsl"),
    ("app/src/main/AndroidManifest.xml", "xml"),
    ("app/src/main/cpp/CMakeLists.txt", "cmake"),
    ("local.properties.example", "properties"),
    (".gitignore", "gitignore"),
    ("iosApp/project.yml", "yaml"),
    ("iosApp/bootstrap_ios.sh", "bash"),
    ("iosApp/READ_ME_FIRST_JAMES.md", "markdown"),
    ("iosApp/IOS_BUILD_HANDOFF.md", "markdown"),
]

CURSOR_RULE_FILES = [
    ".cursor/rules/kmp-ios-parity.mdc",
    ".cursor/rules/kmp-shared-module.mdc",
    ".cursor/rules/kmp-compose-ui.mdc",
]

SOURCE_ROOTS = [
    os.path.join("app", "src"),
    os.path.join("shared", "src"),
    os.path.join("composeApp", "src"),
    os.path.join("iosApp", "iosApp"),
]


def should_skip_dir(dirname: str) -> bool:
    return dirname in SKIP_DIR_NAMES or dirname.startswith(".")


def append_file(codebase: list[str], rel_path: str, lang: str) -> None:
    full_path = os.path.join(ROOT, rel_path)
    if not os.path.isfile(full_path):
        return
    print(f"Appending: {rel_path}")
    codebase.append(f"### {rel_path}")
    codebase.append(f"```{lang}")
    with open(full_path, encoding="utf-8", errors="replace") as handle:
        codebase.append(handle.read())
    codebase.append("```")
    codebase.append("")


def collect_source_files() -> list[tuple[str, str]]:
    collected: list[tuple[str, str]] = []
    for source_root in SOURCE_ROOTS:
        abs_root = os.path.join(ROOT, source_root)
        if not os.path.isdir(abs_root):
            continue
        for dirpath, dirnames, filenames in os.walk(abs_root):
            dirnames[:] = [d for d in dirnames if not should_skip_dir(d)]
            for filename in sorted(filenames):
                ext = os.path.splitext(filename)[1].lower()
                lang = SOURCE_EXTENSIONS.get(ext)
                if lang is None:
                    continue
                full_path = os.path.join(dirpath, filename)
                rel_path = os.path.relpath(full_path, ROOT).replace("\\", "/")
                collected.append((rel_path, lang))
    collected.sort(key=lambda item: item[0].lower())
    return collected


def build_aggregate_section() -> str:
    codebase: list[str] = []
    generated_at = datetime.now(timezone.utc).astimezone().isoformat()
    codebase.append(f"_Aggregate generated: {generated_at}_")
    codebase.append("")

    for rel_path, lang in ROOT_FILES:
        append_file(codebase, rel_path, lang)

    for rel_path in CURSOR_RULE_FILES:
        append_file(codebase, rel_path, "markdown")

    for rel_path, lang in collect_source_files():
        append_file(codebase, rel_path, lang)

    return "\n".join(codebase)


def ensure_target_template() -> str:
    if os.path.exists(TARGET):
        with open(TARGET, encoding="utf-8") as handle:
            return handle.read()
    header = (
        "# CROSS-PLATFORM ARCHITECTURAL AUDIT - INVOICE HAMMER\n\n"
        f"{START_MARKER}\n\n"
        "[CODE_PLACEHOLDER]\n\n"
        f"{END_MARKER}\n"
    )
    with open(TARGET, "w", encoding="utf-8") as handle:
        handle.write(header)
    return header


def main() -> None:
    content = ensure_target_template()
    new_code_section = build_aggregate_section()

    if START_MARKER in content:
        header = content.split(START_MARKER)[0]
        footer = content.split(END_MARKER)[1] if END_MARKER in content else ""
        final_content = f"{header}{START_MARKER}\n\n{new_code_section}\n{END_MARKER}{footer}"
    else:
        final_content = content.replace("[CODE_PLACEHOLDER]", new_code_section)

    with open(TARGET, "w", encoding="utf-8") as handle:
        handle.write(final_content)

    size_mb = os.path.getsize(TARGET) / (1024 * 1024)
    print(f"Successfully aggregated into {TARGET} ({size_mb:.2f} MB)")


if __name__ == "__main__":
    main()

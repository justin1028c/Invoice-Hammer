import re
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "shared/src/commonMain/kotlin/com/fordham/toolbelt/data/implementation"
files = [
    "RoomClientRepository.kt",
    "RoomInvoiceRepository.kt",
    "RoomReceiptRepository.kt",
    "RoomJobNoteRepository.kt",
    "RoomPhotoRepository.kt",
    "SupplierRepositoryImpl.kt",
]

for name in files:
    path = root / name
    if not path.exists():
        continue
    tag = name.replace(".kt", "")
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    out: list[str] = []
    for line in lines:
        out.append(line)
        if "} catch (e: Exception) {" in line:
            next_line = lines[len(out)] if len(out) < len(lines) else ""
            if "logRepositoryFailure" not in next_line:
                indent = re.match(r"(\s*)", next_line).group(1) if next_line else "        "
                out.append(f'{indent}logRepositoryFailure("{tag}", "repository", e)\n')
    text = "".join(out)
    if f'private const val TAG = "{tag}"' not in text and tag != "SupplierRepositoryImpl":
        text = text.rstrip() + f'\n\nprivate const val TAG = "{tag}"\n'
    path.write_text(text, encoding="utf-8")
    print("updated", name)

import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

with open("CROSS_PLATFORM_AUDIT.md", "r", encoding="utf-8") as f:
    lines = f.readlines()

print(f"Total lines: {len(lines)}")
print("Headers:")
for i, line in enumerate(lines[:1000]):
    if line.startswith("#"):
        print(f"{i+1}: {line.strip()}")

# Let's also check if there are other headers in the file:
print("\nHeaders in the entire file:")
for i, line in enumerate(lines):
    if line.startswith("# ") or line.startswith("## ") or line.startswith("### "):
        # only print if it contains words, not code comments inside markdown code blocks
        # a simple heuristic: if it has '#' and we are not in a code block
        pass

# Let's count how many headers of each type
h1 = 0
h2 = 0
for i, line in enumerate(lines):
    if line.startswith("# "):
        h1 += 1
        print(f"H1 at {i+1}: {line.strip()}")
    elif line.startswith("## "):
        h2 += 1
        if h2 < 30:
            print(f"  H2 at {i+1}: {line.strip()}")

print("\n--- Last 50 lines ---")
for i, line in enumerate(lines[-50:]):
    print(f"{len(lines)-50+i+1}: {line.strip()}")

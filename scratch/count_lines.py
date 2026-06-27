import os

def find_large_files(root_dir, line_threshold=300):
    candidates = []
    for root, dirs, files in os.walk(root_dir):
        # Exclude build, .gradle, .git directories
        if any(ignored in root for ignored in ["build", ".gradle", ".git", ".idea", "node_modules"]):
            continue
        for file in files:
            if file.endswith(".kt"):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, "r", encoding="utf-8") as f:
                        line_count = len(f.readlines())
                    if line_count > line_threshold:
                        relative_path = os.path.relpath(filepath, root_dir)
                        candidates.append((relative_path, line_count))
                except Exception as e:
                    pass
    
    candidates.sort(key=lambda x: x[1], reverse=True)
    print("\nKotlin files exceeding {} lines:".format(line_threshold))
    print("{:<70} | {:<5}".format("File Path", "Lines"))
    print("-" * 80)
    for path, lines in candidates:
        print("{:<70} | {:<5}".format(path, lines))

if __name__ == "__main__":
    find_large_files("c:\\Users\\Justin\\AndroidStudioProjects\\InvoiceApp")

import os
import sys
import re
from fpdf import FPDF

class MarkdownPDF(FPDF):
    def __init__(self):
        super().__init__()
        self.set_margins(15, 15, 15)
        self.set_auto_page_break(auto=True, margin=15)

    def header(self):
        if self.page_no() > 1:
            self.set_font("helvetica", "I", 8)
            self.set_text_color(128, 128, 128)
            self.cell(0, 10, "Google Play Store Launch Audit - Invoice Hammer", align="R")
            self.ln(8)
            self.line(15, 18, 195, 18)
            self.ln(5)

    def footer(self):
        self.set_y(-15)
        self.set_font("helvetica", "I", 8)
        self.set_text_color(128, 128, 128)
        self.cell(0, 10, f"Page {self.page_no()}/{{nb}}", align="C")

def clean_non_ascii(text):
    # Map common non-ASCII characters to ASCII equivalents
    replacements = {
        "\u2014": "-",        # em dash
        "\u2013": "-",        # en dash
        "\u201c": '"',        # left double quote
        "\u201d": '"',        # right double quote
        "\u2018": "'",        # left single quote
        "\u2019": "'",        # right single quote
        "\u2022": "-",        # bullet point
        "\u2192": "->",       # right arrow
        "\u2265": ">=",       # greater than or equal
        "\u2264": "<=",       # less than or equal
        # Emojis & misc icons
        "🔨": "Tool",
        "🎉": "Celebration",
        "💡": "Tip",
        "⚙️": "Settings",
        "🍏": "iOS",
        "🛡️": "Shield",
        "❌": "Error",
        "✅": "Success",
        "🔍": "Audit"
    }
    for char, replacement in replacements.items():
        text = text.replace(char, replacement)
    
    # Remove any other non-latin1 characters
    text = text.encode("latin-1", "replace").decode("latin-1")
    return text

def clean_md_links(text):
    # Replace markdown links like [text](file:///url) with just the text or plain text representation
    text = re.sub(r'\[([^\]]+)\]\(file:///[^\)]+\)', r'\1', text)
    text = re.sub(r'\[([^\]]+)\]\([^\)]+\)', r'\1', text)
    # Replace backticks
    text = text.replace("`", "")
    return text

def convert_md_to_pdf(md_path, pdf_path):
    if not os.path.exists(md_path):
        print(f"Error: Source markdown file not found: {md_path}")
        sys.exit(1)

    with open(md_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    pdf = MarkdownPDF()
    pdf.add_page()
    pdf.set_font("helvetica", size=10)

    in_code_block = False

    for line in lines:
        stripped = line.strip()

        # Handle code blocks
        if stripped.startswith("```"):
            in_code_block = not in_code_block
            continue

        if in_code_block:
            pdf.set_font("courier", size=9)
            pdf.set_text_color(50, 50, 50)
            cleaned_line = clean_non_ascii(line).rstrip()
            pdf.multi_cell(0, 5, cleaned_line, new_x="LMARGIN", new_y="NEXT")
            continue

        # Reset formatting default
        pdf.set_font("helvetica", size=10)
        pdf.set_text_color(0, 0, 0)

        # Skip empty lines
        if not stripped:
            pdf.ln(3)
            continue

        # Strip markdown links and clean text
        cleaned_line = clean_md_links(line)
        cleaned_line = clean_non_ascii(cleaned_line).rstrip()

        # Handle headings
        if cleaned_line.startswith("# "):
            pdf.ln(5)
            pdf.set_font("helvetica", "B", 16)
            pdf.set_text_color(242, 108, 37) # Brand Orange color approximate
            pdf.multi_cell(0, 10, cleaned_line[2:], new_x="LMARGIN", new_y="NEXT")
            pdf.ln(3)
        elif cleaned_line.startswith("## "):
            pdf.ln(4)
            pdf.set_font("helvetica", "B", 12)
            pdf.set_text_color(31, 78, 121) # Dark blue accent
            pdf.multi_cell(0, 8, cleaned_line[3:], new_x="LMARGIN", new_y="NEXT")
            pdf.ln(2)
        elif cleaned_line.startswith("### "):
            pdf.ln(3)
            pdf.set_font("helvetica", "B", 10)
            pdf.multi_cell(0, 6, cleaned_line[4:], new_x="LMARGIN", new_y="NEXT")
            pdf.ln(1)
        # Handle blockquotes/alerts
        elif cleaned_line.startswith("> "):
            pdf.set_font("helvetica", "I", 10)
            pdf.set_text_color(100, 100, 100)
            content = cleaned_line[2:].replace("[!IMPORTANT]", "IMPORTANT:").replace("[!NOTE]", "NOTE:")
            # Draw blockquote
            pdf.multi_cell(0, 5, content, new_x="LMARGIN", new_y="NEXT")
        # Handle lists
        elif cleaned_line.lstrip().startswith("- ") or cleaned_line.lstrip().startswith("* "):
            bullet_indent = len(cleaned_line) - len(cleaned_line.lstrip())
            content = cleaned_line.lstrip()[2:]
            pdf.set_font("helvetica", size=10)
            # Indent slightly and print bullet point
            pdf.set_x(15 + bullet_indent + 5)
            pdf.multi_cell(0, 5, f"-  {content}", new_x="LMARGIN", new_y="NEXT")
        # Handle numbered lists
        elif re.match(r'^\s*\d+\.\s', cleaned_line):
            match = re.match(r'^(\s*\d+\.\s)(.*)', cleaned_line)
            bullet_indent = len(match.group(1)) - len(match.group(1).lstrip())
            content = match.group(2)
            pdf.set_font("helvetica", size=10)
            pdf.set_x(15 + bullet_indent + 5)
            pdf.multi_cell(0, 5, f"{match.group(1).strip()}  {content}", new_x="LMARGIN", new_y="NEXT")
        # Normal paragraph
        else:
            pdf.multi_cell(0, 5, cleaned_line, new_x="LMARGIN", new_y="NEXT")

    os.makedirs(os.path.dirname(pdf_path), exist_ok=True)
    pdf.output(pdf_path)
    print(f"Successfully generated PDF: {pdf_path}")

if __name__ == "__main__":
    md_file = r"C:\Users\Justin\.gemini\antigravity-ide\brain\925d3e6e-06cc-4664-adf3-6c251726a8f0\google_play_launch_audit.md"
    pdf_file = r"c:\Users\Justin\AndroidStudioProjects\InvoiceApp\dist\google_play_launch_audit.pdf"
    
    convert_md_to_pdf(md_file, pdf_file)

from __future__ import annotations

from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import Image as RLImage
from reportlab.platypus import PageBreak, Paragraph, Preformatted, SimpleDocTemplate, Spacer

ROOT = Path(r"d:\Prep\System_Design")


def load_font(size: int, bold: bool = False):
    font_candidates = []
    if bold:
        font_candidates.extend([
            r"C:\Windows\Fonts\arialbd.ttf",
            r"C:\Windows\Fonts\segoeuib.ttf",
        ])
    else:
        font_candidates.extend([
            r"C:\Windows\Fonts\arial.ttf",
            r"C:\Windows\Fonts\segoeui.ttf",
        ])

    for candidate in font_candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default()


def draw_centered_text(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], text: str, font, fill: str):
    left, top, right, bottom = box
    text_box = draw.multiline_textbbox((0, 0), text, font=font, spacing=6, align="center")
    width = text_box[2] - text_box[0]
    height = text_box[3] - text_box[1]
    x = left + (right - left - width) / 2
    y = top + (bottom - top - height) / 2
    draw.multiline_text((x, y), text, font=font, fill=fill, spacing=6, align="center")


def arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], fill: str = "#1d4f91", width: int = 7):
    draw.line([start, end], fill=fill, width=width)
    ex, ey = end
    draw.polygon([(ex, ey), (ex - 22, ey - 12), (ex - 22, ey + 12)], fill=fill)


def day2_image(path: Path):
    img = Image.new("RGB", (1600, 900), "#f4f7fb")
    draw = ImageDraw.Draw(img)
    title = load_font(52, bold=True)
    subtitle = load_font(24)
    box_title = load_font(28, bold=True)
    box_body = load_font(20)

    draw.text((60, 40), "Day 2: Networking, Scalability, and Delivery", font=title, fill="#163b66")
    draw.text((60, 110), "How requests move from users to reliable backend systems", font=subtitle, fill="#43668f")

    boxes = [
        ((60, 270, 300, 590), "Client", "Browser or app\ninitiates request"),
        ((350, 220, 620, 640), "DNS", "Resolves domain to\nservice IP address"),
        ((680, 180, 980, 680), "Load Balancer", "Distributes traffic\nacross healthy nodes"),
        ((1040, 140, 1320, 350), "Cache/CDN", "Serves hot content\ncloser to users"),
        ((1040, 390, 1320, 600), "App Cluster", "Handles business\nlogic on many nodes"),
        ((1370, 260, 1540, 540), "Storage", "Durable data layer\nDB / object store"),
    ]

    for box, title_text, body in boxes:
        draw.rounded_rectangle(box, radius=28, fill="#e6eef7", outline="#24548f", width=4)
        left, top, right, bottom = box
        draw.rectangle((left, top, right, top + 56), fill="#24548f")
        draw_centered_text(draw, (left, top + 6, right, top + 50), title_text, box_title, "#ffffff")
        draw_centered_text(draw, (left + 18, top + 76, right - 18, bottom - 18), body, box_body, "#17365d")

    arrow(draw, (300, 430), (350, 430))
    arrow(draw, (620, 430), (680, 430))
    arrow(draw, (980, 290), (1040, 245))
    arrow(draw, (980, 520), (1040, 495))
    arrow(draw, (1320, 495), (1370, 400))

    notes = [
        (80, 700, "TCP: ordered and reliable"),
        (470, 700, "DNS cache reduces repeated lookups"),
        (960, 700, "Reverse proxy + LB remove hotspots"),
    ]
    note_font = load_font(20, bold=True)
    for x, y, text in notes:
        draw.rounded_rectangle((x, y, x + 360, y + 62), radius=18, fill="#dce8f5", outline="#7ca0c8", width=2)
        draw_centered_text(draw, (x + 10, y + 8, x + 350, y + 54), text, note_font, "#1b416f")

    img.save(path)


def day3_image(path: Path):
    img = Image.new("RGB", (1600, 900), "#f7f8fb")
    draw = ImageDraw.Draw(img)
    title = load_font(52, bold=True)
    subtitle = load_font(24)
    box_title = load_font(28, bold=True)
    box_body = load_font(20)

    draw.text((60, 40), "Day 3: Databases, Consistency, and Partitioning", font=title, fill="#20345b")
    draw.text((60, 110), "How large systems split data without losing correctness", font=subtitle, fill="#586a8d")

    draw.rounded_rectangle((80, 300, 320, 600), radius=28, fill="#e9ecf7", outline="#4d5f8a", width=4)
    draw.rectangle((80, 300, 320, 356), fill="#4d5f8a")
    draw_centered_text(draw, (80, 306, 320, 348), "Keys", box_title, "#ffffff")
    draw_centered_text(draw, (100, 390, 300, 580), "user:1\norder:14\nsession:87\ncart:92", box_body, "#22304d")

    draw.ellipse((430, 210, 1130, 810), outline="#214d86", width=8)
    draw_centered_text(draw, (620, 118, 940, 148), "Consistent Hash Ring", load_font(30, bold=True), "#214d86")

    ring_nodes = [
        ((760, 210), "Shard A"),
        ((1040, 360), "Shard B"),
        ((980, 660), "Shard C"),
        ((520, 660), "Shard D"),
        ((430, 380), "Virtual nodes spread load"),
    ]

    for (x, y), label in ring_nodes:
        if label.startswith("Virtual"):
            draw.rounded_rectangle((x - 130, y - 30, x + 130, y + 30), radius=14, fill="#dde8f7", outline="#7b98bf", width=2)
            draw_centered_text(draw, (x - 120, y - 22, x + 120, y + 22), label, load_font(18, bold=True), "#23466f")
        else:
            draw.ellipse((x - 58, y - 58, x + 58, y + 58), fill="#dfe7f4", outline="#214d86", width=4)
            draw_centered_text(draw, (x - 48, y - 22, x + 48, y + 22), label, load_font(22, bold=True), "#193a64")

    arrow(draw, (320, 450), (430, 450), fill="#214d86")
    draw.rounded_rectangle((1190, 280, 1510, 610), radius=28, fill="#e9ecf7", outline="#4d5f8a", width=4)
    draw.rectangle((1190, 280, 1510, 336), fill="#4d5f8a")
    draw_centered_text(draw, (1190, 286, 1510, 328), "Data Outcomes", box_title, "#ffffff")
    draw_centered_text(draw, (1210, 370, 1490, 590), "Sharding\nReplication\nRebalancing\nMinimal key movement", box_body, "#22304d")
    arrow(draw, (1130, 450), (1190, 450), fill="#214d86")

    img.save(path)


def read_code(path: Path) -> str:
    return path.read_text(encoding="utf-8").strip()


def build_pdf(path: Path, title: str, subtitle: str, image_path: Path, sections: Iterable[tuple[str, list[str]]], code_blocks: Iterable[tuple[str, str]]):
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name="TitleCustom", parent=styles["Title"], textColor=colors.HexColor("#173b67"), spaceAfter=18))
    styles.add(ParagraphStyle(name="SubTitleCustom", parent=styles["Heading3"], textColor=colors.HexColor("#4d6f96"), spaceAfter=16))
    styles.add(ParagraphStyle(name="SectionCustom", parent=styles["Heading2"], textColor=colors.HexColor("#1e4d8a"), spaceBefore=12, spaceAfter=8))
    styles.add(ParagraphStyle(name="BodyCustom", parent=styles["BodyText"], leading=15, spaceAfter=6))
    styles.add(ParagraphStyle(name="BulletCustom", parent=styles["BodyText"], leading=15, leftIndent=14, bulletIndent=0, spaceAfter=4))
    code_style = ParagraphStyle(
        name="CodeCaption",
        parent=styles["Heading3"],
        textColor=colors.HexColor("#335c8a"),
        spaceBefore=12,
        spaceAfter=6,
    )

    doc = SimpleDocTemplate(str(path), pagesize=A4, rightMargin=42, leftMargin=42, topMargin=42, bottomMargin=42)
    story = [
        Paragraph(title, styles["TitleCustom"]),
        Paragraph(subtitle, styles["SubTitleCustom"]),
        RLImage(str(image_path), width=6.8 * inch, height=3.8 * inch),
        Spacer(1, 18),
    ]

    for heading, bullets in sections:
        story.append(Paragraph(heading, styles["SectionCustom"]))
        for bullet in bullets:
            story.append(Paragraph(bullet, styles["BulletCustom"], bulletText="-"))
        story.append(Spacer(1, 8))

    story.append(PageBreak())
    story.append(Paragraph("Code Samples", styles["SectionCustom"]))
    for caption, code in code_blocks:
        story.append(Paragraph(caption, code_style))
        story.append(Preformatted(code, styles["Code"]))
        story.append(Spacer(1, 10))

    doc.build(story)


def main():
    day2_dir = ROOT / "Day-2"
    day3_dir = ROOT / "Day-3"

    day2_img = day2_dir / "Day2.png"
    day3_img = day3_dir / "Day3.png"
    day2_image(day2_img)
    day3_image(day3_img)

    build_pdf(
        day2_dir / "System_Design_Day_2.pdf",
        "Day 2: Networking, Scalability, and Delivery",
        "Notes built from the system design fundamentals around networking, traffic routing, caching, and availability.",
        day2_img,
        [
            ("Networking foundations", [
                "IP addresses identify machines on a network and make routing possible.",
                "The OSI model is a practical troubleshooting map: application, presentation, session, transport, network, data link, and physical.",
                "TCP favors reliability and ordered delivery; UDP favors low latency and minimal overhead.",
            ]),
            ("Name resolution and request entry", [
                "DNS translates human-readable domains into IP addresses using resolvers, root servers, TLD servers, and authoritative servers.",
                "Reverse proxies and load balancers often sit at the edge to terminate TLS, route traffic, and protect backend services.",
                "Caching DNS responses lowers repeated lookup cost and improves user-perceived latency.",
            ]),
            ("Scaling and delivery", [
                "Load balancing distributes traffic across healthy instances using algorithms such as round-robin or least connections.",
                "Clusters improve availability and throughput by coordinating multiple nodes toward the same goal.",
                "Caches, CDNs, and proxies reduce latency and absorb repeated read traffic before it reaches origin systems.",
                "Availability is usually expressed in nines, while scalability is the ability to add or remove resources based on demand.",
            ]),
            ("Storage basics", [
                "File, block, and object storage solve different problems and should not be treated as interchangeable.",
                "RAID, NAS, and distributed file systems help when throughput, durability, or recovery requirements become explicit design constraints.",
            ]),
        ],
        [
            ("Python: round-robin load balancer", read_code(day2_dir / "load-balancer-sample" / "python" / "load_balancer.py")),
            ("Java: round-robin load balancer", read_code(day2_dir / "load-balancer-sample" / "java" / "RoundRobinLoadBalancer.java")),
        ],
    )

    build_pdf(
        day3_dir / "System_Design_Day_3.pdf",
        "Day 3: Databases, Consistency, and Partitioning",
        "Notes built from the database and distributed data chapters: SQL/NoSQL, transactions, replication, sharding, and consistent hashing.",
        day3_img,
        [
            ("Database fundamentals", [
                "A DBMS manages persistence, schema, indexing, backup, and recovery around the raw data itself.",
                "SQL systems are usually better for joins, strict schema, and strong transactional guarantees; NoSQL systems are often better for flexible schema and horizontal scale.",
                "Indexes make reads faster but add storage overhead and write cost.",
            ]),
            ("Consistency and transactions", [
                "Normalization reduces redundancy, while denormalization improves read performance by duplicating data intentionally.",
                "ACID favors strong correctness and transactions; BASE accepts looser guarantees in exchange for scale and availability.",
                "CAP explains the partition-time consistency vs availability trade-off, and PACELC adds the normal-case latency vs consistency trade-off.",
                "Distributed transactions need coordination patterns such as two-phase commit, three-phase commit, or sagas.",
            ]),
            ("Partitioning and scaling data", [
                "Sharding horizontally partitions a dataset so each shard stores only part of the total data.",
                "Consistent hashing reduces key remapping when nodes are added or removed, especially when virtual nodes are used.",
                "Federation splits databases by function and exposes them as one logical system to users.",
            ]),
            ("Operational trade-offs", [
                "Replication improves read scaling and failover, but also introduces lag, conflict handling, and recovery concerns.",
                "Once data is distributed, rebalancing, cross-shard joins, and hotspot handling become first-class design problems.",
            ]),
        ],
        [
            ("Python: consistent hash ring", read_code(day3_dir / "consistent-hashing-sample" / "python" / "consistent_hashing.py")),
            ("Java: consistent hash ring", read_code(day3_dir / "consistent-hashing-sample" / "java" / "ConsistentHashRing.java")),
        ],
    )


if __name__ == "__main__":
    main()

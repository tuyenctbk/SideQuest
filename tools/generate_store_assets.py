#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_store_icon():
    src_path = "app/src/main/res/drawable/img_launcher_fg_1794444305032.jpg"
    size = (512, 512)
    if os.path.exists(src_path):
        src_img = Image.open(src_path).convert("RGBA")
        src_img = src_img.resize(size, Image.Resampling.LANCZOS)
    else:
        src_img = Image.new("RGBA", size, (13, 17, 23, 255))

    # Create squircle rounded mask with gold border
    mask = Image.new("L", size, 0)
    draw_mask = ImageDraw.Draw(mask)
    corner_radius = 100
    draw_mask.rounded_rectangle([0, 0, size[0], size[1]], radius=corner_radius, fill=255)

    # Base canvas
    icon = Image.new("RGBA", size, (0, 0, 0, 0))
    icon.paste(src_img, (0, 0), mask)

    # Add subtle golden border
    border_draw = ImageDraw.Draw(icon)
    border_draw.rounded_rectangle(
        [1, 1, size[0] - 2, size[1] - 2],
        radius=corner_radius,
        outline=(212, 175, 55, 230), # GuildGold
        width=4
    )
    # Inner glow accent
    border_draw.rounded_rectangle(
        [4, 4, size[0] - 5, size[1] - 5],
        radius=corner_radius - 3,
        outline=(0, 229, 255, 100), # CyberCyan glow
        width=2
    )

    os.makedirs("docs", exist_ok=True)
    icon.save("docs/ic_launcher-512.png", "PNG")
    icon.save("ic_launcher-512.png", "PNG")
    print("Saved ic_launcher-512.png (512x512)")

def create_feature_graphic():
    width, height = 1024, 500
    canvas = Image.new("RGB", (width, height), (10, 13, 20))

    hero_path = "app/src/main/res/drawable/img_hero_sidequest_1794444307373.jpg"
    if os.path.exists(hero_path):
        hero_img = Image.open(hero_path).convert("RGB")
        # Crop and fit to right 65% of banner
        hero_w = int(width * 0.72)
        hero_resized = hero_img.resize((hero_w, int(hero_w * (hero_img.height / hero_img.width))), Image.Resampling.LANCZOS)
        # Center vertically
        y_offset = (height - hero_resized.height) // 2
        canvas.paste(hero_resized, (width - hero_w + 40, y_offset))

    # Gradient overlay to blend left side smoothly
    gradient = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw_grad = ImageDraw.Draw(gradient)
    for x in range(width):
        if x < 380:
            alpha = 255
        elif x < 720:
            ratio = (x - 380) / (720 - 380)
            alpha = int(255 * (1.0 - ratio * 0.85))
        else:
            alpha = 40
        draw_grad.line([(x, 0), (x, height)], fill=(10, 13, 20, alpha))
    
    # Top and bottom vignette
    for y in range(80):
        alpha = int(200 * (1.0 - y / 80))
        draw_grad.line([(0, y), (width, y)], fill=(10, 13, 20, alpha))
        draw_grad.line([(0, height - 1 - y), (width, height - 1 - y)], fill=(10, 13, 20, alpha))

    canvas = Image.alpha_composite(canvas.convert("RGBA"), gradient).convert("RGB")
    draw = ImageDraw.Draw(canvas)

    # Fonts
    cinzel_path = "app/src/main/res/font/cinzel.ttf"
    space_path = "app/src/main/res/font/space_grotesk.ttf"
    jakarta_path = "app/src/main/res/font/plus_jakarta_sans.ttf"

    font_badge = ImageFont.truetype(space_path, 13) if os.path.exists(space_path) else ImageFont.load_default()
    font_title = ImageFont.truetype(cinzel_path, 54) if os.path.exists(cinzel_path) else ImageFont.load_default()
    font_sub = ImageFont.truetype(space_path, 20) if os.path.exists(space_path) else ImageFont.load_default()
    font_tag = ImageFont.truetype(jakarta_path, 15) if os.path.exists(jakarta_path) else ImageFont.load_default()

    # Badge Pill: REAL-WORLD AUDIO RPG
    badge_text = "✦ REAL-WORLD AUDIO RPG & TOUR GUIDE ✦"
    badge_bbox = font_badge.getbbox(badge_text)
    badge_w = badge_bbox[2] - badge_bbox[0]
    draw.rounded_rectangle([48, 68, 48 + badge_w + 24, 68 + 26], radius=13, fill=(35, 28, 10), outline=(212, 175, 55), width=1)
    draw.text((60, 72), badge_text, font=font_badge, fill=(245, 200, 66))

    # Main Title: SideQuest
    draw.text((48, 110), "SideQuest", font=font_title, fill=(255, 255, 255))
    
    # Gold accent line
    draw.rectangle([48, 185, 260, 188], fill=(212, 175, 55))

    # Subtitle: AI Dungeon Master Lore
    draw.text((48, 205), "Walk Your City. Live The Quest.", font=font_sub, fill=(0, 229, 255))

    # Bullet descriptions
    bullets = [
        "🗺️ Anchored to real parks, plaques & coffee shops",
        "🎙️ Spoken AI Dungeon Master narrating your route",
        "🛡️ Collect relics, solve riddles & build Guild XP",
        "⚡ Hands-free Pocket DM & Offline Audio Walking"
    ]
    y_pos = 252
    for b in bullets:
        draw.text((48, y_pos), b, font=font_tag, fill=(220, 225, 235))
        y_pos += 30

    # Add small persona badges on bottom right
    personas = [
        ("CYBERPUNK", (0, 229, 255)),
        ("VICTORIAN", (212, 175, 55)),
        ("ARCANE", (52, 211, 153))
    ]
    px = 640
    for name, col in personas:
        draw.rounded_rectangle([px, 440, px + 105, 472], radius=8, fill=(15, 20, 30), outline=col, width=1)
        draw.text((px + 12, 448), name, font=font_badge, fill=col)
        px += 118

    # Border frame
    draw.rectangle([0, 0, width - 1, height - 1], outline=(212, 175, 55), width=2)

    canvas.save("docs/feature_graphic.png", "PNG")
    canvas.save("feature_graphic.png", "PNG")
    print("Saved feature_graphic.png (1024x500)")

if __name__ == "__main__":
    create_store_icon()
    create_feature_graphic()


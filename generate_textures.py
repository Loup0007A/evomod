"""
Génère des textures placeholder 16x16 pour Evolution Mod.
Style pixel-art simple cohérent avec l'esthétique vanilla Minecraft :
chaque texture est construite à la main pixel par pixel (pas de filtre flou),
avec une palette de 4-6 couleurs et un léger bruit pour éviter l'aspect plat.
"""
from PIL import Image
import random

random.seed(42)  # déterministe pour reproductibilité

def noisy(base_rgb, amount=12):
    r, g, b = base_rgb
    return (
        max(0, min(255, r + random.randint(-amount, amount))),
        max(0, min(255, g + random.randint(-amount, amount))),
        max(0, min(255, b + random.randint(-amount, amount))),
    )

def save(img, path):
    img.save(path)
    print(f"  -> {path}")

# ─────────────────────────────────────────────────────────
# 1. BIOLUMINESCENT LEAVES — vert profond avec points cyan lumineux
# ─────────────────────────────────────────────────────────
def make_bioluminescent_leaves():
    img = Image.new("RGBA", (16, 16))
    base_dark = (22, 60, 38)
    base_mid = (34, 90, 58)
    glow_cyan = (120, 255, 220)
    glow_dim = (60, 180, 160)
    for y in range(16):
        for x in range(16):
            # motif de feuillage type vanilla : alternance + trous (alpha 0) pour le cutout
            is_leaf = (x + y * 2) % 5 != 0  # texture pleine, pas de trous (cube plein pour feuilles)
            c = noisy(base_mid if (x + y) % 3 == 0 else base_dark, 10)
            img.putpixel((x, y), (*c, 255))
    # points lumineux bioluminescents dispersés
    glow_points = [(2,3),(7,2),(11,5),(4,9),(13,11),(8,13),(1,12),(14,2),(6,6),(10,14)]
    for (x, y) in glow_points:
        img.putpixel((x, y), (*glow_cyan, 255))
    # halo léger autour de chaque point
    for (x, y) in glow_points:
        for dx, dy in [(-1,0),(1,0),(0,-1),(0,1)]:
            nx, ny = x+dx, y+dy
            if 0 <= nx < 16 and 0 <= ny < 16:
                existing = img.getpixel((nx, ny))
                blended = tuple(int(existing[i]*0.6 + glow_dim[i]*0.4) for i in range(3))
                img.putpixel((nx, ny), (*blended, 255))
    return img

# ─────────────────────────────────────────────────────────
# 2. MUTANT LOG — écorce brun-violet avec veines pulsantes
# ─────────────────────────────────────────────────────────
def make_mutant_log_side():
    img = Image.new("RGBA", (16, 16))
    bark_dark = (54, 36, 48)
    bark_mid = (74, 50, 64)
    vein_purple = (160, 70, 200)
    for y in range(16):
        for x in range(16):
            # rainures verticales façon écorce
            stripe = (x % 4 == 0 or x % 4 == 1)
            c = noisy(bark_mid if stripe else bark_dark, 8)
            img.putpixel((x, y), (*c, 255))
    # veines mutantes diagonales
    for i in range(16):
        x1, y1 = i, (i * 3) % 16
        img.putpixel((x1, y1), (*vein_purple, 255))
        x2 = (i + 6) % 16
        img.putpixel((x2, (i*5+2) % 16), (*noisy(vein_purple, 20), 255))
    return img

def make_mutant_log_top():
    img = Image.new("RGBA", (16, 16))
    ring_dark = (90, 60, 78)
    ring_light = (130, 90, 115)
    center_glow = (190, 120, 230)
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            dist = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            ring = int(dist) % 3
            base = ring_light if ring == 0 else ring_dark
            c = noisy(base, 6)
            img.putpixel((x, y), (*c, 255))
    # centre lumineux mutant
    for dx in range(-1, 2):
        for dy in range(-1, 2):
            x, y = int(cx) + dx, int(cy) + dy
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), (*center_glow, 255))
    return img

# ─────────────────────────────────────────────────────────
# 3. GENETIC CRYSTAL — améthyste/violet avec facettes
# ─────────────────────────────────────────────────────────
def make_genetic_crystal():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    deep_purple = (90, 30, 140)
    mid_purple = (140, 70, 200)
    light_purple = (200, 150, 255)
    highlight = (240, 220, 255)
    # forme de cristal en losange vertical
    shape = []
    for y in range(16):
        # largeur variable selon y pour former une pointe en haut et en bas
        if y < 3:
            half_width = y // 2
        elif y > 12:
            half_width = (15 - y) // 2
        else:
            half_width = 4 - abs(y - 7) // 3
        half_width = max(1, half_width)
        cx = 7
        for x in range(cx - half_width, cx + half_width + 1):
            if 0 <= x < 16:
                shape.append((x, y))
    for (x, y) in shape:
        # dégradé horizontal pour effet facette
        rel = (x - 7) / 5.0
        if rel < -0.3:
            base = deep_purple
        elif rel < 0.3:
            base = mid_purple
        else:
            base = light_purple
        img.putpixel((x, y), (*noisy(base, 10), 255))
    # ligne de highlight au centre
    for y in range(2, 14):
        if (7, y) in shape:
            img.putpixel((7, y), (*highlight, 200))
    return img

# ─────────────────────────────────────────────────────────
# 4. MUTANT MEAT — viande rouge/violette veinée de toxines
# ─────────────────────────────────────────────────────────
def make_mutant_meat():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    meat_red = (150, 50, 55)
    meat_dark = (110, 30, 40)
    fat_white = (230, 215, 200)
    toxin_green = (130, 200, 60)
    # forme grossière de morceau de viande (ovale irrégulier)
    cx, cy = 8, 8
    for y in range(16):
        for x in range(16):
            dist = (abs(x - cx) ** 1.3 / 7.0) ** 2 + (abs(y - cy) ** 1.3 / 6.0) ** 2
            if dist < 1.0:
                edge = dist > 0.7
                base = fat_white if (x + y) % 6 == 0 else (meat_dark if edge else meat_red)
                img.putpixel((x, y), (*noisy(base, 12), 255))
    # veines toxiques mutantes
    toxin_points = [(5,6),(9,9),(7,11),(10,5),(4,9)]
    for (x, y) in toxin_points:
        if img.getpixel((x, y))[3] > 0:
            img.putpixel((x, y), (*toxin_green, 255))
    return img

# ─────────────────────────────────────────────────────────
# 5. MUTANT FRUIT — fruit rond multicolore (effet génétique imprévisible)
# ─────────────────────────────────────────────────────────
def make_mutant_fruit():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    skin_outer = (210, 60, 150)   # rose-violet
    skin_inner = (240, 110, 190)
    highlight = (255, 200, 230)
    stem_brown = (90, 60, 40)
    leaf_green = (90, 160, 70)
    cx, cy = 8, 9
    r = 6
    for y in range(16):
        for x in range(16):
            dist = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if dist < r:
                base = highlight if dist < r * 0.35 else (skin_inner if dist < r * 0.75 else skin_outer)
                img.putpixel((x, y), (*noisy(base, 10), 255))
    # tige
    for y in range(1, 4):
        img.putpixel((8, y), (*stem_brown, 255))
    # petite feuille
    img.putpixel((9, 2), (*leaf_green, 255))
    img.putpixel((10, 2), (*leaf_green, 255))
    img.putpixel((9, 1), (*leaf_green, 255))
    return img

# ─────────────────────────────────────────────────────────
# 6. EVOLUTION SCANNER — appareil gris/bleu avec écran lumineux
# ─────────────────────────────────────────────────────────
def make_evolution_scanner():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    casing_dark = (50, 55, 60)
    casing_mid = (75, 82, 90)
    screen_bg = (15, 25, 30)
    screen_glow = (80, 230, 200)
    button_red = (200, 60, 60)
    # corps de l'appareil (rectangle vertical)
    for y in range(2, 15):
        for x in range(4, 13):
            base = casing_mid if (x + y) % 4 != 0 else casing_dark
            img.putpixel((x, y), (*noisy(base, 8), 255))
    # écran
    for y in range(4, 9):
        for x in range(6, 11):
            img.putpixel((x, y), (*screen_bg, 255))
    # ligne de scan lumineuse sur l'écran
    for x in range(6, 11):
        img.putpixel((x, 6), (*screen_glow, 255))
    # bouton
    img.putpixel((8, 11), (*button_red, 255))
    img.putpixel((8, 12), (*noisy(button_red, 15), 255))
    return img

# ─────────────────────────────────────────────────────────
# Sauvegarde de toutes les textures
# ─────────────────────────────────────────────────────────
import os

BASE = "/home/claude/evolutionmod/src/main/resources/assets/evolutionmod/textures"

print("Génération des textures...")
save(make_bioluminescent_leaves(), f"{BASE}/block/bioluminescent_leaves.png")
save(make_mutant_log_side(), f"{BASE}/block/mutant_log.png")
save(make_mutant_log_top(), f"{BASE}/block/mutant_log_top.png")
save(make_genetic_crystal(), f"{BASE}/item/genetic_crystal.png")
save(make_mutant_meat(), f"{BASE}/item/mutant_meat.png")
save(make_mutant_fruit(), f"{BASE}/item/mutant_fruit.png")
save(make_evolution_scanner(), f"{BASE}/item/evolution_scanner.png")
print("Terminé.")

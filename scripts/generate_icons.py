#!/usr/bin/env python3
from PIL import Image
import os, sys

src = sys.argv[1]
res_dir = sys.argv[2]
img = Image.open(src)
print(f"Source: {img.size}")

launcher_sizes = {
    "mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192
}
fg_sizes = {
    "mipmap-mdpi": 108, "mipmap-hdpi": 162, "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324, "mipmap-xxxhdpi": 432
}

for folder, size in launcher_sizes.items():
    out_dir = os.path.join(res_dir, folder)
    os.makedirs(out_dir, exist_ok=True)
    resized = img.resize((size, size), Image.LANCZOS)
    resized.save(os.path.join(out_dir, "ic_launcher.png"))
    resized.save(os.path.join(out_dir, "ic_launcher_round.png"))
    print(f"  {folder}: {size}x{size}")

for folder, size in fg_sizes.items():
    out_dir = os.path.join(res_dir, folder)
    os.makedirs(out_dir, exist_ok=True)
    # En Adaptive Icon (108dp), la zona visible central segura es de 72dp (66% del tamaño)
    safe_size = int(size * 0.72)
    offset = (size - safe_size) // 2
    fg_canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    resized_fg = img.resize((safe_size, safe_size), Image.LANCZOS)
    fg_canvas.paste(resized_fg, (offset, offset))
    fg_canvas.save(os.path.join(out_dir, "ic_launcher_foreground.png"))
    print(f"  {folder} foreground (padded): {size}x{size}")

print("All icons generated!")
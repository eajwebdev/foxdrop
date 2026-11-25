"""
Generates FoxDrop brand assets from reference/logos (MASTER_PROMPT.md §11).

    python -m venv tools/assets/.venv
    tools/assets/.venv/Scripts/python -m pip install -r tools/assets/requirements.txt   (Windows)
    tools/assets/.venv/bin/python -m pip install -r tools/assets/requirements.txt       (macOS/Linux)
    <venv python> tools/assets/make_assets.py

Outputs (all committed):
  - app-*/src/main/res/mipmap-*/ic_launcher_foreground.png + ic_launcher_monochrome.png (adaptive icons)
  - app-*/src/main/ic_launcher-playstore.png (512 px)
  - core/designsystem/src/main/res/drawable-nodpi/foxdrop_*.webp (wordmark + marks, light/dark)
  - core/designsystem/src/main/res/drawable/foxdrop_splash_mark.xml (vector traced from the fox mark)
"""

from __future__ import annotations

import io
import re
import sys
from collections import deque
from pathlib import Path

from PIL import Image, ImageChops, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
LOGOS = ROOT / "reference" / "logos"
DESIGN_RES = ROOT / "core" / "designsystem" / "src" / "main" / "res"

NAVY = (5, 21, 60)
PINK = (242, 1, 79)
WHITE = (255, 255, 255)

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
ADAPTIVE_DP = 108
# Adaptive icons: 108 dp canvas, 66 dp safe-zone circle (0.61), ~72 dp visible on most masks (0.67).
SAFE_DIAGONAL = 0.74  # wide marks: diagonal fits the visible area, the centre of interest is in the safe zone
SAFE_FRACTION_ROUND = 0.64  # round badge: diameter just inside the safe zone


def remove_outer_background(img: Image.Image, threshold: int = 40, max_hole_area: int = 0) -> Image.Image:
    """Makes the white background connected to the image border transparent and un-blends anti-aliased edges,
    keeping white areas that are inside the artwork (fox face, parcel). Enclosed white areas smaller than
    [max_hole_area] pixels (letter counters such as the inside of "O") are treated as background too."""
    rgb = img.convert("RGB")
    w, h = rgb.size
    px = rgb.load()
    background = bytearray(w * h)

    def is_light(x: int, y: int) -> bool:
        r, g, b = px[x, y]
        return min(r, g, b) >= 255 - threshold

    queue = deque()
    for x in range(w):
        queue.append((x, 0))
        queue.append((x, h - 1))
    for y in range(h):
        queue.append((0, y))
        queue.append((w - 1, y))
    while queue:
        x, y = queue.popleft()
        i = y * w + x
        if background[i] or not is_light(x, y):
            continue
        background[i] = 1
        if x > 0:
            queue.append((x - 1, y))
        if x < w - 1:
            queue.append((x + 1, y))
        if y > 0:
            queue.append((x, y - 1))
        if y < h - 1:
            queue.append((x, y + 1))

    if max_hole_area:
        _remove_small_holes(background, w, h, is_light, max_hole_area)

    mask = Image.frombytes("L", (w, h), bytes(v * 255 for v in background))
    edge = mask.filter(ImageFilter.MaxFilter(5))  # 2 px ring around the background
    out = Image.new("RGBA", (w, h))
    opx = out.load()
    epx = edge.load()
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            if background[y * w + x]:
                opx[x, y] = (255, 255, 255, 0)
            elif epx[x, y]:
                # Colour-to-alpha against white for the anti-aliased border.
                a = max(255 - r, 255 - g, 255 - b)
                if a == 0:
                    opx[x, y] = (255, 255, 255, 0)
                    continue
                af = a / 255.0
                un = tuple(max(0, min(255, round((c - (1 - af) * 255) / af))) for c in (r, g, b))
                opx[x, y] = (*un, a)
            else:
                opx[x, y] = (r, g, b, 255)
    return out


def _remove_small_holes(background: bytearray, w: int, h: int, is_light, max_area: int) -> None:
    seen = bytearray(w * h)
    for start in range(w * h):
        if background[start] or seen[start]:
            continue
        sx, sy = start % w, start // w
        if not is_light(sx, sy):
            continue
        component = []
        stack = [start]
        seen[start] = 1
        while stack:
            i = stack.pop()
            component.append(i)
            x, y = i % w, i // w
            for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if 0 <= nx < w and 0 <= ny < h:
                    j = ny * w + nx
                    if not seen[j] and not background[j] and is_light(nx, ny):
                        seen[j] = 1
                        stack.append(j)
        if len(component) <= max_area:
            for i in component:
                background[i] = 1


def recolor_navy(img: Image.Image, to: tuple[int, int, int]) -> Image.Image:
    """Replaces navy ink with [to] so marks read on the navy brand background.

    Every pixel is treated as a mix of the three brand inks: p = navy + b·(pink − navy) + c·(white − navy).
    Solving for b, c (least squares) and re-mixing with navy swapped for [to] keeps anti-aliased edges clean.
    """
    u = [PINK[i] - NAVY[i] for i in range(3)]
    v = [WHITE[i] - NAVY[i] for i in range(3)]
    uu = sum(x * x for x in u)
    vv = sum(x * x for x in v)
    uv = sum(u[i] * v[i] for i in range(3))
    det = uu * vv - uv * uv
    out = img.copy()
    p = out.load()
    w, h = out.size
    for y in range(h):
        for x in range(w):
            r, g, bl, a = p[x, y]
            if not a:
                continue
            d = (r - NAVY[0], g - NAVY[1], bl - NAVY[2])
            du = sum(d[i] * u[i] for i in range(3))
            dv = sum(d[i] * v[i] for i in range(3))
            pink_share = max(0.0, min(1.0, (du * vv - dv * uv) / det))
            white_share = max(0.0, min(1.0 - pink_share, (dv * uu - du * uv) / det))
            navy_share = 1.0 - pink_share - white_share
            p[x, y] = (
                *(
                    round(PINK[i] * pink_share + WHITE[i] * white_share + to[i] * navy_share)
                    for i in range(3)
                ),
                a,
            )
    return out


def crop_to_content(img: Image.Image, region: tuple[int, int, int, int] | None = None, pad: int = 4) -> Image.Image:
    src = img.crop(region) if region else img
    bbox = src.getchannel("A").point(lambda a: 255 if a > 16 else 0).getbbox()
    if not bbox:
        raise ValueError("empty image")
    l, t, r, b = bbox
    return src.crop((max(0, l - pad), max(0, t - pad), min(src.width, r + pad), min(src.height, b + pad)))


def fit_on_canvas(mark: Image.Image, size: int, fraction: float, by_diagonal: bool = False) -> Image.Image:
    """Centres [mark] on a square canvas. With [by_diagonal], the mark's diagonal (not its longest side) is
    limited to [fraction] of the canvas, so wide marks stay inside a circular safe zone."""
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    target = size * fraction
    if by_diagonal:
        scale = target / (mark.width**2 + mark.height**2) ** 0.5
    else:
        scale = min(target / mark.width, target / mark.height)
    resized = mark.resize((max(1, round(mark.width * scale)), max(1, round(mark.height * scale))), Image.LANCZOS)
    canvas.alpha_composite(resized, ((size - resized.width) // 2, (size - resized.height) // 2))
    return canvas


def monochrome(img: Image.Image) -> Image.Image:
    """Themed-icon layer: white silhouette, the system tints it."""
    alpha = img.getchannel("A")
    out = Image.new("RGBA", img.size, (255, 255, 255, 0))
    out.putalpha(alpha)
    return out


def write_launcher(
    app_dir: Path,
    foreground: Image.Image,
    background_rgb: tuple[int, int, int],
    round_mark: bool = False,
) -> None:
    res = app_dir / "src" / "main" / "res"
    for density, factor in DENSITIES.items():
        size = round(ADAPTIVE_DP * factor)
        if round_mark:
            fg = fit_on_canvas(foreground, size, SAFE_FRACTION_ROUND)
        else:
            fg = fit_on_canvas(foreground, size, SAFE_DIAGONAL, by_diagonal=True)
        folder = res / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        fg.save(folder / "ic_launcher_foreground.png", optimize=True)
        monochrome(fg).save(folder / "ic_launcher_monochrome.png", optimize=True)
    # Play Store listing icon (512 px, full bleed square, no transparency).
    store = Image.new("RGBA", (512, 512), (*background_rgb, 255))
    store.alpha_composite(fit_on_canvas(foreground, 512, 0.72))
    store.convert("RGB").save(app_dir / "src" / "main" / "ic_launcher-playstore.png", optimize=True)


def save_webp(img: Image.Image, path: Path, max_width: int) -> None:
    """Lossless WebP (minSdk 26 supports it): about a third smaller than PNG, same pixels."""
    path.parent.mkdir(parents=True, exist_ok=True)
    if img.width > max_width:
        img = img.resize((max_width, round(img.height * max_width / img.width)), Image.LANCZOS)
    img.save(path, format="WEBP", lossless=True, quality=100, method=6)


# ---------------------------------------------------------------- vector splash mark


def svg_to_vector_drawable(svg: str, size_dp: int, name: str) -> str:
    """Converts vtracer's SVG (paths with fill + translate) into an animated-vector-ready VectorDrawable.
    The whole mark sits in group 'mark' so the AVD can slide/scale it."""
    view = re.search(r'width="(\d+)" height="(\d+)"', svg)
    if not view:
        raise ValueError("unexpected svg header")
    vw, vh = int(view.group(1)), int(view.group(2))
    # Splash icons without an icon background are 288 dp and must fit a 192 dp circle (ratio 0.667):
    # pad the viewport so the mark's diagonal stays just inside it.
    side = round(((vw**2 + vh**2) ** 0.5) / 0.64)
    paths = []
    for m in re.finditer(r'<path d="([^"]+)" fill="(#[0-9A-Fa-f]{6})" transform="translate\((-?[\d.]+),(-?[\d.]+)\)"/>', svg):
        d, fill, tx, ty = m.group(1), m.group(2), float(m.group(3)), float(m.group(4))
        paths.append((d.strip(), fill.upper(), tx, ty))
    if not paths:
        raise ValueError("no paths parsed from svg")
    ox, oy = (side - vw) / 2, (side - vh) / 2
    body = []
    for d, fill, tx, ty in paths:
        body.append(
            f'        <group android:translateX="{tx + ox:.1f}" android:translateY="{ty + oy:.1f}">\n'
            f'            <path android:fillColor="{fill}" android:pathData="{d}" />\n'
            f"        </group>"
        )
    pivot = side / 2
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"<!-- Generated by tools/assets/make_assets.py from reference/logos ({name}). Do not edit by hand.\n"
        "     The splash screen scales the icon to its own size; long paths are the traced logo, drawn once. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:tools="http://schemas.android.com/tools"\n'
        '    tools:ignore="VectorPath"\n'
        f'    android:width="{size_dp}dp"\n'
        f'    android:height="{size_dp}dp"\n'
        f'    android:viewportWidth="{side}"\n'
        f'    android:viewportHeight="{side}">\n'
        f'    <group android:name="mark" android:pivotX="{pivot:.1f}" android:pivotY="{pivot:.1f}">\n'
        + "\n".join(body)
        + "\n    </group>\n</vector>\n"
    )


def trace_mark(mark: Image.Image, out_xml: Path, size_dp: int, name: str) -> None:
    import vtracer  # only needed for the splash vector

    small = mark.copy()
    small.thumbnail((360, 360), Image.LANCZOS)
    # Flatten semi-transparent edges and snap colours to the brand palette so the trace has few paths.
    flat = Image.new("RGBA", small.size, (0, 0, 0, 0))
    sp, fp = small.load(), flat.load()
    palette = [PINK, WHITE, NAVY]
    for y in range(small.height):
        for x in range(small.width):
            r, g, b, a = sp[x, y]
            if a < 128:
                continue
            best = min(palette, key=lambda c: (c[0] - r) ** 2 + (c[1] - g) ** 2 + (c[2] - b) ** 2)
            fp[x, y] = (*best, 255)
    buffer = io.BytesIO()
    flat.save(buffer, format="PNG")
    svg = vtracer.convert_raw_image_to_svg(
        buffer.getvalue(),
        img_format="png",
        colormode="color",
        hierarchical="stacked",
        mode="spline",
        filter_speckle=6,
        color_precision=6,
        layer_difference=32,
        corner_threshold=60,
        length_threshold=4.0,
        splice_threshold=45,
        path_precision=1,
    )
    out_xml.parent.mkdir(parents=True, exist_ok=True)
    out_xml.write_text(svg_to_vector_drawable(svg, size_dp, name), encoding="utf-8")


def main() -> int:
    swift = remove_outer_background(Image.open(LOGOS / "foxdrop_swift_fox_delivery_logo.png"))
    emblem = remove_outer_background(Image.open(LOGOS / "foxdrop_delivery_logo_with_fox_emblem.png"))
    badge = remove_outer_background(Image.open(LOGOS / "foxdrop_dynamic_fox_delivery_logo.png"))
    # Wordmark: letter counters ("o", "R", "O") must be see-through, the fox's white face (large) stays.
    speedy = remove_outer_background(Image.open(LOGOS / "foxdrop_speedy_delivery_logo.png"), max_hole_area=2500)

    # Marks without wordmarks (rows measured on the 1254 px source images).
    swift_mark = crop_to_content(swift, (0, 0, swift.width, 800))
    emblem_mark = crop_to_content(emblem, (0, 0, emblem.width, 830))
    badge_mark = crop_to_content(badge)
    wordmark = crop_to_content(speedy)

    swift_mark_on_navy = recolor_navy(swift_mark, WHITE)

    write_launcher(ROOT / "app-customer", swift_mark_on_navy, NAVY)
    # The FD emblem loses its parcel when recoloured, so the business icon keeps the original inks on white
    # (ic_launcher_background in app-business is white).
    write_launcher(ROOT / "app-business", emblem_mark, WHITE)
    # The badge is already a navy circle; let it fill most of the safe zone.
    write_launcher(ROOT / "app-rider", badge_mark, NAVY, round_mark=True)

    nodpi = DESIGN_RES / "drawable-nodpi"
    save_webp(wordmark, nodpi / "foxdrop_wordmark.webp", 720)
    save_webp(recolor_navy(wordmark, WHITE), nodpi / "foxdrop_wordmark_on_dark.webp", 720)
    save_webp(swift_mark, nodpi / "foxdrop_mark.webp", 480)
    save_webp(swift_mark_on_navy, nodpi / "foxdrop_mark_on_dark.webp", 480)
    save_webp(emblem_mark, nodpi / "foxdrop_emblem.webp", 480)

    if "--no-vector" not in sys.argv:
        # 192 dp intrinsic size (lint limit is 200); the splash screen scales it to its 288 dp icon slot.
        trace_mark(swift_mark_on_navy, DESIGN_RES / "drawable" / "foxdrop_splash_mark.xml", 192, "swift fox mark")
    print("assets written")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

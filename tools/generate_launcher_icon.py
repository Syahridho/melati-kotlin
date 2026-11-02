"""Buat ulang ikon launcher Melati dari drawable/melati_big.png.

Cara pakai (butuh Pillow: `python -m pip install pillow`):

    python tools/generate_launcher_icon.py

Menghasilkan, untuk setiap kerapatan mdpi..xxxhdpi:
  res/mipmap-<dpi>/ic_launcher.png            48dp, latar putih, logo 60%
  res/mipmap-<dpi>/ic_launcher_round.png      sama (launcher yang memotong bentuk)
  res/mipmap-<dpi>/ic_launcher_foreground.png 108dp, transparan, logo 50%

Adaptive icon (mipmap-anydpi-v26/ic_launcher.xml) memakai warna latar
@color/ic_launcher_background (#FFFFFF) dan foreground PNG di atas. Logo 50%
dari kanvas 108dp berarti 75% dari diameter mask yang terlihat (72dp), sehingga
tidak terpotong dan porsinya sebanding dengan ikon bawaan sistem.
"""

from pathlib import Path

from PIL import Image

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"
SOURCE = RES / "drawable/melati_big.png"

DENSITY = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
WHITE = (255, 255, 255, 255)
TRANSPARENT = (0, 0, 0, 0)

LEGACY_DP = 48
LEGACY_LOGO_FRACTION = 0.60
ADAPTIVE_DP = 108
ADAPTIVE_LOGO_FRACTION = 0.50


def compose(logo: Image.Image, canvas_px: int, fraction: float, bg: tuple) -> Image.Image:
    """Kanvas persegi berisi logo terpusat, lebarnya `fraction` x kanvas."""
    canvas = Image.new("RGBA", (canvas_px, canvas_px), bg)
    width = int(round(canvas_px * fraction))
    height = int(round(width * logo.height / logo.width))
    scaled = logo.resize((width, height), Image.LANCZOS)
    canvas.alpha_composite(scaled, ((canvas_px - width) // 2, (canvas_px - height) // 2))
    return canvas


def main() -> None:
    logo = Image.open(SOURCE).convert("RGBA")

    for name, factor in DENSITY.items():
        folder = RES / f"mipmap-{name}"

        legacy_px = int(LEGACY_DP * factor)
        legacy = compose(logo, legacy_px, LEGACY_LOGO_FRACTION, WHITE)
        legacy.save(folder / "ic_launcher.png")
        legacy.save(folder / "ic_launcher_round.png")

        foreground_px = int(ADAPTIVE_DP * factor)
        compose(logo, foreground_px, ADAPTIVE_LOGO_FRACTION, TRANSPARENT).save(
            folder / "ic_launcher_foreground.png"
        )

        print(f"{name:<8} legacy {legacy_px:3d}px  adaptive {foreground_px:3d}px")


if __name__ == "__main__":
    main()

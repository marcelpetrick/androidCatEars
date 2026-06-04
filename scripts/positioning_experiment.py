#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
# SPDX-License-Identifier: GPL-3.0-or-later
from __future__ import annotations

import argparse
import math
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageOps
from scipy import ndimage


@dataclass(frozen=True)
class FaceRegion:
    left: int
    top: int
    right: int
    bottom: int

    @property
    def width(self) -> int:
        return self.right - self.left

    @property
    def height(self) -> int:
        return self.bottom - self.top

    @property
    def center_x(self) -> float:
        return self.left + self.width / 2


@dataclass(frozen=True)
class EarPlacement:
    face: FaceRegion
    base_y: float
    left_x: float
    right_x: float
    size: float
    score: float


def skin_mask(image: Image.Image) -> np.ndarray:
    rgb = np.asarray(image.convert("RGB"), dtype=np.uint8)
    r = rgb[:, :, 0].astype(np.int16)
    g = rgb[:, :, 1].astype(np.int16)
    b = rgb[:, :, 2].astype(np.int16)
    ycbcr = np.asarray(image.convert("YCbCr"), dtype=np.uint8)
    cb = ycbcr[:, :, 1].astype(np.int16)
    cr = ycbcr[:, :, 2].astype(np.int16)

    # Conservative skin model tuned for the supplied outdoor hat selfies.
    rgb_rule = (r > 70) & (g > 45) & (b > 30) & (r > g + 8) & (r > b + 18)
    chroma_rule = (cb >= 77) & (cb <= 135) & (cr >= 132) & (cr <= 178)
    mask = rgb_rule & chroma_rule
    mask = ndimage.binary_opening(mask, structure=np.ones((5, 5)))
    mask = ndimage.binary_closing(mask, structure=np.ones((17, 17)))
    mask = ndimage.binary_fill_holes(mask)
    return mask


def detect_face_region(image: Image.Image) -> tuple[FaceRegion, float]:
    mask = skin_mask(image)
    height, width = mask.shape
    labels, count = ndimage.label(mask)
    if count == 0:
        fallback = FaceRegion(width // 3, height // 4, width * 2 // 3, height * 3 // 5)
        return fallback, 0.0

    objects = ndimage.find_objects(labels)
    best: tuple[float, FaceRegion] | None = None
    for label_index, slices in enumerate(objects, start=1):
        if slices is None:
            continue
        ys, xs = slices
        area = int((labels[slices] == label_index).sum())
        left, right = xs.start, xs.stop
        top, bottom = ys.start, ys.stop
        region = FaceRegion(left, top, right, bottom)
        if region.width < width * 0.12 or region.height < height * 0.08:
            continue
        center_penalty = abs(region.center_x - width / 2) / width
        top_penalty = max(0.0, (region.top - height * 0.62) / height)
        score = area / (1 + center_penalty * 3 + top_penalty * 8)
        if best is None or score > best[0]:
            best = (score, region)

    if best is None:
        fallback = FaceRegion(width // 3, height // 4, width * 2 // 3, height * 3 // 5)
        return fallback, 0.0
    return best[1], best[0]


def compute_placement(image: Image.Image) -> EarPlacement:
    face, score = detect_face_region(image)
    face_width = max(face.width, image.width * 0.16)
    face_height = max(face.height, image.height * 0.18)

    # Attach at the visible forehead/top-of-head line, with a tiny overlap so
    # anti-aliased ear bases visibly touch skin instead of floating above it.
    base_y = face.top + face_height * 0.065
    size = face_width * 0.42
    spacing = face_width * 0.31
    return EarPlacement(
        face=face,
        base_y=base_y,
        left_x=face.center_x - spacing,
        right_x=face.center_x + spacing,
        size=size,
        score=score,
    )


def draw_ear(draw: ImageDraw.ImageDraw, cx: float, base_y: float, size: float, side: int) -> None:
    tip = (cx + side * size * 0.05, base_y - size)
    outer_left = (cx - size * 0.35, base_y)
    outer_right = (cx + size * 0.35, base_y)
    inner_tip = (tip[0], base_y - size * 0.66)
    inner_left = (cx - size * 0.16, base_y - size * 0.16)
    inner_right = (cx + size * 0.16, base_y - size * 0.16)

    draw.polygon([tip, outer_left, outer_right], fill=(125, 82, 48, 232), outline=(54, 31, 18, 255))
    draw.polygon([inner_tip, inner_left, inner_right], fill=(232, 160, 160, 218), outline=(90, 48, 42, 180))

    for phase in (-0.8, -0.25, 0.25, 0.8):
        base_x = cx + phase * size * 0.23
        draw.line(
            [(base_x, base_y - size * 0.05), (base_x + side * size * 0.04, base_y - size * 0.54)],
            fill=(64, 38, 22, 210),
            width=max(2, int(size * 0.014)),
        )


def annotate(image_path: Path, output_path: Path, max_height: int) -> None:
    source = ImageOps.exif_transpose(Image.open(image_path)).convert("RGBA")
    scale = min(1.0, max_height / source.height)
    if scale < 1.0:
        source = source.resize((round(source.width * scale), round(source.height * scale)), Image.Resampling.LANCZOS)

    placement = compute_placement(source)
    overlay = Image.new("RGBA", source.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay, "RGBA")

    draw_ear(draw, placement.left_x, placement.base_y, placement.size, side=-1)
    draw_ear(draw, placement.right_x, placement.base_y, placement.size, side=1)

    face = placement.face
    draw.rectangle([face.left, face.top, face.right, face.bottom], outline=(255, 215, 0, 230), width=3)
    draw.line([(face.left, placement.base_y), (face.right, placement.base_y)], fill=(0, 220, 255, 245), width=4)
    draw.line(
        [
            (placement.left_x - placement.size * 0.35, placement.base_y),
            (placement.left_x + placement.size * 0.35, placement.base_y),
        ],
        fill=(255, 80, 80, 255),
        width=5,
    )
    draw.line(
        [
            (placement.right_x - placement.size * 0.35, placement.base_y),
            (placement.right_x + placement.size * 0.35, placement.base_y),
        ],
        fill=(255, 80, 80, 255),
        width=5,
    )
    for x in (placement.left_x, placement.right_x):
        draw.ellipse([x - 6, placement.base_y - 6, x + 6, placement.base_y + 6], fill=(255, 80, 80, 255))

    composed = Image.alpha_composite(source, overlay)
    draw = ImageDraw.Draw(composed)
    font = ImageFont.load_default()
    label = (
        f"forehead/base line y={placement.base_y:.0f}; "
        f"ear bottom overlaps this line; score={placement.score:.0f}"
    )
    pad = 8
    text_bbox = draw.textbbox((0, 0), label, font=font)
    box = [pad, pad, text_bbox[2] + pad * 3, text_bbox[3] + pad * 3]
    draw.rectangle(box, fill=(0, 0, 0, 170))
    draw.text((pad * 2, pad * 2), label, fill=(255, 255, 255), font=font)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    composed.convert("RGB").save(output_path, quality=92)


def main() -> int:
    parser = argparse.ArgumentParser(description="Create annotated cat-ear positioning experiments.")
    parser.add_argument("--input-dir", type=Path, default=Path("media/faceExamples"))
    parser.add_argument("--output-dir", type=Path, default=Path("media/faceExamples/positioning"))
    parser.add_argument("--max-height", type=int, default=1800)
    args = parser.parse_args()

    images = sorted(p for p in args.input_dir.glob("*.jpg") if p.is_file())
    if not images:
        raise SystemExit(f"No JPG inputs found in {args.input_dir}")
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for image in images:
        annotate(image, args.output_dir / f"{image.stem}_positioning.jpg", args.max_height)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

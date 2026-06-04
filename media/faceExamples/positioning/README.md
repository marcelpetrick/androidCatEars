# Ear Positioning Experiment

This directory contains annotated positioning outputs generated from the face examples in `media/faceExamples/`.

## Goal

Tune the cat-ear placement so the base of each ear sits on the visible top of the head/forehead region instead of floating above the face.

## Inputs

- 10 portrait face examples from `media/faceExamples/`.
- All examples include a hat, so the experiment focuses on the visible skin/forehead top line as the attachment cue.

## Method

The script `scripts/positioning_experiment.py`:

1. Loads each input image with EXIF orientation applied.
2. Builds a conservative skin mask using RGB and YCbCr thresholds.
3. Selects the strongest central skin component as the face/head region.
4. Uses the top of that region as the forehead attachment reference.
5. Draws procedural app-style ears with the ear bases lowered slightly into the detected forehead line.
6. Adds annotations:
   - Yellow rectangle: detected skin/face component.
   - Cyan line: computed forehead/base reference.
   - Red segments and dots: actual ear-base contact line.

## Current Constants

- Ear base line: `face.top + face_height * 0.065`
- Ear size: `face_width * 0.42`
- Ear horizontal spacing from face center: `face_width * 0.31`

The base was first tested at `0.03` of the face height. That still looked a little high. The current `0.065` value lowers the ears enough that the bases visibly plant into the forehead/hat-edge region without sliding down onto the eyes.

## Outputs

- `input_contact_sheet.jpg`: overview of the unmodified inputs.
- `positioning_contact_sheet.jpg`: overview of the final annotated positioning pass.
- `*_positioning.jpg`: full annotated result for each source image.

## Findings

- The earlier app strategy of using side-of-head human ear landmarks is not a good mental model for this bug. It can help horizontal spacing, but the visual attachment must be driven by the forehead/top-of-head line.
- For these examples, a small downward overlap below the detected top skin line looks more natural than placing the ear base exactly at or above that line.
- The hat makes perfect automatic placement ambiguous: visually, the ears can attach either to the forehead/top skin line or the hat silhouette. For the current requested bug, the forehead/top skin line is the better diagnostic reference.
- The generated ears now read as attached rather than floating in the contact sheet. Some images still look costume-like because the procedural ear shape is tall and opaque; that is a style/rendering issue separate from the base-position bug.

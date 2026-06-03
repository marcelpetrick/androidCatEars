# Cat Ears Camera – Project Discovery and Requirements Summary

## Project Overview

The goal is to create a modern Android application that provides a real-time camera preview with augmented reality style cat-ear overlays.

The application should detect a user's face on-device, position cat ears correctly above the head, and allow the user to capture and share photos containing the overlay.

The project serves multiple purposes:

1. Refresh Android development skills after a long break.
2. Learn and use the modern Android ecosystem.
3. Experiment with camera and machine learning technologies.
4. Build and eventually ship a polished Android application.
5. Maintain professional software engineering practices from day one.

---

# Original Technical Discussion

## Initial Technology Question

The project started with a discussion about whether to use:

* Qt
* C++
* Kotlin
* Android-native technologies

The original idea was to possibly reuse Qt skills and support desktop simulations on Linux.

### Decision

Do **not** use Qt for the Android application itself.

Use a modern native Android stack instead.

### Reasoning

The application relies heavily on:

* Camera access
* Front/back camera switching
* Real-time face detection
* Real-time overlays
* Modern Android UI

These are best supported by Android-native technologies.

Qt deployment to Android is possible, but would introduce additional complexity and provide little benefit for this project.

---

# Chosen Technology Stack

## Language

Kotlin

## UI Framework

Jetpack Compose

## Camera Framework

CameraX

## Face Detection

ML Kit Face Detection

## Build System

Gradle with Kotlin DSL

## IDE

Android Studio

## Machine Learning

On-device only.

Initially use:

* ML Kit Face Detection

Do not introduce:

* ONNX
* TensorFlow Lite
* Custom models

unless future requirements make them necessary.

---

# Desktop Development Considerations

## Requirement

Fast iteration during development on Manjaro Linux.

## Discussion

The project does not require a fully cross-platform codebase.

Android-first development is acceptable.

### Decision

Use:

* Android Studio
* Android Emulator
* Physical Android device

for development.

If needed later, create a lightweight desktop simulation utility for testing overlay positioning logic.

The desktop simulator is optional and not part of the initial scope.

---

# Application Concept

## Core Idea

The user opens the application.

The application displays a live camera preview.

The user can:

* Switch between front and rear cameras.
* Enable a cat-ear overlay.
* See the overlay rendered in real time.
* Capture a photo.
* Share the resulting image.

---

# Scope Decisions

## In Scope

### Camera Preview

Real-time camera preview.

### Camera Selection

* Front camera
* Rear camera

### Face Detection

Single face only.

### Overlay

2D cat-ear graphics.

### Photo Capture

Capture still images.

### Image Saving

Save generated images locally.

### Sharing

Allow images to be shared using Android sharing intents.

Examples:

* Messaging apps
* Email
* Social media
* Apple users through messaging applications

### Professional Engineering Practices

From day one:

* Modular architecture
* Tests
* CI/CD readiness
* Release build support
* Proper Git repository

---

## Out of Scope for Initial Version

### Multiple Faces

Not required initially.

### Video Recording

Deferred to a later milestone.

### Advanced AR

Not required.

### Custom AI Models

Not required.

### Desktop Application

Not required.

---

# Platform Requirements

## Minimum Android Version

Android 14 (API 34)

## Primary Test Device

Modern Android device.

Current user device characteristics:

* Android 16
* Approx. 6 GB RAM
* 8 CPU cores
* Recent Xiaomi Pro-class device

The application should remain compatible with other modern Android devices.

---

# Package Information

## Package Name

```text
it.marcelpetrick.catears
```

---

# Licensing

## License

GPL v3

All source code should be compatible with GPLv3 requirements.

---

# Questions Asked and Answers Given

## Question 1

What kind of desktop simulation is required?

### Answer

A Linux desktop version is desirable only to shorten development cycles and test behavior.

A true cross-platform application is not required.

Android-first is acceptable.

---

## Question 2

Is there existing Qt or C++ code?

### Answer

No.

The project starts from scratch.

---

## Question 3

Should this become a reusable framework?

### Answer

No.

It should be a fun toy application.

However, it should be built professionally.

---

## Question 4

Must Android and Linux share one codebase?

### Answer

No.

Android-first development is acceptable.

---

## Question 5

Should the application support real-time preview and image capture?

### Answer

Yes.

Requirements:

* Real-time preview
* Real-time overlay
* Photo capture

Video can come later.

---

## Question 6

Should machine learning be on-device?

### Answer

Yes.

No cloud processing.

No server-side AI.

---

## Question 7

Target hardware?

### Answer

Modern Android hardware.

The app should remain generally compatible with current Android phones.

---

## Question 8

Comfort level with modern Android technologies?

### Answer

Comfortable learning:

* Kotlin
* Compose
* CameraX
* Modern Android architecture

---

## Question 9

Project quality expectations?

### Answer

Professional quality from the beginning:

* Modular architecture
* Continuous integration
* Testing
* Release builds

---

## Question 10

Primary goal?

### Answer

1. Refresh Android skills.
2. Learn camera APIs.
3. Experiment with machine learning.
4. Ship an application.

---

## Additional Follow-up Questions

### Minimum Android Version

Answer:

Android 14+

---

### App Name

Answer:

Undecided.

To be chosen later.

---

### Package Name

Answer:

Use:

```text
it.marcelpetrick.catears
```

---

### Build Tool

Answer:

Use recommended modern defaults.

Result:

Gradle Kotlin DSL.

---

### IDE

Answer:

Android Studio.

---

### Photo Output

Answer:

Save locally and make shareable.

---

### Video Recording

Answer:

Postpone to a future milestone.

---

### Face Count

Answer:

Single face only.

---

### Overlay Type

Answer:

2D cat-ear graphics.

---

### License

Answer:

GPLv3.

---

# Future Ideas (Not Part of MVP)

The following ideas may be considered later:

## Video Recording

Record video with cat-ear overlays.

## Multiple Face Tracking

Support multiple people simultaneously.

## Additional Filters

Examples:

* Dog ears
* Glasses
* Hats
* Masks
* Anime effects

## Animated Overlays

Animated cat ears.

## Facial Expressions

React to:

* Smiling
* Blinking
* Mouth opening

## Custom AI Models

Evaluate:

* ONNX Runtime
* TensorFlow Lite

only if ML Kit becomes limiting.

## Desktop Simulation Tool

A Linux utility for testing face landmark geometry and overlay placement without deploying to Android.

## Overlay Marketplace

Downloadable overlay packs.

## Social Features

Sharing and publishing workflows.

---

# Final Product Vision

A professionally engineered Android application that:

* Uses CameraX for live camera access.
* Uses ML Kit for on-device face detection.
* Renders cat-ear overlays in real time.
* Supports front and rear cameras.
* Captures photos.
* Saves and shares images.
* Targets Android 14+.
* Is implemented using Kotlin and Jetpack Compose.
* Uses GPLv3 licensing.
* Serves as a modern Android learning and shipping project.

This document captures all decisions, assumptions, answers, constraints, and future ideas discussed so far, and should be sufficient as the discovery/requirements input for an agentic development system.

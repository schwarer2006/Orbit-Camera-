# ORBIT Camera

**Computational Art Photography for Android**

> **Many perspectives. One expressive photograph.**

ORBIT Camera is a native Android photography application for creating **artistic multi-exposure compositions from multiple photographs**. Instead of building a panorama or a 3D model, ORBIT combines different viewpoints, contours, colors, and textures into a new photographic image.

### Inspired by multi-perspective photography

ORBIT draws inspiration from the photographic approach of **Pep Ventosa**, particularly his *In the Round* series: photographing a subject from a sequence of viewpoints and layering those perspectives into a single, painterly composition. Rather than reconstructing a 3D scene or stitching a panorama, ORBIT explores how repeated contours, shifting backgrounds, light and color can build a new visual interpretation of the subject.

**ORBIT is an independent, original software project.** It is not developed by, affiliated with, or endorsed by Pep Ventosa. The goal is not to reproduce any individual artwork, but to give photographers their own creative controls for multi-perspective image-making.

The app combines **guided photo capture**, an optional **onion-skin / ghost overlay**, **interval photography**, and a local **2D image-fusion engine** with adjustable creative controls.

**Project status:** Active development / early prototype. Features described below are present in the supplied source code; compatibility, performance, and reliability on physical Android devices still require validation.

## Highlights

- **Native Android camera:** Live preview and photo capture using CameraX.
- **Guided photo sequences:** Stationary, Linear, and Orbit capture modes, with selectable targets of 30, 50, 75, or 100 images.
- **Ghost Overlay (Onion Skin):** Optionally display the previous or first captured photograph transparently over the live preview, with adjustable opacity. The overlay is a composition aid, not part of the captured image.
- **Manual or interval capture:** Shoot on demand or use automated intervals of **2, 3, 5, or 10 seconds**, with pause/resume/stop controls.
- **Gallery import:** Create projects from images already taken with a smartphone or an external camera.
- **Creative image fusion:** Process photographs locally using different 2D compositing approaches, reference-frame alignment, and adjustable image characteristics.
- **Creative Studio:** Adjust abstraction, subject stability, color intensity, contour emphasis, background dissolution, and more.
- **Local projects:** Reopen and manage image sequences and their creative settings.
- **JPEG export:** Save results from the Studio to the device gallery.

## Artistic modes

| Mode | Creative direction |
| --- | --- |
| **Impressionist** | Painterly color blending, soft contours, and atmospheric backgrounds |
| **In the Round** | A strong central subject with layered viewpoints, repeated contours, and expressive color |
| **Architectural** | Structural lines, edges, and geometric repetition |
| **Ethereal** | Gentle, translucent, dreamlike compositions |
| **Motion Flow** | Directional repetition and visual movement |
| **Urban Layers** | Dense overlays of city scenes, structures, and details |

Each mode provides a starting configuration. Creative parameters can be adjusted further in the Studio. Output depends heavily on the source photographs; these modes do not guarantee identical results for every scene.

## How it works

1. **Capture or import** a sequence of photos.
2. **Select a project** and open it in the Studio.
3. **Choose an artistic mode** and optionally adjust the reference image or alignment.
4. **Tune the composition** with creative sliders.
5. **Render and export** the result as a JPEG image.

### Guided shooting

- **Stationary:** Vary composition while remaining in roughly the same place.
- **Linear:** Move laterally along a subject such as a facade or row of trees.
- **Orbit:** Move in an arc around a subject such as a tree, sculpture, or standalone building. A complete 360° circuit is not required.

The optional Ghost Overlay makes it easier to see changes in position between successive photographs. Interval capture helps create consistent sequences while moving around the subject.

## Technology

| Component | Implementation |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose / Material 3 |
| Camera | Android CameraX |
| Architecture | ViewModels, repositories, Kotlin Coroutines |
| Image processing | Local 2D bitmap fusion and creative post-processing |
| Storage | Local project/image storage; Android media export |
| Build system | Gradle (Kotlin DSL) |
| Minimum Android version | Android 7.0 (API 24), according to the current Gradle configuration |

The application does **not need a generative AI model** to produce its image-fusion effects. Its core photographic processing is designed to run locally.

## Getting started

### Requirements

- Android Studio with a compatible Android SDK and Java/JDK installation
- Android device or emulator running **Android 7.0 / API 24 or newer**
- Camera access for the camera workflow
- Sufficient free device storage for source images and rendered results

### Build

1. Clone or download this repository.
2. Open the repository root in **Android Studio**.
3. Let Gradle synchronize and install the SDK components requested by the project.
4. Select the `app` run configuration.
5. Run on an Android device or emulator and grant camera access when prompted.

To build a debug APK from the command line, use the Gradle wrapper **if available in your checkout**:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

**Build note:** The supplied archive contains Gradle configuration and wrapper properties, but no `gradlew` or `gradlew.bat` launchers. If those files are absent in your checkout, use Android Studio's build actions or restore/generate the standard Gradle wrapper files before running the commands above.

### Offline-first approach and build configuration

ORBIT's creative image-fusion workflow is designed for **on-device processing** and does not require generative AI or an online image-processing service. The current development project still contains some **Firebase / Google Services build integrations** (including Firebase AI and App Check dependencies). These appear in the Gradle configuration, although no `google-services.json` was included in the reviewed snapshot.

These are **development configuration leftovers to be reviewed**, not core requirements of ORBIT's artistic workflow. Before calling a release fully offline or independent of cloud services, we will remove integrations that are not used and verify the resulting build. The presence of dependencies alone does not establish whether any network requests actually occur at runtime.

## Project structure

The main Kotlin application code is organized as follows:

```text
app/src/main/java/com/example/
├── data/                 # Project repository and persistence
├── engine/               # Bitmap utilities and fusion engine
├── model/                # Projects, capture modes and art styles
├── ui/
│   ├── camera/           # CameraX, guided capture and overlay UI
│   ├── navigation/       # App navigation
│   ├── projects/         # Project management and import
│   ├── settings/         # Settings screen
│   ├── studio/           # Artistic controls and image rendering
│   └── theme/            # Visual design
└── MainActivity.kt
```

## Creative controls

The code includes five primary controls:

- **Abstraction** — the overall degree of artistic transformation.
- **Subject Stability** — how strongly the reference subject is emphasized.
- **Color Intensity** — the vibrancy and presence of colors.
- **Edge Emphasis** — how prominent repeated edges and contours appear.
- **Background Dissolve** — how much background structure is visually blended.

Additional parameters include echo strength and count, perspective spread, color density, warmth, glow, shadow depth, softness, motion strength, detail density, atmosphere, and blend strength.

> **Important:** Controls are artistic transformations rather than a guarantee of precise subject segmentation or physically correct geometric registration. Some processing behavior remains experimental.

## Current limitations

- The app is an **early development build**, not a confirmed production release.
- Camera preview and image fusion can be computationally expensive; performance depends on device memory and image count.
- The final Studio export currently uses the rendered preview bitmap in the inspected implementation. A separate full-resolution rendering/export pipeline remains an improvement area.
- Some advanced alignment and artistic effects are experimental and may require refinement on real multi-view photo sequences.
- Physical-device testing, especially repeated slider adjustments, Ghost Overlay, interval capture, and longer photo series, is recommended.

## Roadmap

Development follows a **Build → Test → Use → Improve → Release** approach, with a strong quick-to-market focus.

- [x] Native Android project structure and camera interface
- [x] Project management and gallery-import implementation
- [x] Local image-fusion engine and creative-style models
- [x] Ghost Overlay and interval-capture implementation
- [ ] Validate and stabilize Studio sliders under repeated rendering
- [ ] Improve subject-centered alignment and artistic output consistency
- [ ] Add a dedicated full-resolution export path
- [ ] Optimize longer photo series on physical devices
- [ ] Prepare a production-ready Android release

Checked items mean **implemented in source**, not independently certified or tested across devices.

## Development philosophy

ORBIT prioritizes real, usable creative functionality over extensive process overhead:

**Build → Test → Use → Improve → Release**

Small improvements, focused smoke tests, and early Android builds are preferred over lengthy approval gates and unnecessary infrastructure.

## Artistic roots and attribution

The photographic work of **Pep Ventosa**, especially *In the Round*, is an important conceptual inspiration for ORBIT. Its emphasis on layering different viewpoints of the same subject helped shape the project's creative direction. ORBIT translates that general photographic idea into an independently developed, interactive Android workflow with adjustable abstraction, color, contour and blending controls.

**All referenced artists retain the rights to their own works.** ORBIT is not an official Pep Ventosa application, nor is it affiliated with or endorsed by him. No original photographs by Pep Ventosa are bundled with this project.

## Contributing and licensing

Feedback, technical suggestions, and reproducible bug reports are welcome. When reporting an issue, include the Android version, device model, steps to reproduce, and relevant Logcat output where possible.

**License:** No license file was included in the inspected project archive. Until a license is added, do not assume the source code is open-source or freely reusable. Add an explicit `LICENSE` file before inviting third-party reuse or redistribution.

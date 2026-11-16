# SELF-MIND Android

A native Android starter project for the SELF-MIND mobile control app.

## Current build (v0.1.0)
- Native Kotlin Android UI
- Chat-style local demo interface
- On-device conversation history
- Brain, Cyber Intelligence, Builder and Memory sections
- Draft-only module planning
- No external network calls and no API keys stored

## Important status
This is a working **offline MVP shell**, not yet an autonomous AI. The chat replies are local demo responses. Real reasoning, code generation, multi-agent orchestration and sandbox execution require a separately deployed backend and model connection. This project does not claim those capabilities are already implemented.

## Open and build
1. Install Android Studio on a computer, or use a compatible Android IDE that supports Gradle Android projects.
2. Open this folder as an existing Gradle project.
3. Allow Gradle sync. The project uses Android Gradle Plugin 8.6.1, Kotlin 2.0.20, JDK 17 and Android SDK 35.
4. Build > Build APK(s).
5. Install the generated APK on Android 8.0 (API 26) or newer.

## Build on GitHub Actions
The included workflow `.github/workflows/android.yml` builds a debug APK when the project is pushed to GitHub. Download the APK from the workflow run's Artifacts section.

## Next engineering milestones
1. Add a small authenticated backend API.
2. Connect an OpenAI-compatible model endpoint without embedding secrets in the APK.
3. Add retrieval memory and source citations.
4. Add agent orchestration with explicit tool permissions.
5. Add a disposable sandbox for generated code, automated tests and rollback.
6. Require user approval before integrating generated code into the app.

## Security design
- Never put provider API keys in source code or APK resources.
- Do not execute generated shell commands directly on the Android device.
- Run generated code in an isolated server-side container with resource limits.
- Restrict cybersecurity work to owned or explicitly authorized lab systems.

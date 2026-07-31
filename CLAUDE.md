# IB TECH App - temi Robot Development Guide

## 1. Tech Stack
- Language: Kotlin
- UI Framework: Jetpack Compose (Material3)
- Architecture: MVVM Pattern
- Target Device: temi Robot (Android)

## 2. Coding Guidelines
- Do not hardcode strings; use `res/values/strings.xml`.
- Keep Composable functions small and modularized.
- Handle all Robot SDK callbacks inside ViewModel or a dedicated Repository.
- Always check if temi Robot instance is initialized before calling SDK methods.

## 3. UI/UX Rules
- Screen Resolution: Optimized for temi robot's display.
- Design System: Clean, high-visibility UI for indoor service robot interactions.
- Always provide visual feedback (e.g., status badges, loading spinners) when the robot is moving or performing tasks.
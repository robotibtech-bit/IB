# temi SDK Reference Guide

## 1. SDK Initialization & Robot Instance
To interact with temi, get the `Robot` instance:
```kotlin
val robot = Robot.getInstance()

Navigation / Movement:

robot.goTo(locationName: String) : Move robot to a saved position.

robot.saveLocation(locationName: String) : Save current position.

robot.skidJoy(x: Float, y: Float) : Manual joystick control.

Voice / TTS (Text to Speech):

robot.speak(TtsRequest.create(text, showOnScreen)) : Make temi speak.

Listeners & Callbacks:

OnGoToLocationStatusChangedListener : Monitor movement status (START, GOING, COMPLETE, CALCULATING).

OnRobotReadyListener : Triggered when temi SDK is ready.


---

### ③ `docs/ROADMAP.md` (`docs` 폴더 내에 생성)
앱 제작의 구체적인 요구사항과 단계별 과제를 정의해 둡니다.

```markdown
# temi Robot App Development Roadmap

## [ ] Phase 1: SDK Integration & Setup
- Add temi SDK dependency in `app/build.gradle.kts`.
- Create `TemiRepository.kt` to encapsulate temi `Robot` instance and listeners.

## [ ] Phase 2: Navigation & Voice Features
- Implement ViewModel for handling Robot speech (`speak`) and movement (`goTo`).
- Add status flow to track whether the robot is currently moving or standing by.

## [ ] Phase 3: Compose UI Design
- Create a main dashboard screen showing temi's current status (Location, Battery/Connection, Current Action).
- Add interactive buttons: "Move to Office", "Move to Lobby", "Speak Welcome Message".

## [ ] Phase 4: Testing & Error Handling
- Add fallback toast messages or UI alerts when navigation fails or destination is unreachable.
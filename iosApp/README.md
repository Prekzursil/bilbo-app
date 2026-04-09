# Spark iOS App

## Overview

The iOS app is built with SwiftUI and leverages Apple's Screen Time framework (FamilyControls, DeviceActivityMonitor, ManagedSettings) for app usage tracking and digital wellness features.

## Prerequisites

- Xcode 16+
- iOS 16+ deployment target
- Apple Developer account with Screen Time entitlements
- CocoaPods or Swift Package Manager

## Setup

1. Open `iosApp.xcodeproj` in Xcode
2. Select your development team under Signing & Capabilities
3. Add the following entitlements:
   - `com.apple.developer.family-controls`
4. Build the `Shared` Kotlin framework first:

```bash
cd ../
./gradlew :shared:assembleDebugXCFramework
```

5. The generated XCFramework will be at:
   `shared/build/XCFrameworks/debug/Shared.xcframework`

## Xcode Project Configuration

Link the following Apple frameworks:
- `FamilyControls.framework`
- `ManagedSettings.framework`
- `ManagedSettingsUI.framework`
- `DeviceActivity.framework`

## Extensions Required

| Extension | Type | Purpose |
|-----------|------|---------|
| `SparkDeviceActivityMonitor` | DeviceActivityMonitor | Track app usage events |
| `SparkShieldConfiguration` | ShieldConfigurationDataSource | Custom shield UI |
| `SparkShieldAction` | ShieldActionDelegate | Handle shield tap actions |

## Architecture

- **SwiftUI** for all UI
- **Shared KMP framework** for business logic, data models, and network calls
- **MVVM** pattern with `@StateObject` / `@ObservedObject` view models that bridge to KMP flows
- **Combine** for reactive bindings from KMP `StateFlow` via SKIE or manual wrappers

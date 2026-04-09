// FamilyControlsManager.swift
// Bilbo — iOS Platform
//
// Manages FamilyControls authorization lifecycle:
//   • Requests .individual authorization on first launch
//   • Degrades to tracking-only mode when denied
//   • Exposes re-request flow callable from Settings
//   • Published state observable by SwiftUI views

import Foundation
import FamilyControls
import Combine

/// Authorization states for FamilyControls
enum FamilyControlsAuthState: String, Codable {
    case notDetermined  // Never asked
    case authorized     // User approved
    case denied         // User denied or revoked
}

@MainActor
final class FamilyControlsManager: ObservableObject {

    // MARK: - Shared instance

    static let shared = FamilyControlsManager()

    // MARK: - Published state

    @Published private(set) var authState: FamilyControlsAuthState = .notDetermined
    @Published private(set) var isRequestInProgress: Bool = false
    @Published var lastError: Error?

    // MARK: - Private

    private let defaults = UserDefaults(suiteName: "group.dev.bilbo.app") ?? .standard
    private let authStateKey = "familyControlsAuthState"
    private let center = AuthorizationCenter.shared

    // MARK: - Init

    private init() {
        restorePersistedState()
    }

    // MARK: - Public API

    /// Call once on first launch. No-ops if already authorized.
    func requestAuthorizationIfNeeded() async {
        guard authState == .notDetermined else { return }
        await requestAuthorization()
    }

    /// Re-request callable from Settings, even if previously denied.
    func requestAuthorization() async {
        guard !isRequestInProgress else { return }
        isRequestInProgress = true
        defer { isRequestInProgress = false }

        do {
            try await center.requestAuthorization(for: .individual)
            // System does not return a value; absence of throw means success.
            transition(to: .authorized)
        } catch let error as FamilyControlsError {
            handleFamilyControlsError(error)
        } catch {
            lastError = error
            // Treat unknown errors as denied to avoid blocking the user.
            transition(to: .denied)
        }
    }

    /// Whether the app can enforce screen time restrictions.
    /// Returns false when denied, allowing graceful tracking-only fallback.
    var canEnforceRestrictions: Bool {
        authState == .authorized
    }

    /// True when the app is running in tracking-only mode (no enforcement).
    var isTrackingOnly: Bool {
        authState != .authorized
    }

    // MARK: - Private helpers

    private func handleFamilyControlsError(_ error: FamilyControlsError) {
        switch error {
        case .invalidAccountType:
            // Device is managed or restrictions apply; treat as denied.
            transition(to: .denied)
            lastError = error
        case .authorizationConflict:
            // Another family member / MDM already controls this.
            transition(to: .denied)
            lastError = error
        case .authorizationCanceled:
            // User dismissed — stay in notDetermined so we can ask again.
            transition(to: .notDetermined)
        case .unavailable:
            transition(to: .denied)
            lastError = error
        @unknown default:
            transition(to: .denied)
            lastError = error
        }
    }

    private func transition(to newState: FamilyControlsAuthState) {
        authState = newState
        persistState(newState)
    }

    private func persistState(_ state: FamilyControlsAuthState) {
        defaults.set(state.rawValue, forKey: authStateKey)
    }

    private func restorePersistedState() {
        guard let raw = defaults.string(forKey: authStateKey),
              let state = FamilyControlsAuthState(rawValue: raw) else {
            authState = .notDetermined
            return
        }
        authState = state
    }
}

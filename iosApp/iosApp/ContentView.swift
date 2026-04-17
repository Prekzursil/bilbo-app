import SwiftUI

struct ContentView: View {

    @StateObject private var viewModel = ContentViewModel()

    var body: some View {
        NavigationStack {
            Group {
                switch viewModel.authState {
                case .loading:
                    ProgressView("Loading…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                case .authenticated:
                    MainTabView()

                case .unauthenticated:
                    OnboardingView()
                }
            }
        }
        .onAppear {
            viewModel.checkAuthState()
        }
    }
}

// MARK: - AuthState

enum AuthState {
    case loading
    case authenticated
    case unauthenticated
}

// MARK: - ContentViewModel

@MainActor
class ContentViewModel: ObservableObject {

    @Published var authState: AuthState = .loading

    /// Resolves the initial auth state for the app.
    ///
    /// Bilbo's shared `AuthManager` is **lazy by design** — the user is never
    /// prompted to sign in until they explicitly opt into a social feature
    /// (e.g. "Find a Focus Buddy"). See
    /// `shared/src/commonMain/kotlin/dev/bilbo/auth/AuthManager.kt` for the
    /// contract.  As a result, surfacing `.unauthenticated` at launch is the
    /// correct, intended behaviour — it routes the user into onboarding, which
    /// is the entry point on a fresh install.
    ///
    /// Once the KMP→iOS `AuthManager` bridge ships, this method will consult
    /// the persisted session and transition to `.authenticated` for returning
    /// users; until then, `.unauthenticated` is not a bug.
    func checkAuthState() {
        Task {
            let isSignedIn = await Self.fetchSignedInState()
            authState = isSignedIn ? .authenticated : .unauthenticated
        }
    }

    /// Returns the restored session state.
    ///
    /// Always `false` on first launch (the app intentionally defers sign-in
    /// until a social feature is requested).  The small artificial delay keeps
    /// the splash screen visible long enough to avoid flicker.
    private static func fetchSignedInState() async -> Bool {
        try? await Task.sleep(nanoseconds: 250_000_000)
        return false
    }
}

// MARK: - Onboarding

struct OnboardingView: View {
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "bolt.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(.orange)
            Text("Bilbo")
                .font(.largeTitle.bold())
            Text("Your digital wellness companion")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Spacer()
            Button("Get Started") {
                // Navigate to sign-up flow
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding(.horizontal)

            Button("Sign In") {
                // Navigate to sign-in flow
            }
            .padding(.bottom)
        }
    }
}

#Preview {
    ContentView()
}

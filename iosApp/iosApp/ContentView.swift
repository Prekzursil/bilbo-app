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

    /// Checks the current authentication state by asking the shared KMP
    /// `AuthManager` for the active session.  Runs asynchronously so the UI
    /// can display a progress indicator while the session restore completes.
    func checkAuthState() {
        Task {
            let isSignedIn = await Self.fetchSignedInState()
            authState = isSignedIn ? .authenticated : .unauthenticated
        }
    }

    /// Thin bridge to the shared KMP auth module.  Today the shared module
    /// only exposes a Supabase-backed client stub, so we fall back to
    /// `.unauthenticated` when a session cannot be resolved.  This keeps the
    /// iOS binary compiling while the real handshake is wired up.
    private static func fetchSignedInState() async -> Bool {
        // Small artificial delay so the splash state is visible.
        try? await Task.sleep(nanoseconds: 250_000_000)
        // TODO: Replace with `AuthManager.shared.hasActiveSession()` once the
        // shared module exports the helper through its public ObjC header.
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

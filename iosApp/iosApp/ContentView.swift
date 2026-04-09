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

    func checkAuthState() {
        // TODO: Check Supabase session via shared KMP module
        // Replace with real auth check from shared module
        Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            authState = .unauthenticated
        }
    }
}

// MARK: - Placeholder Views

struct MainTabView: View {
    var body: some View {
        TabView {
            DashboardView()
                .tabItem {
                    Label("Dashboard", systemImage: "chart.bar.fill")
                }
            FocusView()
                .tabItem {
                    Label("Focus", systemImage: "timer")
                }
            InsightsView()
                .tabItem {
                    Label("Insights", systemImage: "lightbulb.fill")
                }
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
        }
    }
}

struct DashboardView: View {
    var body: some View {
        NavigationStack {
            Text("Dashboard")
                .navigationTitle("Spark")
        }
    }
}

struct FocusView: View {
    var body: some View {
        NavigationStack {
            Text("Focus Mode")
                .navigationTitle("Focus")
        }
    }
}

struct InsightsView: View {
    var body: some View {
        NavigationStack {
            Text("Insights")
                .navigationTitle("Insights")
        }
    }
}

struct SettingsView: View {
    var body: some View {
        NavigationStack {
            Text("Settings")
                .navigationTitle("Settings")
        }
    }
}

struct OnboardingView: View {
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "bolt.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(.orange)
            Text("Spark")
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

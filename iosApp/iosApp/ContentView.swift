import SwiftUI
import Shared

/// Compose(:shared)의 MainViewController 를 SwiftUI 로 래핑한다.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // BASE_URL 은 local.properties 에서 주입됨(AppConfig). 여기서 따로 넘기지 않는다.
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

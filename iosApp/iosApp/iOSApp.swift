import SwiftUI
import FirebaseCore
import FirebaseAnalytics
import Shared

/// 앱 시작 시 Firebase 를 초기화하고, Kotlin(:shared) 의 AnalyticsLogger 가 위임할 브리지를 주입한다.
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        // Kotlin/Native 는 Firebase ObjC SDK 를 직접 못 보므로, 실제 전송은 이 Swift 브리지가 담당한다.
        AnalyticsBridgeRegistry.shared.bridge = FirebaseAnalyticsBridge()
        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

/// Kotlin `AnalyticsBridge` 프로토콜을 Firebase iOS SDK 로 구현.
/// KMP 의 Map<String, Any> 는 [String: Any] 로 브리지되고, 숫자 값은 KotlinNumber(=NSNumber)로 넘어와
/// Firebase 파라미터로 그대로 사용할 수 있다.
class FirebaseAnalyticsBridge: AnalyticsBridge {
    func logEvent(name: String, params: [String: Any]) {
        Analytics.logEvent(name, parameters: params)
    }

    func setUserId(id: String?) {
        Analytics.setUserID(id)
    }

    func setUserProperty(key: String, value: String) {
        Analytics.setUserProperty(value, forName: key)
    }
}

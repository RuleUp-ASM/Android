# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# 작업 원칙 (필수)

- **최소 변경**: 사이드 이펙트가 발생하지 않도록 요청된 목표를 달성하는 최소한의 변경만 한다. 요청 범위를 벗어난 리팩터링·포맷팅·"겸사겸사" 수정은 하지 않는다.
- **추측 금지**: 명세가 명확하지 않으면 임의로 가정해 진행하지 않는다. 모호한 지점을 짚어 선택지/질문을 먼저 제시하고, 사용자의 확인을 받은 뒤 진행한다.

# 빌드 · 테스트 · 린트

JDK 21. Gradle wrapper 사용(`./gradlew`). CI(`.github/workflows/`)는 `assembleDebug` · `ktlintCheck` + `lint` · `test` 세 워크플로를 `main`/`develop` push·PR 마다 돌린다.

```bash
./gradlew assembleDebug          # 앱 빌드 (CI build.yml 과 동일)
./gradlew test                   # 전 모듈 유닛 테스트 (CI test.yml)
./gradlew ktlintFormat           # 로컬 자동 포맷 — 커밋 전 실행
./gradlew ktlintCheck            # 포맷 검사 (CI lint.yml, 실패 시 빌드 실패)
./gradlew lint                   # Android lint
```

단일 테스트 실행 (모듈 스코프로 지정해야 다른 모듈에서 "No tests found" 로 깨지지 않는다):

```bash
# android library 모듈 (대부분)
./gradlew :challenge:domain:testDebugUnitTest --tests "*CreateChallengeUseCaseTest*"
./gradlew :verification:data:testDebugUnitTest --tests "*GeofenceReconcileTest.지오펜스*"

# 순수 JVM 모듈 (:core:domain, :observability:domain) 은 variant 가 없다
./gradlew :core:domain:test --tests "*CategoryTest*"
```

## 빌드에 필요한 로컬 설정

- `local.properties` 키 → `app/build.gradle.kts` 가 `BuildConfig`·manifestPlaceholder 로 주입한다:
  `BASE_URL`(Retrofit base, `@BaseUrl`), `KAKAO_NATIVE_APP_KEY`, `KAKAO_REST_API_KEY`, `KAKAO_REDIRECT_URI`, `GOOGLE_CLIENT_ID`, `GOOGLE_REDIRECT_URI`(스킴 부분이 AppAuth redirect 로 쓰임), `AMPLITUDE_API_KEY`.
  값이 비면 빈 문자열로 주입될 뿐 빌드는 통과한다 — 런타임에 조용히 죽으므로 증상이 나오면 여기부터 본다.
- `app/google-services.json` 은 커밋하지 않는다. 파일이 **있을 때만** google-services·Crashlytics 플러그인이 적용된다(`app/build.gradle.kts` 상단 조건부 apply). CI 는 시크릿에서 복원한다.
- 버전은 전부 `gradle/libs.versions.toml` 버전 카탈로그. 동적 버전(`1.+`)은 쓰지 않는다.

# 아키텍처 (필수)

이 프로젝트는 **DDD · MVI · feature 기반 멀티모듈**을 따른다. 새 코드는 기존 컨벤션과 일관되게 작성한다.

## 모듈 지도

| 묶음 | 모듈 | 성격 |
|---|---|---|
| feature | `onboarding`, `challenge`, `profile`, `verification` (각 `data`/`domain`/`presentation`), `home:presentation` | 화면 단위 기능 |
| core | `core:domain`(JVM), `core:network`, `core:datastore`, `core:designsystem`, `core:ui` | 횡단 관심사 |
| observability | `observability:domain`(JVM), `observability:data`, `observability:debug`(debug 변형 전용) | 로깅/계측 |
| host | `app` | 컴포지션 루트 |

- `:app` 은 **전 모듈 집계점 + 컴포지션 루트**다. 내비게이션 레지스트리, Hilt 최상위 모듈, 헬퍼 구현체, FCM 수신, 딥링크 파싱이 여기 있다. 화면 코드는 여기 두지 않는다.
- convention plugin(`buildSrc`/`build-logic`)이 없다. 새 모듈은 성격이 같은 기존 모듈의 `build.gradle.kts` 를 복사해 시작한다 (예: presentation → `challenge/presentation`, domain → `challenge/domain`).
- 대부분의 모듈이 `compileSdk 37` / `minSdk 26` / JVM 11 로 각자 선언한다. `settings.gradle.kts` 의 `include` 추가도 잊지 않는다.

## 모듈 구조
- **feature 모듈**: `:<feature>:data`, `:<feature>:domain`, `:<feature>:presentation` 세 레이어로 분리한다.
- **core 모듈**: 여러 feature가 공유하는 횡단 관심사. core에는 **(a) 둘 이상의 feature가 쓰거나 (b) core 계약 시그니처에 등장하는 것**만 둔다. 소비자가 하나뿐이면 그 feature로 내린다.
- 의존 방향: `presentation → domain ← data`. domain은 다른 레이어에 의존하지 않는다.
- **feature 간 의존은 `domain` 레이어까지만 허용한다.** `data`·`presentation`을 가로질러 의존하지 않는다. 다른 feature의 능력이 필요하면 그쪽 `domain`의 계약을 직접 쓴다.
  - 타입을 core에 베껴 포트로 감싸지 않는다. 결합은 그대로 남는데 예외 타입·반환값 같은 표현력만 잃는다. (`ScreenAppBindingPort`가 그 사례였다 — `BoundScreenApp`은 `ScreenApp`을 이름만 바꾼 복제였고, 어댑터는 순수 통과였으며, 쿨다운 예외를 구분할 수 없게 만들었다.)
  - 순환은 Gradle이 빌드 단계에서 막으므로 엉킬 여지는 기계적으로 차단된다.
- 패키지 루트: `com.ruleup.<feature>.<layer>` (예: `com.ruleup.challenge.domain`). core 는 `com.ruleup.<name>` (`com.ruleup.domain`, `com.ruleup.network`, `com.ruleup.ui`, `com.ruleup.designsystem`, `com.ruleup.datastore`).
- 공개 시그니처에 노출되는 의존만 `api(...)`, 나머지는 `implementation(...)` (예: `challenge:domain` 은 `core:domain` 을 `api` 로, `observability:domain` 을 `implementation` 으로 가져간다).

## DDD (domain 레이어)
- `domain/entity/`: 도메인 모델.
- `domain/usecase/`: 단일 책임 UseCase (예: `CreateChallengeUseCase`).
- `domain/<Name>Repository.kt`: Repository **인터페이스**를 domain에 두고, 구현은 `data` 모듈에 둔다.

### UseCase를 만드는 기준
UseCase는 **여러 협력자를 엮는 조립**을 위한 것이다. repository·포트를 둘 이상 엮거나, 호출에 부수효과가 따라붙을 때만 만든다.
- 단일 repository로의 위임(인자를 그대로 넘기거나 기본값만 지정하는 경우 포함)은 UseCase를 만들지 않는다. ViewModel이 domain의 Repository 인터페이스를 직접 주입받아 호출한다.
- **협력자 없이 성립하는 비즈니스 규칙·검증·정규화는 entity 소관이다.** 규칙이 값에 붙어 있어야 그 값을 만드는 모든 경로에 규칙이 걸린다 — UseCase로 빼면 그것을 거치지 않은 경로가 규칙을 통과해 버린다.
- 인자 기본값은 Repository 인터페이스 시그니처에 둔다. UseCase가 같은 기본값을 다시 선언하지 않는다.
- 나중에 협력자가 늘면 그때 UseCase로 올린다. 미리 만들어 두지 않는다.

### data 레이어 구성
`data/api/<Feature>Api.kt`(Retrofit 인터페이스) · `data/dto/`(`@Serializable` 요청/응답) · `data/repository/<Name>RepositoryImpl.kt`(DTO ↔ entity 매핑 포함) · `data/di/`(`<Feature>NetworkModule` 은 Retrofit api 를 `@Provides`, `<Feature>RepositoryModule` 은 구현체를 `@Binds`).

## MVI (presentation 레이어)
화면별로 `viewmodel/` 패키지를 두고 다음 요소로 구성한다 (기존 `CreateChallenge*` 컨벤션):
- `<Screen>Intent`: 사용자/시스템 의도(이벤트).
- `<Screen>State`: 화면 상태 (단일 불변 상태).
- `<Screen>Effect`: 일회성 사이드 이펙트(네비게이션·토스트 등).
- `<Screen>ReducerEvent`: 상태 전이 이벤트.
- `<Screen>ViewModel`: Intent 수신 → State/Effect 방출.
- Composable 화면은 State를 구독하고 Intent를 올려보낸다(상태 호이스팅).

구현 세부:
- 네 계약 타입은 보통 한 파일 `viewmodel/<Screen>Contract.kt` 에 모으고, ViewModel 은 `viewmodel/<Screen>ViewModel.kt` 로 나눈다.
- `core:ui` 의 `MviViewModel<I, S, E, F>`(`com.ruleup.ui.mvi`)를 상속한다. `onIntent` 로 받고, 상태 변이는 **반드시** `dispatch(event)` → `reduce(state, event)` 한 곳만 거친다. `_uiState` 를 직접 만들지 않는다. 이펙트가 없는 화면은 타입 파라미터에 `NoEffect` 를 쓴다.
- 검증 계층 규칙: 범위 상수·불변식은 domain 타입에, 입력 차단은 화면(view)에 둔다. **ViewModel 에서 clamp 하지 않는다.**

# 내비게이션 (`:app` 호스트 + feature 계약)

Navigation3 기반. 경로 문자열은 한 곳에만 존재한다.

1. `core:domain` 의 `navigation/AppRoutes.kt` — 앱의 모든 path 상수 단일 소스.
2. feature `domain/navigation/<Name>Page.kt` — `Page` 인터페이스 구현. `AppRoutes` 상수를 참조하고 typed 인자를 `NavRoute(path, args: Map<String,String>)` 로 직렬화한다.
3. feature `presentation` — 화면 Composable + ViewModel.
4. `app/navigation/AppRouteRegistry.kt` 의 `appRoutes` 리스트에 `AppRoute(path=..., render={...})` **한 줄 추가**. 여기서 `isBottomTab`/`isRoot`/`isLoginRequired`/`syntheticStack` 을 정한다.

- `AppRoute.isLoginRequired` **기본값은 `true`** 이고, 이건 의도적이다. 공개로 열려면 명시적으로 꺼야 하며, 그 순간 `AppRouteAccessPolicyTest` 가 실패해 리뷰를 강제한다.
- 화면 이동은 ViewModel 이 `NavigationHelper.navigateTo(page)` / `navigateByRoute(route)` / `navigateToBack()` 로 한다. 백스택 교체(`replaceStackWith`)는 스플래시→딥링크 목적지 같은 경우만.
- 딥링크는 인증보다 먼저 도착하므로 `PendingDeepLink` 에 보류했다가 자동 로그인 성공 후 1회 소비한다(`consumeFor`). 목적지를 백스택에 먼저 깔지 않는다.
- 외부 진입은 App Links `https://android.ruleup.co.kr/inv/...`(친구 초대) 하나뿐이다. 알림 탭은 MainActivity 를 명시한 인텐트에 딥링크 URI 를 실어 처리한다 — 웹페이지가 앱 화면을 직접 여는 경로는 없다.
- `MainActivity` 는 `launchMode="singleTask"`. standard 로 바꾸면 인스턴스가 중복되어 싱글톤 `NavigationHelper` 신호를 두 NavHost 가 나눠 먹는 버그가 재발한다(#94).

# 크로스 커팅

- **헬퍼 계약은 `core:domain/helper/`, 구현은 `:app/helper/`**: `NavigationHelper`, `MessageHelper`, `PushNotificationHelper`, 그리고 `navigation/RouteAccessPolicy`. feature 가 호스트의 능력이 필요할 때 core 에 계약만 두고 `:app` 이 구현하는 패턴이다.
- **CompositionLocal**: `LocalNavigationHelper`, `LocalMessageHelper`, `LocalObservability`, `LocalScreenTracker` 만 허용된다(`.editorconfig` 의 `compose_allowed_composition_locals` allowlist). 새로 추가하면 ktlint 가 막는다.
- **DI**: Hilt. `@HiltViewModel` + 생성자 주입, 화면은 `hiltViewModel()`. 모든 `@Module` 은 `SingletonComponent` 기준이며 `:app` 의 Hilt 컴포넌트가 전 모듈 바인딩을 모은다.
- **네트워크**: `core:network/di/NetworkModule` 이 OkHttp/Retrofit/Json 을 제공한다. 액세스 토큰은 인터셉터가 자동으로 붙이되 `NO_AUTH_PATHS`(`/auth/oauth`, `/auth/signup`, `/auth/refresh`)는 제외한다 — 만료 토큰이 로그인 요청에 실리면 401 로 막힌다. 401 갱신은 `auth/TokenAuthenticator`.
- **관측**: ViewModel 은 `Observability`(`observability:domain/api`)를 주입받는다. 이벤트 카탈로그는 각 feature domain 의 `observability/<Feature>Events.kt` 에 둔다. `observability:debug` 는 `debugImplementation` 이라 릴리스 APK 에 없다.
- **디자인 시스템**: `core:designsystem` 의 `RuleUpTheme`/`RuleUpColor`/`RuleUpSpacing`/`RuleUpType` 과 `RuleUp*` 컴포넌트를 쓴다. 색·간격을 화면에서 하드코딩하지 않는다.
- **verification 모듈**은 온디바이스 신호 수집(지오펜스·사용기록·Health)과 WorkManager 주기 sync 를 담당한다. 수동 QA 시나리오·adb 명령은 `VERIFICATION_TEST_PLAN.md` 참고.

# 규칙을 강제하는 테스트

아키텍처 규칙은 문서가 아니라 테스트로 강제된다. 위반하면 `./gradlew test` 가 깨진다.

- `app/src/test/.../ArchitectureTest.kt` (Konsist): domain 은 `android.*`/`androidx.*` 를 import 하지 않는다 · domain 은 data/presentation 을 모른다 · presentation ↮ data · `*RepositoryImpl` 은 data/datastore 에만 · `*UseCase` 는 domain 에만.
- `app/src/test/.../navigation/AppRouteAccessPolicyTest.kt`: 미등록 경로는 로그인 요구, 공개 라우트 목록은 현재 비어 있어야 한다.

# 작업 워크플로우 (필수)

새로운 작업이 지시되면 **반드시** 아래 순서를 따른다. 단순 질문·조회·읽기 전용 작업은 예외다.

저장소: `RuleUp-ASM/Android` · 기본(base) 브랜치: `develop`

## 1. GitHub 이슈 생성
작업을 시작하기 전에 먼저 이슈를 만든다.

```bash
gh issue create --title "<간결한 작업 제목>" --body "<배경·목표·완료 조건>"
```

- 적절한 라벨이 있으면 `--label`로 붙인다 (`enhancement`, `bug`, `documentation` 등).
- 생성된 **이슈 번호**를 기억한다. 이후 모든 단계에서 사용한다.

## 2. 이슈 번호로 브랜치 생성
`develop`에서 분기하고, 브랜치 이름은 `<타입>/<이슈번호>` 형식으로 한다.

```bash
git fetch origin
git switch develop && git pull
git switch -c feat/<이슈번호>   # 기본은 feat/, 성격에 따라 fix/·chore/·docs/ 사용
```

기존 컨벤션 예시: `feat/20`, `feat/11`, `feat/9`.

## 3. 작업 진행
- 해당 브랜치에서만 작업한다. `develop`에 직접 커밋하지 않는다.
- 커밋·PR 메시지 본문은 한국어로 작성한다 (기존 히스토리 컨벤션).
- 커밋 메시지는 `<type>(scope): 설명` 형식을 따른다 (`feat`, `fix`, `refactor`, `docs`, `chore`, `lint`).
- 가능하면 `Closes #<이슈번호>`를 PR 본문에 포함해 이슈를 자동으로 닫는다.

## 4. 작업 단위로 쪼개서 머지
큰 작업은 한 번에 머지하지 말고 **논리적 작업 단위로 나눠** 각각 PR을 올려 머지한다.

- 각 작업 단위마다 의미 있는 커밋으로 분리한다.
- 작업 단위가 완료되면 `develop` 대상으로 PR을 만든다:

  ```bash
  git push -u origin feat/<이슈번호>
  gh pr create --base develop --head feat/<이슈번호> \
    --title "<작업 단위 제목>" --body "$(cat <<'EOF'
  ## 작업 내용
  - ...

  Closes #<이슈번호>
  EOF
  )"
  ```

- 다음 작업 단위는 머지된 `develop`에서 새로 분기하거나, 같은 이슈가 이어지면 같은 브랜치에서 다음 단위를 이어간다.
- PR 생성·머지 등 되돌리기 어려운 외부 동작은 진행 전 사용자에게 확인받는다.

## 메모
- 모든 PR의 base는 `develop` (main 아님).
- 커밋 author co-author 트레일러는 기존 설정을 따른다.

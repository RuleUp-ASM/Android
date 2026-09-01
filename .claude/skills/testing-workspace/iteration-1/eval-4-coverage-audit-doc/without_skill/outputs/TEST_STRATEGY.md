# TEST_STRATEGY.md

RuleUp Android 의 테스트 지형·공백·보강 순서를 한 곳에 모은 문서다.
목적은 "커버리지 %를 올리자"가 아니라 **깨지면 사용자가 다치는 곳부터 테스트로 못을 박는 것**이다.

기준 시점: `develop` (2026-08-31). 실행 기준: `./gradlew test` (CI `test.yml`).

---

## 1. 지금 이 레포가 쓰는 테스트 방식

문서보다 코드가 먼저다. 기존 테스트에서 읽히는 실제 컨벤션은 다음과 같다.

- **프레임워크**: 대부분 `kotlin.test`(`testImplementation(kotlin("test-junit"))`). `:app` 만 JUnit4 + Konsist, `:core:datastore` 는 `libs.junit` + `kotlinx-coroutines-test`.
- **모킹 라이브러리 없음**: MockK·Mockito·Robolectric·Turkey/Turbine 어느 것도 버전 카탈로그에 없다. 협력자는 **손으로 쓴 fake** 로 대체한다 (`challenge/domain/.../fake/FakeChallengeRepository.kt`, `onboarding/domain/.../fake/Fakes.kt`).
- **테스트 이름은 한국어 백틱**: `` fun `모르는 정렬 값은 null 이다`() ``. 이름이 곧 명세다.
- **주석은 "왜 이게 깨지면 안 되는가"**: `// 딥링크는 외부 입력이다. 모르는 경로를 공개로 보면 오타 하나가 인증 우회 통로가 된다.` 처럼 결과를 적는다. given/when/then 라벨은 쓰지 않는다.
- **공유 테스트 자산은 testFixtures**: `:observability:domain` 이 `FakeClock` / `RecordingSink` / `TestObservability` 를 `testFixtures` 로 노출하고, `:core:datastore` · `:onboarding:domain` · `:observability:data` 가 가져다 쓴다. **이 패턴을 확장하는 것이 앞으로의 기본 전략이다.**
- **계층 규칙은 테스트로 강제**: `ArchitectureTest`(Konsist) 6종, `AppRouteAccessPolicyTest` 3종.

### 테스트 계층 (이 레포에서 실제로 성립하는 것)

| 계층 | 대상 | 위치 | 도구 |
|---|---|---|---|
| 케이스 | entity·값 객체·enum 매핑·순수 함수 | `<module>/src/test` | kotlin.test |
| 모듈 | UseCase·Repository 구현·매퍼·직렬화 | `<layer>/src/test` | kotlin.test + fake + `kotlinx-coroutines-test` |
| 화면 로직 | ViewModel `reduce` / copy·포매터 함수 | `presentation/src/test` | kotlin.test (Compose 렌더링 없이) |
| 경계 | 아키텍처 규칙·라우트 정책 | `app/src/test` | Konsist |
| 계측 | `android.net.Uri` 등 프레임워크 타입 | `app/src/androidTest` | AndroidJUnit4 (**CI 미실행**) |

Compose UI 렌더링 테스트는 레포에 **한 건도 없다**. 화면 테스트는 전부 "Composable 파일 안의 순수 함수"를 부르는 방식이다 (`AppealSheetTest` 가 `reasonCounter()`, `privacyNotice()` 를 직접 부른다). 이 방식이 싸고 잘 굴러가고 있으므로, 아래 계획도 이 방식을 기본값으로 둔다.

---

## 2. 현황: 모듈별 테스트 밀도

`src/main` 의 `.kt` 파일 수 대비 테스트 파일 수. (테스트 열에는 fake·fixture 파일도 포함돼 있어 실제 테스트 클래스는 그보다 적다.)

| 모듈 | main | test | androidTest | testFixtures | 테스트 의존성 선언 | 판정 |
|---|---:|---:|---:|---:|---|---|
| `:app` | 25 | 3 | 2 | – | junit, konsist | 아키텍처만 |
| `:core:domain` | 20 | 3 | – | – | kotlin test | 부분 |
| `:core:network` | 7 | **0** | – | – | **없음** | ❗공백 |
| `:core:datastore` | 2 | 1 | – | – | junit, coroutines-test, fixtures | 양호 |
| `:core:designsystem` | 14 | 0 | – | – | 없음 | 허용 |
| `:core:ui` | 6 | **0** | – | – | **없음** | ❗공백 |
| `:onboarding:domain` | 26 | 10 | – | – | kotlin test, fixtures | 양호 |
| `:onboarding:data` | 13 | 1 | – | – | kotlin test | 부분 |
| `:onboarding:presentation` | 34 | **0** | – | – | **없음** | ❗공백 |
| `:challenge:domain` | 29 | 7 | – | – | kotlin test | 부분 |
| `:challenge:data` | 18 | **0** | – | – | **없음** | ❗❗최대 공백 |
| `:challenge:presentation` | 49 | 4 | – | – | kotlin test | 부분 |
| `:profile:domain` | 12 | **0** | – | – | kotlin test (선언만) | ❗공백 |
| `:profile:data` | 15 | 1 | – | – | kotlin test, serialization | 부분 |
| `:profile:presentation` | 27 | 1 | – | – | kotlin test | 부분 |
| `:home:presentation` | 4 | **0** | – | – | **없음** | 공백 |
| `:verification:domain` | 32 | 8 | – | – | kotlin test, coroutines-test | 양호 |
| `:verification:data` | 61 | 10 | – | – | kotlin test, serialization, coroutines-test | 양호(범위 편중) |
| `:verification:presentation` | 10 | 2 | – | – | kotlin test | 부분 |
| `:observability:domain` | 25 | 1 | – | 3 | kotlin test, self-fixtures | 양호 |
| `:observability:data` | 20 | 3 | – | – | kotlin test, fixtures | 양호 |
| `:observability:debug` | 3 | 0 | – | – | junit (선언만) | 허용 |

**잘 덮여 있는 곳** (여기는 손대지 않는다): 인증 UseCase 6종, `TokenRepositoryImpl` 의 IO 실패 경로, verification 의 신호 매핑·직렬화 계약·`VerificationRepositoryImpl` 예외 번역, observability 의 정책·싱크·TTI, 관측 이벤트 카탈로그 3종(`ChallengeEvents`, `OnboardingEvents`).

---

## 3. 비어 있는 곳 (우선순위)

### P0 — 사고가 나면 전 화면이 함께 죽는 곳

#### P0-1. `:core:network` 인증 파이프라인 — 테스트 소스셋 자체가 없음
`core/network/src/main/kotlin/com/ruleup/network/` 아래 7개 파일 전부 미테스트이고, `build.gradle.kts` 에 `testImplementation` 줄이 아예 없다.

여기서 깨질 수 있는 것들이 이미 KDoc 에 위험으로 명시돼 있는데 그중 어느 것도 테스트가 없다:

- `TokenAuthenticator.authenticate()`
  - `/auth/refresh` 자기 자신의 401 → 무한 재귀 차단
  - `responseCount >= 2` → 재시도 상한
  - 실패한 토큰과 캐시가 다르면 갱신 없이 재시도 (동시 401 경합)
  - refresh 가 **예외**(5xx·네트워크)면 토큰을 지우지 않는다 ← 지우면 멀쩡한 사용자가 로그아웃된다
  - refresh 가 **null**(세션 만료)이면 토큰을 정리한다, 단 그 사이 refreshToken 이 회전했으면 정리하지 않는다
- `NetworkModule` 의 `authInterceptor`: `NO_AUTH_PATHS`(`/auth/oauth`, `/auth/signup`, `/auth/refresh`)에 만료 토큰이 실리지 않는다 / `/auth/logout` 에는 **실린다**
- `provideRetrofit` 의 baseUrl 정규화: trailing slash 보정, 빈 값은 보정하지 않고 던진다
- `BaseResponse.getOrThrow()` / `throwOnError()` / `requireField()` — `ApiException` 의 `code`·`reason`·`retryAfterSeconds`·`rejoinAvailableAt` 이 보존되는지. **이 매핑이 아래 P0-2 의 모든 예외 번역이 딛고 서 있는 바닥이다.**

> `Authenticator` / `Interceptor` 는 OkHttp 타입이지만 순수 JVM 에서 인스턴스를 만들어 호출할 수 있다. `okhttp3.Response.Builder` 로 401 응답을 조립하고 `Interceptor.Chain` 을 fake 로 쓰면 Robolectric 없이 검증된다. `TokenRepository` · `TokenRefresher` 는 이미 domain 인터페이스라 fake 로 대체된다.

#### P0-2. `:challenge:data` — 18개 파일, 테스트 0, 의존성 선언조차 없음
레포에서 **가장 큰 무테스트 덩어리**이자, 서버 계약이 가장 자주 바뀌는 곳이다.

- 에러 코드 → 도메인 예외 번역 **11종**이 전부 미검증:
  `RECOMMENDATION_RATE_LIMITED`, `DRAFT_NOT_FOUND`/`DRAFT_EXPIRED`, `INVALID_WEEKLY_COUNT`, `CHALLENGE_NOT_FOUND`, `VERSION_CONFLICT`, `CHALLENGE_NOT_EDITABLE`, `MODERATION_LOCKED`, `JOIN_BLOCKED`(+`reason` 으로 8종 분기), `OWNER_ALREADY_EXISTS`, `WATCHER_LIMIT_EXCEEDED`, `CURSOR_INVALID`, `INVALID_SORT_TYPE`/`INVALID_FILTER_VALUE`/`NOT_CLONEABLE`
- DTO ↔ entity 매핑 함수 **48개** 미검증 (`toDomain`/`toRequest` 계열). 8개 응답 DTO 어느 것도 직렬화 라운드트립 테스트가 없다.
- `ChallengeRepositoryImpl` 의 멀티파트 이미지 업로드 경로(`ImageReader`)

`:verification:data` 가 같은 문제를 이미 잘 푼 선례다 — `VerificationRepositoryImplTest`(예외 번역 15케이스) + `VerificationDtoSerializationTest`(라운드트립 12케이스). **같은 형태를 그대로 복사해 challenge 에 적용한다.**

> `JOIN_BLOCKED` 의 `reason` 8종은 `JoinBlockReasonTest` 가 domain 쪽에서 문자열→enum 매핑만 덮고 있다. "409 응답이 실제로 `JoinBlockedException(reason)` 으로 번역되는가"는 아무도 보지 않는다.

### P1 — 화면 동작이 조용히 틀어지는 곳

#### P1-1. ViewModel 21개 전부 미테스트
`reduce(state, event)` 는 CLAUDE.md 가 "상태 변이는 반드시 이 한 곳만 거친다"고 못 박은 지점인데, 그 한 곳을 검증하는 테스트가 **0건**이다.

미테스트 ViewModel 전량:
`CreateChallengeViewModel`, `ChallengeDetailViewModel`, `ExploreListViewModel`, `ExploreViewModel`, `RankingViewModel`, `ChallengeSettingsViewModel`, `ChallengeTargetsViewModel`, `HomeViewModel`, `LoginViewModel`, `OnboardingViewModel`, `SplashViewModel`, `MyAppealsViewModel`, `MyCalendarViewModel`, `ProfileEditViewModel`, `ReputationHistoryViewModel`, `MyHomeViewModel`, `FriendInviteViewModel`, `MyStatsViewModel`, `MyTemperatureViewModel`, `VerificationLocationViewModel`, `PermissionRepairViewModel`.

전부를 한꺼번에 덮을 필요는 없다. **먼저 덮을 4개**는 상태 기계가 복잡하거나 실패 분기가 사용자에게 직접 보이는 것들이다:
- `OnboardingViewModel` — 다단계 폼(약관→생년→성별→닉네임→관심→사진), 뒤로가기·건너뛰기·검증 실패
- `SplashViewModel` — 자동 로그인 × 보류 딥링크 × 강제 업데이트 게이트 3중 분기 (여기가 틀리면 앱이 안 열린다)
- `CreateChallengeViewModel` — 초안 429 쿨다운, 만료 재발급, 확정 전 편집
- `ChallengeDetailViewModel` — 3탭 로딩·오늘 인증·이의 제출

> 걸림돌: `:core:ui`, `:onboarding:presentation`, `:home:presentation` 에는 `testImplementation` 선언이 없고, ViewModel 테스트에는 `kotlinx-coroutines-test` 가 필요하다. **각 presentation 모듈에 `kotlin("test-junit")` + `libs.kotlinx.coroutines.test` 를 추가하는 것이 선행 작업이다.**

#### P1-2. `:core:ui` 의 `MviViewModel` 베이스 자체
21개 ViewModel 이 전부 이 클래스 위에 서 있는데 테스트가 없다. 덮어야 할 것:
- `dispatch` → `reduce` 로만 상태가 바뀐다
- `effect` 는 `Channel.BUFFERED` 라 **구독자가 없어도 유실되지 않는다** (화면 회전 중 방출)
- 같은 effect 를 두 번 소비하지 않는다 (`receiveAsFlow` 특성)

#### P1-3. `:profile:domain` — 테스트 의존성은 선언돼 있는데 테스트 0
enum `fromValue` 폴백이 4종 있다. 폴백 방향이 서로 다르고(`MilestoneType` 은 `ETC` 로 떨어지고 나머지는 `null`), 이 차이가 화면에서 "칠하지 않음 vs 기타로 칠함"을 가른다:
`MilestoneType.fromValue`(→`ETC`), `ImageModerationStatus.fromValue`(→null), `NicknameCheckReason.fromValue`(→null), `CalendarDayStatus.fromValue`(→null), `DayItemStatus.fromValue`(→null), `StatsPeriod`.

#### P1-4. `:onboarding:data` 저장소 3종
`TokenRefresherImpl` 만 테스트가 있다. `AuthRepositoryImpl`(소셜 로그인·가입·로그아웃 요청 조립, 약관 6종 직렬화), `IntroRepositoryImpl`, `DeviceIdentityRepositoryImpl`(ANDROID_ID) 는 미테스트.

#### P1-5. `:profile:data` — DTO 11개 중 1개만 테스트
`CalendarResponseTest` 만 있다. `MyHomeResponse`, `MyProfileResponse`, `StatsResponse`, `ReputationResponse`, `MyChallengesSliceResponse`, `FriendInvitationResponse`, `ProfileRequest/Response` 미검증. `MyPageRepositoryImpl` · `ProfileRepositoryImpl` 도 미검증.

### P2 — 지금은 안 깨지지만 계약이 바뀌면 조용히 틀어지는 곳

- **`:challenge:domain` enum 매핑 잔여분**: `ChallengeMode`, `ChallengeVisibility`, `ChallengeStatus`, `OwnerType`(→`USER` 폴백), `JoinNote`(→`IMMEDIATE` 폴백), `MemberRole`, `RoleAction`, `DelegationStatus`, `DelegationAction`, `TodayVerificationStatus`, `VerificationType`, `VerificationMethod`, `ParamKind`(→`NUMBER` 폴백), `ThreadItemType`, `WatcherStatus`, `WatcherType`, `WatcherChannel`, `RankingMode`. `ExploreSort`·`JoinBlockReason` 만 테스트가 있다.
- **`ParamSpec.clamp(value)`** (`RoutineVerification.kt:118`) — 값 클램프는 domain 불변식인데 경계 테스트가 없다.
- **`ChallengeUpdate` / `ChallengeField` / `ChallengeConfig`** — 부분 수정 요청 조립과 낙관적 잠금(`version`).
- **`:verification:domain` 잔여 entity**: `Appeal`, `ChallengeSetup`, `DeviceIntro`, `GeofenceTarget`, `ManualSubmitResult`, `Place`, `SyncResult`, `TodayResult`, `VerificationException`, `VerificationStatus`. UseCase 3개 중 `SubmitDeviceIntroUseCase` 만 미테스트.
- **`:verification:data` 의 sync/signal 수집 계열**: `SyncScopeProviderImpl`, `EnvelopeMetadataProviderImpl`, `ProgressCacheStoreImpl`, `SyncPolicyStoreImpl`, `BootSessionProvider`, `DiagnosticsProvider`, `IntegrityTokenProvider`, `VerificationSyncWorker`, `VerificationSyncSchedulerImpl`, `SignalCollectorImpl`, `GapRecorder`, `WakeSignalProvider`, `UsageEventCollector`, `HealthConnectCollector`, `GeofenceRegisterImpl`. (`SyncGate`·`SyncOutcome`·`GeofenceReconcile` 은 이미 테스트가 있다.)
- **`:app` 호스트 로직**: `NavigationHelperImpl`(`Channel.trySend` 실패 시 신호가 **조용히 버려진다** — `println` 만 남는다), `RuleUpMessagingService`(미지 push type 폐기 규칙), `PushTokenRegister`, `ScreenTracker`, `JankTracker`, `AppNavHost` 의 `NavSignal` 처리.
- **네비게이션 `Page` 계약**: `Page.toRoute()` 가 만든 path 가 `appRoutes` 에 실제로 등록돼 있는지 검사하는 테스트가 없다. 오타가 나면 런타임에 빈 화면이 뜬다. Konsist/리플렉션 없이도 **`Page` 구현체 목록을 손으로 나열한 표 테스트** 하나면 충분하다.

### P3 — 값은 있지만 급하지 않은 곳

- 화면 포매터 순수 함수: `profile/presentation/common/Formatters.kt`, `challenge/presentation/create/ChallengeDates.kt`, `challenge/presentation/detail/component/RoomDates.kt`, `home/presentation`. (`MyAppealsScreenTest`·`TodayVerificationCopyTest` 와 같은 방식으로 싸게 덮인다.)
- `onboarding/presentation/oauth/util/PkceUtil.kt` — code_verifier/challenge 생성. 암호 관련인데 테스트가 없고, 모듈에 테스트 소스셋도 없다.
- `challenge/presentation/watcher/WatcherInviteSharer.kt`, `profile/presentation/invite/QrCode.kt`.

---

## 4. 도구·파이프라인 공백

이건 개별 테스트가 아니라 **테스트가 자랄 수 있는 땅**의 문제다.

1. **계측 테스트가 CI 에서 안 돈다.** `.github/workflows/` 에는 `assembleDebug` · `ktlintCheck`+`lint` · `test` 세 개뿐이고 `connectedAndroidTest` 가 없다. 즉 `app/src/androidTest/.../NavRouteUriParserTest.kt` 는 **작성돼 있지만 한 번도 CI 에서 실행되지 않는다.** 딥링크 파서는 외부 입력 진입점인데 사실상 무보호다.
   - 선택지 A: 에뮬레이터 워크플로 추가 (느리고 flaky)
   - 선택지 B: `android.net.Uri` 의존을 걷어내고(파싱을 순수 함수로 분리하거나 Robolectric 도입) 유닛 테스트로 내린다 ← **권장**
2. **커버리지 측정 도구가 없다.** JaCoCo·Kover 어느 쪽도 설정돼 있지 않아 이 문서의 수치가 전부 "파일 수 세기"다. 숫자로 관리할 생각이면 Kover 를 루트에 붙이는 것이 먼저다. 다만 **%를 목표로 삼지는 않는다** — 위 P0/P1 목록이 목표다.
3. **템플릿 잔재**: `app/src/test/.../ExampleUnitTest.kt`(`2+2=4`), `app/src/androidTest/.../ExampleInstrumentedTest.kt`. 지워도 잃을 게 없다.
4. **테스트 의존성 선언이 6개 모듈에 없다**: `:core:network`, `:core:ui`, `:core:designsystem`, `:challenge:data`, `:onboarding:presentation`, `:home:presentation`. 테스트를 쓰려면 이 줄부터 추가해야 한다.
5. **공유 fake 가 모듈마다 재발명된다.** `FakeChallengeRepository`, `onboarding/domain/fake/Fakes.kt` 가 각자 산다. `:observability:domain` 이 testFixtures 로 푼 방식대로, feature domain 도 fake repository 를 `testFixtures` 로 올리면 data·presentation 테스트가 그대로 재사용한다.

---

## 5. 보강 순서 (작업 단위 = PR 단위)

CLAUDE.md 의 워크플로우대로 이슈 하나 → `test/<번호>` 브랜치 → 단위별 PR 로 쪼갠다.

| # | 작업 단위 | 대상 | 왜 이 순서인가 |
|---|---|---|---|
| 1 | 테스트 소스셋 정지작업 | 6개 모듈에 `testImplementation` 추가, `Example*Test` 제거 | 이후 모든 작업의 전제 |
| 2 | `:core:network` 인증 파이프라인 | `TokenAuthenticatorTest`, `AuthInterceptorTest`, `BaseResponseTest` | 여기가 깨지면 전 화면이 401 |
| 3 | `:challenge:data` 예외 번역 | `ChallengeRepositoryImplTest`, `ExploreRepositoryImplTest`, `RoomRepositoryImplTest`, `WatcherRepositoryImplTest` | 서버 계약 변경 빈도 1위 |
| 4 | `:challenge:data` 직렬화 계약 | `ChallengeDtoSerializationTest` (verification 선례 복사) | 3과 짝 |
| 5 | `:core:ui` MVI 베이스 | `MviViewModelTest` | 21개 VM 의 바닥 |
| 6 | ViewModel 4종 | Splash / Onboarding / CreateChallenge / ChallengeDetail | 상태 기계가 가장 복잡 |
| 7 | `:profile:domain` + `:profile:data` | enum 폴백 6종, 응답 DTO 라운드트립 | 싸고 즉시 회수 |
| 8 | 네비게이션 계약 | `PageRouteRegistrationTest` (모든 `Page.PATH` 가 `appRoutes` 에 있다) | 빈 화면 사고 예방 |
| 9 | 딥링크 파서를 유닛으로 내리기 | `NavRouteUriParser` 의 `Uri` 의존 제거 또는 Robolectric | CI 에서 실제로 돌게 |
| 10 | 잔여 enum·entity·포매터 | P2·P3 목록 | 여유 있을 때 |

---

## 6. 새 코드에 대한 규칙 (앞으로)

이 문서가 다시 이렇게 길어지지 않도록, 새로 쓰는 코드는 아래를 따른다.

- **domain entity 에 `init { require(...) }` 나 `fromValue` 를 추가하면 같은 PR 에 테스트를 넣는다.** 폴백 방향(null 이냐 기본값이냐)이 곧 화면 동작이다.
- **data 레이어에서 `ApiException.code` 로 분기를 만들면 같은 PR 에 그 분기 테스트를 넣는다.** 서버 코드 문자열은 오타가 나도 컴파일이 통과한다.
- **UseCase 를 새로 만들면 협력자 조합별 테스트를 넣는다.** (UseCase 를 만들 기준 자체는 CLAUDE.md 참고 — 단일 위임이면 UseCase 를 만들지 않으므로 테스트도 필요 없다.)
- **ViewModel 은 `reduce` 를 테스트한다.** Compose 렌더링을 부르지 않는다. 화면에 보이는 문자열 조립은 `TodayVerificationCopy` 처럼 **Composable 바깥의 순수 함수로 빼서** 테스트한다.
- **관측 이벤트 카탈로그를 추가하면 이름·필수 키를 고정하는 테스트를 넣는다.** (`ChallengeEventsTest`·`OnboardingEventsTest` 선례)
- **모킹 라이브러리를 새로 들이지 않는다.** fake 로 안 되는 상황이 나오면 그때 별도 논의한다.

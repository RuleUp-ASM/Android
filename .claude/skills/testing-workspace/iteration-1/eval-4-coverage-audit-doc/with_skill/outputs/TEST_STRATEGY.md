# RuleUp 테스트 전략

마지막 갱신: 2026-08-31 (커버리지 표는 `coverage_map.py` 출력)

품질 지표는 "테스트가 몇 개인가"가 아니라 **"무엇을 아직 못 잡는가"**다. 2절 숫자는 기계가 세고, 값은 3절에 있다.

## 1. 다섯 층

| 층 | 무엇을 지키는가 | 대상 | 실행 |
|---|---|---|---|
| 케이스 | 규칙 하나 | entity·값 객체·enum·DTO 매퍼·순수 함수 | `./gradlew test` |
| 모듈 | 케이스를 엮은 한 모듈의 계약 | UseCase·RepositoryImpl·ViewModel | `./gradlew test` |
| UI | 상태 → 화면, 조작 → 의도 | Composable (Robolectric) | `./gradlew test` |
| 통합 | 모듈 경계를 건너는 결합 | 네비게이션·직렬화·아키텍처 규칙 | `./gradlew test` |
| 인수 | 사용자 스토리가 실제로 된다 | 실서버 관통 | 수동/야간 |

경로는 **갈라지는 층에서 한 번만** 검증한다. 위층은 아래층을 옳게 엮었는지만 본다.
작성 기준(이름 규칙·경로 열거법·안 하는 것)은 `.claude/skills/testing/SKILL.md`.

## 2. 현황

`python3 .claude/skills/testing/scripts/coverage_map.py` 출력. 손으로 세지 않는다.

### 층 × 모듈 커버리지

숫자는 `@Test` 개수. `–` 는 그 층 테스트가 없다는 뜻이다.

| 모듈 | 케이스 | 모듈 | UI | 통합 | 인수 | 합계 |
|---|---|---|---|---|---|---|
| `:app` | – | – | – | 18 | – | 18 |
| `:challenge:domain` | 28 | 4 | – | – | – | 32 |
| `:challenge:presentation` | 15 | – | – | – | – | 15 |
| `:core:datastore` | – | 13 | – | – | – | 13 |
| `:core:domain` | 17 | – | – | – | – | 17 |
| `:observability:data` | 20 | – | – | – | – | 20 |
| `:observability:domain` | 11 | – | – | – | – | 11 |
| `:onboarding:data` | 4 | – | – | – | – | 4 |
| `:onboarding:domain` | 8 | 30 | – | – | – | 38 |
| `:profile:data` | 3 | – | – | – | – | 3 |
| `:profile:presentation` | 4 | – | – | – | – | 4 |
| `:verification:data` | 40 | 15 | – | – | – | 55 |
| `:verification:domain` | 22 | 11 | – | – | – | 33 |
| `:verification:presentation` | 7 | – | – | – | – | 7 |
| **합계** | **179** | **73** | **0** | **18** | **0** | **270** |

테스트 파일 수: 케이스 38, 모듈 11, UI 0, 통합 5, 인수 0

### 표를 그대로 믿기 전에 알아야 할 것

숫자를 보정하지 않고 각주로 남긴다 — 표는 스크립트가 재생성하고, 아래는 사람이 안다.

- **`:app` 통합 18 중 8건은 `./gradlew test` 가 돌리지 않는다.** `ExampleInstrumentedTest`(1) · `NavRouteUriParserTest`(7) 이 `androidTest` 라 계측 실행에만 붙는다. CI(`test.yml`)는 `./gradlew test` 뿐이므로 딥링크 파서 회귀는 **초록을 통과한다**.
- **템플릿 잔재 2건**(`ExampleUnitTest` 1, `ExampleInstrumentedTest` 1)이 합계에 낀다. 실제로 규칙을 지키는 통합 테스트는 9건(`ArchitectureTest` 6 + `AppRouteAccessPolicyTest` 3)이다.
- **표에 아예 없는 모듈이 8개다**: `:challenge:data` · `:core:network` · `:core:designsystem` · `:core:ui` · `:home:presentation` · `:onboarding:presentation` · `:profile:domain` · `:observability:debug`. 행이 없다는 건 0 이라는 뜻이다.
- 스크립트의 "테스트 없는 대상"은 이름 규칙(`*UseCase`·`*ViewModel`·`*RepositoryImpl`·`*Screen`) 과 `entity/`·`model/` 안의 불변식만 본다. **DTO 매퍼와 `:core:network` 는 규칙에 안 걸려 60건 목록에 나오지 않는다** — 아래 2.1 의 상위 항목이 거기서 나왔다.

### 2.1 다음에 메울 구멍 (인프라 없이 지금 쓸 수 있는 것)

스크립트가 뽑은 60건 전체는 `--gaps` 로 다시 뽑는다. 아래는 **기반 공사 없이 오늘 쓸 수 있고, 못 잡으면 사용자에게 보이는** 순서다.

| # | 대상 | 층 | 왜 여기가 먼저인가 |
|---|---|---|---|
| 1 | `:challenge:data` DTO → entity 매퍼 (`ChallengeResponse.kt` 608줄 외 5파일) | 케이스 | `?:` 기본값이 40곳 넘는다. `weeklyCount = (config.weeklyCount ?: 3).coerceIn(1, 7)` 은 서버가 8을 줘도 **조용히 7로 만든다** — 지금은 그게 의도인지 사고인지 아무 데도 안 적혀 있다 |
| 2 | `ChallengeRepositoryImpl`·`ExploreRepositoryImpl`·`RoomRepositoryImpl`·`WatcherRepositoryImpl` | 모듈 | `ApiException.code` → 도메인 예외 변환이 15종. 화면 동작이 갈리는 지점(429 쿨다운·409 JOIN_BLOCKED·DRAFT_EXPIRED)인데 한 건도 안 잡혀 있다. 본보기는 `VerificationRepositoryImplTest` |
| 3 | `core:network` `TokenAuthenticator` · `NO_AUTH_PATHS` 인터셉터 | 모듈 | 만료 토큰이 로그인 요청에 실리면 401 로 앱이 통째로 막힌다. 전 모듈이 지나가는 길인데 테스트가 0이고 **스크립트도 못 잡는다** |
| 4 | `challenge/domain/entity/Challenge.kt` | 케이스 | 불변식이 있는데 테스트가 없다. `ChallengeLimits` 범위를 화면과 매퍼가 같이 보는 단일 출처다 |
| 5 | `verification/domain/entity/GeofenceTarget.kt` | 케이스 | `requestId = "{userId}#{challengeId}#{anchorIndex}"` 조합 규칙이 서버 보고 키다. 어긋나면 인증이 통째로 유실되는데 형식을 고정한 곳이 없다 |
| 6 | `SubmitDeviceIntroUseCase` | 모듈 | 협력자 4개를 순서대로 엮는다(capture → submit → save → reschedule). 조립 순서가 틀리면 정책이 반영 안 된 채 스케줄된다 — 딱 모듈 층이 잡을 것 |
| 7 | `:profile:data` 매퍼 (939줄 중 `CalendarResponse` 3건만 있음) | 케이스 | 1번과 같은 이유. 범위가 작아 싸다 |
| 8 | `ExampleUnitTest` · `ExampleInstrumentedTest` 삭제 | – | 템플릿 잔재. 지우면 통계가 정직해진다 |

## 3. 미검증 — 알면서 안 하고 있는 것

여기가 품질 논의를 하는 자리다. 위 2.1 은 "곧 한다", 아래는 **"조건이 생기기 전까지 안 한다"**.

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| ViewModel 전이 (24개, UI 층 대상 전부) | 인텐트 → 협력자 호출 → 상태·이펙트·네비게이션이 어긋나도 모른다. 지금 `presentation` 테스트는 Composable 에서 뽑아낸 순수 함수(케이스 층)만 본다 | 실행 기반이 없다 — `viewModelScope` 의 Main 디스패처 교체와 `NavigationHelper` 대역이 둘 다 필요하다 | `kotlinx-coroutines-test`(카탈로그에 이미 있음) 를 presentation 모듈에 붙이고 `core:domain` 에 `testFixtures` 로 `RecordingNavigationHelper` 를 둔다 → `references/viewmodel.md`. 이 한 건이 24개를 연다 |
| Compose 화면 (30개, UI 층 0) | 상태가 화면에 안 그려져도, PENDING 을 실패처럼 그려도, 정원 초과에 버튼이 안 잠겨도 CI 는 초록이다 | Robolectric 이 카탈로그에 없고, presentation 모듈에 `isIncludeAndroidResources` 설정이 없다 | `libs.versions.toml` 에 robolectric 추가 + 모듈 `testOptions` + SDK 고정 → `references/ui.md` |
| 인수 층 전체 (0건) | 서버가 필드 이름이나 정책을 바꾸면 **아래 네 층이 전부 초록인 채로** 앱이 망가진다 | 로그인 진입점이 카카오·구글 OAuth 뿐이라 자동화가 동의 화면을 통과하지 못한다 | BE 테스트 토큰 발급 경로. 그 전까지는 수동 획득 토큰을 `RULEUP_ACCEPTANCE_TOKEN` 으로 주입해 같은 테스트를 돌린다 → `references/acceptance.md` |
| 딥링크 URI 파서 (`NavRouteUriParserTest`) | 테스트는 7건 있는데 **CI 가 안 돌린다.** 파서 회귀가 초록을 통과한다 | `android.net.Uri` 가 프레임워크 타입이라 계측 테스트로 뒀다 | Robolectric 이 들어오면 `src/test` 로 이관한다 (위 UI 항목과 같은 공사에 얹힌다) |
| 런타임 권한·지오펜스 OS 등록·Health 연동 | 권한 거부 후 복구 동선이 막혀도, 지오펜스가 실제로 안 걸려도 모른다 | Robolectric 도 JVM 도 OS 다이얼로그와 지오펜스 트리거를 흉내내지 못한다 | 에뮬레이터 CI 워크플로. 그전까지는 `VERIFICATION_TEST_PLAN.md` 의 수동 QA 시나리오가 이 자리를 대신한다 |
| Hilt 그래프 조립 · `:app` 헬퍼 구현체 · `:core:designsystem` | – | **의도적으로 안 한다.** DI 가 못 엮으면 빌드가 깨지고, 색·간격·토큰은 코드가 이미 말한다 (`SKILL.md` "무엇을 테스트하지 않는가") | 안 한다. 단 "릴리스에서 debug sink 가 빠진다" 처럼 **바인딩 자체가 정책**이 되면 그때 통합 층 1건 |

## 4. 인수 시나리오 ↔ 하위 테스트

인수가 깨졌는데 하위 층이 전부 초록이었다면 하위 층에 구멍이 있다는 뜻이다. **그 구멍이 다음 작업이다.**
아래 스토리는 지금 앱에 실제로 있는 동선(`AppRouteRegistry`)에서만 뽑았다.

| 스토리 | 상태 | 미리 잡아주는 하위 테스트 |
|---|---|---|
| 소셜 로그인 → 온보딩 6단계 → 홈 | 미구축 | `SocialLoginUseCaseTest`·`SignupUseCaseTest`·`ValidateBirthDateUseCaseTest`·`SignupFormTest`·`TokenRepositoryImplTest` / **구멍**: `LoginViewModel`·`OnboardingViewModel` 전이 |
| 앱 재실행 → 자동 로그인 → 보류 딥링크 1회 소비 | 미구축 | `AutoLoginUseCaseTest`·`PendingDeepLinkTest`·`TokenRefresherImplTest` / **구멍**: `TokenAuthenticator`(2.1-3), `SplashViewModel` |
| 챌린지 생성(입력 → 확인 → 생성) → 내 목록에 보임 | 미구축 | `CreateChallengeCommandTest`(입력 규칙)·`CreateChallengeUseCaseTest`(조립)·`RoutineDescriptionTest` / **구멍**: `ChallengeRepositoryImpl` 예외 변환(2.1-2), `CreateChallengeViewModel` |
| 탐색 → 카테고리 목록 → 상세 → 참여 | 미구축 | `ExploreTest`·`JoinBlockReasonTest`·`CategoryTest` / **구멍**: `ExploreRepositoryImpl` 커서·필터 예외(2.1-2), 목록 DTO 매퍼(2.1-1) |
| 인증 셋업(장소 핀 → 지오펜스 등록) → 오늘 인증 반영 | 미구축 | `AnchorSetTest`·`BindLocationUseCaseTest`·`GeofenceReconcileTest`·`TodayStatusTest`·`LocationChangeLockTest` / **구멍**: `GeofenceTarget` requestId 형식(2.1-5), 실제 OS 트리거(3절) |
| 주기 sync: 신호 수집 → 전송 → 정책 갱신 | 미구축 | `RunSyncUseCaseTest`·`SyncGateTest`·`SyncOutcomeTest`·`SignalBatchSplitTest`·`VerificationRepositoryImplTest`·`VerificationDtoSerializationTest` / **구멍**: `SubmitDeviceIntroUseCase`(2.1-6) |
| 친구 초대 링크(`/inv`) 열기 → 로그인 후 목적지 도착 | 미구축 | `PendingDeepLinkTest`·`AppRouteAccessPolicyTest` / **구멍**: `NavRouteUriParserTest` 가 CI 에서 안 돈다(3절), `FriendInviteViewModel` |
| 푸시 알림 탭 → 딥링크 목적지 | 미구축 | `AppRouteAccessPolicyTest` / **구멍**: 파서(위와 동일), `RuleUpMessagingService` 페이로드 파싱 |

"구멍"으로 적힌 것이 2.1 과 3절의 우선순위 근거다. 두 스토리 이상에서 반복되는 구멍(딥링크 파서, ViewModel 전이)이 가장 비싸다.

## 5. 돌리는 법

    ./gradlew test                      # 케이스·모듈·UI·통합 (CI test.yml 과 동일)
    ./gradlew ktlintFormat              # 커밋 전
    ./gradlew ktlintCheck lint

    # 단일 테스트는 모듈 스코프를 반드시 준다 — 안 주면 다른 모듈에서 "No tests found" 로 깨진다
    ./gradlew :challenge:domain:testDebugUnitTest --tests "*CreateChallengeCommandTest*"   # android library
    ./gradlew :core:domain:test --tests "*CategoryTest*"                                   # 순수 JVM

    # 커버리지 표 재생성
    python3 .claude/skills/testing/scripts/coverage_map.py
    python3 .claude/skills/testing/scripts/coverage_map.py --gaps

    # 인수 (아직 없음 — 3절 참조)
    RULEUP_ACCEPTANCE=1 RULEUP_ACCEPTANCE_TOKEN=... \
      ./gradlew :app:testDebugUnitTest --tests "*AcceptanceTest*"

verification 수동 QA 시나리오·adb 명령은 `VERIFICATION_TEST_PLAN.md`.

### 갱신 규칙

테스트를 늘리거나 줄이는 PR 은 이 문서도 같이 고친다 — 코드와 같은 PR 에 있어야 안 썩는다.
2절 표는 스크립트 출력을 붙이고, 3절은 손으로 쓴다(왜 안 했는지가 값이고 그건 기계가 모른다).
채운 항목은 지우지 말고 완료로 옮긴다.

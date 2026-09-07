# RuleUp 테스트 전략

마지막 갱신: 2026-09-01 (커버리지 표는 `coverage_map.py` 출력)

품질 지표는 "테스트가 몇 개인가"가 아니라 **"무엇을 아직 못 잡는가"**다. 이 문서의 중심은 2절 숫자가 아니라 3절 미검증 목록이다.

## 1. 다섯 층

| 층 | 무엇을 지키는가 | 대상 | 실행 |
|---|---|---|---|
| 케이스 | 규칙 하나 | entity·값 객체·매퍼·순수 함수 | `./gradlew test` |
| 모듈 | 한 모듈의 계약 | UseCase·RepositoryImpl·ViewModel | `./gradlew test` |
| UI | 상태 → 화면, 조작 → 의도 | Composable (Robolectric) | `./gradlew test` |
| 통합 | 모듈 경계를 건너는 결합 | 네비게이션·직렬화·아키텍처 규칙 | `./gradlew test` |
| 인수 | 사용자 스토리 | 실서버 관통 | 수동/야간 |

경로는 갈라지는 층에서 한 번만 검증한다. 위층은 아래층을 옳게 엮었는지만 본다.
작성 기준은 `.claude/skills/testing/SKILL.md`.

층은 파일 이름이 아니라 **"이 규칙이 깨지면 어느 파일을 고치나"**로 고른다. 그래서 `MyAppealsScreenTest`·`AppealSheetTest` 처럼 이름에 Screen 이 붙어 있어도 Composable 에서 뽑아낸 순수 함수를 보는 것은 **케이스** 층이다. 아래 표의 UI 열이 0 인 것도 그래서다 — 화면을 실제로 띄우는 테스트가 하나도 없다.

## 2. 현황

`python3 .claude/skills/testing/scripts/coverage_map.py` 출력.

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

**표를 액면 그대로 읽지 않는다.** 두 가지가 숫자를 부풀린다.

- `:app` 의 18 중 **8 은 CI 가 돌리지 않는다.** `src/androidTest` 의 `NavRouteUriParserTest`(7) · `ExampleInstrumentedTest`(1) 는 계측 테스트인데 `test.yml` 은 `./gradlew test` 만 돌린다. 실제로 CI 를 지키는 건 `ArchitectureTest`(6) · `AppRouteAccessPolicyTest`(3) 뿐이다.
- `ExampleUnitTest`(`2 + 2 == 4`) · `ExampleInstrumentedTest` 는 프로젝트 템플릿 잔재다. 지우면 합계가 2 줄고 정확해진다.
- `:challenge:data` · `:onboarding:presentation` · `:home:presentation` · `:profile:domain` 은 `src/test` 디렉터리 자체가 없다.

### 테스트가 없는 대상 — 58건

전체 목록은 `python3 .claude/skills/testing/scripts/coverage_map.py --gaps` 로 뽑는다. 종류별 집계는 이렇다.

| 종류 | 요구 층 | 건수 |
|---|---|---|
| 화면(`*Screen.kt`) | UI | 26 |
| ViewModel | 모듈 | 21 |
| RepositoryImpl | 모듈 | 10 |
| UseCase | 모듈 | 1 (`SubmitDeviceIntroUseCase`) |
| 불변식 있는 entity | 케이스 | **0** |

읽는 법: **케이스 층은 이미 촘촘하다**(불변식을 가진 entity 중 테스트 없는 것이 0건). 구멍은 전부 그 위 — ViewModel·RepositoryImpl 이 모듈 층에서 통째로 비어 있고, UI 층은 실행 기반부터 없다.

## 3. 미검증 — 알면서 안 하고 있는 것

품질 논의는 이 절에서 한다. **풀리는 조건**이 "인프라를 세운다"인 항목이 다음 작업이고, "에뮬레이터 CI"·"BE 토큰"인 항목은 당분간 수동 QA 에 맡긴다.

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| UI 층 전체 (화면 26개) | 상태가 화면에 잘못 그려져도 모른다 — PENDING 을 실패처럼 그리기, 잠겨야 할 참여 버튼이 눌리기 | Robolectric 실행 기반이 없다. `libs.versions.toml` 에 robolectric 항목 자체가 없고 presentation 모듈에 `testOptions.unitTests.isIncludeAndroidResources` 도 없다 | 카탈로그 robolectric 추가 + `debugImplementation(ui-test-manifest)` + testOptions. 기대값 출처는 Figma `1134:2` (`references/ui.md`) |
| ViewModel 21개 | MVI 의 intent → reduce → state/effect 결선이 통째로 미검증. 큰 것부터 `ChallengeDetailViewModel`(1005줄) · `CreateChallengeViewModel`(602) · `VerificationLocationViewModel`(334) · `OnboardingViewModel`(321) | presentation 모듈에 `kotlinx-coroutines-test` 가 없어 `runTest`·Main 디스패처 교체를 못 한다 | 모듈별 `testImplementation(libs.kotlinx.coroutines.test)` + `MainDispatcherRule` + Fake repository (`references/viewmodel.md`) |
| HTTP → 도메인 예외 변환 (`ChallengeRepositoryImpl`) | 서버가 코드·reason 키를 바꾸면 화면이 "쿨다운 카운트다운"·"가입 차단 사유" 대신 일반 에러를 띄우는데 조용히 지나간다. 429→`RecommendationRateLimitedException`, 409 `JOIN_BLOCKED`+reason→`JoinBlockedException` 이 여기서 갈린다 | `:challenge:data` 에는 `src/test` 도 `testImplementation` 도 없다 | Fake Api + `testImplementation(kotlin("test-junit"))`. `VerificationRepositoryImplTest` 가 같은 층의 본보기다 |
| 딥링크 파싱 (`NavRouteUriParser`) | 알림·초대 링크가 엉뚱한 화면으로 가도 CI 는 초록이다. 테스트 7개가 이미 있는데 **CI 가 실행하지 않는다** | `android.net.Uri` 의존이라 JVM 에서 못 돌아 `src/androidTest` 에 있고, `test.yml` 은 `./gradlew test` 만 돌린다 | 위 UI 인프라와 같은 조건 — Robolectric 이 들어오면 `src/test` 로 옮긴다 |
| 401 갱신·인증 헤더 (`TokenAuthenticator`, `NetworkModule` 인터셉터) | 갱신 재귀 차단이나 `NO_AUTH_PATHS` 제외가 깨지면 **로그인 자체가 막히는데** 테스트가 하나도 없다 | `:core:network` 에 `testImplementation` 이 없고 MockWebServer 를 들여오지 않았다 | MockWebServer 를 카탈로그에 넣을지 결정 |
| 런타임 권한 다이얼로그 (verification) | 권한 거부 후 복구 동선(`PermissionRepairScreen`)이 막혀도 모른다 | Robolectric 이 시스템 권한 다이얼로그를 못 흉내낸다 | 에뮬레이터 CI 워크플로. 그전까지는 `VERIFICATION_TEST_PLAN.md` 수동 QA |
| 지오펜스·사용기록 실제 수집 | OS 가 콜백을 안 주면 인증이 통째로 안 된다 | 실제 단말 신호가 필요하다. 순수 판정 로직(`GeofenceReconcileTest`·`SyncGateTest`)은 이미 케이스 층이 잡는다 | 실기기 수동 QA — `VERIFICATION_TEST_PLAN.md` |
| WorkManager 30분 주기 sync 스케줄 | 워커가 등록·재등록되지 않아도 모른다. sync **내용**은 `RunSyncUseCaseTest` 가 잡지만 **호출된다는 사실**은 아무도 안 본다 | `work-testing` 미도입 | `androidx.work:work-testing` |
| 인수 층 전체 | 실서버 스토리를 하나도 못 돈다 | 로그인이 카카오·구글 OAuth 뿐이라 헤드리스로 토큰을 얻을 수 없다 | BE 테스트 토큰 발급 경로 (`references/acceptance.md`) |

### 안 채우기로 한 것

- **얇은 위임 RepositoryImpl** — `IntroRepositoryImpl`(34줄) · `WatcherRepositoryImpl`(46줄) · `RoomRepositoryImpl`(67줄) 처럼 DTO 를 그대로 넘기는 구현. 깨지면 컴파일이 먼저 깨진다. 매핑 규칙이나 예외 변환이 붙는 순간 위 표로 올린다.
- **`SubmitDeviceIntroUseCase`** — 협력자 하나로의 단순 위임. UseCase 판정 기준(`CLAUDE.md`)상 협력자가 늘 때 같이 테스트를 단다.
- **Hilt 배선 자체** — 못 엮이면 빌드가 깨진다. 단 "릴리스에 debug sink 가 빠지는가"처럼 **정책**인 바인딩은 예외이며, 지금은 `debugImplementation` 구성이 그것을 보장한다.

## 4. 인수 시나리오 ↔ 하위 테스트

인수가 깨졌는데 하위 층이 전부 초록이었다면 하위 층에 구멍이 있다는 뜻이다. **그 구멍이 다음 작업이다.**

| 스토리 | 상태 | 미리 잡아주는 하위 테스트 | 비어 있는 곳 |
|---|---|---|---|
| 소셜 로그인 → 온보딩 5단계 → 홈 | 미구축 | `SocialLoginUseCaseTest`·`SignupUseCaseTest`(조립), `SignupFormTest`·`ValidateBirthDateUseCaseTest`(입력 규칙), `AppRouteAccessPolicyTest`(보호 경로) | `OnboardingViewModel`·`LoginViewModel`, 온보딩 화면 6종 |
| 앱 재실행 → 자동 로그인 → 만료 토큰 갱신 | 미구축 | `AutoLoginUseCaseTest`, `TokenRepositoryImplTest`, `TokenRefresherImplTest`(401·SESSION_EXPIRED 판정) | `TokenAuthenticator`(재귀 차단), `SplashViewModel` |
| 챌린지 생성 → 확인 → 내 목록에 보임 | 미구축 | `CreateChallengeCommandTest`(입력 규칙), `CreateChallengeUseCaseTest`(조립), `RoutineDescriptionTest` | `CreateChallengeViewModel`, `ChallengeRepositoryImpl`, 생성·확인 화면 |
| 탐색 → 상세 → 참여, 막히면 사유가 보인다 | 미구축 | `ExploreTest`(필터·정렬), `JoinBlockReasonTest`(차단 사유 종류) | `ChallengeRepositoryImpl` 의 409 `JOIN_BLOCKED` 변환, `ExploreViewModel`·`ChallengeDetailViewModel`, 상세 화면 |
| 위치 앵커 등록 → 지오펜스 → 30분 sync → 오늘 상태 갱신 | 부분 (수동 QA) | `AnchorSetTest`, `GeofenceReconcileTest`·`GeofenceResponsivenessTest`, `SyncGateTest`·`SyncOutcomeTest`, `RunSyncUseCaseTest`, `TodayStatusTest`, `VerificationRepositoryImplTest` | `VerificationLocationViewModel`, 워커 스케줄 등록, 실제 OS 콜백(=수동) |
| 실패한 인증에 이의제기 | 미구축 | `AppealSheetTest`·`MyAppealsScreenTest`(문구·이력 표기, 케이스 층) | 이의제기 창 마감 판정 경로(`AppealWindowClosedException`), `MyAppealsViewModel` |
| 친구 초대 링크(`/inv/...`)로 앱 열기 → 로그인 후 1회 소비 | 미구축 | `PendingDeepLinkTest`(보류·1회 소비), `NavRouteUriParserTest`(**CI 미실행**) | 파서 테스트를 CI 가 도는 곳으로 옮기기 |

## 5. 돌리는 법

```
./gradlew test                      # 케이스·모듈·UI·통합 (CI test.yml 과 동일)
./gradlew ktlintFormat              # 커밋 전
./gradlew :<module>:testDebugUnitTest --tests "*XxxTest*"   # android library 모듈
./gradlew :core:domain:test --tests "*CategoryTest*"        # 순수 JVM 모듈

RULEUP_ACCEPTANCE=1 RULEUP_ACCEPTANCE_TOKEN=... \
  ./gradlew :app:testDebugUnitTest --tests "*AcceptanceTest*"   # 인수 (미구축)
```

모듈 스코프를 반드시 지정한다 — 안 하면 다른 모듈에서 "No tests found" 로 깨진다.

verification 수동 QA 시나리오와 adb 명령은 `VERIFICATION_TEST_PLAN.md`.

## 갱신 규칙

테스트를 늘리거나 줄이는 PR 은 이 문서도 같이 고친다. 코드와 같은 PR 에 있어야 안 썩는다.

- 2절 현황 표는 손으로 세지 않는다 — `coverage_map.py` 출력을 붙인다
- 3절 미검증 목록은 손으로 쓴다 — 왜 안 했는지가 값이고, 그건 기계가 모른다
- 채운 항목은 지우지 말고 완료로 옮긴다 — 무엇을 언제 메웠는지가 다음 판단의 근거다

# RuleUp 테스트 전략

마지막 갱신: 2026-09-01 (현황 표는 `coverage_map.py` 출력)

품질 지표는 "테스트가 몇 개인가"가 아니라 **"무엇을 아직 못 잡는가"**다. 이 문서의 중심은 3절이다.

verification 수동 QA 시나리오는 성격이 달라 `VERIFICATION_TEST_PLAN.md` 에 그대로 둔다.

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

## 2. 현황

<!-- python3 .claude/skills/testing/scripts/coverage_map.py -->

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

### 표가 말하지 않는 것

숫자만 보면 놓치는 사실 넷. 다음 판단은 여기서 나온다.

- **`:app` 18 중 8 은 CI 에서 안 돈다.** `app/src/androidTest/` 의 `NavRouteUriParserTest`(7) + `ExampleInstrumentedTest`(1) 는 계측 테스트다. CI(`test.yml`)는 `./gradlew test` 만 돌리고 에뮬레이터 워크플로가 없으므로 **딥링크 파서 7건은 어디서도 초록이 아니다**. 파서가 `android.net.Uri` 를 쓰는 게 이유였다(파일 KDoc 참고) — Robolectric 이 들어오면 유닛으로 내릴 수 있다.
- **`:app` 18 중 1 은 템플릿 잔재다.** `ExampleUnitTest`(2 + 2 = 4). 실질 통합 층은 `ArchitectureTest`(6) + `AppRouteAccessPolicyTest`(3) 9건이다.
- **presentation 의 "케이스 26건"은 화면 테스트가 아니다.** `:challenge:presentation` 15, `:profile:presentation` 4, `:verification:presentation` 7 은 전부 Composable 에서 뽑아낸 순수 함수(문구 조립·행 계산·잠금 조건)를 본다. 화면이 그려지는지는 아무도 안 본다.
- **테스트 소스셋이 아예 없는 모듈이 있다.** `:challenge:data`, `:onboarding:presentation`, `:home:presentation`, `:core:network`, `:core:ui`, `:core:designsystem`. `:profile:domain` 은 소스셋 선언만 있고 테스트가 0이다.

### 테스트가 없는 대상 — 58건

전체 목록은 `python3 .claude/skills/testing/scripts/coverage_map.py --gaps`. 여기서는 성격별 집계만 둔다.

| 모듈 | 화면(UI) | ViewModel | RepositoryImpl | UseCase | 계 |
|---|---|---|---|---|---|
| `:challenge:data` | – | – | 4 | – | 4 |
| `:challenge:presentation` | 8 | 7 | – | – | 15 |
| `:home:presentation` | 1 | 1 | – | – | 2 |
| `:onboarding:data` | – | – | 3 | – | 3 |
| `:onboarding:presentation` | 8 | 3 | – | – | 11 |
| `:profile:data` | – | – | 2 | – | 2 |
| `:profile:presentation` | 7 | 8 | – | – | 15 |
| `:verification:data` | – | – | 1 | – | 1 |
| `:verification:domain` | – | – | – | 1 | 1 |
| `:verification:presentation` | 2 | 2 | – | – | 4 |
| **합계** | **26** | **21** | **10** | **1** | **58** |

**58건을 다 메우지 않는다.** 화면 26 + ViewModel 21 은 실행 기반이 없어서 비어 있는 것이라 개별 항목이 아니라 한 덩어리의 결정이다(3절 UI-1·VM-1). 아래가 지금 바로 메울 수 있는 것들이다.

### 2-1. 다음에 메울 것

| 대상 | 층 | 왜 지금 | 비용 |
|---|---|---|---|
| `SubmitDeviceIntroUseCase` | 모듈 | `:verification:domain` 에 이미 Fake·`runTest` 기반이 있고, UseCase 3개 중 이것만 비어 있다 | 파일 하나 |
| `NavRouteUriParser` 를 유닛으로 내리기 | 케이스 | 딥링크는 인증보다 먼저 도착하는 경로인데 검증이 CI 밖에 있다. Robolectric 도입 시 `app/src/test/` 로 이사 | 이사 + 러너 교체 |
| `ChallengeRepositoryImpl`·`ExploreRepositoryImpl` DTO↔entity 매핑 | 모듈 | `:challenge:data` 는 테스트 소스셋조차 없다. 응답 nullable 이 기본값으로 뭉개지는 지점이 여기다 | 소스셋 + Fake api |
| `challenge`·`profile` DTO 직렬화 왕복 | 케이스 | `:verification:data` 의 `VerificationDtoSerializationTest` 가 이미 본이다. `:profile:data` 는 `CalendarResponseTest` 하나뿐 | 얕음 |
| `:profile:domain` entity(`Reputation`·`StatsReport`·`ActivityCalendar`) | 케이스 | 테스트 0. 계산·포맷 규칙이 화면 순수 함수 쪽에만 걸려 있다 | 얕음 |

## 3. 미검증 — 알면서 안 하고 있는 것

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| **UI-1. Composable 26개 전부** | 상태가 화면에 안 그려지거나, 누른 게 의도로 안 올라가도 모른다. PENDING 을 실패처럼 그리는 종류의 회귀는 수동 QA(TC-05)에서만 잡힌다 | Robolectric 이 `libs.versions.toml` 에 아예 없다. 층 전체가 실행 기반 미구축 | Robolectric + `ui-test-junit4` 를 카탈로그에 넣고 `:challenge:presentation` 에 첫 화면 테스트 한 개 (`references/ui.md`) |
| **VM-1. ViewModel 21개 전부** | intent → state 전이, effect 방출, 에러 → 화면 문구 변환이 전부 무검증. 반면 아래 domain 층은 촘촘하다 — 위험이 조립부에 몰려 있다 | `kotlinx-coroutines-test` 가 `:core:datastore`·`:verification:*` 에만 있다. presentation 모듈에는 Main 디스패처 규칙도 Fake NavigationHelper 도 없다 | `MainDispatcherRule` + Fake helper 를 한 모듈에 세우고 나머지가 복사 (`references/viewmodel.md`) |
| **NET-1. OkHttp 인터셉터 · `TokenAuthenticator`** | `NO_AUTH_PATHS` 가 어긋나 만료 토큰이 `/auth/*` 에 실리면 로그인이 401 로 막힌다. 401 갱신 재시도·동시 갱신 중복도 무검증 | MockWebServer 미도입. `:core:network` 에 테스트 소스셋이 없다 | MockWebServer 도입. 갱신 로직 자체는 `TokenRefresherImplTest` 가 부분적으로 덮는다 |
| **DB-1. Room DAO 실제 쿼리** | 버퍼 적재·조회·삭제 쿼리가 틀려도 모른다. 지금은 컨버터(`VerificationTypeConvertersTest`)와 매퍼만 덮인다 | in-memory Room 은 Robolectric 이나 계측 실행이 필요하다 | UI-1 과 같은 조건(Robolectric) |
| **WORK-1. `VerificationSyncWorker` 스케줄링** | 주기 sync 가 안 걸리거나 중복 등록돼도 모른다. `RunSyncUseCase` 는 "무엇을 보내나"만 보고 "언제 도나"는 안 본다 | `work-testing` 미도입 | `work-testing` + Robolectric |
| **PUSH-1. FCM 수신 → 딥링크 인텐트** | 알림 탭이 엉뚱한 화면을 열거나 아무 데도 안 가도 모른다 | `RuleUpMessagingService` 가 프레임워크에 붙어 있고, 페이로드 계약이 BE 와 아직 미확정 | 페이로드 계약 확정 + Robolectric |
| **PERM-1. 런타임 권한 다이얼로그·설정 복귀** | 권한 거부 후 복구 동선이 막혀도 모른다 | Robolectric 이 시스템 다이얼로그와 설정 화면 복귀를 못 흉내낸다 | 에뮬레이터 CI 워크플로. 그때까지는 `VERIFICATION_TEST_PLAN.md` 0-2 절이 담당 |
| **ACC-1. 인수 테스트 전부(0건)** | 실서버를 관통하는 스토리를 하나도 못 돈다. 4절이 전부 "미구축"인 이유 | 로그인이 카카오·구글 OAuth 뿐이라 자동화가 진입부터 막힌다 | BE 테스트 계정 토큰 발급 경로 (`references/acceptance.md`) |
| **CI-1. 계측 테스트가 CI 에서 안 돈다** | `app/src/androidTest/` 8건(딥링크 파서 7 포함)이 회귀를 못 막는다 | 에뮬레이터 워크플로가 없다 | 파서는 Robolectric 으로 유닛에 내리는 게 싸다(2-1). 나머지는 에뮬레이터 CI |

의도적으로 **안 넣은 것**: data class 의 `copy`·`equals`, Hilt 배선 자체, Composable 의 색·간격, 앱이 안 읽는 서버 응답 필드. 기준은 SKILL.md "무엇을 테스트하지 않는가".

## 4. 인수 시나리오 ↔ 하위 테스트

인수가 깨졌는데 하위 층이 전부 초록이었다면 하위 층에 구멍이 있다는 뜻이다. 그 구멍이 다음 작업이다.
지금 인수 층은 0건이라(3절 ACC-1) 오른쪽 칸이 곧 **그 스토리에서 지금 믿을 수 있는 전부**다.

| 스토리 | 상태 | 미리 잡아주는 하위 테스트 | 안 덮인 구간 |
|---|---|---|---|
| 소셜 로그인 → 온보딩 → 홈 | 미구축 | `SocialLoginUseCaseTest`·`SignupUseCaseTest`·`AutoLoginUseCaseTest`·`ValidateBirthDateUseCaseTest`·`SignupFormTest`·`TokenRepositoryImplTest` | `LoginViewModel`·`OnboardingViewModel`·`SplashViewModel`, 온보딩 8화면 |
| 챌린지 생성 → 내 목록에 보임 | 미구축 | `CreateChallengeCommandTest`(입력 규칙)·`CreateChallengeUseCaseTest`(조립)·`RoutineDescriptionTest` | `CreateChallengeViewModel`, 입력·확인 화면, `ChallengeRepositoryImpl` 매핑 |
| 탐색 → 방 참여 | 미구축 | `ExploreTest`·`JoinBlockReasonTest`·`CategoryTest` | `ExploreViewModel`·`ExploreListViewModel`, `ExploreRepositoryImpl`·`RoomRepositoryImpl` |
| 장소 인증 설정 → 지오펜스 등록 | 미구축 | `BindLocationUseCaseTest`·`AnchorSetTest`·`GeofenceReconcileTest`·`GeofenceResponsivenessTest` | `VerificationLocationViewModel`, 지도 화면, 런타임 권한(PERM-1) |
| 하루 지내기 → sync → 상세에 오늘 상태 | 미구축 | `RunSyncUseCaseTest`·`SyncGateTest`·`SyncOutcomeTest`·`SignalBatchSplitTest`·`TodayStatusTest`·`VerificationRepositoryImplTest`·`TodayVerificationCopyTest` | 워커 스케줄링(WORK-1), 상세 화면 렌더 — 수동 TC-01·TC-05 가 대신하는 중 |
| 실패 → 이의제기 제출 → 이력 확인 | 미구축 | `AppealSheetTest`·`MyAppealsScreenTest`(문구 조립) | `MyAppealsViewModel`, 제출 경로 전체(RepositoryImpl 무검증) |
| 친구 초대 링크로 앱 열기 → 방 진입 | 미구축 | `PendingDeepLinkTest`(보류·1회 소비), `NavRouteUriParserTest`(계측이라 CI 밖 — CI-1) | `FriendInviteViewModel`, 인증 전 도착 시 실제 합류 |
| 마이 홈 → 통계·온도·평판 | 미구축 | `CalendarResponseTest` 하나뿐 | `:profile:domain` entity 전부, 마이 VM 8개·화면 7개 |

## 5. 돌리는 법

    ./gradlew test                      # 케이스·모듈·UI·통합 (CI 와 동일)
    ./gradlew ktlintFormat              # 커밋 전
    ./gradlew :<module>:testDebugUnitTest --tests "*XxxTest*"   # android library
    ./gradlew :core:domain:test --tests "*CategoryTest*"        # 순수 JVM (variant 없음)

    python3 .claude/skills/testing/scripts/coverage_map.py          # 2절 갱신
    python3 .claude/skills/testing/scripts/coverage_map.py --gaps   # 구멍 전체 목록

인수 층은 아직 없다(3절 ACC-1). 생기면 여기에 실행 방법을 적는다.
verification 수동 QA 시나리오는 `VERIFICATION_TEST_PLAN.md`.

---

이 문서는 테스트를 늘리거나 줄이는 PR 에서 같이 고친다. 2절은 스크립트 출력을 붙이고, 3절은 손으로 쓴다 —
왜 안 했는지가 값이고 그건 기계가 모른다. 채운 항목은 지우지 말고 완료로 옮긴다.

# TEST_STRATEGY.md

이 문서는 **지금 무엇이 검증되고 있고, 무엇이 비어 있으며, 어느 순서로 메울지**를 적는다.
수동 QA 시나리오는 `VERIFICATION_TEST_PLAN.md` 소관이고, 여기서는 `./gradlew test` 로 도는 자동 테스트만 다룬다.

측정 기준일: 2026-09-01 / 기준 커밋: `develop` (5a844b0)

---

## 1. 현재 상태

### 1.1 숫자

전체 프로덕션 코드 **44,102 LOC** 대비 테스트 코드 **4,998 LOC** (약 11%).
테스트 클래스 **52개**(그중 1개는 템플릿 잔존물), 페이크·픽스처 파일 6개, `androidTest` 2개.

| 모듈 | main LOC | test LOC | 테스트 클래스 | 상태 |
|---|---:|---:|---:|---|
| `verification:data` | 4,715 | 1,165 | 10 | 양호 |
| `verification:domain` | 1,715 | 814 | 8 | 양호 |
| `onboarding:domain` | 957 | 861 | 8 | 양호 |
| `core:datastore` | 184 | 239 | 1 | 양호 |
| `challenge:domain` | 2,387 | 666 | 6 | 보통 |
| `observability:data` | 1,019 | 250 | 3 | 보통 |
| `observability:domain` | 782 | 192 | 1 (+픽스처 3) | 보통 |
| `core:domain` | 681 | 163 | 3 | 보통 |
| `verification:presentation` | 1,834 | 103 | 2 | 얇음 |
| `onboarding:data` | 856 | 99 | 1 | 얇음 |
| `app` | 1,725 | 131 | 3 (1개는 템플릿) | 얇음 |
| `profile:data` | 939 | 66 | 1 | 얇음 |
| `challenge:presentation` | 13,429 | 204 | 4 | **거의 없음** |
| `profile:presentation` | 3,967 | 45 | 1 | **거의 없음** |
| `challenge:data` | 2,573 | 0 | 0 | **없음** |
| `onboarding:presentation` | 3,443 | 0 | 0 | **없음** |
| `core:network` | 407 | 0 | 0 | **없음** |
| `home:presentation` | 747 | 0 | 0 | **없음** |
| `profile:domain` | 483 | 0 | 0 | **없음** |
| `core:designsystem` | 990 | 0 | 0 | 없음(의도적, §5 참조) |
| `core:ui` | 141 | 0 | 0 | **없음** |
| `observability:debug` | 128 | 0 | 0 | 없음(debug 전용, 낮은 우선순위) |

### 1.2 지금 잘 되고 있는 것 — 이 관행을 기준선으로 삼는다

- **verification / onboarding:domain** 은 값 객체·불변식·UseCase 조립을 페이크로 촘촘히 덮는다. `AnchorSetTest`, `SignalBatchSplitTest`, `SyncGateTest`, `AutoLoginUseCaseTest` 가 본보기다.
- **테스트 이름이 한국어 문장이고 "무엇을 보장하는지"를 말한다.** (`지오펜스 재조정은 …`, `하한을 넘기면 카운터가 다음 행동을 알린다`) 새 테스트도 이 형식을 따른다.
- **페이크는 손으로 만든다.** `challenge/domain/.../fake/FakeChallengeRepository.kt`, `onboarding/domain/.../fake/Fakes.kt`, `observability:domain` 의 `testFixtures`(`FakeClock`, `RecordingSink`, `TestObservability`). 목 라이브러리를 쓰지 않는 현재 방침은 유지한다.
- **아키텍처 규칙이 테스트로 강제된다.** `ArchitectureTest`(Konsist), `AppRouteAccessPolicyTest`.
- **presentation 도 "순수 함수로 뽑아 검증"하는 패턴이 이미 있다.** `AppealSheetTest`, `TodayVerificationCopyTest`, `MyAppealsScreenTest` 는 Compose 를 띄우지 않고 카피 생성 함수만 검증한다 — 도구 없이 지금 당장 쓸 수 있는 유일한 UI 검증 수단이다.

---

## 2. 테스트 층과 배치 규칙

기존 코드에서 귀납한 규칙이다. 새 테스트는 여기에 맞춘다.

| 층 | 대상 | 위치 | 도구 | 예시 |
|---|---|---|---|---|
| **케이스** | 엔티티·값 객체 불변식, enum `fromValue`, 순수 계산 | `<feature>/domain/src/test` | `kotlin("test-junit")` | `RoutineDescriptionTest`, `AnchorSetTest` |
| **매퍼** | DTO ↔ entity, Room 컨버터, 직렬화 왕복 | `<feature>/data/src/test` | + `kotlinx-serialization-json` | `SignalEntityMapperTest`, `CalendarResponseTest` |
| **모듈** | UseCase, Repository 구현 (페이크 협력자) | domain / data `src/test` | + `kotlinx-coroutines-test` | `RunSyncUseCaseTest`, `VerificationRepositoryImplTest` |
| **화면 로직** | ViewModel 의 intent→state/effect 전이 | `<feature>/presentation/src/test` | + `coroutines-test` (**미도입**) | 없음 |
| **화면 표현** | 카피·표시 규칙 순수 함수 | `<feature>/presentation/src/test` | `kotlin("test-junit")` | `AppealSheetTest` |
| **구조** | 레이어 의존, 라우트 접근 정책 | `app/src/test` | Konsist | `ArchitectureTest` |
| **UI 렌더** | Compose 트리 | — | **없음** | — |

**층 선택 원칙**: 협력자가 없으면 케이스 층, 페이크로 대체 가능하면 모듈 층, 렌더가 꼭 필요할 때만 UI 층.
카피·포맷 로직은 Composable 밖으로 뽑아 케이스 층으로 내린다 — 지금 `AppealSheetTest` 가 하는 방식이고, UI 도구 없이도 검증 가능한 코드의 비율을 늘리는 게 이 레포의 현실적인 전략이다.

---

## 3. 도구 현황 — 구조적 결함

**이게 커버리지 공백의 근인이다. 3장을 해결하지 않으면 4장의 P0 대부분은 착수 자체가 불가능하다.**

1. **`core:network`, `challenge:data`, `core:ui`, `core:designsystem`, `onboarding:presentation`, `home:presentation` 은 `build.gradle.kts` 에 `testImplementation` 블록이 아예 없다.** 테스트 파일을 놓아도 컴파일되지 않는다. 이 6개 모듈이 프로덕션 코드 **9,301 LOC** 를 차지한다.
2. **`kotlinx-coroutines-test` 가 3개 모듈에만 있다** (`core:datastore`, `verification:data`, `verification:domain`). ViewModel·suspend Repository 를 검증하려면 각 모듈에 추가해야 한다.
3. **Compose UI 테스트 수단이 없다.** Robolectric 미도입, `compose-ui-test-junit4` 는 `app`·`observability:data` 의 `androidTestImplementation` 에만 있고 버전 카탈로그에 Robolectric 항목 자체가 없다.
4. **`androidTest` 를 도는 CI 잡이 없다.** `test.yml` 은 `./gradlew test` 만 돌린다 → `app/src/androidTest/.../NavRouteUriParserTest.kt` 는 **한 번도 CI 에서 실행된 적이 없다**. 딥링크 파서는 앱의 유일한 외부 진입점(App Links `https://android.ruleup.co.kr/inv/...`)이라 이 공백은 위험도가 높다.
5. **커버리지 측정 도구(JaCoCo/Kover)가 없다.** "어디가 비었는지"를 사람이 파일 목록으로 세야 한다 — 이 문서가 필요했던 이유이기도 하다.
6. **템플릿 잔존물**: `app/src/test/.../ExampleUnitTest.kt`(`assertEquals(4, 2+2)`), `app/src/androidTest/.../ExampleInstrumentedTest.kt`. 신호가 0인 테스트가 초록불에 섞여 있다.

---

## 4. 비어 있는 곳 — 우선순위

우선순위 기준: **틀렸을 때의 피해 × 지금 검증되지 않을 확률**.
"조용히 로그아웃", "조용히 빈 화면", "조용히 데이터 유실" 세 가지가 P0 로 간다.

### P0 — 세션·데이터가 걸린 미검증 로직

#### P0-1. `core:network` 전체 (407 LOC, 테스트 0, 테스트 의존성도 0)

**`TokenAuthenticator`** 는 이 레포에서 가장 분기가 많고 가장 조용히 실패하는 단위다. 코드 주석이 각 분기마다 "잘못하면 멀쩡한 사용자가 로그아웃된다"고 적어 놓았는데, 그 주석을 지키는 테스트가 하나도 없다.

검증해야 할 경로:
- `` `refresh 요청의 401 은 다시 갱신하러 들어가지 않는다` `` — 무한 재귀 차단 (`AUTH_REFRESH_PATH` 분기)
- `` `재시도가 2회에 이르면 포기한다` `` — `responseCount >= MAX_ATTEMPTS`
- `` `다른 스레드가 이미 갱신했으면 캐시 토큰으로 바로 재시도한다` `` — `current != failedToken` 분기
- `` `갱신이 네트워크 오류로 실패하면 토큰을 지우지 않는다` `` — 5xx·타임아웃에 로그아웃시키지 않는다는 계약
- `` `refreshToken 이 그 사이 회전했으면 세션을 정리하지 않는다` `` — `latestRefresh != refreshToken` 분기
- `` `갱신이 null 이고 회전도 없었으면 로컬 토큰을 정리한다` `` — 유일하게 로그아웃이 허용되는 경로
- `` `갱신에 성공하면 새 액세스 토큰으로 재시도한다` ``

**`NetworkModule` 의 auth 인터셉터** — `NO_AUTH_PATHS`(`/auth/oauth`, `/auth/signup`, `/auth/refresh`)에 만료 토큰이 실리면 로그인 자체가 401 로 막힌다. CLAUDE.md 가 명시적으로 경고하는 함정인데 회귀 방지 테스트가 없다.
- `` `비인증 경로에는 Authorization 헤더를 붙이지 않는다` ``
- `` `로그아웃은 비인증 경로가 아니다` `` (주석이 못박은 예외)
- `` `BASE_URL 에 trailing slash 가 없으면 보정한다` `` / `` `빈 BASE_URL 은 보정하지 않고 그대로 던진다` ``

**`BaseResponse` / `ApiException`** — `getOrThrow()`·`throwOnError()` 는 앱 전체에서 **67곳**이 쓰는 단일 에러 깔때기다. 여기가 틀리면 모든 화면의 에러 처리가 같이 틀린다.
- `` `success 여도 data 가 없으면 예외로 만든다` ``
- `` `error 가 통째로 비어도 UNKNOWN 으로 떨어뜨린다` ``
- `` `reason·retryAfterSeconds·rejoinAvailableAt 이 예외까지 살아 간다` `` — 이게 죽으면 P0-3 의 가입 차단 분기가 통째로 무너진다
- `` `requireField 는 누락된 필드 이름을 예외에 남긴다` ``

> 선행 작업: `core/network/build.gradle.kts` 에 `testImplementation` 추가 (`kotlin("test-junit")`, `kotlinx-coroutines-test`, OkHttp `mockwebserver`).

#### P0-2. 딥링크 파서가 CI 에서 안 돈다

`app/src/androidTest/java/com/ruleup/android_ruleup/deeplink/NavRouteUriParserTest.kt`.
테스트는 이미 쓰여 있는데 `./gradlew test` 가 건드리지 않는다. `NavRouteUriParser` 가 `android.net.Uri` 에 묶여 있으면 Robolectric 도입 전까지는 (a) 파싱 코어를 순수 함수로 분리해 `src/test` 로 내리거나 (b) `androidTest` 잡을 CI 에 추가해야 한다. **(a) 를 권장한다** — 에뮬레이터 잡은 CI 시간을 크게 늘린다.

#### P0-3. `challenge:data` — 2,573 LOC, 테스트 0

앱에서 가장 큰 미검증 데이터 레이어다. DTO 매핑 함수 53개, `requireField` 25회, 그리고 예외 번역:

```
ChallengeRepositoryImpl:177  throw JoinBlockedException(
                               reason = JoinBlockReason.fromValue(e.reason),
                               rejoinAvailableAt = e.rejoinAvailableAt, ...)
```

`JoinBlockReasonTest` 는 **domain 쪽 enum 만** 검증한다. 서버의 `409 JOIN_BLOCKED` + `reason` 이 여기까지 도달해 올바른 분기로 번역되는지는 아무도 보지 않는다. CLAUDE.md 가 "쿨다운 예외를 구분할 수 없게 만들었다"는 과거 사고를 기록한 바로 그 지점이다.

- `` `JOIN_BLOCKED 는 reason 과 재시도 시각을 실은 예외로 번역된다` ``
- `` `모르는 reason 은 예외를 삼키지 않는다` ``
- `` `필수 필드가 빠진 챌린지 응답은 어떤 필드인지 말하고 실패한다` `` (`ChallengeResponse` 의 `requireField` 17회)
- `ExploreResponse` / `RankingResponse` / `RoomResponse` / `WatcherResponse` / `ThreadResponse` 각각 매핑 왕복 1건씩 — 최소한 "서버가 준 nullable 이 화면 타입에서 어떻게 되는가"를 고정한다.

> 선행 작업: `challenge/data/build.gradle.kts` 에 `testImplementation` 추가 (`kotlin("test-junit")`, `kotlinx-serialization-json`, `kotlinx-coroutines-test`).

#### P0-4. ViewModel 21개 전부 미검증 (4,345 LOC)

MVI 규약상 상태 전이가 `dispatch → reduce` 한 곳으로 모여 있어 **테스트하기 가장 쉬운 코드인데 한 줄도 없다.**

가장 먼저 덮을 3개:

1. **`SplashViewModel`** (112 LOC) — 앱의 모든 콜드스타트가 지나는 길. 재진입 가드(`started`), 게이트 순서(강제 업데이트가 자동 로그인보다 먼저), 딥링크 3분기(`Open`/`Dropped`/`None`).
   - `` `강제 업데이트가 걸리면 자동 로그인을 시도하지 않는다` ``
   - `` `진입 인텐트가 두 번 와도 절차는 한 번만 돈다` `` (액티비티 재생성 시 인트로 조회·토큰 재발급 중복 발사 방지)
   - `` `보류 딥링크가 있으면 백스택을 교체한다` ``
   - `` `인증 전이라 딥링크가 버려지면 로그인으로 보낸다` ``
2. **`ChallengeDetailViewModel`** (1,005 LOC, 단일 파일 최대) — 참여·인증·이의 흐름이 전부 여기 있다. 통째로는 무리니 참여 차단 분기(`JoinBlockedException` 수신 → 상태)부터 시작한다.
3. **`CreateChallengeViewModel`** (602 LOC) — `CreateChallengeCommand` 의 불변식은 domain 에서 검증되지만, **화면이 그 불변식을 어기는 입력을 만들지 않는지**는 미검증. CLAUDE.md 의 "ViewModel 에서 clamp 하지 않는다" 규칙이 지켜지는지 확인할 자리이기도 하다.

나머지 18개는 P1 로 내린다. 다만 `onboarding:presentation`(3,443 LOC)과 `home:presentation`(747 LOC)은 **testImplementation 블록이 없어** 착수 전 §3-1 처리가 필요하다.

### P1 — 조용히 틀리는 매핑·계약

#### P1-1. `profile:domain` (483 LOC, 테스트 0)

`fromValue` 가 4개 있고 전부 서버 enum 확장에 취약하다:
- `MilestoneType.fromValue` 는 모르는 값을 **`ETC` 로 흡수** — 서버가 새 마일스톤을 추가하면 조용히 "기타"로 표시된다.
- `NicknameCheckReason` / `CalendarDayStatus` / `DayItemStatus` / `ImageModerationStatus` 는 **null 반환** — 화면에서 어떻게 처리되는지 고정되어 있지 않다.
- `` `모르는 마일스톤은 기타로 흡수한다` `` / `` `모르는 캘린더 상태는 칠하지 않는다` `` 처럼 **의도를 테스트로 못박는다**(후자는 `CalendarResponseTest` 에 이미 있는 표현 — domain 쪽에도 대응 테스트가 필요하다).

#### P1-2. `profile:data` 매핑 (939 LOC, 테스트 1)

매핑 함수 16개 중 `CalendarResponse` 만 검증. `MyHomeResponse`, `StatsResponse`, `ReputationResponse`, `MyProfileResponse`, `MyChallengesSliceResponse`, `FriendInvitationResponse` 는 전부 공백.

#### P1-3. `onboarding:data` (856 LOC, 테스트 1)

`TokenRefresherImpl` 만 검증. `AuthRepositoryImpl`(소셜 로그인·가입 요청 구성), `IntroRepositoryImpl`(버전 게이트 응답 해석), `DeviceIdentityRepositoryImpl`(`deviceId` = ANDROID_ID) 미검증.

#### P1-4. 네비게이션 인자 왕복

`Page` 구현체 13개가 각자 `NavRoute(path, args: Map<String,String>)` 로 직렬화하는데 **왕복 테스트가 하나도 없다.** `AppRouteAccessPolicyTest` 는 라우트 접근 정책만 보고 인자는 보지 않는다.
- `` `Page 는 자기 path 를 AppRoutes 상수에서 가져온다` `` (Konsist 나 리플렉션으로 일괄 검증 가능)
- `` `직렬화한 인자는 렌더 쪽에서 같은 키로 복원된다` `` — 최소한 인자를 가진 Page(`ChallengeDetailPage` 등)에 대해
- `NavRouteJson` 의 `ignoreUnknownKeys` 계약: `` `스키마가 바뀐 백스택 인스턴스도 살아남는다` ``

#### P1-5. `core:ui` `MviViewModel` (141 LOC, 테스트 0)

모든 화면의 토대인데 검증이 없다.
- `` `상태 변이는 reduce 를 거친 결과만 반영된다` ``
- `` `이펙트는 구독 전에 보내도 유실되지 않는다` `` (`Channel.BUFFERED` 계약 — 화면 회전 중 토스트·네비게이션이 사라지는 버그의 근원지)

### P2 — 여력이 될 때

- **`observability:data` 싱크 매퍼**: `AmplitudeEventMapper`, `FirebaseEventMapper`, `ChannelFilterSink`, `SeverityFilterSink` 미검증. `CompositeSink`·`RuntimePolicy`·`PolicyConfig` 는 이미 있다. 계측이 틀려도 사용자 기능은 안 죽지만 잘못된 데이터로 의사결정이 흐른다.
- **`home:presentation`** (747 LOC, 0): 홈은 진입 빈도가 최고지만 대부분 조합 코드다. `HomeViewModel`(87 LOC)만 덮으면 충분하다.
- **`app` 헬퍼 구현체**: `NavigationHelperImpl`, `MessageHelperImpl`, `PushNotificationHelperImpl`, `NavRouteUriParser`(→ P0-2), `PushTokenRegister`. Android 의존이 붙어 있어 Robolectric 도입과 함께 다룬다.
- **`core:designsystem`** (990 LOC, 0): **의도적으로 비워 둔다.** 스냅샷 테스트 도구 없이 Compose 컴포넌트를 검증하면 비용 대비 신호가 낮다. Paparazzi/Roborazzi 도입을 별도 안건으로 다루기 전까지 착수하지 않는다.
- **`observability:debug`** (128 LOC): `debugImplementation` 이라 릴리스에 없다. 우선순위 최하.
- **템플릿 제거**: `ExampleUnitTest`, `ExampleInstrumentedTest`.

---

## 5. 실행 계획

**1단계 — 바닥 깔기 (테스트를 쓸 수 있게 만든다)**
1. `core:network`, `challenge:data`, `core:ui`, `onboarding:presentation`, `home:presentation` 에 `testImplementation` 블록 추가.
2. `challenge:presentation`, `profile:presentation`, `verification:presentation`, `onboarding:presentation` 에 `kotlinx-coroutines-test` 추가.
3. `ExampleUnitTest` / `ExampleInstrumentedTest` 삭제.

**2단계 — P0 (세션·데이터)**
4. `TokenAuthenticator` + auth 인터셉터 + `BaseResponse`.
5. `NavRouteUriParser` 파싱 코어를 순수 함수로 분리해 `src/test` 로 이관.
6. `challenge:data` DTO 매핑 + `JoinBlockedException` 번역.
7. `SplashViewModel`, `CreateChallengeViewModel`, `ChallengeDetailViewModel`(참여 분기).

**3단계 — P1**
8. `profile:domain` `fromValue` 4종 + `profile:data` 매핑.
9. `onboarding:data` 리포지토리 3종.
10. `Page` ↔ `NavRoute` 왕복 + `MviViewModel`.
11. 남은 ViewModel 18개 — 화면 작업이 들어올 때 그 화면부터.

**4단계 — 도구 결정 (별도 안건)**
12. JaCoCo 또는 Kover 도입 → 이 문서의 표를 자동 생성으로 대체.
13. Robolectric 도입 여부 결정 → 결정 전까지 Compose 로직은 순수 함수로 뽑아 케이스 층에서 검증한다.
14. `androidTest` CI 잡 필요성 재검토 (2단계-5 로 대부분 해소되면 불필요).

---

## 6. 앞으로의 규칙

- **새 entity·값 객체에 `require`/`init` 불변식을 넣으면 같은 PR 에 케이스 테스트를 붙인다.** 불변식은 그것을 어기는 테스트가 있어야 살아 있다.
- **새 DTO 매핑에는 왕복 테스트 1건을 붙인다.** 특히 `requireField` 와 `fromValue` 를 쓴 자리 — nullable 처리 의도가 코드만으로는 드러나지 않는다.
- **새 ViewModel 에는 최소한 "성공 경로 1건 + 실패 경로 1건 + 재진입/중복 인텐트 1건"** 을 붙인다.
- **버그를 고칠 때는 그 버그를 재현하는 테스트를 먼저 쓴다.** 지금 테스트가 잘 깔린 `verification` 모듈이 그렇게 자랐다.
- **테스트 이름은 한국어 문장으로, "무엇을 보장하는지"를 적는다.** 함수명 반복(`fun createChallenge_success`)은 쓰지 않는다.
- **목 라이브러리를 도입하지 않는다.** 페이크를 손으로 만들고, 여러 모듈이 공유하면 `testFixtures` 로 올린다(`observability:domain` 선례).
- **이 문서는 §1 표를 기준으로 분기마다 갱신한다.** 커버리지 도구가 도입되면 표는 자동 산출로 대체하고, §4 우선순위만 사람이 유지한다.

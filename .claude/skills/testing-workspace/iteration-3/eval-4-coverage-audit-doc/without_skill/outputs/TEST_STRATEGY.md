# TEST_STRATEGY.md

이 저장소의 테스트가 **지금 어디를 지키고 있고, 어디가 비어 있는지**를 기록한다.
목표는 커버리지 숫자가 아니라 **"깨지면 사용자가 다치는 곳부터 테스트가 있다"** 는 상태다.

- 대상 커밋 기준: `develop` (감사 시점 2026-08-31)
- 확인 방법: `src/main` 과 `src/test`·`src/androidTest` 를 모듈별로 대조하고, 각 모듈 `build.gradle.kts` 의 테스트 의존과 CI 워크플로를 함께 읽었다. Gradle 은 실행하지 않았다 — 아래 수치는 **파일 수와 소스 대조**이며 실행 커버리지(JaCoCo 등)가 아니다.
- 이 문서에 **미결(❓)** 로 표시한 항목은 도입 여부를 사람이 정해야 하는 것이다. 임의로 진행하지 않는다.

---

## 1. 지금 상태

### 1.1 CI 가 실제로 돌리는 것

`.github/workflows/` 는 `main`/`develop` push·PR 마다 세 워크플로를 돌린다.

| 워크플로 | 명령 | 테스트를 도는가 |
|---|---|---|
| `build.yml` | `./gradlew assembleDebug` | 아니오 |
| `lint.yml` | `./gradlew ktlintCheck` · `./gradlew lint` | 아니오 |
| `test.yml` | `./gradlew test` | **예 — 유닛 테스트만** |

> **`connectedAndroidTest` 를 도는 워크플로가 없다.** `src/androidTest` 에 있는 테스트는 **누구도 실행하지 않는다.** 지금 거기에 7개짜리 딥링크 파서 테스트가 들어 있다(§3.1-A).

### 1.2 모듈별 소스 대 테스트

`*.kt` 파일 수. 테스트 파일 수에는 fake/fixture 파일도 포함돼 있어 실제 테스트 클래스 수는 이보다 적다.

| 모듈 | main | test | 상태 |
|---|---:|---:|---|
| `verification/data` | 61 | 10 | 🟢 기준점 |
| `verification/domain` | 32 | 8 | 🟢 기준점 |
| `onboarding/domain` | 26 | 10 | 🟢 |
| `challenge/domain` | 29 | 7 | 🟡 엔티티 일부만 |
| `core/domain` | 20 | 3 | 🟡 |
| `observability/data` | 20 | 3 | 🟡 |
| `verification/presentation` | 10 | 2 | 🟡 헬퍼만 |
| `app` | 25 | 5 | 🟡 2개는 템플릿 스텁, 1개는 CI 밖 |
| `core/datastore` | 2 | 1 | 🟡 13케이스로 촘촘하나 캐시 경로가 빔 |
| `profile/presentation` | 27 | 1 | 🔴 |
| `profile/data` | 15 | 1 | 🔴 |
| `onboarding/data` | 13 | 1 | 🔴 |
| `challenge/presentation` | 49 | 4 | 🔴 헬퍼만 |
| `observability/domain` | 25 | 1 | 🟡 (대부분 데이터 홀더) |
| **`challenge/data`** | **18** | **0** | 🔴 **테스트 소스셋 자체가 없음** |
| **`onboarding/presentation`** | **34** | **0** | 🔴 **테스트 의존조차 없음** |
| **`profile/domain`** | **12** | **0** | 🔴 |
| **`core/network`** | **7** | **0** | 🔴 **최고 위험** |
| **`core/ui`** | **6** | **0** | 🔴 |
| **`home/presentation`** | **4** | **0** | 🔴 |
| `core/designsystem` | 14 | 0 | ⚪ 의도적 비범위(§6) |
| `observability/debug` | 3 | 0 | ⚪ 의도적 비범위(§6) |

### 1.3 잘 되고 있는 것 — 이걸 기준으로 나머지를 끌어올린다

빈 곳을 채울 때 **새 양식을 만들지 말고 아래를 그대로 따른다.**

- **`verification` 모듈이 이 저장소의 기준점이다.** `RunSyncUseCaseTest` 는 413 분할 전송·gap 은 첫 조각에만·더 못 쪼개면 폐기까지 정책 문장 단위로 잡아 두었고, `VerificationRepositoryImplTest` 는 HTTP 에러 코드마다 어떤 도메인 예외로 갈라지는지를 15개 케이스로 고정한다.
- **테스트 이름이 한국어 문장이고, 주석이 "왜 이게 깨지면 안 되는지"를 적는다.** 예: `` `모르는 사유는 null 이다` ``, `` `자동 방 수동 제출은 도메인 어휘로 올리지 않는다` ``. 이 컨벤션을 유지한다.
- **모킹 라이브러리가 없다. 손으로 쓴 fake 를 쓴다.** `challenge/domain/src/test/.../fake/FakeChallengeRepository.kt`, `onboarding/domain/src/test/.../fake/Fakes.kt` 가 그 예다. mockk/mockito 를 새로 들이지 않는다.
- **관측 이벤트 카탈로그는 페이로드 단위로 고정돼 있다.** `ChallengeEventsTest`(12케이스)·`OnboardingEventsTest`(6케이스)가 이벤트 이름 전수와 "성공에는 `error_code` 키를 아예 넣지 않는다" 같은 스키마 계약까지 잡는다.
- **아키텍처 규칙이 문서가 아니라 테스트다.** `app/src/test/.../ArchitectureTest.kt`(Konsist 6규칙) · `AppRouteAccessPolicyTest`(로그인 기본값 강제).
- **`observability:domain` 만이 `java-test-fixtures` 로 fake 를 공유한다**(`FakeClock`·`RecordingSink`·`TestObservability`). 세 모듈이 이걸 재사용 중이다 — 확장할 모델이 이미 있다는 뜻이다.

---

## 2. 테스트 층

이 저장소에서 실제로 성립하는 층은 넷이다. **가장 싼 층에서 잡을 수 있는 것은 위층으로 올리지 않는다.**

| 층 | 어디에 | 무엇을 잡나 | 협력자 |
|---|---|---|---|
| **케이스** | 해당 모듈 `src/test` | 엔티티 불변식, 값 객체, enum 매핑, 순수 함수, DTO↔entity 매퍼, 문구 조립 | 없음 |
| **모듈** | 해당 모듈 `src/test` | UseCase 조립, Repository 구현의 에러 변환, ViewModel 의 `reduce`/`onIntent` | 손으로 쓴 fake |
| **호스트 규칙** | `app/src/test` | 아키텍처 규칙(Konsist), 라우트 등록·접근 정책 | 없음 |
| **계측** | `src/androidTest` | Android 프레임워크 타입이 실제로 필요한 것 | 기기/에뮬레이터 |

- **케이스 층이 이 저장소의 주력이다.** 지금 있는 테스트의 대부분이 여기 있고, 맞는 선택이다.
- **계측 층은 CI 가 안 돈다(§1.1).** 여기에 무언가를 두는 것은 "테스트를 안 쓴 것"과 거의 같다. 새 테스트를 `androidTest` 에 두지 않는다.
- **UI(Compose) 층은 현재 존재하지 않는다.** 도입 여부는 §6-❓ 참조.

---

## 3. 비어 있는 곳

우선순위는 **(터졌을 때의 피해) × (지금 아무것도 막지 못함)** 으로 매겼다.

### 3.1 P0 — 지금 당장 위험하거나, 테스트를 쓰는 것 자체가 막혀 있는 곳

#### A. `core/network` — 테스트가 0이고, 여기가 터지면 전원 로그아웃된다 🔴

`core/network/build.gradle.kts` 에 **테스트 의존이 한 줄도 없고 `src/test` 디렉터리도 없다.**

`core/network/src/main/kotlin/com/ruleup/network/auth/TokenAuthenticator.kt` 는 401 자동 갱신을 맡는다. 분기가 7개인데 **하나도 검증되지 않는다.** 실패 모드가 전부 사용자 눈에 보이는 사고다:

| 분기 | 코드 근거 | 깨지면 |
|---|---|---|
| `/auth/refresh` 자신의 401 은 갱신하지 않는다 | `requestPath.contains(AUTH_REFRESH_PATH)` → `null` | 무한 재귀 |
| 재시도 2회에서 멈춘다 | `responseCount(response) >= MAX_ATTEMPTS` | 무한 재시도 |
| 캐시 토큰이 실패 토큰과 다르면 갱신 없이 재시도 | `current != failedToken` → `retryWith(current)` | 동시 401 때 중복 갱신 |
| refreshToken 이 없으면 조용히 포기 | `?: return null` | — |
| **갱신이 예외로 실패하면 토큰을 지우지 않는다** | `catch (e: Exception)` → `w(...)` → `null` | **네트워크 끊김에 멀쩡한 사용자가 로그아웃** |
| **refreshToken 이 그새 회전했으면 정리하지 않고 최신 토큰으로 재시도** | `latestRefresh != refreshToken` → `retryWith(latestAccess)` | 콜드스타트 AutoLogin 과 겹칠 때 세션 유실 |
| 진짜 만료면 `clear()` 후 포기 | `tokenRepository.clear()` | 만료돼도 로그인 화면으로 못 감 |

> **⚠️ 감사 중 발견한 의심 지점 — 테스트가 없어서 확인되지 않는다.**
> `failedToken` 은 `response.request.header("Authorization")?.removePrefix("Bearer ")` 다. `NetworkModule` 의 `NO_AUTH_PATHS` 가 `/auth/oauth`·`/auth/signup` 요청에서 헤더를 아예 빼므로, 그 요청이 401 을 받으면 **`failedToken == null`** 이 된다. 캐시 토큰이 있으면 `current != failedToken` 이 참이 되어 **`retryWith(current)` — 즉 원래 인증 헤더를 빼기로 한 요청에 Bearer 를 붙여 재시도한다.** `/auth/refresh` 는 33행에서 먼저 걸러지지만 나머지 두 경로는 걸러지지 않는다.
> 이게 의도인지 버그인지 **코드만으로는 판단할 수 없다.** 테스트를 쓰기 전에 확인이 필요하다(❓). CLAUDE.md 가 "만료 토큰이 로그인 요청에 실리면 401 로 막힌다"고 경고한 바로 그 상황과 모양이 같다.

`core/network/.../dto/BaseResponse.kt` 도 전부 미검증이다. 순수 JVM 이라 **오늘 당장 100% 잡을 수 있는데 0% 다.**

- **`getOrThrow()` 는 `success == true` 인데 `data == null` 이면 `code = "UNKNOWN"` 예외를 던진다.** 반면 **`throwOnError()` 는 `success` 만 보고 `data` 는 안 본다** — 같은 응답이 한쪽에서는 통과하고 한쪽에서는 던진다. 이 비대칭이 두 함수가 따로 있는 이유 전부인데, 문서도 테스트도 없다.
- **`ErrorBody.code`·`message` 는 non-null 이고 기본값이 없다.** 따라서 `"UNKNOWN"` 폴백은 `error` 가 통째로 없을 때만 닿는다 — 에러 본문이 **깨져 있으면** `ApiException` 이 아니라 `MissingFieldException` 이 난다. 비자명하고 미검증이다.
- `ErrorBody?.toException()` 이 `retryAfterSeconds`(429 백오프)·`reason`(409 `JOIN_BLOCKED` 하위 분기 키)·`rejoinAvailableAt` 을 예외 너머로 실어 나른다. 호출부가 `reason` 으로 분기하므로 "reason 이 던져진 뒤에도 살아 있다"는 실제 계약이다.
- `requireField` 의 `code = "RESPONSE_FIELD_MISSING"` 문자열은 다른 곳에서 매칭될 수 있는 값이다.

**`TokenAuthenticator` 와 `BaseResponse` 는 막는 것이 없다.** 의존이 전부 인터페이스(`TokenRepository`·`TokenRefresher`·`Observability`)라 fake 로 세울 수 있고, OkHttp `Request`/`Response`(및 `priorResponse` 체인)는 순수 JVM 에서 빌드된다. 필요한 것은 `testImplementation(kotlin("test-junit"))` + `libs.okhttp` + `libs.kotlinx.coroutines.test` + `testFixtures(project(":observability:domain"))` 뿐이다.

**반면 `NetworkModule` 은 테스트가 아니라 리팩터가 선행돼야 한다.** 인증 인터셉터가 `provideOkHttpClient` 안에서 만들어지는 **익명 람다**이고 `NO_AUTH_PATHS` 는 `private` 이라, 밖에서 부를 손잡이가 없다. 여기서 미검증인 것:
- `skipAuth` 판정이 `contains` 다 — `startsWith`/경로 일치가 아니다. `/auth/oauth-callback` 같은 경로도 함께 걸린다. **로그아웃은 액세스 토큰이 필요해 의도적으로 목록에서 빠져 있다** — 그 의도를 지키는 것이 없다.
- `cachedAccessToken() ?: runBlocking { getAccessToken() }` 의 콜드스타트 1회 블로킹.
- 디버그 로깅 분기의 `redactHeader("Authorization")`·`redactHeader("Cookie")` — **토큰이 로그로 새는지를 가르는 보안 분기**인데 아무 assert 가 없다.

→ 인터셉터를 `TokenAuthenticator` 옆에 이름 있는 클래스로 추출하면 위가 전부 케이스 층으로 내려온다(❓ 리팩터 승인 필요).
**`provideRetrofit` 의 baseUrl 정규화**만은 지금도 순수하게 잡힌다: 끝에 `/` 없으면 붙이고, 있으면 그대로, **비어 있으면 일부러 정규화하지 않아 `Retrofit.Builder` 가 던지게 둔다**(`local.properties` 의 BASE_URL 누락을 조용히 넘기지 않으려는 fail-fast). 이 fail-fast 를 지키는 것이 없다.

#### A-2. `core/datastore` — 촘촘하지만 인터셉터가 기대는 캐시 경로가 비었다 🟡

`TokenRepositoryImplTest` 는 13케이스로 저장·복원, `clear()` 후 로그인 이력 보존, `saveSession` 의 쓰기 1회 원자성, IO 예외 시 빈 값 환원과 관측 채널 기록, **비 IOException 은 삼키지 않음**까지 잘 잡는다. 이 모듈은 §1.3 의 기준점에 가깝다. 다만:

- **`userId: Flow<String?>` 를 아무 테스트도 건드리지 않는다.** suspend `getUserId()` 만 본다. `TokenRepository.userId` 의 KDoc 은 소비자가 `isLoggedIn` 이 아니라 **이 Flow 를 구독해야 한다**고 못 박는데, 그 Flow 가 로그인 후 방출하는지 검증이 없다.
- **`getAccessToken()` 의 `.also { cachedAccess = it }` 캐시 워밍이 미검증이다.** 이게 정확히 `NetworkModule` 인터셉터가 기대는 콜드스타트 경로다(§3.1-A). 워밍 전 `cachedAccessToken()` 이 null 이라는 것도 미검증.
- 쓰기 경로의 **비 IOException 전파** — `write()` 는 `IOException` 만 잡으므로 `RuntimeException` 은 호출부로 나간다. 읽기 쪽 대칭 케이스는 잡혀 있는데 쓰기 쪽만 없다. `FakeDataStore.failWritesWith` 가 `Throwable` 을 받으므로 **한 줄이면 된다.**
- `isLoggedIn` 은 refresh 키만 본다 — "access 는 있는데 refresh 가 없는" 비대칭 상태가 미검증.

#### B. ViewModel 21개 전부 미검증 — 그리고 지금은 쓸 수도 없다 🔴

| 모듈 | ViewModel | 총 라인 | 테스트 |
|---|---:|---:|---|
| `challenge/presentation` | 7 | 2,338 | 0 |
| `profile/presentation` | 8 | 757 | 0 |
| `onboarding/presentation` | 3 | 580 | 0 |
| `verification/presentation` | 2 | 383 | 0 |
| `home/presentation` | 1 | 87 | 0 |

가장 큰 `ChallengeDetailViewModel`(1,005줄)이 참여·복제·스레드 커서 페이징·랭킹 스코프 캐싱·이의 제출·역할 변경·위임을 전부 들고 있는데 검증이 없다.

**`core/ui/.../MviViewmodel.kt` 의 실제 모양이 무엇이 가능한지를 정한다.** 이 파일 자체도 테스트가 없다.

| 멤버 | 가시성 | 구현 |
|---|---|---|
| `uiState: StateFlow<S>` | public | `MutableStateFlow(initialState).asStateFlow()` — `.value` 를 동기로 읽을 수 있다 |
| `effect: Flow<F>` | public | **`Channel<F>(Channel.BUFFERED).receiveAsFlow()`** — SharedFlow 가 아니다 |
| `onIntent(intent: I)` | public abstract | **테스트가 부를 수 있는 유일한 진입점** |
| `currentState` / `reduce` / `dispatch` / `emitEffect` | **전부 protected** | 테스트에서 직접 못 부른다 |

여기서 나오는 결론:

1. **`reduce` 는 순수 함수지만 테스트가 직접 부를 수 없다.** `dispatch` 가 `_uiState.update { reduce(it, event) }` 하나뿐이라 전이 로직 자체는 코루틴도 fake 도 필요 없다. 그러나 `protected` 라 **서브클래스를 세우거나 가시성을 여는 결정이 선행돼야 한다**(❓). 결정만 나면 `ChallengeDetailReducerEvent` 27종의 전이를 `kotlin("test-junit")` 만으로 잡을 수 있다.
   - 부수 조건: `_uiState.update` 는 CAS 경합 시 `reduce` 를 **다시 호출한다.** `reduce` 가 순수해야 한다는 계약이 있는데 강제하는 것이 없다 — 로그를 찍거나 이펙트를 쏘는 `reduce` 는 동시 `dispatch` 에서 두 번 발화한다.
2. **`onIntent` 경로는 지금 구조적으로 막혀 있다.**
   - `onboarding/presentation`·`home/presentation` 은 `build.gradle.kts` 에 **`testImplementation` 이 한 줄도 없다.**
   - 나머지 세 presentation 모듈은 `testImplementation(kotlin("test-junit"))` 뿐이다. **`kotlinx-coroutines-test` 가 없다.**
   - **fake 를 재사용할 수 없다.** `FakeChallengeRepository` 는 `challenge/domain/src/test` 에 있고 `challenge/presentation` 에서 보이지 않는다. `observability:domain` 만 `java-test-fixtures` 를 켜 두었다.
3. **이펙트 하니스에는 함정이 있다. 테스트를 쓸 때 반드시 지킨다.**
   - `emitEffect` 는 `viewModelScope.launch` 로 **비동기**인데 `dispatch` 는 동기다. `onIntent` 직후 `effect` 를 확인하면 아무것도 없다. → **ViewModel 을 생성하기 *전에*** `Dispatchers.setMain(StandardTestDispatcher(...))` 를 걸고(생성 시점에 `viewModelScope` 가 잡힌다), act 와 assert 사이에 `advanceUntilIdle()` 을 넣는다.
   - 소스 순서가 `emitEffect(...)` → `dispatch(...)` 인 곳은 **실제로는 상태가 먼저, 이펙트가 나중**에 관측된다. 이 순서 보장을 아무도 안 잡고 있다.
   - `Channel.BUFFERED`(64) 라서 수집 전에 방출된 이펙트도 **버퍼에 남아 나중에 온다** — `SharedFlow(replay=0)` 와 다르다. 대신 `receiveAsFlow()` 는 **단일 소비자**라 수집기를 둘 띄우면 이벤트가 나뉜다. 떠도는 수집기를 남기지 않는다.
   - 채널을 닫는 `onCleared()` 오버라이드가 없다.

그래서 지금 presentation 모듈이 하고 있는 것은 **ViewModel 밖으로 순수 함수를 빼서 그것만 테스트하는 우회**다 — `AppealSheetTest`(`reasonCounter`·`privacyNotice`), `TodayVerificationCopyTest`, `TargetAppsCopyTest`, `MyAppealsScreenTest`(`rowTitle`·`appealDateLabel`), `PermissionRepairRowsTest`, `LocationChangeLockTest`. **이 우회 자체는 좋은 패턴이고 유지할 값어치가 있다.** 다만 그것으로 잡히지 않는 오케스트레이션이 통째로 남는다.

#### C. `challenge/data` — 18개 파일, 테스트 소스셋 없음 🔴

`build.gradle.kts` 에 `testImplementation` 이 없고 `src/test` 도 없다. **feature `data` 모듈 중 유일하다** (`profile`·`verification`·`onboarding`·`observability` 는 전부 있다).

여기 든 것 중 가장 위험한 것:

- **`ChallengeUpdate.toRequestBody(): JsonObject`** (`dto/ChallengeRequest.kt`) — 손으로 조립하는 JSON 이고 **이미지에 3-상태 의미**가 있다: `removeImage` → `JsonNull`, `imageUrl != null` → 값, 그 외 → **키 자체를 안 넣음**. 여기가 틀리면 사용자 대표 이미지가 조용히 지워지거나 수정이 유실된다. 이 모듈에서 단일 최우선 테스트 대상이다.
- **`ChallengeRepositoryImpl`** 의 에러 변환 — `createDraft`/`create`/`getChallenge`/`update`/`join`/`claimOwner` 가 `ApiException.code` 를 도메인 예외로 가른다. `update` 는 4-way, `join` 은 **에러 본문에서 두 번째 enum 을 더 파싱한다**(`JoinBlockReason.fromValue(e.reason)` + `rejoinAvailableAt`). `verification/data` 에는 정확히 같은 종류의 테스트가 이미 있다 — 여기만 없다.
- **`SetupNotifierImpl`** (`notification/`) — 이 모듈에서 가장 조밀한 판단 로직이고 매퍼가 아니다. `kindFor()` 가 권한 게이트 → `VerificationMethod` 분기(`GPS_*` → 앵커 등록, `SCREEN_TIME_*` → **미등록일 때만** 앱 등록, `HEALTH`/`WAKE`/`SLEEP`/`SELF_CHECK` → 알림 없음)로 갈린다. `androidPermission()` 은 토큰 문자열을 `uppercase()` 정규화하고 별칭(`LOCATION`/`GPS`/`GEOFENCE` → `ACCESS_FINE_LOCATION`)을 흡수하며, **매핑되지 않은 토큰은 "허가됨"으로 통과시킨다**(`return@all true`). 이건 `Context.checkSelfPermission` 을 타서 Robolectric 이 필요하다(❓).

#### D. 딥링크 — 있는 테스트는 CI 밖에 있고, 해석 함수는 아예 테스트가 없다 🔴

딥링크는 **외부 입력**이다. 문제가 두 겹이다.

**첫째, 있는 것이 안 돈다.** `app/src/androidTest/.../deeplink/NavRouteUriParserTest.kt` 의 7개 테스트를 **CI 가 절대 실행하지 않는다**(§1.1). 게다가 그 7개 중 3개(`/w/token`·`/inv/CODE`·`/app` 없는 경로)는 **전부 같은 분기**(`segments.first() != APP_SEGMENT`)로 떨어지는 중복이다.

**둘째, 정작 위험한 함수들은 어디에도 테스트가 없다.**

- **`resolveStartRoute` / `resolveNewIntentRoute` — 테스트 0개.** 미검증 분기: `uri == null` → 조용히 null · 친구 초대(`/inv`) → **경고 없이** null · 해석 실패 → `w(TAG)` 경고 후 null · 해석은 됐지만 `appRouteByPath` 에 **미등록** → 같은 경고 후 null · 정상. 미등록 경로가 빈 화면이 아니라 경고로 떨어지는 것이 계약인데 아무도 안 잡는다.
- **`Uri.isFriendInvite()` 는 한 번도 실행되지 않는다.** 기존 `친구 초대` 테스트는 `isFriendInvite` 가 아니라 `toNavRoute` 의 `APP_SEGMENT` 검사로 null 이 된다. `/inv` 단락의 **관측 가능한 차이는 "경고 로그를 억제한다"는 것뿐**인데 그게 안 잡혀 있다.
- **로그 리댁션.** `resolveStartRoute` 는 `uri.path` 만 로그에 남기고 **쿼리 인자는 일부러 뺀다.** 프라이버시 계약인데 assert 가 없다.
- **`toAppLinkUri()` 는 왕복으로만 검증된다.** 인자 값에 `&`·`=`·`/`·공백·한글이 들어갈 때의 URL 인코딩, `args` 가 비면 `?` 가 안 붙는 것, `path` 앞뒤 슬래시가 `//` 를 만드는 것 — 전부 미검증.
- `toNavRoute()` 의 쿼리 엣지: 값 없는 파라미터(`?a`) → `""` 로 보존, **이름이 빈 파라미터는 버림**(`filter { it.isNotEmpty() }`, 테스트 없음), 중복 키는 첫 값만.
- **호스트를 검증하지 않는다.** `https://evil.example/app/...` 도 유효한 `NavRoute` 가 된다. 지금은 **`/app` intent-filter 가 없다는 것이 유일한 안전 근거**이고 KDoc 이 그렇게 적어 두었다 — 그런데 그 가정을 붙들어 두는 테스트가 없다. `/app` 필터를 여는 PR 이 오면 `canManage` 같은 권한 스위치가 그대로 통과한다.

**옮길 수 있는가:** `NavRouteUriParser` 는 `android.net.Uri` 를 **시그니처에** 쓴다(`fun Uri.toNavRoute()`, `fun NavRoute.toAppLinkUri(): Uri`). 순수 JVM 테스트에서는 `Uri.parse` 가 `Stub!` 로 죽는다. **Robolectric 이면 된다** — Robolectric 은 실제 AOSP `Uri` 구현(순수 자바, JNI 없음)을 로드하므로 `pathSegments`·`getQueryParameter`·`Uri.Builder` 가 동일하게 동작한다. 나머지 협력자는 전부 JVM 순수다(`NavRoute` 는 data class, `appRouteByPath` 는 Map, `Observability` 는 fixture 존재).
**막는 것:** Robolectric 이 버전 카탈로그에 없다. 새 의존 추가다(❓, §5-4).

### 3.2 P1 — 위험 대비 공백

#### E. DTO→entity 매퍼의 조용한 기본값

`challenge/data` 와 `profile/data` 의 매퍼는 대부분 널 병합·enum 폴백·계산 필드를 품고 있다. 서버가 필드를 하나 빼먹었을 때 **앱이 조용히 그럴듯한 거짓말을 하는** 자리다.

특히 눈에 띄는 것:

- **같은 `mode` 필드의 폴백이 파일마다 다르다.** `DraftDto`·`ChallengeSettingsResponse`·`MyChallengeResponse` 는 `?: SOLO`, `ChallengeDetailResponse` 는 `?: GROUP`. `status` 도 `?: UPCOMING` 과 `?: ACTIVE` 로 갈린다. **이게 의도인지 사고인지 테스트로도 문서로도 구분되지 않는다.** (❓ 정책 확인 필요 — 테스트를 쓰기 전에 어느 쪽이 맞는지 정해야 한다.)
- **`MyUserResponse.toDomain()` 의 `onboardingCompleted ?: true`** — 서버가 이 필드를 빼면 **온보딩을 건너뛴다.** 실패 방향이 열린 쪽이다.
- `VerificationDto.toDomain()` — `type` 이 없으면 `method` 로 역추론한다(`SELF_CHECK` → `MANUAL`, 아니면 `AUTO`).
- `ExploreChallengesResponse.toDomain()` — `more = hasNext ?: (nextCursor != null)` 이고 `nextCursor = nextCursor.takeIf { more }`. **무한 페이징 방어**인데 미검증.
- `ChallengeCategoryCountResponse.toDomain()` — `Category.fromValue(code)` 실패 시 **표시 라벨로 재조회**(`entries.find { it.label == name }`). 라벨 한 글자만 바뀌면 카테고리가 통째로 사라진다.
- `MyChallengesResponse.toDomain()` — `(challenges ?: items)` 로 **서버의 두 가지 키 이름을 모두 받는다.** 관용의 근거가 테스트에 없다.
- `MyProfileResponse.toDomain()` — `LocalDate.parse` 를 `runCatching{}.getOrNull()` 로 삼킨다. 잘못된 생년월일이 조용히 null 이 된다.
- `StatsResponse.toDomain(requested)` — 서버 에코가 이상하면 **요청한 기간으로 되돌린다.**
- `moderationState()` — `"EXEMPT"`/`"APPROVED"` 를 `APPROVED` 로 합치고, `"NONE"` 과 미지 값을 똑같이 `NONE` 으로 떨군다.

또한 **폴백을 안 두기로 한 자리**(`TodayVerificationStatus`·`ThreadItemType`·`ChallengeVisibility`·`WatcherChannel`·`Gender` 등은 null 을 그대로 흘려 화면이 필드를 생략한다)도 테스트가 없다. 이건 특히 아깝다 — 누군가 "버그인 줄 알고" 폴백을 넣어 고칠 수 있는 자리다. `JoinBlockReasonTest` 의 `` `모르는 사유는 null 이다` `` 가 정확히 이 역할을 하는 모범 사례다.

#### F. `challenge/domain` 엔티티 — enum 은 15종인데 테스트는 2종

`fromValue` 를 가진 enum 이 `Challenge.kt`·`ChallengeDetail.kt`·`ChallengeMember.kt`·`Delegation.kt`·`Explore.kt`·`Room.kt`·`RoutineVerification.kt`·`Thread.kt`·`Watcher.kt` 에 걸쳐 있는데, 전수·미지값 계약을 잡은 것은 `JoinBlockReasonTest`·`ExploreTest` 둘뿐이다.

추가로:

- **`ParamSpec.clamp(value: Double)`** (`RoutineVerification.kt`) — 순수 함수, 미검증. CLAUDE.md 의 "clamp 는 domain 타입에" 규칙의 실체다.
- `Challenge.kt` 의 `init { require(...) }` 는 `CreateChallengeCommandTest` 가 4케이스로 잡는다 — 이건 잘 돼 있다.

#### G. `profile/domain` — 12파일 0테스트

테스트 의존은 이미 있다(`kotlin("test-junit")`). 소스셋만 없다. `Reputation`·`StatsReport`·`ActivityCalendar` 에 계산·경계값이 있으면 케이스 층으로 바로 갈 수 있다.

#### H. 관측 — 카탈로그는 잡혔는데 "실제로 쏘는가"는 아무도 안 본다

`Observability` 를 주입받는 ViewModel 이 7개다(`ExploreList`·`Explore`·`ChallengeDetail`·`CreateChallenge`·`Splash`·`Login`·`Onboarding`). `ChallengeEventsTest`/`OnboardingEventsTest` 는 **이벤트 페이로드의 모양**을 고정하지만, **그 이벤트가 옳은 순간에 방출되는지**는 하나도 잡지 않는다. `ExploreListViewModel.logImpression` 의 중복 노출 방지, `LoginViewModel` 의 실패 시 `error_code` 동봉 같은 것이 여기 해당한다.

`RecordingSink`·`TestObservability` fixture 가 이미 있으므로, presentation 모듈이 `testFixtures(project(":observability:domain"))` 를 가져오면 바로 가능해진다.

`observability/data` 의 `AmplitudeEventMapper`·`FirebaseEventMapper`·`ChannelFilterSink`·`SeverityFilterSink` 도 순수 매퍼/필터인데 미검증이다(`CompositeSink` 만 잡혀 있다).

#### I. 라우트 등록 정합성

`AppRouteRegistry` 에 27개 라우트가 있고, feature 쪽에는 `Page` 구현이 17개 있다. `AppRouteAccessPolicyTest` 는 **로그인 게이팅**만 잡는다. 잡히지 않는 것:

- 모든 `Page.toRoute().path` 가 `appRoutes` 에 실제로 등록돼 있는가 (등록 누락 = 런타임에 빈 화면)
- `AppRoutes` 상수와 `Page` 가 쓰는 경로가 일치하는가
- `syntheticStack` 이 만드는 백스택이 기대대로인가

`appRoutes` 는 `app/src/test` 에서 이미 접근 가능하다(`AppRouteAccessPolicyTest` 가 그러고 있다). **새 의존 없이 오늘 쓸 수 있는 테스트다.**

#### J. 개별 누락

- `verification/domain/usecase/SubmitDeviceIntroUseCase` — 협력자 4개, **저장 후 리스케줄** 이라는 순서 계약이 있는데 미검증. 같은 모듈의 다른 UseCase 2개는 잡혀 있다.
- `onboarding/data/AuthRepositoryImpl` — `mapAuthFailure` 가 `ApiException`·`IOException` 을 `AuthException` 으로 옮긴다. 미검증(같은 모듈 `TokenRefresherImpl` 만 잡혀 있다).
- `onboarding/presentation/common/AuthFailureUi.kt` 의 **`toAuthFailureUi()`** — KDoc 이 *"기획 스펙의 «모든 에러 코드에 UI 가 있어야 한다»를 한곳에서 지킨다"* 라고 선언한다. **그 전수성을 강제하는 테스트가 없다.** `AuthFailure` 항목이 늘면 `when` 이 컴파일 에러로 잡아 주긴 하지만, Toast/Dialog/Blocking 의 **등급 배정**(예: `BIRTHDATE_UNDERAGE` 는 Blocking, `SESSION_EXPIRED` 는 `restartFromLogin = true`)은 아무도 안 지킨다. 순수 함수라 케이스 층에 딱 맞는데, 이 모듈에 테스트 의존이 없어서 못 쓴다.
- `ProfileEditViewModel.kt` 최상단의 **`String?.remainingDays()`** — 닉네임 변경 쿨다운 계산. 순수 함수, 미검증.
- `CreateChallengeViewModel` 의 `isValidPeriod`·`durationDays`, `MyChallengeStoreImpl.all()`(**역순 반환 + upsert 시 원래 순서 유지**), `TargetAppStoreImpl.isRegistered()`(**빈 리스트는 미등록**) — 전부 순수 JVM, 전부 미검증.

### 3.3 P2 — 정리

- `app/src/test/.../ExampleUnitTest.kt` (`assertEquals(4, 2 + 2)`) 와 `app/src/androidTest/.../ExampleInstrumentedTest.kt` 는 프로젝트 템플릿 잔재다. 지운다.

---

## 4. 층별 규약

새 테스트를 쓸 때 지킨다.

**어디에 쓰나**
- 테스트는 **테스트 대상과 같은 Gradle 모듈의 `src/test`** 에 둔다. `data` 모듈의 매퍼는 `internal` 이지만 같은 모듈의 유닛 테스트 소스셋에서 보인다.
- **`src/androidTest` 에 새로 쓰지 않는다.** CI 가 안 돈다(§1.1).
- 패키지 경로는 대상과 동일하게 미러링한다.

**어떻게 쓰나**
- `kotlin.test` (`kotlin("test-junit")`) 를 쓴다. `app` 만 `org.junit` + Konsist 다.
- **테스트 이름은 백틱 한국어 문장으로, "무엇이 참인가"를 적는다.** `` fun `모르는 정렬 값은 null 이다`() `` 처럼. `testFooReturnsNull` 같은 이름을 쓰지 않는다.
- **주석은 "이게 깨지면 무슨 일이 나는지"를 적는다.** 코드를 다시 설명하지 않는다. 기존 예: `// 딥링크는 외부 입력이다. 모르는 경로를 공개로 보면 오타 하나가 인증 우회 통로가 된다.`
- **모킹 라이브러리를 도입하지 않는다.** 손으로 쓴 fake 를 쓰고, 검증 대상이 아닌 메서드는 호출되면 실패시킨다(`FakeChallengeRepository` 방식).
- **fake 를 두 모듈 이상이 쓰게 되면 `testFixtures` 로 올린다.** 한 모듈만 쓰면 그 모듈 `src/test` 에 둔다.

**무엇을 어느 층에서 잡나**
- 협력자가 없는 규칙·검증·정규화 → **케이스 층**(엔티티/값 객체). CLAUDE.md 의 "규칙은 entity 소관" 과 같은 선이다.
- 서버 문자열 → enum: **전수 매핑 1케이스 + 미지값 1케이스.** 미지값이 null 인지 폴백인지가 계약이므로 반드시 적는다.
- DTO→entity 매퍼: **필드 전수를 쓰지 않는다.** 널 병합·계산 필드·enum 폴백·리스트 드롭이 있는 필드만 잡는다.
- Repository 구현: **에러 코드 → 도메인 예외 갈래**를 잡는다. 성공 경로는 매핑이 비자명할 때만.
- UseCase: **조립 순서와 부수효과**(무엇을 어떤 순서로 불렀는가). 값 매핑은 아래층에 맡긴다.
- ViewModel: **`reduce` 는 순수 함수로 전이만**, `onIntent` 는 fake 를 세워 **분기와 이펙트 방출**만. 문구 조립은 최상위 순수 함수로 빼서 케이스 층에서 잡는다(현행 `AppealSheetTest` 방식).

**무엇을 잡지 않나**
- Compose 렌더링 결과, 색·간격, 미리보기.
- Retrofit 인터페이스 선언, Hilt 모듈 배선(컴파일이 잡는다).
- `when` 이 컴파일 에러로 이미 강제하는 전수성 — 다만 **분기의 결과값 배정**은 잡는다(§3.2-J 의 `toAuthFailureUi`).

---

## 5. 백로그

위에서 아래로. 각 항목은 별도 이슈·별도 PR 로 쪼갠다(CLAUDE.md 워크플로).

**0단계 — 코드를 쓰기 전에 사람이 답해야 하는 것**

0. **`TokenAuthenticator` 의 no-auth 경로 재시도가 의도인가**(§3.1-A ⚠️). 버그면 이건 테스트 백로그가 아니라 버그 이슈다. 답이 나와야 5번의 테스트를 어느 쪽으로 쓸지 정해진다.
0-b. **`ChallengeDetailResponse` 의 `mode ?: GROUP` / `status ?: ACTIVE` 가 다른 매퍼와 다른 것이 의도인가**(§3.2-E). 답이 나와야 6·7번이 무엇을 고정할지 정해진다.

**1단계 — 새 의존 없이 오늘 가능한 것**

1. `app/src/test` 에 **라우트 등록 정합성 테스트** 추가 (§3.2-I). `appRoutes` 는 이미 접근 가능하다.
2. `challenge/domain` 의 남은 enum `fromValue` 전수·미지값 테스트, `ParamSpec.clamp` (§3.2-F).
3. `profile/domain` 에 `src/test` 신설 (§3.2-G).
4. `core/datastore` 의 빈 자리 4개 — `userId` Flow, 캐시 워밍, 쓰기 경로 비-IOException, `isLoggedIn` 비대칭 (§3.1-A-2). fake 와 fixture 가 이미 다 있어 제일 싸다.
5. 템플릿 스텁 2개 삭제 (§3.3).

**2단계 — 테스트 의존 한 줄씩 추가 (구조 변경 없음)**

6. **`core/network`** 에 테스트 소스셋 신설 (§3.1-A). 순서: `BaseResponse` 4분기 + `getOrThrow`/`throwOnError` 비대칭(제일 싸다) → `TokenAuthenticator` 분기, 특히 **"갱신이 예외로 실패하면 `clear()` 를 부르지 않는다"** → `provideRetrofit` 의 baseUrl 정규화·fail-fast. **`NetworkModule` 인터셉터는 여기서 손대지 않는다**(10번).
7. **`challenge/data`** 에 테스트 소스셋 신설 (`kotlin("test-junit")` + `kotlinx.serialization.json` — 매퍼가 `JsonElement`/`JsonObject` 를 다뤄 필수). `toRequestBody` 3-상태 → `ChallengeRepositoryImpl` 에러 변환 → 인메모리 store 순 (§3.1-C).
8. `profile/data` 매퍼 (§3.2-E), `onboarding/data/AuthRepositoryImpl` (§3.2-J).
9. `verification/domain/SubmitDeviceIntroUseCase` (§3.2-J), `observability/data` 의 매퍼·필터 싱크 (§3.2-H).

**2.5단계 — 리팩터가 선행되는 것 (❓ 승인 필요)**

10. `NetworkModule` 의 인증 인터셉터를 `com.ruleup.network.auth` 의 이름 있는 클래스로 추출 (§3.1-A). 그러면 `NO_AUTH_PATHS` 스킵·콜드스타트 블로킹·로그 리댁션이 전부 케이스 층으로 내려온다. **테스트를 위한 프로덕션 코드 변경이므로 진행 전 확인받는다.**

**3단계 — ViewModel (구조 결정이 선행돼야 한다)**

11. `onboarding/presentation` 에 테스트 의존 추가 후 **`toAuthFailureUi()` 등급 배정 테스트** — 순수 함수라 아래 결정 없이도 가능하고, 스펙이 KDoc 에 명문화돼 있어 값어치가 높다 (§3.2-J). ViewModel 로 가기 전에 이것부터 한다.
12. **선행 결정 (❓)**:
    (a) `reduce`·`currentState` 가 `protected` 인 채로 테스트용 서브클래스를 세울지, 가시성을 열지.
    (b) presentation 모듈에 `kotlinx-coroutines-test` 를 추가할지.
    (c) feature `domain` 모듈에 testFixtures 를 켜서 fake 를 presentation 과 공유할지. — **android library 모듈이므로 `java-test-fixtures` 플러그인이 아니라 `android { testFixtures { enable = true } }` 다.** JVM 모듈인 `observability:domain`·`core:domain` 과 문법이 다르다.
13. `reduce` 전이 테스트를 큰 것부터: `ChallengeDetail` → `CreateChallenge` → `Onboarding` → `VerificationLocation`.
14. `onIntent` 경로 + 관측 이벤트 방출 시점 (§3.2-H). §3.1-B-3 의 이펙트 하니스 함정을 그대로 따른다.

**4단계 — Robolectric 도입 여부 (❓ 결정 필요)**

15. 도입하면 세 가지가 풀린다: 딥링크 테스트가 CI 로 들어오고 + `resolveStartRoute`/`isFriendInvite` 를 처음으로 잡을 수 있고(§3.1-D), `SetupNotifierImpl` 을 잡을 수 있다(§3.1-C). 도입하지 않으면 셋 다 **영구히 미검증으로 남는 것을 받아들이는 선택**이다. 버전 카탈로그에 Robolectric 이 없으므로 새 의존 추가이고, `testOptions { unitTests.isIncludeAndroidResources = true }` 설정도 따라온다 — 진행 전 확인받는다.

---

## 6. 명시적 비범위

지금 테스트하지 않기로 한 것. 나중에 "왜 여기만 비었지"를 다시 묻지 않기 위해 적는다.

- **`core/designsystem` (14파일)** — 토큰과 Compose 컴포넌트. 렌더링 결과를 유닛 테스트로 잡을 값어치가 없다.
- **`observability/debug` (3파일)** — `debugImplementation` 이라 릴리스 APK 에 없다.
- **Hilt 모듈 / Retrofit API 인터페이스 선언** — 컴파일과 앱 기동이 잡는다.
- **`observability/domain` 의 `model/`·`event/` 대부분** — 로직 없는 데이터 홀더다. `TtiTracker` 만 로직이 있고 이미 잡혀 있다.
- **`verification/data` 의 Android 프레임워크 수집기** (`HealthConnectCollector`·`UsageEventCollector`·`GeofenceBroadcastReceiver`·`VerificationSyncWorker`) — 실기기 신호가 필요하다. 이건 `VERIFICATION_TEST_PLAN.md` 의 수동 QA 시나리오가 담당한다. **순수 매핑 부분은 이미 분리돼 테스트돼 있다**(`UsageSignalMappingTest`·`GeofenceReconcileTest`) — 이 분리가 옳은 형태다.
- ❓ **Compose UI 테스트 층 전체** — 지금 없다. 만들지 여부는 결정 사항이다. 만들지 않기로 하면 화면 조립은 계속 수동 QA 몫으로 남는다.

---

## 7. 이 문서를 갱신하는 시점

- 새 feature 모듈을 만들 때 → §1.2 표에 줄 추가
- §5 백로그 항목을 끝냈을 때 → 해당 항목과 대응하는 §3 항목 제거
- ❓ 결정이 내려졌을 때 → 결정과 근거를 본문에 반영하고 ❓ 제거

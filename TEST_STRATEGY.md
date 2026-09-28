# RuleUp 테스트 전략

마지막 갱신: 2026-09-28 · 현황 표는 `.claude/skills/testing/scripts/coverage_map.py` 출력

이 문서의 중심은 커버리지 숫자가 아니라 **3절 미검증 목록**이다. 숫자만 있는 문서는 늘어나는 걸
보며 안심하게 만들 뿐, 다음에 뭘 해야 하는지 말해주지 않는다.

verification 모듈의 수동 QA 시나리오는 `VERIFICATION_TEST_PLAN.md` 를 따로 본다.

## 1. 다섯 층

| 층 | 무엇을 지키는가 | 대상 | 실행 |
|---|---|---|---|
| 케이스 | 규칙 하나 | entity·값 객체·매퍼·순수 함수 | `./gradlew test` |
| 모듈 | 케이스 | 모듈 | UI | 통합 | 인수 | 합계 |
|---|---|---|---|---|---|---|
| `(unknown)` | 114 | – | – | – | – | 114 |
| `:.agents\skills\testing-workspace\iteration-1\eval-2-viewmodel-tests\with_skill\outputs\challenge\presentation` | 8 | 25 | – | – | – | 33 |
| `:.agents\skills\testing-workspace\iteration-1\eval-2-viewmodel-tests\without_skill\outputs\challenge\presentation` | – | 34 | – | – | – | 34 |
| `:.agents\skills\testing-workspace\iteration-1\eval-3-ui-robolectric\with_skill\outputs\challenge\presentation` | – | – | 20 | – | – | 20 |
| `:.agents\skills\testing-workspace\iteration-1\eval-3-ui-robolectric\without_skill\outputs\challenge\presentation` | – | – | 50 | – | – | 50 |
| `:.agents\skills\testing-workspace\iteration-2\eval-2-viewmodel-tests\with_skill\outputs\challenge\presentation` | 7 | 26 | – | – | – | 33 |
| `:.agents\skills\testing-workspace\iteration-2\eval-2-viewmodel-tests\without_skill\outputs\challenge\presentation` | 7 | 26 | – | – | – | 33 |
| `:.agents\skills\testing-workspace\iteration-2\eval-3-ui-robolectric\with_skill\outputs\challenge\presentation` | – | – | 28 | – | – | 28 |
| `:.agents\skills\testing-workspace\iteration-2\eval-3-ui-robolectric\without_skill\outputs\challenge\presentation` | 5 | – | 69 | – | – | 74 |
| `:.agents\skills\testing-workspace\iteration-3\eval-2-viewmodel-tests\with_skill\outputs\challenge\presentation` | 11 | 26 | – | – | – | 37 |
| `:.agents\skills\testing-workspace\iteration-3\eval-2-viewmodel-tests\without_skill\outputs\challenge\presentation` | – | 32 | – | – | – | 32 |
| `:.agents\skills\testing-workspace\iteration-3\eval-3-ui-robolectric\with_skill\outputs\challenge\presentation` | – | – | 22 | – | – | 22 |
| `:.agents\skills\testing-workspace\iteration-3\eval-3-ui-robolectric\without_skill\outputs\challenge\presentation` | 37 | – | – | – | – | 37 |
| `:app` | – | – | 27 | 25 | 16 | 68 |
| `:challenge\data` | 50 | – | – | – | – | 50 |
| `:challenge\domain` | 34 | 4 | – | – | – | 38 |
| `:challenge\presentation` | 74 | 82 | 68 | – | – | 224 |
| `:core\datastore` | – | 13 | – | – | – | 13 |
| `:core\domain` | 23 | – | – | – | – | 23 |
| `:core\network` | 5 | – | – | – | – | 5 |
| `:home\presentation` | 9 | 7 | 6 | – | – | 22 |
| `:logging\domain` | 10 | – | – | – | – | 10 |
| `:notification\data` | 10 | – | – | – | – | 10 |
| `:notification\domain` | 12 | – | – | – | – | 12 |
| `:notification\presentation` | 3 | 18 | 13 | – | – | 34 |
| `:observability\data` | 20 | – | – | – | – | 20 |
| `:onboarding\data` | 17 | – | – | – | – | 17 |
| `:onboarding\domain` | 8 | 35 | – | – | – | 43 |
| `:onboarding\presentation` | 14 | 31 | 39 | – | – | 84 |
| `:profile\data` | 30 | – | – | – | – | 30 |
| `:profile\domain` | 5 | – | – | – | – | 5 |
| `:profile\presentation` | 8 | 75 | 76 | – | – | 159 |
| `:report\data` | 12 | 8 | – | – | – | 20 |
| `:report\domain` | 14 | – | – | – | – | 14 |
| `:report\presentation` | 5 | 9 | 13 | – | – | 27 |
| `:support\domain` | 12 | – | – | – | – | 12 |
| `:support\presentation` | 3 | 23 | 10 | – | – | 36 |
| `:tti\domain` | 13 | – | – | – | – | 13 |
| `:verification\data` | 59 | 15 | – | – | – | 74 |
| `:verification\domain` | 36 | 32 | – | – | – | 68 |
| `:verification\presentation` | 7 | 17 | 8 | – | – | 32 |
| **합계** | **682** | **538** | **449** | **25** | **16** | **1710** |

테스트 파일 수: 케이스 108, 모듈 56, UI 58, 통합 8, 인수 2

## 3. 미검증 — 알면서 안 하고 있는 것

### 2026-09-28 권한·동의 및 안내 모달 통합 검증 (#504 · #505)

현황 표는 2026-09-28 다시 집계했다. 이번 변경에서는 presentation의 `SensitiveConsentTest`를
verification domain의 아래 테스트로 대체하고 생성·참여 연동 검증을 추가했다.

- `PermissionSnapshotTest`: 서버 권한 토큰에 따른 위치·건강 동의 매핑, 다른 권한과 미지원 토큰 제외.
- `CheckVerificationAccessUseCaseTest`: 기기 권한과 서버 동의의 독립성, 중복 동의 제거, 빈 권한 목록, 조회 실패.
- `AgreeVerificationConsentUseCaseTest`: 기존·인트로 약관 버전 선택, 두 동의의 일괄 저장,
  빈 동의 목록·중복 제거, 저장 실패, 인증과 무관한 약관 제출 방지.
- `CreateChallengeViewModelTest`·`ChallengeDetailJoinTest`: 서버 권한 목록 우선, 수동 인증 전환,
  동의 저장 후 권한 요청, 거부 시 설정 유지, 복귀 후 생성·참여 재개, 닫은 설정의 늦은 결과 무시.
  권한 설정만 연 경우에는 장소·앱 등록을 건너뛰어 참여하지 않는다.
- `VerificationAccessContentTest`: 사용자 요청(#505)에 따른 단일 안내의 동의·권한 표시,
  저장 중 연타 차단, 동의 완료 후 권한 요청 버튼, 다음에 하기 동작.

실제 OS·Health Connect 권한창과 서버 연결을 함께 거치는 실기기 검증은 아래 환경 항목에 남아 있다.

품질 논의는 이 절에서 한다. 각 항목은 *무엇을 못 잡는가 · 왜 안 했나 · 풀리는 조건*을 적는다.

### 코드로 메울 수 있는 것

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| RepositoryImpl 9건 (Room·Watcher·Auth·DeviceIdentity·Intro·MyPage·Profile·Account·Signal) | 매핑은 덮었지만 **impl 의 조립·예외 변환**은 안 덮였다 | 위험이 큰 축(Challenge 에러 번역·Explore)부터 먼저 했다 | 이어서 진행 |
| `ChallengeDetailViewModel` 의 나머지 전이 | 방 탭·이의·감시자·권한 경로 | 1005줄에 협력자 11종 — 가입 경로만 덮었다. 한 파일에 다 넣으면 무엇이 깨졌는지 읽기 어려워진다 | 경로별로 나눠 진행 |
| 화면 3건 (ChallengeTargets·Splash·VerificationLocation) | 상태별 렌더 | 순수 함수(`filterApps`·`updateMessage`)는 덮었고, 나머지는 Context·런처가 얽혀 화면 분리가 선행한다 | 화면 분리 합의 |

### 판단이 필요한 것 — 코드로는 못 정한다

전부 이슈로 올려 뒀다. **각 항목의 현재 동작은 테스트가 못 박고 있는데, 그건 "이게 옳다"가
아니라 "지금 이렇다"를 기록한 것이다** — 판단이 서면 해당 테스트도 함께 뒤집어야 한다.

| 이슈 | 무엇 | 판단 주체 |
|---|---|---|
| [#399](https://github.com/RuleUp-ASM/Android/issues/399) | 관심 단계 문구가 Figma 와 다르다 (`습관` vs `챌린지`) | 기획 |
| [#400](https://github.com/RuleUp-ASM/Android/issues/400) | 로그인 화면 문구가 Figma 와 다르다 (`계속하기` vs `시작하기`) | 기획 |
| [#401](https://github.com/RuleUp-ASM/Android/issues/401) | 모르는 참여 형태를 매퍼마다 다르게 접는다 — 같은 챌린지가 목록과 상세에서 다르게 보인다 | 정책 |
| [#402](https://github.com/RuleUp-ASM/Android/issues/402) | `setCapacity` 가 ViewModel 에서 clamp — `CLAUDE.md` 검증 계층 규칙과 어긋난다 | 설계 |
| [#403](https://github.com/RuleUp-ASM/Android/issues/403) | 활동 캘린더에 조회 실패를 알리는 자리가 없다 — 실패와 "기록 없는 달"이 같아 보인다 | 디자인 |
| [#404](https://github.com/RuleUp-ASM/Android/issues/404) | 대상 앱 검색이 줄임말을 못 찾는다 — 못 찾으면 등록을 포기하고 자동 인증이 성립하지 않는다 | 기획 |

### 환경이 필요한 것

| 무엇 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|
| 런타임 권한 다이얼로그·지오펜스 | Robolectric 이 못 흉내낸다 | 에뮬레이터 CI 워크플로 |
| 계측 테스트 8건이 CI 밖 | `test.yml` 이 `./gradlew test` 만 돈다 | 위와 같은 워크플로 |
| ~~인수 테스트 실행 확인~~ | — | **2026-09-07 해소.** 시크릿을 받아 스테이징에서 14건 전부 통과시켰다. 그 과정에서 하네스 버그 둘(봉투 미해제·`dev/tokens` 경로 이중 `/api`)과 `#417`(4xx 미변환)을 잡았다 | — |
| `TokenAuthenticator` 401 갱신 | 자동 로그아웃 분기가 어긋나면 전 사용자가 튕긴다 | `core:network` 에 테스트 소스셋이 **생겼다**(`ErrorBodyInterceptorTest`) — 이제 막는 건 시간뿐이다 | 이어서 진행 |


### 2026-09-28 · 권한·공통 모델·화면 계측

- `PreviewSmokeTest`: 이번에 추가한 79개 Preview를 호스트 주입이나 네트워크 없이 렌더링한다. 카카오 지도는 inspection placeholder로 검사한다.
- `ScreenTrackingTest`: 복원된 첫 화면, 같은 경로의 다른 인자, 뒤로 가기, 중복/미등록 경로의 screen_view를 검사한다.
- `TtiScreenTest`: 초기 로딩 완료·렌더 프레임·지도 완료 이후 1회 전송을 검사한다. 실제 기기의 TTI 수치와 jank는 측정하지 않았다.
- 공통 User/Challenge 포함 구조와 DTO request/response 변경은 기존 매퍼·ViewModel·UI 테스트를 새 모델로 이행해 검증한다.
- `ChallengeTargetsViewModelTest`: 데이터 없음·조회 실패도 초기 로딩을 종료하는지 검사한다.
- `assembleDebug`, `test`, `lint` 통과. 서버 인수 테스트는 기본 실행에서 제외되며, OS 권한창·FCM·실제 지도·Health Connect·릴리즈 설치는 이번에 실행하지 않았다.

## 4. 인수 시나리오 ↔ 하위 테스트

인수가 깨졌는데 하위 층이 전부 초록이었다면 **하위 층에 구멍이 있다**는 뜻이고, 그게 다음 작업이다.

| 스토리 | 상태 | 미리 잡아주는 하위 테스트 |
|---|---|---|
| 로그인 → 첫 화면 진입 | 하위만 | `SplashViewModelTest`(진입 순서·딥링크) · `LoginViewModelTest`(결과 4갈래) · `AuthResponseMappingTest` |
| 탐색 목록·인기 조회 | **인수 있음** | `ExploreAcceptanceTest` · `ExploreListViewModelTest` · `ExploreResponseMappingTest` |
| 챌린지 생성 → 내 목록에 보임 | 하위만 | `CreateChallengeCommandTest` · `CreateChallengeViewModelTest` · `HomeChallengeMergeTest` |
| 초대 링크로 참여 → 방 진입 | 하위만 | `ChallengeInviteViewModelTest` · `ChallengeInviteContentTest` · `InviteLinkTest`(딥링크 세 갈래) · `ChallengeDetailJoinTest` |
| 감시자 초대 링크로 수락 → 통지 수신 대상이 됨 | 하위만 | `WatcherAcceptViewModelTest`(수락 전/후 분리) · `InviteLinkTest` · `WatchingViewModelTest`(수신 끄기) |
| 알림 탭 → 읽음 지점이 뒤로 밀리지 않는다 | 하위만 | `NotificationCenterViewModelTest`(첫 페이지에서만 갱신) · `NotificationPageTest`(기준선 판정) · `PushMessageTest` |
| 매너 온도 폐기 후 티어가 화면 전체에서 일관 | **인수 있음** | `AccountContractAcceptanceTest` · `MyTierResponseMappingTest` · `MyTierContentTest`(유예 밴드) |
| 약관 재동의·철회가 사유별로 갈린다 | **인수 있음** | `AccountContractAcceptanceTest`(철회 금지·버전 불일치) · `AgreementsViewModelTest` · `ErrorBodyInterceptorTest` |
| 인증 제출 → 오늘 상태가 바뀜 | 하위만 | `RunSyncUseCaseTest` · `SubmitDeviceIntroUseCaseTest` · `TodayStatusTest` |

## 5. 돌리는 법

```bash
./gradlew test                      # 케이스·모듈·UI·통합 (CI 와 동일)
./gradlew ktlintFormat              # 커밋 전
./gradlew :profile:presentation:testDebugUnitTest --tests "*MyHomeViewModelTest*"
./gradlew :core:domain:test --tests "*CategoryTest*"   # 순수 JVM 모듈은 variant 가 없다
python3 .claude/skills/testing/scripts/coverage_map.py  # 이 문서 2절 갱신용
```

```bash
# 인수 — 개발용 토큰 발급 경로(POST /api/v1/dev/tokens)를 쓴다.
RULEUP_ACCEPTANCE=1 DEV_TOKEN_SECRET=... \
  ./gradlew :app:testDebugUnitTest --tests "*AcceptanceTest*"
```

켜지 않으면 **실패가 아니라 건너뜀**이다 — 리포트에 "건너뜀"으로 남아야 존재가 드러난다.
실패로 두면 사람들이 무시하는 법을 배우고, 아예 빼면 있다는 걸 아무도 모른다.

인수 테스트는 기본 CI 에서 뺀다 — 실서버 상태를 바꾸므로 PR 마다 돌리면 데이터가 쌓이고
CI 가 남의 네트워크 사정에 인질이 된다.

## 갱신 규칙

테스트를 늘리거나 줄이는 PR 은 이 문서도 같이 고친다. 코드와 같은 PR 에 있어야 안 썩는다.
2절 표는 손으로 세지 말고 스크립트 출력을 붙인다. 3절은 손으로 쓴다 — **왜 안 했는지가 값이고,
그건 기계가 모른다.**


## #509 QA 수정 검증 (2026-09-28)

- 완료: 종료일 기준 D-day, 복제 초안 1회 전달, 초기 캘린더 선택일 조회, 제재 ID 누락 보존, 빈 약관 버전 폴백, 미래 생일 구분, KST 제재 시각, 429 기본 대기, 수집 대상 복구와 실패 시 기존 대상 보존, 중복 신고 코드 매핑의 회귀 검증.
- 완료: `ktlintFormat test assembleDebug assembleRelease lint --no-parallel --no-daemon --continue` 전체 성공. debug/release 테스트 및 릴리즈 빌드 포함. 현재 release는 minify 비활성 상태이므로 R8 검증은 포함하지 않는다.
- 미검증: Play 설치 referrer→가입→초대 알림 종단, 실제 FCM/Crashlytics·카카오 키, API 26·OEM 절전·GPS 체류·Health Connect 판정·서버 자정 전환. API36 실기기에서 권한·수집 대상 복구·sync 요청 성공까지 확인. 실제 건강 기록·GPS 체류·서버 발행 조건이 필요한 판정은 미검증.
- 미검증: Room 버퍼 15일/10,000행 상한의 대량 실데이터 성능과 BUFFER_EVICTED 서버 수신 결과. SQL 컴파일은 확인했으나 단말 부하 시험은 별도 필요.
- 미검증: 가로 화면/분할 화면의 카드·칩·실패 예정 카드 여백은 재현 화면과 크기 조건 필요.


## #509 실기기 QA (2026-09-28)

- 환경: Samsung SM-S948N, Android 16/API36, staging debug, chore/503. 사용자 승인 계정 및 실제 신호로 검증.
- 완료: APK 데이터 유지 업데이트, 로그인 후 홈/마이 조회, cold/warm deeplink extra의 활동 캘린더 선택 날짜와 상세 표시.
- 완료: 전경·백그라운드 위치, 사용정보 접근, Health Connect 걸음·거리·수면 권한 요청과 복귀 상태 반영. FCM 토큰 서버 등록.
- 추가 수정: 전체 Android 권한 이름을 정규화해 허용 상태·수집 대상·정보 수집 동의·권한 요청 경로·권한 복구 행 필터에서 동일하게 해석. 실제 서버의 android.permission.health.READ_STEPS/READ_DISTANCE로 수집 대상 0→2 복구 확인.
- 완료: 21:41 KST 실제 동기화 HTTP 200, Worker SUCCESS. 건강 수집 대상 2개, 실제 건강 기록 0건으로 판정 성공은 미검증. GPS 챌린지는 서버 인증 장소 미설정 상태.
- 검증: verification:domain 전체 단위 테스트 및 debug 앱/계측 APK 빌드 성공. PermissionSnapshotTest 10건, RestoreVerificationTargetsUseCaseTest 4건 성공. 실기기 계측 테스트 8건 성공(URI 7건 + 앱 컨텍스트 1건).
- 남음: Health Connect 실제 기록 제공 및 판정, 건강 백그라운드 읽기(현재 DENIED), GPS 장소 설정·체류·재부팅, OEM 절전 장시간 생존, 실제 FCM/Crashlytics, Play 설치 경로, API26, 자정 전환. 현재 기기만으로 모든 환경 조건을 대체할 수 없음.
- 추가 확인: challenge:presentation·verification:presentation 단위 테스트와 변경 모듈 ktlintCheck 성공. git diff --check 통과.

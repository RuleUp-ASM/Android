# RuleUp 테스트 전략

마지막 갱신: 2026-09-02 · 현황 표는 `.claude/skills/testing/scripts/coverage_map.py` 출력

이 문서의 중심은 커버리지 숫자가 아니라 **3절 미검증 목록**이다. 숫자만 있는 문서는 늘어나는 걸
보며 안심하게 만들 뿐, 다음에 뭘 해야 하는지 말해주지 않는다.

verification 모듈의 수동 QA 시나리오는 `VERIFICATION_TEST_PLAN.md` 를 따로 본다.

## 1. 다섯 층

| 층 | 무엇을 지키는가 | 대상 | 실행 |
|---|---|---|---|
| 케이스 | 규칙 하나 | entity·값 객체·매퍼·순수 함수 | `./gradlew test` |
| 모듈 | 한 모듈의 계약 | UseCase·RepositoryImpl·ViewModel | `./gradlew test` |
| UI | 상태 → 화면, 조작 → 의도 (기대값의 출처는 **Figma**) | Composable (Robolectric) | `./gradlew test` |
| 통합 | 모듈 경계를 건너는 결합 | 네비게이션·직렬화·아키텍처 규칙 | `./gradlew test` |
| 인수 | 사용자 스토리 | 실서버 관통 | 수동/야간 |

**경로는 갈라지는 층에서 한 번만 검증한다.** 위층은 아래층을 옳게 엮었는지만 본다 — 층마다
되풀이하면 테스트 수가 곱으로 늘고, 그러면 아무도 리팩터링을 못 한다.

층을 고르는 기준은 하나다: **이 규칙이 깨지면 어느 파일을 고칠 것인가.** 그 파일이 사는 층에 둔다.
`presentation` 안에 있다고 UI 층이 아니다 — Composable 에서 뽑아낸 순수 함수는 케이스 층이다.

작성 기준 전체는 `.claude/skills/testing/SKILL.md`.

## 2. 현황

숫자는 `@Test` 개수. `–` 는 그 층 테스트가 없다는 뜻이다.

| 모듈 | 케이스 | 모듈 | UI | 통합 | 인수 | 합계 |
|---|---|---|---|---|---|---|
| `:app` | – | – | – | 18 | 4 | 22 |
| `:challenge:data` | 34 | – | – | – | – | 34 |
| `:challenge:domain` | 28 | 4 | – | – | – | 32 |
| `:challenge:presentation` | 30 | 51 | 30 | – | – | 111 |
| `:core:datastore` | – | 13 | – | – | – | 13 |
| `:core:domain` | 17 | – | – | – | – | 17 |
| `:home:presentation` | 8 | 7 | 6 | – | – | 21 |
| `:observability:data` | 20 | – | – | – | – | 20 |
| `:observability:domain` | 11 | – | – | – | – | 11 |
| `:onboarding:data` | 11 | – | – | – | – | 11 |
| `:onboarding:domain` | 8 | 30 | – | – | – | 38 |
| `:onboarding:presentation` | 3 | 18 | 30 | – | – | 51 |
| `:profile:data` | 8 | – | – | – | – | 8 |
| `:profile:presentation` | 4 | 60 | 38 | – | – | 102 |
| `:verification:data` | 55 | 15 | – | – | – | 70 |
| `:verification:domain` | 22 | 14 | – | – | – | 36 |
| `:verification:presentation` | 7 | 10 | 3 | – | – | 20 |
| **합계** | **266** | **222** | **107** | **18** | **4** | **617** |

테스트 파일 수: 케이스 51, 모듈 32, UI 23, 통합 5, 인수 1

테스트 파일 수: 케이스 51, 모듈 32, UI 23, 통합 5, 인수 1

앞의 네 층은 전부 JVM 에서 돌아 CI(`test.yml`)가 그대로 커버한다. 인수만 밖에 있다.

**표가 말하지 않는 것** — `:app` 의 통합 18건 중 8건은 `src/androidTest` 라 CI(`./gradlew test`)가
돌리지 않는다. 딥링크 파서 테스트가 여기 있어서 **외부 진입점 회귀가 초록불로 지나간다.**

## 3. 미검증 — 알면서 안 하고 있는 것

품질 논의는 이 절에서 한다. 각 항목은 *무엇을 못 잡는가 · 왜 안 했나 · 풀리는 조건*을 적는다.

### 코드로 메울 수 있는 것

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| RepositoryImpl 8건 (Room·Watcher·Auth·DeviceIdentity·Intro·MyPage·Profile·Signal) | 매핑은 덮었지만 **impl 의 조립·예외 변환**은 안 덮였다 | 위험이 큰 축(Challenge 에러 번역·Explore)부터 먼저 했다 | 이어서 진행 |
| `ChallengeDetailViewModel` 의 나머지 전이 | 방 탭·이의·감시자·권한 경로 | 1005줄에 협력자 11종 — 가입 경로만 덮었다. 한 파일에 다 넣으면 무엇이 깨졌는지 읽기 어려워진다 | 경로별로 나눠 진행 |
| 화면 3건 (ChallengeTargets·Splash·VerificationLocation) | 상태별 렌더 | 순수 함수(`filterApps`·`updateMessage`)는 덮었고, 나머지는 Context·런처가 얽혀 화면 분리가 선행한다 | 화면 분리 합의 |
| `TokenAuthenticator` 401 갱신 | 자동 로그아웃 분기가 어긋나면 전 사용자가 튕긴다 | `core:network` 에 테스트 소스셋이 없다 | 테스트 의존성 선언 |

### 판단이 필요한 것 — 코드로는 못 정한다

| 무엇 | 상황 | 필요한 판단 |
|---|---|---|
| 관심 단계 문구 | Figma `1134:1725` 는 `어떤 습관에 관심 있나요?`, 코드는 `어떤 챌린지에 관심 있나요?` | 기획 |
| 로그인 화면 문구 | Figma `1134:1670` 은 `카카오로 계속하기`, 코드는 `카카오로 시작하기` | 기획 |
| 참여 형태 기본값 | 모르는 값을 설정 매퍼는 `SOLO`, 상세 매퍼는 `GROUP` 으로 접는다 — 같은 미지 값이 화면마다 다르게 보인다 | 정책 |
| 대상 앱 줄임말 검색 | 부분 일치라 `카톡` 으로 `카카오톡` 을 못 찾는다. 못 찾으면 등록을 포기하고 자동 인증이 성립하지 않는다 | 기획 |
| 활동 캘린더 오류 표시 | ViewModel 은 `errorMessage` 를 채우는데 화면에 그리는 자리가 없다 — 조회 실패와 "기록 없는 달"이 같아 보인다 | 디자인 |
| `ChallengeSettingsViewModel.setCapacity` | ViewModel 에서 clamp 한다 — `CLAUDE.md` 의 "범위는 domain 타입, 입력 차단은 화면" 과 어긋난다 | 설계 |

### 환경이 필요한 것

| 무엇 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|
| 런타임 권한 다이얼로그·지오펜스 | Robolectric 이 못 흉내낸다 | 에뮬레이터 CI 워크플로 |
| 계측 테스트 8건이 CI 밖 | `test.yml` 이 `./gradlew test` 만 돈다 | 위와 같은 워크플로 |
| **인수 테스트 실행 확인** | 기반은 세웠고 4건이 있으나 `DEV_TOKEN_SECRET` 이 없어 **실서버에 붙여 돌려본 적이 없다** | 시크릿을 가진 사람이 1회 실행 |

## 4. 인수 시나리오 ↔ 하위 테스트

인수가 깨졌는데 하위 층이 전부 초록이었다면 **하위 층에 구멍이 있다**는 뜻이고, 그게 다음 작업이다.

| 스토리 | 상태 | 미리 잡아주는 하위 테스트 |
|---|---|---|
| 로그인 → 첫 화면 진입 | 하위만 | `SplashViewModelTest`(진입 순서·딥링크) · `LoginViewModelTest`(결과 4갈래) · `AuthResponseMappingTest` |
| 탐색 목록·인기 조회 | **인수 있음** | `ExploreAcceptanceTest` · `ExploreListViewModelTest` · `ExploreResponseMappingTest` |
| 챌린지 생성 → 내 목록에 보임 | 하위만 | `CreateChallengeCommandTest` · `CreateChallengeViewModelTest` · `HomeChallengeMergeTest` |
| 초대 링크로 참여 → 방 진입 | 하위만 | `ChallengeDetailJoinTest` · `ChallengeRepositoryErrorMappingTest` · `NavRouteUriParserTest`(**CI 밖**) |
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

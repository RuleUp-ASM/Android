# RuleUp 테스트 전략

마지막 갱신: 2026-09-01 · 현황 표는 `.claude/skills/testing/scripts/coverage_map.py` 출력

이 문서의 중심은 커버리지 숫자가 아니라 **미검증 목록**이다. 숫자만 있는 문서는 늘어나는 걸 보며
안심하게 만들 뿐, 다음에 뭘 해야 하는지 말해주지 않는다.

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

앞의 네 층은 전부 JVM 에서 돌아 CI(`test.yml`)가 그대로 커버한다. 인수만 밖에 있다.

**표가 말하지 않는 것 하나** — `:app` 의 18건 중 8건은 `src/androidTest` 라 CI(`./gradlew test`)가
돌리지 않는다. 딥링크 파서 테스트가 여기 있어서, 외부 진입점 회귀가 초록불로 지나간다.

## 3. 미검증 — 알면서 안 하고 있는 것

품질 논의는 이 절에서 한다. 채운 항목은 지우지 말고 4절로 옮긴다 — 무엇을 언제 메웠는지가
다음 판단의 근거다.

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| 화면 3건 (UI 층) — ChallengeTargets · Splash · VerificationLocation | 상태가 화면에 잘못 그려져도 모른다. "대기 중"이 "실패"로 보이는 종류의 오독 | 이 12건은 **상태 호이스팅된 `Content` 컴포저블이 아예 없다** — 화면을 쪼개는 리팩터링이 선행한다(가시성만 여는 것과 다른 크기의 변경이다) | 화면 분리 합의 |
| RepositoryImpl 8건 | DTO 매핑이 조용히 기본값을 넣는다. `?:` 로 접힌 값은 예외 없이 틀린 값이 된다 | 에러 번역(가장 위험한 축)만 먼저 덮었다 | 모듈별 테스트 소스셋 신설 + 응답 샘플 확보 |
| `TokenAuthenticator` 401 갱신 | 자동 로그아웃 분기가 어긋나면 전 사용자가 튕긴다 | `core:network` 에 테스트 소스셋이 없다 | 테스트 의존성 선언 |
| ChallengeDetailViewModel 의 나머지 전이 | 1005줄에 협력자 11종 — 가입 경로만 덮었고 방 탭·이의·감시자·권한 전이가 남았다 | 한 파일에 다 넣으면 무엇이 깨졌는지 읽기 어려워진다 | 경로별로 나눠 진행 |
| OnboardingViewModel | 6단계 입력 전이 | 아직 손대지 않았다 | 이어서 진행 |
| 대상 앱 줄임말 검색 | 사용자가 "카톡"으로 못 찾고 등록을 포기하면 자동 인증이 성립하지 않는다 | 부분 일치라 줄임말·초성이 안 걸린다. 매칭 방식을 바꿀지는 기획 판단이라 현재 동작만 못 박았다 | 기획 확인 |
| 활동 캘린더 오류 표시 | 조회 실패와 "기록 없는 달"이 같아 보인다 | ViewModel 은 errorMessage 를 채우는데 화면에 그리는 자리가 없다 | 디자인 확인 |
| 관심 단계 문구 | 디자인과 코드가 다른 채로 나간다 | Figma `1134:1725` 는 `어떤 습관에 관심 있나요?` / `탐색 추천에 사용해요…`, 코드는 `어떤 챌린지에 관심 있나요?` / `선택한 분야 기반으로…`. 어느 쪽이 맞는지는 기획 판단이라 테스트로 한쪽을 못 박지 않았다 | 기획 확인 |
| 런타임 권한 다이얼로그·지오펜스 | 권한 거부 후 복구 동선이 막혀도 모른다 | Robolectric 이 못 흉내낸다 | 에뮬레이터 CI 워크플로 |
| 계측 테스트가 CI 밖 | `androidTest` 8건이 한 번도 실행되지 않는다 | `test.yml` 이 `./gradlew test` 만 돈다 | 위와 같은 워크플로 |
| 인수 시나리오 4건 (참여·인증·이의·초대) | 서버가 계약을 바꾸면 배포 후에 안다 | 기반은 세웠고(탐색 4건) 나머지 스토리는 서버 상태를 만들고 되돌리는 절차가 더 필요하다 | 삭제 API 확인 후 이어서 |

## 4. 이번에 메운 것

| 무엇 | 언제 |
|---|---|
| ViewModel·Compose 실행 기반 (#373) | 2026-09-01 |
| profile ViewModel 8건 (#374) | 2026-09-01 |
| challenge ViewModel 5건 (#375) | 2026-09-01 |
| onboarding·home ViewModel 4건 · challenge:data 에러 번역 (#376) | 2026-09-01 |
| 탐색 응답 매핑 기본값 (#386) | 2026-09-02 |
| **인수 테스트 기반 + 탐색 스토리 4건** (#387) | 2026-09-02 |
| 마이 탭 화면 5건 · 로그인·권한·랭킹 3건 · 초대·편집 2건 (#390~#392) | 2026-09-02 |
| 좌표 바인딩·생성·가입 ViewModel (#393~#395) | 2026-09-02 |
| 응답 매핑 — 탐색·설정·인증·마이홈·장소·인트로·랭킹·방 (#396~) | 2026-09-02 |
| 화면 14건 (UI 층) — 온보딩 6 · 홈 · 탐색 · 목록 · 상세 · 생성 2 · 설정 (#384) | 2026-09-01 |

47건이 "안 쓴 게 아니라 못 쓰는" 상태였다 — `viewModelScope` 가 `Dispatchers.Main` 을 쓰는데
JVM 테스트엔 Main 이 없었고, Robolectric 은 버전 카탈로그에 아예 없었다.

## 5. 인수 시나리오 ↔ 하위 테스트

인수가 깨졌는데 하위 층이 전부 초록이었다면 **하위 층에 구멍이 있다**는 뜻이고, 그 구멍이 다음 작업이다.

| 스토리 | 상태 | 미리 잡아주는 하위 테스트 |
|---|---|---|
| 로그인 → 첫 화면 진입 | 미구축 | `SplashViewModelTest`(진입 순서·딥링크) · `LoginViewModelTest`(결과 4갈래) · `AutoLoginUseCaseTest` |
| 챌린지 생성 → 내 목록에 보임 | 미구축 | `CreateChallengeCommandTest`(입력 규칙) · `CreateChallengeUseCaseTest`(조립) · `HomeChallengeMergeTest`(병합) |
| 초대 링크로 참여 → 방 진입 | 미구축 | `ChallengeRepositoryErrorMappingTest`(가입 거절 사유) · `NavRouteUriParserTest`(딥링크, **CI 밖**) |
| 인증 제출 → 오늘 상태가 바뀜 | 미구축 | `RunSyncUseCaseTest` · `VerificationRepositoryImplTest` · `TodayStatusTest` |
| 인증 실패 → 이의 → 상태가 바뀜 | 미구축 | `AppealSheetTest` · `MyAppealsViewModelTest`(재시도) |

## 6. 돌리는 법

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

인수 테스트는 기본 CI 에서 빼고 수동/야간으로만 돌린다 — 실서버 상태를 바꾸므로
PR 마다 돌리면 데이터가 쌓이고 CI 가 남의 네트워크 사정에 인질이 된다.

## 갱신 규칙

테스트를 늘리거나 줄이는 PR 은 이 문서도 같이 고친다. 코드와 같은 PR 에 있어야 안 썩는다.
2절 표는 손으로 세지 말고 스크립트 출력을 붙인다. 3절은 손으로 쓴다 — **왜 안 했는지가 값이고,
그건 기계가 모른다.**

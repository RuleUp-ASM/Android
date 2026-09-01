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
| `:app` | – | – | – | 18 | – | 18 |
| `:challenge:data` | 10 | – | – | – | – | 10 |
| `:challenge:domain` | 28 | 4 | – | – | – | 32 |
| `:challenge:presentation` | 15 | 45 | – | – | – | 60 |
| `:core:datastore` | – | 13 | – | – | – | 13 |
| `:core:domain` | 17 | – | – | – | – | 17 |
| `:home:presentation` | 8 | 7 | – | – | – | 15 |
| `:observability:data` | 20 | – | – | – | – | 20 |
| `:observability:domain` | 11 | – | – | – | – | 11 |
| `:onboarding:data` | 4 | – | – | – | – | 4 |
| `:onboarding:domain` | 8 | 30 | – | – | – | 38 |
| `:onboarding:presentation` | – | 12 | 5 | – | – | 17 |
| `:profile:data` | 3 | – | – | – | – | 3 |
| `:profile:presentation` | 4 | 60 | – | – | – | 64 |
| `:verification:data` | 40 | 15 | – | – | – | 55 |
| `:verification:domain` | 22 | 11 | – | – | – | 33 |
| `:verification:presentation` | 7 | 4 | – | – | – | 11 |
| **합계** | **197** | **201** | **5** | **18** | **0** | **421** |

테스트 파일 수: 케이스 40, 모듈 28, UI 1, 통합 5, 인수 0
앞의 네 층은 전부 JVM 에서 돌아 CI(`test.yml`)가 그대로 커버한다. 인수만 밖에 있다.

**표가 말하지 않는 것 하나** — `:app` 의 18건 중 8건은 `src/androidTest` 라 CI(`./gradlew test`)가
돌리지 않는다. 딥링크 파서 테스트가 여기 있어서, 외부 진입점 회귀가 초록불로 지나간다.

## 3. 미검증 — 알면서 안 하고 있는 것

품질 논의는 이 절에서 한다. 채운 항목은 지우지 말고 4절로 옮긴다 — 무엇을 언제 메웠는지가
다음 판단의 근거다.

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| 화면 26건 (UI 층) | 상태가 화면에 잘못 그려져도 모른다. "대기 중"이 "실패"로 보이는 종류의 오독 | 대상 Composable 이 대부분 `private` 이라 가시성을 여는 프로덕션 변경이 선행한다. 기대 문구는 Figma 에서 가져와야 해 화면당 대조 비용이 든다 | 가시성 개방 합의 + 화면별 Figma 프레임 확인 |
| RepositoryImpl 10건 | DTO 매핑이 조용히 기본값을 넣는다. `?:` 로 접힌 값은 예외 없이 틀린 값이 된다 | 에러 번역(가장 위험한 축)만 먼저 덮었다 | 모듈별 테스트 소스셋 신설 + 응답 샘플 확보 |
| `TokenAuthenticator` 401 갱신 | 자동 로그아웃 분기가 어긋나면 전 사용자가 튕긴다 | `core:network` 에 테스트 소스셋이 없다 | 테스트 의존성 선언 |
| ViewModel 4건 (CreateChallenge·ChallengeDetail·Onboarding·VerificationLocation) | 생성·상세는 각각 600·1000줄로 앱에서 가장 복잡한 전이를 담는다 | 크기 때문에 별도 작업 단위로 뺐다 | 이어서 진행 |
| 런타임 권한 다이얼로그·지오펜스 | 권한 거부 후 복구 동선이 막혀도 모른다 | Robolectric 이 못 흉내낸다 | 에뮬레이터 CI 워크플로 |
| 계측 테스트가 CI 밖 | `androidTest` 8건이 한 번도 실행되지 않는다 | `test.yml` 이 `./gradlew test` 만 돈다 | 위와 같은 워크플로 |
| 인수 테스트 0건 | 서버가 계약을 바꾸면 배포 후에 안다 | 로그인 진입점이 OAuth 뿐이라 자동화가 동의 화면을 통과할 수 없다 | **BE 의 테스트 전용 토큰 발급 경로** |

## 4. 이번에 메운 것

| 무엇 | 언제 |
|---|---|
| ViewModel·Compose 실행 기반 (#373) | 2026-09-01 |
| profile ViewModel 8건 (#374) | 2026-09-01 |
| challenge ViewModel 5건 (#375) | 2026-09-01 |
| onboarding·home ViewModel 4건 · challenge:data 에러 번역 (#376) | 2026-09-01 |

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

인수 테스트가 생기면(3절 참고) 기본 CI 에서 빼고 수동/야간으로만 돌린다 — 실서버 상태를 바꾸므로
PR 마다 돌리면 데이터가 쌓이고 CI 가 남의 네트워크 사정에 인질이 된다.

## 갱신 규칙

테스트를 늘리거나 줄이는 PR 은 이 문서도 같이 고친다. 코드와 같은 PR 에 있어야 안 썩는다.
2절 표는 손으로 세지 말고 스크립트 출력을 붙인다. 3절은 손으로 쓴다 — **왜 안 했는지가 값이고,
그건 기계가 모른다.**

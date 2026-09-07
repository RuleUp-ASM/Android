# RuleUp 테스트 전략

마지막 갱신: 2026-08-31 (커버리지 표는 `coverage_map.py` 출력)

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

`python3 .claude/skills/testing/scripts/coverage_map.py` 출력 (이 PR 이 더한 `ChallengeDetailScreenTest` 20개 포함).

| 모듈 | 케이스 | 모듈 | UI | 통합 | 인수 | 합계 |
|---|---|---|---|---|---|---|
| `:app` | – | – | – | 18 | – | 18 |
| `:challenge:domain` | 28 | 4 | – | – | – | 32 |
| `:challenge:presentation` | 15 | – | 20 | – | – | 35 |
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
| **합계** | **179** | **73** | **20** | **18** | **0** | **290** |

테스트 파일 수: 케이스 38, 모듈 11, UI 1, 통합 5, 인수 0

UI 층은 이 PR 에서 처음 생겼다. 실행 기반은 `:challenge:presentation` 에만 세워져 있으니
(`robolectric.properties` · `testOptions.unitTests.isIncludeAndroidResources` · `debugImplementation(ui-test-manifest)`)
다른 presentation 모듈에 UI 테스트를 쓰려면 그 모듈에도 같은 세 가지를 옮겨야 한다.

## 3. 미검증 — 알면서 안 하고 있는 것

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| 상세 CTA 의 다음 단계 결정 (`DetailSetupAction`) | 권한·앱 등록·앵커를 건너뛰고 바로 가입시켜도 모른다 | 계산이 `hiltViewModel()` 을 꺼내는 `ChallengeDetailScreen` 안에 있어 상태만으로 못 띄운다 | 계산을 순수 함수로 꺼내면 케이스 층 한 줄 |
| 가입 차단 시트 사유 8종 문구 | 탈퇴/강퇴를 구분하는 문구가 새거나, 모르는 사유가 빈 시트로 뜬다 | `when` 이 private Composable 안에 있고 시트는 `ModalBottomSheet` 라 이 층에서 열기 비싸다 | 문구 대응표를 순수 함수로 분리 |
| 멤버 확인 다이얼로그(탈퇴·삭제) | 확인 없이 탈퇴가 나가도 모른다 | 다이얼로그를 여는 조작이 방 멤버 섹션 깊숙이 있어 UI 층에서 도달 비용이 크다 | 멤버 섹션 자체의 UI 테스트를 세울 때 함께 |
| 권한 바텀시트 (런타임/사용정보/헬스 3분기) | 걸음 권한이 필요한 사용자를 사용정보 설정으로 보내도 모른다 | 시트가 ViewModel 결합부에 있고 실제 권한 요청은 Robolectric 밖이다 | 시트를 상태 호이스팅해 분리 |
| 런타임 권한 다이얼로그·지오펜스·WorkManager | 권한 거부 후 복구 동선이 막혀도 모른다 | Robolectric 이 못 흉내낸다 | 에뮬레이터 CI 워크플로 (`VERIFICATION_TEST_PLAN.md` 수동 절차로 대신) |
| `ChallengeDetailViewModel` (모듈 층) | 의도 → 상태 전이·에러 흡수가 깨져도 모른다 | ViewModel 층 실행 기반(`coroutines-test`)이 이 모듈에 아직 없다 | `references/viewmodel.md` 대로 기반 신설 |
| 인수(실서버) 테스트 | 사용자 스토리를 하나도 관통하지 못한다 | 로그인이 OAuth 뿐이라 토큰을 못 만든다 | BE 테스트 토큰 발급 경로 |

> 이 표는 이번 작업에서 **직접 만난 구멍만** 적었다. `coverage_map.py --gaps` 가 뽑는
> "테스트 없는 대상" 전체를 메울 것/안 메울 것으로 가르는 일은 아직 안 했다 — 전부 옮기면
> 이 절이 할 일 목록이 되어 아무도 안 본다.

## 4. 인수 시나리오 ↔ 하위 테스트

| 스토리 | 상태 | 미리 잡아주는 하위 테스트 |
|---|---|---|
| 챌린지 생성 → 내 목록에 보임 | 미구축 | `CreateChallengeCommandTest`(입력 규칙), `CreateChallengeUseCaseTest`(조립) |
| 상세 진입 → 참여 → 방 상세로 전환 | 미구축 | `ChallengeDetailScreenTest`(멤버면 참여 버튼을 다시 안 보인다·차단 사유 표시) |
| 방에서 오늘 판정을 확인 | 미구축 | `ChallengeDetailScreenTest`(판정 모달 노출 조건), `VerificationResultModalTest`(문구) |
| 권한이 끊긴 채 방에 머무름 → 복구 | 미구축 | `ChallengeDetailScreenTest`(권한 배너 노출·재연결 의도) |

## 5. 돌리는 법

    ./gradlew test                      # 케이스·모듈·UI·통합 (CI 와 동일)
    ./gradlew ktlintFormat              # 커밋 전
    ./gradlew :challenge:presentation:testDebugUnitTest --tests "*ChallengeDetailScreenTest*"

verification 수동 QA 시나리오는 `VERIFICATION_TEST_PLAN.md`.

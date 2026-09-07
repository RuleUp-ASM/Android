# TEST_STRATEGY.md

레포 루트의 단일 문서. **품질 지표는 "테스트가 몇 개인가"가 아니라 "무엇을 아직 못 잡는가"다** — 그래서 이 문서의 중심은 커버리지 숫자가 아니라 미검증 목록이다. 숫자만 있는 문서는 늘어나는 걸 보며 안심하게 만들 뿐, 다음에 뭘 해야 하는지 말해주지 않는다.

이미 있는 `VERIFICATION_TEST_PLAN.md`(verification 수동 QA 시나리오)는 그대로 둔다. 성격이 다르고, 여기서는 링크로만 가리킨다.

## 갱신 규칙

테스트를 늘리거나 줄이는 PR 은 이 문서도 같이 고친다. 코드와 같은 PR 에 있어야 안 썩는다.

- **현황 표는 손으로 세지 않는다.** `python3 .claude/skills/testing/scripts/coverage_map.py` 출력을 붙인다
- **미검증 목록은 손으로 쓴다.** 왜 안 했는지가 값이고, 그건 기계가 모른다
- 채운 항목은 지우지 말고 **완료로 옮긴다** — 무엇을 언제 메웠는지가 다음 판단의 근거다

## 형식

```markdown
# RuleUp 테스트 전략

마지막 갱신: YYYY-MM-DD (커버리지 표는 coverage_map.py 출력)

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

<!-- coverage_map.py 출력 -->

## 3. 미검증 — 알면서 안 하고 있는 것

품질 논의는 이 절에서 한다. 각 항목은 **무엇을 못 잡는가 · 왜 안 했는가 · 무엇이 있어야 되는가**를 적는다.

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| 런타임 권한 다이얼로그 | 권한 거부 후 복구 동선이 막혀도 모른다 | Robolectric 이 못 흉내낸다 | 에뮬레이터 CI 워크플로 |
| 인수 테스트 로그인 | 실서버 스토리를 하나도 못 돈다 | 로그인이 OAuth 뿐 | BE 테스트 토큰 발급 경로 |
| … | | | |

## 4. 인수 시나리오 ↔ 하위 테스트

인수가 깨졌는데 하위 층이 전부 초록이었다면 하위 층에 구멍이 있다는 뜻이다. 그 구멍이 다음 작업이다.

| 스토리 | 상태 | 미리 잡아주는 하위 테스트 |
|---|---|---|
| 챌린지 생성 → 내 목록에 보임 | 미구축 | `CreateChallengeCommandTest`(입력 규칙), `CreateChallengeUseCaseTest`(조립) |
| … | | |

## 5. 돌리는 법

    ./gradlew test                      # 케이스·모듈·UI·통합 (CI 와 동일)
    ./gradlew ktlintFormat              # 커밋 전
    ./gradlew :<module>:testDebugUnitTest --tests "*XxxTest*"

    RULEUP_ACCEPTANCE=1 RULEUP_ACCEPTANCE_TOKEN=... \
      ./gradlew :app:testDebugUnitTest --tests "*AcceptanceTest*"   # 인수

verification 수동 QA 시나리오는 `VERIFICATION_TEST_PLAN.md`.
```

## 처음 만들 때

문서를 지어내지 마라. 3절(미검증)과 4절(인수 시나리오)은 **사실을 모아야** 채워진다.

1. `coverage_map.py` 로 2절을 채운다
2. 스크립트가 뽑은 "테스트 없는 대상"을 사용자와 함께 훑어 **메울 것 / 안 메울 것**으로 가른다. 안 메울 것만 3절로 간다 — 전부 옮기면 3절이 60줄짜리 할 일 목록이 되어 아무도 안 본다
3. 4절 스토리는 지금 앱에 실제로 있는 동선에서 뽑는다. 없는 기능을 미리 적지 않는다
4. 1절은 SKILL.md 를 그대로 옮긴다. 규칙이 두 군데서 갈라지지 않도록, 바뀌면 둘 다 고친다

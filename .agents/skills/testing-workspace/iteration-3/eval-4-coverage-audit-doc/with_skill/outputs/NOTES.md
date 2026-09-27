# NOTES

## 어떻게 구멍을 찾았나

1. `.claude/skills/testing/SKILL.md` → `references/strategy-doc.md` 를 읽고 문서 형식과 "처음 만들 때" 절차를 따랐다. `TEST_STRATEGY.md` 는 레포에 아직 없어서 신규 작성이다.
2. `python3 .claude/skills/testing/scripts/coverage_map.py` (+ `--gaps`) 로 층×모듈 격자와 58건의 "테스트 없는 대상"을 뽑았다. 2절 표는 이 출력 그대로다.
3. 스크립트가 못 보는 것을 수동으로 보완했다.
   - **모듈별 테스트 의존성**: 모든 `build.gradle.kts` 의 `testImplementation` 을 훑어, 구멍이 "안 썼다"인지 "못 쓴다"인지 갈랐다. `libs.versions.toml` 에 robolectric 항목이 없고, presentation 모듈에 `kotlinx-coroutines-test` 가 없고, `:core:network`·`:challenge:data` 에는 test 의존성 자체가 없다 → UI·ViewModel 구멍의 실제 원인은 인프라 부재다.
   - **CI 가 실제로 도는 범위**: `.github/workflows/test.yml` 이 `./gradlew test` 만 돌리는데 `:app` 18건 중 8건이 `src/androidTest` 다. 격자 숫자가 CI 커버리지를 과장한다는 걸 표 밑에 명시했다.
   - **에러 요인 열거**: `grep` 으로 도메인 예외 29종과 data 층의 HTTP 코드 처리 지점을 찾아, 429/409 를 도메인 예외로 옮기는 `ChallengeRepositoryImpl` 이 무테스트라는 걸 집어냈다(`VerificationRepositoryImpl` 은 이미 테스트가 있다).
   - **크기순 정렬**: ViewModel·RepositoryImpl 을 LOC 로 정렬해 "21개 다 없다" 대신 우선순위(ChallengeDetail 1005줄 …)를 적었다.
   - **인수 스토리**: 지어내지 않고 `app/navigation/AppRouteRegistry.kt` 의 실제 등록 라우트와 기존 테스트 파일 목록에서만 뽑았다.

## 판단한 것 — 넣은 것 / 뺀 것

- **58건 원본 목록을 2절에 통째로 붙이지 않았다.** 그대로 붙이면 120줄이 되어 3절(진짜 논의 지점)이 묻힌다. 대신 종류별 집계표 + 재생성 명령을 남겼다. `strategy-doc.md` 의 "손으로 세지 않는다"는 현황 격자를 가리키고, gap 목록은 3절 분류의 입력이므로 이 처리가 규칙과 어긋나지 않는다고 봤다.
- **58건을 전부 3절로 옮기지 않았다.** `strategy-doc.md` 가 "전부 옮기면 60줄짜리 할 일 목록이 되어 아무도 안 본다"고 못 박는다. 종류별로 묶어 9행으로 줄이고, 각 행의 "풀리는 조건"이 다음 작업 순서를 대신하게 했다.
- **"안 채우기로 한 것"을 별도 소절로 뺐다.** 얇은 위임 RepositoryImpl 3개와 `SubmitDeviceIntroUseCase` 는 스크립트가 구멍으로 세지만 테스트할 값이 없다고 판단했다(깨지면 컴파일이 먼저 깨진다). 근거는 SKILL.md 의 "컴파일러가 이미 보장하는 것"과 CLAUDE.md 의 UseCase 판정 기준.
- **케이스 층 entity 구멍 0 을 결론으로 썼다.** 숫자가 아니라 "구멍이 위층에만 있다"는 읽는 법을 적었다 — 다음에 뭘 할지 말해주는 게 문서의 목적이므로.
- **`MyAppealsScreenTest` 를 UI 층으로 세지 않았다.** 이름에 Screen 이 있지만 Robolectric 없이 순수 함수를 보는 케이스 층이다. 층은 이름이 아니라 대상으로 고른다는 SKILL 규칙을 1절 아래 한 줄로 남겨, 표의 UI=0 이 오타로 보이지 않게 했다.
- **`ExampleUnitTest`·`ExampleInstrumentedTest`(템플릿 잔재) 를 지적하되 지우자고 결론내지 않았다.** 문서 범위 밖의 코드 변경이고, 판단은 사용자 몫이다.
- **사용자 확인이 필요한 지점을 남겼다.** `strategy-doc.md` 는 "메울 것 / 안 메울 것"을 사용자와 함께 가르라고 한다. 이 실행은 대화 없이 진행했으므로 위 두 분류(특히 "안 채우기로 한 것", MockWebServer 도입 여부)는 **제안**이며 확정 전 확인이 필요하다.
- **`./gradlew` 은 실행하지 않았다**(지시 사항). 격자 숫자는 정적 스캔 결과이며, 테스트가 실제로 통과하는지는 확인하지 않았다.

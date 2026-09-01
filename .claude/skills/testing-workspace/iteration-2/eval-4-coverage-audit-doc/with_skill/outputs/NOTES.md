# NOTES

## 구멍을 어떻게 찾았나

1. `.claude/skills/testing/SKILL.md` → `references/strategy-doc.md` 를 읽고 문서 형식과 갱신 규칙을 확보했다.
2. `python3 .claude/skills/testing/scripts/coverage_map.py` 를 레포 루트에서 돌렸다(읽기 전용). 층×모듈 격자와 "테스트 없는 대상" 58건이 여기서 나왔다.
3. 스크립트가 못 보는 것을 손으로 확인했다:
   - `find */src/test/**/*.kt` 로 실제 테스트 파일 71개(워크스페이스 산출물 제외)를 훑어 각 모듈이 무엇을 보고 있는지 확인.
   - 모든 `build.gradle.kts` 의 `testImplementation` 을 grep → **Robolectric 부재**, `kotlinx-coroutines-test` 가 3개 모듈에만 있음, 테스트 소스셋이 없는 모듈 목록을 얻었다. UI·ViewModel 층이 "안 짠 것"이 아니라 "실행 기반이 없는 것"이라는 판단의 근거.
   - `find app -path "*/src/androidTest/*"` → `NavRouteUriParserTest`(7건)가 계측 테스트라는 걸 발견. `.github/workflows/test.yml` 이 `./gradlew test` 만 돌리는 걸 확인해 **CI 에서 안 도는 8건**을 특정했다. 스크립트는 androidTest 를 격자에 합산하므로 `:app` 18 이 실제보다 부풀어 보인다.
   - `core/network`·`app/src/main` 파일 목록, `NO_AUTH_PATHS` 위치, `VerificationSyncWorker` 존재를 grep 으로 확인해 3절의 NET-1·WORK-1·PUSH-1 을 사실 위에 올렸다.
   - `AppRouteRegistry.kt` 의 실제 등록 라우트와 각 feature 의 UseCase·Repository 목록으로 4절 스토리를 뽑았다. 앱에 없는 기능은 적지 않았다.

## 판단한 것 — 넣은 것과 뺀 것

- **58건 전체 목록을 문서에 붙이지 않았다.** strategy-doc 은 "스크립트 출력을 붙인다"고 하지만, 경로까지 붙이면 130줄이 되어 3절(미검증)이 파묻힌다. 격자 표는 출력 그대로 붙이고, 구멍은 **모듈×성격 집계표 + `--gaps` 명령**으로 대체했다. 숫자는 같은 실행에서 나왔고 재생성은 한 줄이다.
- **화면 26 + ViewModel 21 을 개별 항목으로 쪼개지 않았다.** 전부 같은 원인(실행 기반 부재)이라 47개 할 일이 아니라 결정 두 개(UI-1·VM-1)다. strategy-doc 이 경고한 "60줄짜리 할 일 목록" 을 피하려는 선택.
- **"다음에 메울 것"(2-1) 절을 형식에 없는데 추가했다.** 사용자의 요청이 "어디가 비어 있는지"였고, 3절은 정의상 *안 메울 것*만 담는다. 그래서 지금 인프라로 바로 메울 수 있는 5건을 따로 세웠다. 선정 기준은 **이미 있는 실행 기반으로 쓸 수 있는가** — `SubmitDeviceIntroUseCase`(같은 모듈에 Fake·runTest 있음), `:challenge:data` 매핑, DTO 왕복, `:profile:domain` entity.
- **`ExampleUnitTest`(2+2=4)와 계측 테스트 8건을 격자에서 지우지 않고 각주로 남겼다.** 표를 손으로 고치면 다음 스크립트 실행과 어긋난다. 대신 "표가 말하지 않는 것"에서 실질 수치를 밝혔다.
- **인수 스토리를 8개로 제한했다.** `AppRouteRegistry` 에 실제로 등록된 화면에서만 뽑았고, 웹 범위인 비유저 감시자 같은 미구현 동선은 넣지 않았다.
- **3절에 넣지 않은 것**: data class `copy`·`equals`, Hilt 배선, Composable 색·간격, 앱이 안 읽는 응답 필드 — SKILL.md 의 "테스트하지 않는 것"에 해당해 미검증이 아니라 비대상이다. 문서에는 한 줄로만 밝혔다.
- **추측으로 채우지 않은 곳**: FCM 페이로드 계약은 BE 와 미확정이라 "무엇을 검증한다"를 쓰지 않고 풀리는 조건으로만 남겼다. 인수 테스트 실행 명령도 아직 없는 인프라라 5절에 지어내지 않았다.

## 확인

레포 파일은 하나도 고치지 않았다. 실행한 것은 읽기(`cat`/`grep`/`find`)와 `coverage_map.py`(출력만) 뿐이고, gradle 은 돌리지 않았다. 산출물은 이 디렉터리의 `TEST_STRATEGY.md`(레포 루트에 놓일 자리)와 이 파일뿐이다.

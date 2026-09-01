# NOTES.md

## 어떻게 공백을 찾았나

커버리지 도구(JaCoCo/Kover)가 레포에 없어서 측정값을 얻을 수 없었다. 그래서 **파일·라인 수 대조 + 구조적 추론**으로 대신했다. 순서는 이랬다.

1. **모듈 목록을 `settings.gradle.kts` 에서 확정**하고, 모듈마다 `src/main` · `src/test` · `src/androidTest` 의 `.kt` 파일 수와 총 라인 수를 세어 대조표를 만들었다. 이게 §1 표다. 여기서 "0" 인 칸이 1차 후보다.
2. **프로덕션 코드를 종류별로 열거**했다 — `*ViewModel.kt`(21개), `*UseCase.kt`(10개), `*RepositoryImpl.kt`(12개), domain entity 전체, data 모듈의 DTO/매퍼 전체. 그리고 기존 테스트 파일 58개 목록과 이름으로 대조해 "대응 테스트가 있는가"를 항목별로 확인했다.
3. **`build.gradle.kts` 22개의 test 의존성을 전수 조회**했다. 여기서 가장 중요한 발견이 나왔다 — 6개 모듈은 `testImplementation` 블록 자체가 없다. 즉 "테스트를 안 쓴 것"이 아니라 **쓸 수 없는 상태**다. 이걸 별도 장(§3)으로 뺀 이유다.
4. **CI 워크플로 3개를 읽어** 무엇이 실제로 도는지 확인했다. `test.yml` 이 `./gradlew test` 만 돌린다 → `androidTest` 의 `NavRouteUriParserTest` 는 CI 에서 한 번도 실행된 적이 없다. 파일 목록만 봤으면 "딥링크 테스트 있음"으로 잘못 셌을 항목이다.
5. **위험도 판정을 위해 후보 코드를 실제로 읽었다** — `TokenAuthenticator`, `NetworkModule` 의 auth 인터셉터, `BaseResponse`/`ApiException`, `SplashViewModel`, `MviViewModel`, `Challenge.kt` 의 `init` 블록, `ChallengeRepositoryImpl` 의 `JoinBlockedException` 번역. 파일 크기가 아니라 **분기 수와 실패 시 피해**로 P0 를 골랐다.
6. **기존 테스트의 이름과 내용을 표본 조사**해(`AppealSheetTest`, `CreateChallengeCommandTest`, `MyAppealsScreenTest`, `CalendarResponseTest`) 이 레포의 층 구분·명명·페이크 관행을 귀납했다. §2 는 새로 발명한 규칙이 아니라 이미 하고 있는 것을 적은 것이다.
7. **`grep` 으로 위험 패턴을 정량화**했다 — `getOrThrow()`/`throwOnError()` 67곳, `challenge:data` 의 매핑 함수 53개·`requireField` 25회, `fromValue` 정의 위치. 우선순위 근거로 쓸 수 있는 숫자만 문서에 남겼다.

## 무엇을 넣고 무엇을 뺐나 — 판단

**넣은 것**

- **"테스트가 없다"보다 "테스트를 쓸 수 없다"를 먼저 배치했다.** §3 을 §4 보다 앞에 둔 건 의도적이다. P0 항목 대부분이 gradle 파일 수정 없이는 착수 자체가 불가능해서, 우선순위 목록만 주면 실행이 첫 항목에서 막힌다.
- **`androidTest` 가 CI 에서 안 돈다는 사실.** 커버리지 감사의 결과물이라기보다 CI 설정 문제지만, "있다고 세면 안 되는 테스트"라서 공백 목록에 포함하는 게 정직하다고 봤다.
- **테스트 이름 제안을 한국어 문장으로 구체적으로.** "TokenAuthenticator 테스트 필요" 수준이면 다음 사람이 어느 분기를 덮어야 하는지 다시 코드를 읽어야 한다. 실제 분기를 읽고 경로별로 이름을 뽑았다.
- **`ExampleUnitTest` 같은 템플릿 잔존물.** 사소하지만 초록불의 신뢰도를 깎는다.

**뺀 것 / 의도적으로 낮춘 것**

- **`core:designsystem` 990 LOC 를 P0 로 올리지 않았다.** 라인 수만 보면 큰 공백이지만 스냅샷 도구 없이 Compose 컴포넌트를 검증하면 신호 대비 유지 비용이 나쁘다. "의도적으로 비워 둔다"고 **명시**했다 — 다음 감사에서 다시 공백으로 잡히지 않게.
- **`observability:debug`(128 LOC) 는 거의 다루지 않았다.** `debugImplementation` 이라 릴리스 APK 에 없다.
- **커버리지 퍼센트 목표를 정하지 않았다.** 측정 도구가 없어 검증할 수 없는 숫자이고, 임의로 "80%" 같은 목표를 적으면 §4 의 우선순위와 충돌한다(줄 수가 많은 designsystem 을 채우는 게 더 이득처럼 보이게 된다).
- **ViewModel 21개를 전부 나열하지 않았다.** 크기순 상위 3개만 P0 로 지목하고 나머지는 "화면 작업이 들어올 때 그 화면부터"로 넘겼다. 21개 전부를 백로그로 만들면 아무도 시작하지 않는다.
- **테스트 도구 도입(Robolectric/mockk/Kover)을 결정하지 않고 "별도 안건"으로 남겼다.** CLAUDE.md 의 "추측 금지" 원칙에 따라, 라이브러리 추가는 팀 결정 사항이라 감사 문서가 단독으로 확정할 일이 아니라고 봤다. 다만 각 선택지가 어느 공백을 푸는지는 적어 뒀다.
- **`VERIFICATION_TEST_PLAN.md` 의 수동 QA 영역은 다루지 않았다.** 문서 첫 줄에 범위를 명시해 중복·충돌을 피했다.

## 검증하지 못한 것

- `./gradlew test` 실행이 금지되어 있어 **현재 테스트가 전부 통과하는지는 확인하지 못했다.** §1 의 숫자는 파일·라인 수이지 실행 결과가 아니다.
- 라인 수는 주석·import·빈 줄을 포함한 원시 카운트다. 모듈 간 상대 비교용으로는 충분하지만 절대 커버리지 지표는 아니다.
- `NavRouteUriParser` 가 `android.net.Uri` 에 실제로 묶여 있는지는 파일을 열어 확인하지 않고, `androidTest` 에 놓인 위치로 추정했다. 문서에도 "묶여 있으면"이라는 조건부로 적었다.

# NOTES.md

## 어떻게 공백을 찾았나

빌드/테스트 실행은 금지였으므로 전부 정적 분석으로 했다. 순서는 다음과 같다.

1. **모듈 지도 확보** — `settings.gradle.kts` 의 `include` 22개를 기준선으로 삼았다. 디렉터리를 훑는 대신 빌드가 인정하는 모듈만 셌다.
2. **밀도 측정** — 모듈마다 `src/main` · `src/test` · `src/androidTest` · `src/testFixtures` 의 `.kt` 파일 수를 세어 표를 만들었다. 파일 수는 커버리지가 아니지만, **0 인 칸**은 확실한 신호다.
3. **테스트 인프라 유무** — 각 모듈 `build.gradle.kts` 에서 `testImplementation` 계열을 grep 했다. 여기서 두 종류의 공백이 갈렸다:
   - 의존성 선언조차 없음 (`:core:network`, `:core:ui`, `:core:designsystem`, `:challenge:data`, `:onboarding:presentation`, `:home:presentation`) → 테스트를 쓰려면 빌드 파일부터 고쳐야 함
   - 선언은 있는데 테스트가 0 (`:profile:domain`, `:observability:debug`) → 의도적으로 비운 것인지 잊은 것인지 구분 필요
4. **프로덕션 표면 목록화** — `*ViewModel.kt` / `*UseCase.kt` / `*RepositoryImpl.kt` / `entity/*` / `dto/*` 를 각각 find 로 뽑아 "있어야 할 테스트 대상" 목록을 만들었다.
5. **기존 테스트의 실제 커버 내용 확인** — 모든 테스트 파일에서 `` fun `...` `` 이름을 전부 추출해 한 번에 읽었다. 파일 이름만으로는 `MyAppealsScreenTest` 가 Compose 테스트인지 순수 함수 테스트인지 알 수 없는데, 메서드 이름과 import 를 보면 바로 갈린다. 이 단계에서 "presentation 테스트 4건은 전부 copy/포매터 함수이고 ViewModel 테스트는 0건"이라는 핵심 사실이 나왔다.
6. **위험 지점 정독** — 표면 목록 중 실패 반경이 큰 것만 실제로 읽었다: `TokenAuthenticator`, `NetworkModule`, `BaseResponse`, `MviViewModel`, `NavigationHelperImpl`, `NavRouteUriParser`, `RuleUpMessagingService`, `ChallengeRepositoryImpl`. 이 파일들의 KDoc 이 이미 "이걸 틀리면 이런 일이 난다"를 적어 두고 있어서, 테스트 케이스 목록은 사실상 그 주석을 옮긴 것이다.
7. **분기 밀도 측정** — `e.code ==` / `when (e.code)` / `CODE_* =` 를 data 모듈 전체에서 grep 해 에러 코드 분기 수를, `fun .*toDomain|toRequest|toEntity` 로 매퍼 수를 셌다. `:challenge:data` 가 11종 분기 · 48개 매퍼에 테스트 0 이라는 게 여기서 나왔다.
8. **파이프라인 확인** — `.github/workflows/test.yml` 을 읽어 `./gradlew test` 만 돈다는 것, 즉 `androidTest` 의 `NavRouteUriParserTest` 가 CI 에서 한 번도 실행되지 않는다는 것을 확인했다. 버전 카탈로그(`gradle/libs.versions.toml`)를 grep 해 MockK·Robolectric·Turbine·JaCoCo/Kover 가 전부 없다는 것도 확인했다.

## 판단: 무엇을 넣고 무엇을 뺐나

**넣은 것**

- **"테스트 의존성 선언이 없는 모듈" 을 별도 항목으로 분리했다.** 단순히 "테스트 0" 이라고 쓰면 착수 비용이 다르다는 사실이 묻힌다. 이게 실행 계획 1번이 된 이유다.
- **파이프라인 공백(§4)을 개별 테스트 공백과 같은 무게로 다뤘다.** 특히 "androidTest 가 CI 에서 안 돈다"는 어떤 커버리지 표에도 안 나타나지만, 딥링크 파서가 무보호라는 결론을 바꾼다.
- **이미 잘 덮인 영역을 명시했다.** 감사 문서가 공백만 나열하면 verification·observability·인증 UseCase 처럼 잘 돼 있는 곳까지 다시 손대게 된다.
- **기존 컨벤션을 §1 에 먼저 못 박았다.** 테스트를 새로 쓸 사람이 MockK 를 들여오는 게 이 레포에서 가장 흔한 사고 경로라서, "fake 로 한다 / kotlin.test 다 / 이름은 한국어다" 를 앞에 뒀다.

**뺀 것**

- **커버리지 목표 %.** 측정 도구가 없어서 숫자를 쓰면 근거 없는 수치가 된다. 대신 P0~P3 목록 자체를 목표로 뒀고, Kover 도입은 "숫자로 관리할 생각이면" 이라는 조건부로만 적었다.
- **`:core:designsystem` · `:observability:debug` 를 공백으로 세지 않았다.** 전자는 Compose 컴포넌트 모음이고 후자는 debug 변형 전용(릴리스에 없음)이라, 테스트를 요구하면 비용만 든다. "허용" 으로 표시하고 넘어갔다.
- **모든 ViewModel 21개에 대한 개별 케이스 나열.** 목록은 전량 실었지만 케이스는 먼저 덮을 4개만 적었다. 21개 × 케이스를 다 쓰면 문서가 아니라 백로그가 되고, 실제로는 2·3번 작업을 하면서 기준이 바뀐다.
- **Compose UI 테스트 도입 제안.** 이 레포는 "Composable 파일 안의 순수 함수를 부른다" 는 방식으로 화면 로직을 이미 싸게 덮고 있다. 도구를 새로 들이자고 쓰는 대신 그 방식을 기본값으로 명문화하는 쪽을 택했다.
- **개별 DTO 필드 단위 공백.** DTO 는 11+8+... 개라 파일별로 나열하면 신호가 죽는다. "모듈 단위로 라운드트립 테스트 한 파일" (verification 선례) 로 묶어 제안했다.
- **파일별 라인 커버리지 추정.** 실행 없이 추정하면 틀린 정밀도를 주므로 하지 않았다. 표의 수치는 전부 "파일 수" 라고 명시했다.

## 확인하지 못한 것 / 가정

- 테스트 파일 수에 fake·fixture 파일이 섞여 있다 (`FakeChallengeRepository.kt`, `Fakes.kt`, `Users.kt`). 표에 주석으로 밝혀 뒀다.
- 실제로 `./gradlew test` 가 모두 통과하는지는 실행 금지라 확인하지 못했다. 문서는 "현재 테스트는 통과한다"를 전제하지 않는다.
- `:home:presentation` 은 파일 4개뿐이라 내부를 정독하지 않았다. 공백으로만 표시했다.
- 리포지토리 규칙상 실제 파일은 하나도 수정하지 않았다. 두 문서 모두 지정된 outputs 디렉터리에만 썼다.

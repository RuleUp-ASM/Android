# 통합 층

모듈 경계를 건너는 결합만 본다. 한 모듈 안에서 끝나는 건 모듈 층이다.

이 레포에서 경계를 건너다 실제로 깨지는 건 네 종류다. 공통점은 **각 모듈은 혼자서 다 옳은데 합쳐 놓으면 안 맞는다**는 것 — 그래서 아래층 테스트를 아무리 늘려도 안 잡힌다.

## 1. 네비게이션 — Page ↔ AppRoutes ↔ 레지스트리

경로 문자열이 세 곳(`AppRoutes` 상수 · feature 의 `Page` 구현 · `:app` 의 `appRoutes` 리스트)을 거치므로 하나를 빠뜨리면 런타임에 아무 데도 안 가는 화면이 된다. 이미 `AppRouteAccessPolicyTest` 가 접근 정책을 잡고 있으니 그 옆에 나란히 둔다.

볼 것:

- **모든 `Page` 구현의 목적지가 레지스트리에 있다** — Konsist 로 `Page` 하위 타입을 훑어 각 `route.path` 가 `appRoutes` 의 path 집합에 들어 있는지. 새 화면을 만들고 등록을 잊는 사고를 이게 잡는다
- **레지스트리에 중복 path 가 없다**
- **`AppRoutes` 상수 중 아무도 안 쓰는 게 없다** — 지운 화면의 상수가 남아 다음 사람을 헷갈리게 한다
- **`isBottomTab`/`isRoot`/`syntheticStack` 조합의 불변식** — 루트가 아닌데 bottom tab 이라거나 하는 모순
- **딥링크 왕복** — `NavRoute` → URI → `NavRoute` 가 같은 값으로 돌아온다. `android.net.Uri` 를 쓰는 파서라 계측 쪽에 이미 있다(`app/src/androidTest/.../NavRouteUriParserTest`). 파서를 건드리면 거기에 이어 쓴다

## 2. 직렬화 왕복

서버 계약을 실서버 없이 붙잡아 두는 층이다. `VerificationDtoSerializationTest` 가 기준 형태.

- **요청 DTO** → JSON 문자열. 서버가 기대하는 키 이름·형식이 나오는가. `@SerialName` 오타는 이걸로만 잡힌다
- **응답 JSON** → DTO → entity. 실제 서버 응답 샘플을 문자열 그대로 박아 넣는다. 손으로 만든 DTO 인스턴스에서 시작하면 파싱을 안 거쳐서 계약을 못 본다
- **없는 필드 · 모르는 필드** — `ignoreUnknownKeys` 가 켜져 있어도 "서버가 필드를 더 보내도 안 깨진다"를 명시적으로 고정해 두면 나중에 설정이 바뀔 때 걸린다
- **Room 컨버터** — enum ↔ 문자열 왕복(`VerificationTypeConvertersTest`). DB 에 남은 옛 값이 새 enum 에 없을 때 어떻게 되는지가 특히 중요하다

응답 샘플은 실제 것을 쓴다. 지어낸 JSON 으로 테스트를 초록으로 만들면 초록불이 계약을 보증하지 않는다.

## 3. 아키텍처 규칙

`ArchitectureTest`(Konsist)가 이미 domain 순수성·레이어 방향·구현체 위치를 강제한다. 새 규칙을 문서로 적고 싶어지면 **여기 테스트로 적어라** — 문서는 안 지켜져도 조용하지만 테스트는 깨진다.

테스트로 옮길 값이 있는 규칙의 예:

- feature 간 의존은 `domain` 까지만 (`CLAUDE.md` 의 규칙인데 아직 테스트가 없다)
- `CompositionLocal` allowlist 는 ktlint 가 잡는다 — 중복해서 만들지 않는다
- `MviViewModel` 을 상속하지 않은 `*ViewModel` 이 없다
- `_uiState` 를 직접 만든 ViewModel 이 없다

규칙을 새로 추가할 땐 **지금 통과하는지 먼저 확인**한다. 이미 위반이 있으면 규칙만 넣어 CI 를 빨갛게 만들지 말고, 위반 목록을 사용자에게 보고하고 어떻게 할지 정한 다음 넣는다.

## 4. 조립 검증

`:app` 이 전 모듈을 모으는 지점에서만 드러나는 것들.

- **관측 이벤트 카탈로그** — feature 마다 `observability/<Feature>Events.kt` 가 있는데 이름이 겹치거나 규칙(prefix·snake_case)을 어기면 대시보드가 조용히 틀린다. 전 모듈 이벤트 이름을 모아 중복·형식을 단언한다
- **릴리스 빌드에 debug sink 가 없다** — `observability:debug` 는 `debugImplementation` 이라 빠지는 게 맞다. 이게 정책이면 검증할 값이 있다

Hilt 그래프 자체는 테스트하지 않는다 — 못 엮으면 빌드가 깨진다.

---

## 어디에 두는가

| 대상 | 위치 |
|---|---|
| 네비게이션·아키텍처·조립 | `app/src/test/java/com/ruleup/android_ruleup/…` (기존 `ArchitectureTest`·`AppRouteAccessPolicyTest` 옆) |
| 직렬화 왕복 | 그 DTO 가 사는 `<feature>/data/src/test/…/dto/` |
| `android.*` 타입이 필요한 것 | `app/src/androidTest/…` — 계측이라 CI 밖이라는 걸 알고 둔다 |

파일 이름은 대상 + `Test` 로 기존 관례를 따르되, 무엇을 엮는지가 이름에 없으면 `…IntegrationTest` 를 붙인다.

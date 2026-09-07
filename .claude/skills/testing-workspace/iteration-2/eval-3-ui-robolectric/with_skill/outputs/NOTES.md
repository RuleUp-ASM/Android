# ChallengeDetailScreen UI 테스트 — 작업 메모

## 1. 고른 층: UI (Robolectric + Compose)

"이 규칙이 깨지면 어느 파일을 고치나"로 골랐다.

- `PENDING 을 실패로 그린다`·`이미 멤버인데 참여 버튼이 뜬다`·`차단 사유를 설명해 버린다` → 고칠 파일은
  `ChallengeDetailScreen.kt` 다 → **UI 층**.
- 사유 enum·티어 비교 같은 값 규칙은 이미 domain(케이스 층) 소관이라 여기서 다시 세지 않는다.
- 화면 이동·API 호출은 ViewModel(모듈 층) 소관이라 **화면은 Intent 를 올렸는가까지만** 본다.
  그래서 클릭 테스트의 단언은 전부 "어떤 Intent 가 올라갔는가"이고, 그 뒤는 보지 않는다.

`challenge/presentation` 의 기존 테스트 4개는 이름만 presentation 이지 **케이스 층**이다
(`resultNote`·`appealHint` 같은 순수 함수 검증). 그래서 겹치는 게 없다 — 이 화면의 "오늘 인증 문구"
계산은 이미 그쪽이 잡고 있어 여기서 다시 검증하지 않았다.

## 2. 세운 실행 기반 (레포 첫 Compose UI 테스트)

이 레포에는 Compose UI 테스트가 하나도 없었다. `ui-test-junit4` 는 `:app` 의 `androidTest` 에만 있고
쓰는 파일도 없다. 그래서 네 가지를 새로 깔았다.

| 파일 | 무엇 |
|---|---|
| `gradle/libs.versions.toml` | `robolectric = "4.16"` 버전·라이브러리 항목 추가 (동적 버전 금지 규칙대로 고정) |
| `challenge/presentation/build.gradle.kts` | `testOptions.unitTests.isIncludeAndroidResources = true`, `testImplementation(robolectric / compose-bom / ui-test-junit4)`, **`debugImplementation(ui-test-manifest)`** |
| `challenge/presentation/src/test/resources/robolectric.properties` | `sdk=35`, `qualifiers=w411dp-h891dp-xhdpi` |
| `.../src/test/.../detail/ChallengeDetailFixtures.kt` | 상태 조립기 + 전역 클릭 가드 우회 헬퍼 |

주의점 셋:

1. **`ui-test-manifest` 는 `debugImplementation` 이어야 한다.** 단위 테스트는 debug 변형의 *병합된
   매니페스트*를 읽는데, `testImplementation` 으로 넣으면 클래스는 와도 `createComposeRule()` 이 띄울
   `ComponentActivity` 항목이 매니페스트에 없어 런타임에 터진다.
2. **`compileSdk = 37` 은 Robolectric 이 못 받는다.** `robolectric.properties` 로 모듈 전체를 SDK 35 로
   고정했다. `robolectric` 버전과 `sdk` 값은 **짝으로 움직인다** — 실제로 돌릴 때 이 판이 지원하는 최대
   SDK 를 확인하고 맞춰야 한다(gradle 을 돌리지 않는 조건이라 이 짝만 미검증으로 남는다).
3. **`SingleClickGuard` 가 첫 클릭을 삼킨다.** `object` 필드에 마지막 클릭 시각이 JVM 수명 동안 남는데
   Robolectric 은 `SystemClock.elapsedRealtime()` 을 테스트마다 되감는다 → 차이가 음수가 되어 클릭이
   전부 막힌다(하나만 돌리면 통과, 클래스를 통째로 돌리면 실패). `@Before` 에서 60초, 클릭 직전에 1초씩
   **단조 증가**시켜 넘긴다. 프로덕션에 테스트용 리셋 훅은 뚫지 않았다.

## 3. 프로덕션 변경 — 확인이 필요합니다

`ChallengeDetailContent` 를 `private` → `internal` 로 열었습니다 (`ChallengeDetailScreen.kt`).
바깥 `ChallengeDetailScreen` 은 `hiltViewModel()`·권한 런처·`LifecycleEventEffect` 를 직접 꺼내므로
상태를 넣어 렌더할 수 없고, 상태 호이스팅된 안쪽 `Content` 가 유일한 테스트 대상입니다.
동작은 그대로이고 모듈 밖으로 새지도 않지만, **테스트 편의로 프로덕션 가시성을 바꾸는 판단**이라
그대로 진행할지 확인 부탁드립니다. 다른 방법은 없습니다.

## 4. 경로를 어떻게 열거했나

타입에서 읽어냈다. 기억으로 "다 했다"고 하지 않았다.

- **화면의 `when` 분기**: `isLoading` → `detail == null` → `room != null` → 공개 상세. 네 갈래 각각 하나씩.
- **`JoinBlockReason` (enum 8종)**: `entries` 를 순회하고 기대 제목은 **`else` 없는 `when`** 으로 적었다.
  사유가 늘면 테스트가 컴파일되지 않는다 — 그게 열거를 끝냈다는 근거다.
- **nullable 필드**: `owner`(방장 없음) · `description`(빈 문자열) · `rejoinAvailableAt`(날짜 없음) ·
  `gate.minTier`/`myDisplayTier`(티어 없음) · `watchers`(403 흡수) 각각 있음/없음 두 갈래.
- **불리언 게이트**: `myRole.isMember`, `hideJoinButton`, `cloneable`, `canClone`, `isJoining` — 서로
  독립이라 각각 하나씩만. 2ⁿ 조합은 만들지 않았다.
- **가장 비싼 오독**: "권한이 꺼져 자동 인증이 멈췄다"는 배너. 자동 인증은 조용히 멈추므로 이 배너가
  없으면 사용자는 매일 실패가 쌓여 강퇴될 때까지 모른다. 보이는 경우와 **안 보이는 경우**를 함께 단언했다.

테스트 이름 목록(총 28개, 파일 2개):

**`ChallengeDetailScreenTest`** (19)
1. 불러오는 중에는 실패도 참여 버튼도 먼저 보여주지 않는다
2. 상세를 불러오지 못하면 서버가 준 이유를 그대로 보여준다
3. 이유를 모르면 빈 화면 대신 실패했다는 사실을 말한다
4. 공개 상세는 제목과 참여 인원과 참여 조건을 보여준다
5. 방장이 없는 방은 방장 이름을 지어내지 않는다
6. 설명이 없으면 설명 자리를 만들지 않는다
7. 참여 버튼을 누르면 지금 해야 할 셋업 단계로 넘어간다
8. 이미 참여 중이면 참여 버튼을 다시 보여주지 않는다
9. 초대 링크로만 들어오는 방은 참여 버튼 대신 이유를 보여준다
10. 참여를 요청하는 동안에는 다시 눌러도 또 요청하지 않는다
11. 복제할 수 없는 챌린지에는 템플릿 버튼을 두지 않는다
12. 템플릿으로 만들기를 누르면 복제 의도가 올라간다
13. 초안을 만드는 동안에는 다시 눌러도 또 만들지 않는다
14. 내 감시자를 조회하지 못했으면 감시자 자리를 만들지 않는다
15. 뒤로를 누르면 화면을 떠난다
16. 참여 중인데 권한이 꺼져 있으면 인증이 멈췄다고 알린다
17. 권한이 다 켜져 있으면 경고를 띄우지 않는다
18. 권한 경고를 누르면 권한을 다시 잇는 화면으로 보낸다
19. 방 안에서 탭을 누르면 그 탭을 열라는 의도가 올라간다

**`ChallengeDetailJoinBlockTest`** (9)
1. 가입이 막힌 모든 사유는 무엇에 막혔는지 말한다 *(entries 순회)*
2. 앱이 모르는 사유여도 빈 시트를 띄우지 않는다
3. 티어로 막혔으면 필요한 티어와 내 티어를 같이 보여준다
4. 티어를 모르면 티어를 지어내지 않는다
5. 차단된 사용자에게 차단 사유를 설명하지 않는다
6. 재입장 가능일을 알면 언제부터인지 말한다
7. 재입장 가능일을 모르면 날짜를 지어내지 않는다
8. 동시 참여 수에 막혔으면 정리할 수 있는 곳으로 보낸다
9. 안내를 닫으면 차단 안내를 내린다

## 5. 일부러 테스트하지 않은 것

| 무엇 | 왜 |
|---|---|
| **CTA 라벨 매핑**(권한 → 앱 등록 → 앵커 → 참여) | 바깥 `ChallengeDetailScreen` 안에서 계산된다. `hiltViewModel()`·런타임 권한 조회를 끼고 있어 렌더할 수 없다. 순수 함수로 뽑으면 케이스 층에서 잡히는데, 그건 가시성 변경보다 큰 프로덕션 수정이라 먼저 합의가 필요하다 |
| **권한 바텀시트**(허용하기/사용정보 설정/헬스 커넥트 분기) | 시트를 여는 `showPermissionSheet` 이 바깥 Composable 의 로컬 상태다. 위와 같은 이유로 지금 구조에선 닿지 않는다 |
| 런타임 권한 다이얼로그·설정 화면 이동 | Robolectric 이 못 흉내낸다. 계측 테스트/수동 QA(`VERIFICATION_TEST_PLAN.md`) 몫 |
| 방 피드·랭킹 탭 내부(페이징·재시도·세그먼트) | 탭을 여는 건 ViewModel 이라 화면만으로는 상태가 안 바뀐다. 별도 대상(`RoomFeedTab`/`RoomRankingTab`)의 UI 테스트로 따로 다뤄야 하고, 이번 요청 범위(상세 화면) 밖이다 |
| 판정 결과 모달의 문구 | `VerificationResultModalTest`(케이스 층)가 이미 잡는다. 층을 건너뛰어 위에서 또 세지 않는다 |
| 색·간격·타이포 | 디자인 시스템 토큰을 쓴다는 건 코드가 이미 말한다 |
| 로딩 중 + 방 홈이 동시에 참인 상태에서 권한 배너가 안 뜨는지 | `!isLoading` 가드 하나짜리라 값이 낮다고 판단해 뺐다. 필요하면 한 줄로 추가된다 |

## 6. 남은 일

- **`TEST_STRATEGY.md` 가 레포에 없다.** 스킬 절차상 테스트를 늘리면 갱신해야 하지만, 문서를 처음 만들 때는
  `coverage_map.py` 결과를 놓고 "메울 것 / 안 메울 것"을 사용자와 갈라야 한다(레퍼런스 「처음 만들 때」).
  지어낼 수 없어 이번엔 만들지 않았다. 위 5절 표가 그 문서 3절(미검증)의 초안이다.
- 실행 확인이 남았다(이번 작업은 gradle 미실행 조건):
  `./gradlew :challenge:presentation:testDebugUnitTest --tests "*ChallengeDetail*Test*"`.
  첫 실행은 `android-all` jar 를 받느라 느리다. 여기서 SDK 짝(robolectric 4.16 ↔ sdk=35)을 확정한다.
- 커밋 전 `./gradlew ktlintFormat`.

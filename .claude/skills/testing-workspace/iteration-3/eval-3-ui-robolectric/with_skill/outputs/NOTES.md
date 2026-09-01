# ChallengeDetailScreen UI 테스트 — 작업 메모

## 1. 고른 층과 이유

**UI 층 (Robolectric + Compose).**

판단 기준은 "이 규칙이 깨지면 어느 파일을 고치나"다.

- "참여 중인데 참여하기 버튼이 또 뜬다" → 고칠 파일은 `ChallengeDetailScreen.kt` → UI
- "권한이 꺼졌는데 배너가 안 뜬다" → 같은 파일 → UI
- "차단 사유가 시트 문구로 안 옮겨진다" → `JoinBlockedSheet`(같은 파일) → UI

반대로 **아래 것들은 이 층에서 안 봤다.**

- 실패 사유 문구·이의 마감일 계산 → 이미 케이스 층에 있다
  (`TodayVerificationCopyTest`, `VerificationResultModalTest`, `AppealSheetTest`).
- `joinable`·`hideJoinButton`·`canClaimOwner` 같은 파생 규칙의 **경계값** → entity·State 의 몫이다.
  UI 는 "막혔을 때 버튼이 잠기는가" 하나만 보고, 몇 명부터 정원 초과인지는 다시 세지 않는다.
- 화면 이동 → 화면은 Intent 만 올린다. 어디로 가는지는 ViewModel 층.

즉 이 파일들은 **상태 → 화면의 갈래**와 **조작 → Intent**만 본다.

## 2. 세운 실행 기반

레포에 Compose UI 테스트가 하나도 없었다(`ui-test-junit4` 는 `:app` 에만 선언돼 있고 쓰는 파일이 없음).
Robolectric 을 고른 이유는 CI 가 이미 돌리는 `./gradlew test` 안에서 돌기 때문이다 — 계측 테스트로
쓰면 에뮬레이터 워크플로를 새로 붙여야 하고, 붙여도 PR 마다 못 돌린다.

| 파일 | 변경 |
|---|---|
| `gradle/libs.versions.toml` | `robolectric = "4.16.1"` (Maven Central 최신 안정판 확인) + 라이브러리 항목 |
| `challenge/presentation/build.gradle.kts` | `testOptions.unitTests.isIncludeAndroidResources = true`, `testImplementation` robolectric·compose-bom·ui-test-junit4, **`debugImplementation`** ui-test-manifest |
| `challenge/presentation/src/test/resources/robolectric.properties` | `sdk=35`, `qualifiers=w411dp-h891dp-xhdpi` |
| `.../detail/ChallengeDetailScreen.kt` | `ChallengeDetailContent` 를 `private` → `internal` |
| `.../test/.../detail/ChallengeDetailComposeSupport.kt` | 렌더 헬퍼 + 전역 클릭 가드 우회 |

함정 세 개를 미리 막아 뒀다.

1. **ui-test-manifest 는 `debugImplementation` 이어야 한다.** `testImplementation` 으로 넣으면 클래스는
   오지만 `createComposeRule` 이 띄울 `ComponentActivity` 가 병합된 매니페스트에 없어 런타임에 터진다.
2. **`compileSdk 37` 은 Robolectric 이 못 받는다.** `android-all` jar 가 없어 다운로드에서 실패하므로
   모듈 전체를 `robolectric.properties` 로 35 에 고정했다. `qualifiers` 도 못 박아 "창이 좁아서
   `assertIsDisplayed` 가 실패"하는 축을 없앴다.
3. **`SingleClickGuard` 가 첫 클릭을 삼킨다.** `object` 라 JVM 이 사는 동안 테스트를 건너 살아남는데
   Robolectric 은 `SystemClock.elapsedRealtime()` 을 테스트마다 되감는다 → 차이가 음수가 되어
   계속 막힌다(하나만 돌리면 통과, 클래스 전체를 돌리면 실패). `@Before` 에서 그림자 시계를 60초씩
   **단조 증가**시키고, 한 테스트에서 두 번 누를 때는 `clickPastGuard()` 로 1초씩 더 민다.
   프로덕션에 테스트용 리셋 훅은 뚫지 않았다 — 릴리스 빌드에도 남는다.

### 프로덕션 변경 1건 — 확인 필요

`ChallengeDetailContent` 를 `private` → `internal` 로 열었다. 상태를 인자로 받고 Intent 를 콜백으로
올리는 순수 화면이라 테스트 대상은 이쪽이고(바깥 `ChallengeDetailScreen` 은 `hiltViewModel()` 을
직접 꺼낸다), 여는 것 말고 방법이 없다. 동작은 그대로이고 모듈 밖으로 새지도 않지만 **테스트 편의로
프로덕션 가시성을 바꾸는 판단**이라 그대로 넘기지 않고 적어 둔다.

## 3. 경로를 어떻게 열거했나

출처를 사람의 기억이 아니라 **타입과 화면 코드의 분기**에 뒀다.

- `ChallengeDetailContent` 의 최상위 `when` → 로딩 / 상세 없음 / 방 상세 / 공개 상세 **4갈래**.
  네 갈래 모두 테스트가 있다.
- `RoomTab` (enum, 3종) → 탭 전환 1건 + "정보 탭이 아닐 때 헤더를 접는다" 1건.
  각 탭 **안쪽**은 각자의 Composable 이라 여기서 다시 세지 않았다.
- 하단 CTA 의 분기 → `myRole.isMember`(숨김) · `hideJoinButton`(안내 문구) · `isJoining`(잠금) ·
  `cloneable`(복제 버튼) **4갈래** 각각 1건.
- 권한 배너 → `permissions == null`(모름) / 요구 권한이 꺼짐 / 방이 아님. 앞 둘을 봤다.
- `JoinBlockReason` (enum, 8종) + 미지 사유(null) → **`entries` 를 순회하는 테스트 하나**로
  "어떤 사유든 닫을 길이 있다"를 고정했다. 값이 늘면 이 테스트가 먼저 잡는다.
  개별 문구는 규칙이 다른 것만 따로 봤다(티어·재입장 대기·영구 차단).

열거를 끝냈다는 근거는 `JoinBlockReason.entries` 순회와, 못 다룬 것을 아래 5절에 남긴 것이다.

## 4. 기대값의 출처

Figma 「🏁 23 · 최종 · 전체 화면」 페이지 `1134:2` (파일 `JDWCrvnTlAyrtzMI2MtpcC`).
문서에 적힌 `4xx`·`6xx`·`9xx` 대 노드는 구 페이지라 쓰지 않고 이름으로 다시 찾았다.

| 쓴 프레임 | 가져온 것 |
|---|---|
| `1134:1291` 공개 상세 프리뷰 | 하단 CTA 가 **"참여하기"** 하나 |
| `1134:155` 상세·정보 Head | 탭 **정보·피드·랭킹**, 헤더에 `종료까지`·`내 달성률` |
| `1134:243` 상세·피드 Head | AppBar + Tabs 만 — **피드 탭에는 달성률 헤더가 없다** |
| `1134:143` 상세·정보 | 멤버 화면에는 하단 참여 CTA 가 없다 |
| `1134:1077` 참여 차단 시트 3종 | 버튼 **"내 티어 보기"**, 공통 **"닫기"** |

구현에서 문구를 베끼면 "오늘 이후의 변경"만 잡고 "처음부터 틀렸다"는 영영 못 잡으므로, 위 다섯 건은
전부 디자인에서 읽어 왔다.

### 디자인과 코드가 어긋나는 3건 — 임의로 정하지 않았다

테스트로 한쪽을 못 박는 순간 그게 정답이 되어 버리므로 **판정하지 않고 그대로 올린다.**

| 자리 | Figma | 코드 |
|---|---|---|
| 차단 시트 · 정원(FULL) | 제목 "정원이 다 찼어요" + 버튼 **"비슷한 챌린지 보기"** | 제목 "정원이 찼어요", **버튼 없음** |
| 차단 시트 · 무료 한도(FREE_LIMIT) | 버튼 **"내 챌린지 관리"** | 버튼 "참여 중인 챌린지 보기" |
| 비멤버 상세 상단바 | 챌린지 제목 ("평일 아침 헬스장 출석") | 고정 문구 **"챌린지"** |

티어 시트도 Figma 는 "실버 티어부터 참여할 수 있어요 / 지금은 브론즈예요"(한글)인데 코드는
"필요한 티어 SILVER · 내 티어 BRONZE"(enum 값)다. 문장을 통째로 못 박는 대신 **두 티어가 함께
보인다**는 규칙만 단언했다 — 어느 표기가 맞는지는 사용자 판정 사항이다.

### 디자인이 아예 없는 것 — 구현에서 임시 고정

권한 끊김 배너("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기")는 최종 페이지에 프레임이 없다
(`1134:997` 은 재연결 *화면*이지 방 안 배너가 아니다). 지어내지 않고 구현 문구로 임시 고정한 뒤
테스트 KDoc 에 "디자인 미확정"을 남겼다.

## 5. 일부러 안 한 것

| 무엇 | 왜 |
|---|---|
| 권한 바텀시트(`PermissionBottomSheet`) 렌더·허용 흐름 | 바깥 `ChallengeDetailScreen` 이 `hiltViewModel()`·`Context`·런타임 권한 런처를 직접 쥔다. Robolectric 이 흉내내지 못하는 영역이라 계측/수동 QA 쪽이다 |
| 탈퇴·삭제 확인 다이얼로그 | `RoomMemberSection` 안(멤버 목록 필요)에서 열린다. 멤버 섹션 자체를 별도 대상으로 잡는 게 맞다 |
| 판정 결과 모달 | `VerificationResultModalTest`(케이스 층)가 이미 문구 규칙을 지킨다. 여기서 다시 세면 곱이 된다 |
| 피드 페이징·랭킹 세그먼트 | 탭 **안쪽**이라 `RoomFeedTab`·`RoomRankingTab` 을 대상으로 하는 별개 작업 |
| 색·간격·폰트 | 디자인 시스템 토큰을 쓴다는 건 코드가 이미 말한다 |
| `TEST_STRATEGY.md` 갱신 | **문서가 아직 레포에 없다.** 처음 만들려면 `coverage_map.py` 출력과 "메울 것/안 메울 것" 합의가 필요해 지어내지 않았다. 아래 행들이 만들 때 3절(미검증)로 들어가야 한다 |

`TEST_STRATEGY.md` 3절에 넣을 행:

| 무엇 | 못 잡는 위험 | 왜 안 했나 | 풀리는 조건 |
|---|---|---|---|
| 권한 끊김 배너 문구 | 문구가 처음부터 틀려도 초록 | Figma 에 프레임 없음 | 디자인 확정 |
| 차단 시트 FULL·FREE_LIMIT 문구/버튼 | 디자인과 다른 화면이 나감 | 디자인·코드 불일치 미결 | 어느 쪽이 맞는지 확정 |
| 런타임 권한 다이얼로그(위치·사용정보·Health) | 거부 후 복구 동선이 막혀도 모름 | Robolectric 이 못 흉내냄 | 에뮬레이터 CI 워크플로 |

## 6. 만든 파일

```
gradle/libs.versions.toml                                       (수정)
challenge/presentation/build.gradle.kts                         (수정)
challenge/presentation/src/main/.../detail/ChallengeDetailScreen.kt   (가시성만 수정)
challenge/presentation/src/test/resources/robolectric.properties      (신규)
challenge/presentation/src/test/.../detail/ChallengeDetailComposeSupport.kt (신규)
challenge/presentation/src/test/.../detail/ChallengeDetailFixtures.kt      (신규)
challenge/presentation/src/test/.../detail/ChallengeDetailScreenTest.kt    (신규, 16건)
challenge/presentation/src/test/.../detail/ChallengeDetailJoinBlockTest.kt (신규, 6건)
```

돌리는 법 (이번 작업에서는 실행하지 않았다):

```bash
./gradlew :challenge:presentation:testDebugUnitTest --tests "*ChallengeDetail*Test*"
```

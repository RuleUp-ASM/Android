# 방 대기열 — challenge:domain 설계

레포는 건드리지 않고 `outputs/` 아래에 레포와 같은 경로로 놓았습니다. 그대로 옮기면 됩니다.

```
challenge/domain/src/main/kotlin/com/ruleup/challenge/domain/entity/Waitlist.kt   (신규)
challenge/domain/src/test/kotlin/com/ruleup/challenge/domain/entity/WaitlistTest.kt (신규)
```

기존 파일 3개는 **아래 diff 만큼만** 손대면 됩니다(그 diff 도 아직 적용하지 않았습니다).

---

## 1. 신규 — `entity/Waitlist.kt`

| 타입 | 역할 |
|---|---|
| `WaitlistPolicy` | 상한 = 정원 × 50%(내림). 화면과 도메인이 같은 숫자를 본다 |
| `WaitlistPosition` | `Calculating` / `Assigned(number)` — 순번의 "아직 모름"을 타입으로 가둔다 |
| `ChallengeWaitlist` | 공개 상세에 실리는 대기열 현황(`waitingCount`·`maxSize`·`myPosition`) |
| `JoinAction` | 참여 CTA 가 할 일 — `JOIN` / `JOIN_WAITLIST` / `BLOCKED` |
| `WaitlistTicket` | 대기 등록 결과 |

핵심 판단 세 가지입니다.

**① 순번은 `Int?` 가 아니라 `WaitlistPosition` 입니다.**
"계산 전이면 null" 을 `Int?` 로 들고 다니면 화면이 `?: 1`, `orEmpty()`, `maxOf(1, it)` 같은 걸로 언제든 1번을
만들어 냅니다. 그리고 그 거짓말은 **눈에 안 보입니다** — 진짜 1번과 화면이 똑같이 생겼으니까요.
`Calculating` 을 받은 화면에는 "확인 중" 말고 그릴 게 없습니다. 서버가 0 이하를 보내도 `of()` 가
`Calculating` 으로 떨어뜨립니다(계산 전과 구분할 근거가 없으므로).

**② 정원 50% 상한은 `WaitlistPolicy` 에 두되, 최종 판정은 서버입니다.**
앱 계산은 "버튼을 눌러보게 두지 않기" 위한 사전 차단이고, 서버가 `waitlist.maxSize` 를 같이 내려주므로
표시·차단은 그 값을 씁니다. 경합으로 밀리면 서버가 409 로 막습니다(아래 `WAITLIST_FULL`).

**③ 버튼 상태 계산은 `ChallengeDetail.joinAction` 한 곳입니다.**
지금도 상세 화면·탐색 카드가 `isFull`·`joinBlockReason`·`gate.eligible` 을 각자 조합하고 있어서,
여기에 대기열 조건까지 흩뿌리면 카드와 상세가 서로 다른 문구를 냅니다. 기존 `joinable`(자물쇠 표시용)은
의미가 바뀌면 안 되니 그대로 두고 `joinAction` 을 옆에 추가했습니다.

## 2. `entity/ChallengeDetail.kt` — 필드 1개 + 계산 프로퍼티 1개

```diff
     val myRole: MemberRole,
     // 방장 본인 조회에서만
     val moderation: ChallengeModeration?,
+    // 대기열이 열린 방에서만. 방이 시작돼 대기열이 비워지면 null 로 돌아온다 — 오류가 아니다.
+    val waitlist: ChallengeWaitlist?,
 ) {
     /** 참여 버튼을 활성할 수 있는지. 자격·정원·차단 사유를 모두 통과해야 한다. */
     val joinable: Boolean
         get() = joinBlockReason == null && gate.eligible && !isFull && myRole == MemberRole.NONE
+
+    /**
+     * 참여 CTA 가 실제로 할 일. **정원이 찼다고 막지 않는다** — 대기열이 열려 있으면 대기 등록으로 간다.
+     * 그래서 [JoinBlockReason.FULL] 만 예외로 빠져나가고, 티어·재입장 대기 같은 사유는 자리가 나도
+     * 못 들어가므로 그대로 막는다.
+     */
+    val joinAction: JoinAction
+        get() =
+            when {
+                myRole.isMember -> JoinAction.BLOCKED
+                joinBlockReason != null && joinBlockReason != JoinBlockReason.FULL -> JoinAction.BLOCKED
+                !gate.eligible -> JoinAction.BLOCKED
+                !isFull -> JoinAction.JOIN
+                waitlist != null && !waitlist.isFull && !waitlist.isWaiting -> JoinAction.JOIN_WAITLIST
+                else -> JoinAction.BLOCKED
+            }
 }
```

## 3. `entity/ChallengeMember.kt` — 차단 사유 2종

```diff
     FULL("FULL"),
+
+    // 정원도 대기열도 다 찼다 — 대기 등록조차 받지 않는다
+    WAITLIST_FULL("WAITLIST_FULL"),
+
+    // 이미 대기 중
+    ALREADY_WAITLISTED("ALREADY_WAITLISTED"),
```
```diff
     /** 막힌 순간의 상태가 곧 낡는가 — 정원은 수시로 변해 다시 받아야 뱃지가 맞는다. */
     val needsRefresh: Boolean
-        get() = this == FULL
+        get() = this == FULL || this == WAITLIST_FULL
```

대기 등록 실패는 새 예외를 만들지 않고 기존 `JoinBlockedException` 을 재사용합니다 — 이미 사유별 문구를
그리는 `JoinBlockedSheet` 가 있고, 대기 등록은 사용자 눈에 "참여를 눌렀는데 막힌 것" 과 같은 사건입니다.

`JoinBlockReasonTest` 의 8종 표에 두 줄을 더해야 합니다(`entries.size` 를 세는 테스트라 안 고치면 깨집니다).

## 4. `repository/ChallengeRepository.kt` — 메서드 2개

대기열은 참여의 다른 경로라 `ChallengeRepository` 에 얹었습니다. 엔드포인트가 2개뿐이고
`join`/`leaveChallenge` 와 같은 리소스 계열이라 별도 `WaitlistRepository` 를 새로 팔 무게가 아닙니다.

```kotlin
/**
 * 정원이 찬 방의 대기열에 등록한다 (명세 POST /challenges/{challengeId}/waitlist).
 *
 * 자리가 나면 **선착순으로 자동 참여**된다 — 그래서 자동 인증 방의 권한은 [join] 과 마찬가지로
 * 호출 전에 확보돼 있어야 한다. 참여가 일어나는 순간에는 권한 화면을 띄울 기회가 없다.
 *
 * 대기열까지 찼거나 이미 대기 중이면 [JoinBlockedException] 이 던져진다
 * (reason `WAITLIST_FULL` / `ALREADY_WAITLISTED`). 정원과 함께 수시로 변하는 값이라 화면은
 * 사유를 받은 뒤 상세를 다시 받아 뱃지를 맞춘다.
 */
suspend fun joinWaitlist(challengeId: String): WaitlistTicket

/** 대기 등록을 취소한다 (명세 DELETE /challenges/{challengeId}/waitlist/me). */
suspend fun leaveWaitlist(challengeId: String)
```

UseCase 는 만들지 않았습니다. 협력자 하나(레포지토리)로의 위임이라 CLAUDE.md 기준으로 ViewModel 이
`ChallengeRepository` 를 직접 주입받아 부르면 됩니다.

---

## 확인이 필요합니다 (아직 추측으로 채우지 않은 것)

1. **참여료·환불은 앱에 아무 표현도 없습니다.** 레포 전체에 결제·금액·환불 개념이 한 줄도 없어서(=이번에
   처음 생기는 개념), 환불 금액 타입을 여기서 만들면 통화·단위·정산 시점을 전부 제가 지어내는 셈이 됩니다.
   지금 설계는 **"환불은 서버 정산이고 앱은 알림으로만 안다"** 를 전제로 KDoc 에 규칙만 적어 뒀습니다.
   - (a) 이대로 간다(앱에는 환불 UI 없음, 푸시 문구만) → 지금 코드로 끝.
   - (b) 앱이 환불 금액·시각을 화면에 보여준다 → 참여료 도메인부터 같이 설계해야 합니다. 명세 주세요.
2. **대기열이 UPCOMING 방 전용인가요?** "시작되면 통째로 사라진다" 는 곧 "진행 중인 방에는 대기열이
   없다" 로 읽힙니다. 그런데 이 레포는 진행 중 방에도 중간 참여를 허용합니다(`joinNote = NEXT_CYCLE`).
   진행 중 만석 방에서도 대기를 받을지에 따라 버튼 노출 범위가 달라집니다. 지금은 **클라이언트가 상태로
   판단하지 않고 서버가 `waitlist` 블록을 주는지로만** 판단하게 해 뒀습니다(어느 쪽 정책이든 안 깨집니다).
3. **50% 의 반올림 방향.** 정원 7명 → 3명(내림)으로 잡았습니다. 정원 1명 방은 상한 0 이라 대기열이 아예
   안 열립니다. 서버 계산식과 일치해야 버튼 상태가 어긋나지 않으니 BE 와 맞춰 주세요.
4. **자동 참여를 앱이 어떻게 아는지.** 푸시로 오는 게 맞을 텐데, 그 페이로드에 `personalSetupRequired`
   (개인 인증 설정 화면으로 보낼지)가 들어오나요? 대기 중에는 셋업을 안 거쳤으므로 자동 참여된 사람은
   셋업 미완 상태로 방에 들어옵니다 — 이 경로를 누가 유도할지 정해야 합니다.
5. **취소 API 의 응답.** 방이 시작돼 대기열이 이미 사라진 뒤 취소를 누르면 200 인가요 404 인가요?

## 다음 단위 (이번 범위 밖)

- `:challenge:data` — `waitlist` 응답 DTO·매핑, `joinWaitlist`/`leaveWaitlist` 구현
- `:challenge:presentation` — 상세 CTA 를 `joinAction` 기준으로 분기, "확인 중" 문구, 대기 취소 진입점
- `ChallengeEvents` — 대기 등록/자동 참여 전환율 이벤트(대기열이 이탈을 줄이는지 보려면 필요합니다)

작업 워크플로우대로면 이슈부터 만들고 `feat/<번호>` 에서 진행해야 하는데, 위 5개가 정해지기 전에는
브랜치를 파도 되돌릴 것 같아 설계만 먼저 드립니다. 1·2번만 정해지면 바로 올리겠습니다.

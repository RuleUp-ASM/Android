# 방 대기열 — `challenge:domain`

## 만든 것

| 파일 | 내용 |
|---|---|
| `challenge/domain/.../entity/Waitlist.kt` | `WaitlistPolicy` · `WaitlistPosition` · `WaitlistEntry` · `WaitlistStatus` · `WaitlistExitReason` · `WaitlistRefund` · `WaitlistExit` · 예외 2종 |
| `challenge/domain/.../repository/WaitlistRepository.kt` | `getWaitlist` / `enqueue` / `cancel` |
| `challenge/domain/.../entity/WaitlistTest.kt` | 50% 경계 · null 순번 · 종료 사유 · 환불 |

### 요구사항이 어디에 갇혔는지

**정원의 50% 한도.** `WaitlistPolicy.maxSize(capacity)` 한 곳에만 있습니다. 화면은
`WaitlistStatus.canEnqueue` / `isFull` 만 보고 버튼을 그리고 비율을 다시 계산하지 않습니다 —
두 곳에서 계산하면 "버튼은 열렸는데 요청은 409" 상태가 생깁니다. 정수 나눗셈이라 **내림**입니다
(정원 3 → 1명, 정원 5 → 2명). 한도 초과는 눌러서 실패하는 게 아니라 **버튼 자체가 비활성**입니다.

**순번 null.** 숫자가 아니라 타입으로 갈랐습니다.

```kotlin
sealed interface WaitlistPosition {
    data object Calculating : WaitlistPosition          // 화면: "확인 중"
    data class Assigned(val order: Int) : WaitlistPosition   // order >= 1
}
```

`fromValue(null)` 은 물론이고 서버가 `0`·음수를 보내도 `Calculating` 으로 떨어집니다.
`Int?` 로 두면 어딘가에서 `?: 1` 이나 `?: 0` 이 붙는 걸 막을 수단이 없어서 타입으로 막았습니다.

**자동 참여.** 승격은 서버가 선착순으로 하므로 포트에 "승격" 메서드를 두지 않았습니다.
앱은 상태를 다시 받아 확인만 합니다.

**시작 시 소멸 + 환불.** `WaitlistExit(reason, refund)`. 환불은
`NotCharged` / `Refunded(amount)` 로 갈랐습니다 — "참여료가 없던 방"과 "0원 환불"을 한 숫자로
접으면 참여료 없는 방에도 환불 문구가 뜹니다.

### UseCase 는 만들지 않았습니다

대기열 등록·취소는 단일 repository 위임이라 CLAUDE.md 기준(협력자 둘 이상 또는 부수효과)에
걸리지 않습니다. ViewModel 이 `WaitlistRepository` 를 직접 주입받으면 됩니다. 승격 알림 처리처럼
협력자가 붙는 시점에 UseCase 로 올리는 게 맞습니다.

---

## 확인이 필요한 것 (진행하지 않고 남겨둔 부분)

명세 문서에 대기열 계약이 아직 없어서, 아래는 제가 정하지 않고 남겼습니다. 답을 주시면 그대로 맞추겠습니다.

1. **참여료가 이 레포에 아직 없습니다.** `fee`·`환불`·금액 개념이 코드 어디에도 없어서
   `WaitlistRefund.Refunded(amount: Int)` 의 **단위·통화를 임의로 Int(원 단위)로 뒀습니다.**
   포인트인지 현금인지, 결제 모듈이 따로 생기는지에 따라 이 타입은 통째로 바뀝니다.
2. **50% 의 반올림 방향.** 내림으로 갔습니다("50%까지만"을 넘지 않는 해석).
   그 결과 **정원 1인 방은 대기열이 0명**입니다. 올림/반올림이면 `WaitlistPolicy` 한 줄만 바뀝니다.
3. **대기 취소도 환불인지.** 요구사항에는 "방이 시작되면 환불"만 있었는데, 본인 취소도 환불로
   가정하고 `cancel` 이 `WaitlistExit` 을 돌려주게 했습니다. 취소는 환불 없음이면 알려주세요.
4. **소멸·승격을 앱이 어떻게 아는지.** 대기 중 방이 시작되거나 자동 참여되는 건 서버 이벤트라
   앱은 알 방법이 없습니다. FCM 페이로드 계약(있다면 `WaitlistExit` 을 그 payload 로 씁니다) 또는
   재조회 시점 중 어느 쪽인지 BE 확인이 필요합니다.
5. **대기 현황을 별도 조회로 둘지, 상세 응답에 실을지.** 지금은 `getWaitlist` 별도 호출입니다
   (정원 찬 방에서만 필요하므로). BE 가 `GET /challenges/{id}` 응답에 실어준다면
   `ChallengeDetail` 에 `waitlist: WaitlistStatus?` 를 추가하고 이 메서드는 지우는 쪽이 왕복이 줍니다.
6. **`JoinBlockReason` 에 `WAITLIST_FULL` 을 넣을지.** 지금은 넣지 않았습니다 —
   `JoinBlockReasonTest` 가 "명세의 8종"을 개수까지 못 박고 있어서, 서버가 실제로 그 사유를
   내려주는 게 확인되기 전에 건드리면 테스트만 흔들립니다. 서버가 409 `JOIN_BLOCKED` 의
   `reason` 으로 준다면 enum + 테스트 한 줄씩 추가하면 됩니다.

## 남은 레이어

`:challenge:data` (엔드포인트·DTO·`WaitlistRepositoryImpl`·Hilt `@Binds`) 와 화면은 아직입니다.
위 1~6 이 정리되면 이어서 붙이겠습니다. 특히 DTO 의 `position: Int?` 는 반드시
`WaitlistPosition.fromValue` 를 거쳐 매핑해야 합니다 — 거기서 `?: 1` 이 붙으면 지금 만든 타입이
무의미해집니다.

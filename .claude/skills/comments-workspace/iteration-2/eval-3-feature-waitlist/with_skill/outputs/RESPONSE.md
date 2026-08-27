# 방 대기열 — challenge:domain

`challenge/domain` 에 넣을 파일 3개를 잡았습니다. domain 만 손댔고 data·presentation 은 아직 없습니다.

| 파일 | 위치 |
|---|---|
| `Waitlist.kt` | `challenge/domain/src/main/kotlin/com/ruleup/challenge/domain/entity/` |
| `WaitlistRepository.kt` | `challenge/domain/src/main/kotlin/com/ruleup/challenge/domain/repository/` |
| `WaitlistTest.kt` | `challenge/domain/src/test/kotlin/com/ruleup/challenge/domain/entity/` |

## 판단한 것

**50% 상한은 `WaitlistPolicy` 한 곳에만 둡니다.** 화면 잠금 조건과 `ChallengeWaitlist.isFull` 이 같은 함수를 봐서, 한쪽만 고쳐지는 일이 안 생깁니다. 버튼 상태는 `ChallengeDetail.joinAction(waitlist)` 이 `Join` / `EnterWaitlist` / `WaitlistFull` / `AlreadyWaiting` / `Blocked` 중 하나로 판정하니, 화면은 이 결과로 분기만 하면 됩니다. ViewModel 에서 다시 계산하지 않습니다.

**순번 null 은 타입으로 막았습니다.** `WaitlistPosition` 을 `Calculating` / `Of(rank)` 로 갈랐고, `Of` 는 `rank >= 1` 을 강제합니다. `Int?` 로 두면 어느 화면에서든 `?: 1` 이나 `?: 0` 으로 접힐 수 있고, 그 순간 사용자는 "곧 들어간다"는 없던 사실을 보게 됩니다. 기존 `roomSuccessRate` 를 null 로 남겨 둔 것과 같은 기준입니다. data 레이어에서 `WaitlistPosition.of(dto.position)` 으로 매핑하세요.

**정원이 찼는데 대기열을 모르면 잠급니다.** `joinAction(waitlist = null)` 은 `Blocked(FULL)` 입니다 — 못 들어갈 방을 열려 있는 것처럼 보이면 눌렀다가 거절을 봅니다.

## 명세가 없어서 가정한 것 (확인 필요)

1. **엔드포인트** — `GET/POST/DELETE /challenges/{id}/waitlist` 로 가정했습니다. 실제 경로가 다르면 `WaitlistRepository` 문구만 바뀝니다.
2. **홀수 정원 반올림** — 정원 5면 대기 2명(내림)입니다. 서버가 올림(3)이면 클라가 한 명을 더 받아 놓고 409 를 보게 되니, BE 규칙을 확인해서 맞춰야 합니다.
3. **참여료·환불** — 레포에 결제/포인트 개념이 아직 없습니다. 금액·통화·환불 시점을 지어내지 않고 `WaitlistExit.entryFeeRefunded: Boolean` 하나만 뒀습니다(모르면 false — 없던 환불을 안내하면 문의가 됩니다). 앱이 금액을 보여줘야 한다면 계약이 더 필요합니다.
4. **자동 참여·대기열 소멸 통지** — 서버가 선착순으로 자동 참여시키므로 앱은 폴링하지 않습니다. 대신 "PROMOTED / CHALLENGE_STARTED" 를 앱이 어떻게 아는지가 비어 있어 **FCM 푸시 payload** 로 가정하고 `WaitlistExit` 를 뒀습니다. 푸시가 아니라 방 재조회로 알아채는 방식이면 이 타입은 빠집니다.
5. **에러 코드** — 대기열 상한 초과·대기 중 방 시작의 서버 코드가 없어 `WaitlistFullException` / `WaitlistClosedException` 만 만들고 매핑은 열어 뒀습니다.

## 고른 갈래 하나

대기열 차단 사유를 기존 `JoinBlockReason` 에 값으로 넣지 않고 `JoinAction` 의 별도 케이스로 뒀습니다. `JoinBlockReason` 은 "공개 상세 `joinBlockReason` = 가입 409 `reason`" 이라는 서버 어휘라, 서버가 안 보내는 값을 끼워 넣으면 `JoinBlockReasonTest` 의 "명세 N종" 단언이 뜻을 잃습니다. 서버가 실제로 `WAITLIST_FULL` 을 409 로 내려준다면 그때 enum 에 추가하고 `JoinAction` 쪽을 지우는 게 맞습니다.

## 남은 작업

- `:challenge:data` — `WaitlistApi`·DTO·`WaitlistRepositoryImpl`·`@Binds` (엔드포인트 확정 후)
- `:challenge:presentation` — 상세 화면 버튼이 `joinAction` 을 구독하도록, "확인 중" 문구
- `ChallengeEvents` — 대기열 진입/이탈 이벤트가 퍼널에 필요한지 확인

## 진행 방식

프로젝트 워크플로우대로면 이슈부터 만들고 `feat/<번호>` 브랜치에서 작업합니다. 위 가정 5건(특히 2·3)은 BE 확인 없이 코드로 굳히면 되돌리는 비용이 커서, 확인 후에 진행할지 알려 주세요.

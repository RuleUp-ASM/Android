# 지오펜스가 옛 좌표에서 계속 터지는 문제

진단하신 게 맞습니다. 원인은 `toAdd` 를 구하는 한 줄입니다.

```kotlin
val toAdd = desired.filter { it.requestId !in registeredIds }
```

`requestId` 는 앵커가 아니라 **멤버**(`userId#challengeId#index`)에 붙는 키입니다. 사용자가 인증 장소를
옮기면 `lat`/`lng`/`radiusM`/`dwellMinutes` 는 바뀌지만 `requestId` 는 그대로죠. 그래서 이 필터는 바뀐
목표를 "이미 등록됨"으로 보고 `toAdd` 에서 빼버립니다. `toRemoveIds` 도 그 id 가 `desired` 에 여전히
있으니 건드리지 않고요. 결과적으로 **어느 계획에도 안 잡혀서 OS 에는 옛 좌표 펜스가 그대로 남습니다.**

## 고친 방식

차집합을 없애고 `desired` 전체를 그대로 등록합니다.

```kotlin
val desiredIds = desired.mapTo(HashSet()) { it.requestId }
val toRemove = registeredIds.filter { it !in desiredIds }
return ReconcilePlan(toAdd = desired, toRemoveIds = toRemove)
```

`addGeofences` 는 동일 `requestId` 를 **멱등 교체**하므로 전체 재등록이 안전합니다. 바뀐 목표는 교체로
갱신되고, `desired` 에서 사라진 것만 해제됩니다. 이건 이 레포의 실제 프로덕션 코드
(`verification/data/.../GeofenceReconcile.kt`)가 이미 쓰고 있는 방식이기도 합니다 — 거기 `toRemove` 만
계산하고 추가는 `registerAll(targets)` 로 전량 등록합니다. 이 파일만 차집합 필터가 끼어 어긋나 있었습니다.

## 같이 잡힌 두 번째 버그

`registeredIds` 로 추가를 거르는 건 앵커 이동과 무관하게도 틀렸습니다. 지오펜싱 API 에는 **등록분 조회
API 가 없습니다.** 그래서 이 집합은 OS 의 실제 상태가 아니라 "직전에 등록을 시도해 로컬 DB 에 보존한
id" 일 뿐입니다. 원래 KDoc 이 짚은 대로 재부팅·Play Services 갱신이면 OS 등록분은 비지만 로컬 집합은
남아 있으니, 차집합을 쓰면 **콜드스타트에서 아무것도 재등록하지 않고 펜스가 전부 죽습니다.** 앵커를 안
옮긴 사용자도 재부팅 한 번이면 인증이 멈추는 경로였습니다. 전량 등록으로 바꾸면서 같이 없어집니다.

KDoc 에 `registeredIds` 가 실제로 무엇인지, 왜 제거 계산에만 쓰는지를 적어뒀습니다.

## 남은 판단 두 가지 (제 임의로 안 바꾼 것들)

1. **바뀐 id 를 `toRemoveIds` 에 넣지 않았습니다.** remove 와 add 는 별개 비동기 호출이라 순서가 뒤집히면
   방금 등록한 펜스를 제거가 지워버립니다. 멱등 교체가 되니 제거 없이 교체만 하는 게 맞다고 봤습니다.
2. **재등록은 DWELL 타이머를 리셋합니다.** 전량 재등록이면 안 바뀐 펜스도 매 콜드스타트마다 체류 시계가
   0 으로 돌아가고, `INITIAL_TRIGGER_DWELL` 때문에 ENTER 도 다시 뜹니다. 체류 목표가 긴 방(예: 60분)에서
   앱이 자주 콜드스타트하면 체류가 영영 안 쌓일 수 있습니다. 이건 이 버그와 별개로 **기존 프로덕션 코드도
   갖고 있는 성질**이라 이번 수정 범위에선 손대지 않았습니다.

   고치려면 로컬 스냅샷에 좌표까지 들어 있으니(`geofence_target` 테이블에 `lat`/`lng`/`radiusM`/
   `dwellMinutes` 다 있음) 내용 비교로 **실제로 바뀐 것만** 재등록할 수 있습니다. 다만 그러면 재부팅 후
   "스냅샷은 그대로인데 OS 는 비어 있는" 상황에서 다시 아무것도 등록 안 하는 문제로 되돌아가므로,
   부팅/Play Services 갱신을 감지해 강제 전량 등록하는 경로가 같이 필요합니다. 별도 이슈로 다루는 게
   좋아 보이는데, 진행할까요?

## 참고

`registeredIds` 라는 이름이 이 버그를 부른 원인 같습니다. OS 등록분처럼 읽히거든요. `previousIds` 정도로
바꾸는 걸 권합니다만, 호출부를 건드려야 해서 이번엔 이름을 유지하고 KDoc 으로만 정정했습니다.

`reconcilePlan` 이 **정체성(requestId)만** 보고 추가 대상을 골라서 생긴 버그였습니다.

`requestId` 는 `{userId}#{challengeId}#{anchorIndex}` 에서 파생되므로 사용자가 앵커를 옮겨도 값이 그대로입니다.
그래서 `it.requestId !in registeredIds` 는 "이미 등록됨" 으로 판정하고 `toAdd` 에서 빼버렸고, OS 에는 옛 좌표의 펜스가 그대로 남아 계속 터졌습니다.

## 고친 내용

- 두 번째 인자를 `registeredIds: Set<String>` → `registered: List<GeofenceTarget>` 로 바꿨습니다.
  id 만으로는 앵커가 바뀐 것을 알 방법이 없어서, 직전에 등록한 **목표 스냅샷**이 필요합니다.
- 추가 대상 판정을 `requestId` 비교에서 **목표 전체 비교**로 바꿨습니다.
  `GeofenceTarget` 이 data class 라 `lat`·`lng`·`radiusM`·`dwellMinutes` 중 하나만 달라져도 재등록 대상으로 잡히고,
  나중에 앵커 필드가 늘어도 비교가 자동으로 따라갑니다.
- 제거 대상은 그대로 "더 이상 원하지 않는 id" 입니다. 앵커만 바뀐 건 제거 목록에 넣지 않았습니다 —
  `addGeofences` 가 같은 `requestId` 를 멱등 교체하므로, 지웠다 다시 넣으면 그 사이 펜스가 비는 구간만 생깁니다.

## 호출부에서 확인할 것

- `registered` 에 넘길 값은 로컬에 보존한 직전 목표입니다 (`geofenceTargetDao.all()` 상당). OS 는 등록된 펜스 목록을 조회하는 API 가 없어서 이 스냅샷이 유일한 근거입니다.
- 스냅샷 갱신 시점: OS 등록에 성공한 뒤에 저장해야 합니다. 먼저 저장하면 등록 실패한 목표가 "등록됨" 으로 남아 다음 콜드스타트에서도 재시도되지 않습니다.
- 부팅으로 OS 등록이 날아간 경우는 이 함수 밖(BootReceiver 의 재등록 경로)에서 계속 처리해야 합니다. 이 계획은 앵커가 안 바뀐 목표를 다시 넣지 않기 때문입니다.

기존 KDoc 의 §2.3 근거와 콜드스타트 호출 이유는 그대로 뒀습니다.

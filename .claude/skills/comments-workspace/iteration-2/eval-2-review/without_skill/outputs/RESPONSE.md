# 지오펜스 두 파일 주석 리뷰

## 총평

과한 주석은 생각보다 적다. **양이 문제가 아니라 정확도와 중복이 문제**다.

- 사실과 다른 주석 **2건**(둘 다 클래스 KDoc). 이건 "빼거나 말거나"가 아니라 **반드시 고쳐야 할 것** — 지금 상태로는 읽는 사람을 반대 방향으로 보낸다.
- 다른 파일 KDoc과 **거의 그대로 겹치는 주석 3건**. 원본 쪽이 더 잘 써 있으니 여기서 덜어낸다.
- 순수 재진술(코드를 한글로 옮긴 것) **2~3줄**. 잘라내면 된다.
- 정작 이 두 파일에서 **제일 안 뻔한 지점 2곳은 주석이 없다**. 빼는 것보다 여기 채우는 게 이득이 크다.

`geofenceResponsivenessFor` KDoc(길어서 제일 의심스러워 보이는 것)은 **길이만 길지 과하지 않다**. 뒤에서 이유를 적었다.

---

## 1. 먼저 고쳐야 할 것 — 사실과 다른 주석

### (a) `GeofenceBroadcastReceiver.kt:21-23` — 틀렸다

```
벽시계(`observedAt`)와 monotonic 시각(`observedElapsedMillis`)을 **수신 시점에 함께** 찍는다.
```

두 군데가 어긋난다.

1. **`observedAt` 이라는 이름이 이 파일에 없다.** 리시버가 쓰는 필드는 `occurredAt` 이고, `observedAt` 으로 바뀌는 건 한참 뒤 `SignalEntityMappers.kt:27` (`observedAt = occurredAt`) 에서다. 파일 안에서 grep 해도 안 나오는 이름을 KDoc이 강조한다.
2. **"수신 시점에 함께" 가 아니다.** 코드는 `occurredAt = location?.time ?: System.currentTimeMillis()` 다. 즉 위치가 실려 오는 **정상 경로에서는 fix 시각**이고, 수신 시각으로 떨어지는 건 위치가 없는 fallback 뿐이다. 반면 `observedElapsedMillis` 는 항상 수신 시점. 두 값의 기준이 애초에 다르다.

이게 왜 심각하냐면 — 같은 패키지의 `GeofenceRegisterImpl.kt:161-162` 가 **정반대로, 그리고 맞게** 써 있다:

> 전이 시각은 배달 시점이 아니라 fix 시각(`location.time`)에서 오고

같은 패키지 안에서 두 주석이 서로를 부정한다. 리시버 쪽이 틀렸다.

덧붙여 "둘의 간격이 어긋나면 시각 조작이라 서버가 대조한다"는 대조 로직의 전제 자체가 이 어긋남에 걸린다 — 벽시계가 fix 시각이면 배달 지연(최대 5분, 아래 §12 참고)이 그대로 간격 차이로 들어온다. 주석을 고치면서 **이 대조가 지연을 감안하는지 서버와 한 번 맞춰볼 값어치가 있다.**

권장: 문단을 이 정도로 줄이고 정확하게.

> `observedElapsedMillis`(monotonic)는 **수신 시점에만** 찍을 수 있다 — 나중에 재구성할 수 없어 여기서 놓치면 영영 못 채운다. 벽시계 짝(`occurredAt`)은 가능하면 fix 시각을 쓴다. 서버가 둘을 대조해 시각 조작을 본다(전송 스펙 §6.4).

### (b) `GeofenceRegisterImpl.kt:24-25` — 과장됐다

```
지오펜싱 API 호출은 모두 [Context.hasFineLocation] 가드 뒤에서 일어나고 SecurityException 을 삼키므로
```

"**모두**"가 사실이 아니다. `hasFineLocation` 가드 뒤에 있는 건 `addGeofences`(등록) 뿐이다. `removeGeofences` 는 네 군데(`reconcile:49`, `bind:73`, `unbind:85`, `clear:93`) 전부 **권한 확인 없이** 호출된다. 특히 `bind()` 는 73행에서 먼저 제거하고 76행에 가서야 권한을 본다.

실제로 안전하긴 하다 — `removeGeofences` 는 위치 권한을 요구하지 않고, 전부 `runCatching` 안이다. 하지만 **주석이 근거로 내세운 불변식이 코드와 다르면 그 불변식을 믿고 코드를 고치는 사람이 다친다.** 클래스 단위 `@SuppressLint` 를 정당화하는 문장이라 더 그렇다.

권장:

> 등록(`addGeofences`)만 [Context.hasFineLocation] 가드 뒤에 두고, 모든 지오펜싱 호출을 `runCatching` 으로 감싸 SecurityException 을 삼킨다. 해제는 권한을 요구하지 않는다. 그래서 권한 lint(MissingPermission)를 클래스 단위로 억제한다.

이 주석 자체는 **꼭 남겨야 한다.** 린트 억제의 근거를 적는 건 주석이 존재하는 이유 그 자체다. 문장만 정확하게 고치면 된다.

---

## 2. 빼거나 줄일 것

### 중복 — 다른 파일에 더 잘 써 있다

| 위치 | 내용 | 판단 |
|---|---|---|
| `Receiver.kt:18-19` | "즉시 Room 에 적재한다(앱이 죽어도 보존 → sync 가 드레인)" | `GeofenceEntities.kt:6-7` 에 **거의 같은 문장**이 이미 있다("리시버가 앱이 죽어도 보존되도록 즉시 적재하고 sync 가 드레인한다"). 스키마 쪽이 이 규칙의 제자리다. 여기선 "(전송 스펙 §1)" 앵커만 남기고 문장은 덜어낸다 |
| `Receiver.kt:37-38` 뒷문장 | "0m·'mock 아님'으로 접으면 없던 사실이 판정에 들어간다" | `GeofenceEntities.kt:12-13` 과 사실상 동일. 다만 **여기가 null 이 실제로 만들어지는 지점**이라 남기는 쪽에 한 표. 대신 앞문장이 더 중요하다(아래 참고) |
| `RegisterImpl.kt:21-22` | "reconcile 은 차집합만 제거하고 새 목표 전체를 멱등 등록한다(재부팅 후 전부 재등록)" | `GeofenceReconcile.kt` KDoc이 같은 말을 **이유까지 붙여** 더 잘 한다("OS 는 등록된 펜스 목록 조회 API 가 없으므로"). 여기 문장은 이유가 빠진 요약본이라 값이 없다. **삭제** |

### 순수 재진술

- **`Receiver.kt:61`** — "적재 직후 expedited catch-up flush 를 걸어 다음 30분 주기를 기다리지 않고 전송(전송 스펙 §0.6)."
  앞 절반("expedited catch-up flush 를 걸어")은 바로 아랫줄 `enqueueCatchUp` 을 한글로 옮긴 것이고, expedited 라는 사실은 `VerificationSyncSchedulerImpl` 의 KDoc 소관이다. 값은 **"다음 30분 주기를 기다리지 않고"** 하나뿐 → 한 줄로:
  > 다음 30분 주기를 기다리지 않고 바로 올린다(전송 스펙 §0.6).

- **`Receiver.kt:19`** — "에러(GEOFENCE_NOT_AVAILABLE=위치 꺼짐 등)는 무시한다."
  `if (event.hasError()) return` 의 재진술 + 상수 뜻풀이. 상수 뜻풀이는 값이 있으니 통째로 버리진 말되, **정작 궁금한 건 "왜 gap 으로 기록하지 않는가"** 다. 이 파일은 다른 결측은 `GapRecorder` 로 보고하는 세계관 위에 있는데(옆 파일 `recordNotRegistered`), 여기만 조용히 `return` 한다. 그 이유를 못 적겠으면 그건 주석 문제가 아니라 **누락된 gap 기록**일 수 있다. 확인해볼 것.

- **`RegisterImpl.kt:65`** — "이 멤버(prefix) 소속 기존 펜스 중 새 목록에 없는 것만 해제 — 다른 멤버 목표는 유지(명세 §5.4.3)."
  앞 절반은 `filterNot { it in newIds }` 를 그대로 읽은 것. 값은 뒤 절반(`bind` 가 `reconcile` 과 다른 이유). 앞을 잘라 뒤만 남긴다:
  > 다른 멤버의 목표는 건드리지 않는다 — 그래서 `reconcile` 이 아니라 prefix 범위로 지운다(명세 §5.4.3).

---

## 3. 그대로 남길 것 (건드리지 말 것)

- **`Receiver.kt:37` 앞문장** — "위치가 없는 전이도 신호로서 유효하다(어느 펜스를 언제 넘었는지)."
  이게 이 파일에서 제일 값진 주석이다. 이게 없으면 다음 사람이 "`location == null` 이면 어차피 못 쓰니 `return`" 으로 '정리'해 버린다. 커밋 `9ad2b89`("좌표 결측을 (0,0) 으로 위조하지 않는다")가 정확히 이 판단의 기록이다.
- **`RegisterImpl.kt:39`** — "권한 없으면 OS 등록 불가 → 목표만 보존하고 종료(허용 후 reconcile 이 재시도)."
  등록도 못 할 목표를 왜 저장하는지는 코드에서 절대 안 읽힌다. 유지.
- **`RegisterImpl.kt:98-99`** — heartbeat 기록 + 실패를 삼키되 gap 으로 보고 + 다음 reconcile 재시도.
  `onSuccess`/`onFailure` 두 갈래가 각각 왜 그 일을 하는지, 그리고 삼킨 실패가 어디서 회수되는지를 담고 있다. 셋 다 코드에서 유도 불가. 유지.
- **`RegisterImpl.kt:131`** — "DWELL 이 OS 에서 직접 '체류 임계 도달'을 쏜다(명세 §2.1)."
  한 줄로 `setLoiteringDelay` 가 앱 쪽 타이머가 아닌 이유를 설명한다. 모범 사례.
- **`RegisterImpl.kt:24-25`** — 위 §1(b) 대로 문장만 고쳐서 유지.

### `geofenceResponsivenessFor` KDoc(157-168)은 과하지 않다

6줄 함수에 4문단이라 제일 먼저 의심이 가지만, 세 문단이 각각 다른 질문에 답한다.

1. **왜 0이 아니라 값을 지정하나** — "지정하지 않으면 기본 0(=최대한 빨리)이라 배칭이 아예 꺼진다". 이건 API 문서를 안 보면 모른다. 필수.
2. **늦게 와도 왜 괜찮나** — 이게 5분이라는 선택을 정당화하는 유일한 근거다. 필수. (그리고 §1(a)의 오류를 잡아주는 문장이기도 하다.)
3. **왜 `min()` 이고 왜 0은 예외인가** — `dwellMinutes` 가 서버 값이라 하한을 못 잡는다는 부분이 핵심. 필수.

`GeofenceResponsivenessTest` 가 세 분기를 이름과 한 줄 주석으로 이미 커버하므로 3문단은 조금 압축 여지가 있지만, 이건 **이슈 #357 의 판단 기록**이라 지금 길이를 유지해도 손해가 아니다. 굳이 손대지 않는 쪽을 권한다.

`:154` 의 "배칭 허용 기본치 5분(Google 권고 수준)" 도 유지. 다만 "Google 권고 수준"은 출처가 없어 검증이 안 된다 — 문서 링크를 달거나 그 표현을 빼는 게 낫다.

---

## 4. 오히려 없어서 아쉬운 것 (추가 권장)

빼는 것보다 이쪽이 이득이 크다.

### (a) `reconcile()` 에 "부분 목록 금지" 경고 — `RegisterImpl.kt:38` 근처

`reconcile()` 은 마지막에 `persist()` 를 부르고, `persist()` 는 `geofenceTargetDao.clear()` 로 **전체 테이블을 지운다**(117-120). 즉 **일부 멤버 목표만 담은 리스트로 `reconcile` 을 부르면 다른 멤버 목표가 전부 날아간다.**

지금은 호출자가 `reconcilePersisted()`(전체 집합) 뿐이라 사고가 안 나지만 — 바로 아래 `bind()` 가 prefix 범위로 조심스럽게 작업하는 걸 본 사람은 `reconcile(oneMemberTargets)` 을 자연스럽게 호출할 수 있다. 시그니처가 그걸 전혀 막지 않는다. 한 줄이면 된다:

> `targets` 는 **항상 전체 desired set** 이다 — 부분 목록을 넘기면 `persist` 가 나머지 멤버 목표까지 지운다. 멤버 단위 갱신은 [bind] 를 쓴다.

### (b) 리시버가 왜 `@AndroidEntryPoint` 가 아닌가 — `Receiver.kt:25` 근처

`GeofenceBroadcastReceiver` 는 Hilt 없이 `verificationDatabase(context)` 로 DAO 를 직접 꺼낸다. 그런데 **같은 패키지의 `BootReceiver` 는 `@AndroidEntryPoint` + `@Inject`** 다. 나란히 놓고 보면 왜 다른지 설명이 필요한 비대칭이고, `enqueueCatchUp` KDoc이 "Hilt 그래프에 접근 못 하는 BroadcastReceiver" 를 전제로 삼고 있어 더 헷갈린다. 이유(콜드스타트 지연? Hilt 초기화 전 발화?)를 한 줄 남기거나, 이유가 없다면 `BootReceiver` 와 같은 방식으로 정리하는 게 맞다.

### (c) 도메인 포트가 한 약속이 구현에 없다 (참고)

리뷰 대상 밖이지만 걸린다. `GeofenceRegister.reconcile` KDoc:

> 최대 100개 제한(TOO_MANY_GEOFENCES) 초과 시 마감 임박 우선으로 등록한다(명세 §2.1)

`GeofenceRegisterImpl` 에는 **100개 캡도, 마감 임박 정렬도 없다.** 구현 없는 약속이 인터페이스 주석에만 남아 있다. 주석을 지우든(TODO/이슈로 옮기든) 구현하든 — 지금은 포트 주석이 거짓말을 하고 있다.

---

## 정리

| 액션 | 대상 |
|---|---|
| **고친다(필수)** | `Receiver.kt:21-23` 사실 오류 · `RegisterImpl.kt:24-25` "모두" 과장 |
| **지운다** | `RegisterImpl.kt:21-22`(GeofenceReconcile 중복) · `Receiver.kt:18-19` 본문(엔티티 중복) |
| **줄인다** | `Receiver.kt:61` · `RegisterImpl.kt:65` 앞절반 |
| **남긴다** | `Receiver.kt:37` 앞문장 · `RegisterImpl.kt:39`, `:98-99`, `:131`, `:154`, `:157-168` |
| **추가한다** | `reconcile` 부분목록 경고 · 리시버 non-Hilt 이유 |
| **확인해볼 것** | `hasError()` 를 gap 없이 삼키는 게 맞나 · fix 시각 기준이면 §6.4 대조가 배달 지연을 감안하나 · 포트의 100개 캡 약속 |

코드는 한 줄도 고치지 않았다 — 위는 전부 제안이다.

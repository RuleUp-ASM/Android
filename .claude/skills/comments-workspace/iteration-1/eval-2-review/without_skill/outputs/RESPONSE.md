두 파일 다 읽고, 주변 파일(`GeofenceEntities.kt`, `GeofenceReconcile.kt`, `LocationPermissions.kt`, `VerificationSyncSchedulerImpl.kt`)까지 대조해서 봤어. 결론부터 말하면 **"과한 주석"보다 더 큰 문제는 ①다른 파일과 겹치는 중복과 ②사실이 틀린 주석 두 건**이야. 순수하게 길이만 과한 건 사실 하나뿐이고.

우선 안심해도 되는 것: `명세 §2.1`, `전송 스펙 §0.6` 같은 스펙 앵커는 **전부 유지**. 코드가 절대 스스로 말할 수 없는 정보고, 이 레포 전반이 쓰는 컨벤션이야. 이건 손대지 마.

---

## 🔴 먼저 고쳐야 할 것 — 주석이 틀렸다

### 1. `GeofenceBroadcastReceiver.kt:21-23` — 두 군데가 사실과 다름

```
 * 벽시계(`observedAt`)와 monotonic 시각(`observedElapsedMillis`)을 **수신 시점에 함께** 찍는다.
```

- **`observedAt` 은 이 파일에 없다.** 여기 지역변수는 `occurredAt` 이고, `observedAt` 은 `SyncRequest` DTO 필드명이야 (`SignalEntityMappers.kt:27` 에서 `observedAt = occurredAt` 으로 매핑됨). 이 파일만 보고 `observedAt` 을 찾으면 못 찾아.
- **"수신 시점에 함께 찍는다"가 거짓이다.** 실제 코드는
  ```kotlin
  val occurredAt = location?.time ?: System.currentTimeMillis()   // ← fix 시각
  val observedElapsedMillis = SystemClock.elapsedRealtime()       // ← 수신 시각
  ```
  위치가 실려 오면 `occurredAt` 은 **fix 시각**이고 수신 시각이 아니야. 둘이 같은 시점인 건 fallback 분기뿐.
- 게다가 같은 PR 범위 안에서 **두 주석이 서로 모순**돼. `GeofenceRegisterImpl.kt:161-162` 는 정반대로 "전이 시각은 배달 시점이 아니라 fix 시각(`location.time`)에서 온다"고 못박아 놨어. 그리고 우린 responsiveness 로 최대 5분 배칭을 켜놨으니 그 간극은 실제로 벌어져.

§6.4 시각 조작 대조가 어긋났을 때 이 주석 믿고 디버깅하면 완전히 헛다리 짚는다. **지우지 말고 사실만 고쳐.** 의도(둘 다 여기서 안 찍으면 영영 복구 못 한다)는 이 파일에서 가장 값어치 있는 주석이야. 예시:

```kotlin
/**
 * 지오펜스 전이 수신(전송 스펙 §1). 에러(GEOFENCE_NOT_AVAILABLE=위치 꺼짐 등)는 무시한다 —
 * 다음 reconcile 이 재등록한다.
 *
 * monotonic 시각(`observedElapsedMillis`)은 **수신 시점에만** 찍을 수 있다. 서버가 이걸
 * 벽시계(`occurredAt`, 전송 시 `observedAt`)와 대조해 시각 조작을 잡는데(전송 스펙 §6.4),
 * 나중에 재구성할 수 없는 값이라 여기서 놓치면 영영 못 채운다.
 */
```

### 2. `GeofenceRegisterImpl.kt:24-25` — SuppressLint 근거가 부정확

```
 * 지오펜싱 API 호출은 모두 [Context.hasFineLocation] 가드 뒤에서 일어나고 ...
```

"모두"가 아니야. `unbind()`(l.85), `clear()`(l.93) 의 `removeGeofences` 는 권한 가드가 없고, `bind()`(l.73) 도 제거를 먼저 하고 나중에 검사해. 실제로는 `runCatching` 이 막아주니까 **안전하긴 한데**, 이 주석의 존재 이유 자체가 "억제해도 되는 근거를 기록으로 남기는 것"이라 근거가 틀리면 주석의 값어치가 0이 돼. 실제 불변식대로 고쳐:

```
 * 등록(addGeofences)은 [Context.hasFineLocation] 가드 뒤에서만, 해제(removeGeofences)는
 * runCatching 으로 SecurityException 을 삼킨다. 그래서 권한 lint 를 클래스 단위로 억제한다.
```

이 주석 자체는 **반드시 유지**. 애노테이션 억제 근거는 코드가 못 하는 말이야.

---

## 🟡 빼도 되는 것 — 다른 파일에 이미 있는 말

전부 "이 파일에만 있는 줄 알았는데 원본이 따로 있는" 케이스야. 같은 사실은 **그 사실을 강제하는 곳에 한 번만** 두는 게 맞아.

| 위치 | 중복 원본 | 조치 |
|---|---|---|
| `Receiver.kt:18-19` "즉시 Room 에 적재한다(앱이 죽어도 보존 → sync 가 드레인)" | `GeofenceEntities.kt:7` 에 거의 그대로 있음 | 삭제 |
| `Receiver.kt:37-38` "정확도·mock 은 지어내지 않고 null — 0m·mock 아님으로 접으면 없던 사실이…" | `GeofenceEntities.kt:12-14` 에 거의 그대로 있음. 게다가 이 규칙은 `accuracy: Float?` 타입이 이미 강제함 | 앞 문장만 남기기 |
| `RegisterImpl.kt:22` "reconcile 은 차집합만 제거하고 새 목표 전체를 멱등 등록" | `GeofenceReconcile.kt` KDoc 이 같은 말을 더 자세히 함 (그쪽이 그 로직의 주인) | 삭제 |
| `Receiver.kt:61` "expedited catch-up flush… 다음 30분 주기를 기다리지 않고" | `VerificationSyncSchedulerImpl.enqueueCatchUp` KDoc | 삭제하거나 `// 전송 스펙 §0.6` 한 줄로 |
| `RegisterImpl.kt:154` "30분 주기 전송보다 한참 짧아 전달 지연에 묻힌다" | 바로 밑 함수 KDoc 2번째 문단과 같은 말 | 뒷문장 삭제, "Google 권고 수준"만 남기기 |

`Receiver.kt:37-38` 은 이렇게 줄이면 돼 — **살릴 문장은 "왜 return 하지 않는가"** 쪽이야. 이건 다른 데 없거든:

```kotlin
// 위치가 없는 전이도 신호로서 유효하다(어느 펜스를 언제 넘었는지). 정확도·mock 은 null 로 둔다.
```

---

## 🟢 그대로 둘 것

이건 다 "코드를 읽어도 알 수 없는 것"을 말하고 있어서 건드리면 손해야.

- `RegisterImpl.kt:39` — 권한 없을 때 목표만 보존하는 이유 + "허용 후 reconcile 이 재시도". persist 를 왜 남기는지 코드에 안 나옴.
- `RegisterImpl.kt:65` — `bind` 가 prefix 범위만 건드리는 이유(다른 멤버 목표 유지, §5.4.3). `requestId = userId#challengeId#index` 라는 형식을 모르면 절대 추론 불가.
- `RegisterImpl.kt:98-99` — heartbeat 기록 + 어떤 예외를 왜 삼키는지 + gap 으로 보고한다는 것. 세 사실 다 non-obvious.
- `RegisterImpl.kt:131` — DWELL 을 OS 가 직접 쏜다(= 앱이 체류를 계산하지 않는다). 한 줄이고 설계 결정이라 값어치 있음.
- `Receiver.kt` 의 "에러 무시" 는 이유("다음 reconcile 이 재등록")를 덧붙이면 더 좋아. 지금은 `if (hasError()) return` 을 한국어로 옮긴 것에 가까워.

---

## 🟠 길이가 과한 건 딱 하나

`geofenceResponsivenessFor` KDoc (`RegisterImpl.kt:157-168) — **6줄짜리 함수에 12줄 KDoc**. 파일에서 제일 큰 블록이고, 네가 "과하다"고 느낀 게 아마 여기일 거야.

근데 내용을 뜯어보면 대부분은 살려야 해:

- **1문단** "지정 안 하면 기본 0 이라 배칭이 아예 꺼진다" → GMS API 함정. 코드에 안 보임. **유지.**
- **2문단** "늦게 배달돼도 판정은 멀쩡" → 5분을 고른 안전성 근거. 근데 위 상수 주석과 겹침. **상수 쪽을 지우고 여기 유지.**
- **3문단** "loiteringDelay 를 넘기면 DWELL 이 늦어져서 짧은 체류 목표가 손해" → **이 함수가 존재하는 유일한 이유.** 절대 유지.
- 마지막 "0 인 펜스는 DWELL 을 안 쏘니 깎을 이유 없다" → else 분기 근거. 한 절이니 유지.

즉 지울 건 없고 **압축**만 하면 돼. 6~7줄로 줄이면 충분. 참고로 세 분기는 `GeofenceResponsivenessTest` 가 이미 검증하니까 "무엇을 하는가"는 테스트가 말해주고, 주석은 "왜 min 인가"만 남기면 돼.

---

## 보너스: 오히려 없어서 아쉬운 것

`Receiver.kt:46-47` 의 `goAsync()` + `CoroutineScope(Dispatchers.IO)` 조합. BroadcastReceiver 는 라이프사이클이 없어서 스코프를 붙일 데가 없고, `goAsync()` 는 ~10초 제한이 있다는 게 이 패턴의 전제인데 아무 데도 안 적혀 있어. `BootReceiver` 도 같은 패턴이고. 리뷰에서 "unscoped CoroutineScope 아니냐"고 반드시 걸리는 자리라, 둘 중 한 곳에 한 줄 남기는 게 위에서 지우는 다섯 줄보다 값어치 있어.

---

**요약**: 사실 오류 2건(시각 주석, SuppressLint 근거)을 고치고, 다른 파일과 겹치는 5건을 지우고, responsiveness KDoc 하나를 절반으로 압축. 스펙 앵커와 "왜"를 설명하는 5건은 그대로. 순 감소는 대략 15줄쯤인데, 실제로 얻는 건 줄 수보다 **모순되는 주석 두 개가 사라지는 것**이야.

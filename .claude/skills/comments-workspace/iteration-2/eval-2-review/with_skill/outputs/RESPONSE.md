두 파일 주석을 한 줄씩 봤습니다. 결론부터: **과한 곳은 딱 두 군데**(`GeofenceRegisterImpl` 하단 `geofenceResponsivenessFor` KDoc, 클래스 KDoc 1문단)이고, 그보다 먼저 봐야 할 건 **리시버 KDoc이 코드와 어긋나 있다**는 점입니다. 코드는 손대지 않았고 판정만 정리했습니다.

---

## 0. 먼저 — 주석 정리 이전에 사실 확인이 필요한 곳

`GeofenceBroadcastReceiver.kt:21-23`

```
벽시계(`observedAt`)와 monotonic 시각(`observedElapsedMillis`)을 **수신 시점에 함께** 찍는다.
```

코드와 두 가지가 안 맞습니다.

1. `observedAt` 라는 필드는 없습니다. 실제 필드는 `occurredAt` (`GeofenceEntities.kt:25`).
2. 더 중요한 건 **"수신 시점에 함께"가 아니라는 것**입니다.

```kotlin
val occurredAt = location?.time ?: System.currentTimeMillis()   // fix 시각
val observedElapsedMillis = SystemClock.elapsedRealtime()       // 수신 시각
```

위치가 실리면 벽시계는 **fix 시각**이고 monotonic 은 **수신 시각**입니다. 둘이 같은 순간인 건 위치가 없어 fallback 을 타는 경우뿐이에요. 그런데 같은 PR 안의 다른 주석(`GeofenceRegisterImpl.kt:161-162`)은 배칭 때문에 배달이 **최대 5분(`DEFAULT_GEOFENCE_RESPONSIVENESS_MS`)** 늦을 수 있다고 명시합니다. 즉 정상 기기에서도 두 값이 최대 5분 벌어질 수 있습니다.

§6.4 대조가 "둘의 간격"을 보는 거라면 이건 주석이 낡은 게 아니라 **코드가 오탐을 만드는 쪽**일 수 있습니다. 스킬 기준으로도 주석과 코드가 어긋날 땐 어느 쪽이 틀렸는지부터 가리는 자리고요. 판단이 필요합니다:

- 서버가 `bootEpoch` 기준으로 보정해서 fix 시각이어도 무방하다 → **주석을 코드에 맞게 고친다** (한 줄: `// monotonic 은 수신 시각, occurredAt 은 fix 시각 — 시각 조작 대조용(전송 스펙 §6.4).`)
- 두 값이 같은 순간이어야 한다 → **코드가 버그**. `observedElapsedMillis` 를 fix 시각에 맞춰 보정하거나, 벽시계도 수신 시점으로 통일해야 합니다.

어느 쪽인지는 전송 스펙 §6.4 를 봐야 확정됩니다. 여기부터 정하고 나머지를 손대는 게 순서 같습니다.

---

## 1. GeofenceBroadcastReceiver.kt

| 위치 | 주석 | 판정 |
|---|---|---|
| 18-19 | 클래스 요약 + 전송 스펙 §1 | **유지**(요약 한 줄은 필요) |
| 19 | `에러(...)는 무시한다` | **고쳐서 유지** |
| 21-23 | 시각 두 값 문단 | **고친다** (위 0번) |
| 37-38 | 위치 없는 전이 / null 유지 | **한 줄로 줄여 유지** |
| 61 | catch-up flush | **한 줄로 줄여 유지** |

**19줄 `에러는 무시한다`** — `if (event.hasError()) return` 이 "무시한다"는 이미 말합니다. 주석이 채워야 할 자리는 **왜 무시해도 되는가**인데 그게 비어 있어요. 특히 `GeofenceRegisterImpl` 은 미등록을 `GEOFENCE_NOT_REGISTERED` gap 으로 남기는데, 여기 에러 경로는 gap 을 **안** 남깁니다. 그게 의도인지 누락인지 코드로는 안 보입니다. 의도라면 그 한 줄이 지금 문장보다 훨씬 값어치 있습니다.

**37-38줄** — 이건 "안 고른 대안"의 전형이라 남길 값어치가 확실합니다. 다만 첫 문장은 `GeofenceEntities.kt:13-14` 의 엔티티 KDoc 과 거의 같은 말이라 중복이에요. 뒷문장만 남기면 충분합니다.

```kotlin
// 0m·"mock 아님"으로 접으면 없던 사실이 판정에 들어간다.
```

**61줄** — 호출부 이름이 `enqueueCatchUp` 이라 "catch-up flush 를 건다"는 코드 되풀이입니다. 남는 정보는 "다음 주기를 안 기다린다" + 절 번호뿐.

```kotlin
// 다음 30분 주기를 기다리지 않고 바로 보낸다(전송 스펙 §0.6).
```

`goAsync()` / `pending.finish()` 쪽은 주석이 없는데, 그건 그대로 두는 게 맞습니다. 안드로이드 리시버의 흔한 관용이라 읽는 사람이 이미 아는 것에 속해요.

---

## 2. GeofenceRegisterImpl.kt

### 여기가 제일 과합니다 — 157-168줄 `geofenceResponsivenessFor` KDoc (12줄)

주석 하나가 12줄인데, 그중 **1문단은 Play Services 배칭 원리 설명**입니다. 원리는 궁금하면 문서를 찾을 수 있고, 찾을 수 없는 건 "그래서 어떻게 되는데"뿐이에요. 4문단(`dwellMinutes` 하한 가정)은 `minOf` 가 이미 말하고, "체류 목표 0 은 DWELL 을 안 쏜다"는 `GeofenceResponsivenessTest` 세 케이스가 이미 잡고 있습니다. 세 줄이면 충분합니다.

```kotlin
/**
 * 통지 지연 허용치. 지정하지 않으면 기본 0 이라 배칭이 꺼져 위치 하드웨어가 상시 깨어 있다.
 * 늦게 배달돼도 전이 시각은 fix 시각(`location.time`)에서 오므로 판정은 안 흔들린다.
 * 다만 [loiteringDelayMillis] 를 넘기면 DWELL 이 체류 임계보다 늦게 잡혀 체류 목표가 짧은 방이 손해다.
 */
```

**154줄 상수 주석**도 뒷문장("30분 주기 전송보다 한참 짧아…")이 위 KDoc 2문단과 겹칩니다. `// 배칭 허용 기본치 5분(Google 권고 수준).` 만 남기면 됩니다.

### 20-26줄 클래스 KDoc — 1문단 통째로 뺍니다

```
GeofencingClient 로 활성 좌표를 OS 에 사전 등록(zero-touch presence, 명세 §2.1).
reconcile 은 차집합만 제거하고 새 목표 전체를 멱등 등록한다(재부팅 후 전부 재등록).
```

이 두 줄은 **이미 두 군데에 더 있습니다.**

- `GeofenceRegister.kt:5-9` (도메인 포트 KDoc) — zero-touch presence, 명세 §2.1, 휘발 시 재등록
- `GeofenceReconcile.kt:5-10` — 차집합/멱등 등록/재부팅 후 전부 재등록

같은 사실이 세 곳에 있으면 하나가 낡을 때 나머지가 거짓말이 됩니다. 계약은 포트에 있으니 구현 KDoc 은 **구현에만 있는 사실**만 남깁니다. 즉 `@SuppressLint` 근거만 남습니다.

```kotlin
/** 지오펜싱 호출이 모두 [Context.hasFineLocation] 가드 뒤에 있고 SecurityException 도 삼키므로 MissingPermission 을 억제한다. */
@SuppressLint("MissingPermission")
```

이 근거는 **절대 지우면 안 되는 쪽**입니다. "위험해 보이는데 의도된 것"이라, 없으면 다음 사람이 억제를 풀고 lint 를 고치려다 동작을 바꿉니다. 두 파일 통틀어 값어치 대비 가장 짧은 주석이에요.

### 나머지

| 위치 | 주석 | 판정 |
|---|---|---|
| 39 | 권한 없으면 목표만 보존 | **줄여서 유지** — 코드가 `persist` + `return` 으로 절반 말합니다. 남는 건 "허용 후 다시 불린다"는 이 파일 밖 사실이에요. → `// 허용 후 reconcile 이 다시 부른다 — 목표만 남겨 둔다.` |
| 65 | bind 의 prefix 처리 | **뺍니다** — 앞 절반은 바로 아래 3줄(`byRequestIdPrefix` → `filterNot { it in newIds }`)의 되풀이고, 뒤 절반("다른 멤버 목표는 유지")은 포트 KDoc(`GeofenceRegister.kt:21-22`)에 이미 있습니다. 다만 여기 절 번호(§5.4.3)가 포트의 §5 보다 정확하니, **번호만 포트 쪽으로 올리고** 여기선 지우는 게 낫습니다. |
| 98-99 | registerAll 성공/실패 처리 | **유지**(살짝 압축) — `onSuccess`/`onFailure` 이름이 절반 말하지만, "왜 삼키는가(다음 reconcile 이 재시도)"와 §0.5·§0.7 근거는 코드에 없습니다. 두 줄 상한 안이에요. |
| 131 | `DWELL 이 OS 에서 직접 쏜다` | **유지** — 경계선이지만, "앱에서 체류를 직접 재지 않는다"는 대안 배제라 값어치가 있습니다. 지금 문장이 그 뉘앙스를 덜 살리니 `// 체류는 앱에서 재지 않는다 — OS 가 DWELL 로 직접 쏜다(명세 §2.1).` 정도가 낫습니다. |

---

## 정리

- **지운다**: 클래스 KDoc 1문단(3중 중복), `bind` 의 65줄, 154줄 뒷문장, `geofenceResponsivenessFor` KDoc 1·4문단
- **줄인다**: 리시버 37-38 → 한 줄, 61 → 한 줄, `reconcile` 39 → 한 줄
- **지키다**: `@SuppressLint` 근거, `registerAll` 의 §0.5·§0.7, `accuracy`/`isMock` null 유지 이유
- **먼저 정한다**: 리시버 KDoc 의 시각 두 값 — 주석을 고칠지 코드를 고칠지 (§6.4 확인 필요)

전체적으로 "주석이 많다"기보다 **같은 사실이 포트·Reconcile·엔티티·구현 네 군데에 흩어져 있는 것**이 문제입니다. 계약은 포트에, 결측 의미는 엔티티에 두고 구현에는 구현에만 있는 이유만 남기면 자연스럽게 절반으로 줄어듭니다.

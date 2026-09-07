# RuleUp 자동인증(Verification) 수동 테스트 시나리오

> 작성 기준: develop 브랜치, 현재 코드 직접 검증. 디바이스/에뮬레이터(API 26+ 권장) 필요.
> ⚠️ 먼저 **0. 사전 준비**와 **B. 현재 막혀 있는 경로**를 읽으세요. 일부 신호(지오펜스·부팅·Health)는 코드/매니페스트 누락으로 **앱 UI로는 E2E 테스트가 불가**합니다.

---

## 0. 사전 준비

### 0-1. 빌드/환경
- [ ] `BASE_URL`에 `/api` 반영됐는지 확인(`local.properties` → `https://staging-api.ruleup.co.kr/api`) 후 **재빌드 + 재설치**.
- [ ] 소셜 로그인 정상 동작(앞서 수정 완료) → 토큰 확보 상태.
- [ ] 자동인증 루틴이 있는 **챌린지를 1개 생성/참여**해 둔다(생성 플로우가 AUTO면 위치/활동 권한을 요청함). `challengeId`를 메모해 둔다(딥링크·상세 테스트에 필요).

### 0-2. 권한 부여 경로 (중요)
| 권한 | 용도 | 부여 방법 (현재) |
|---|---|---|
| ACCESS_FINE_LOCATION | 위치 샘플·지오펜스 등록 | 챌린지 생성 AUTO 플로우에서 **자동 요청** |
| ACTIVITY_RECOGNITION / CAMERA | (AUTO) | 챌린지 생성에서 자동 요청 |
| GET_USAGE_STATS (사용기록 접근) | 스크린타임 | **인앱 UI 없음** → adb 우회: `adb shell appops set com.ruleup.android_ruleup GET_USAGE_STATS allow` |
| ACCESS_BACKGROUND_LOCATION | 앱 종료중 지오펜스 | **매니페스트 미선언 → 부여 불가** (B 참고) |
| Health Connect READ_* | HEALTH/SLEEP | **매니페스트 미선언 + 그랜트 UI 없음 → 부여 불가** (B 참고) |

### 0-3. 유용한 adb 명령
```bash
# HTTP 로그 보기 (Timber Tree 심은 뒤부터 보임)
adb logcat -s HttpClient:D

# 딥링크로 화면 진입 (인앱 진입 동선이 없는 화면용)
adb shell am start -a android.intent.action.VIEW -d "ruleup://app/verification/progress" com.ruleup.android_ruleup
adb shell am start -a android.intent.action.VIEW -d "ruleup://app/verification/detail?challengeId=<ID>" com.ruleup.android_ruleup
adb shell am start -a android.intent.action.VIEW -d "ruleup://app/verification/manual?challengeId=<ID>" com.ruleup.android_ruleup
adb shell am start -a android.intent.action.VIEW -d "ruleup://app/verification/location?challengeMemberId=<ID>&defaultRadiusM=100&dwellMinutes=60" com.ruleup.android_ruleup

# 주기 sync 워커 강제 실행 (PeriodicWork는 즉시 안 돎)
adb shell dumpsys jobscheduler | findstr ruleup        # JOB_ID 찾기 (Windows: findstr / *nix: grep)
adb shell cmd jobscheduler run -f com.ruleup.android_ruleup <JOB_ID>

# 위치 모킹: 에뮬레이터 Extended controls → Location, 또는 개발자옵션 '모의 위치 앱 선택'
```
> 딥링크 URL에 `&`가 있으면 셸이 잘라먹으니 **URL 전체를 따옴표로 감싸세요**(위 예시처럼).

---

## A. 지금 검증 가능한 시나리오

각 항목: **사전조건 / 절차 / 기대결과**. `HttpClient` 로그로 요청·응답을 함께 확인.

### TC-01. Sync 네트워크 호출 & 페이로드
- 사전조건: 위치 권한 허용, 네트워크 연결.
- 절차: `cmd jobscheduler run`으로 sync 워커 강제 실행.
- 기대결과:
  - `POST .../api/v1/verifications/sync` → **200**.
  - 요청 바디에 `signals` 배열, 최소 `LOCATION` 1건(위치 샘플). 응답에 `nextSyncAfterSec`.
  - 성공 후 다음 주기가 응답값으로 reschedule(`UPDATE`).

### TC-02. 위치 샘플 수집 + isMock
- 사전조건: 위치 권한 허용.
- 절차: (a) 정상 위치에서 sync 1회. (b) 모의 위치 켜고 sync 1회.
- 기대결과: 페이로드 `LOCATION` 이벤트의 `lat/lng/accuracy/isMock` 정상. 모의 위치일 때 **`isMock=true`** 전송(안티치트 핵심).

### TC-03. 스크린타임/Usage 수집 (adb 우회)
- 사전조건: `adb shell appops set ... GET_USAGE_STATS allow` 후 앱 재실행.
- 절차: 앱 몇 개 전환(RESUMED/PAUSED), 화면 잠금/해제 후 sync.
- 기대결과: 페이로드에 `SCREEN_TIME`(AppUsageEvent·ScreenEvent). 커서가 증분 갱신돼 다음 sync에 중복 미전송.
- 참고: 인앱 그랜트 UI는 없음(0-2). appop 미허용 시 `SCREEN_TIME`은 비어야 정상.

### TC-04. Progress 화면 렌더
- 사전조건: 챌린지 존재, sync 1회 이상(progress 캐시 채움).
- 절차: 딥링크 `verification/progress` 진입.
- 기대결과:
  - 카드별 제목 / "진행률 N%" / 오늘 상태 라벨("진행 중"/"인증 완료"/"인증 실패"/"오늘은 대상일 아님") / "남은 N일".
  - 마지막 sync가 **2시간 초과(또는 없음)면** "⚠️ 신호 미수신 — 권한을 점검해주세요" 배지.
  - 카드 탭 → 상세로 이동.

### TC-05. Detail 화면 — TodayStatus별 렌더 (핵심: PENDING≠실패)
- 사전조건: 백엔드가 각 상태를 반환하도록 데이터 구성(또는 가능한 케이스부터).
- 절차: 딥링크 `verification/detail?challengeId=<ID>` 또는 Progress 카드 탭.
- 기대결과:
  - **SUCCESS**: "인증 완료" + verifiedAt + 증거(dwell/usage/firstUnlock).
  - **FAILED**: `failureReason` 한글 카피 + 조건부 CTA 버튼.
  - **PENDING**: "아직 진행 중이에요. 창이 닫히면 확정돼요." — **빨강/실패로 보이면 버그**.
  - **NOT_TARGET**: "오늘은 이 챌린지의 대상일이 아니에요."
  - 미지원 방식은 "(이 기기 미지원)" 표기, 하단 최근 dailyLogs 표시.

### TC-06. Detail CTA 라우팅
- 절차: FAILED 상태에서 reason별 CTA 탭.
- 기대결과:
  - `NO_SIGNAL_RECEIVED` / `PERMISSION_MISSING` → "권한 설정으로" → **앱 설정 화면** 오픈.
  - `GEOFENCE_NOT_CONFIGURED` → "지도 핀 설정으로" → **위치 핀 화면**(`verification/location`) 이동.
  - 그 외 reason → CTA 버튼 없음.

### TC-07. 수동 제출 — 자가 체크(SELF_CHECK)
- 사전조건: 챌린지 존재, 오늘 미인증.
- 절차: 딥링크 `verification/manual?challengeId=<ID>` → "자가 체크로 인증".
- 기대결과: `POST .../api/v1/challenges/<ID>/verifications` 200 → "오늘 인증을 제출했어요" → 뒤로. "사진으로 인증(준비 중)" 버튼은 **비활성**.

### TC-08. 수동 폴백 + 주간 한도
- 절차: "오늘은 수동으로 증명하기"(asFallback=true) 제출. 한도 초과까지 반복.
- 기대결과: 정상 시 "오늘은 수동으로 잠정 인증했어요. 이의가 없으면 확정돼요"(verifiedVia=MANUAL_FALLBACK, disputeClosesAt). 한도 초과 시 **409 FALLBACK_LIMIT_EXCEEDED** → "이번 주 수동 인증을 모두 사용했어요".

### TC-09. 중복 인증(409 ALREADY_VERIFIED)
- 절차: 오늘 이미 인증된 챌린지에 수동 제출 재시도.
- 기대결과: **409 ALREADY_VERIFIED** → "오늘은 이미 인증했어요"가 **에러가 아닌 안내**로 표시.

### TC-10. 장소 검색(NEARBY_BRAND) + 핀 바인딩
- 절차: 딥링크 `verification/location?...` → 검색창에 "스포애니" 등 입력 → 검색 → 결과 탭 → 지도 핀/반경 조정 → 확인.
- 기대결과: `GET .../api/v1/places/search?q=...&lat=...&lng=...&radiusM=...` 200, 좌표 없는 결과는 제외, 최대 10개. 결과 탭 시 지도 recenter. 반경 50~1000m로 클램프. 확인 시 바인딩 완료 메시지.

### TC-11. Sync 에러/백오프
- 절차: (a) 연속으로 sync 강제 실행(서버 429 유도). (b) 비행기모드에서 워커 실행 후 네트워크를 복구하고 다시 실행.
- 기대결과:
  - **429 SYNC_TOO_FREQUENT** → markSynced 안 함(데이터 보존) + 워커 retry.
  - **400 INVALID_SIGNAL_PAYLOAD** → 해당 배치 discard(markSynced) 후 재전송 안 함(무한루프 방지).
  - 오프라인 → 네트워크 constraint로 미실행/대기, 복구 후 재시도.
  - **실패분 재전송(핵심)**: (b) 에서 실패한 신호가 **다음 sync 요청 본문에 다시 실려 나가야 한다.**
    버퍼에 남아 있기만 하고 다음 배치에 안 실리면 그 신호는 영영 판정에 못 들어간다(#319).
    확인법 — 실패 직후 요청의 신호 건수와 복구 후 요청의 신호 건수를 비교한다. 뒤쪽이 앞쪽을 포함해야 한다.

### TC-12. Render 규칙 추가 확인
- 절차: 다양한 상태로 Progress/Detail 재확인.
- 기대결과: PENDING·NOT_TARGET은 **어디서도 실패로 렌더되지 않음**. stale 배지는 lastSyncedAt 기준 2시간 경계에서 토글.

---

## B. 현재 막혀 있어 E2E 불가 (코드/매니페스트 보강 전엔 테스트 불가)

> 직접 확인함: `verification/` 모듈에 **AndroidManifest가 없고**, 앱 매니페스트에도 아래 리시버/권한이 **선언돼 있지 않음**(app 매니페스트 권한 = INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK 뿐).

| 막힌 시나리오 | 원인 | 풀려면 |
|---|---|---|
| 지오펜스 ENTER/EXIT/DWELL 발화 | `GeofenceBroadcastReceiver`가 어떤 매니페스트에도 **미등록** | 앱 매니페스트에 `<receiver>` + PendingIntent 대상 등록 |
| 재부팅 후 지오펜스 재등록 | `BootReceiver` 미등록 + `RECEIVE_BOOT_COMPLETED` 미선언 | 리시버 + 권한 선언 |
| 앱 종료 상태 지오펜스 | `ACCESS_BACKGROUND_LOCATION` 미선언/미요청 | 권한 선언 + 백그라운드 위치 그랜트 UI |
| HEALTH / SLEEP 수집 | Health Connect `READ_*` 권한 **매니페스트 미선언** + 인앱 그랜트 UI 없음 | HC 권한 선언 + `requestPermissions` 런처 |
| 인앱 권한 부여 UX | 사용기록·백그라운드위치·HC 모두 **요청 화면 없음** | 각 권한 요청/딥링크 UI |
| 생성→스코프 연동 | 챌린지 생성이 `HealthTargetStore`/`UsageTargetStore` 미설정, `BindLocation`/`RegisterGeofences` 미호출 | 생성 완료 시 스코프 채우기 + 지오펜스 등록 |

→ 이 경로들을 테스트하려면 먼저 **매니페스트/권한/스코프 배선 작업**이 필요합니다. 원하면 이 부분 패치도 도와줄 수 있습니다.

---

## C. 자동 테스트로 이미 커버됨 (수동 재검 불필요)

JVM 단위 테스트로 검증된 로직 — 수동 QA에서 반복할 필요 없음:
- `RunSyncUseCaseTest` — sync 흐름(빈 배치→미전송 / 200→markSynced+purge / 400→discard / 429→보존)
- `VerificationCopyTest` — §6.4 렌더 규칙(PENDING/NOT_TARGET≠실패, CTA 라우팅, stale 2h, ISO 파싱)
- `VerificationRepositoryImplTest` — 수동 제출 매핑(409 ALREADY/FALLBACK_LIMIT, fallback verifiedVia/disputeClosesAt)
- `VerificationDtoSerializationTest` — DTO 직렬화(geofence/HEALTH/SLEEP, epoch→ISO, nextSyncAfterSec 기본 1800, unknown→PENDING, 좌표 필터)
- `SignalEntityMapperTest`·`UsageEntityMapperTest`·`UsageSignalMappingTest` — 엔티티/이벤트 매핑, isMock 보존, unknown enum 폴백
- `GeofenceReconcileTest` — 지오펜스 diff-set 제거
- `SyncOutcomeTest`·`BindLocationUseCaseTest` — 예외→결과 매핑, 반경 클램프

> 단위 테스트 한 번 돌려두면 좋음: `./gradlew :verification:data:testDebugUnitTest :verification:domain:test :verification:presentation:testDebugUnitTest`
> (※ 미커버: 모든 ViewModel/Compose 화면, 실제 신호 수집, Room DAO 통합, WorkManager 스케줄 — 수동 QA 대상.)
</content>

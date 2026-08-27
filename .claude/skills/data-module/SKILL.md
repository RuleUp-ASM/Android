---
name: data-module
description: RuleUp Android 레포에서 data 레이어(api·dto·repository·di)를 구현하는 절차. 서버 API 를 앱에 붙이는 모든 작업 — 새 엔드포인트 연동, Retrofit 인터페이스 추가, Request/Response DTO 작성, Repository 구현체와 Hilt 바인딩, 새 feature 의 `:<feature>:data` 모듈 신설 — 에 사용한다. "API 붙여줘", "서버 연동", "엔드포인트 추가", "DTO 만들어줘", "레포지토리 구현해줘", "응답 매핑", "data 모듈" 같은 말이 나오면 코드를 쓰기 전에 반드시 이 스킬을 먼저 읽는다. 응답 DTO 의 nullable 규칙, 매핑 함수의 위치, 개념 폴더 분리 기준 같은 이 레포 고유의 계약이 여기에만 있고, 모르고 짜면 리뷰에서 통째로 되돌아온다. 화면·ViewModel·UseCase 작업이어도 그 과정에서 API 호출부가 새로 필요해지면 이 스킬을 읽고 data 레이어를 만든다.
---

# data 레이어 만들기

## 진행 방식: 바로 구현한다

**확인을 기다리지 않는다.** 배치를 정하고 파일까지 쓴 뒤, 무엇을 어디에 뒀고 왜 그렇게 판단했는지를
답변에 요약해 남긴다. 사용자는 그 요약과 코드를 같이 보고 고칠 곳을 짚는다 — 승인 턴을 한 번 더
받는 것보다 이쪽이 빠르고, 틀려도 diff 한 줄이다.

빈칸이 있어도 멈추지 않는다. **안전한 쪽으로 가정하고 진행하되, 가정을 코드 주석과 답변 양쪽에
남긴다.** 침묵하는 추측만 금지다 — 드러낸 가정은 사용자가 한 줄로 반박할 수 있다.

멈춰야 하는 경우는 하나뿐이다: **답이 달라지면 코드를 다시 짜야 하는 질문.** 예를 들어 응답 JSON
예시가 아예 없거나, 같은 엔드포인트를 새 Api 로 뺄지 기존 Api 에 붙일지가 계약상 갈리는 경우.
이때도 "물어보고 대기"가 아니라 **한쪽을 골라 구현하고 다른 선택지를 답변에 적는다.**

### 시작 전 확인 (읽기로 끝나는 것들)

물어볼 필요 없이 레포에서 바로 확인된다:

- 대상 feature 의 `:<feature>:data` 모듈이 있는지 (`settings.gradle.kts`)
- `:<feature>:domain` 에 Repository 인터페이스·entity 가 있는지. 없으면 **함께 만든다**
  (data 는 domain 계약을 구현하는 어댑터라 domain 없이는 컴파일되지 않는다)
- 비슷한 응답을 이미 다루는 DTO 가 있는지 — 페이징·enum·에러 코드 처리 방식을 그대로 따른다

### 배치 결정

**개념이 2개 이상이면 개념 폴더로 나눈다.** 여기서 "개념"은 서로 다른 리소스 묶음이다 —
`auth`(로그인·가입·토큰)와 `intro`(인트로 화면 데이터)처럼 같이 바뀌지 않는 것들.

```
# 단일 개념 — flat (challenge/data, profile/data, verification/data)
<feature>/data/
├── api/<Concept>Api.kt
├── dto/<Concept>Request.kt, <Concept>Response.kt
├── repository/<Name>RepositoryImpl.kt
└── di/

# 개념 2개 이상 — 개념 폴더 (onboarding/data)
<feature>/data/
├── auth/{api,dto,repository}/
├── intro/{api,dto,repository}/
└── di/                       ← di 는 언제나 모듈 루트. 개념별로 쪼개지 않는다.
```

- `di/` 를 루트에 두는 이유: Hilt 모듈은 개념이 아니라 **모듈 단위 조립**이다.
  `<Feature>NetworkModule` 하나가 그 모듈의 모든 Api 를 제공하는 편이 찾기 쉽다.
- **기존 flat 모듈을 개념 폴더로 재편하지 않는다.** 최소 변경 원칙 — 이미 flat 인 모듈에 개념이
  하나 더 붙어도 flat 을 유지하고, 재편이 낫겠다는 판단만 답변에 한 줄 남긴다. 임의로 옮기면
  리뷰 diff 가 본질을 덮는다.

### 새 엔드포인트를 어느 Api 에 둘까

**기본은 기존 Api 에 함수를 추가하는 것이다.** Repository 마다 Api 를 따로 두지 않는다 —
Retrofit 인터페이스는 **서버 계약의 목록**이지 소비자별 뷰가 아니다. 실제로 `ChallengeApi` 하나가
엔드포인트 30개를 갖고 `ChallengeRepositoryImpl`·`ExploreRepositoryImpl`·`RoomRepositoryImpl`·
`WatcherRepositoryImpl` 넷이 나눠 쓴다.

**URL prefix 가 다르다고 쪼개지 않는다.** `ChallengeApi` 에 `v1/challenge-categories` 와
`v1/rankings/challenges` 가 같이 있고, `VerificationApi` 에는 `v1/verifications/*` 와
`v1/challenges/{id}/verification` 이 섞여 있다. 기준은 경로가 아니라 **어느 feature 의 책임인가** 다.

새 Api 인터페이스를 만드는 경우는 둘뿐이다:

1. **서버·인증이 다르다** → 별도 Api + 별도 Retrofit/OkHttp 가 반드시 필요하다.
   `KakaoLocalApi`(`dapi.kakao.com`, `KakaoAK` 헤더)가 그 예다. 공용 클라이언트를 재사용하면
   인터셉터가 `Authorization: Bearer …` 를 덮어써 KakaoAK 헤더가 지워진다.
2. **개념 폴더를 새로 만들 때** → 폴더가 갈리면 Api 도 갈린다 (`auth/api/AuthApi`,
   `intro/api/IntroApi`).

flat 모듈에 Api 가 이미 둘 이상이면 **성격이 맞는 쪽에 붙인다.** `profile/data` 는 `MyPageApi`
(마이 탭 조회 대시보드 — `v1/me/*`)와 `ProfileApi`(신원·편집 — `v1/profile`, `v1/nicknames/check`)
로 갈려 있다. 둘 다 같은 Retrofit 을 쓰므로 이 분리는 **읽는 사람을 위한 것**이지 기술적 제약이 아니다.

애매하면 멈추지 말고 **한쪽을 골라 붙인 뒤 근거와 대안을 답변에 남긴다.** 조회만 있는 동안은
기존 Api 에 얹고, 나중에 그 리소스의 쓰기 엔드포인트(생성·수정·차단 등)가 붙어 무게가 실리면
그때 전용 Api 로 뗀다. 처음부터 Api 를 하나 더 만들어 두면 함수 한 개짜리 인터페이스와
DI 바인딩만 늘어난다.

### 작업 후 답변에 남길 요약

구현을 마친 뒤 이 형식으로 보고한다. 배치는 파일 목록만 봐도 알지만, **매핑 판단과 가정은
코드를 정독해야 드러나므로** 표로 뽑아 준다 — 사용자가 반박할 지점이 바로 여기다:

```markdown
## 배치
challenge/data/
├── api/RankingApi.kt              (신규)
├── dto/RankingResponse.kt         (신규) — RankingItemResponse, RankingResponse + toDomain()
├── repository/RankingRepositoryImpl.kt (신규)
└── di/ChallengeNetworkModule.kt   (수정) — provideRankingApi 추가
    di/ChallengeRepositoryModule.kt (수정) — bindRankingRepository 추가

## domain 쪽 전제
:challenge:domain 에 RankingRepository 인터페이스 + Ranking entity 필요 (없으면 함께 정의)

## 응답 필드 → entity 매핑
| 응답 필드 | 타입 | 누락 시 | 근거 |
|---|---|---|---|
| challengeId | String? | requireField | 없으면 어떤 방인지 특정 불가 |
| rank | Int? | 배열 index+1 | 서버가 정렬해 내려줌 |
| score | Int? | 0 | 표시용 집계값 |

## 가정한 것 (틀리면 알려주세요)
- 커서 파라미터가 명세에 없어 단일 조회로 구현. 페이징이 있으면 cursor/size 추가 필요
```

## 구현 규칙

### api/`<Concept>`Api.kt

```kotlin
interface RankingApi {
    // 그룹 랭킹 조회. base(.../api/) + v1/... → /api/v1/challenges/{id}/ranking
    @GET("v1/challenges/{challengeId}/ranking")
    suspend fun getRanking(
        @Path("challengeId") challengeId: String,
        @Query("period") period: String? = null,
    ): BaseResponse<RankingResponse>
}
```

- 모든 함수는 `suspend`, 반환은 **항상 `BaseResponse<T>`** (`com.ruleup.network.dto`).
  본문 없는 응답(`success` 만 오는 경우)은 `BaseResponse<EmptyData>`.
- 경로는 **선행 슬래시 없이** `v1/...` 으로 쓴다. base URL 이 `.../api/` 로 끝나므로
  `/v1/...` 로 쓰면 `/api` 가 잘려 나간다.
- 각 함수 위에 한 줄 주석으로 실제 최종 경로를 남긴다. base URL 조합이 눈에 안 보이기 때문이다
  (comments 스킬 ② — 코드 밖에 있는 계약).

### dto/`<Concept>`Request.kt

요청은 **서버 계약을 그대로 반영한다** — 필수 필드는 non-null, 선택 필드만 nullable + `= null`.
응답과 규칙이 반대인 이유: 요청은 우리가 만들어 보내는 값이라 컴파일 타임에 강제할 수 있고,
필수 필드를 nullable 로 두면 빠뜨린 채 400 을 받고서야 알게 된다.

domain → request 변환(`internal fun SignupForm.toRequest(): SignUpRequest`)은 **이 파일 안에** 둔다.

주의: `Json(encodeDefaults=false)` 라 **값이 기본값과 같으면 직렬화에서 빠진다.**
`platform = "ANDROID"` 같이 항상 보내야 하는 상수 필드에 기본값을 주면 필드가 통째로 사라진다.

### dto/`<Concept>`Response.kt — 핵심 규칙 두 개

**① 응답 필드는 예외 없이 전부 nullable + `= null`.**

서버는 필드를 언제든 빼거나 늦게 채운다. non-null 로 선언하면 필드 하나 누락에
`SerializationException` 이 터져 화면 전체가 죽는다. nullable 이면 그 값만 없는 상태로 흐르고,
어디까지 허용할지는 아래 매핑 함수에서 우리가 정한다.

**② 매핑 함수는 DTO 와 같은 파일에, DTO 선언 아래에 둔다.**

응답 필드를 고치면 매핑도 반드시 같이 고쳐야 한다. 파일이 갈리면 한쪽만 고친 채 컴파일이
통과해 버린다. 같은 파일에 있으면 필드를 지우는 순간 매핑이 빨갛게 뜬다.

```kotlin
@Serializable
data class RankingItemResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("tier")
    val tier: String? = null,
    @SerialName("score")
    val score: Int? = null,
)

internal fun RankingItemResponse.toDomain(index: Int): RankingItem =
    RankingItem(
        challengeId = challengeId.requireField("challengeId"),
        // rank 가 비면 배열 순서로 보정한다 — 서버가 이미 정렬해 내려준다.
        rank = rank ?: (index + 1),
        nickname = nickname.orEmpty(),
        tier = Tier.fromValue(tier),
        score = score ?: 0,
    )
```

- `@SerialName` 은 프로퍼티명과 같아도 **항상 명시한다.** 난독화·리팩터링에도 와이어 이름이 남는다.
- 매핑 함수는 `internal`. data 모듈 밖에서 DTO 를 만질 일은 없다.
- 여러 응답이 한 개념이면 파일 하나에 모으고 `// ---------- 탐색: 실시간 인기 ----------` 같은
  구분 주석으로 나눈다. 요청·응답이 섞인 공용 조각은 `<Name>Dto.kt`.

### null 을 entity 로 옮기는 기준

nullable 을 non-null entity 로 옮길 때 **필드마다 판단한다.** 기계적으로 하나를 고르지 않는다:

| 성격 | 처리 | 예 |
|---|---|---|
| 식별자 — 없으면 그 객체가 성립 불가 | `requireField("필드명")` | `challengeId`, `user.id` |
| 표시용 문자열 | `orEmpty()` / nullable 유지 | `title`, `imageUrl` |
| 카운트·점수 | `?: 0` | `participantCount` |
| enum | `fromValue(...)` 폴백 (`?: 기본값`) | `Tier`, `VerificationType` |
| 권한·가능 여부 | **안전한 쪽으로** | `joinable = joinable ?: false` |

마지막 줄이 제일 중요하다. **모르는 값은 사용자에게 유리한 쪽이 아니라 안전한 쪽으로 떨어뜨린다** —
못 들어갈 방을 열려 있는 것처럼 보여주면 사용자는 눌렀다가 에러를 본다.

enum 을 `requireField` 로 막지 않는 이유도 같다. 서버가 enum 값을 하나 추가하는 순간
구버전 앱이 통째로 막힌다. 모르는 값은 기본값으로 흘려보내고 화면은 계속 돈다.

`requireField` 는 `ApiException(code = "RESPONSE_FIELD_MISSING")` 을 던진다 — 즉
**서버 에러와 같은 통로로 흐른다.** 호출부가 따로 처리할 필요가 없다.

### repository/`<Name>`RepositoryImpl.kt

```kotlin
class RankingRepositoryImpl
    @Inject
    constructor(
        private val api: RankingApi,
    ) : RankingRepository {
        override suspend fun getRanking(challengeId: String): Ranking =
            api.getRanking(challengeId).getOrThrow().toDomain()

        override suspend fun leave(challengeId: String) {
            api.leave(challengeId).throwOnError()
        }
    }
```

- `getOrThrow()` 는 data 를 꺼내거나 `ApiException` 을 던진다. `EmptyData` 응답은 `throwOnError()`.
- Impl 은 얇게 유지한다 — 호출 → `getOrThrow()` → `toDomain()`. 분기 로직은 domain 으로 올린다.
- **feature 가 에러 코드로 화면을 가르는 경우에만** 예외를 도메인 타입으로 번역한다
  (`AuthRepositoryImpl` 의 `mapAuthFailure` 참고). 이때 `IOException`(네트워크)은 서버 에러와
  구분한다 — 화면이 "다시 시도"를 권할지 "로그인부터 다시"를 권할지가 갈린다.
  코드 매핑 표는 Response 파일에 `internal fun ApiException.toXFailure()` 로 둔다.
- 클래스 이름은 `*RepositoryImpl`. 아키텍처 테스트가 이 이름을 data/datastore 모듈로 강제한다.
  네트워크가 아닌 로컬 저장소는 `*StoreImpl` 을 쓴다(`TargetAppStoreImpl`).

### di/

`<Feature>NetworkModule` — Retrofit 인스턴스에서 Api 를 만든다. `object` + `@Provides`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ChallengeNetworkModule {
    @Provides
    @Singleton
    fun provideRankingApi(retrofit: Retrofit): RankingApi = retrofit.create()
}
```

`<Feature>RepositoryModule` — 구현체를 domain 인터페이스에 묶는다. `abstract class` + `@Binds`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ChallengeRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRankingRepository(impl: RankingRepositoryImpl): RankingRepository
}
```

모듈당 이 두 개면 충분하다. Api·Repository 가 늘어나면 **새 파일을 만들지 말고 함수를 추가한다.**
`OkHttp`/`Retrofit`/`Json` 자체는 `core:network` 가 이미 제공하므로 다시 만들지 않는다.

## 새 `:<feature>:data` 모듈을 만드는 경우

`references/new-module.md` 를 읽는다 — `build.gradle.kts` 템플릿, `settings.gradle.kts` include,
`:app` 의존 추가까지 빠뜨리기 쉬운 순서가 정리돼 있다.

## 마무리

1. `./gradlew ktlintFormat` — 후행 콤마·import 정렬을 자동으로 맞춘다. 안 돌리면 CI 가 막는다.
2. `./gradlew :<feature>:data:assembleDebug` 로 컴파일 확인.
3. domain 에 Repository 인터페이스를 새로 만들었다면, **단일 repository 위임에는 UseCase 를
   만들지 않는다** — ViewModel 이 Repository 를 직접 주입받는다 (루트 `CLAUDE.md` 참고).
4. 매핑 분기가 복잡해졌으면(폴백이 3개 이상, 조건부 분기) 직렬화 라운드트립 테스트를 함께 쓴다.
   `verification/data/src/test/.../VerificationDtoSerializationTest.kt` 가 본보기다.
   기본은 미작성 — 필요할 때만 권한다.

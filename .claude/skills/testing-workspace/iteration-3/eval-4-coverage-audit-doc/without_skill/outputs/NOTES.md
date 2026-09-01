# NOTES

`TEST_STRATEGY.md` 를 어떻게 만들었고, 무엇을 넣고 뺐는지.

## 어떻게 공백을 찾았나

Gradle 은 실행하지 않았다(요청 제약). 그래서 **실행 커버리지가 아니라 소스 대조**로 감사했다. 순서는 이렇다.

1. **모듈 전수 확보** — `settings.gradle.kts` 의 `include` 22개를 기준으로 삼았다. 디렉터리를 훑는 방식은 `build/` 산출물에 걸리므로 쓰지 않았다.
2. **소스 대 테스트 수치** — 모듈마다 `src/main` 과 `src/test`·`src/androidTest` 의 `.kt` 파일 수를 세어 §1.2 표를 만들었다. 이건 어디를 **먼저 들여다볼지** 고르는 용도로만 썼다. 파일 수는 커버리지가 아니고, 그렇게 문서에도 명시했다.
3. **테스트 **인프라** 를 먼저 읽었다 — 이게 가장 큰 소득이었다.** 22개 모듈의 `build.gradle.kts` 에서 `testImplementation`/`androidTestImplementation`/`testFixtures` 를 전부 뽑고, `gradle/libs.versions.toml` 에서 테스트 라이브러리를 확인하고, `.github/workflows/*` 에서 실제로 도는 Gradle 태스크를 확인했다. 여기서 나온 것:
   - CI 가 `./gradlew test` 만 돌아 **`androidTest` 는 아무도 실행하지 않는다.**
   - mockk/robolectric/turbine 이 **카탈로그에 아예 없다.**
   - presentation 모듈에 `kotlinx-coroutines-test` 가 없고, 두 모듈은 테스트 의존이 0줄이다.
   → **"테스트가 없다" 와 "테스트를 쓸 수가 없다" 가 다른 문제**라는 게 여기서 갈렸고, 그게 문서 P0 의 골격이 됐다.
4. **종류별 전수 확보** — ViewModel·UseCase·RepositoryImpl·entity·navigation Page 를 이름 규칙으로 뽑아 기존 테스트 파일명과 대조했다.
5. **기존 테스트의 케이스 이름을 전부 읽었다** (`grep 'fun \`'`). 파일이 있다고 덮여 있는 게 아니기 때문이다. 이게 실제로 중복 계상을 막았다 — 예를 들어 `SyncResult.mergedWith` 는 직접 테스트가 없어 후보로 잡았다가, `RunSyncUseCaseTest` 의 `쪼갠 조각들의 갱신이 하나로 합쳐진다` 가 이미 덮고 있는 걸 보고 뺐다.
6. **위험 지점은 소스를 직접 읽었다.** `TokenAuthenticator`, `BaseResponse`, `MviViewmodel`, `NavRouteUriParser`, `AuthFailureUi`, `SyncResult`, `VerificationException`, 큰 ViewModel 들의 함수 목록.
7. **두 갈래는 병렬 서브에이전트로 파냈다** — (a) `core/network`+`core/datastore`+deeplink+MVI 베이스, (b) `challenge/data`+`profile/data` 의 매퍼·리포지토리. 폭이 넓고 파일당 읽을 양이 많은 구간이라 나눴다. 돌아온 결과 중 **load-bearing 한 것(`TokenAuthenticator` 분기, `BaseResponse`, `MviViewModel` 시그니처)은 내가 직접 원본을 다시 읽어 확인**하고 문서에 넣었다.

## 판단 — 무엇을 넣고 뺐나

**넣은 것**

- **"테스트를 쓸 수 없는 상태"를 "테스트가 없는 상태"보다 위에 뒀다.** `core/network` 에 테스트 의존이 0줄인 것은 케이스 하나가 빠진 것과 무게가 다르다. 후자는 쓰면 되지만 전자는 아무도 시도조차 못 한다.
- **잘 되고 있는 것(§1.3)을 먼저 적었다.** 이 저장소는 컨벤션이 뚜렷하다 — 한국어 문장 테스트명, "깨지면 무슨 일이 나는지"를 적는 주석, 모킹 라이브러리 없이 손으로 쓴 fake. 빈 곳을 채우는 사람이 새 양식을 발명하면 그게 더 나쁘다. `verification` 모듈을 명시적으로 기준점으로 지목했다.
- **감사 중 나온 의심 지점 하나를 ⚠️ 로 남겼다** — `TokenAuthenticator` 가 `Authorization` 헤더가 없는 요청(`/auth/oauth`·`/auth/signup`)에 Bearer 를 붙여 재시도할 수 있어 보이는 것. **버그라고 단정하지 않고 "코드만으로는 판단할 수 없으니 확인이 필요하다"로 적고 백로그 0단계에 뒀다.** CLAUDE.md 의 추측 금지 원칙 때문이고, 실제로 의도일 수 있다.
- **결정이 필요한 곳을 ❓ 로 표시하고 그 결정에 백로그 항목을 매달았다.** Robolectric 도입, `reduce` 가시성, `NetworkModule` 인터셉터 추출, `mode` 폴백 불일치. 문서가 몰래 대신 결정하지 않게 했다.
- **"안 하기로 한 것"(§6)을 명시적으로 적었다.** 나중에 "왜 여기만 비었지"를 다시 묻지 않기 위해서다. `verification/data` 의 프레임워크 수집기는 `VERIFICATION_TEST_PLAN.md` 의 수동 QA 가 이미 담당하고 있어서 뺐고, 그 사실을 적었다.
- **`androidTest` 를 "커버돼 있음"으로 세지 않았다.** CI 가 안 돌리므로 회귀를 못 막는다. 딥링크 테스트 7개가 여기 해당한다.

**뺀 것**

- **파일 수 기반 커버리지 퍼센트.** 계산할 수는 있었지만 fake·fixture 파일이 테스트 수에 섞여 있어 오해를 부르고, Gradle 을 못 돌려 검증할 수도 없었다.
- **"이 클래스에 테스트를 추가하라"식의 기계적 전수 목록.** entity 마다 한 줄씩 뽑으면 200줄짜리 무시당하는 표가 된다. 대신 **비자명한 로직이 실제로 든 것**만 이름을 대고 그 로직이 무엇인지 적었다. 예: `MyChallengeStoreImpl` 은 "테스트 없음"이 아니라 "역순 반환 + upsert 시 원래 순서 유지가 미검증".
- **`core:designsystem`(14파일)·`observability:debug`(3파일)·Hilt 모듈·Retrofit 인터페이스 선언.** 0테스트지만 빈 곳이 아니다. §1.2 표에서 ⚪ 로 구분하고 §6 에 근거를 적었다.
- **`observability:domain` 의 25파일을 P1 로 올리지 않았다.** 대부분 로직 없는 데이터 홀더고, 로직이 있는 `TtiTracker` 는 이미 잡혀 있다. 파일 수만 보면 24/25 가 빈 것처럼 보이는 자리라 일부러 짚었다.
- **구체적인 테스트 코드.** 요청은 전략 문서였고, 코드를 미리 써 두면 §5 의 ❓ 결정을 앞질러 버린다.
- **Compose UI 테스트 층 설계.** 지금 이 저장소에 존재하지 않는 층이라 "만들자"고 제안하는 대신 §6 에 ❓ 로 남겨 결정 사항으로 뒀다.

**애매했던 판단 두 개**

- `challenge/presentation` 을 🔴 로 표시했다. 테스트 파일이 4개 있지만 전부 ViewModel 밖으로 뺀 문구 헬퍼이고, 49개 파일 2,338줄의 오케스트레이션은 통째로 비어 있다. 다만 **그 우회 패턴 자체는 좋은 것**이라 §3.1-B 에 "유지할 값어치가 있다"고 명시했다 — 표의 🔴 만 보고 기존 테스트를 갈아엎지 않도록.
- `core/datastore` 를 처음엔 🟢 로 뒀다가 🟡 로 내렸다. 13케이스가 촘촘하지만, **`NetworkModule` 인터셉터가 의존하는 캐시 워밍 경로**와 `userId` Flow 가 비어 있다. 모듈 안에서 보면 잘 된 테스트인데 모듈 경계를 넘어 보면 구멍이 있는 사례라 별도 항목(§3.1-A-2)으로 뺐다.

## 확인하지 못한 것

- **실행 커버리지 미확인.** Gradle 을 돌리지 않았으므로 `./gradlew test` 가 지금 통과하는지, 어떤 라인이 실제로 실행되는지 모른다. "테스트 파일이 있다"까지만 확인했다.
- **`.claude/` 이하는 읽지 않았다**(작업 제외 범위). grep 결과에 경로가 스쳤을 뿐 내용은 쓰지 않았다.

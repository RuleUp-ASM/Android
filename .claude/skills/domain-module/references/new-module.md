# 새 `:<feature>:domain` 모듈 만들기

기존 모듈에 entity·계약을 추가하는 경우에는 이 문서가 필요 없다. `settings.gradle.kts` 에
`:<feature>:domain` 이 이미 있으면 SKILL.md 의 규칙만 따르면 된다.

## 순서

### 1. `settings.gradle.kts` 에 include 추가

```kotlin
include(":<feature>:domain")
```

같은 feature 의 `data`·`presentation` 옆에 붙여 둔다. 여기를 빼먹으면 Gradle 이 모듈 자체를 모르므로
IDE 에서만 멀쩡해 보인다.

### 2. `<feature>/domain/build.gradle.kts`

convention plugin 이 없어 매번 전문을 쓴다. `challenge/domain` 이 기준이다:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ruleup.<feature>.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    // Page/NavRoute, Category(공유 커널)가 본 모듈의 공개 시그니처에 노출되므로 api 로 전파한다.
    api(project(":core:domain"))
    // 비즈니스 이벤트 로깅. 내부 구현 세부라 implementation 으로 둔다.
    implementation(project(":observability:domain"))
    implementation(libs.kotlinx.coroutines.core)
    // UseCase 의 @Inject 생성자(런타임 Hilt 컴포넌트에서 제공). 도메인은 hilt 런타임 없이 annotation 만.
    implementation(libs.javax.inject)

    testImplementation(kotlin("test-junit"))
}
```

- `android.library` 플러그인을 쓰지만 **Android API 는 쓰지 않는다.** 아키텍처 테스트가
  `android.*`/`androidx.*` import 를 막는다. 이 플러그인은 feature 모듈 간 variant 정합
  (`testDebugUnitTest` 등)을 맞추기 위한 것이다.
  `:core:domain`·`:observability:domain` 은 예외로 순수 JVM(`jetbrains.kotlin.jvm`)이다 —
  variant 가 없어 테스트 태스크가 `test` 다.
- **hilt·ksp·serialization 플러그인을 넣지 않는다.** domain 은 `javax.inject` 어노테이션만 쓰고
  주입 그래프 조립은 `:app` 이 한다. `@Serializable` 이 필요하면 그건 DTO 이므로 data 로 가야 한다.
- `api` 와 `implementation` 을 구분한다 — **공개 시그니처에 노출되는 의존만 `api`.**
  `core:domain` 은 `Page`·`Category` 가 시그니처에 나오므로 `api`, `observability:domain` 은
  이벤트 팩토리 내부에서만 쓰이므로 `implementation`.
- 다른 feature 의 능력이 필요하면 그쪽 **`domain` 만** 의존한다
  (`onboarding:domain` → `api(project(":profile:domain"))`). `data`·`presentation` 은 금지 —
  아키텍처 테스트가 막는다.
- 테스트 픽스처가 필요하면 `testImplementation(testFixtures(project(":observability:domain")))`
  처럼 가져온다(`FakeClock`·`RecordingSink`·`TestObservability` 가 거기 있다).

### 3. 소스 디렉터리

```
<feature>/domain/src/main/kotlin/com/ruleup/<feature>/domain/
├── entity/
├── repository/
├── usecase/            ← 필요할 때만
├── navigation/
└── observability/

<feature>/domain/src/test/kotlin/com/ruleup/<feature>/domain/
├── entity/
├── usecase/
└── fake/               ← Fake 구현체 + 픽스처 팩토리
```

소스 루트는 `src/main/kotlin/`(`java/` 아님). `AndroidManifest.xml` 은 필요 없다.

### 4. 소비 모듈에 의존 추가

- `<feature>/data/build.gradle.kts` → `implementation(project(":<feature>:domain"))`
- `<feature>/presentation/build.gradle.kts` → `implementation(project(":<feature>:domain"))`
- `app/build.gradle.kts` → `implementation(project(":<feature>:domain"))`

`:app` 은 라우트 레지스트리에서 Page 를 참조하므로 domain 도 직접 의존한다.

### 5. 확인

```bash
./gradlew ktlintFormat
./gradlew :<feature>:domain:testDebugUnitTest
./gradlew :app:test            # 아키텍처 테스트(Konsist) — 레이어 위반을 여기서 잡는다
./gradlew :app:assembleDebug
```

`:app:test` 를 돌려야 "domain 이 androidx 를 import 했다" 같은 위반이 드러난다.
모듈만 빌드하면 그냥 통과한다.

# 새 `:<feature>:data` 모듈 만들기

기존 모듈에 API 를 추가하는 경우에는 이 문서가 필요 없다. `settings.gradle.kts` 에
`:<feature>:data` 가 이미 있으면 SKILL.md 의 2단계 규칙만 따르면 된다.

## 순서

빠뜨리면 컴파일은 되는데 런타임에 조용히 죽는 항목이 섞여 있어 순서대로 확인한다.

### 1. `settings.gradle.kts` 에 include 추가

```kotlin
include(":<feature>:data")
```

같은 feature 의 `domain`·`presentation` 옆에 붙여 둔다. 여기를 빼먹으면 Gradle 이 모듈 자체를
모르므로 IDE 에서만 멀쩡해 보인다.

### 2. `<feature>/data/build.gradle.kts`

`onboarding/data/build.gradle.kts` 가 기준이다. convention plugin 이 없어 매번 전문을 쓴다:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ruleup.<feature>.data"
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
    implementation(project(":<feature>:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:domain"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test-junit"))
}
```

- `kotlin.serialization` 플러그인이 없으면 `@Serializable` 이 런타임에 터진다. DTO 를 쓰는
  data 모듈에는 항상 넣는다.
- `presentation` 이나 다른 feature 의 `data` 를 의존에 넣지 않는다 — 아키텍처 테스트가 막는다.
- 다른 feature 의 능력이 필요하면 그쪽 **`domain`** 만 의존한다.
- 소스 루트는 `src/main/kotlin/`(`java/` 아님). 패키지는 `com.ruleup.<feature>.data`.

### 3. 소스 디렉터리

```
<feature>/data/src/main/kotlin/com/ruleup/<feature>/data/
├── api/ 또는 <개념>/api/
├── dto/ 또는 <개념>/dto/
├── repository/ 또는 <개념>/repository/
└── di/
```

`AndroidManifest.xml` 은 필요 없다 — 이 모듈이 권한이나 컴포넌트를 선언할 때만 만든다.

### 4. `:app` 에 의존 추가

`app/build.gradle.kts` 의 dependencies 에 한 줄:

```kotlin
implementation(project(":<feature>:data"))
```

**이걸 빼면 빌드는 통과하고 앱은 실행되다가 주입 시점에 죽는다.** `:app` 이 전 모듈의 Hilt
바인딩을 모으는 컴포지션 루트라, 여기 없는 모듈의 `@Module` 은 그래프에 아예 들어오지 않는다.
증상은 "Repository 를 못 찾는다"는 컴파일 에러(`MissingBinding`)로 나오는데, 원인이 이 한 줄인지
알아채기까지 시간이 걸린다.

### 5. 확인

```bash
./gradlew ktlintFormat
./gradlew :<feature>:data:assembleDebug
./gradlew :app:assembleDebug     # Hilt 그래프가 실제로 완성되는지
```

`:app` 까지 빌드해야 DI 누락이 드러난다. data 모듈만 빌드하면 바인딩이 비어 있어도 통과한다.

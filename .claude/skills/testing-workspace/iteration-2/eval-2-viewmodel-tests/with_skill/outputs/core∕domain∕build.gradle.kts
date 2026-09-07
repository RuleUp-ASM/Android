plugins {
    id("java-library")
    // 다른 모듈의 테스트가 쓰는 대역(RecordingNavigationHelper)을 내주기 위한 것. observability:domain 과 같은 방식이다.
    id("java-test-fixtures")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    // 포트 구현체의 @Inject 생성자(런타임 Hilt 컴포넌트에서 제공). 도메인은 hilt 런타임 없이 annotation 만.
    implementation(libs.javax.inject)

    // NavigationHelper.navigationFlow 가 Flow 라 fixture 컴파일에도 coroutines 가 필요하다.
    // 위의 implementation 은 testFixtures 컴파일 경로로 오지 않아 여기서 다시 건다.
    testFixturesImplementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test-junit"))
}

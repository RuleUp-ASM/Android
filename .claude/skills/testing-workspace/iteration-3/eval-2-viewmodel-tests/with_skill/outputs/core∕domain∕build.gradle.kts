plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    // 화면 이동은 Effect 가 아니라 NavigationHelper 호출이라, 그 대역이 없으면 ViewModel 테스트가 성립하지 않는다.
    id("java-test-fixtures")
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

    // NavigationHelper.navigationFlow 가 Flow 라 fixture 컴파일 경로에도 coroutines 가 필요하다.
    testFixturesImplementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test-junit"))
}

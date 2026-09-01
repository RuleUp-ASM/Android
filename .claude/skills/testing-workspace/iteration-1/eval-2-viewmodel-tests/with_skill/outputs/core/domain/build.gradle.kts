plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    // ViewModel 테스트가 쓰는 NavigationHelper 대역을 다른 모듈에 내주기 위한 것. main 에 섞지 않는다.
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

    // main 의 coroutines 는 implementation 이라 testFixtures 컴파일 경로에 오지 않는다. NavigationHelper.navigationFlow 가 Flow 다.
    testFixturesImplementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test-junit"))
}

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    // 호스트 헬퍼(NavigationHelper·MessageHelper) 대역을 소비 모듈에 내준다.
    `java-test-fixtures`
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

    testImplementation(kotlin("test-junit"))

    // coroutines 가 implementation 이라 testFixtures 컴파일 경로엔 오지 않는다.
    // NavigationHelper.navigationFlow 가 Flow 라 이 줄이 없으면 fixture 가 컴파일되지 않는다.
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}

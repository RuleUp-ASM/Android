
# 카카오 SDK — ClientError 가 에러 원인 enum 필드를 이름으로 리플렉션 조회한다(난독화 시 NoSuchFieldException)
# https://developers.kakao.com/docs/latest/ko/android/getting-started#project-pro-guard
-keep class com.kakao.sdk.**.model.* { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.**

# 카카오맵 SDK(네이티브 엔진이 JNI 로 클래스를 찾는다)
-keep class com.kakao.vectormap.** { *; }
-keep interface com.kakao.vectormap.**

# 응답 본문을 읽지 않는 DTO 는 R8 이 지우고 반환형을 BaseResponse<Object> 로 바꿔, 요청 전에 컨버터 생성이 실패한다(#655).
-keep,allowobfuscation @kotlinx.serialization.Serializable class com.ruleup.**

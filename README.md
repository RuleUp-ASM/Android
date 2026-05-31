# Android

RuleUp FE 레포지토리입니다.

## 모듈 구조

### 디렉토리 레이아웃

```
Android/
├── app/                          # Application module · DI Composition Root
├── build-logic/convention/       # Gradle Convention Plugins
├── core/
│   ├── designsystem/             # Theme · Color · Typography · Atomic Composables
│   ├── ui/                       # 재사용 Composable
│   ├── common/                   # Result · Dispatcher · 확장 함수
│   ├── model/                    # 공용 도메인 모델
│   ├── navigation/               # Navigation3 NavKey · 라우트 컨트랙트
│   ├── network/                  # Retrofit · OkHttp · Serialization
│   └── datastore/                # Preferences DataStore
└── feature/
    └── <name>/
        ├── presentation/         # Screen · ViewModel · NavGraph
        ├── domain/               # UseCase · Repository 인터페이스
        └── data/                 # Repository 구현 · DataSource · DTO · Mapper
```

### 신규 모듈 작성 가이드

| 만들고 싶은 것 | 적용할 Plugin |
| --- | --- | --- |
| 새 화면 (feature 의 presentation) | `ruleup.android.presentation` |
| 새 유스케이스 (feature 의 domain) | `ruleup.android.domain` |
| 새 Repository 구현 (feature 의 data) | `ruleup.android.data` |
| 공용 UI 컴포넌트 | `ruleup.android.library` + `ruleup.android.compose` |
| 공용 Kotlin 유틸 | `ruleup.jvm.library` |

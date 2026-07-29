# 작업 원칙 (필수)

- **최소 변경**: 사이드 이펙트가 발생하지 않도록 요청된 목표를 달성하는 최소한의 변경만 한다. 요청 범위를 벗어난 리팩터링·포맷팅·"겸사겸사" 수정은 하지 않는다.
- **추측 금지**: 명세가 명확하지 않으면 임의로 가정해 진행하지 않는다. 모호한 지점을 짚어 선택지/질문을 먼저 제시하고, 사용자의 확인을 받은 뒤 진행한다.

# 아키텍처 (필수)

이 프로젝트는 **DDD · MVI · feature 기반 멀티모듈**을 따른다. 새 코드는 기존 컨벤션과 일관되게 작성한다.

## 모듈 구조
- **feature 모듈**: `:<feature>:data`, `:<feature>:domain`, `:<feature>:presentation` 세 레이어로 분리한다.
- **core 모듈**: 여러 feature가 공유하는 횡단 관심사.
- 의존 방향: `presentation → domain ← data`. domain은 다른 레이어에 의존하지 않는다. feature 간 직접 의존 금지(공유는 core 경유).
- 패키지 루트: `com.ruleup.<feature>.<layer>` (예: `com.ruleup.challenge.domain`).

## DDD (domain 레이어)
- `domain/entity/`: 도메인 모델.
- `domain/usecase/`: 단일 책임 UseCase (예: `CreateChallengeUseCase`).
- `domain/<Name>Repository.kt`: Repository **인터페이스**를 domain에 두고, 구현은 `data` 모듈에 둔다.

## MVI (presentation 레이어)
화면별로 `viewmodel/` 패키지를 두고 다음 요소로 구성한다 (기존 `CreateChallenge*` 컨벤션):
- `<Screen>Intent`: 사용자/시스템 의도(이벤트).
- `<Screen>State`: 화면 상태 (단일 불변 상태).
- `<Screen>Effect`: 일회성 사이드 이펙트(네비게이션·토스트 등).
- `<Screen>ReducerEvent`: 상태 전이 이벤트.
- `<Screen>ViewModel`: Intent 수신 → State/Effect 방출.
- Composable 화면은 State를 구독하고 Intent를 올려보낸다(상태 호이스팅).

# 작업 워크플로우 (필수)

새로운 작업이 지시되면 **반드시** 아래 순서를 따른다. 단순 질문·조회·읽기 전용 작업은 예외다.

저장소: `RuleUp-ASM/Android` · 기본(base) 브랜치: `develop`

## 1. GitHub 이슈 생성
작업을 시작하기 전에 먼저 이슈를 만든다.

```bash
gh issue create --title "<간결한 작업 제목>" --body "<배경·목표·완료 조건>"
```

- 적절한 라벨이 있으면 `--label`로 붙인다 (`enhancement`, `bug`, `documentation` 등).
- 생성된 **이슈 번호**를 기억한다. 이후 모든 단계에서 사용한다.

## 2. 이슈 번호로 브랜치 생성
`develop`에서 분기하고, 브랜치 이름은 `<타입>/<이슈번호>` 형식으로 한다.

```bash
git fetch origin
git switch develop && git pull
git switch -c feat/<이슈번호>   # 기본은 feat/, 성격에 따라 fix/·chore/·docs/ 사용
```

기존 컨벤션 예시: `feat/20`, `feat/11`, `feat/9`.

## 3. 작업 진행
- 해당 브랜치에서만 작업한다. `develop`에 직접 커밋하지 않는다.
- 커밋·PR 메시지 본문은 한국어로 작성한다 (기존 히스토리 컨벤션).
- 커밋 메시지는 `<type>(scope): 설명` 형식을 따른다 (`feat`, `fix`, `refactor`, `docs`, `chore`, `lint`).
- 가능하면 `Closes #<이슈번호>`를 PR 본문에 포함해 이슈를 자동으로 닫는다.

## 4. 작업 단위로 쪼개서 머지
큰 작업은 한 번에 머지하지 말고 **논리적 작업 단위로 나눠** 각각 PR을 올려 머지한다.

- 각 작업 단위마다 의미 있는 커밋으로 분리한다.
- 작업 단위가 완료되면 `develop` 대상으로 PR을 만든다:

  ```bash
  git push -u origin feat/<이슈번호>
  gh pr create --base develop --head feat/<이슈번호> \
    --title "<작업 단위 제목>" --body "$(cat <<'EOF'
  ## 작업 내용
  - ...

  Closes #<이슈번호>
  EOF
  )"
  ```

- 다음 작업 단위는 머지된 `develop`에서 새로 분기하거나, 같은 이슈가 이어지면 같은 브랜치에서 다음 단위를 이어간다.
- PR 생성·머지 등 되돌리기 어려운 외부 동작은 진행 전 사용자에게 확인받는다.

## 메모
- 모든 PR의 base는 `develop` (main 아님).
- 커밋 author co-author 트레일러는 기존 설정을 따른다.

# docs

공유용 PDF 와 그 원본. **PDF 를 직접 고치지 말고 `src/` 의 HTML 을 고친 뒤 다시 뽑는다.**

| 파일 | 무엇 | 원본 |
|---|---|---|
| `RuleUp-수동QA-시나리오목록.pdf` | 정책·도메인별 수동 QA 시나리오 타이틀 216개 | `src/manual-qa-titles.html` |
| `RuleUp-관측이벤트와퍼널.pdf` | 관측 이벤트 37종과 퍼널·지표 | `src/observability.html` |

## 다시 뽑는 법

헤드리스 Chrome 으로 인쇄한다. 별도 도구를 설치하지 않으려고 이 방법을 쓴다 — macOS 에 Chrome 과
한글 폰트(Apple SD Gothic Neo)가 이미 있다.

```bash
cd docs
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --headless --disable-gpu --no-pdf-header-footer \
  --print-to-pdf="RuleUp-관측이벤트와퍼널.pdf" \
  "file://$PWD/src/observability.html"
```

`--no-pdf-header-footer` 를 빼면 페이지마다 파일 경로와 날짜가 머리말·꼬리말로 박힌다.

## 두 문서의 관계

관측 PDF 는 **레포의 `OBSERVABILITY.md` 에서 온 공유용 사본**이다. 이벤트 이름·속성이 바뀌면
마크다운이 먼저 바뀌고 PDF 는 따라온다. 둘이 어긋나면 마크다운이 맞다.

수동 QA PDF 는 원본이 이 HTML 뿐이다. 자동 인증 상세 절차는 레포의 `VERIFICATION_TEST_PLAN.md`,
자동 테스트 커버리지는 `TEST_STRATEGY.md` 가 따로 갖는다.

## PDF 를 커밋하는 것에 대해

PDF 는 바이너리라 diff 가 읽히지 않고, 다시 뽑을 때마다 새 blob 이 통째로 쌓인다. 그래도 넣어 둔
이유는 **공유할 때 링크 하나로 끝나야 하기 때문**이다. 대신 원본을 같이 둬서 오타 하나에 문서를
처음부터 다시 만들지 않게 했다.

자주 바뀌는 문서는 PDF 를 갱신하지 말고 마크다운만 고친다. PDF 는 밖으로 내보낼 때만 다시 뽑는다.

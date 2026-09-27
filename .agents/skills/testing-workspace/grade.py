#!/usr/bin/env python3
"""iteration 디렉터리의 각 run 을 기계적으로 채점해 grading.json 을 남긴다."""
import json, re, sys
from pathlib import Path

HANGUL = re.compile(r"[가-힣]")
TESTFN = re.compile(r"@Test[^\n]*\n(?:\s*@\w+[^\n]*\n)*\s*fun\s+`([^`]+)`", re.MULTILINE)


def read_all(root: Path):
    """outputs 아래 모든 파일을 (상대경로, 내용) 으로."""
    out = {}
    if not root.exists():
        return out
    for p in root.rglob("*"):
        if p.is_file():
            try:
                out[str(p.relative_to(root))] = p.read_text(encoding="utf-8", errors="replace")
            except OSError:
                pass
    return out


def kt_tests(files):
    """테스트 소스만."""
    return {k: v for k, v in files.items() if k.endswith("Test.kt")}


def all_test_names(files):
    names = []
    for v in kt_tests(files).values():
        names += TESTFN.findall(v)
    return names


def joined(files, pred=lambda k: True):
    return "\n".join(v for k, v in files.items() if pred(k))


def E(cond, ev):
    return bool(cond), ev


def grade1(f, repo):
    t, names = kt_tests(f), all_test_names(f)
    src = joined(f, lambda k: k.endswith(".kt"))
    tj = joined(f, lambda k: k.endswith("Test.kt"))
    entries_used = len(re.findall(r"\.entries\b", tj))
    return [
        E(any("challenge/domain/src/test" in k and "/entity/" in k for k in t), f"테스트 파일: {list(t)}"),
        E(names and all(HANGUL.search(n) for n in names), f"{len(names)}개 중 한글 이름 {sum(1 for n in names if HANGUL.search(n))}개"),
        E(names and len(set(names)) == len(names),
          f"중복 이름: {sorted(n for n in set(names) if names.count(n) > 1)} ({len(names)}개 중 고유 {len(set(names))}개)"),
        E(names and not any(re.search(r"Exception|Error|Throwable", n) for n in names),
          f"예외명 포함 이름: {[n for n in names if re.search(r'Exception|Error', n)]}"),
        E(entries_used >= 2, f"entries 기반 열거 {entries_used}회"),
        E(not re.search(r"\bFake\w+|mockk|Mockito|\bmock\(", tj), "Fake/Mock 사용 흔적 없음" if not re.search(r"\bFake\w+|mockk|Mockito", tj) else "Fake/Mock 발견"),
        E("kotlin.test" in src and "org.junit.Assert" not in src, f"kotlin.test={'kotlin.test' in src}, junit.Assert={'org.junit.Assert' in src}"),
        E(names and not any(re.search(r"\b(is|get|from|to)[A-Z]\w+|\w+\(\)", n) for n in names),
          f"구현 심볼 노출: {[n for n in names if re.search(r'\b(is|get|from|to)[A-Z]\w+', n)]}"),
    ]


def grade2(f, repo):
    tj, names = joined(f, lambda k: k.endswith(".kt")), all_test_names(f)
    gradle = joined(f, lambda k: k.endswith("build.gradle.kts"))
    return [
        E("setMain" in tj and "resetMain" in tj, f"setMain={'setMain' in tj}, resetMain={'resetMain' in tj}"),
        E(re.search(r"coroutines[.-]test|coroutines\.test", gradle), "gradle 에 coroutines-test" if gradle else "build.gradle.kts 산출물 없음"),
        E(re.search(r":\s*NavigationHelper|NavigationHelper\s*\{", tj), "NavigationHelper 대역 정의됨" if re.search(r":\s*NavigationHelper", tj) else "없음"),
        E(not re.search(r"\.effect\.first\(\)", tj) or re.search(r"toList\(|effect\.collect|launch\s*\(", tj),
          f"bare first()={bool(re.search(r'.effect.first()', tj))}, 수집자={bool(re.search(r'toList|collect', tj))}"),
        E(not re.search(r"mockk|Mockito|io\.mockk", tj + gradle), "목킹 라이브러리 없음" if not re.search(r"mockk|Mockito", tj + gradle) else "목킹 발견"),
        E(names and all(HANGUL.search(n) for n in names), f"{len(names)}개 테스트, 한글 {sum(1 for n in names if HANGUL.search(n))}개"),
        E(not re.search(r"assertEquals\(\s*\w*State\(", tj), "State 통째 비교 없음"),
        E(re.search(r"Exception|실패|오류|에러", tj), "실패 경로 검증 흔적"),
    ]


def grade3(f, repo):
    tj = joined(f, lambda k: k.endswith(".kt"))
    toml = joined(f, lambda k: k.endswith(".toml"))
    gradle = joined(f, lambda k: k.endswith("build.gradle.kts"))
    props = joined(f, lambda k: k.endswith(".properties"))
    return [
        E(re.search(r'robolectric\s*=\s*"[\d.]+"', toml), f"toml robolectric 핀: {bool(re.search(r'robolectric', toml))}"),
        E("isIncludeAndroidResources" in gradle, f"isIncludeAndroidResources={'isIncludeAndroidResources' in gradle}"),
        E(re.search(r"sdk\s*=\s*\d+", props) or re.search(r"@Config\s*\(\s*sdk", tj), f"properties sdk={bool(re.search(r'sdk', props))}, @Config sdk={bool(re.search(r'@Config', tj))}"),
        E("RobolectricTestRunner" in tj, f"RobolectricTestRunner={'RobolectricTestRunner' in tj}"),
        E(any("src/test" in k for k in f if k.endswith("Test.kt")) and not any("androidTest" in k for k in f),
          f"테스트 위치: {[k for k in f if k.endswith('Test.kt')]}"),
        E("RuleUpTheme" in tj, f"RuleUpTheme={'RuleUpTheme' in tj}"),
        E(re.search(r"1134[:-]\d+|figma|Figma", joined(f)),
          f"Figma 근거: {re.findall(r'1134[:-]\d+', joined(f))[:5] or '없음'}"),
        E(re.search(r"미확정|디자인에 없|프레임이 없|design.{0,12}(missing|absent)", joined(f)),
          "디자인 미확정 표시 있음" if re.search(r"미확정|디자인에 없|프레임이 없", joined(f)) else "표시 없음"),
    ]


def grade4(f, repo):
    doc = next((v for k, v in f.items() if k.endswith("TEST_STRATEGY.md")), "")
    paths = re.findall(r"[\w/]+\.kt", doc)
    real = [p for p in paths if (repo / p).exists()]
    return [
        E(any(k.endswith("TEST_STRATEGY.md") for k in f), f"파일: {[k for k in f if k.endswith('.md')]}"),
        E(doc.count("|") > 20 and re.search(r"케이스|유닛", doc) and re.search(r"인수", doc), "층 격자 표 존재"),
        E(re.search(r"왜|이유|사유", doc), "미검증 사유 열 존재"),
        E(re.search(r"조건|필요|풀리|해소|블로커|차단", doc), "해소 조건 존재"),
        E(re.search(r"인수 ?시나리오|스토리", doc), "인수 시나리오 절 존재"),
        E("VERIFICATION_TEST_PLAN" in doc, f"기존 문서 참조={'VERIFICATION_TEST_PLAN' in doc}"),
        E(re.search(r":\w[\w:]*:test\w*UnitTest|:\w[\w:]*:test\b", doc), "모듈 스코프 실행 명령 존재"),
        E(len(real) >= 3, f"실존하는 레포 경로 {len(real)}개 / 언급 {len(paths)}개"),
    ]


GRADERS = {"eval-1": grade1, "eval-2": grade2, "eval-3": grade3, "eval-4": grade4}


def main():
    it = Path(sys.argv[1])
    repo = Path(".").resolve()
    for evd in sorted(it.glob("eval-*")):
        meta = json.loads((evd / "eval_metadata.json").read_text())
        g = GRADERS[evd.name[:6]]
        for run in ("with_skill", "without_skill"):
            outs = evd / run / "outputs"
            if not outs.exists():
                continue
            files = read_all(outs)
            try:
                results = g(files, repo)
            except Exception as e:  # 산출물이 아예 없을 때
                results = [(False, f"채점 실패: {e}")] * len(meta["assertions"])
            exp = [{"text": t, "passed": p, "evidence": str(ev)[:300]}
                   for t, (p, ev) in zip(meta["assertions"], results)]
            (evd / run / "grading.json").write_text(json.dumps(
                {"eval_id": meta["eval_id"], "run": run, "expectations": exp,
                 "passed": sum(e["passed"] for e in exp), "total": len(exp)},
                ensure_ascii=False, indent=2))
            print(f"{evd.name:32s} {run:14s} {sum(e['passed'] for e in exp)}/{len(exp)}")


if __name__ == "__main__":
    main()

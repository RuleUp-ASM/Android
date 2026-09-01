#!/usr/bin/env python3
"""층 x 모듈 테스트 커버리지 격자와 빈 구멍을 뽑는다.

TEST_STRATEGY.md 의 현황 표는 손으로 세면 금방 낡는다. 레포 루트에서 돌리고 출력을
그 문서의 해당 절에 그대로 붙인다.

    python3 .claude/skills/testing/scripts/coverage_map.py            # 마크다운
    python3 .claude/skills/testing/scripts/coverage_map.py --gaps     # 빈 구멍만

층 분류는 파일 이름과 내용으로 한다(SKILL.md 의 다섯 층). 기존 파일을 개명하지 않아도
되도록 UI 층은 이름이 아니라 Robolectric 러너 유무로 가른다.
"""

import argparse
import re
import sys
from collections import defaultdict
from pathlib import Path

LAYERS = ["케이스", "모듈", "UI", "통합", "인수"]

SKIP_DIRS = {"build", ".git", ".gradle", ".idea", ".claude", "node_modules"}

TEST_FN = re.compile(r"^\s*@Test\b", re.MULTILINE)


def walk_kt(root: Path):
    for path in root.rglob("*.kt"):
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        yield path


def module_of(path: Path, root: Path) -> str:
    """가장 가까운 상위 build.gradle.kts 소유 디렉터리를 :a:b 형태로."""
    cur = path.parent
    while cur != root and cur != cur.parent:
        if (cur / "build.gradle.kts").exists():
            return ":" + str(cur.relative_to(root)).replace("/", ":")
        cur = cur.parent
    return "(unknown)"


def classify(path: Path, text: str) -> str:
    name = path.name
    posix = path.as_posix()
    if "/acceptance/" in posix or name.endswith("AcceptanceTest.kt"):
        return "인수"
    if "RobolectricTestRunner" in text or "createComposeRule" in text:
        return "UI"
    if name.endswith("IntegrationTest.kt") or "/app/src/" in posix:
        return "통합"
    if name.endswith(("UseCaseTest.kt", "RepositoryImplTest.kt", "ViewModelTest.kt")):
        return "모듈"
    return "케이스"


TYPE_DECL = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:public\s+|internal\s+|abstract\s+|open\s+|sealed\s+|value\s+|data\s+|enum\s+)*"
    r"(?:class|interface|object)\s+(\w+)",
    re.MULTILINE,
)


def top_level_types(text: str) -> list[tuple[str, str]]:
    """(타입 이름, 그 선언부터 다음 선언까지의 본문). 파일 하나에 타입이 여럿인 흔한 형태를 가른다."""
    hits = [(m.group(1), m.start()) for m in TYPE_DECL.finditer(text) if m.start() == 0 or text[m.start() - 1] == "\n"]
    out = []
    for i, (name, start) in enumerate(hits):
        end = hits[i + 1][1] if i + 1 < len(hits) else len(text)
        out.append((name, text[start:end]))
    return out


def gap_targets(path: Path, text: str) -> list[tuple[str, str]]:
    """이 프로덕션 파일이 테스트를 요구하는 (대상 이름, 이유) 목록. 파일명이 아니라 선언된 타입으로 센다 —
    한 파일에 타입이 여럿일 때 파일명으로만 맞추면, 다른 이름으로 이미 덮인 타입까지 구멍으로 잡힌다."""
    name = path.name
    posix = path.as_posix()
    if "/src/main/" not in posix:
        return []
    if name.endswith("UseCase.kt"):
        return [(path.stem, "UseCase — 모듈 층")]
    if name.endswith("ViewModel.kt"):
        return [(path.stem, "ViewModel — 모듈 층")]
    if name.endswith("RepositoryImpl.kt"):
        return [(path.stem, "RepositoryImpl — 모듈 층")]
    if "/presentation/" in posix and name.endswith("Screen.kt"):
        return [(path.stem, "화면 — UI 층")]
    if "/domain/" in posix and ("/entity/" in posix or "/model/" in posix):
        found = []
        for type_name, body in top_level_types(text):
            if "require(" in body or "check(" in body or re.search(r"\binit\s*\{", body):
                found.append((type_name, "불변식 있는 entity — 케이스 층"))
        return found
    return []


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".", help="레포 루트 (기본: 현재 디렉터리)")
    ap.add_argument("--gaps", action="store_true", help="빈 구멍만 출력")
    args = ap.parse_args()

    root = Path(args.root).resolve()
    if not (root / "settings.gradle.kts").exists():
        print(f"레포 루트가 아니다: {root}", file=sys.stderr)
        return 1

    grid = defaultdict(lambda: defaultdict(int))   # module -> layer -> @Test 수
    files_by_layer = defaultdict(int)
    tested_names: set[str] = set()                 # "CreateChallengeCommand" 처럼 Test 를 뗀 이름
    production: list[tuple[Path, str, str, str]] = []  # (path, module, 대상 이름, 이유)

    for path in walk_kt(root):
        posix = path.as_posix()
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue

        is_test_src = "/src/test/" in posix or "/src/androidTest/" in posix
        if is_test_src:
            if not path.name.endswith("Test.kt"):
                continue  # Fake·fixture 는 세지 않는다
            layer = classify(path, text)
            count = len(TEST_FN.findall(text))
            grid[module_of(path, root)][layer] += count
            files_by_layer[layer] += 1
            tested_names.add(path.name[: -len("Test.kt")])
        else:
            for target, reason in gap_targets(path, text):
                production.append((path, module_of(path, root), target, reason))

    gaps = [(p, m, t, r) for p, m, t, r in production if t not in tested_names]

    if not args.gaps:
        print("## 층 × 모듈 커버리지\n")
        print("숫자는 `@Test` 개수. `–` 는 그 층 테스트가 없다는 뜻이다.\n")
        header = "| 모듈 | " + " | ".join(LAYERS) + " | 합계 |"
        print(header)
        print("|" + "---|" * (len(LAYERS) + 2))
        totals = defaultdict(int)
        for module in sorted(grid):
            row = [str(grid[module].get(layer) or "–") for layer in LAYERS]
            total = sum(grid[module].values())
            for layer in LAYERS:
                totals[layer] += grid[module].get(layer, 0)
            print(f"| `{module}` | " + " | ".join(row) + f" | {total} |")
        print(
            "| **합계** | "
            + " | ".join(f"**{totals[layer]}**" for layer in LAYERS)
            + f" | **{sum(totals.values())}** |"
        )
        print()
        print(
            "테스트 파일 수: "
            + ", ".join(f"{layer} {files_by_layer.get(layer, 0)}" for layer in LAYERS)
        )
        print()

    print("## 테스트가 없는 대상\n")
    if not gaps:
        print("없음.\n")
    else:
        by_module = defaultdict(list)
        for path, module, target, reason in gaps:
            by_module[module].append((target, path.relative_to(root), reason))
        for module in sorted(by_module):
            print(f"### `{module}`\n")
            for target, rel, reason in sorted(by_module[module]):
                print(f"- **{target}** — {reason}  \n  `{rel}`")
            print()
        print(
            f"합계 {len(gaps)}건. 전부 메울 필요는 없다 — "
            "빠져도 되는 건 이유와 함께 TEST_STRATEGY.md 의 미검증 목록으로 옮긴다.\n"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())

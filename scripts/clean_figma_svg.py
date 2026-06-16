#!/usr/bin/env python3
"""Figma `download_assets` 로 받은 SVG 에서 실제 아이콘(<g id="icon">)만 추출해
캔버스 배경·화면 프레임·드롭섀도 등 부모 잔재를 제거한 깔끔한 SVG 를 만든다.

usage: clean_figma_svg.py <in.svg> <out.svg> [icon_group_id=icon]
"""
import sys
import re
import xml.etree.ElementTree as ET

SVG_NS = "http://www.w3.org/2000/svg"
ET.register_namespace("", SVG_NS)


def local(tag: str) -> str:
    return tag.split("}", 1)[-1]


def main() -> int:
    src, dst = sys.argv[1], sys.argv[2]
    group_id = sys.argv[3] if len(sys.argv) > 3 else "icon"

    tree = ET.parse(src)
    root = tree.getroot()
    vb = root.get("viewBox")
    w, h = root.get("width"), root.get("height")

    # 부모 맵 구성 후 id=group_id 인 그룹과 그 조상 transform 누적
    parents = {c: p for p in root.iter() for c in p}
    icon = next((e for e in root.iter() if e.get("id") == group_id), None)
    if icon is None:
        # 폴백: 첫 번째 <g> 중 path 를 직접 가진 것
        icon = next((e for e in root.iter() if local(e.tag) == "g"
                     and any(local(c.tag) == "path" for c in e)), None)
    if icon is None:
        print(f"!! icon group not found in {src}", file=sys.stderr)
        return 1

    transforms = []
    node = parents.get(icon)
    while node is not None and node is not root:
        t = node.get("transform")
        if t:
            transforms.append(t)
        node = parents.get(node)
    transforms.reverse()

    # 아이콘이 참조할 수 있는 defs(그라데이션/필터) 보존
    defs = root.find(f"{{{SVG_NS}}}defs")

    out = ET.Element(f"{{{SVG_NS}}}svg")
    if vb:
        out.set("viewBox", vb)
    if w:
        out.set("width", w)
    if h:
        out.set("height", h)
    out.set("fill", "none")
    # 아이콘이 url(#...) 로 defs 를 참조할 때만 defs 보존(미사용 필터/그라데이션 잔재 제거).
    icon_str = ET.tostring(icon, encoding="unicode")
    if defs is not None and "url(#" in icon_str:
        out.append(defs)
    if transforms:
        wrap = ET.SubElement(out, f"{{{SVG_NS}}}g")
        wrap.set("transform", " ".join(transforms))
        wrap.append(icon)
    else:
        out.append(icon)

    xml = ET.tostring(out, encoding="unicode")
    # 빈 defs 등 정리
    xml = re.sub(r"\s+xmlns:ns\d+=\"[^\"]*\"", "", xml)
    with open(dst, "w") as f:
        f.write('<?xml version="1.0" encoding="UTF-8"?>\n')
        f.write(xml + "\n")
    print(f"cleaned {src} -> {dst}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

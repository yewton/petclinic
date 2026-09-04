#!/usr/bin/env python3
"""Gradle dependency verification metadata の既存エントリを改竄から保護する。"""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

CHECKSUM_TYPES = frozenset({"md5", "sha1", "sha256", "sha512"})
VERIFICATION_FLAGS = ("verify-metadata", "verify-signatures")

ChecksumKey = tuple[str, str, str, str, str]
ComponentCoordinate = tuple[str, str, str]
TrustRule = tuple[tuple[str, str], ...]


def local_name(tag: str) -> str:
    """XML namespace を除いた要素名を返す。"""

    return tag.rsplit("}", 1)[-1]


def first_child(element: ET.Element, name: str) -> ET.Element | None:
    """直下にある指定名の要素を返す。"""

    return next((child for child in element if local_name(child.tag) == name), None)


def parse_metadata(path: Path) -> ET.Element:
    """検証メタデータを読み込み、ルート要素を返す。"""

    try:
        return ET.parse(path).getroot()
    except (OSError, ET.ParseError) as error:
        raise ValueError(f"{path} を読み込めません: {error}") from error


def checksum_table(root: ET.Element) -> dict[ChecksumKey, frozenset[str]]:
    """成果物ごとに受け入れ可能な checksum の集合を収集する。"""

    checksums: defaultdict[ChecksumKey, set[str]] = defaultdict(set)
    for component in root.iter():
        if local_name(component.tag) != "component":
            continue
        coordinates = (
            component.get("group", ""),
            component.get("name", ""),
            component.get("version", ""),
        )
        for artifact in component:
            if local_name(artifact.tag) != "artifact":
                continue
            artifact_name = artifact.get("name", "")
            for checksum in artifact:
                checksum_type = local_name(checksum.tag)
                if checksum_type in CHECKSUM_TYPES and checksum.get("value"):
                    key = (*coordinates, artifact_name, checksum_type)
                    checksums[key].add(checksum.get("value", ""))
                    checksums[key].update(
                        additional.get("value", "")
                        for additional in checksum
                        if local_name(additional.tag) == "also-trust"
                        and additional.get("value")
                    )
    return {key: frozenset(values) for key, values in checksums.items()}


def component_coordinates(root: ET.Element) -> frozenset[ComponentCoordinate]:
    """記録されているコンポーネント座標を収集する。"""

    return frozenset(
        (
            component.get("group", ""),
            component.get("name", ""),
            component.get("version", ""),
        )
        for component in root.iter()
        if local_name(component.tag) == "component"
    )


def configuration(root: ET.Element) -> ET.Element | None:
    """configuration 要素を返す。"""

    return first_child(root, "configuration")


def enabled_flag(config: ET.Element | None, name: str) -> bool:
    """検証フラグが明示的に有効か判定する。"""

    if config is None:
        return False
    element = first_child(config, name)
    return element is not None and (element.text or "").strip().lower() == "true"


def trust_rules(config: ET.Element | None) -> frozenset[TrustRule]:
    """trusted-artifacts の各ルールを比較可能な形で返す。"""

    if config is None:
        return frozenset()
    trusted_artifacts = first_child(config, "trusted-artifacts")
    if trusted_artifacts is None:
        return frozenset()
    return frozenset(
        tuple(sorted(rule.attrib.items()))
        for rule in trusted_artifacts
        if local_name(rule.tag) == "trust"
    )


def format_checksum_key(key: ChecksumKey) -> str:
    """checksum のキーを診断用に整形する。"""

    group, name, version, artifact, checksum_type = key
    return f"{group}:{name}:{version} / {artifact} [{checksum_type}]"


def format_component(component: ComponentCoordinate) -> str:
    """コンポーネント座標を診断用に整形する。"""

    return ":".join(component)


def format_trust_rule(rule: TrustRule) -> str:
    """trust ルールを診断用に整形する。"""

    return ", ".join(f'{name}="{value}"' for name, value in rule)


def detect_violations(before: ET.Element, after: ET.Element) -> list[str]:
    """既存 checksum の変化、不審な削除、検証設定の弱体化を検出する。"""

    violations: list[str] = []
    before_checksums = checksum_table(before)
    after_checksums = checksum_table(after)

    for key in sorted(before_checksums.keys() & after_checksums.keys()):
        if before_checksums[key] != after_checksums[key]:
            violations.extend(
                (
                    f"既存 checksum が変化しました: {format_checksum_key(key)}",
                    f"  変更前: {', '.join(sorted(before_checksums[key]))}",
                    f"  変更後: {', '.join(sorted(after_checksums[key]))}",
                )
            )

    before_components = component_coordinates(before)
    after_components = component_coordinates(after)
    removed_components = before_components - after_components
    after_modules = {(group, name) for group, name, _version in after_components}
    for component in sorted(removed_components):
        group, name, _version = component
        if (group, name) not in after_modules:
            violations.append(
                f"依存モジュールが完全に削除されました: {format_component(component)}。"
                "意図した依存削除であれば、手元でメタデータを再生成し、"
                "内容を確認してコミットしてください"
            )

    for key in sorted(before_checksums.keys() - after_checksums.keys()):
        component = key[:3]
        if component in after_components:
            violations.append(
                f"既存 checksum エントリが削除されました: {format_checksum_key(key)}"
            )

    before_config = configuration(before)
    after_config = configuration(after)
    for flag in VERIFICATION_FLAGS:
        if enabled_flag(before_config, flag) and not enabled_flag(after_config, flag):
            violations.append(
                f"検証設定が弱められました: {flag} が true ではありません"
            )

    before_trust = trust_rules(before_config)
    after_trust = trust_rules(after_config)
    for rule in sorted(before_trust - after_trust):
        violations.append(
            f"trusted-artifacts のルールが削除されました: {format_trust_rule(rule)}"
        )
    for rule in sorted(after_trust - before_trust):
        violations.append(
            f"trusted-artifacts のルールが追加されました: {format_trust_rule(rule)}"
        )

    return violations


def main() -> int:
    """コマンドライン引数を処理し、異常があれば終了コード 1 を返す。"""

    parser = argparse.ArgumentParser(
        description="再生成前後の Gradle dependency verification metadata を比較します。"
    )
    parser.add_argument(
        "before", type=Path, help="再生成前の verification-metadata.xml"
    )
    parser.add_argument("after", type=Path, help="再生成後の verification-metadata.xml")
    args = parser.parse_args()

    try:
        before = parse_metadata(args.before)
        after = parse_metadata(args.after)
    except ValueError as error:
        print(f"エラー: {error}", file=sys.stderr)
        return 2

    violations = detect_violations(before, after)
    if violations:
        print(
            "dependency verification metadata の安全性検査に失敗しました:",
            file=sys.stderr,
        )
        for violation in violations:
            print(violation, file=sys.stderr)
        return 1

    print("dependency verification metadata の既存 checksum と検証設定は安全です。")
    return 0


if __name__ == "__main__":
    sys.exit(main())

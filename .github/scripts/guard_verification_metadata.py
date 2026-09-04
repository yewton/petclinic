#!/usr/bin/env python3
"""Gradle dependency verification metadata の既存エントリを改竄から保護する。"""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path

NAMESPACE = "https://schema.gradle.org/dependency-verification"
XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance"
CHECKSUM_TYPES = frozenset({"md5", "sha1", "sha256", "sha512"})
VERIFICATION_FLAGS = ("verify-metadata", "verify-signatures")

ELEMENT_CHILDREN = {
    "verification-metadata": frozenset({"configuration", "components"}),
    "configuration": frozenset(
        {
            "verify-metadata",
            "verify-signatures",
            "keyring-format",
            "key-servers",
            "trusted-artifacts",
            "ignored-keys",
            "trusted-keys",
        }
    ),
    "key-servers": frozenset({"key-server"}),
    "trusted-artifacts": frozenset({"trust"}),
    "ignored-keys": frozenset({"ignored-key"}),
    "trusted-keys": frozenset({"trusted-key"}),
    "trusted-key": frozenset({"trusting"}),
    "components": frozenset({"component"}),
    "component": frozenset({"artifact"}),
    "artifact": frozenset({"ignored-keys", "pgp", *CHECKSUM_TYPES}),
    **{checksum_type: frozenset({"also-trust"}) for checksum_type in CHECKSUM_TYPES},
    "verify-metadata": frozenset(),
    "verify-signatures": frozenset(),
    "keyring-format": frozenset(),
    "key-server": frozenset(),
    "trust": frozenset(),
    "ignored-key": frozenset(),
    "trusting": frozenset(),
    "pgp": frozenset(),
    "also-trust": frozenset(),
}

COORDINATE_ATTRIBUTES = frozenset({"group", "name", "version", "regex", "file"})
ELEMENT_ATTRIBUTES = {
    "verification-metadata": frozenset({f"{{{XSI_NAMESPACE}}}schemaLocation"}),
    "configuration": frozenset(),
    "verify-metadata": frozenset(),
    "verify-signatures": frozenset(),
    "keyring-format": frozenset(),
    "key-servers": frozenset({"enabled"}),
    "key-server": frozenset({"uri"}),
    "trusted-artifacts": frozenset(),
    "trust": COORDINATE_ATTRIBUTES | {"reason"},
    "ignored-keys": frozenset(),
    "ignored-key": frozenset({"id", "reason"}),
    "trusted-keys": frozenset(),
    "trusted-key": frozenset({"id", "group", "name", "version", "file", "regex"}),
    "trusting": COORDINATE_ATTRIBUTES,
    "components": frozenset(),
    "component": frozenset({"group", "name", "version"}),
    "artifact": frozenset({"name"}),
    **{
        checksum_type: frozenset({"value", "origin", "reason"})
        for checksum_type in CHECKSUM_TYPES
    },
    "pgp": frozenset({"value"}),
    "also-trust": frozenset({"value"}),
}

REQUIRED_ATTRIBUTES = {
    "component": frozenset({"group", "name", "version"}),
    "artifact": frozenset({"name"}),
    **{checksum_type: frozenset({"value"}) for checksum_type in CHECKSUM_TYPES},
    "pgp": frozenset({"value"}),
    "ignored-key": frozenset({"id"}),
    "trusted-key": frozenset({"id"}),
}

EXACT_CHILD_COUNTS = {
    "verification-metadata": {"configuration": 1, "components": 1},
    "configuration": {"verify-metadata": 1, "verify-signatures": 1},
}

AT_MOST_ONE_CHILD = {
    "configuration": frozenset(
        {
            "keyring-format",
            "key-servers",
            "trusted-artifacts",
            "ignored-keys",
            "trusted-keys",
        }
    )
}

ChecksumKey = tuple[str, str, str, str, str]
ComponentCoordinate = tuple[str, str, str]
TrustRule = tuple[tuple[str, str], ...]


class MetadataStructureError(ValueError):
    """検証メタデータが期待する構造に従っていないことを表す。"""


def qualified(name: str) -> str:
    """Gradle dependency verification namespace の修飾名を返す。"""

    return f"{{{NAMESPACE}}}{name}"


def split_tag(tag: str) -> tuple[str, str]:
    """要素名を namespace とローカル名に分ける。"""

    if tag.startswith("{") and "}" in tag:
        namespace, name = tag[1:].split("}", 1)
        return namespace, name
    return "", tag


def local_name(tag: str) -> str:
    """XML namespace を除いた要素名を返す。"""

    return tag.rsplit("}", 1)[-1]


def first_child(element: ET.Element, name: str) -> ET.Element | None:
    """直下にある指定名の要素を返す。"""

    return next((child for child in element if child.tag == qualified(name)), None)


def validate_element(element: ET.Element, path: str) -> None:
    """要素の namespace、属性、親子関係、主要要素の個数を検証する。"""

    namespace, name = split_tag(element.tag)
    if namespace != NAMESPACE:
        actual = namespace or "namespace なし"
        raise MetadataStructureError(
            f"{path}: namespace が不正です: {actual}（期待値: {NAMESPACE}）"
        )
    if name not in ELEMENT_CHILDREN:
        raise MetadataStructureError(f"{path}: 期待しない要素 <{name}> です")

    allowed_attributes = ELEMENT_ATTRIBUTES[name]
    for attribute in element.attrib:
        if attribute not in allowed_attributes:
            raise MetadataStructureError(
                f"{path}: <{name}> に期待しない属性 {attribute!r} があります"
            )
    for attribute in REQUIRED_ATTRIBUTES.get(name, frozenset()):
        if not element.get(attribute):
            raise MetadataStructureError(
                f"{path}: <{name}> に必須属性 {attribute!r} がありません"
            )

    child_counts: Counter[str] = Counter()
    for child in element:
        child_namespace, child_name = split_tag(child.tag)
        child_path = f"{path}/{child_name}"
        if child_namespace != NAMESPACE:
            actual = child_namespace or "namespace なし"
            raise MetadataStructureError(
                f"{child_path}: namespace が不正です: {actual}（期待値: {NAMESPACE}）"
            )
        if child_name not in ELEMENT_CHILDREN[name]:
            raise MetadataStructureError(
                f"{path}: <{name}> 直下に期待しない要素 <{child_name}> があります"
            )
        child_counts[child_name] += 1

    for child_name, expected_count in EXACT_CHILD_COUNTS.get(name, {}).items():
        actual_count = child_counts[child_name]
        if actual_count != expected_count:
            raise MetadataStructureError(
                f"{path}: <{child_name}> は {expected_count} 個必要ですが、"
                f"{actual_count} 個あります"
            )
    for child_name in AT_MOST_ONE_CHILD.get(name, frozenset()):
        actual_count = child_counts[child_name]
        if actual_count > 1:
            raise MetadataStructureError(
                f"{path}: <{child_name}> は最大 1 個ですが、{actual_count} 個あります"
            )

    for child in element:
        child_name = local_name(child.tag)
        validate_element(child, f"{path}/{child_name}")


def validate_structure(root: ET.Element) -> None:
    """Gradle dependency verification metadata の構造全体を検証する。"""

    namespace, name = split_tag(root.tag)
    if name != "verification-metadata":
        raise MetadataStructureError(
            f"root 要素が不正です: <{name}>（期待値: <verification-metadata>）"
        )
    if namespace != NAMESPACE:
        actual = namespace or "namespace なし"
        raise MetadataStructureError(
            f"root の namespace が不正です: {actual}（期待値: {NAMESPACE}）"
        )
    validate_element(root, "/verification-metadata")


def parse_metadata(path: Path) -> ET.Element:
    """検証メタデータを読み込み、ルート要素を返す。"""

    try:
        root = ET.parse(path).getroot()
    except (OSError, ET.ParseError) as error:
        raise ValueError(f"{path} を読み込めません: {error}") from error
    try:
        validate_structure(root)
    except MetadataStructureError as error:
        raise MetadataStructureError(f"{path}: {error}") from error
    return root


def checksum_table(root: ET.Element) -> dict[ChecksumKey, frozenset[str]]:
    """成果物ごとに受け入れ可能な checksum の集合を収集する。"""

    checksums: defaultdict[ChecksumKey, set[str]] = defaultdict(set)
    components = first_child(root, "components")
    assert components is not None
    for component in components:
        coordinates = (
            component.attrib["group"],
            component.attrib["name"],
            component.attrib["version"],
        )
        for artifact in component:
            artifact_name = artifact.attrib["name"]
            for checksum in artifact:
                checksum_type = local_name(checksum.tag)
                if checksum_type in CHECKSUM_TYPES:
                    key = (*coordinates, artifact_name, checksum_type)
                    checksums[key].add(checksum.attrib["value"])
                    checksums[key].update(
                        additional.get("value", "")
                        for additional in checksum
                        if local_name(additional.tag) == "also-trust"
                        and additional.get("value")
                    )
    return {key: frozenset(values) for key, values in checksums.items()}


def component_coordinates(root: ET.Element) -> frozenset[ComponentCoordinate]:
    """記録されているコンポーネント座標を収集する。"""

    components = first_child(root, "components")
    assert components is not None
    return frozenset(
        (
            component.attrib["group"],
            component.attrib["name"],
            component.attrib["version"],
        )
        for component in components
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
    except MetadataStructureError as error:
        print(
            "dependency verification metadata の安全性検査に失敗しました:",
            file=sys.stderr,
        )
        print(f"構造が不正です: {error}", file=sys.stderr)
        return 1
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

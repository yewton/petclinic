#!/usr/bin/env python3
"""guard_verification_metadata.py のコマンドライン動作を検証する。"""

from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("guard_verification_metadata.py")
REPOSITORY_METADATA = SCRIPT.parents[2] / "gradle" / "verification-metadata.xml"
NAMESPACE = "https://schema.gradle.org/dependency-verification"


def metadata(
    components: str,
    *,
    verify_metadata: str = "true",
    verify_signatures: str = "true",
    trust_rules: str = '<trust file=".*-sources[.]jar" regex="true"/>',
) -> str:
    """テスト用の最小限の検証メタデータを組み立てる。"""

    return f"""<?xml version="1.0" encoding="UTF-8"?>
<verification-metadata xmlns="{NAMESPACE}">
  <configuration>
    <verify-metadata>{verify_metadata}</verify-metadata>
    <verify-signatures>{verify_signatures}</verify-signatures>
    <trusted-artifacts>{trust_rules}</trusted-artifacts>
  </configuration>
  <components>{components}</components>
</verification-metadata>
"""


def component(version: str, checksum: str) -> str:
    """テスト用 component を組み立てる。"""

    return f"""
    <component group="example" name="library" version="{version}">
      <artifact name="library-{version}.jar">
        <sha256 value="{checksum}"/>
      </artifact>
    </component>
"""


class GuardVerificationMetadataTest(unittest.TestCase):
    """CLI の終了コードと診断内容を検証する。"""

    def run_guard(self, before: str, after: str) -> subprocess.CompletedProcess[str]:
        """一時ファイルに XML を保存してスクリプトを実行する。"""

        with tempfile.TemporaryDirectory() as directory:
            before_path = Path(directory, "before.xml")
            after_path = Path(directory, "after.xml")
            before_path.write_text(before, encoding="utf-8")
            after_path.write_text(after, encoding="utf-8")
            return subprocess.run(
                [sys.executable, str(SCRIPT), str(before_path), str(after_path)],
                check=False,
                capture_output=True,
                text=True,
            )

    def run_guard_paths(
        self, before: Path, after: Path
    ) -> subprocess.CompletedProcess[str]:
        """実ファイルを入力してスクリプトを実行する。"""

        return subprocess.run(
            [sys.executable, str(SCRIPT), str(before), str(after)],
            check=False,
            capture_output=True,
            text=True,
        )

    def test_unchanged_metadata_passes(self) -> None:
        xml = metadata(component("1.0", "original"))

        result = self.run_guard(xml, xml)

        self.assertEqual(0, result.returncode, result.stderr)

    def test_repository_metadata_passes(self) -> None:
        result = self.run_guard_paths(REPOSITORY_METADATA, REPOSITORY_METADATA)

        self.assertEqual(0, result.returncode, result.stderr)

    def test_removing_version_with_replacement_version_passes(self) -> None:
        before = metadata(component("1.0", "original"))
        after = metadata(component("2.0", "added"))

        result = self.run_guard(before, after)

        self.assertEqual(0, result.returncode, result.stderr)

    def test_removing_entire_module_fails(self) -> None:
        before = metadata(component("1.0", "original"))
        after = metadata("")

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("依存モジュールが完全に削除されました", result.stderr)
        self.assertIn("example:library:1.0", result.stderr)
        self.assertIn("手元でメタデータを再生成", result.stderr)

    def test_removing_all_components_fails(self) -> None:
        before = metadata(
            component("1.0", "first")
            + component("2.0", "second").replace('name="library"', 'name="other"')
        )
        after = metadata("")

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("example:library:1.0", result.stderr)
        self.assertIn("example:other:2.0", result.stderr)

    def test_dummy_component_in_trusted_artifacts_cannot_bypass_removal(self) -> None:
        before = metadata(component("1.0", "original"))
        after = metadata(
            "",
            trust_rules=(
                '<trust file=".*-sources[.]jar" regex="true"/>'
                '<component group="example" name="library" version="dummy"/>'
            ),
        )

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("構造が不正です", result.stderr)
        self.assertIn(
            "<trusted-artifacts> 直下に期待しない要素 <component>", result.stderr
        )

    def test_wrong_root_element_fails(self) -> None:
        before = metadata(component("1.0", "original"))
        after = before.replace("verification-metadata", "unexpected-root")

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("root 要素が不正です", result.stderr)

    def test_wrong_namespace_fails(self) -> None:
        before = metadata(component("1.0", "original"))
        after = before.replace(NAMESPACE, "https://example.invalid/wrong-namespace")

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("root の namespace が不正です", result.stderr)

    def test_wrong_nested_namespace_fails(self) -> None:
        before = metadata(component("1.0", "original"))
        after = before.replace("<components>", '<components xmlns="">')

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("namespace が不正です: namespace なし", result.stderr)

    def test_root_requires_single_configuration_and_components(self) -> None:
        before = metadata(component("1.0", "original"))
        cases = {
            "configuration": before.replace(
                "</configuration>", "</configuration><configuration/>"
            ),
            "components": before.replace("</components>", "</components><components/>"),
        }

        for element, after in cases.items():
            with self.subTest(element=element):
                result = self.run_guard(before, after)

                self.assertEqual(1, result.returncode)
                self.assertIn(f"<{element}> は 1 個必要", result.stderr)

    def test_component_without_version_fails(self) -> None:
        before = metadata(component("1.0", "original"))
        after = before.replace(
            '<component group="example" name="library" version="1.0">',
            '<component group="example" name="library">',
        )

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("必須属性 'version' がありません", result.stderr)

    def test_component_outside_components_fails(self) -> None:
        before = metadata(component("1.0", "original"))
        misplaced = component("dummy", "dummy")
        after = metadata("").replace("<components>", f"{misplaced}<components>")

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn(
            "<verification-metadata> 直下に期待しない要素 <component>",
            result.stderr,
        )

    def test_unexpected_elements_at_each_metadata_level_fail(self) -> None:
        before = metadata(component("1.0", "original"))
        cases = {
            "root": before.replace(
                "</verification-metadata>", "<unexpected/></verification-metadata>"
            ),
            "configuration": before.replace(
                "<configuration>", "<configuration><unexpected/>"
            ),
            "trusted-artifacts": before.replace(
                "<trusted-artifacts>", "<trusted-artifacts><unexpected/>"
            ),
            "components": before.replace("<components>", "<components><unexpected/>"),
            "component": before.replace(
                '<component group="example" name="library" version="1.0">',
                '<component group="example" name="library" version="1.0">'
                "<unexpected/>",
            ),
            "artifact": before.replace(
                '<artifact name="library-1.0.jar">',
                '<artifact name="library-1.0.jar"><unexpected/>',
            ),
            "checksum": before.replace(
                '<sha256 value="original"/>',
                '<sha256 value="original"><unexpected/></sha256>',
            ),
        }

        for level, after in cases.items():
            with self.subTest(level=level):
                result = self.run_guard(before, after)

                self.assertEqual(1, result.returncode)
                self.assertIn("期待しない要素 <unexpected>", result.stderr)

    def test_removing_checksum_from_retained_component_fails(self) -> None:
        before = metadata(component("1.0", "original"))
        after = before.replace('<sha256 value="original"/>', "")

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("既存 checksum エントリが削除されました", result.stderr)

    def test_changed_existing_checksum_fails(self) -> None:
        before = metadata(component("1.0", "original"))
        after = metadata(component("1.0", "tampered"))

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("example:library:1.0 / library-1.0.jar [sha256]", result.stderr)
        self.assertIn("original", result.stderr)
        self.assertIn("tampered", result.stderr)

    def test_added_also_trust_checksum_fails(self) -> None:
        before = metadata(component("1.0", "original"))
        after = before.replace(
            '<sha256 value="original"/>',
            '<sha256 value="original"><also-trust value="tampered"/></sha256>',
        )

        result = self.run_guard(before, after)

        self.assertEqual(1, result.returncode)
        self.assertIn("example:library:1.0 / library-1.0.jar [sha256]", result.stderr)
        self.assertIn("original", result.stderr)
        self.assertIn("tampered", result.stderr)

    def test_disabling_verification_flags_fails(self) -> None:
        before = metadata(component("1.0", "original"))

        for flag, options in (
            ("verify-metadata", {"verify_metadata": "false"}),
            ("verify-signatures", {"verify_signatures": "false"}),
        ):
            with self.subTest(flag=flag):
                after = metadata(component("1.0", "original"), **options)
                result = self.run_guard(before, after)

                self.assertEqual(1, result.returncode)
                self.assertIn(flag, result.stderr)

    def test_changing_trusted_artifacts_fails(self) -> None:
        before = metadata(component("1.0", "original"))

        for trust_rules in (
            "",
            '<trust file=".*-sources[.]jar" regex="true"/>'
            '<trust file=".*-javadoc[.]jar" regex="true"/>',
        ):
            with self.subTest(trust_rules=trust_rules):
                after = metadata(component("1.0", "original"), trust_rules=trust_rules)
                result = self.run_guard(before, after)

                self.assertEqual(1, result.returncode)
                self.assertIn("trusted-artifacts", result.stderr)


if __name__ == "__main__":
    unittest.main()

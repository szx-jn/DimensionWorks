from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))

import check_localization  # noqa: E402


class CheckLocalizationTest(unittest.TestCase):
    def make_instance(self, *, source_zh=None, override_zh=None, source_en=None) -> Path:
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        instance = Path(temp.name)
        mods = instance / "mods"
        mods.mkdir()
        jar = mods / "example.jar"
        with ZipFile(jar, "w") as archive:
            archive.writestr(
                "META-INF/mods.toml",
                'modLoader="javafml"\n[[mods]]\nmodId="example"\n',
            )
            archive.writestr(
                "assets/example/lang/en_us.json",
                json.dumps(source_en or {"example.hello": "Hello"}, ensure_ascii=False),
            )
            if source_zh is not None:
                archive.writestr(
                    "assets/example/lang/zh_cn.json",
                    json.dumps(source_zh, ensure_ascii=False),
                )
        return instance

    def make_override(
        self,
        root: Path,
        *,
        translatable: dict[str, str] | None = None,
        manuals: dict[str, str] | None = None,
    ) -> None:
        assets = root / "assets" / "example"
        if translatable is not None:
            lang = assets / "lang"
            lang.mkdir(parents=True)
            (lang / "zh_cn.json").write_text(
                json.dumps(translatable, ensure_ascii=False),
                encoding="utf-8",
            )
        for relative, text in (manuals or {}).items():
            path = assets / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")

    def test_reports_missing_language_key(self) -> None:
        instance = self.make_instance()
        with tempfile.TemporaryDirectory() as temp:
            override = Path(temp)
            report = check_localization.build_report(instance, override)
        self.assertEqual(report.missing_language_keys, [("example", "example.hello")])

    def test_accepts_override_for_missing_language_key(self) -> None:
        instance = self.make_instance()
        with tempfile.TemporaryDirectory() as temp:
            override = Path(temp)
            self.make_override(override, translatable={"example.hello": "你好"})
            report = check_localization.build_report(instance, override)
        self.assertEqual(report.missing_language_keys, [])
        self.assertEqual(report.language_key_count, 1)

    def test_rejects_unknown_override_key(self) -> None:
        instance = self.make_instance(source_zh={"example.hello": "你好"})
        with tempfile.TemporaryDirectory() as temp:
            override = Path(temp)
            self.make_override(override, translatable={"example.unknown": "未知"})
            report = check_localization.build_report(instance, override)
        self.assertEqual(report.unknown_override_keys, [("example", "example.unknown")])

    def test_rejects_override_when_source_already_has_translation(self) -> None:
        instance = self.make_instance(source_zh={"example.hello": "你好"})
        with tempfile.TemporaryDirectory() as temp:
            override = Path(temp)
            self.make_override(override, translatable={"example.hello": "您好"})
            report = check_localization.build_report(instance, override)
        self.assertEqual(
            report.redundant_override_keys,
            [("example", "example.hello")],
        )

    def test_accepts_explicit_omnisearch_copy_overrides(self) -> None:
        source_en = {
            "hint.omnisearch.education": "Hold %s to inspect this item",
            "hint.omnisearch.hover_action": "Hold %s to inspect it",
            "key.categories.omnisearch": "Omnisearch",
            "key.omnisearch.inspect_hovered": "Inspect Hovered Item",
            "key.omnisearch.open": "Open Search Window",
        }
        source_zh = {
            "hint.omnisearch.education": "长按 %s 查看资料",
            "hint.omnisearch.hover_action": "长按 %s 查看资料",
            "key.categories.omnisearch": "Omnisearch",
            "key.omnisearch.inspect_hovered": "查询悬停物品",
            "key.omnisearch.open": "打开搜索窗口",
        }
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        instance = Path(temp.name)
        mods = instance / "mods"
        mods.mkdir()
        with ZipFile(mods / "omnisearch.jar", "w") as archive:
            archive.writestr(
                "assets/omnisearch/lang/en_us.json",
                json.dumps(source_en),
            )
            archive.writestr(
                "assets/omnisearch/lang/zh_cn.json",
                json.dumps(source_zh, ensure_ascii=False),
            )

        with tempfile.TemporaryDirectory() as temp_override:
            override = Path(temp_override)
            lang = override / "assets/omnisearch/lang"
            lang.mkdir(parents=True)
            (lang / "zh_cn.json").write_text(
                json.dumps(
                    {
                        "hint.omnisearch.education": "按住 %s 打开百科",
                        "hint.omnisearch.hover_action": "按住 %s 打开百科",
                        "key.categories.omnisearch": "MC 百科",
                        "key.omnisearch.inspect_hovered": "查询悬停物品",
                        "key.omnisearch.open": "打开百科搜索",
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            report = check_localization.build_report(instance, override)

        self.assertEqual(report.redundant_override_keys, [])
        self.assertEqual(report.placeholder_mismatches, [])

    def test_checks_placeholders_for_explicit_omnisearch_overrides(self) -> None:
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        instance = Path(temp.name)
        mods = instance / "mods"
        mods.mkdir()
        with ZipFile(mods / "omnisearch.jar", "w") as archive:
            archive.writestr(
                "assets/omnisearch/lang/en_us.json",
                json.dumps({"hint.omnisearch.education": "Hold %s to inspect this item"}),
            )
            archive.writestr(
                "assets/omnisearch/lang/zh_cn.json",
                json.dumps({"hint.omnisearch.education": "长按 %s 查看资料"}, ensure_ascii=False),
            )

        with tempfile.TemporaryDirectory() as temp_override:
            override = Path(temp_override)
            lang = override / "assets/omnisearch/lang"
            lang.mkdir(parents=True)
            (lang / "zh_cn.json").write_text(
                json.dumps({"hint.omnisearch.education": "按住 B 打开百科"}),
                encoding="utf-8",
            )
            report = check_localization.build_report(instance, override)

        self.assertEqual(
            report.placeholder_mismatches,
            [("omnisearch", "hint.omnisearch.education", ["%s"], [])],
        )

    def test_rejects_placeholder_mismatch(self) -> None:
        instance = self.make_instance(source_en={"example.hello": "Hello %s"})
        with tempfile.TemporaryDirectory() as temp:
            override = Path(temp)
            self.make_override(override, translatable={"example.hello": "你好"})
            report = check_localization.build_report(instance, override)
        self.assertEqual(
            report.placeholder_mismatches,
            [("example", "example.hello", ["%s"], [])],
        )

    def test_rejects_duplicate_override_json_key(self) -> None:
        instance = self.make_instance()
        with tempfile.TemporaryDirectory() as temp:
            override = Path(temp)
            lang = override / "assets" / "example" / "lang"
            lang.mkdir(parents=True)
            (lang / "zh_cn.json").write_text(
                '{"example.hello":"你好","example.hello":"您好"}',
                encoding="utf-8",
            )
            report = check_localization.build_report(instance, override)
        self.assertEqual(
            report.duplicate_override_keys,
            [("example", "example.hello")],
        )


    def test_accepts_reordered_explicit_placeholders(self) -> None:
        instance = self.make_instance(
            source_en={"example.hello": "%1$s gave %2$s"},
        )
        with tempfile.TemporaryDirectory() as temp:
            override = Path(temp)
            self.make_override(
                override,
                translatable={"example.hello": "%2$s 由 %1$s 交给"},
            )
            report = check_localization.build_report(instance, override)
        self.assertEqual(report.placeholder_mismatches, [])

    def test_accepts_explicit_reorder_for_anonymous_source_placeholders(self) -> None:
        instance = self.make_instance(
            source_en={"example.hello": "%s gave %s"},
        )
        with tempfile.TemporaryDirectory() as temp:
            override = Path(temp)
            self.make_override(
                override,
                translatable={"example.hello": "%2$s 由 %1$s 交给"},
            )
            report = check_localization.build_report(instance, override)
        self.assertEqual(report.placeholder_mismatches, [])

    def test_rejects_repeated_argument_replacing_distinct_placeholders(self) -> None:
        instance = self.make_instance(
            source_en={"example.hello": "%s gave %s"},
        )
        with tempfile.TemporaryDirectory() as temp:
            override = Path(temp)
            self.make_override(
                override,
                translatable={"example.hello": "%1$s 交给了 %1$s"},
            )
            report = check_localization.build_report(instance, override)
        self.assertEqual(len(report.placeholder_mismatches), 1)

    def test_reports_missing_guideme_page(self) -> None:
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        instance = Path(temp.name)
        mods = instance / "mods"
        mods.mkdir()
        with ZipFile(mods / "guide.jar", "w") as archive:
            archive.writestr(
                "assets/example/exampleguide/index.md",
                "---\nnavigation:\n  title: Index\n---\n# Hello\n",
            )
        with tempfile.TemporaryDirectory() as temp_override:
            report = check_localization.build_report(instance, Path(temp_override))
        self.assertEqual(
            report.missing_manual_pages,
            [("guideme", "example", "exampleguide", "index.md")],
        )

    def test_accepts_guideme_translation(self) -> None:
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        instance = Path(temp.name)
        mods = instance / "mods"
        mods.mkdir()
        with ZipFile(mods / "guide.jar", "w") as archive:
            archive.writestr(
                "assets/example/exampleguide/index.md",
                "---\nnavigation:\n  title: Index\n---\n# Hello\n",
            )
        with tempfile.TemporaryDirectory() as temp_override:
            override = Path(temp_override)
            self.make_override(
                override,
                manuals={"exampleguide/_zh_cn/index.md": "---\nnavigation:\n  title: 索引\n---\n# 你好\n"},
            )
            report = check_localization.build_report(instance, override)
        self.assertEqual(report.missing_manual_pages, [])
        self.assertEqual(report.manual_page_count, 1)

    def test_reports_missing_patchouli_page(self) -> None:
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        instance = Path(temp.name)
        mods = instance / "mods"
        mods.mkdir()
        with ZipFile(mods / "book.jar", "w") as archive:
            archive.writestr(
                "assets/example/patchouli_books/guide/en_us/entries/start.json",
                '{"name":"Start","text":"Welcome"}',
            )
        with tempfile.TemporaryDirectory() as temp_override:
            report = check_localization.build_report(instance, Path(temp_override))
        self.assertEqual(
            report.missing_manual_pages,
            [("patchouli", "example", "guide", "entries/start.json")],
        )

    def test_accepts_patchouli_translation(self) -> None:
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        instance = Path(temp.name)
        mods = instance / "mods"
        mods.mkdir()
        with ZipFile(mods / "book.jar", "w") as archive:
            archive.writestr(
                "assets/example/patchouli_books/guide/en_us/entries/start.json",
                '{"name":"Start","text":"Welcome"}',
            )
        with tempfile.TemporaryDirectory() as temp_override:
            override = Path(temp_override)
            self.make_override(
                override,
                manuals={
                    "patchouli_books/guide/zh_cn/entries/start.json": (
                        '{"name":"开始","text":"欢迎"}'
                    )
                },
            )
            report = check_localization.build_report(instance, override)
        self.assertEqual(report.missing_manual_pages, [])
        self.assertEqual(report.manual_page_count, 1)


    def test_rejects_unknown_and_redundant_guideme_overrides(self) -> None:
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        instance = Path(temp.name)
        mods = instance / "mods"
        mods.mkdir()
        with ZipFile(mods / "guide.jar", "w") as archive:
            archive.writestr("assets/example/exampleguide/index.md", "# Index")
            archive.writestr("assets/example/exampleguide/_zh_cn/existing.md", "# 已有")
        with tempfile.TemporaryDirectory() as temp_override:
            override = Path(temp_override)
            self.make_override(
                override,
                manuals={
                    "exampleguide/_zh_cn/index.md": "# 索引",
                    "exampleguide/_zh_cn/existing.md": "# 重复",
                    "exampleguide/_zh_cn/extra.md": "# 多余",
                },
            )
            report = check_localization.build_report(instance, override)
        self.assertEqual(
            report.unknown_manual_overrides,
            [("guideme", "example", "exampleguide", "extra.md")],
        )
        self.assertEqual(
            report.redundant_manual_overrides,
            [("guideme", "example", "exampleguide", "existing.md")],
        )
        self.assertEqual(report.manual_page_count, 1)

    def test_rejects_unknown_and_redundant_patchouli_overrides(self) -> None:
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        instance = Path(temp.name)
        mods = instance / "mods"
        mods.mkdir()
        with ZipFile(mods / "book.jar", "w") as archive:
            archive.writestr(
                "assets/example/patchouli_books/guide/en_us/entries/start.json",
                '{"name":"Start"}',
            )
            archive.writestr(
                "assets/example/patchouli_books/guide/zh_cn/entries/existing.json",
                '{"name":"已有"}',
            )
        with tempfile.TemporaryDirectory() as temp_override:
            override = Path(temp_override)
            self.make_override(
                override,
                manuals={
                    "patchouli_books/guide/zh_cn/entries/start.json": '{"name":"开始"}',
                    "patchouli_books/guide/zh_cn/entries/existing.json": '{"name":"重复"}',
                    "patchouli_books/guide/zh_cn/entries/extra.json": '{"name":"多余"}',
                },
            )
            report = check_localization.build_report(instance, override)
        self.assertEqual(
            report.unknown_manual_overrides,
            [("patchouli", "example", "guide", "entries/extra.json")],
        )
        self.assertEqual(
            report.redundant_manual_overrides,
            [("patchouli", "example", "guide", "entries/existing.json")],
        )
        self.assertEqual(report.manual_page_count, 1)

if __name__ == "__main__":
    unittest.main()

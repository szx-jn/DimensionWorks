from __future__ import annotations
import hashlib
import importlib.util
import io
import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'scripts'))
import fixed_source
spec = importlib.util.spec_from_file_location('pack_preflight', ROOT / '.codex/hooks/pack_preflight.py')
preflight = importlib.util.module_from_spec(spec)
spec.loader.exec_module(preflight)

class FixedSourceTest(unittest.TestCase):
    def source(self):
        return dict(id='fixture', repository='owner/repo', commit='a'*40, source_sha256='b'*64,
                    sha256=hashlib.sha256(b'expected jar').hexdigest(), file_name='native-1.21.1.jar')
    def test_requires_immutable_and_safe_source_fields(self):
        for field,value in [('commit','main'),('repository','../..'),('file_name','../native.jar'),('sha256',''),('commit',None)]:
            with self.subTest(field=field,value=value):
                mod=self.source();mod[field]=value
                with self.assertRaises(ValueError):fixed_source.validate_source(mod)
    def test_validates_entire_archive_before_extraction(self):
        with tempfile.TemporaryDirectory() as name:
            root=Path(name);archive=root/'source.zip';out=root/'out';out.mkdir()
            with zipfile.ZipFile(archive,'w') as z:z.writestr('repo/valid','data');z.writestr('repo/../../escape','invalid')
            with self.assertRaises(ValueError):fixed_source.extract_source(archive,out)
            self.assertEqual(list(out.iterdir()),[])
            self.assertFalse((root/'escape').exists())
    def test_source_checksum_failure_does_not_build(self):
        with tempfile.TemporaryDirectory() as name, patch.object(fixed_source.urllib.request,'urlopen',return_value=io.BytesIO(b'bad archive')), patch.object(fixed_source.subprocess,'run') as run:
            with self.assertRaisesRegex(ValueError,'source checksum'):fixed_source.build_jar(self.source(),Path(name))
            run.assert_not_called()
    def test_only_expected_artifact_is_reused_and_wrong_build_is_rejected(self):
        with tempfile.TemporaryDirectory() as name:
            root=Path(name);mod=self.source();cache=root/'github-source/owner/repo'/mod['commit'];artifact=cache/'source/build/libs'/mod['file_name'];artifact.parent.mkdir(parents=True);artifact.write_bytes(b'expected jar')
            with patch.object(fixed_source.subprocess,'run') as run:
                self.assertEqual(fixed_source.build_jar(mod,root),artifact);run.assert_not_called()
            artifact.write_bytes(b'bad jar');archive=cache/'source.zip'
            with zipfile.ZipFile(archive,'w') as z:z.writestr('repo/gradlew','fixture')
            mod['source_sha256']=fixed_source.sha256(archive)
            with patch.object(fixed_source.subprocess,'run',return_value=subprocess.CompletedProcess([],0)):
                with self.assertRaisesRegex(ValueError,'artifact checksum'):fixed_source.build_jar(mod,root)

class PreflightInvocationTest(unittest.TestCase):
    def test_source_read_and_compilation_do_not_trigger_packaging(self):
        for command in ['python3 -m py_compile scripts/package_mrpack.py', 'python3 -c "print(\'scripts/package_mrpack.py\')"', 'python3 other.py scripts/package_mrpack.py']:
            with self.subTest(command=command):self.assertFalse(preflight.is_packaging_command(command))
    def test_direct_and_module_packaging_preserve_arguments(self):
        for command in ['python3 -u scripts/package_mrpack.py --version 0.3.52', 'python3 -m package_mrpack --version 0.3.52']:
            self.assertTrue(preflight.is_packaging_command(command));self.assertEqual(preflight.packaging_tokens(command),['--version','0.3.52'])
    def test_real_packaging_still_denies_skip_hook(self):
        event=dict(hook_event_name='PreToolUse',tool_name='Bash',tool_input={'command':'python3 scripts/package_mrpack.py --skip-hook'})
        output=io.StringIO()
        with patch.object(preflight.sys,'stdin',io.StringIO(json.dumps(event))),patch.object(preflight.sys,'stdout',output),patch.object(preflight.subprocess,'run') as run:
            self.assertEqual(preflight.main(),0);run.assert_not_called()
        self.assertEqual(json.loads(output.getvalue())['hookSpecificOutput']['permissionDecision'],'deny')

if __name__=='__main__':unittest.main()

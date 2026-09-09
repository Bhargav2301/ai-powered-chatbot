"""Record APK identity/content and verify the distributable offline model pack."""
import argparse
import hashlib
import json
import pathlib
import re
import subprocess
import zipfile


def inspect(apk, aapt, bundled):
    path = pathlib.Path(apk)
    lock = json.loads((pathlib.Path(__file__).resolve().parents[1] / 'docs/local-model-lock.json').read_text())
    badging = subprocess.check_output([aapt, 'dump', 'badging', str(path)], text=True)
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    assert package, 'APK package identity missing'
    with path.open('rb') as source:
        apk_digest = hashlib.file_digest(source, 'sha256').hexdigest()
    info = dict(application_id=package[1], version_code=int(package[2]), version_name=package[3],
                apk_bytes=path.stat().st_size, apk_sha256=apk_digest)
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        info['native_abis'] = [n.split('/')[1] for n in names if n.endswith('/libpolymath_llm.so')]
        dex = b''.join(archive.read(n) for n in names if re.fullmatch(r'classes\d*\.dex', n))
        info['contains_local_setup_text'] = b'Qwen3' in dex and (b'Download Qwen' in dex or b'Prepare included AI' in dex)
        info['bundled_gguf_files'] = [n for n in names if n.endswith('.gguf')]
        if bundled:
            name = 'assets/models/' + lock['file']
            assert info['application_id'] == 'com.polymath.app.offline'
            assert info['version_name'] == '0.3.1' and info['version_code'] == 4
            assert "application-label:'Polymath Offline'" in badging
            assert archive.getinfo(name).file_size == lock['bytes']
            assert archive.getinfo(name).compress_type == zipfile.ZIP_STORED
            with archive.open(name) as source:
                digest = hashlib.file_digest(source, 'sha256').hexdigest()
            assert digest == lock['sha256'], 'Bundled weights differ from approved model'
            assert {'arm64-v8a', 'x86_64'} <= set(info['native_abis'])
            assert info['contains_local_setup_text']
            info['model_sha256'] = digest
    return info


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('apk')
    parser.add_argument('--aapt', required=True)
    parser.add_argument('--require-bundled', action='store_true')
    args = parser.parse_args()
    print(json.dumps(inspect(args.apk, args.aapt, args.require_bundled), indent=2))

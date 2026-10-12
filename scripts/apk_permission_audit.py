#!/usr/bin/env python3
"""Audit permissions of the *built APK*, including transitive merged manifests."""
from pathlib import Path
import re
import subprocess
import sys

ALLOWED = {
    'android.permission.INTERNET',
    'android.permission.FOREGROUND_SERVICE',
    'android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED',
    'android.permission.RECEIVE_BOOT_COMPLETED',
    'android.permission.POST_NOTIFICATIONS',
    'android.permission.HIDE_OVERLAY_WINDOWS',
}

def parse_permissions(aapt_output: str) -> set[str]:
    return set(re.findall(r"uses-permission(?:-sdk-23)?:\s+name='([^']+)'", aapt_output))

def main() -> int:
    if len(sys.argv) != 3:
        print('usage: apk_permission_audit.py <aapt-path> <debug.apk>')
        return 2
    aapt, apk = sys.argv[1:]
    if not Path(apk).is_file():
        print('APK_SECURITY_GATE_FAILED: APK missing')
        return 2
    result = subprocess.run([aapt, 'dump', 'permissions', apk],
                            capture_output=True, text=True, check=False)
    if result.returncode != 0:
        print('APK_SECURITY_GATE_FAILED: aapt failed')
        return 2
    actual = parse_permissions(result.stdout)
    if actual != ALLOWED:
        print(f'APK_SECURITY_GATE_FAILED: unexpected={sorted(actual - ALLOWED)}, missing={sorted(ALLOWED - actual)}')
        return 1
    print('APK_SECURITY_PERMISSIONS_OK: declared APK permissions match least-privilege allowlist')
    return 0

if __name__ == '__main__':
    sys.exit(main())

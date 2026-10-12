#!/usr/bin/env python3
"""Fail closed on common privacy-scope regressions; not a substitute for APK audit."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
android = '{http://schemas.android.com/apk/res/android}'
permissions = {p.attrib.get(android+'name', '') for p in manifest.findall('uses-permission')}
allowed = {
    'android.permission.INTERNET',
    'android.permission.FOREGROUND_SERVICE',
    'android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED',
    'android.permission.RECEIVE_BOOT_COMPLETED',
    'android.permission.POST_NOTIFICATIONS',
    'android.permission.HIDE_OVERLAY_WINDOWS',
}
problems = []
if permissions != allowed:
    problems.append(f'Permission allowlist differs: extra={permissions - allowed}, missing={allowed - permissions}')
application = manifest.find('application')
for attr, desired in {
    'allowBackup': 'false',
    'usesCleartextTraffic': 'false',
    'dataExtractionRules': '@xml/data_extraction_rules',
}.items():
    if application is None or application.attrib.get(android+attr) != desired:
        problems.append(f'Application attribute {attr} must equal {desired}')
allowed_components = {
    ('activity', '.MainActivity'): (True, None),
    ('service', '.network.CompromissoVpnService'): (True, 'android.permission.BIND_VPN_SERVICE'),
    ('receiver', '.admin.CompromissoDeviceAdminReceiver'): (True, 'android.permission.BIND_DEVICE_ADMIN'),
    ('receiver', '.boot.BootReceiver'): (False, None),
    ('receiver', '.daily.DailyVerseReceiver'): (False, None),
}
seen_components = set()
for el in ([] if application is None else application):
    if el.tag not in ('activity', 'service', 'receiver', 'provider', 'activity-alias'):
        continue
    name = el.attrib.get(android + 'name')
    key = (el.tag, name)
    seen_components.add(key)
    expected = allowed_components.get(key)
    if expected is None:
        problems.append(f'Unexpected application component {key}')
        continue
    exported, permission = expected
    if el.attrib.get(android + 'exported') != str(exported).lower():
        problems.append(f'Incorrect exported value for {key}')
    if el.attrib.get(android + 'permission') != permission:
        problems.append(f'Incorrect permission for {key}')
for key in allowed_components.keys() - seen_components:
    problems.append(f'Missing required component {key}')
vpn = application.find("service[@{http://schemas.android.com/apk/res/android}name='.network.CompromissoVpnService']") if application is not None else None
supports = vpn.find("meta-data[@{http://schemas.android.com/apk/res/android}name='android.net.VpnService.SUPPORTS_ALWAYS_ON']") if vpn is not None else None
if supports is None or supports.attrib.get(android+'value') != 'false':
    problems.append('DNS-only VPN must not advertise Always-on until full-tunnel verified')
source = '\n'.join(p.read_text(encoding='utf-8') for p in (ROOT / 'app/src/main/java').rglob('*.kt'))
for token in ['MediaStore.', 'READ_MEDIA_', 'READ_EXTERNAL_STORAGE', 'MANAGE_EXTERNAL_STORAGE',
              'AccessibilityService', 'getInstalledPackages(', 'getInstalledApplications(',
              'TelephonyManager', 'AccountManager', 'ClipboardManager', 'WebView(',
              'ContentResolver', 'MediaStore', 'PackageManager.GET', 'QUERY_ALL_PACKAGES',
              'Runtime.getRuntime().exec', 'DatagramSocket(', 'UPSTREAM_DNS']:
    if token in source:
        problems.append(f'Unexpected sensitive API / insecure DNS token: {token}')
for p in (ROOT / 'app/src/main').rglob('*.xml'):
    try: ET.parse(p)
    except ET.ParseError as error: problems.append(f'Invalid XML {p}: {error}')
if problems:
    print('SOURCE_SECURITY_GATE_FAILED')
    for item in problems: print(' -', item)
    sys.exit(1)
print('SOURCE_SECURITY_GATE_OK: manifest, permissions, exported components, XML, banned APIs')

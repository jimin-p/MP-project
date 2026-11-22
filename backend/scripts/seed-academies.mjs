import { readFile } from 'node:fs/promises';
import { normalizeCatalog } from './catalog.mjs';

const args = process.argv.slice(2);
const projectId = args[args.indexOf('--project') + 1];
if (!args.includes('--project') || !projectId || projectId.startsWith('--')) {
  throw new Error('대상 프로젝트를 --project PROJECT_ID 로 지정하세요. 기본 동작은 미리보기입니다.');
}
const catalog = normalizeCatalog(JSON.parse(await readFile(new URL('../../app/src/main/assets/academies.json', import.meta.url), 'utf8')));
console.log(`프로젝트 ${projectId}: 샘플 학원 ${catalog.length}개 검증 완료`);
if (!args.includes('--write')) {
  console.log('미리보기 완료. 업로드하려면 같은 명령에 --write 를 추가하세요. 기존 문서는 덮어쓰지 않습니다.');
} else {
  const emulator = process.env.FIRESTORE_EMULATOR_HOST;
  if (emulator && !projectId.startsWith('demo-')) throw new Error('에뮬레이터에는 demo- 프로젝트 ID를 사용하세요.');
  if (!emulator && projectId.startsWith('demo-')) throw new Error('demo- 프로젝트는 FIRESTORE_EMULATOR_HOST가 필요합니다.');
  const { initializeApp, applicationDefault, deleteApp } = await import('firebase-admin/app');
  const { getFirestore, FieldValue } = await import('firebase-admin/firestore');
  if (!emulator && args.includes('--firebase-cli')) {
    const { createRequire } = await import('node:module');
    const require = createRequire(import.meta.url);
    const auth = require('firebase-tools/lib/auth');
    const account = auth.getProjectDefaultAccount(process.cwd(), projectId) || auth.getGlobalDefaultAccount();
    if (!account?.tokens?.refresh_token) throw new Error('먼저 firebase login을 실행하세요.');
    const token = await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform', 'https://www.googleapis.com/auth/firebase']);
    const toValue = value => typeof value === 'boolean' ? { booleanValue: value } : typeof value === 'number' ? (Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value }) : { stringValue: value };
    let created = 0;
    for (const entry of catalog) {
      const name = `projects/${projectId}/databases/(default)/documents/academies/${entry.id}`;
      const response = await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents:commit`, {
        method: 'POST', headers: { Authorization: `Bearer ${token.access_token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ writes: [{ update: { name, fields: Object.fromEntries(Object.entries(entry.data).map(([key, value]) => [key, toValue(value)])) }, currentDocument: { exists: false }, updateTransforms: [{ fieldPath: 'createdAt', setToServerValue: 'REQUEST_TIME' }] }] }),
        signal: AbortSignal.timeout(30000)
      });
      if (response.ok) created++;
      else { const error = await response.json(); if (error.error?.status !== 'ALREADY_EXISTS') throw new Error(JSON.stringify(error.error)); }
    }
    console.log(`추가 ${created}개, 기존 문서 유지 ${catalog.length - created}개`);
    process.exit(0);
  }
  const app = initializeApp({ projectId, ...(emulator ? {} : { credential: applicationDefault() }) });
  try {
    const db = getFirestore(app);
    let created = 0;
    for (const entry of catalog) {
      try {
        await db.collection('academies').doc(entry.id).create({ ...entry.data, createdAt: FieldValue.serverTimestamp() });
        created++;
      } catch (error) {
        if (error.code !== 6 && error.code !== 'already-exists') throw error;
      }
    }
    console.log(`추가 ${created}개, 기존 문서 유지 ${catalog.length - created}개`);
  } finally { await deleteApp(app); }
}

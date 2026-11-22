import { before, after, beforeEach, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, getDocs, collection, setDoc, updateDoc, deleteDoc, serverTimestamp } from 'firebase/firestore';
let environment;
before(async () => {
  environment = await initializeTestEnvironment({projectId: 'demo-classpot', firestore: {
    rules: await readFile(new URL('../../firestore.rules', import.meta.url), 'utf8')
  }});
});
after(async () => { await environment?.cleanup(); });
beforeEach(async () => { await environment.clearFirestore(); });
const user = (uid = 'alice') => environment.authenticatedContext(uid, {email: `${uid}@example.com`}).firestore();
const profile = () => ({email: 'alice@example.com', displayName: '', createdAt: serverTimestamp(), updatedAt: serverTimestamp()});

test('only authenticated clients can read catalog; no client can write catalog', async () => {
  await environment.withSecurityRulesDisabled(async context => setDoc(doc(context.firestore(), 'academies/a'), {name: '학원'}));
  await assertSucceeds(getDocs(collection(user(), 'academies')));
  await assertFails(getDoc(doc(environment.unauthenticatedContext().firestore(), 'academies/a')));
  await assertFails(setDoc(doc(user(), 'academies/b'), {name: '위조'}));
  await assertFails(updateDoc(doc(user(), 'academies/a'), {rating: 5}));
  await assertFails(deleteDoc(doc(user(), 'academies/a')));
});
test('owner creates and updates profile while createdAt stays unchanged', async () => {
  const ref = doc(user(), 'users/alice');
  await assertSucceeds(setDoc(ref, profile()));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(updateDoc(ref, {displayName: '학생', updatedAt: serverTimestamp()}));
  await assertFails(updateDoc(ref, {createdAt: new Date(0), updatedAt: serverTimestamp()}));
});
test('cross-user access, profile enumeration, deletion and role escalation are denied', async () => {
  await assertSucceeds(setDoc(doc(user(), 'users/alice'), profile()));
  await assertFails(getDoc(doc(user('bob'), 'users/alice')));
  await assertFails(setDoc(doc(user('bob'), 'users/alice'), profile()));
  await assertFails(getDocs(collection(user(), 'users')));
  await assertFails(deleteDoc(doc(user(), 'users/alice')));
  await assertFails(updateDoc(doc(user(), 'users/alice'), {role: 'admin', updatedAt: serverTimestamp()}));
});
test('spoofed email, missing fields and arbitrary timestamps are denied', async () => {
  const ref = doc(user(), 'users/alice');
  await assertFails(setDoc(ref, {...profile(), email: 'bob@example.com'}));
  await assertFails(setDoc(ref, {email: 'alice@example.com'}));
  await assertFails(setDoc(ref, {...profile(), updatedAt: new Date(0)}));
});
test('unimplemented collections and public ping stay closed', async () => {
  await assertFails(setDoc(doc(user(), 'reviews/a'), {rating: 5}));
  await assertFails(setDoc(doc(user(), 'enrollments/a'), {status: 'approved'}));
  await assertFails(getDoc(doc(environment.unauthenticatedContext().firestore(), 'connection_test/ping')));
});

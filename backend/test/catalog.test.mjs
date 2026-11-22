import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { normalizeCatalog } from '../scripts/catalog.mjs';
const sample = JSON.parse(await readFile(new URL('../../app/src/main/assets/academies.json', import.meta.url), 'utf8'));

test('entire bundled catalog is valid with stable unique IDs and canonical fields', () => {
  const result = normalizeCatalog(sample);
  assert.equal(result.length, 130);
  assert.equal(new Set(result.map(x => x.id)).size, result.length);
  assert.deepEqual(result, normalizeCatalog([...sample]));
  assert.ok(result.every(x => x.data.price && x.data.image && x.data.isSample && x.data.reviewCount === 0));
});
test('duplicate academies fail before writes', () => assert.throws(() => normalizeCatalog([sample[0], sample[0]])));
test('malformed numeric or category fields fail before writes', () => {
  for (const change of [{latitude: 91}, {longitude: NaN}, {tuition: -1}, {tuition: 1.5}, {rating: 6}, {category: 'unknown'}]) {
    assert.throws(() => normalizeCatalog([{...sample[0], ...change}]));
  }
});
test('empty or malformed catalogs fail', () => {
  for (const input of [[], {}, [{name: ''}]]) assert.throws(() => normalizeCatalog(input));
});

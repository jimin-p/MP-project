import { createHash } from 'node:crypto';

export function normalizeCatalog(input) {
  if (!Array.isArray(input) || input.length === 0) throw new Error('학원 배열이 비어 있습니다.');
  const ids = new Set();
  return input.map((item, index) => {
    const error = message => { throw new Error(`${index + 1}번째 학원: ${message}`); };
    const text = key => typeof item[key] === 'string' && item[key].trim() ? item[key].trim() : error(`${key} 누락`);
    const number = (key, min, max, integer = false) => {
      const value = item[key];
      if (typeof value !== 'number' || !Number.isFinite(value) || value < min || value > max || (integer && !Number.isInteger(value))) error(`${key} 범위/형식 오류`);
      return value;
    };
    const name = text('name'), address = text('address');
    const category = text('category') === '교육' ? '입시교육' : text('category');
    if (!['입시교육', '어학', '예체능', '자격증', '원데이클래스'].includes(category)) error('알 수 없는 category');
    const id = 'sample-' + createHash('sha256').update(`${name}\n${address}`).digest('hex').slice(0, 24);
    if (ids.has(id)) error('이름과 주소가 같은 중복 학원');
    ids.add(id);
    return { id, data: {
      name, address, category,
      latitude: number('latitude', -90, 90), longitude: number('longitude', -180, 180),
      tuition: number('tuition', 0, 2147483647, true), rating: number('rating', 0, 5),
      curriculum: text('curriculum'), price: text(item.price !== undefined ? 'price' : '가격'),
      image: text(item.image !== undefined ? 'image' : '이미지'),
      reviewCount: 0, isSample: true
    }};
  });
}

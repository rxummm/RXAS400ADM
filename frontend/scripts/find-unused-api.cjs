const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..', 'src');

function walk(dir, exts, acc = []) {
  for (const f of fs.readdirSync(dir)) {
    const p = path.join(dir, f);
    const s = fs.statSync(p);
    if (s.isDirectory()) walk(p, exts, acc);
    else if (exts.some((e) => p.endsWith(e))) acc.push(p);
  }
  return acc;
}

const apiDir = path.join(ROOT, 'api');
const apiFiles = walk(apiDir, ['.ts']);
const consumers = walk(ROOT, ['.vue', '.ts']).filter((f) => !f.startsWith(apiDir));
const consumerContent = consumers.map((f) => fs.readFileSync(f, 'utf8')).join('\n');

const unused = [];
for (const apiFile of apiFiles) {
  const content = fs.readFileSync(apiFile, 'utf8');
  const exports = [
    ...[...content.matchAll(/export const (\w+)/g)].map((m) => m[1]),
    ...[...content.matchAll(/export function (\w+)/g)].map((m) => m[1]),
  ];
  for (const name of exports) {
    const re = new RegExp('\\b' + name + '\\b');
    if (!re.test(consumerContent)) unused.push(name + '  @  ' + path.relative(ROOT, apiFile));
  }
}
console.log(unused.length ? unused.join('\n') : '(无未使用的导出函数)');
console.log('--- 共检查导出符号，未使用 ' + unused.length + ' 个 ---');

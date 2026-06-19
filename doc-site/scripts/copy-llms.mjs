// 將 docusaurus-plugin-llms 於 build 產生的 llms.txt / llms-full.txt
// 複製到 repo 根目錄，作為「進版控、平時開發可直接讀」的彙整副本。
// 由 package.json 的 postbuild 腳本於 `npm run build` 後自動執行。
import { copyFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const buildDir = join(here, '..', 'build');     // doc-site/build
const repoRoot = join(here, '..', '..');         // repo 根目錄

const files = ['llms.txt', 'llms-full.txt'];
let copied = 0;
for (const name of files) {
  const src = join(buildDir, name);
  if (!existsSync(src)) {
    console.warn(`[copy-llms] 找不到 ${src}，略過（請確認 docusaurus-plugin-llms 已正確產生）`);
    continue;
  }
  copyFileSync(src, join(repoRoot, name));
  console.log(`[copy-llms] 已複製 ${name} 至 repo 根目錄`);
  copied++;
}
if (copied === 0) {
  console.warn('[copy-llms] 未複製任何檔案。');
}

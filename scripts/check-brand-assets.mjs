import { createHash } from 'node:crypto';
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const expectedHashes = new Map([
  ['.github/assets/readme-header.png', 'c6b02c166edda405a2b9636e4d932014755762b197c78aa8a3af9bbb33172658'],
  ['.github/assets/social-preview.png', 'd8029fc8ff1eada727f59452bc9970f61d375c1990735e47860a86f7ca54bbfc'],
  ['docs/assets/logo.svg', 'bc292ca3fa5b19c0e736fb070147c58c1ef4e79a6156d7cc88d1c9e4c4e99cd3'],
  ['docs/assets/favicon.svg', 'bc292ca3fa5b19c0e736fb070147c58c1ef4e79a6156d7cc88d1c9e4c4e99cd3'],
  ['docs/assets/social-preview.png', 'd8029fc8ff1eada727f59452bc9970f61d375c1990735e47860a86f7ca54bbfc'],
]);

const read = (file) => readFileSync(resolve(root, file), 'utf8').replace(/\r\n/g, '\n');
const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};

for (const [file, expected] of expectedHashes) {
  const absolute = resolve(root, file);
  assert(existsSync(absolute), `missing required O10 source asset: ${file}`);
  const actual = createHash('sha256').update(readFileSync(absolute)).digest('hex');
  assert(actual === expected, `${file} has ${actual}; expected the oss-brand v0.2.0 hash ${expected}`);
}

const logo = read('docs/assets/logo.svg');
assert(logo.includes('data-oss-project="O10"'), 'docs logo must identify the O10 project asset');
assert(logo.includes('data-layer="q-frame"') && logo.includes('<rect x="5" y="5" width="16" height="16" rx="2"') && logo.includes('<rect x="11" y="11" width="16" height="16" rx="2"'), 'docs logo must use the shared Q frame');
assert(logo.includes('M19 13V23') && logo.includes('M14 18H24'), 'docs logo must use the O10 kit route');
assert(!logo.includes('M 4 4 L 28 4'), 'docs logo must not retain the former shared backend mark');
assert(!logo.includes('M7 6H21V20H7Z'), 'docs logo must not use the O08 geometry');
assert(!logo.includes('M5 8H13V14H5Z'), 'docs logo must not use the O09 geometry');

for (const [file, endorsement] of [
  ['README.md', 'Open source by [DevsLab](https://devslab.kr/)'],
  ['README.ko.md', '[DevsLab](https://devslab.kr/) 오픈소스'],
  ['docs/index.md', 'Open source by DevsLab'],
  ['docs/index.ko.md', 'DevsLab 오픈소스'],
]) {
  const content = read(file);
  assert(content.includes('https://devslab.kr/brand/open-source/'), `${file} must link to the canonical OSS brand guide`);
  assert(content.includes(endorsement), `${file} must include its localized DevsLab endorsement`);
}

const mkdocs = read('mkdocs.yml');
assert(mkdocs.includes('custom_dir: docs/overrides'), 'mkdocs must load the source-only template override');
assert(mkdocs.includes('favicon: assets/favicon.svg'), 'mkdocs must use the O10 favicon source');
assert(mkdocs.includes('logo: assets/logo.svg'), 'mkdocs must use the O10 logo source');

const metadata = read('docs/overrides/main.html');
assert(metadata.includes('https://devslab-kit.devslab.kr/assets/social-preview.png'), 'metadata must use the local O10 social preview');
assert(metadata.includes('og:image:alt') && metadata.includes('twitter:image:alt'), 'metadata must describe the social preview');

const styles = read('docs/stylesheets/extra.css');
assert(styles.includes('.oss-project-intro') && styles.includes('data-atmosphere="project"'), 'docs need the O10 identity panel and scoped atmosphere');
const slateAtmosphere = `[data-md-color-scheme="slate"] .oss-project-intro[data-atmosphere="project"]::before {
  background: linear-gradient(135deg, rgb(34 211 238 / 0.10), transparent 66%);
}`;
assert(styles.includes(slateAtmosphere), 'Material slate must apply the O10 cyan atmosphere directly at the 0.10 opacity cap');
assert(styles.includes('pointer-events: none'), 'atmosphere decoration must not intercept input');
assert(styles.includes('@media (forced-colors: active), print'), 'atmosphere must be suppressed for forced colors and print');

console.log(`O10 brand source contract passed (${expectedHashes.size} exact assets).`);

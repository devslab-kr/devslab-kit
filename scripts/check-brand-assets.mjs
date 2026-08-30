import { createHash } from 'node:crypto';
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const expectedHashes = new Map([
  ['.github/assets/readme-header.png', '181bb155b383789b0e88454a4ff9d16dafd22d5083140fa83255bb4e306785a2'],
  ['.github/assets/social-preview.png', '4666515df130b8d85a5e4ac3fa38c5699af938c9a30eeab98a3ace0d5d301e2c'],
  ['docs/assets/logo.svg', '927af7009de24040ac94c0586f3d93dac30c28b08c1d29b2a83c1bf74f1c4248'],
  ['docs/assets/favicon.svg', '927af7009de24040ac94c0586f3d93dac30c28b08c1d29b2a83c1bf74f1c4248'],
  ['docs/assets/social-preview.png', '4666515df130b8d85a5e4ac3fa38c5699af938c9a30eeab98a3ace0d5d301e2c'],
]);

const read = (file) => readFileSync(resolve(root, file), 'utf8');
const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};

for (const [file, expected] of expectedHashes) {
  const absolute = resolve(root, file);
  assert(existsSync(absolute), `missing required O10 source asset: ${file}`);
  const actual = createHash('sha256').update(readFileSync(absolute)).digest('hex');
  assert(actual === expected, `${file} has ${actual}; expected the oss-brand v0.1.1 hash ${expected}`);
}

const logo = read('docs/assets/logo.svg');
assert(logo.includes('data-oss-project="O10"'), 'docs logo must identify the O10 project asset');
assert(logo.includes('M5 5H13V13H5Z') && logo.includes('M13 10H19V22H13Z'), 'docs logo must use the O10 lattice geometry');
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

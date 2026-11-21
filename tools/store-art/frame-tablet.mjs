import { createRequire } from 'node:module';
import { mkdirSync, readFileSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const { chromium } = createRequire(import.meta.url)('playwright');
const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, '..', '..');
const fontDir = join(repo, 'app/src/main/res/font');
const captures = join(here, 'captures');
const out = join(here, 'out');

const devices = {
  'tablet-7': { width: 1200, height: 1920, orientation: 'portrait', screenWidth: 800 },
  'tablet-10': { width: 2560, height: 1600, orientation: 'landscape', screenWidth: 1740 },
};

const shots = [
  { file: '01-daily', title: 'A calm puzzle', accent: 'every day', sub: 'One new daily puzzle, the same for everyone.' },
  { file: '02-games', title: 'Sudoku, words,', accent: 'memory & more', sub: 'Filter by logic, word, memory or spatial.' },
  { file: '03-sudoku-play', title: 'Calm puzzles.', accent: 'Nothing else.', sub: 'No ads, no pop-ups, no pressure.' },
  { file: '04-game-page', title: 'Play at', accent: 'your own pace', sub: 'Four levels, easy to expert. The timer is optional.' },
  { file: '05-wordsearch', title: 'Word puzzles', accent: 'in your language', sub: 'Find the words, then solve the hidden bonus word.' },
  { file: '06-starbattle', title: 'Easy on', accent: 'the eyes', sub: 'Seven themes, including a colorblind-friendly one.' },
  { file: '07-stats', title: 'Your progress', accent: 'stays with you', sub: 'Stats and streaks, stored only on your device.' },
  { file: '08-privacy', title: 'Nothing leaves', accent: 'your device', sub: 'Private by design, and open source.', privacy: true },
];

const privacyRows = [
  ['No ads', 'Not now, not ever.', 'x'],
  ['No account', 'Open the app and play.', 'x'],
  ['No internet needed', 'Works fully offline.', 'x'],
  ['No tracking', 'No analytics, no data collection.', 'x'],
  ['Open source', 'The code is public on GitHub.', 'check'],
];

const font = (file, weight) =>
  `@font-face{font-family:'Jakarta';font-weight:${weight};src:url('data:font/ttf;base64,${readFileSync(join(fontDir, file)).toString('base64')}')}`;
const fonts = [
  font('plus_jakarta_sans_regular.ttf', 400),
  font('plus_jakarta_sans_medium.ttf', 500),
  font('plus_jakarta_sans_bold.ttf', 700),
  font('plus_jakarta_sans_extrabold.ttf', 800),
].join('\n');

const icon = (kind) =>
  kind === 'check'
    ? '<svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/></svg>'
    : '<svg viewBox="0 0 24 24"><path d="M7 7l10 10M17 7L7 17" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round"/></svg>';

const appMark =
  '<div class="appmark"><svg viewBox="0 0 24 24"><rect x="4" y="4" width="7" height="7" rx="2" fill="#fff"/><rect x="13" y="4" width="7" height="7" rx="2" fill="#fff"/><rect x="4" y="13" width="7" height="7" rx="2" fill="#fff"/><rect x="13" y="13" width="7" height="7" rx="2" fill="#fff"/></svg></div>';

function statusBar(dark, scale) {
  const ink = dark ? '#f5f3ff' : '#1b1b1f';
  return `<div class="status" style="color:${ink};height:${40 * scale}px;font-size:${22 * scale}px;padding:0 ${36 * scale}px">
    <span>9:41</span>
    <span class="icons" style="gap:${10 * scale}px">
      <svg viewBox="0 0 24 24" style="width:${26 * scale}px"><path d="M12 20l-11-12a16 16 0 0 1 22 0z" fill="${ink}"/></svg>
      <svg viewBox="0 0 24 24" style="width:${22 * scale}px"><path d="M22 3v19H3z" fill="${ink}"/></svg>
      <svg viewBox="0 0 24 24" style="width:${16 * scale}px"><rect x="6" y="2" width="12" height="20" rx="2" fill="${ink}"/></svg>
    </span></div>`;
}

function page(device, shot, capture) {
  const d = devices[device];
  const s = d.width / 1200;
  const portrait = d.orientation === 'portrait';
  const titleSize = portrait ? 108 : 112;
  const subSize = portrait ? 40 : 48;
  const bezel = Math.round(d.screenWidth * 0.032);
  const radius = Math.round(d.screenWidth * 0.06);
  const statusScale = portrait ? 1 : 1.35;
  const k = portrait ? 1 : 1.3;
  const body = shot.privacy
    ? `<div class="privacy" style="width:${portrait ? 960 : 2160}px">
        ${privacyRows
          .map(([t, sub, k]) => `<div class="row"><div class="badge">${icon(k)}</div><div><div class="rt">${t}</div><div class="rs">${sub}</div></div></div>`)
          .join('')}
        ${appMark}
      </div>`
    : `<div class="device" style="padding:${bezel}px;border-radius:${radius}px">
        <div class="screen" style="width:${d.screenWidth}px;border-radius:${radius - bezel / 2}px;background:${capture.top}">
          ${statusBar(capture.dark, statusScale)}
          <img src="${capture.uri}" style="width:${d.screenWidth}px;display:block"/>
        </div>
      </div>`;
  return `<!DOCTYPE html><html><head><style>
    ${fonts}
    *{box-sizing:border-box;margin:0;padding:0}
    body{width:${d.width}px;height:${d.height}px;overflow:hidden;font-family:'Jakarta';color:#fff;
      background:radial-gradient(ellipse 70% 50% at 85% 8%, #5b2bb5 0%, rgba(91,43,181,0) 70%),
        radial-gradient(ellipse 60% 40% at 0% 100%, #3a1a7a 0%, rgba(58,26,122,0) 70%),
        linear-gradient(180deg,#2a1260 0%,#170c36 55%,#0f0a22 100%);
      display:flex;flex-direction:column;align-items:center;position:relative}
    body > *{flex:none}
    body::before{content:'';position:absolute;inset:0;
      background-image:linear-gradient(rgba(255,255,255,.035) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.035) 1px,transparent 1px);
      background-size:${Math.round(180 * s)}px ${Math.round(180 * s)}px}
    .head{position:relative;text-align:center;margin-top:${portrait ? 110 : 70}px}
    h1{font-weight:800;font-size:${titleSize}px;line-height:1.02;letter-spacing:-0.035em}
    h1 .accent{color:#d8b4fe}
    p{margin-top:${portrait ? 28 : 20}px;font-weight:500;font-size:${subSize}px;color:#c4b5fd;letter-spacing:-0.01em}
    .device{position:relative;margin-top:${portrait ? 90 : 50}px;background:#1c1c22;
      box-shadow:0 0 0 ${Math.max(2, Math.round(3 * s))}px #3a3a44, 0 40px 120px rgba(0,0,0,.55)}
    .screen{overflow:hidden;position:relative}
    .status{display:flex;justify-content:space-between;align-items:center;font-weight:700}
    .icons{display:flex;align-items:center}
    .privacy{position:relative;${portrait ? 'margin-top:120px' : 'margin:auto 0'};display:${portrait ? 'flex' : 'grid'};
      ${portrait ? 'flex-direction:column' : 'grid-template-columns:1fr 1fr'};gap:${portrait ? 36 : 56}px;align-items:stretch}
    .row{display:flex;align-items:center;gap:${Math.round(40 * k)}px;padding:${portrait ? '36px 44px' : '64px 76px'};border-radius:${Math.round(32 * k)}px;background:rgba(24,22,36,.92);border:2px solid rgba(255,255,255,.08)}
    .badge{flex:none;width:${Math.round(96 * k)}px;height:${Math.round(96 * k)}px;border-radius:${Math.round(24 * k)}px;background:#7c3aed;display:flex;align-items:center;justify-content:center}
    .badge svg{width:${Math.round(56 * k)}px;height:${Math.round(56 * k)}px}
    .rt{font-weight:700;font-size:${Math.round(52 * k)}px;letter-spacing:-0.02em}
    .rs{font-weight:500;font-size:${Math.round(34 * k)}px;color:#cbd5e1;margin-top:${Math.round(6 * k)}px}
    .appmark{${portrait ? 'align-self:center;margin-top:40px' : 'display:flex;align-items:center;justify-content:center'}}
    .appmark svg{width:${Math.round(180 * k)}px;height:${Math.round(180 * k)}px;padding:${Math.round(36 * k)}px;border-radius:${Math.round(44 * k)}px;background:#7c3aed;box-shadow:0 20px 80px rgba(124,58,237,.6)}
  </style></head><body>
    <div class="head"><h1>${shot.title}${portrait ? '<br>' : ' '}<span class="accent">${shot.accent}</span></h1><p>${shot.sub}</p></div>
    ${body}
  </body></html>`;
}

async function captureInfo(browser, path) {
  const uri = `data:image/png;base64,${readFileSync(path).toString('base64')}`;
  const probe = await browser.newPage();
  const top = await probe.evaluate(async (src) => {
    const img = new Image();
    img.src = src;
    await img.decode();
    const c = document.createElement('canvas');
    c.width = 1;
    c.height = 1;
    const ctx = c.getContext('2d');
    ctx.drawImage(img, 4, 4, 1, 1, 0, 0, 1, 1);
    return Array.from(ctx.getImageData(0, 0, 1, 1).data.slice(0, 3));
  }, uri);
  await probe.close();
  const [r, g, b] = top;
  return { uri, top: `rgb(${r},${g},${b})`, dark: 0.299 * r + 0.587 * g + 0.114 * b < 128 };
}

const browser = await chromium.launch();
for (const device of Object.keys(devices)) {
  const d = devices[device];
  const target = join(out, device);
  mkdirSync(target, { recursive: true });
  const page_ = await browser.newPage({ viewport: { width: d.width, height: d.height }, deviceScaleFactor: 1 });
  for (const shot of shots) {
    const source = join(captures, device, `${shot.file}.png`);
    if (!shot.privacy && !existsSync(source)) {
      console.warn(`missing capture ${source}`);
      continue;
    }
    const capture = shot.privacy ? null : await captureInfo(browser, source);
    await page_.setContent(page(device, shot, capture), { waitUntil: 'load' });
    await page_.evaluate(() => document.fonts.ready);
    await page_.screenshot({ path: join(target, `${shot.file}.png`) });
    console.log(`${device}/${shot.file}.png`);
  }
  await page_.close();
}
await browser.close();

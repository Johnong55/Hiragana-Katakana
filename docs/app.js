const PRAISE_REACTIONS = [
  { img: 'assets/memes/meme_hamster.jpg', text: 'ĐỈNH NÓC KỊCH TRẦN!' },
  { img: 'assets/memes/meme_duck.jpg', text: '10 ĐIỂM KHÔNG CÓ NHƯNG!' },
  { img: 'assets/memes/meme_shiba.jpg', text: 'SENSEI GẬT ĐẦU HÀI LÒNG!' },
  { img: 'assets/memes/meme_capybara.jpg', text: 'MƯỢT NHƯ SUNSILK LUÔN!' },
  { img: 'assets/memes/meme_red_panda.jpg', text: 'NGẦU ĐÉT LUÔN BRO!' },
  { img: 'assets/memes/meme_cat_blep.jpg', text: 'DỄ ẸC ẤY MÀ HEHE!' },
  { img: 'assets/memes/meme_hamster.jpg', text: 'CHUẨN KHÔNG CẦN CHỈNH!' },
  { img: 'assets/memes/meme_duck.jpg', text: 'UY TÍN LUÔN BẠN ƠI!' }
];

const ERROR_REACTIONS = [
  { img: 'assets/memes/meme_cat_surprised.jpg', text: 'HẢ?! GÌ LẠ VẬY BRO?!' },
  { img: 'assets/memes/meme_cat_grumpy.jpg', text: 'CỐ TỎ RA ỔN NHƯNG SAI RỒI...' },
  { img: 'assets/memes/meme_cat_angry.jpg', text: 'NÓNG MÁU À NHA! NHÌN KỸ LẠI NÈ!' },
  { img: 'assets/memes/meme_cat_reading.jpg', text: 'ĐEO KÍNH VÀO SOI LẠI NÈ!' },
  { img: 'assets/memes/meme_cat_surprised.jpg', text: 'ỦA ALO?! LỘN TIỆM RỒI BẠN ƠI!' },
  { img: 'assets/memes/meme_cat_grumpy.jpg', text: 'KHÓC MỘT DÒNG SÔNG, THỬ LẠI NHA!' }
];

const TIPS_ARTICLES = [
  { num: 1, cat: 'Mẹo nhớ chữ', title: 'Phân biệt シ (shi) và ツ (tsu) trong 3 giây', jp: 'シ vs ツ', ro: 'shi vs tsu', body: 'シ (shi) có 2 nét nhỏ xếp dọc, nét dài hất TỪ DƯỚI LÊN. ツ (tsu) có 2 nét nhỏ xếp ngang, nét dài kéo TỪ TRÊN XUỐNG.' },
  { num: 2, cat: 'Mẹo nhớ chữ', title: 'Phân biệt ソ (so) và ン (n) cực nhanh', jp: 'ソ vs ン', ro: 'so vs n', body: 'ソ (so) có nét dài kéo TỪ TRÊN XUỐNG giống cột dọc; còn ン (n) có nét dài hất TỪ DƯỚI LÊN.' },
  { num: 3, cat: 'Mẹo nhớ chữ', title: 'Xúc âm っ (tsu nhỏ) — nhịp nín thở 1 phách', jp: 'きって vs きて', ro: 'kitte (tem) vs kite (đến)', body: 'Chữ っ nhỏ tạo 1 nhịp ngắt hơi rồi gấp đôi phụ âm đứng sau. Thiếu っ là đổi hẳn nghĩa từ!' },
  { num: 4, cat: 'Kanji hình ảnh', title: 'Chữ 休 (Nghỉ ngơi) = Người (亻) tựa vào Cây (木)', jp: '亻 + 木 = 休', ro: 'yasumu (nghỉ ngơi)', body: 'Bộ nhân đứng 亻 đứng tựa lưng vào chữ 木 (cây) tạo thành chữ 休 (Hưu) nghĩa là nghỉ ngơi dưới bóng cây.' },
  { num: 5, cat: 'Kanji hình ảnh', title: 'Chữ 明 (Sáng) = Mặt trời (日) + Mặt trăng (月)', jp: '日 + 月 = 明', ro: 'akarui (sáng) • ashita (ngày mai)', body: 'Ghép 日 (mặt trời) và 月 (mặt trăng) thành chữ 明 (Minh) nghĩa là sáng rõ, tươi sáng.' },
  { num: 6, cat: 'Kanji hình ảnh', title: 'Từ 木 (Cây) đến 林 (Rừng thưa) và 森 (Rừng rậm)', jp: '木 → 林 → 森', ro: 'ki → hayashi → mori', body: '1 cây là 木, 2 cây đứng cạnh là 林, 3 cây tụ lại thành 森 (rừng rậm)!' },
  { num: 7, cat: 'Ngữ pháp vui', title: 'Trợ từ は viết là HA nhưng đọc là WA', jp: 'わたし は 学生 です', ro: 'watashi wa gakusei desu', body: 'Khi làm trợ từ đánh dấu chủ đề câu, は luôn đọc là wa (không bao giờ viết わ).' },
  { num: 8, cat: 'Ngữ pháp vui', title: 'あります (đồ vật) vs います (người & thú cưng)', jp: 'いぬが います • 本が あります', ro: 'inu ga imasu • hon ga arimasu', body: 'います dùng cho người và động vật; あります dùng cho đồ vật, cây cối.' },
  { num: 9, cat: 'Văn hoá Nhật', title: 'Vì sao nói いただきます trước khi ăn?', jp: 'いただきます', ro: 'itadakimasu', body: 'Bày tỏ lòng biết ơn với thiên nhiên và người đã nấu bữa ăn. Ăn xong nói ごちそうさまでした!' },
  { num: 10, cat: 'Văn hoá Nhật', title: 'すみません — Câu nói quyền năng nhất ở Nhật', jp: 'すみません', ro: 'sumimasen', body: 'Dùng trong 3 tình huống: Xin lỗi nhẹ, Gọi nhân viên/hỏi đường, và Cảm ơn khi ai đó nhường/giúp mình.' }
];

const COUNTERS_DATA = [
  { cat: 'Giờ (〜時)', label: '4 giờ', kanji: '四時', kana: 'よじ', ro: 'yoji', irregular: true, note: 'Không đọc よんじ' },
  { cat: 'Giờ (〜時)', label: '7 giờ', kanji: '七時', kana: 'しちじ', ro: 'shichiji', irregular: true, note: 'Không đọc ななじ' },
  { cat: 'Giờ (〜時)', label: '9 giờ', kanji: '九時', kana: 'くじ', ro: 'kuji', irregular: true, note: 'Không đọc きゅうじ' },
  { cat: 'Phút (〜分)', label: '1 phút', kanji: '一分', kana: 'いっぷん', ro: 'ippun', irregular: true, note: 'Biến âm っぷん' },
  { cat: 'Phút (〜分)', label: '3 phút', kanji: '三分', kana: 'さんぷん', ro: 'sanpun', irregular: true, note: 'Biến âm ぷん' },
  { cat: 'Phút (〜分)', label: '6 phút', kanji: '六分', kana: 'ろっぷん', ro: 'roppun', irregular: true, note: 'Biến âm っぷん' },
  { cat: 'Phút (〜分)', label: '8 phút', kanji: '八分', kana: 'はっぷん', ro: 'happun', irregular: true, note: 'Biến âm っぷん' },
  { cat: 'Người (〜人)', label: '1 người', kanji: '一人', kana: 'ひとり', ro: 'hitori', irregular: true, note: 'Đọc đặc biệt' },
  { cat: 'Người (〜人)', label: '2 người', kanji: '二人', kana: 'ふたり', ro: 'futari', irregular: true, note: 'Đọc đặc biệt' },
  { cat: 'Người (〜人)', label: '4 người', kanji: '四人', kana: 'よにん', ro: 'yonin', irregular: true, note: 'Không đọc よんにん' }
];

const READING_PASSAGES = [
  {
    num: 1, titleVi: 'Giới thiệu bản thân', titleJa: 'じこしょうかい', grammar: 'A は B です ・ の',
    sentences: [
      { jp: 'はじめまして。わたしは ミンです。', vi: 'Rất hân hạnh. Tôi là Minh.' },
      { jp: 'ベトナムの だいがくせいです。', vi: 'Tôi là sinh viên đại học Việt Nam.' },
      { jp: 'いま にほんごを べんきょうして います。', vi: 'Hiện tôi đang học tiếng Nhật.' }
    ],
    q: 'Minh hiện đang làm gì?',
    opts: ['Nhân viên công ty ở Nhật', 'Sinh viên đại học ở Việt Nam', 'Giáo viên tiếng Nhật'],
    ans: 1
  },
  {
    num: 2, titleVi: 'Một ngày của tôi', titleJa: 'わたしの一日', grammar: 'Thời gian に ・ 動詞ます',
    sentences: [
      { jp: 'わたしは まいにち 六じに おきます。', vi: 'Mỗi ngày tôi dậy lúc 6 giờ.' },
      { jp: 'あさごはんは パンと たまごを たべます。', vi: 'Bữa sáng tôi ăn bánh mì và trứng.' },
      { jp: 'よるは 十じはんに ねます。', vi: 'Buổi tối tôi ngủ lúc 10 giờ rưỡi.' }
    ],
    q: 'Nhân vật tôi ăn sáng món gì?',
    opts: ['Cơm và cá', 'Bánh mì và trứng', 'Mì ramen'],
    ans: 1
  }
];

const state = {
  characters: [],
  words: [],
  learned: new Set(JSON.parse(localStorage.getItem('kana_learned') || '[]')),
  streak: parseInt(localStorage.getItem('kana_streak') || '1', 10),
  theme: localStorage.getItem('kana_theme') || 'light',
  activeTab: 'roadmap',
  alphabetScript: 'hiragana',
  selectedChar: null,
  showGhost: true,
  userStrokes: [],
  quizModeMixed: true,
  quizQuestion: null
};

if (state.theme === 'dark') {
  document.documentElement.setAttribute('data-theme', 'dark');
}

function speakJa(text) {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = 'ja-JP';
  u.rate = 0.92;
  window.speechSynthesis.speak(u);
}

function saveProgress(charStr) {
  if (charStr) {
    state.learned.add(charStr);
    localStorage.setItem('kana_learned', JSON.stringify([...state.learned]));
  }
  const today = Math.floor(Date.now() / 86400000);
  const last = parseInt(localStorage.getItem('kana_last_day') || '0', 10);
  if (last !== today) {
    state.streak = last === today - 1 ? state.streak + 1 : Math.max(1, state.streak);
    localStorage.setItem('kana_streak', String(state.streak));
    localStorage.setItem('kana_last_day', String(today));
  }
  updateStreakBar();
}

function updateStreakBar() {
  const total = state.characters.length || 241;
  document.getElementById('streak-summary-text').textContent =
    `${state.streak} ngày • ${state.learned.size}/${total} chữ đã thuộc`;
}

let popupTimer = null;
function showMemePopup({ isError, message, onContinue }) {
  if (popupTimer) clearTimeout(popupTimer);
  const list = isError ? ERROR_REACTIONS : PRAISE_REACTIONS;
  const pick = list[Math.floor(Math.random() * list.length)];
  const overlay = document.getElementById('meme-popup-overlay');
  const box = document.getElementById('meme-popup-box');
  const img = document.getElementById('meme-popup-img');
  const headline = document.getElementById('meme-popup-headline');
  const msg = document.getElementById('meme-popup-msg');
  const btn = document.getElementById('meme-popup-btn');

  box.className = 'meme-popup-box' + (isError ? ' error' : '');
  img.src = pick.img;
  headline.textContent = pick.text;
  msg.textContent = message || '';
  overlay.classList.add('show');

  const closeAndRun = () => {
    if (popupTimer) clearTimeout(popupTimer);
    overlay.classList.remove('show');
    if (onContinue) onContinue();
  };

  if (isError) {
    btn.style.display = 'inline-flex';
    btn.textContent = onContinue ? 'Tiếp theo →' : 'Thử lại';
    btn.onclick = (e) => { e.stopPropagation(); closeAndRun(); };
    overlay.onclick = closeAndRun;
  } else {
    btn.style.display = 'none';
    overlay.onclick = closeAndRun;
    popupTimer = setTimeout(closeAndRun, 1300);
  }
}

function setupDrawer() {
  const backdrop = document.getElementById('drawer-backdrop');
  const sheet = document.getElementById('drawer-sheet');
  const open = () => { backdrop.classList.add('open'); sheet.classList.add('open'); };
  const close = () => { backdrop.classList.remove('open'); sheet.classList.remove('open'); };

  document.getElementById('btn-open-drawer').onclick = open;
  document.getElementById('btn-close-drawer').onclick = close;
  backdrop.onclick = close;

  const items = [
    { img: 'assets/memes/meme_cat_reading.jpg', title: 'Đọc hiểu JLPT N5', sub: 'Đoạn văn song ngữ & câu hỏi', view: 'reading' },
    { img: 'assets/memes/meme_duck.jpg', title: 'Số đếm, Giờ & Trợ số từ', sub: 'Biến âm 〜時, 〜分, 〜人', view: 'counters' },
    { img: 'assets/memes/meme_cat_blep.jpg', title: 'Ôn tập Đa dạng (Viết + Chọn)', sub: 'Xen kẽ viết nét & trắc nghiệm', tab: 'quiz' },
    { img: 'assets/memes/meme_hamster.jpg', title: 'Bảng chữ & Luyện viết nét', sub: 'Hiragana, Katakana, 80 Kanji N5', tab: 'alphabet' },
    { img: 'assets/memes/meme_shiba.jpg', title: 'Tạp chí Tips & Từ điển', sub: 'Mẹo nhớ chữ & 302 từ vựng N5', tab: 'tips' }
  ];

  const listEl = document.getElementById('drawer-menu-list');
  listEl.innerHTML = '';
  items.forEach((it) => {
    const div = document.createElement('div');
    div.className = 'drawer-item';
    div.innerHTML = `
      <img src="${it.img}" class="meme-avatar-lg" alt="" />
      <div>
        <div style="font-weight:700;font-size:14px">${it.title}</div>
        <div style="font-size:12px;color:var(--ink-muted)">${it.sub}</div>
      </div>
    `;
    div.onclick = () => {
      close();
      if (it.tab) switchTab(it.tab);
      else if (it.view === 'reading') renderReadingView();
      else if (it.view === 'counters') renderCountersView();
    };
    listEl.appendChild(div);
  });
}

function switchTab(tabName) {
  state.activeTab = tabName;
  document.querySelectorAll('.nav-tab').forEach((el) => {
    el.classList.toggle('active', el.dataset.tab === tabName);
  });
  if (tabName === 'roadmap') renderRoadmap();
  else if (tabName === 'alphabet') renderAlphabet();
  else if (tabName === 'quiz') renderQuiz();
  else if (tabName === 'tips') renderTipsAndDict();
}

function renderRoadmap() {
  document.getElementById('screen-title').textContent = 'LỘ TRÌNH JLPT N5';
  const el = document.getElementById('app-content');
  const stages = [
    { num: '01', badge: 'あ', title: 'Hiragana Cơ Bản (46 chữ)', desc: 'Nền tảng bảng chữ mềm tiếng Nhật', script: 'hiragana', cat: 'basic' },
    { num: '02', badge: 'ア', title: 'Katakana Cơ Bản (46 chữ)', desc: 'Bảng chữ cứng cho từ mượn nước ngoài', script: 'katakana', cat: 'basic' },
    { num: '03', badge: 'が', title: 'Âm Đục & Âm Ghép', desc: 'Dakuten, Handakuten & Yoon', script: 'hiragana', cat: 'extended' },
    { num: '04', badge: '語', title: '302 Từ Vựng & Mẫu Câu N5', desc: 'Từ vựng giao tiếp & đọc câu ví dụ', action: 'tips' },
    { num: '05', badge: '文', title: 'Đọc Hiểu & Ngữ Pháp N5', desc: 'Luyện đọc đoạn văn ngắn N5 có dịch', action: 'reading' },
    { num: '06', badge: '時', title: 'Số Đếm, Giờ & Trợ Số Từ', desc: 'Biến âm giờ, phút, ngày, người', action: 'counters' },
    { num: '07', badge: '漢', title: '80 Chữ Kanji JLPT N5', desc: 'Tập viết chuẩn thứ tự nét 80 Kanji N5', script: 'kanji', cat: 'kanji' },
    { num: '08', badge: 'N5', title: 'Tổng Ôn Tập Đa Dạng N5', desc: 'Kết hợp viết nét & phản xạ trắc nghiệm', action: 'quiz' }
  ];

  el.innerHTML = stages.map((s, idx) => `
    <div class="card stage-card" data-idx="${idx}">
      <div class="stage-node">${s.badge}</div>
      <div style="flex:1">
        <div style="font-weight:800;font-size:14px">CHẶNG ${s.num} • ${s.title}</div>
        <div style="font-size:12px;color:var(--ink-muted);margin-top:2px">${s.desc}</div>
      </div>
      <span style="font-weight:800">→</span>
    </div>
  `).join('');

  el.querySelectorAll('.stage-card').forEach((card) => {
    card.onclick = () => {
      const s = stages[parseInt(card.dataset.idx, 10)];
      if (s.script) {
        state.alphabetScript = s.script;
        switchTab('alphabet');
      } else if (s.action === 'reading') renderReadingView();
      else if (s.action === 'counters') renderCountersView();
      else if (s.action === 'tips') switchTab('tips');
      else if (s.action === 'quiz') switchTab('quiz');
    };
  });
}

function renderAlphabet() {
  document.getElementById('screen-title').textContent = 'BẢNG CHỮ & LUYỆN VIẾT';
  const el = document.getElementById('app-content');
  const filtered = state.characters.filter((c) => {
    if (state.alphabetScript === 'hiragana') return c.script === 'hiragana' && c.category === 'basic';
    if (state.alphabetScript === 'katakana') return c.script === 'katakana' && c.category === 'basic';
    if (state.alphabetScript === 'kanji') return c.script === 'kanji';
    return c.category !== 'basic' && c.script !== 'kanji';
  });

  if (!state.selectedChar || !filtered.some((c) => c.char === state.selectedChar.char)) {
    state.selectedChar = filtered[0] || state.characters[0];
  }
  const cur = state.selectedChar;

  el.innerHTML = `
    <div style="display:flex;gap:6px;margin-bottom:10px;overflow-x:auto">
      <button class="chip ${state.alphabetScript === 'hiragana' ? 'btn-primary' : ''}" data-sc="hiragana">Hiragana</button>
      <button class="chip ${state.alphabetScript === 'katakana' ? 'btn-primary' : ''}" data-sc="katakana">Katakana</button>
      <button class="chip ${state.alphabetScript === 'extended' ? 'btn-primary' : ''}" data-sc="extended">Âm đục/ghép</button>
      <button class="chip ${state.alphabetScript === 'kanji' ? 'btn-primary' : ''}" data-sc="kanji">80 Kanji N5</button>
    </div>

    ${cur ? `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div>
          <span style="font-size:28px;font-weight:800">${cur.char}</span>
          <span style="font-size:13px;font-weight:700;margin-left:8px;color:var(--ink-muted)">${cur.row} • ${cur.strokeCount} nét</span>
        </div>
        <div style="display:flex;gap:6px">
          <button class="btn" id="btn-Ghost">${state.showGhost ? 'Ẩn nét mờ' : 'Hiện nét mờ'}</button>
          <button class="btn btn-primary" id="btn-speak-cur">Nghe</button>
        </div>
      </div>
      <div style="font-size:15px;font-weight:700;margin-top:6px;line-height:1.4">Âm & Nghĩa: ${cur.romaji}</div>
      <div class="canvas-wrap">
        <canvas id="draw-canvas" width="310" height="310"></canvas>
      </div>
      <div style="display:flex;gap:8px;margin-top:8px">
        <button class="btn" style="flex:1" id="btn-undo">Hoàn tác</button>
        <button class="btn" style="flex:1" id="btn-clear">Xoá</button>
        <button class="btn btn-primary" style="flex:1.4" id="btn-check-write">Kiểm tra</button>
      </div>
      ${state.words.filter((w) => w.introduces === cur.char || (w.chars && w.chars.includes(cur.char))).slice(0, 3).map((w) => `
        <div style="margin-top:6px;padding:6px 8px;border:1px solid var(--outline);border-radius:4px;font-size:13px;line-height:1.4">
          <strong>${w.word} (${w.romaji})</strong>: ${w.meaning_vi}
        </div>
      `).join('')}
    </div>
    ` : ''}

    <div class="grid-5">
      ${filtered.map((c) => `
        <div class="kana-cell ${state.learned.has(c.char) ? 'learned' : ''}" data-ch="${c.char}">
          <span class="ch">${c.char}</span>
          <span class="ro">${c.romaji}</span>
        </div>
      `).join('')}
    </div>
  `;

  el.querySelectorAll('[data-sc]').forEach((b) => {
    b.onclick = () => { state.alphabetScript = b.dataset.sc; renderAlphabet(); };
  });
  el.querySelectorAll('.kana-cell').forEach((cell) => {
    cell.onclick = () => {
      const found = filtered.find((x) => x.char === cell.dataset.ch);
      if (found) {
        state.selectedChar = found;
        state.userStrokes = [];
        speakJa(found.char);
        renderAlphabet();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    };
  });

  if (cur) {
    document.getElementById('btn-speak-cur').onclick = () => speakJa(cur.char);
    document.getElementById('btn-Ghost').onclick = () => {
      state.showGhost = !state.showGhost;
      drawCanvasTemplate(cur);
      document.getElementById('btn-Ghost').textContent = state.showGhost ? 'Ẩn nét mờ' : 'Hiện nét mờ';
    };
    setupDrawingCanvas(cur, () => {
      saveProgress(cur.char);
      const idx = filtered.findIndex((x) => x.char === cur.char);
      if (idx >= 0 && idx + 1 < filtered.length) {
        state.selectedChar = filtered[idx + 1];
      }
      renderAlphabet();
    });
  }
}

function setupDrawingCanvas(template, onPassCallback) {
  const canvas = document.getElementById('draw-canvas');
  if (!canvas) return;
  state.userStrokes = [];
  let currentPoints = [];
  let isDrawing = false;

  const getPos = (e) => {
    const rect = canvas.getBoundingClientRect();
    const clientX = e.touches ? e.touches[0].clientX : e.clientX;
    const clientY = e.touches ? e.touches[0].clientY : e.clientY;
    return [
      Math.max(0, Math.min(1, (clientX - rect.left) / rect.width)),
      Math.max(0, Math.min(1, (clientY - rect.top) / rect.height))
    ];
  };

  const start = (e) => {
    e.preventDefault();
    isDrawing = true;
    currentPoints = [getPos(e)];
    drawCanvasTemplate(template, currentPoints);
  };
  const move = (e) => {
    if (!isDrawing) return;
    e.preventDefault();
    currentPoints.push(getPos(e));
    drawCanvasTemplate(template, currentPoints);
  };
  const end = (e) => {
    if (!isDrawing) return;
    e.preventDefault();
    isDrawing = false;
    if (currentPoints.length > 2) {
      state.userStrokes.push(currentPoints);
    }
    currentPoints = [];
    drawCanvasTemplate(template);
  };

  canvas.addEventListener('mousedown', start);
  canvas.addEventListener('mousemove', move);
  window.addEventListener('mouseup', end);
  canvas.addEventListener('touchstart', start, { passive: false });
  canvas.addEventListener('touchmove', move, { passive: false });
  canvas.addEventListener('touchend', end, { passive: false });

  document.getElementById('btn-undo').onclick = () => {
    state.userStrokes.pop();
    drawCanvasTemplate(template);
  };
  document.getElementById('btn-clear').onclick = () => {
    state.userStrokes = [];
    drawCanvasTemplate(template);
  };
  document.getElementById('btn-check-write').onclick = () => {
    const res = evaluateStrokes(state.userStrokes, template);
    speakJa(template.char);
    if (res.passed) {
      showMemePopup({
        isError: false,
        message: `${template.char} (${template.romaji}) • ${res.score} điểm`,
        onContinue: onPassCallback
      });
    } else {
      showMemePopup({
        isError: true,
        message: res.reason
      });
    }
  };

  drawCanvasTemplate(template);
}

function drawCanvasTemplate(template, activeStroke) {
  const canvas = document.getElementById('draw-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const w = canvas.width;
  const h = canvas.height;
  ctx.clearRect(0, 0, w, h);

  // Đường kẻ ô chữ thập
  ctx.save();
  ctx.strokeStyle = '#CBB99B';
  ctx.setLineDash([6, 6]);
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(w / 2, 0); ctx.lineTo(w / 2, h);
  ctx.moveTo(0, h / 2); ctx.lineTo(w, h / 2);
  ctx.stroke();
  ctx.restore();

  // Nét mẫu mờ + số thứ tự nét
  if (state.showGhost && template && template.strokes) {
    template.strokes.forEach((st, idx) => {
      if (!st.length) return;
      ctx.save();
      ctx.strokeStyle = 'rgba(107, 83, 64, 0.28)';
      ctx.lineWidth = 14;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.beginPath();
      ctx.moveTo(st[0][0] * w, st[0][1] * h);
      st.forEach((pt) => ctx.lineTo(pt[0] * w, pt[1] * h));
      ctx.stroke();

      ctx.fillStyle = '#5C3A21';
      ctx.font = 'bold 12px sans-serif';
      ctx.fillText(String(idx + 1), st[0][0] * w - 4, st[0][1] * h - 6);
      ctx.restore();
    });
  }

  // Nét người dùng vẽ
  const allUser = activeStroke ? [...state.userStrokes, activeStroke] : state.userStrokes;
  allUser.forEach((st) => {
    if (!st || st.length < 2) return;
    ctx.save();
    ctx.strokeStyle = '#2C1E14';
    ctx.lineWidth = 12;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();
    ctx.moveTo(st[0][0] * w, st[0][1] * h);
    st.forEach((pt) => ctx.lineTo(pt[0] * w, pt[1] * h));
    ctx.stroke();
    ctx.restore();
  });
}

function evaluateStrokes(userStrokes, template) {
  const expected = template.strokeCount || template.strokes.length;
  if (userStrokes.length !== expected) {
    return {
      passed: false,
      score: 40,
      reason: `Chữ ${template.char} cần đúng ${expected} nét (bạn vừa viết ${userStrokes.length} nét).`
    };
  }
  let totalDist = 0;
  for (let i = 0; i < expected; i++) {
    const u = userStrokes[i];
    const t = template.strokes[i];
    const uStart = u[0], uEnd = u[u.length - 1];
    const tStart = t[0], tEnd = t[t.length - 1];
    const dStart = Math.hypot(uStart[0] - tStart[0], uStart[1] - tStart[1]);
    const dEnd = Math.hypot(uEnd[0] - tEnd[0], uEnd[1] - tEnd[1]);
    totalDist += (dStart + dEnd) / 2;
  }
  const avgDist = totalDist / expected;
  const score = Math.max(45, Math.min(100, Math.round(100 - avgDist * 135)));
  if (avgDist > 0.36) {
    return {
      passed: false,
      score,
      reason: `Vị trí hoặc hướng nét chữ ${template.char} chưa khớp mẫu. Hãy bật nét mờ để nhìn lại nhé!`
    };
  }
  return { passed: true, score };
}

function renderQuiz() {
  document.getElementById('screen-title').textContent = 'ÔN TẬP ĐA DẠNG';
  const el = document.getElementById('app-content');
  if (!state.characters.length) return;

  const target = state.characters[Math.floor(Math.random() * state.characters.length)];
  const styles = state.quizModeMixed ? ['WRITE', 'PICK_CHAR', 'PICK_ROMAJI'] : ['WRITE'];
  const style = styles[Math.floor(Math.random() * styles.length)];

  const distractors = state.characters
    .filter((c) => c.char !== target.char && c.romaji !== target.romaji)
    .sort(() => Math.random() - 0.5)
    .slice(0, 3);
  const options = [...distractors, target].sort(() => Math.random() - 0.5);

  el.innerHTML = `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
      <div style="display:flex;align-items:center;gap:8px">
        <img src="assets/memes/${state.quizModeMixed ? 'meme_cat_blep.jpg' : 'meme_shiba.jpg'}" class="meme-avatar" alt="" />
        <span style="font-weight:700;font-size:13px">${state.quizModeMixed ? 'Chế độ: Đa dạng' : 'Chế độ: Chỉ viết'}</span>
      </div>
      <button class="btn" id="btn-toggle-quiz-mode">Đổi chế độ</button>
    </div>

    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div>
          <div style="font-size:12px;color:var(--ink-muted)">${target.script.toUpperCase()} • ${target.row}</div>
          <div style="font-size:18px;font-weight:800;margin-top:4px">
            ${style === 'WRITE' ? `Viết chữ có âm: "${target.romaji}"`
              : style === 'PICK_CHAR' ? `Chọn đúng chữ cho âm: "${target.romaji}"`
              : `Chữ 「${target.char}」 đọc là gì?`}
          </div>
        </div>
        <button class="btn btn-primary" id="btn-quiz-speak">Nghe</button>
      </div>
    </div>

    ${style === 'WRITE' ? `
      <div class="card">
        <div class="canvas-wrap">
          <canvas id="draw-canvas" width="310" height="310"></canvas>
        </div>
        <div style="display:flex;gap:8px;margin-top:8px">
          <button class="btn" style="flex:1" id="btn-undo">Hoàn tác</button>
          <button class="btn" style="flex:1" id="btn-clear">Xoá</button>
          <button class="btn" style="flex:1" id="btn-skip">Bỏ qua</button>
          <button class="btn btn-primary" style="flex:1.3" id="btn-check-write">Kiểm tra</button>
        </div>
      </div>
    ` : `
      <div id="quiz-choices">
        ${options.map((opt, idx) => `
          <button class="choice-btn" data-idx="${idx}">
            ${String.fromCharCode(65 + idx)}. ${style === 'PICK_CHAR' ? opt.char : opt.romaji}
          </button>
        `).join('')}
      </div>
    `}
  `;

  document.getElementById('btn-toggle-quiz-mode').onclick = () => {
    state.quizModeMixed = !state.quizModeMixed;
    renderQuiz();
  };
  document.getElementById('btn-quiz-speak').onclick = () => speakJa(target.char);

  if (style === 'WRITE') {
    state.showGhost = false;
    setupDrawingCanvas(target, () => {
      saveProgress(target.char);
      renderQuiz();
    });
    document.getElementById('btn-skip').onclick = () => renderQuiz();
  } else {
    el.querySelectorAll('.choice-btn').forEach((btn) => {
      btn.onclick = () => {
        const picked = options[parseInt(btn.dataset.idx, 10)];
        const isCorrect = picked.char === target.char;
        speakJa(target.char);
        if (isCorrect) saveProgress(target.char);
        showMemePopup({
          isError: !isCorrect,
          message: `Đáp án: ${target.char} (${target.romaji})`,
          onContinue: () => renderQuiz()
        });
      };
    });
  }
}

function renderReadingView() {
  document.getElementById('screen-title').textContent = 'ĐỌC HIỂU JLPT N5';
  const el = document.getElementById('app-content');
  el.innerHTML = READING_PASSAGES.map((p, pIdx) => `
    <div class="card">
      <div style="font-weight:800;font-size:15px">BÀI 0${p.num} • ${p.titleVi} (${p.titleJa})</div>
      <div style="font-size:12px;color:var(--ink-muted);margin:4px 0 8px">Ngữ pháp: ${p.grammar}</div>
      ${p.sentences.map((s) => `
        <div style="padding:6px 0;border-top:1px solid var(--outline);cursor:pointer" onclick="speakJa('${s.jp}')">
          <div style="font-weight:700;font-size:15px">${s.jp}</div>
          <div style="font-size:12px;color:var(--ink-muted)">→ ${s.vi}</div>
        </div>
      `).join('')}
      <div style="margin-top:10px;font-weight:700;font-size:14px">Câu hỏi: ${p.q}</div>
      <div style="margin-top:6px">
        ${p.opts.map((o, oIdx) => `
          <button class="choice-btn" data-p="${pIdx}" data-o="${oIdx}">${String.fromCharCode(65 + oIdx)}. ${o}</button>
        `).join('')}
      </div>
    </div>
  `).join('');

  el.querySelectorAll('.choice-btn').forEach((btn) => {
    btn.onclick = () => {
      const p = READING_PASSAGES[parseInt(btn.dataset.p, 10)];
      const o = parseInt(btn.dataset.o, 10);
      const ok = o === p.ans;
      if (ok) saveProgress();
      showMemePopup({
        isError: !ok,
        message: `Đáp án đúng: ${p.opts[p.ans]}`
      });
    };
  });
}

function renderCountersView() {
  document.getElementById('screen-title').textContent = 'SỐ ĐẾM, GIỜ & TRỢ SỐ TỪ';
  const el = document.getElementById('app-content');
  el.innerHTML = `
    <div class="card">
      <div style="font-weight:800;margin-bottom:6px">CÁC BIẾN ÂM HAY THI JLPT N5 (Chạm để nghe)</div>
      ${COUNTERS_DATA.map((c) => `
        <div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-top:1px solid var(--outline);cursor:pointer" onclick="speakJa('${c.kana}')">
          <div>
            <div style="font-weight:700">${c.label} (${c.kanji}) — <span style="color:var(--error)">${c.kana}</span></div>
            <div style="font-size:12px;color:var(--ink-muted)">${c.cat} • ${c.ro} • ${c.note}</div>
          </div>
          <span class="btn">Nghe</span>
        </div>
      `).join('')}
    </div>
  `;
}

function renderTipsAndDict() {
  document.getElementById('screen-title').textContent = 'TẠP CHÍ TIPS & TỪ ĐIỂN';
  const el = document.getElementById('app-content');
  el.innerHTML = `
    <div class="card">
      <input id="dict-search" type="text" placeholder="Tìm chữ, Kanji, Romaji hoặc nghĩa Việt..."
        style="width:100%;padding:10px;border:1px solid var(--outline);background:var(--bg);color:var(--ink);font-size:14px;border-radius:4px" />
      <div id="dict-results" style="margin-top:8px"></div>
    </div>

    <div style="font-weight:800;margin:12px 0 8px">TẠP CHÍ TIPS MỖI NGÀY</div>
    ${TIPS_ARTICLES.map((t, i) => `
      <div class="card">
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:6px">
          <img src="${PRAISE_REACTIONS[i % PRAISE_REACTIONS.length].img}" class="meme-avatar-lg" alt="" />
          <div>
            <div style="font-size:11px;font-weight:700;color:var(--ink-muted)">SỐ #0${t.num} • ${t.cat.toUpperCase()}</div>
            <div style="font-weight:800;font-size:15px">${t.title}</div>
          </div>
        </div>
        <div style="padding:8px;background:var(--surface-variant);border-radius:4px;font-weight:800;margin-bottom:6px">
          ${t.jp} <span style="font-weight:400;font-size:13px">(${t.ro})</span>
        </div>
        <div style="font-size:13px;line-height:1.5">${t.body}</div>
      </div>
    `).join('')}
  `;

  const input = document.getElementById('dict-search');
  const resEl = document.getElementById('dict-results');
  input.oninput = () => {
    const q = input.value.trim().toLowerCase();
    if (!q) { resEl.innerHTML = ''; return; }
    const matchedWords = state.words.filter((w) =>
      w.word.toLowerCase().includes(q) ||
      w.romaji.toLowerCase().includes(q) ||
      w.meaning_vi.toLowerCase().includes(q)
    ).slice(0, 12);

    resEl.innerHTML = matchedWords.map((w) => `
      <div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-top:1px solid var(--outline);cursor:pointer" onclick="speakJa('${w.word}')">
        <div>
          <div style="font-weight:700">${w.word} (${w.romaji})</div>
          <div style="font-size:12px;color:var(--ink-muted)">${w.meaning_vi}</div>
        </div>
        <span class="btn">Nghe</span>
      </div>
    `).join('') || '<div style="font-size:13px;color:var(--ink-muted)">Không tìm thấy mục phù hợp.</div>';
  };
}

async function initApp() {
  setupDrawer();

  document.getElementById('btn-theme-toggle').onclick = () => {
    state.theme = state.theme === 'dark' ? 'light' : 'dark';
    if (state.theme === 'dark') document.documentElement.setAttribute('data-theme', 'dark');
    else document.documentElement.removeAttribute('data-theme');
    localStorage.setItem('kana_theme', state.theme);
  };

  document.querySelectorAll('.nav-tab').forEach((tab) => {
    tab.onclick = () => switchTab(tab.dataset.tab);
  });

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(() => {});
  }

  try {
    const [strokesResp, vocabResp] = await Promise.all([
      fetch('./assets/kana_strokes.json'),
      fetch('./assets/kana_vocab.json')
    ]);
    const strokesData = await strokesResp.json();
    const vocabData = await vocabResp.json();
    state.characters = strokesData.characters || [];
    state.words = vocabData.words || [];
  } catch (e) {
    console.error(e);
  }

  updateStreakBar();
  renderRoadmap();
}

initApp();

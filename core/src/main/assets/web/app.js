// Состояние приложения
const state = {
  items: [],
  filteredItems: [],
  activeSection: 'ALL',
  searchQuery: '',
  currentVideo: null
};

// DOM элементы
const dom = {
  deviceName: document.getElementById('deviceName'),
  statsPill: document.getElementById('statsPill'),
  searchInput: document.getElementById('searchInput'),
  clearSearchBtn: document.getElementById('clearSearchBtn'),
  refreshBtn: document.getElementById('refreshBtn'),
  loader: document.getElementById('loader'),
  emptyState: document.getElementById('emptyState'),
  emptyMessage: document.getElementById('emptyMessage'),
  mediaGrid: document.getElementById('mediaGrid'),
  tabButtons: document.querySelectorAll('.tab-btn'),
  badgeAll: document.getElementById('badgeAll'),
  badgeX: document.getElementById('badgeX'),
  badgeR: document.getElementById('badgeR'),
  badgeL: document.getElementById('badgeL'),
  // Modal
  playerModal: document.getElementById('playerModal'),
  modalBackdrop: document.getElementById('modalBackdrop'),
  videoPlayer: document.getElementById('videoPlayer'),
  playerTitle: document.getElementById('playerTitle'),
  playerTag: document.getElementById('playerTag'),
  playerDownloadBtn: document.getElementById('playerDownloadBtn'),
  closePlayerBtn: document.getElementById('closePlayerBtn'),
  speedSelect: document.getElementById('speedSelect')
};

// Инициализация
document.addEventListener('DOMContentLoaded', () => {
  initEvents();
  loadData();
});

function initEvents() {
  // Переключение вкладок
  dom.tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      dom.tabButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      state.activeSection = btn.dataset.section;
      applyFilter();
    });
  });

  // Поиск
  dom.searchInput.addEventListener('input', (e) => {
    state.searchQuery = e.target.value.trim().toLowerCase();
    dom.clearSearchBtn.style.display = state.searchQuery ? 'block' : 'none';
    applyFilter();
  });

  dom.clearSearchBtn.addEventListener('click', () => {
    dom.searchInput.value = '';
    state.searchQuery = '';
    dom.clearSearchBtn.style.display = 'none';
    applyFilter();
    dom.searchInput.focus();
  });

  // Кнопка обновления
  dom.refreshBtn.addEventListener('click', () => {
    dom.refreshBtn.style.transform = 'rotate(360deg)';
    setTimeout(() => { dom.refreshBtn.style.transform = 'none'; }, 400);
    loadData();
  });

  // Закрытие модального окна
  dom.closePlayerBtn.addEventListener('click', closePlayer);
  dom.modalBackdrop.addEventListener('click', closePlayer);

  // Изменение скорости
  dom.speedSelect.addEventListener('change', (e) => {
    dom.videoPlayer.playbackRate = parseFloat(e.target.value);
  });

  // Горячие клавиши
  window.addEventListener('keydown', handleHotkeys);
}

// Загрузка данных с Android Ktor сервера
async function loadData() {
  showLoader(true);
  try {
    // 1. Статус сервера
    fetchStatus();

    // 2. Список библиотеки
    const res = await fetch('/api/library');
    if (!res.ok) throw new Error(`HTTP error ${res.status}`);
    const data = await res.json();

    state.items = data.items || [];
    updateBadges();
    applyFilter();
  } catch (err) {
    console.error('Ошибка загрузки медиатеки:', err);
    dom.emptyState.style.display = 'block';
    dom.emptyMessage.textContent = 'Не удалось подключиться к серверу. Проверьте соединение по Wi-Fi.';
    dom.mediaGrid.innerHTML = '';
  } finally {
    showLoader(false);
  }
}

async function fetchStatus() {
  try {
    const res = await fetch('/api/status');
    if (!res.ok) return;
    const status = await res.json();
    if (status.deviceName) {
      dom.deviceName.textContent = status.deviceName;
    }
  } catch (e) {
    console.warn('Не удалось обновить статус:', e);
  }
}

function updateBadges() {
  const counts = { ALL: state.items.length, X: 0, R: 0, L: 0 };
  state.items.forEach(item => {
    if (counts[item.section] !== undefined) {
      counts[item.section]++;
    }
  });

  dom.badgeAll.textContent = counts.ALL;
  dom.badgeX.textContent = counts.X;
  dom.badgeR.textContent = counts.R;
  dom.badgeL.textContent = counts.L;

  const totalSizeStr = formatBytes(state.items.reduce((acc, it) => acc + (it.sizeBytes || 0), 0));
  dom.statsPill.textContent = `${counts.ALL} видео (${totalSizeStr})`;
}

// Фильтрация и поиск
function applyFilter() {
  const q = state.searchQuery;
  const sec = state.activeSection;

  state.filteredItems = state.items.filter(item => {
    // Фильтр по разделу
    if (sec !== 'ALL' && item.section !== sec) return false;

    // Поисковый запрос
    if (q) {
      const inTitle = item.title && item.title.toLowerCase().includes(q);
      const inSub = item.subtitle && item.subtitle.toLowerCase().includes(q);
      const inTags = item.tags && item.tags.some(t => t.toLowerCase().includes(q));
      if (!inTitle && !inSub && !inTags) return false;
    }
    return true;
  });

  renderGrid();
}

// Отрисовка карточек
function renderGrid() {
  const items = state.filteredItems;
  dom.mediaGrid.innerHTML = '';

  if (items.length === 0) {
    dom.emptyState.style.display = 'block';
    dom.emptyMessage.textContent = state.searchQuery 
      ? 'По вашему запросу ничего не найдено' 
      : 'В этом разделе пока нет сохранённых файлов';
    return;
  }

  dom.emptyState.style.display = 'none';

  const fragment = document.createDocumentFragment();

  items.forEach(item => {
    const card = document.createElement('div');
    card.className = 'media-card';

    const posterHtml = item.hasPoster && item.posterUrl
      ? `<img class="poster-img" src="${escapeHtml(item.posterUrl)}" alt="${escapeHtml(item.title)}" loading="lazy" onerror="this.parentElement.innerHTML='<div class=\\'poster-placeholder\\'>▶</div>'">`
      : `<div class="poster-placeholder">▶</div>`;

    const durationBadge = item.duration 
      ? `<span class="badge-duration">${escapeHtml(item.duration)}</span>` 
      : '';

    const sizeStr = formatBytes(item.sizeBytes);
    const dateStr = item.dateModified ? formatDate(item.dateModified) : '';

    card.innerHTML = `
      <div class="poster-wrapper">
        ${posterHtml}
        <span class="badge-tag tag-${item.section}">${item.section}</span>
        ${durationBadge}
        <div class="card-play-overlay">
          <div class="play-circle">▶</div>
        </div>
      </div>
      <div class="card-content">
        <h3 class="card-title" title="${escapeHtml(item.title)}">${escapeHtml(item.title)}</h3>
        <div class="card-meta">
          <span class="card-author">${escapeHtml(item.subtitle || '')}</span>
          <span class="card-size">${sizeStr}</span>
        </div>
        <div class="card-actions">
          <button class="btn-play-card" data-action="play">Смотреть</button>
          <a class="btn-download-card" href="${escapeHtml(item.downloadUrl || item.videoUrl)}" download title="Скачать на ПК">⬇</a>
        </div>
      </div>
    `;

    // Обработчик клика на постер или кнопку «Смотреть»
    const triggerPlay = () => openPlayer(item);
    card.querySelector('.poster-wrapper').addEventListener('click', triggerPlay);
    card.querySelector('.card-title').addEventListener('click', triggerPlay);
    card.querySelector('[data-action="play"]').addEventListener('click', triggerPlay);

    fragment.appendChild(card);
  });

  dom.mediaGrid.appendChild(fragment);
}

// Видеоплеер
function openPlayer(item) {
  state.currentVideo = item;
  dom.playerTitle.textContent = item.title;
  dom.playerTag.textContent = item.section;
  dom.playerTag.className = `section-tag tag-${item.section}`;

  dom.playerDownloadBtn.href = item.downloadUrl || item.videoUrl;
  dom.playerDownloadBtn.download = (item.title || 'video').replace(/[/\\?%*:|"<>]/g, '_') + '.mp4';

  dom.videoPlayer.src = item.videoUrl;
  dom.videoPlayer.playbackRate = parseFloat(dom.speedSelect.value);

  dom.playerModal.style.display = 'flex';
  document.body.style.overflow = 'hidden';

  dom.videoPlayer.play().catch(e => console.log('Autoplay prevented:', e));
}

function closePlayer() {
  dom.videoPlayer.pause();
  dom.videoPlayer.removeAttribute('src');
  dom.videoPlayer.load();

  dom.playerModal.style.display = 'none';
  document.body.style.overflow = 'auto';
  state.currentVideo = null;
}

// Горячие клавиши плеера
function handleHotkeys(e) {
  if (dom.playerModal.style.display !== 'flex') return;

  const target = e.target;
  if (target.tagName === 'INPUT' || target.tagName === 'SELECT') return;

  const player = dom.videoPlayer;

  switch (e.code) {
    case 'Escape':
      closePlayer();
      break;
    case 'Space':
    case 'KeyK':
      e.preventDefault();
      if (player.paused) player.play(); else player.pause();
      break;
    case 'ArrowLeft':
      e.preventDefault();
      player.currentTime = Math.max(0, player.currentTime - 5);
      break;
    case 'ArrowRight':
      e.preventDefault();
      player.currentTime = Math.min(player.duration || 0, player.currentTime + 5);
      break;
    case 'KeyF':
      e.preventDefault();
      if (!document.fullscreenElement) {
        if (player.requestFullscreen) player.requestFullscreen();
        else if (player.webkitRequestFullscreen) player.webkitRequestFullscreen();
      } else {
        if (document.exitFullscreen) document.exitFullscreen();
      }
      break;
    case 'KeyM':
      e.preventDefault();
      player.muted = !player.muted;
      break;
    case 'KeyL':
      e.preventDefault();
      player.loop = !player.loop;
      showToast(player.loop ? 'Повтор включён' : 'Повтор выключен');
      break;
    case 'BracketLeft': // [
      e.preventDefault();
      changeSpeed(-0.25);
      break;
    case 'BracketRight': // ]
      e.preventDefault();
      changeSpeed(0.25);
      break;
  }
}

function changeSpeed(delta) {
  const current = dom.videoPlayer.playbackRate;
  const newRate = Math.max(0.5, Math.min(2.0, current + delta));
  dom.videoPlayer.playbackRate = newRate;
  dom.speedSelect.value = newRate.toString();
  showToast(`Скорость: ${newRate}x`);
}

function showToast(text) {
  let toast = document.getElementById('webToast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'webToast';
    toast.style.cssText = `
      position: fixed;
      bottom: 24px;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(20, 20, 25, 0.9);
      backdrop-filter: blur(8px);
      border: 1px solid #333344;
      color: #fff;
      padding: 8px 18px;
      border-radius: 20px;
      font-size: 0.85rem;
      z-index: 2000;
      pointer-events: none;
      transition: opacity 0.2s ease;
    `;
    document.body.appendChild(toast);
  }
  toast.textContent = text;
  toast.style.opacity = '1';
  clearTimeout(toast.timeout);
  toast.timeout = setTimeout(() => { toast.style.opacity = '0'; }, 1500);
}

function showLoader(visible) {
  dom.loader.style.display = visible ? 'flex' : 'none';
}

function formatBytes(bytes, decimals = 1) {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

function formatDate(timestamp) {
  if (!timestamp) return '';
  const d = new Date(timestamp);
  return d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

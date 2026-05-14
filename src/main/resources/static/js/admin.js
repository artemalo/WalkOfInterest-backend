const AUTH_TOKEN_KEY = 'accessToken';
const PAGE_SIZE = 20;

const STATUS_META = {
    PENDING:  { label: 'В ожидании', cls: 'st-pending'  },
    APPROVED: { label: 'Одобрено',   cls: 'st-approved' },
    REJECTED: { label: 'Отклонено',  cls: 'st-rejected' }
};

const ACTION_META = {
    CREATED:        { label: 'Создана',               icon: '✦' },
    UPDATED:        { label: 'Обновлена',             icon: '✎' },
    SUPPLEMENTED:   { label: 'Дополнена',             icon: '＋' },
    STATUS_CHANGED: { label: 'Статус изменён',        icon: '⇄' },
    DELETED:        { label: 'Удалена',               icon: '✕' }
};

let currentStatus = 'PENDING';
let currentPage   = 0;
let pendingRejectId = null;
let pendingDeleteId = null;

let searchDebounceTimer = null;

// ─────────────────── JWT ───────────────────

function parseJwt(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64    = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const json      = decodeURIComponent(
            window.atob(base64).split('').map(c =>
                '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
            ).join('')
        );
        return JSON.parse(json);
    } catch (e) {
        console.error('Ошибка парсинга токена', e);
        return null;
    }
}

// ─────────────────── HTTP ───────────────────

async function adminFetch(url, options = {}) {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    if (!token) { window.location.href = '/login'; return; }

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
        'Authorization': `Bearer ${token}`
    };

    try {
        const response = await fetch(url, { ...options, headers });
        if (response.status === 401 || response.status === 403) {
            console.warn('Сессия истекла или недостаточно прав');
            localStorage.removeItem(AUTH_TOKEN_KEY);
            window.location.href = '/login';
            return;
        }
        return response;
    } catch (error) {
        console.error('Ошибка сетевого запроса:', error);
        throw error;
    }
}

// ─────────────────── Utils ───────────────────

function escapeHtml(s) {
    if (s == null) return '';
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function formatDateTime(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('ru-RU');
}

// ─────────────────── Init ───────────────────

document.addEventListener('DOMContentLoaded', () => {
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(tab => {
        tab.addEventListener('click', e => {
            tabs.forEach(t => t.classList.remove('active'));
            e.currentTarget.classList.add('active');
            currentStatus = e.currentTarget.getAttribute('data-status');
            currentPage   = 0;
            updatePageDisplay();
            fetchPois();
        });
    });

    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                currentPage = 0;
                updatePageDisplay();
                fetchPois();
            }, 350);
        });
    }

    ['coord-lat', 'coord-lon'].forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('change', () => {
                currentPage = 0;
                updatePageDisplay();
                fetchPois();
            });
        }
    });

    const ownerFilter = document.getElementById('owner-filter');
    if (ownerFilter) {
        ownerFilter.addEventListener('change', () => {
            currentPage = 0;
            updatePageDisplay();
            fetchPois();
        });
    }

    document.addEventListener('keydown', e => {
        if (e.key === 'Escape') { closeRejectModal(); closeDeleteModal(); closePhotoModal(); }
    });
    document.getElementById('reject-modal').addEventListener('click', e => {
        if (e.target.id === 'reject-modal') closeRejectModal();
    });
    document.getElementById('delete-modal').addEventListener('click', e => {
        if (e.target.id === 'delete-modal') closeDeleteModal();
    });

    fetchPois();
});

// ─────────────────── Pagination ───────────────────

window.changePage = function(direction) {
    if (currentPage + direction < 0) return;
    currentPage += direction;
    updatePageDisplay();
    fetchPois();
    window.scrollTo({ top: 0, behavior: 'smooth' });
};

function updatePageDisplay() {
    const el = document.getElementById('current-page-num');
    if (el) el.innerText = currentPage + 1;
}

// ─────────────────── Filters state ───────────────────

function getFilters() {
    const search    = (document.getElementById('search-input')?.value || '').trim() || null;
    const ownerType = (document.getElementById('owner-filter')?.value  || '') || null;
    const latRaw    = document.getElementById('coord-lat')?.value;
    const lonRaw    = document.getElementById('coord-lon')?.value;
    const lat = latRaw && latRaw.trim() ? parseFloat(latRaw) : null;
    const lon = lonRaw && lonRaw.trim() ? parseFloat(lonRaw) : null;
    return { search, ownerType, lat, lon };
}

window.clearCoords = function() {
    const latEl = document.getElementById('coord-lat');
    const lonEl = document.getElementById('coord-lon');
    if (latEl) latEl.value = '';
    if (lonEl) lonEl.value = '';
    currentPage = 0;
    updatePageDisplay();
    fetchPois();
};

// ─────────────────── Fetch ───────────────────

function buildUrl(status, page) {
    const { search, ownerType, lat, lon } = getFilters();
    const params = new URLSearchParams({
        request: status,
        page,
        size: PAGE_SIZE
    });
    if (search)    params.set('search',    search);
    if (ownerType) params.set('ownerType', ownerType);
    if (lat != null && !isNaN(lat)) params.set('lat', lat);
    if (lon != null && !isNaN(lon)) params.set('lon', lon);
    return `/api/admin/pois?${params.toString()}`;
}

function fetchPois() {
    const container = document.getElementById('poi-container');
    if (!container) return;
    container.innerHTML = '<div class="loader">Загрузка…</div>';

    adminFetch(buildUrl(currentStatus, currentPage))
        .then(res => res.json())
        .then(data => renderPois(data))
        .catch(err => {
            container.innerHTML =
                `<p class="error-msg">Ошибка загрузки данных: ${escapeHtml(err.message)}</p>`;
        });
}

// ─────────────────── Render ───────────────────

function renderPois(pois) {
    const container = document.getElementById('poi-container');
    container.innerHTML = '';

    if (!pois || pois.length === 0) {
        container.innerHTML = '<p class="empty-msg">В этой категории ничего не найдено.</p>';
        return;
    }

    pois.forEach(poi => container.appendChild(renderPoiCard(poi)));
}

function renderPoiCard(poi) {
    const updateDate = poi.lastUpdate ? formatDateTime(poi.lastUpdate) : 'неизвестно';

    const ownerHtml = poi.createdUser
        ? `<span class="owner-tag owner-user" title="POI создан пользователем">
               👤 ${escapeHtml(poi.createdUser.username)}
           </span>`
        : `<span class="owner-tag owner-system" title="OSM-импорт, владельца нет">
               🌐 OSM
           </span>`;

    const status     = poi.status || currentStatus;
    const statusMeta = STATUS_META[status] || { label: status, cls: '' };
    const statusBadge = `<span class="status-badge ${statusMeta.cls}">${statusMeta.label}</span>`;

    const tagsHtml = (poi.tags || [])
        .map(t => `<span class="tag-badge">${escapeHtml(t.subcategoryName)}</span>`)
        .join('') || '<span class="muted">подкатегорий нет</span>';

    const point  = poi.point || {};
    const lat    = point.lat != null ? point.lat.toFixed(6) : '—';
    const lon    = point.lon != null ? point.lon.toFixed(6) : '—';
    const osmUrl = (point.lat != null && point.lon != null)
        ? `https://www.openstreetmap.org/?mlat=${point.lat}&mlon=${point.lon}#map=17/${point.lat}/${point.lon}`
        : null;

    const yandexUrl = (point.lat != null && point.lon != null)
            ? `https://yandex.ru/maps/?pt=${point.lon},${point.lat}&z=17&l=map`
            : null;

    const rejectionBlock = (status === 'REJECTED' && poi.rejectionReason)
        ? `<div class="rejection-block">
               <div class="rejection-label">Причина отклонения</div>
               <div class="rejection-text">${escapeHtml(poi.rejectionReason)}</div>
           </div>`
        : '';

    const photoUrl = poi.photoUrl || null;
    const photoHtml = photoUrl
        ? `<div class="poi-photo-wrap">
               <img class="poi-thumb" src="${escapeHtml(photoUrl)}" alt="Фото POI"
                    onclick="openPhotoModal('${escapeHtml(photoUrl)}')" title="Открыть фото" />
               <button class="poi-photo-del-btn" onclick="deletePoiPhoto(${poi.id})" title="Удалить фото">✕</button>
           </div>`
        : `<div class="poi-photo-wrap poi-photo-empty" title="Фото отсутствует">
               <span class="poi-photo-placeholder">нет фото</span>
           </div>`;

    const item = document.createElement('div');
    item.className   = 'poi-item';
    item.dataset.poiId = poi.id;

    item.innerHTML = `
        <div class="poi-header">
            ${photoHtml}
            <div class="poi-info">
                <div class="poi-title-row">
                    <h3>${escapeHtml(poi.name) || '<span class="muted">POI без названия</span>'}</h3>
                    ${statusBadge}
                </div>
                <div class="poi-meta">
                    ${ownerHtml}
                    <span class="meta-sep">·</span>
                    <span class="meta-item">Обновлено: ${updateDate}</span>
                    <span class="meta-sep">·</span>
                    <span class="meta-item">ID: ${poi.id}</span>
                </div>
            </div>
            <div class="poi-actions">
                ${actionButtonsFor(status, poi.id)}
                <button class="expand-btn" onclick="toggleDetails(this)" title="Подробности">▼</button>
            </div>
        </div>
        ${rejectionBlock}
        <div class="poi-details">
            <hr>
            <p><strong>Описание:</strong> ${escapeHtml(poi.description) || '<span class="muted">отсутствует</span>'}</p>
            <p>
                <strong>Координаты:</strong> ${lat}, ${lon}
                ${osmUrl ? `<a class="map-link" href="${osmUrl}" target="_blank" rel="noopener">смотреть в OSM ↗</a>` : ''}
                ${yandexUrl ? `<a class="map-link" href="${yandexUrl}" target="_blank" rel="noopener">смотреть в Яндекс ↗</a>` : ''}
            </p>
            <p><strong>Язык:</strong> <span class="lang-tag">${escapeHtml(poi.lang) || 'default'}</span></p>
            <div class="tags-container">${tagsHtml}</div>
            <div class="history-section" id="history-${poi.id}">
                <div class="history-header">
                    <span class="history-title">История изменений</span>
                    <button class="btn btn-ghost history-load-btn" onclick="loadHistory(${poi.id})">Загрузить</button>
                </div>
                <div class="history-list" id="history-list-${poi.id}"></div>
            </div>
        </div>
    `;

    return item;
}

// ─────────────────── POI Photo ───────────────────

window.openPhotoModal = function(url) {
    const modal = document.getElementById('photo-modal');
    const img   = document.getElementById('photo-modal-img');
    if (!modal || !img) return;
    img.src = url;
    modal.classList.remove('hidden');
};

window.closePhotoModal = function() {
    const modal = document.getElementById('photo-modal');
    if (modal) modal.classList.add('hidden');
};

window.deletePoiPhoto = async function(id) {
    if (!confirm(`Удалить фото POI #${id}? Это действие нельзя отменить.`)) return;

    const res = await adminFetch(`/api/admin/pois/${id}/photo`, { method: 'DELETE' });

    if (res && (res.ok || res.status === 204)) {
        // Обновляем только карточку: убираем фото без перезагрузки всего списка
        const card = document.querySelector(`.poi-item[data-poi-id="${id}"]`);
        if (card) {
            const wrap = card.querySelector('.poi-photo-wrap');
            if (wrap) {
                wrap.outerHTML = `<div class="poi-photo-wrap poi-photo-empty" title="Фото отсутствует">
                    <span class="poi-photo-placeholder">нет фото</span>
                </div>`;
            }
        }
    } else if (res) {
        const text = await res.text().catch(() => '');
        alert(`Не удалось удалить фото (HTTP ${res.status}). ${text}`);
    }
};

function actionButtonsFor(status, id) {
    const buttons = [];
    if (status !== 'APPROVED') {
        buttons.push(`<button class="btn btn-approve" onclick="quickAction(${id}, 'APPROVED')">✓ Одобрить</button>`);
    }
    if (status !== 'REJECTED') {
        buttons.push(`<button class="btn btn-reject" onclick="openRejectModal(${id})">✗ Отклонить</button>`);
    }
    if (status !== 'PENDING') {
        buttons.push(`<button class="btn btn-ghost" onclick="quickAction(${id}, 'PENDING')">↺ В ожидание</button>`);
    }
    if (status === 'REJECTED') {
        buttons.push(`<button class="btn btn-delete" onclick="openDeleteModal(${id})">🗑 Удалить</button>`);
    }
    return buttons.join('');
}

// ─────────────────── Details / History ───────────────────

window.toggleDetails = function(btn) {
    const item    = btn.closest('.poi-item');
    const details = item.querySelector('.poi-details');
    const isOpen  = details.classList.toggle('open');
    btn.innerText = isOpen ? '▲' : '▼';
};

window.loadHistory = async function(poiId) {
    const listEl  = document.getElementById(`history-list-${poiId}`);
    const loadBtn = listEl?.previousElementSibling?.querySelector('.history-load-btn');
    if (!listEl) return;

    if (loadBtn) { loadBtn.disabled = true; loadBtn.innerText = 'Загрузка…'; }

    try {
        const res  = await adminFetch(`/api/admin/pois/${poiId}/history`);
        const data = await res.json();

        if (!data || data.length === 0) {
            listEl.innerHTML = '<p class="muted history-empty">История пуста.</p>';
            return;
        }

        listEl.innerHTML = data.map(renderHistoryItem).join('');
    } catch (e) {
        listEl.innerHTML = `<p class="error-msg">Не удалось загрузить историю: ${escapeHtml(e.message)}</p>`;
    } finally {
        if (loadBtn) loadBtn.style.display = 'none';
    }
};

function renderHistoryItem(h) {
    const meta      = ACTION_META[h.actionType] || { label: h.actionType, icon: '?' };
    const oldSt     = h.oldStatus ? (STATUS_META[h.oldStatus]?.label || h.oldStatus) : null;
    const newSt     = h.newStatus ? (STATUS_META[h.newStatus]?.label || h.newStatus) : null;
    const statusLine = oldSt && newSt && oldSt !== newSt
        ? `<span class="hist-status">${escapeHtml(oldSt)} → ${escapeHtml(newSt)}</span>`
        : (newSt ? `<span class="hist-status">${escapeHtml(newSt)}</span>` : '');
    const noteHtml  = h.note ? `<div class="hist-note">${escapeHtml(h.note)}</div>` : '';
    const actor     = escapeHtml(h.actorUsername || 'system');

    return `
        <div class="hist-item">
            <div class="hist-icon">${meta.icon}</div>
            <div class="hist-body">
                <div class="hist-action">${meta.label} ${statusLine}</div>
                <div class="hist-meta">
                    <span class="hist-actor">${actor}</span>
                    <span class="meta-sep">·</span>
                    <span class="hist-date">${formatDateTime(h.changedAt)}</span>
                </div>
                ${noteHtml}
            </div>
        </div>`;
}

// ─────────────────── Actions ───────────────────

window.quickAction = async function(id, newStatus) {
    const confirmMessage = `Изменить статус POI #${id} на «${STATUS_META[newStatus].label}»?`;
    if (!confirm(confirmMessage)) return;
    await applyStatus(id, newStatus, null);
};

// Reject modal
window.openRejectModal = function(id) {
    pendingRejectId = id;
    const modal = document.getElementById('reject-modal');
    const input = document.getElementById('reject-reason-input');
    input.value = '';
    modal.classList.remove('hidden');
    setTimeout(() => input.focus(), 50);
};

window.closeRejectModal = function() {
    pendingRejectId = null;
    document.getElementById('reject-modal').classList.add('hidden');
};

window.confirmReject = async function() {
    if (pendingRejectId == null) return;
    const reason = document.getElementById('reject-reason-input').value.trim();
    const id = pendingRejectId;
    closeRejectModal();
    await applyStatus(id, 'REJECTED', reason || null);
};

async function applyStatus(id, newStatus, rejectionReason) {
    const params = new URLSearchParams({ request: newStatus });
    if (rejectionReason) params.set('rejectionReason', rejectionReason);

    const res = await adminFetch(`/api/admin/pois/${id}/status?${params.toString()}`, {
        method: 'PATCH'
    });

    if (res && res.ok) {
        fetchPois();
    } else if (res) {
        const text = await res.text().catch(() => '');
        alert(`Не удалось обновить статус (HTTP ${res.status}). ${text}`);
    }
}

// Delete modal
window.openDeleteModal = function(id) {
    pendingDeleteId = id;
    document.getElementById('delete-modal').classList.remove('hidden');
};

window.closeDeleteModal = function() {
    pendingDeleteId = null;
    document.getElementById('delete-modal').classList.add('hidden');
};

window.confirmDelete = async function() {
    if (pendingDeleteId == null) return;
    const id = pendingDeleteId;
    closeDeleteModal();

    const res = await adminFetch(`/api/admin/pois/${id}`, { method: 'DELETE' });

    if (res && (res.ok || res.status === 204)) {
        fetchPois();
    } else if (res) {
        const text = await res.text().catch(() => '');
        alert(`Не удалось удалить POI #${id} (HTTP ${res.status}). ${text}`);
    }
};

// ─────────────────── Logout ───────────────────

window.logout = function() {
    localStorage.removeItem(AUTH_TOKEN_KEY);

    const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute('content');
    const form      = document.createElement('form');
    form.method = 'POST';
    form.action = '/logout';

    const hidden = document.createElement('input');
    hidden.type  = 'hidden';
    hidden.name  = '_csrf';
    hidden.value = csrfToken;

    form.appendChild(hidden);
    document.body.appendChild(form);
    form.submit();
};

const AUTH_TOKEN_KEY = 'accessToken';
const PAGE_SIZE = 20;

const STATUS_META = {
    PENDING:  { label: 'В ожидании', cls: 'st-pending' },
    APPROVED: { label: 'Одобрено',   cls: 'st-approved' },
    REJECTED: { label: 'Отклонено',  cls: 'st-rejected' }
};

let currentStatus = 'PENDING';
let currentPage = 0;
let pendingRejectId = null;

function parseJwt(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error("Ошибка парсинга токена", e);
        return null;
    }
}

async function adminFetch(url, options = {}) {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);

    if (!token) {
        window.location.href = '/login';
        return;
    }

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
        'Authorization': `Bearer ${token}`
    };

    try {
        const response = await fetch(url, { ...options, headers });

        if (response.status === 401 || response.status === 403) {
            console.warn("Сессия истекла или недостаточно прав");
            localStorage.removeItem(AUTH_TOKEN_KEY);
            window.location.href = '/login';
            return;
        }

        return response;
    } catch (error) {
        console.error("Ошибка сетевого запроса:", error);
        throw error;
    }
}

function escapeHtml(s) {
    if (s == null) return '';
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

document.addEventListener("DOMContentLoaded", () => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    if (token) {
        const payload = parseJwt(token);
        const usernameDisplay = document.getElementById('display-username');
        if (payload && usernameDisplay) {
            usernameDisplay.innerText = payload.sub || payload.username || "Администратор";
        }
    }

    const container = document.getElementById('poi-container');
    const tabs = document.querySelectorAll('.tab-btn');
    const pageNumDisplay = document.getElementById('current-page-num');

    fetchPois(currentStatus, currentPage);

    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            tabs.forEach(t => t.classList.remove('active'));
            e.currentTarget.classList.add('active');

            currentStatus = e.currentTarget.getAttribute('data-status');
            currentPage = 0;
            updatePageDisplay();
            fetchPois(currentStatus, currentPage);
        });
    });

    window.changePage = function(direction) {
        if (currentPage + direction < 0) return;
        currentPage += direction;
        updatePageDisplay();
        fetchPois(currentStatus, currentPage);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    function updatePageDisplay() {
        if (pageNumDisplay) pageNumDisplay.innerText = currentPage + 1;
    }

    function fetchPois(status, page = 0) {
        if (!container) return;
        container.innerHTML = '<div class="loader">Загрузка…</div>';

        adminFetch(`/api/admin/pois?request=${status}&page=${page}&size=${PAGE_SIZE}`)
            .then(res => res.json())
            .then(data => renderPois(data))
            .catch(err => {
                container.innerHTML = `<p class="error-msg">Ошибка загрузки данных: ${escapeHtml(err.message)}</p>`;
            });
    }

    function renderPois(pois) {
        container.innerHTML = '';

        if (!pois || pois.length === 0) {
            container.innerHTML = '<p class="empty-msg">В этой категории ничего не найдено.</p>';
            return;
        }

        pois.forEach(poi => container.appendChild(renderPoiCard(poi)));
    }

    function renderPoiCard(poi) {
        const updateDate = poi.lastUpdate
            ? new Date(poi.lastUpdate).toLocaleString('ru-RU')
            : 'неизвестно';

        const ownerHtml = poi.createdUser
            ? `<span class="owner-tag owner-user" title="POI создан пользователем">
                   👤 ${escapeHtml(poi.createdUser.username)}
               </span>`
            : `<span class="owner-tag owner-system" title="OSM-импорт, владельца нет — может дополнять любой пользователь">
                   🌐 OSM
               </span>`;

        const status = poi.status || currentStatus;
        const statusMeta = STATUS_META[status] || { label: status, cls: '' };
        const statusBadge = `<span class="status-badge ${statusMeta.cls}">${statusMeta.label}</span>`;

        const tagsHtml = (poi.tags || [])
            .map(t => `<span class="tag-badge">${escapeHtml(t.subcategoryName)}</span>`)
            .join('') || '<span class="muted">подкатегорий нет</span>';

        const point = poi.point || {};
        const lat = point.lat != null ? point.lat.toFixed(6) : '—';
        const lon = point.lon != null ? point.lon.toFixed(6) : '—';
        const osmUrl = (point.lat != null && point.lon != null)
            ? `https://www.openstreetmap.org/?mlat=${point.lat}&mlon=${point.lon}#map=17/${point.lat}/${point.lon}`
            : null;

        const rejectionBlock = (status === 'REJECTED' && poi.rejectionReason)
            ? `<div class="rejection-block">
                  <div class="rejection-label">Причина отклонения</div>
                  <div class="rejection-text">${escapeHtml(poi.rejectionReason)}</div>
               </div>`
            : '';

        const item = document.createElement('div');
        item.className = 'poi-item';
        item.dataset.poiId = poi.id;

        item.innerHTML = `
            <div class="poi-header">
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
                    ${osmUrl ? `<a class="map-link" href="${osmUrl}" target="_blank" rel="noopener">смотреть на OSM ↗</a>` : ''}
                </p>
                <p><strong>Язык:</strong> <span class="lang-tag">${escapeHtml(poi.lang) || 'default'}</span></p>
                <div class="tags-container">${tagsHtml}</div>
            </div>
        `;

        return item;
    }

    function actionButtonsFor(status, id) {
        const buttons = [];
        if (status !== 'APPROVED') {
            buttons.push(`<button class="btn btn-approve" onclick="quickAction(${id}, 'APPROVED')">✓ Одобрить</button>`);
        }
        if (status !== 'REJECTED') {
            buttons.push(`<button class="btn btn-reject" onclick="openRejectModal(${id})">✗ Отклонить</button>`);
        }
        if (status !== 'PENDING') {
            buttons.push(`<button class="btn btn-ghost" onclick="quickAction(${id}, 'PENDING')">↺ Вернуть в ожидание</button>`);
        }
        return buttons.join('');
    }

    window.toggleDetails = function(btn) {
        const item = btn.closest('.poi-item');
        const details = item.querySelector('.poi-details');
        const isVisible = details.classList.toggle('open');
        btn.innerText = isVisible ? '▲' : '▼';
    };

    window.quickAction = async function(id, newStatus) {
        const confirmMessage = `Изменить статус POI #${id} на «${STATUS_META[newStatus].label}»?`;
        if (!confirm(confirmMessage)) return;

        await applyStatus(id, newStatus, null);
    };

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
            fetchPois(currentStatus, currentPage);
        } else if (res) {
            const text = await res.text().catch(() => '');
            alert(`Не удалось обновить статус (HTTP ${res.status}). ${text}`);
        }
    }

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeRejectModal();
    });
    document.getElementById('reject-modal').addEventListener('click', (e) => {
        if (e.target.id === 'reject-modal') closeRejectModal();
    });

    window.logout = function() {
        localStorage.removeItem('accessToken');

        const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/logout';

        const hiddenField = document.createElement('input');
        hiddenField.type = 'hidden';
        hiddenField.name = '_csrf';
        hiddenField.value = csrfToken;

        form.appendChild(hiddenField);
        document.body.appendChild(form);

        form.submit();
    };
});
const AUTH_TOKEN_KEY = 'accessToken';

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

    let currentStatus = 'PENDING';
    let currentPage = 0;
    const PAGE_SIZE = 20;

    // Инициализация данных
    fetchPois(currentStatus, currentPage);

    // Логика вкладок
    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            tabs.forEach(t => t.classList.remove('active'));
            e.target.classList.add('active');

            currentStatus = e.target.getAttribute('data-status');
            currentPage = 0; // Сброс на первую страницу при смене вкладки
            updatePageDisplay();
            fetchPois(currentStatus, currentPage);
        });
    });

    // Пагинация
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
        container.innerHTML = '<div class="loader">Загрузка...</div>';

        adminFetch(`/api/admin/pois?request=${status}&page=${page}&size=${PAGE_SIZE}`)
            .then(res => res.json())
            .then(data => renderPois(data))
            .catch(err => {
                container.innerHTML = `<p style="color:red">Ошибка загрузки данных: ${err.message}</p>`;
            });
    }

    function renderPois(pois) {
        container.innerHTML = '';

        if (!pois || pois.length === 0) {
            container.innerHTML = '<p class="empty-msg">В этой категории ничего не найдено.</p>';
            return;
        }

        pois.forEach(poi => {
            const updateDate = poi.lastUpdate ? new Date(poi.lastUpdate).toLocaleString('ru-RU') : 'Неизвестно';
            const author = poi.createdUser ? poi.createdUser.username : 'Система';
            const tagsHtml = (poi.tags || []).map(tag =>
                `<span class="tag-badge">${tag.subcategoryName}</span>`
            ).join('');

            const item = document.createElement('div');
            item.className = 'poi-item';

            const statusSelectHtml = `
                <select id="status-select-${poi.id}" class="status-dropdown">
                    <option value="" disabled selected>Изменить статус...</option>
                    ${currentStatus !== 'APPROVED' ? `<option value="APPROVED">Одобрить</option>` : ''}
                    ${currentStatus !== 'REJECTED' ? `<option value="REJECTED">Отклонить</option>` : ''}
                    ${currentStatus !== 'PENDING' ? `<option value="PENDING">Вернуть в ожидание</option>` : ''}
                </select>
                <button class="btn-apply" onclick="applyStatusChange(${poi.id})">Применить</button>
            `;

            item.innerHTML = `
                <div class="poi-header">
                    <div class="poi-photo-placeholder">
                        ${poi.imagePath ? `<img src="${poi.imagePath}" alt="poi">` : 'Нет фото'}
                    </div>
                    <div class="poi-info">
                        <h3>${poi.name || 'POI без названия'}</h3>
                        <p>Автор: <b>${author}</b> | Обновлено: ${updateDate}</p>
                    </div>
                    <div class="poi-actions">
                        ${statusSelectHtml}
                        <button class="expand-btn" onclick="toggleDetails(this)">▼</button>
                    </div>
                </div>
                <div class="poi-details" style="display: none;">
                    <hr>
                    <p><strong>Описание:</strong> ${poi.description || 'Описание отсутствует.'}</p>
                    <p><strong>Координаты:</strong> Широта, Долгота: ${poi.point.lat}, ${poi.point.lon}</p>
                    <p><strong>Язык:</strong> <span class="lang-tag">${poi.lang || 'По умолчанию'}</span></p>
                    <div class="tags-container">${tagsHtml}</div>
                </div>
            `;
            container.appendChild(item);
        });
    }

    window.toggleDetails = function(btn) {
        const item = btn.closest('.poi-item');
        const details = item.querySelector('.poi-details');
        const isVisible = details.style.display === 'block';

        details.style.display = isVisible ? 'none' : 'block';
        btn.innerText = isVisible ? '▼' : '▲';
    };

    window.applyStatusChange = function(id) {
        const selectElement = document.getElementById(`status-select-${id}`);
        const newStatus = selectElement.value;

        if (!newStatus) {
            alert("Пожалуйста, выберите статус из списка.");
            return;
        }

        let confirmMessage = `Вы уверены, что хотите изменить статус на "${newStatus}"?`;

        if (newStatus === 'REJECTED') {
            confirmMessage = `ВНИМАНИЕ!\nВы уверены, что хотите ОТКЛОНИТЬ эту запись?`;
        }

        if (!confirm(confirmMessage)) {
            selectElement.value = "";
            return;
        }

        adminFetch(`/api/admin/pois/${id}/status?request=${newStatus}`, {
            method: 'PATCH'
        })
        .then(res => {
            if (res && res.ok) {
                fetchPois(currentStatus, currentPage);
            }
        })
        .catch(err => alert("Не удалось обновить статус: " + err.message));
    };

    window.logout = function() {
        localStorage.removeItem('accessToken');

        // TODO: logout! (this not work)

        const form = document.getElementById('logout-form');
        if (form) {
            form.submit();
        } else {
            window.location.href = '/login?logout';
        }
    };
});
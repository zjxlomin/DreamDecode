$(document).ready(function() {

    // 페이지네이션 변수
    let currentPage = 0;
    let currentSearchType = 'title'; // 기본값: 제목 검색
    let currentSearchQuery = '';
    let isLoading = false;

    // DOM 요소 캐싱
    const $dreamsContainer = $('#dreamsContainer');
    const $loadMoreContainer = $('#loadMoreContainer');
    const $viewDreamModal = $('#viewDreamModal');
    const $viewDreamModalLoading = $('#viewDreamModalLoading');
    const $createDreamModalLoading = $('#createDreamModalLoading');
    const $searchInput = $('#searchInput');
    const $resetBtn = $('#resetBtn');

    // 로그인 상태 확인하여 꿈 등록 버튼 표시
    function checkLoginAndShowCreateButton() {
        // HttpOnly 쿠키는 JavaScript에서 읽을 수 없으므로 서버 API로 확인
        $.ajax({
            url: '/api/users/me',
            method: 'GET',
            success: function() {
                $('#createDreamBtn').removeClass('d-none');
            },
            error: function() {
                $('#createDreamBtn').addClass('d-none');
            }
        });
    }

    // 페이지 로드 시 버튼 표시 확인
    checkLoginAndShowCreateButton();

    // HTML 이스케이프 함수
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // 카테고리 HTML 생성
    function renderCategories(categories) {
        if (!categories || categories.length === 0) return '';
        return '<div class="dream-meta mb-2"><div class="dream-categories">' +
            categories.map(cat => `
                <button id="category" class="category-badge" style="border: none;"
                    value="${escapeHtml(cat)}" onclick="searchByCatOrTag(this.value, 1)">
                    ${escapeHtml(cat)}
                </button>
            `).join('') +
            '</div></div>';
    }

    // 태그 HTML 생성
    function renderTags(tags) {
        if (!tags || tags.length === 0) return '';
        return '<div class="dream-tags mt-3">' +
            tags.map(tag => `
                <button id="tag" class="tag-btn"
                    value="${escapeHtml(tag)}" onclick="searchByCatOrTag(this.value, 2)">
                    #${escapeHtml(tag)}
                </button>
            `).join('') +
            '</div>';
    }

    // 꿈 카드 HTML 생성
    function createDreamCard(dream) {
        const categoriesHtml = renderCategories(dream.categories);
        const tagsHtml = renderTags(dream.tags);
        return `
            <div class="col-md-4">
                <div class="card h-100 dream-card" data-dream-id="${dream.id}">
                    <div class="card-body">
                        <h5 class="card-title">${escapeHtml(dream.title || '')}</h5>
                        ${categoriesHtml}
                        <p class="card-text">${escapeHtml(dream.content || '')}</p>
                        ${tagsHtml}
                    </div>
                    <div class="card-footer text-center">
                        <a href="#" class="btn btn-primary btn-sm">자세히 보기</a>
                    </div>
                </div>
            </div>
        `;
    }

    // 검색 URL 생성
    function buildSearchUrl(page) {
        if (!currentSearchQuery) {
            return `/api/dream?page=${page}`;
        }
        const encodedQuery = encodeURIComponent(currentSearchQuery);
        const urlMap = {
            'title': `/api/dream/title?title=${encodedQuery}&page=${page}`,
            'category': `/api/dream/category/${encodedQuery}?page=${page}`,
            'tag': `/api/dream/tag/${encodedQuery}?page=${page}`
        };
        return urlMap[currentSearchType] || urlMap['title'];
    }

    // Page 객체에서 hasNext 계산
    function calculateHasNext(data) {
        if (data.hasNext !== undefined) return data.hasNext;
        if (data.last !== undefined) return !data.last;
        if (data.totalPages !== undefined && data.number !== undefined) {
            return data.number < data.totalPages - 1;
        }
        return false;
    }

    // 꿈 목록 렌더링
    function renderDreams(dreams, append = false) {
        if (dreams.length === 0) {
            if (!append) {
                $dreamsContainer.html('<div class="col-12"><div class="search-results-message no-results">검색 결과가 없습니다.</div></div>');
            }
            return;
        }

        const html = dreams.map(createDreamCard).join('');
        if (append) {
            $dreamsContainer.append(html);
        } else {
            $dreamsContainer.html(html);
        }
    }

    // 더보기 버튼 표시/숨김
    function toggleLoadMoreButton(hasNext, context = '') {
        if (hasNext) {
            $loadMoreContainer.show();
            console.log(`[${context}] 다음 페이지가 있어 더보기 버튼 표시`);
        } else {
            $loadMoreContainer.hide();
            console.log(`[${context}] 마지막 페이지 도달 - 더보기 버튼 숨김`);
        }
    }

    // 로딩 오버레이 관리
    function showLoadingOverlay(modalType) {
        const $loading = modalType === 'view' ? $viewDreamModalLoading : $createDreamModalLoading;
        $loading.removeClass('d-none').addClass('d-flex');
    }

    function hideLoadingOverlay(modalType) {
        const $loading = modalType === 'view' ? $viewDreamModalLoading : $createDreamModalLoading;
        $loading.removeClass('d-flex').addClass('d-none');
    }

    // 버튼 활성화/비활성화
    function setButtonsDisabled(disabled, ...buttonIds) {
        buttonIds.forEach(id => {
            const btn = document.getElementById(id);
            if (btn) btn.disabled = disabled;
        });
    }

    // 현재 로그인한 사용자 ID 가져오기 (서버 API 호출)
    // HttpOnly 쿠키는 JavaScript에서 읽을 수 없으므로 서버에서 확인
    let cachedUserId = null;
    let isFetchingUserId = false;
    
    function getCurrentUserId(callback) {
        // 캐시된 값이 있으면 즉시 반환
        if (cachedUserId !== null) {
            callback(cachedUserId);
            return;
        }
        
        // 이미 요청 중이면 대기
        if (isFetchingUserId) {
            // 간단한 폴링으로 대기 (실제로는 Promise를 사용하는 것이 좋지만 jQuery 환경)
            setTimeout(function() {
                getCurrentUserId(callback);
            }, 100);
            return;
        }
        
        isFetchingUserId = true;
        
        $.ajax({
            url: '/api/users/me',
            method: 'GET',
            success: function(data) {
                // UserProfileResponse의 id 필드 사용
                cachedUserId = data.id || null;
                isFetchingUserId = false;
                callback(cachedUserId);
            },
            error: function() {
                cachedUserId = null;
                isFetchingUserId = false;
                callback(null);
            }
        });
    }
    
    // 캐시 초기화 함수 (로그인/로그아웃 시 호출)
    function clearUserIdCache() {
        cachedUserId = null;
    }
    
    // 로그인/로그아웃 이벤트 감지하여 캐시 초기화
    $(document).ajaxSuccess(function(event, xhr, settings) {
        if (settings.url === '/api/auth/login' || settings.url === '/api/auth/logout') {
            clearUserIdCache();
        }
    });

    // 문자열/배열을 배열로 변환 (categories, tags 공통 처리)
    function parseArrayField(field) {
        if (!field) return [];
        if (Array.isArray(field)) return field;
        if (typeof field === 'string') {
            try {
                return JSON.parse(field);
            } catch (e) {
                return field.split(',').map(item => item.trim()).filter(item => item.length > 0);
            }
        }
        return [];
    }

    // 모달 내용 업데이트 함수
    function updateModalWithAnalysisData(data) {
        $('#detailTitle').text(data.dreamTitle || '');
        $('#detailContent').text(data.dreamContent || '');

        // Scenes 처리
        if (data.scenes && data.scenes.length > 0) {
            const html = data.scenes.map(item => `
                <div class="card h-100 dream-card" style="margin: 8px;">
                    <div class="card-body" style="padding: 16px;">
                        <h5 class="card-title">${escapeHtml(item.content)}</h5>
                        <p class="card-text">= ${escapeHtml(item.emotion)}</p>
                        <p class="card-text">${escapeHtml(item.interpretation)}</p>
                    </div>
                </div>
            `).join('');
            $('#detailScenes').html(html);
        } else {
            $('#detailScenes').empty();
        }

        $('#detailInsight').text(data.insight || '');
        $('#detailSuggestion').text(data.suggestion || '');

        // Categories 처리
        const categoriesArray = parseArrayField(data.categories);
        if (categoriesArray.length > 0) {
            const html = categoriesArray.map(cat => `
                <button id="category" class="col category-badge" style="max-width: max-content; margin: 4px; border: none;"
                    value="${escapeHtml(cat)}" data-bs-dismiss="modal"
                    onclick="searchByCatOrTag(this.value, 1)">
                    ${escapeHtml(cat)}
                </button>
            `).join('');
            $('#detailCategories').html(html);
        } else {
            $('#detailCategories').empty();
        }

        // Tags 처리
        const tagsArray = parseArrayField(data.tags);
        if (tagsArray.length > 0) {
            const html = tagsArray.map(tag => `
                <button id="tag" class="col tag-btn" style="max-width: max-content; margin: 4px;"
                    value="${escapeHtml(tag)}" data-bs-dismiss="modal"
                    onclick="searchByCatOrTag(this.value, 2)">
                    #${escapeHtml(tag)}
                </button>
            `).join('');
            $('#detailTags').html(html);
        } else {
            $('#detailTags').empty();
        }

        // 감정 점수 표시
        const sentimentRounded = typeof data.sentiment === 'number' ? data.sentiment.toFixed(2) : '-';
        $('#detailEmotion').text(sentimentRounded + '점');

        const magnitudeRounded = typeof data.magnitude === 'number' ? data.magnitude.toFixed(2) : '-';
        $('#detailMagnitude').text(magnitudeRounded + '점');

        $('#detailPublished').text(data.dreamPublished ? '공개' : '비공개');

        // 편집용 값 세팅
        $('#editTitle').val(data.dreamTitle || '');
        $('#editContent').val(data.dreamContent || '');
        $('#editPublished').prop('checked', !!data.dreamPublished);

        // 모달 dataset 업데이트
        const modalEl = document.getElementById('viewDreamModal');
        if (modalEl) {
            modalEl.dataset.dreamId = String(data.dreamId);
            modalEl.dataset.dreamContent = String(data.dreamContent);
        }

        // 작성자와 현재 로그인 사용자 비교하여 버튼 표시/숨김 처리
        const dreamUserId = data.userId ? parseInt(data.userId) : null;
        
        // HttpOnly 쿠키는 JavaScript에서 읽을 수 없으므로 서버 API로 사용자 ID 확인
        getCurrentUserId(function(currentUserId) {
            console.log('=== 버튼 표시 확인 ===');
            console.log('currentUserId (서버 API):', currentUserId, typeof currentUserId);
            console.log('dreamUserId (data.userId):', dreamUserId, typeof dreamUserId);
            console.log('data.userId (원본):', data.userId);

            const isOwner = currentUserId !== null && dreamUserId !== null && currentUserId === dreamUserId;
            console.log('isOwner:', isOwner);

            if (isOwner) {
                $('#deleteDreamBtn, #editDreamBtn, #reAnalyzeBtn').removeClass('d-none');
                console.log('버튼 표시됨');
            } else {
                $('#deleteDreamBtn, #editDreamBtn, #reAnalyzeBtn').addClass('d-none');
                console.log('버튼 숨김됨');
            }
        });
    }

    // 모달 모드 전환 (보기 <-> 편집)
    function switchModalMode(mode) {
        const isEdit = mode === 'edit';
        $('#viewDreamModalLabel').text(isEdit ? '꿈 수정' : '꿈 상세');

        if (isEdit) {
            $('#detailTitle, #detailContent, #detailPublished').addClass('d-none');
            $('#editTitle, #editContent').removeClass('d-none');
            $('#editPublishedWrap').removeClass('d-none');
            $('#detailScenesWrap, #detailInsightWrap, #detailSuggestionWrap, #detailCategoriesWrap, #detailTagsWrap, #detailEmotionWrap, #detailMagnitudeWrap').addClass('d-none');
            $('#reAnalyzeBtn, #editDreamBtn').addClass('d-none');
            $('#saveDreamBtn, #cancelEditBtn').removeClass('d-none');
        } else {
            $('#detailTitle, #detailContent, #detailPublished').removeClass('d-none');
            $('#editTitle, #editContent').addClass('d-none');
            $('#editPublishedWrap').addClass('d-none');
            $('#detailScenesWrap, #detailInsightWrap, #detailSuggestionWrap, #detailCategoriesWrap, #detailTagsWrap, #detailEmotionWrap, #detailMagnitudeWrap').removeClass('d-none');
            $('#saveDreamBtn, #cancelEditBtn').addClass('d-none');
            $('#reAnalyzeBtn, #editDreamBtn').removeClass('d-none');
        }
    }

    // 더보기 버튼 클릭
    $(document).on('click', '#loadMoreBtn', async function() {
        if (isLoading) return;

        isLoading = true;
        const $btn = $(this);
        const originalText = $btn.html();
        $btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-2"></span>로딩 중...');

        try {
            currentPage++;
            const url = buildSearchUrl(currentPage);
            const res = await fetch(url, { credentials: 'include' });
            if (!res.ok) throw new Error('로딩 실패');
            const data = await res.json();

            const dreams = data.content || [];
            const hasNext = calculateHasNext(data);

            const searchInfo = currentSearchQuery ? `(검색: ${currentSearchType} - "${currentSearchQuery}")` : '(전체)';
            console.log(`[더보기] 페이지 ${currentPage} ${searchInfo} - 받은 데이터: ${dreams.length}개`);
            console.log(`[더보기] Page 정보:`, {
                hasNext: data.hasNext,
                last: data.last,
                number: data.number,
                totalPages: data.totalPages,
                totalElements: data.totalElements,
                계산된hasNext: hasNext
            });

            if (dreams.length === 0) {
                toggleLoadMoreButton(false, '더보기');
                $btn.prop('disabled', false).html(originalText);
                return;
            }

            renderDreams(dreams, true);
            toggleLoadMoreButton(hasNext, '더보기');
            $btn.prop('disabled', false).html(originalText);
        } catch (e) {
            console.error(e);
            alert('더보기 로딩 중 오류가 발생했습니다.');
            $btn.prop('disabled', false).html(originalText);
            currentPage--;
        } finally {
            isLoading = false;
        }
    });

    // 삭제
    $(document).on('click', '#deleteDreamBtn', async function () {
        const modalEl = document.getElementById('viewDreamModal');
        const dreamId = modalEl?.dataset?.dreamId;
        if (!dreamId) return;

        if (!confirm('정말 삭제하시겠습니까?')) return;
        try {
            const res = await fetch(`/api/dream/${dreamId}`, { 
                method: 'DELETE',
                credentials: 'include'
            });
            if (!res.ok) throw new Error('삭제 실패');
            alert('삭제되었습니다.');
            const modal = window.bootstrap?.Modal.getOrCreateInstance(modalEl);
            modal?.hide();
            window.location.reload();
        } catch (e) {
            console.error(e);
            alert('오류가 발생했습니다: ' + (e.message || e));
        }
    });

    // 카드 '자세히 보기' 버튼 → 단건 조회 후 상세 모달 표시
    $(document).on('click', '.dream-card .btn.btn-primary', async function (e) {
        e.preventDefault();
        e.stopPropagation();
        if (this.id === 'submitDreamBtn') return;

        const $card = $(this).closest('.dream-card');
        const dreamId = $card.attr('data-dream-id');
        const userId = Number($card.attr('data-user-id'));
        if (!dreamId) return;

        try {
            const res = await fetch(`/api/dream/${dreamId}/analysis`, { credentials: 'include' });
            if (!res.ok) throw new Error('조회 실패');
            const data = await res.json();

            updateModalWithAnalysisData(data);

            const modalEl = document.getElementById('viewDreamModal');
            if (modalEl && window.bootstrap) {
                const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
                modal.show();
            }
        } catch (err) {
            console.error(err);
            if(getUserIdFromToken() === userId) {
                if(!confirm("분석 결과를 찾을 수 없습니다. 다시 분석하시겠습니까?")) return;

                this.disabled = true;
                this.textContent = '분석 중...';
                alert('꿈 분석을 시작합니다. 새로고침 시 작업이 취소될 수 있습니다.');
                try {
                    const analysisRes = await fetch(`/api/dream/${dreamId}/analysis`, {
                        method: 'POST',
                        credentials: 'include'
                    });
                    if (!analysisRes.ok) {
                        const msg = await analysisRes.text();
                        throw new Error(msg || '분석 실패');
                    }
                    alert('꿈 분석이 완료되었습니다.');
                    this.disabled = false;
                    window.location.reload();
                }
                catch (e) {
                    this.disabled = false;
                    this.textContent = '자세히 보기';
                    console.error(e);
                    alert('오류가 발생했습니다: ' + (e.message || e));
                }
            }
            else {
                alert('상세 조회 중 오류가 발생했습니다.');
            }
        }
    });

    // 수정 모드 토글
    $(document).on('click', '#editDreamBtn', function () {
        switchModalMode('edit');
    });

    // 저장 (PUT)
    $(document).on('click', '#saveDreamBtn', async function () {
        const modalEl = document.getElementById('viewDreamModal');
        const dreamId = modalEl?.dataset?.dreamId;
        if (!dreamId) return;

        const prevContent = modalEl?.dataset?.dreamContent;
        const title = $('#editTitle').val()?.toString().trim();
        const content = $('#editContent').val()?.toString().trim();
        const published = $('#editPublished').is(':checked');

        if (!title || !content) {
            alert('제목과 내용을 모두 입력해주세요.');
            return;
        }

        if (content !== prevContent) {
            setButtonsDisabled(true, 'saveDreamBtn', 'deleteDreamBtn');
            showLoadingOverlay('view');
        }

        try {
            const dreamRes = await fetch(`/api/dream/${dreamId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title, content, published }),
                credentials: 'include'
            });
            if (!dreamRes.ok) throw new Error('수정 실패');

            const analysisRes = await fetch(`/api/dream/${dreamId}/analysis`, { credentials: 'include' });
            if (!analysisRes.ok) throw new Error('분석 결과 조회 실패');
            const data = await analysisRes.json();

            updateModalWithAnalysisData(data);

            const $card = $(`.dream-card[data-dream-id='${dreamId}']`);
            $card.find('.card-title').text(data.dreamTitle || title);
            $card.find('.card-text').text(data.dreamContent || content);

            switchModalMode('view');
            hideLoadingOverlay('view');
            alert('수정되었습니다.');
            setButtonsDisabled(false, 'saveDreamBtn', 'deleteDreamBtn');
        } catch (e) {
            console.error(e);
            hideLoadingOverlay('view');
            alert('오류가 발생했습니다: ' + (e.message || e));
            setButtonsDisabled(false, 'saveDreamBtn', 'deleteDreamBtn');
        }
    });

    // 편집 취소 (보기 모드 복귀)
    $(document).on('click', '#cancelEditBtn', function () {
        switchModalMode('view');
    });

    // 꿈 등록 제출 핸들러
    const $submitBtn = $('#submitDreamBtn');
    if ($submitBtn.length) {
        $submitBtn.on('click', async function () {
            const title = $('#dreamTitle').val()?.trim();
            const content = $('#dreamContent').val()?.trim();
            const published = $('#dreamPublished').is(':checked');

            if (!title || !content) {
                alert('제목과 내용을 모두 입력해주세요.');
                return;
            }

            setButtonsDisabled(true, 'submitDreamBtn');
            showLoadingOverlay('create');

            try {
                const dreamRes = await fetch('/api/dream', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ title, content, published }),
                    credentials: 'include'
                });
                if (!dreamRes.ok) {
                    const msg = await dreamRes.text();
                    throw new Error(msg || '등록 실패');
                }

                const modalEl = document.getElementById('createDreamModal');
                if (modalEl && window.bootstrap) {
                    const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
                    modal.hide();
                }
                $('#createDreamForm')[0].reset();
                hideLoadingOverlay('create');
                alert('꿈이 등록되었습니다.');
                setButtonsDisabled(false, 'submitDreamBtn');
                window.location.reload();
            } catch (e) {
                hideLoadingOverlay('create');
                setButtonsDisabled(false, 'submitDreamBtn');
                console.error(e);
                alert('오류가 발생했습니다: ' + (e.message || e));
            }
        });
    }

    // 재분석 (PUT)
    $(document).on('click', '#reAnalyzeBtn', async function () {
        const modalEl = document.getElementById('viewDreamModal');
        const dreamId = modalEl?.dataset?.dreamId;
        if (!dreamId) return;

        setButtonsDisabled(true, 'reAnalyzeBtn', 'editDreamBtn', 'deleteDreamBtn');
        showLoadingOverlay('view');

        try {
            const analysisRes = await fetch(`/api/dream/${dreamId}/analysis`, { 
                method: 'PUT',
                credentials: 'include'
            });
            if (!analysisRes.ok) {
                const msg = await analysisRes.text();
                throw new Error(msg || '분석 실패');
            }

            const getRes = await fetch(`/api/dream/${dreamId}/analysis`, { credentials: 'include' });
            if (!getRes.ok) throw new Error('분석 결과 조회 실패');
            const data = await getRes.json();

            updateModalWithAnalysisData(data);
            hideLoadingOverlay('view');
            alert('꿈 분석이 완료되었습니다.');
            setButtonsDisabled(false, 'reAnalyzeBtn', 'editDreamBtn', 'deleteDreamBtn');
        } catch (e) {
            console.error(e);
            hideLoadingOverlay('view');
            alert('오류가 발생했습니다: ' + (e.message || e));
            setButtonsDisabled(false, 'reAnalyzeBtn', 'editDreamBtn', 'deleteDreamBtn');
        }
    });

    // 검색 탭 전환
    $('.search-tab').on('click', function() {
        $('.search-tab').removeClass('active');
        $(this).addClass('active');
        currentSearchType = $(this).data('search-type');

        const placeholders = {
            'title': '제목을 입력하세요',
            'category': '카테고리를 입력하세요',
            'tag': '태그를 입력하세요'
        };
        $searchInput.attr('placeholder', placeholders[currentSearchType] || '검색어를 입력하세요');
    });

    // 검색 실행
    async function performSearch() {
        const query = $searchInput.val()?.trim();
        if (!query) {
            alert('검색어를 입력해주세요.');
            return;
        }

        currentSearchQuery = query;
        currentPage = 0;

        $dreamsContainer.html('<div class="col-12"><div class="text-center py-5"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div><p class="mt-3 text-muted">검색 중...</p></div></div>');
        $loadMoreContainer.hide();

        try {
            const url = buildSearchUrl(0);
            const res = await fetch(url, { credentials: 'include' });
            if (!res.ok) throw new Error('검색 실패');
            const data = await res.json();

            const dreams = data.content || [];
            const hasNext = calculateHasNext(data);

            console.log(`[검색] 페이지 0 - 받은 데이터: ${dreams.length}개`);
            console.log(`[검색] Page 정보:`, {
                hasNext: data.hasNext,
                last: data.last,
                number: data.number,
                totalPages: data.totalPages,
                totalElements: data.totalElements,
                계산된hasNext: hasNext
            });

            renderDreams(dreams, false);
            toggleLoadMoreButton(hasNext, '검색');
            $resetBtn.removeClass('d-none');
        } catch (e) {
            console.error(e);
            $dreamsContainer.html('<div class="col-12"><div class="search-results-message no-results">검색 중 오류가 발생했습니다.</div></div>');
            $loadMoreContainer.hide();
        }
    }

    // 검색 버튼 클릭
    $('#searchBtn').on('click', performSearch);

    // Enter 키로 검색
    $searchInput.on('keypress', function(e) {
        if (e.which === 13) {
            performSearch();
        }
    });

    // 초기화 버튼
    $resetBtn.on('click', function() {
        $searchInput.val('');
        currentSearchQuery = '';
        currentSearchType = 'title';
        currentPage = 0;
        $(this).addClass('d-none');
        window.location.reload();
    });

    // 카테고리, 태그 클릭으로 검색
    window.searchByCatOrTag = function(name, type){
        $('.search-tab').removeClass('active');
        if(type === 1){ // 카테고리로 검색
            $('.search-tab[data-search-type="category"]').addClass('active');
            currentSearchType = 'category';
        }
        if(type === 2){ // 태그로 검색
            $('.search-tab[data-search-type="tag"]').addClass('active');
            currentSearchType = 'tag';
        }
        $searchInput.val(name);
        performSearch();
    }
});
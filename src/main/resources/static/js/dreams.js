$(document).ready(function() {

    // Navbar shrink function
    function navbarShrink() {
        const $navbarCollapsible = $('#mainNav');
        if ($navbarCollapsible.length === 0) return;

        if ($(window).scrollTop() === 0) {
            $navbarCollapsible.removeClass('navbar-shrink');
        } else {
            $navbarCollapsible.addClass('navbar-shrink');
        }
    }

    // Shrink navbar on load
    navbarShrink();

    // Shrink navbar on scroll
    $(document).on('scroll', navbarShrink);

    // Activate Bootstrap scrollspy
    const $mainNav = $('#mainNav');
    if ($mainNav.length) {
        $('body').scrollspy({
            target: '#mainNav',
            offset: 80 // rootMargin 대체 (대략 -40% 효과)
        });
    }

    // Collapse responsive navbar when toggler is visible
    const $navbarToggler = $('.navbar-toggler');
    const $responsiveNavItems = $('#navbarResponsive .nav-link');

    $responsiveNavItems.on('click', function() {
        if ($navbarToggler.is(':visible')) {
            $navbarToggler.trigger('click');
        }
    });

    // 삭제
    $(document).on('click', '#deleteDreamBtn', async function () {
        const modalEl = document.getElementById('viewDreamModal');
        const dreamId = modalEl?.dataset?.dreamId;
        if (!dreamId) return;

        if (!confirm('정말 삭제하시겠습니까?')) return;
        try {
            const res = await fetch(`/api/dream/${dreamId}`, { method: 'DELETE' });
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
        // 기본 a 링크 동작 방지 (상단으로 스크롤되는 것 방지)
        e.preventDefault();
        e.stopPropagation();
        // 등록 버튼과 혼동 방지: 모달 내 등록 버튼에는 id가 있음
        if (this.id === 'submitDreamBtn') return;

        const $card = $(this).closest('.dream-card');
        const dreamId = $card.attr('data-dream-id');
        if (!dreamId) return;

        try {
            const res = await fetch(`/api/dream/${dreamId}/analysis`);
            if (!res.ok) throw new Error('조회 실패');
            const data = await res.json();

            $('#detailTitle').text(data.dreamTitle || '');
            $('#detailContent').text(data.dreamContent || '');

            if (data.scenes && data.scenes.length > 0) {
                const html = data.scenes
                    .map(item => `
                        <div class="card h-100 dream-card" style="margin: 8px;">
                            <div class="card-body" style="padding: 16px;">
                                <h5 class="card-title">${item.content}</h5>
                                <p class="card-text">= ${item.emotion}</p>
                                <p class="card-text">${item.interpretation}</p>
                            </div>
                        </div>
                    `)
                    .join('');
                $('#detailScenes').html(`${html}`);
            } else {
                $('#detailScenes').empty();
            }

            $('#detailInsight').text(data.insight || '');
            $('#detailSuggestion').text(data.suggestion || '');

            if (data.categories && data.categories.length > 0) {
                const html = JSON.parse(data.categories)
                    .map(cat => `<div class="col category-tag">${cat}</div>`)
                    .join('');
                $('#detailCategories').html(`${html}`);
            } else {
                $('#detailCategories').empty();
            }

            if (data.tags && data.tags.length > 0) {
                const html = JSON.parse(data.tags)
                    .map(tag => `<div class="col category-tag">${tag}</div>`)
                    .join('');
                $('#detailTags').html(`${html}`);
            } else {
                $('#detailTags').empty();
            }

            $('#detailEmotion').text('-점');
            $('#detailPublished').text((data.published ? '공개' : '비공개'));

            // 편집용 값 세팅
            $('#editTitle').val(data.title || '');
            $('#editContent').val(data.content || '');
            $('#editPublished').prop('checked', !!data.published);

            // 현재 dreamId 보관 및 모달 표시
            const modalEl = document.getElementById('viewDreamModal');
            if (modalEl) modalEl.dataset.dreamId = String(data.id);
            if (modalEl && window.bootstrap) {
                const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
                modal.show();
            }
        } catch (err) {
            console.error(err);
            alert('상세 조회 중 오류가 발생했습니다.');
        }
    });

    // 수정 모드 토글
    $(document).on('click', '#editDreamBtn', function () {
        // 보기 -> 편집 전환 (수정모달 형태)
        $('#viewDreamModalLabel').text('꿈 수정');
        $('#detailTitle, #detailContent, #detailPublished').addClass('d-none');
        $('#editTitle, #editContent').removeClass('d-none');
        $('#editPublishedWrap').removeClass('d-none');
        $('#detailScenesWrap, #detailInsightWrap, #detailSuggestion, #detailCategoriesWrap, #detailTagsWrap, #detailEmotionWrap').addClass('d-none');
        $('#editDreamBtn').addClass('d-none');
        $('#saveDreamBtn, #cancelEditBtn').removeClass('d-none');
    });

    // 저장 (PUT)
    $(document).on('click', '#saveDreamBtn', async function () {
        const modalEl = document.getElementById('viewDreamModal');
        const dreamId = modalEl?.dataset?.dreamId;
        if (!dreamId) return;

        const title = $('#editTitle').val()?.toString().trim();
        const content = $('#editContent').val()?.toString().trim();
        const published = $('#editPublished').is(':checked');

        if (!title || !content) {
            alert('제목과 내용을 모두 입력해주세요.');
            return;
        }

        try {
            const res = await fetch(`/api/dream/${dreamId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title, content, published })
            });
            if (!res.ok) throw new Error('수정 실패');
            let updated;
            try {
                updated = await res.json();
            } catch (_) {
                updated = { title, content, published };
            }

            // 모달 내용 업데이트
            $('#detailTitle').text(updated.title ?? title);
            $('#detailContent').text(updated.content ?? content);
            $('#detailPublished').text((updated.published ?? published) ? '공개' : '비공개');

            // 카드 목록의 해당 항목도 즉시 반영
            const $card = $(`.dream-card[data-dream-id='${dreamId}']`);
            $card.find('.card-title').text(updated.title ?? title);
            $card.find('.card-text').text(updated.content ?? content);

            // 편집 -> 보기 전환 (모달은 유지)
            $('#viewDreamModalLabel').text('꿈 상세');
            $('#detailTitle, #detailContent, #detailPublished').removeClass('d-none');
            $('#editTitle, #editContent').addClass('d-none');
            $('#editPublishedWrap').addClass('d-none');
            $('#detailScenesWrap, #detailInsightWrap, #detailSuggestion, #detailCategoriesWrap, #detailTagsWrap, #detailEmotionWrap').removeClass('d-none');
            $('#saveDreamBtn, #cancelEditBtn').addClass('d-none');
            $('#editDreamBtn').removeClass('d-none');

            alert('수정되었습니다.');
        } catch (e) {
            console.error(e);
            alert('오류가 발생했습니다: ' + (e.message || e));
        }
    });

    // 편집 취소 (보기 모드 복귀)
    $(document).on('click', '#cancelEditBtn', function () {
        $('#viewDreamModalLabel').text('꿈 상세');
        $('#detailTitle, #detailContent, #detailPublished').removeClass('d-none');
        $('#editTitle, #editContent').addClass('d-none');
        $('#editPublishedWrap').addClass('d-none');
        $('#detailScenesWrap, #detailInsightWrap, #detailSuggestion, #detailCategoriesWrap, #detailTagsWrap, #detailEmotionWrap').removeClass('d-none');
        $('#saveDreamBtn, #cancelEditBtn').addClass('d-none');
        $('#editDreamBtn').removeClass('d-none');
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

            try {
                const res = await fetch('/api/dream', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ title, content, published })
                });

                if (!res.ok) {
                    const msg = await res.text();
                    throw new Error(msg || '등록 실패');
                }

                // 성공 후 UI 처리 (Bootstrap 5 Modal API)
                const modalEl = document.getElementById('createDreamModal');
                if (modalEl && window.bootstrap) {
                    const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
                    modal.hide();
                }
                $('#createDreamForm')[0].reset();
                alert('꿈이 등록되었습니다.');
                window.location.reload();
            } catch (e) {
                console.error(e);
                alert('오류가 발생했습니다: ' + (e.message || e));
            }
        });
    }

    // 검색 기능
    let currentSearchType = 'title';
    let currentSearchQuery = '';

    // 검색 탭 전환
    $('.search-tab').on('click', function() {
        $('.search-tab').removeClass('active');
        $(this).addClass('active');
        currentSearchType = $(this).data('search-type');
        
        // 플레이스홀더 변경
        const placeholders = {
            'title': '제목을 입력하세요',
            'category': '카테고리를 입력하세요',
            'tag': '태그를 입력하세요'
        };
        $('#searchInput').attr('placeholder', placeholders[currentSearchType] || '검색어를 입력하세요');
    });

    // 검색 실행
    async function performSearch() {
        const query = $('#searchInput').val()?.trim();
        if (!query) {
            alert('검색어를 입력해주세요.');
            return;
        }

        currentSearchQuery = query;
        const $container = $('#dreamsContainer');
        
        // 로딩 표시
        $container.html('<div class="col-12"><div class="text-center py-5"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div><p class="mt-3 text-muted">검색 중...</p></div></div>');

        try {
            let url = '';
            if (currentSearchType === 'title') {
                url = `/api/dream/title?q=${encodeURIComponent(query)}`;
            } else if (currentSearchType === 'category') {
                url = `/api/dream/category/${encodeURIComponent(query)}`;
            } else if (currentSearchType === 'tag') {
                url = `/api/dream/tag/${encodeURIComponent(query)}`;
            }

            const res = await fetch(url);
            if (!res.ok) throw new Error('검색 실패');
            const dreams = await res.json();

            // 결과 표시
            if (dreams.length === 0) {
                $container.html(`<div class="col-12"><div class="search-results-message no-results">검색 결과가 없습니다.</div></div>`);
            } else {
                let html = '';
                dreams.forEach(dream => {
                    let categoriesHtml = '';
                    if (dream.categories && dream.categories.length > 0) {
                        categoriesHtml = '<div class="dream-meta mb-2">';
                        categoriesHtml += '<div class="dream-categories">';
                        dream.categories.forEach(category => {
                            categoriesHtml += `<span class="category-badge">${escapeHtml(category)}</span>`;
                        });
                        categoriesHtml += '</div></div>';
                    }
                    
                    let tagsHtml = '';
                    if (dream.tags && dream.tags.length > 0) {
                        tagsHtml = '<div class="dream-tags mt-3">';
                        dream.tags.forEach(tag => {
                            tagsHtml += `<button type="button" class="tag-btn">#${escapeHtml(tag)}</button>`;
                        });
                        tagsHtml += '</div>';
                    }
                    
                    html += `
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
                });
                $container.html(html);
            }

            // 초기화 버튼 표시
            $('#resetBtn').removeClass('d-none');
        } catch (e) {
            console.error(e);
            $container.html(`<div class="col-12"><div class="search-results-message no-results">검색 중 오류가 발생했습니다.</div></div>`);
        }
    }

    // 검색 버튼 클릭
    $('#searchBtn').on('click', performSearch);

    // Enter 키로 검색
    $('#searchInput').on('keypress', function(e) {
        if (e.which === 13) {
            performSearch();
        }
    });

    // 초기화 버튼
    $('#resetBtn').on('click', function() {
        $('#searchInput').val('');
        currentSearchQuery = '';
        $(this).addClass('d-none');
        window.location.reload();
    });

    // HTML 이스케이프 함수
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});
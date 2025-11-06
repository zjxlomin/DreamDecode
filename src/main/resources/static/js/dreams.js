$(document).ready(function() {

    // 페이지네이션 변수
    let currentPage = 0;
    let currentSearchType = 'title'; // 기본값: 제목 검색
    let currentSearchQuery = '';
    let isLoading = false;

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

    // 더보기 버튼 클릭
    $(document).on('click', '#loadMoreBtn', async function() {
        if (isLoading) return;

        isLoading = true;
        const $btn = $(this);
        const originalText = $btn.html();
        $btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-2"></span>로딩 중...');

        try {
            currentPage++;
            let url = '';

            if (currentSearchQuery) {
                // 검색 중인 경우
                if (currentSearchType === 'title') {
                    url = `/api/dream/title?q=${encodeURIComponent(currentSearchQuery)}&page=${currentPage}`;
                } else if (currentSearchType === 'category') {
                    url = `/api/dream/category/${encodeURIComponent(currentSearchQuery)}?page=${currentPage}`;
                } else if (currentSearchType === 'tag') {
                    url = `/api/dream/tag/${encodeURIComponent(currentSearchQuery)}?page=${currentPage}`;
                }
            } else {
                // 전체 조회
                url = `/api/dream?page=${currentPage}`;
            }

            const res = await fetch(url);
            if (!res.ok) throw new Error('로딩 실패');
            const data = await res.json();

            // Page 객체에서 content 추출
            const dreams = data.content || [];
            
            // Spring Data JPA Page 객체의 필드 확인
            // hasNext가 없으면 last 필드나 totalPages를 사용하여 계산
            let hasNext = false;
            if (data.hasNext !== undefined) {
                hasNext = data.hasNext;
            } else if (data.last !== undefined) {
                hasNext = !data.last; // last가 false면 다음 페이지가 있음
            } else if (data.totalPages !== undefined && data.number !== undefined) {
                hasNext = data.number < data.totalPages - 1; // 현재 페이지가 마지막 페이지보다 작으면 다음 페이지 있음
            }
            
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
                $('#loadMoreContainer').hide();
                $btn.prop('disabled', false).html(originalText);
                return;
            }

            // 새로운 카드 추가
            const $container = $('#dreamsContainer');
            
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

                const cardHtml = `
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
                $container.append(cardHtml);
            });

            // 더보기 버튼 표시/숨김
            if (hasNext) {
                $('#loadMoreContainer').show();
                console.log(`[더보기] 다음 페이지가 있어 더보기 버튼 표시`);
            } else {
                $('#loadMoreContainer').hide();
                console.log(`[더보기] 마지막 페이지 도달 - 더보기 버튼 숨김`);
            }

            $btn.prop('disabled', false).html(originalText);
        } catch (e) {
            console.error(e);
            alert('더보기 로딩 중 오류가 발생했습니다.');
            $btn.prop('disabled', false).html(originalText);
            currentPage--; // 페이지 롤백
        } finally {
            isLoading = false;
        }
    });

    // JWT 토큰에서 userId 추출
    function getUserIdFromToken() {
        try {
            const token = localStorage.getItem('dd_at');
            if (!token) return null;
            
            // JWT는 header.payload.signature 형식
            const payload = token.split('.')[1];
            if (!payload) return null;
            
            // base64 디코딩 (JWT는 base64url이지만 atob도 대부분 작동)
            const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
            // JWT의 sub 필드에 userId가 저장되어 있음
            return decoded.sub ? parseInt(decoded.sub) : null;
        } catch (e) {
            console.error('토큰 파싱 실패:', e);
            return null;
        }
    }

    // 모달 내용 업데이트 함수 (공통)
    function updateModalWithAnalysisData(data) {
        $('#detailTitle').text(data.dreamTitle || '');
        $('#detailContent').text(data.dreamContent || '');

        if (data.scenes && data.scenes.length > 0) {
            const html = data.scenes
                .map(item => `
                    <div class="card h-100 dream-card" style="margin: 8px;">
                        <div class="card-body" style="padding: 16px;">
                            <h5 class="card-title">${escapeHtml(item.content)}</h5>
                            <p class="card-text">= ${escapeHtml(item.emotion)}</p>
                            <p class="card-text">${escapeHtml(item.interpretation)}</p>
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

        // categories 처리: 문자열이면 배열로 변환
        let categoriesArray = [];
        if (data.categories) {
            if (Array.isArray(data.categories)) {
                categoriesArray = data.categories;
            } else if (typeof data.categories === 'string') {
                try {
                    // JSON 배열 문자열인 경우
                    categoriesArray = JSON.parse(data.categories);
                } catch (e) {
                    // 쉼표로 구분된 문자열인 경우
                    categoriesArray = data.categories.split(',').map(cat => cat.trim()).filter(cat => cat.length > 0);
                }
            }
        }
        
        if (categoriesArray.length > 0) {
            const html = categoriesArray
                .map(cat => `
                    <span class="col category-badge" style="max-width: max-content; margin: 4px;">
                        ${escapeHtml(cat)}
                    </span>
                `)
                .join('');
            $('#detailCategories').html(`${html}`);
        } else {
            $('#detailCategories').empty();
        }

        // tags 처리: 문자열이면 배열로 변환
        let tagsArray = [];
        if (data.tags) {
            if (Array.isArray(data.tags)) {
                tagsArray = data.tags;
            } else if (typeof data.tags === 'string') {
                try {
                    // JSON 배열 문자열인 경우
                    tagsArray = JSON.parse(data.tags);
                } catch (e) {
                    // 쉼표로 구분된 문자열인 경우
                    tagsArray = data.tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
                }
            }
        }
        
        if (tagsArray.length > 0) {
            const html = tagsArray
                .map(tag => `
                    <button type="button" class="col tag-btn" style="max-width: max-content; margin: 4px;">
                        #${escapeHtml(tag)}
                    </button>
                `)
                .join('');
            $('#detailTags').html(`${html}`);
        } else {
            $('#detailTags').empty();
        }

        // 감정점수를 소수점 2번째 자리까지 반올림하여 표시
        const sentimentRounded = typeof data.sentiment === 'number' ? data.sentiment.toFixed(2) : '-';
        $('#detailEmotion').text(sentimentRounded + '점');
        
        // 감정 강도(magnitude)를 소수점 2번째 자리까지 반올림하여 표시
        const magnitudeRounded = typeof data.magnitude === 'number' ? data.magnitude.toFixed(2) : '-';
        $('#detailMagnitude').text(magnitudeRounded + '점');
        
        $('#detailPublished').text((data.dreamPublished ? '공개' : '비공개'));

        // 편집용 값 세팅
        $('#editTitle').val(data.dreamTitle || '');
        $('#editContent').val(data.dreamContent || '');
        $('#editPublished').prop('checked', !!data.dreamPublished);

        // dreamContent 업데이트
        const modalEl = document.getElementById('viewDreamModal');
        if (modalEl) {
            modalEl.dataset.dreamId = String(data.dreamId);
            modalEl.dataset.dreamContent = String(data.dreamContent);
        }

        // 작성자와 현재 로그인 사용자 비교하여 버튼 표시/숨김 처리
        const currentUserId = getUserIdFromToken();
        const dreamUserId = data.userId ? parseInt(data.userId) : null;
        
        // 디버깅: 콘솔에 값 출력
        console.log('=== 버튼 표시 확인 ===');
        console.log('currentUserId (JWT sub):', currentUserId, typeof currentUserId);
        console.log('dreamUserId (data.userId):', dreamUserId, typeof dreamUserId);
        console.log('data.userId (원본):', data.userId);
        
        const isOwner = currentUserId !== null && dreamUserId !== null && currentUserId === dreamUserId;
        console.log('isOwner:', isOwner);

        if (isOwner) {
            // 작성자인 경우 버튼 표시
            $('#deleteDreamBtn, #editDreamBtn, #reAnalyzeBtn').removeClass('d-none');
            console.log('버튼 표시됨');
        } else {
            // 작성자가 아닌 경우 버튼 숨김
            $('#deleteDreamBtn, #editDreamBtn, #reAnalyzeBtn').addClass('d-none');
            console.log('버튼 숨김됨');
        }
    }

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

            // 공통 업데이트 함수 사용
            updateModalWithAnalysisData(data);

            // 모달 표시
            const modalEl = document.getElementById('viewDreamModal');
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
        $('#detailScenesWrap, #detailInsightWrap, #detailSuggestionWrap, #detailCategoriesWrap, #detailTagsWrap, #detailEmotionWrap, #detailMagnitudeWrap').addClass('d-none');
        $('#reAnalyzeBtn').addClass('d-none');
        $('#editDreamBtn').addClass('d-none');
        $('#saveDreamBtn, #cancelEditBtn').removeClass('d-none');
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

                 if(content !== prevContent) {
             document.getElementById("saveDreamBtn").disabled = true;
             document.getElementById("deleteDreamBtn").disabled = true;
             // 로딩 오버레이 표시
             $('#viewDreamModalLoading').removeClass('d-none').addClass('d-flex');
         }

        try {
            const dreamRes = await fetch(`/api/dream/${dreamId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title, content, published })
            });
            if(!dreamRes.ok) throw new Error('수정 실패');
            
            // 내용이 변경되어 재분석이 수행되었을 수 있으므로 최신 분석 결과를 가져와서 모달 업데이트
            const analysisRes = await fetch(`/api/dream/${dreamId}/analysis`);
            if (!analysisRes.ok) throw new Error('분석 결과 조회 실패');
            const data = await analysisRes.json();

            // 공통 업데이트 함수 사용
            updateModalWithAnalysisData(data);

            // 카드 목록의 해당 항목도 즉시 반영
            const $card = $(`.dream-card[data-dream-id='${dreamId}']`);
            $card.find('.card-title').text(data.dreamTitle || title);
            $card.find('.card-text').text(data.dreamContent || content);

            // 편집 -> 보기 전환 (모달은 유지)
            $('#viewDreamModalLabel').text('꿈 상세');
            $('#detailTitle, #detailContent, #detailPublished').removeClass('d-none');
            $('#editTitle, #editContent').addClass('d-none');
            $('#editPublishedWrap').addClass('d-none');
            $('#detailScenesWrap, #detailInsightWrap, #detailSuggestionWrap, #detailCategoriesWrap, #detailTagsWrap, #detailEmotionWrap, #detailMagnitudeWrap').removeClass('d-none');
            $('#saveDreamBtn, #cancelEditBtn').addClass('d-none');
            $('#reAnalyzeBtn').removeClass('d-none');
            $('#editDreamBtn').removeClass('d-none');

                         // 로딩 오버레이 숨김
             $('#viewDreamModalLoading').removeClass('d-flex').addClass('d-none');
             
             alert('수정되었습니다.');
             document.getElementById("saveDreamBtn").disabled = false;
             document.getElementById("deleteDreamBtn").disabled = false;
         } catch (e) {
             console.error(e);
             // 로딩 오버레이 숨김
             $('#viewDreamModalLoading').removeClass('d-flex').addClass('d-none');
             alert('오류가 발생했습니다: ' + (e.message || e));
             document.getElementById("saveDreamBtn").disabled = false;
             document.getElementById("deleteDreamBtn").disabled = false;
         }
    });

    // 편집 취소 (보기 모드 복귀)
    $(document).on('click', '#cancelEditBtn', function () {
        $('#viewDreamModalLabel').text('꿈 상세');
        $('#detailTitle, #detailContent, #detailPublished').removeClass('d-none');
        $('#editTitle, #editContent').addClass('d-none');
        $('#editPublishedWrap').addClass('d-none');
        $('#detailScenesWrap, #detailInsightWrap, #detailSuggestionWrap, #detailCategoriesWrap, #detailTagsWrap, #detailEmotionWrap, #detailMagnitudeWrap').removeClass('d-none');
        $('#saveDreamBtn, #cancelEditBtn').addClass('d-none');
        
        // 버튼 표시 상태는 updateModalWithAnalysisData에서 이미 설정되어 있으므로
        // 편집 취소 시에도 다시 확인 (모달의 dataset에서 userId 가져오기)
        const modalEl = document.getElementById('viewDreamModal');
        if (modalEl && modalEl.dataset.dreamId) {
            // 모달이 이미 열려있고 데이터가 있는 경우, 버튼 표시 상태 재확인
            // updateModalWithAnalysisData에서 이미 처리했으므로 여기서는 그대로 유지
        } else {
            // 데이터가 없는 경우 기본적으로 버튼 표시 (하위 호환성)
            $('#reAnalyzeBtn, #editDreamBtn, #deleteDreamBtn').removeClass('d-none');
        }
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

                         document.getElementById("submitDreamBtn").disabled = true;
             // 로딩 오버레이 표시
             $('#createDreamModalLoading').removeClass('d-none').addClass('d-flex');

             try {
                const dreamRes = await fetch('/api/dream', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ title, content, published })
                });
                if (!dreamRes.ok) {
                    const msg = await dreamRes.text();
                    throw new Error(msg || '등록 실패');
                }

                // 성공 후 UI 처리 (Bootstrap 5 Modal API)
                const modalEl = document.getElementById('createDreamModal');
                if (modalEl && window.bootstrap) {
                    const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
                    modal.hide();
                }
                                 $('#createDreamForm')[0].reset();
                 // 로딩 오버레이 숨김
                 $('#createDreamModalLoading').removeClass('d-flex').addClass('d-none');
                 
                 alert('꿈이 등록되었습니다.');
                 document.getElementById("submitDreamBtn").disabled = false;
                 window.location.reload();
             } catch (e) {
                 // 로딩 오버레이 숨김
                 $('#createDreamModalLoading').removeClass('d-flex').addClass('d-none');
                 document.getElementById("submitDreamBtn").disabled = false;
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

        try {
                         document.getElementById("reAnalyzeBtn").disabled = true;
             document.getElementById("editDreamBtn").disabled = true;
             document.getElementById("deleteDreamBtn").disabled = true;
             // 로딩 오버레이 표시
             $('#viewDreamModalLoading').removeClass('d-none').addClass('d-flex');
             
             const analysisRes = await fetch(`/api/dream/${dreamId}/analysis`, { method: 'PUT'});
            if(!analysisRes.ok){
                const msg = await analysisRes.text();
                throw new Error(msg || '분석 실패');
            }
            
            // 분석 완료 후 최신 분석 결과를 다시 가져와서 모달 업데이트
            const getRes = await fetch(`/api/dream/${dreamId}/analysis`);
            if (!getRes.ok) throw new Error('분석 결과 조회 실패');
            const data = await getRes.json();

            // 공통 업데이트 함수 사용
             updateModalWithAnalysisData(data);

             // 로딩 오버레이 숨김
             $('#viewDreamModalLoading').removeClass('d-flex').addClass('d-none');
             
             alert('꿈 분석이 완료되었습니다.');
             document.getElementById("reAnalyzeBtn").disabled = false;
             document.getElementById("editDreamBtn").disabled = false;
             document.getElementById("deleteDreamBtn").disabled = false;
         } catch (e) {
             console.error(e);
             // 로딩 오버레이 숨김
             $('#viewDreamModalLoading').removeClass('d-flex').addClass('d-none');
             alert('오류가 발생했습니다: ' + (e.message || e));
             document.getElementById("reAnalyzeBtn").disabled = false;
             document.getElementById("editDreamBtn").disabled = false;
             document.getElementById("deleteDreamBtn").disabled = false;
         }
    });

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
        currentPage = 0; // 검색 시 페이지 초기화
        const $container = $('#dreamsContainer');
        
        // 로딩 표시
        $container.html('<div class="col-12"><div class="text-center py-5"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div><p class="mt-3 text-muted">검색 중...</p></div></div>');
        $('#loadMoreContainer').hide();

        try {
            let url = '';
            if (currentSearchType === 'title') {
                url = `/api/dream/title?q=${encodeURIComponent(query)}&page=0`;
            } else if (currentSearchType === 'category') {
                url = `/api/dream/category/${encodeURIComponent(query)}?page=0`;
            } else if (currentSearchType === 'tag') {
                url = `/api/dream/tag/${encodeURIComponent(query)}?page=0`;
            }

            const res = await fetch(url);
            if (!res.ok) throw new Error('검색 실패');
            const data = await res.json();

            // Page 객체에서 content 추출
            const dreams = data.content || [];
            
            // Spring Data JPA Page 객체의 필드 확인
            // hasNext가 없으면 last 필드나 totalPages를 사용하여 계산
            let hasNext = false;
            if (data.hasNext !== undefined) {
                hasNext = data.hasNext;
            } else if (data.last !== undefined) {
                hasNext = !data.last; // last가 false면 다음 페이지가 있음
            } else if (data.totalPages !== undefined && data.number !== undefined) {
                hasNext = data.number < data.totalPages - 1; // 현재 페이지가 마지막 페이지보다 작으면 다음 페이지 있음
            }
            
            console.log(`[검색] 페이지 0 - 받은 데이터: ${dreams.length}개`);
            console.log(`[검색] Page 정보:`, {
                hasNext: data.hasNext,
                last: data.last,
                number: data.number,
                totalPages: data.totalPages,
                totalElements: data.totalElements,
                계산된hasNext: hasNext
            });

            // 결과 표시
            if (dreams.length === 0) {
                $container.html(`<div class="col-12"><div class="search-results-message no-results">검색 결과가 없습니다.</div></div>`);
                $('#loadMoreContainer').hide();
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

                // 더보기 버튼 표시/숨김
                if (hasNext) {
                    $('#loadMoreContainer').show();
                } else {
                    $('#loadMoreContainer').hide();
                }
            }

            // 초기화 버튼 표시
            $('#resetBtn').removeClass('d-none');
        } catch (e) {
            console.error(e);
            $container.html(`<div class="col-12"><div class="search-results-message no-results">검색 중 오류가 발생했습니다.</div></div>`);
            $('#loadMoreContainer').hide();
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
        currentSearchType = 'title';
        currentPage = 0;
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
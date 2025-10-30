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
            const res = await fetch(`/api/dream/${dreamId}`);
            if (!res.ok) throw new Error('조회 실패');
            const data = await res.json();

            $('#detailTitle').text(data.title || '');
            $('#detailContent').text(data.content || '');
            $('#detailAnalysis').text('분석 준비중');
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
        $('#detailAnalysisWrap, #detailEmotionWrap').addClass('d-none');
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

            alert('수정되었습니다.');
            // 성공 시 즉시 보기 모드 업데이트
            $('#detailTitle').text(title);
            $('#detailContent').text(content);
            $('#detailPublished').text(published ? '공개' : '비공개');

            // 편집 -> 보기 전환
            $('#viewDreamModalLabel').text('꿈 상세');
            $('#detailTitle, #detailContent, #detailPublished').removeClass('d-none');
            $('#editTitle, #editContent').addClass('d-none');
            $('#editPublishedWrap').addClass('d-none');
            $('#detailAnalysisWrap, #detailEmotionWrap').removeClass('d-none');
            $('#saveDreamBtn, #cancelEditBtn').addClass('d-none');
            $('#editDreamBtn').removeClass('d-none');

            // 목록도 반영되도록 새로고침
            window.location.reload();
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
        $('#detailAnalysisWrap, #detailEmotionWrap').removeClass('d-none');
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
});
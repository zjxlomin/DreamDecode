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

    // 카드 '자세히 보기' 버튼 → 상세 모달 표시
    $(document).on('click', '.dream-card .btn.btn-primary', function (e) {
        // 등록 버튼과 혼동 방지: 모달 내 등록 버튼에는 id가 있음
        if (this.id === 'submitDreamBtn') return;

        const $card = $(this).closest('.dream-card');
        const title = $card.find('.card-title').text().trim();
        const content = $card.find('.card-text').text().trim();

        $('#detailTitle').text(title || '');
        $('#detailContent').text(content || '');
        $('#detailEmotion').text('-점');
        // 분석/감정점수는 서버 연동 전까지 기본값 유지
        // 추후 API 연동 시 여기서 fetch로 보강 가능

        const modalEl = document.getElementById('viewDreamModal');
        if (modalEl && window.bootstrap) {
            const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
            modal.show();
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
// 공통 JavaScript 파일
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
            offset: 80
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

});


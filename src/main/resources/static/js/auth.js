// ===== 전역 상수 / 함수 (다른 페이지에서도 사용 가능) =====
const AT_KEY = 'dd_at';
let lastSignupEmail = null;
let pwResetVerified = false;

// 비밀번호 규칙: 8~20자, 영문 + 숫자
function validatePasswordRules(pw) {
    if (!pw) return '비밀번호를 입력해 주세요.';
    if (pw.length < 8 || pw.length > 20) {
        return '비밀번호는 8~20자여야 합니다.';
    }
    if (!/[A-Za-z]/.test(pw) || !/[0-9]/.test(pw)) {
        return '비밀번호는 영문과 숫자를 모두 포함해야 합니다.';
    }
    return '';
}

function getAccessToken() {
    return localStorage.getItem(AT_KEY);
}

function setAccessToken(token) {
    if (token) {
        localStorage.setItem(AT_KEY, token);
    } else {
        localStorage.removeItem(AT_KEY);
    }
}

function updateAuthUI() {
    const loggedIn = !!getAccessToken();
    if (loggedIn) {
        $('#navLogin, #navSignup').addClass('d-none');
        $('#navMyPage, #navLogout').removeClass('d-none');
    } else {
        $('#navLogin, #navSignup').removeClass('d-none');
        $('#navMyPage, #navLogout').addClass('d-none');
    }
}

// 다른 JS 파일/페이지에서도 쓸 수 있게 전역으로 노출
window.AT_KEY = AT_KEY;
window.getAccessToken = getAccessToken;
window.setAccessToken = setAccessToken;
window.updateAuthUI = updateAuthUI;
window.validatePasswordRules = validatePasswordRules;

// ===== DOM 로드 후 실행되는 부분 =====
$(function () {

    // ===== ① 모든 AJAX 요청에 AT 자동 첨부 =====
    $.ajaxSetup({
        beforeSend: function (xhr, settings) {
            const token = getAccessToken();

            // 로그인/회원가입 같은 공개 API에는 굳이 붙일 필요는 없지만
            // 있어도 문제는 없어서 간단하게 전부 붙여도 됨.
            if (token) {
                xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            }
        }
        // error는 여기 두지 않고 아래 ajaxError 전역 핸들러에서 처리
    });

    $(document).ajaxError(function (event, xhr, settings, thrownError) {
        const status = xhr.status;

        // 401(Unauthorized) + 403(Forbidden) 모두 로그인 만료로 취급
        if (status !== 401 && status !== 403) return;

        const url = (settings && settings.url) || '';

        // 로그인/회원가입/이메일 관련 에러는 세션 만료가 아닐 수 있으니 제외
        if (
            url.startsWith('/api/auth/login') ||
            url.startsWith('/api/auth/refresh') ||
            url.startsWith('/api/users/signup') ||
            url.startsWith('/api/email/')
        ) {
            return;
        }

        console.warn(status + ' 응답 감지 → 토큰 만료 또는 인증 실패. 자동 로그아웃 처리.', url);

        setAccessToken(null);
        updateAuthUI();

        alert('로그인 시간이 만료되었습니다. 다시 로그인해 주세요.');
    });


    // ===== 공통: 회원가입 생년월일 max 오늘 =====
    const today = new Date().toISOString().split("T")[0];
    $('#signupBirthday').attr('max', today);

    /* ===== 회원가입 ===== */
    $('#signupForm').on('submit', function (e) {
        e.preventDefault();

        const name = $('#signupName').val().trim();
        const email = $('#signupEmail').val().trim();
        const password = $('#signupPassword').val();
        const passwordConfirm = $('#signupPasswordConfirm').val();
        const gender = $('input[name="signupGender"]:checked').val();
        const birthday = $('#signupBirthday').val();

        $('#signupPassword').removeClass('is-invalid');
        $('#signupPasswordConfirm').removeClass('is-invalid');
        $('#signupPasswordError').addClass('d-none').text('');
        $('#signupPasswordConfirmError').addClass('d-none').text('');

        if (!name || !email || !password || !birthday) {
            alert('모든 필수 항목을 입력해 주세요.');
            return;
        }

        const ruleMsg = validatePasswordRules(password);
        if (ruleMsg) {
            $('#signupPassword').addClass('is-invalid');
            $('#signupPasswordError').removeClass('d-none').text(ruleMsg);
            $('#signupPassword').focus();
            return;
        }

        if (password !== passwordConfirm) {
            $('#signupPasswordConfirm').addClass('is-invalid');
            $('#signupPasswordConfirmError')
                .removeClass('d-none')
                .text('비밀번호와 비밀번호 확인이 일치하지 않습니다.');
            $('#signupPasswordConfirm').focus();
            return;
        }

        $.ajax({
            url: '/api/users/signup',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                name: name,
                email: email,
                password: password,
                gender: gender,
                birthday: birthday
            }),
            success: function () {
                lastSignupEmail = email;

                $('#signupForm')[0].reset();
                $('#signupPassword').removeClass('is-invalid');
                $('#signupPasswordConfirm').removeClass('is-invalid');
                $('#signupModal').modal('hide');

                $('#verifyEmail').val(email);
                $('#verifyCode').val('');
                $('#emailVerifyModal').modal('show');

                alert('회원가입이 완료되었습니다.\n이메일 인증을 위해 "인증메일 보내기" 버튼을 눌러 주세요.');
            },
            error: function (xhr) {
                const msg = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message
                    : '회원가입에 실패했습니다. 입력값을 다시 확인해 주세요.';
                alert(msg);
            }
        });
    });

    /* ===== 이메일 인증 (회원가입) ===== */
    $('#emailVerifyForm').on('submit', function (e) {
        e.preventDefault();

        const email = $('#verifyEmail').val();
        const code = $('#verifyCode').val().trim();

        if (!code) {
            alert('인증번호를 입력해 주세요.');
            return;
        }

        $.ajax({
            url: '/api/email/verify-signup',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email, code: code }),
            success: function () {
                alert('이메일 인증이 완료되었습니다.\n이제 로그인할 수 있습니다.');

                $('#emailVerifyModal').modal('hide');

                $('#loginEmail').val(email);
                $('#loginPassword').val('');
                $('#loginModal').modal('show');
            },
            error: function () {
                alert('인증에 실패했습니다.\n코드가 올바른지 / 유효시간이 지났는지 확인해 주세요.');
            }
        });
    });

    $('#resendVerifyMailBtn').on('click', function () {
        const email =
            $('#verifyEmail').val()
            || lastSignupEmail
            || $('#signupEmail').val();

        if (!email) {
            alert('이메일 정보가 없습니다. 다시 회원가입을 진행해 주세요.');
            return;
        }

        $.ajax({
            url: '/api/email/resend-signup',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email }),
            success: function (res) {
                const msg = res && res.message
                    ? res.message
                    : '인증 메일을 보냈습니다.\n메일함을 확인해 주세요.';
                alert(msg);
            },
            error: function () {
                alert('인증 메일 발송에 실패했습니다.\n잠시 후 다시 시도해 주세요.');
            }
        });
    });

    /* ===== 로그인 ===== */
    $('#loginForm').on('submit', function (e) {
        e.preventDefault();

        const email = $('#loginEmail').val().trim();
        const password = $('#loginPassword').val();

        if (!email || !password) {
            alert('이메일과 비밀번호를 입력해 주세요.');
            return;
        }

        $.ajax({
            url: '/api/auth/login',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email, password: password }),
            success: function (res) {
                if (res && res.accessToken) {
                    setAccessToken(res.accessToken);
                    updateAuthUI();
                    alert('로그인에 성공했습니다.');
                    $('#loginModal').modal('hide');
                    $('#loginPassword').val('');
                } else {
                    alert('accessToken이 응답에 없습니다.');
                }
            },
            error: function (xhr) {
                const res = xhr.responseJSON;

                if (res && res.code === 'EMAIL_NOT_VERIFIED') {
                    alert(res.message || '이메일 인증이 필요합니다.\n"인증메일 보내기" 버튼을 눌러 인증메일을 받아 주세요.');

                    $('#loginModal').modal('hide');

                    $('#verifyEmail').val(email);
                    $('#verifyCode').val('');
                    $('#emailVerifyModal').modal('show');

                    lastSignupEmail = email;
                    return;
                }

                const msg = res && res.message
                    ? res.message
                    : '로그인에 실패했습니다. 이메일/비밀번호를 확인해 주세요.';
                alert(msg);
            }
        });
    });

    /* ===== 비밀번호 찾기 모달 열기 ===== */
    $('#openForgotPasswordBtn').on('click', function () {
        const email = $('#loginEmail').val().trim();
        $('#pwResetEmail').val(email || '');

        pwResetVerified = false;
        $('#pwResetPasswordArea').addClass('d-none');
        $('#pwResetNewPassword').val('').removeClass('is-invalid');
        $('#pwResetNewPasswordConfirm').val('').removeClass('is-invalid');
        $('#pwResetPasswordError').addClass('d-none').text('');
        $('#pwResetPasswordConfirmError').addClass('d-none').text('');
        $('#pwResetCode').val('');
        $('#pwResetEmail').prop('readonly', false);
        $('#pwResetCode').prop('readonly', false);

        $('#pwResetStep1Buttons').removeClass('d-none');
        $('#pwResetStep2Buttons').addClass('d-none');

        $('#loginModal').modal('hide');
        $('#forgotPasswordModal').modal('show');
    });

    /* ===== 비밀번호 찾기: 인증메일 보내기 (1단계) ===== */
    $('#pwResetSendMailBtn').on('click', function () {
        const email = $('#pwResetEmail').val().trim();
        if (!email) {
            alert('이메일을 입력해 주세요.');
            return;
        }

        $.ajax({
            url: '/api/password/forgot',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email }),
            success: function (res) {
                const msg = res && res.message
                    ? res.message
                    : '비밀번호 재설정용 인증 메일을 보냈습니다.\n메일함을 확인해 주세요.';
                alert(msg);
            },
            error: function () {
                alert('인증 메일 발송에 실패했습니다.\n잠시 후 다시 시도해 주세요.');
            }
        });
    });

    /* ===== 비밀번호 찾기: 인증번호 확인 (1단계) ===== */
    $('#pwResetVerifyCodeBtn').on('click', function () {
        const email = $('#pwResetEmail').val().trim();
        const code = $('#pwResetCode').val().trim();

        if (!email) {
            alert('이메일을 입력해 주세요.');
            return;
        }
        if (!code) {
            alert('인증번호를 입력해 주세요.');
            return;
        }

        $.ajax({
            url: '/api/password/verify-reset',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email, code: code }),
            success: function (res) {
                pwResetVerified = true;

                $('#pwResetPasswordArea').removeClass('d-none');
                $('#pwResetEmail').prop('readonly', true);
                $('#pwResetCode').prop('readonly', true);

                $('#pwResetStep1Buttons').addClass('d-none');
                $('#pwResetStep2Buttons').removeClass('d-none');

                alert((res && res.message) || '이메일 인증이 완료되었습니다.\n새 비밀번호를 입력해 주세요.');
            },
            error: function (xhr) {
                pwResetVerified = false;
                const res = xhr.responseJSON;
                const msg = res && res.message
                    ? res.message
                    : '인증번호 확인에 실패했습니다.\n번호와 유효시간을 다시 확인해 주세요.';
                alert(msg);
            }
        });
    });

    /* ===== 비밀번호 재설정 제출 (2단계) ===== */
    $('#passwordResetForm').on('submit', function (e) {
        e.preventDefault();

        const email = $('#pwResetEmail').val().trim();
        const code = $('#pwResetCode').val().trim();
        const newPassword = $('#pwResetNewPassword').val();
        const newPasswordConfirm = $('#pwResetNewPasswordConfirm').val();

        $('#pwResetNewPassword').removeClass('is-invalid');
        $('#pwResetNewPasswordConfirm').removeClass('is-invalid');
        $('#pwResetPasswordError').addClass('d-none').text('');
        $('#pwResetPasswordConfirmError').addClass('d-none').text('');

        if (!pwResetVerified) {
            alert('먼저 "인증번호 확인" 버튼을 눌러 이메일 인증을 완료해 주세요.');
            return;
        }

        if (!newPassword || !newPasswordConfirm) {
            alert('새 비밀번호와 비밀번호 확인을 모두 입력해 주세요.');
            return;
        }

        const ruleMsg = validatePasswordRules(newPassword);
        if (ruleMsg) {
            $('#pwResetNewPassword').addClass('is-invalid');
            $('#pwResetPasswordError').removeClass('d-none').text(ruleMsg);
            $('#pwResetNewPassword').focus();
            return;
        }

        if (newPassword !== newPasswordConfirm) {
            $('#pwResetNewPasswordConfirm').addClass('is-invalid');
            $('#pwResetPasswordConfirmError')
                .removeClass('d-none')
                .text('새 비밀번호와 비밀번호 확인이 일치하지 않습니다.');
            $('#pwResetNewPasswordConfirm').focus();
            return;
        }

        $.ajax({
            url: '/api/password/reset',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                email: email,
                code: code,
                newPassword: newPassword
            }),
            success: function (res) {
                alert((res && res.message) || '비밀번호가 성공적으로 변경되었습니다.\n새 비밀번호로 다시 로그인해 주세요.');

                $('#forgotPasswordModal').modal('hide');

                $('#loginEmail').val(email);
                $('#loginPassword').val('');
                $('#loginModal').modal('show');
            },
            error: function (xhr) {
                const res = xhr.responseJSON;
                const msg = res && res.message
                    ? res.message
                    : '비밀번호 변경에 실패했습니다.\n인증번호와 비밀번호를 다시 확인해 주세요.';
                alert(msg);
            }
        });
    });

    /* ===== 로그아웃 ===== */
    $('#logoutLink').on('click', function (e) {
        e.preventDefault();

        const token = getAccessToken();
        if (!token) {
            setAccessToken(null);
            updateAuthUI();
            return;
        }

        $.ajax({
            url: '/api/auth/logout',
            method: 'POST',
            complete: function () {
                setAccessToken(null);
                updateAuthUI();
                alert('로그아웃 되었습니다.');
            }
        });
    });

    // 초기 UI 세팅
    updateAuthUI();
});

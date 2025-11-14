/* global $, getAccessToken */

(function () {
  const DEFAULT_LIMIT = 4;
  let pwChangeVerified = false;
  let accessToken = null;
  let isAllDreamsLoaded = false;

  const $dreamList = $('#myDreamList');
  const $dreamEmptyState = $('#myDreamEmptyState');
  const $viewAllBtn = $('#viewAllMyDreamBtn');
  const $viewDreamModal = $('#viewDreamModal');
  const $viewDreamModalLoading = $('#viewDreamModalLoading');
  const $deleteDreamBtn = $('#deleteDreamBtn');
  const $reAnalyzeBtn = $('#reAnalyzeBtn');
  const $editDreamBtn = $('#editDreamBtn');
  const $saveDreamBtn = $('#saveDreamBtn');
  const $cancelEditBtn = $('#cancelEditBtn');

  function init() {
    accessToken = getAccessToken();

    if (!accessToken) {
      alert('로그인이 필요합니다.');
      window.location.href = '/';
      return;
    }

    $.ajaxSetup({
      xhrFields: {
        withCredentials: true
      }
    });

    setBirthdayMax();
    bindProfileForm();
    bindPasswordModal();
    bindDreamActions();

    fetchProfile();
    fetchMyDreams(false);
  }

  function setBirthdayMax() {
    const today = new Date().toISOString().split('T')[0];
    $('#profileBirthday').attr('max', today);
  }

  function bindProfileForm() {
    $('#profileForm').on('submit', function (e) {
      e.preventDefault();

      const name = $('#profileName').val().trim();
      const birthday = $('#profileBirthday').val();
      const gender = $('input[name="profileGender"]:checked').val();

      if (!name || !birthday || !gender) {
        alert('이름, 성별, 생년월일을 모두 입력해 주세요.');
        return;
      }

      $.ajax({
        url: '/api/users/me',
        method: 'PUT',
        contentType: 'application/json',
        beforeSend: addAuthHeader,
        data: JSON.stringify({
          name,
          birthday,
          gender: parseInt(gender, 10)
        }),
        success: function () {
          alert('내 정보가 수정되었습니다.');
          const editModalEl = document.getElementById('editProfileModal');
          if (editModalEl && window.bootstrap) {
            window.bootstrap.Modal.getOrCreateInstance(editModalEl).hide();
          }
          fetchProfile();
        },
        error: function (xhr) {
          const res = xhr.responseJSON;
          const msg = res && res.message ? res.message : '정보 수정에 실패했습니다.';
          alert(msg);
        }
      });
    });
  }

  function bindPasswordModal() {
    $('#changePasswordModal').on('show.bs.modal', function () {
      pwChangeVerified = false;
      $('#currentPassword').val('');
      $('#newPassword').val('').removeClass('is-invalid');
      $('#newPasswordConfirm').val('').removeClass('is-invalid');
      $('#newPasswordError').addClass('d-none').text('');
      $('#newPasswordConfirmError').addClass('d-none').text('');

      $('#pwChangeStep1').removeClass('d-none');
      $('#pwChangeStep2').addClass('d-none');
      $('#pwChangeStep1Buttons').removeClass('d-none');
      $('#pwChangeStep2Buttons').addClass('d-none');
    });

    $('#checkCurrentPasswordBtn').on('click', function () {
      const currentPw = $('#currentPassword').val();
      if (!currentPw) {
        alert('현재 비밀번호를 입력해 주세요.');
        return;
      }

      $.ajax({
        url: '/api/users/me/check-password',
        method: 'POST',
        contentType: 'application/json',
        beforeSend: addAuthHeader,
        data: JSON.stringify({ currentPassword: currentPw }),
        success: function (res) {
          pwChangeVerified = true;
          $('#pwChangeStep1').addClass('d-none');
          $('#pwChangeStep2').removeClass('d-none');
          $('#pwChangeStep1Buttons').addClass('d-none');
          $('#pwChangeStep2Buttons').removeClass('d-none');

          alert((res && res.message) || '비밀번호가 확인되었습니다.\n새 비밀번호를 입력해 주세요.');
        },
        error: function (xhr) {
          pwChangeVerified = false;
          const res = xhr.responseJSON;
          const msg = res && res.message ? res.message : '현재 비밀번호가 일치하지 않습니다.';
          alert(msg);
        }
      });
    });

    $('#changePasswordBtn').on('click', function () {
      if (!pwChangeVerified) {
        alert('먼저 현재 비밀번호를 확인해 주세요.');
        return;
      }

      const currentPw = $('#currentPassword').val();
      const newPw = $('#newPassword').val();
      const newPwConfirm = $('#newPasswordConfirm').val();

      $('#newPassword').removeClass('is-invalid');
      $('#newPasswordConfirm').removeClass('is-invalid');
      $('#newPasswordError').addClass('d-none').text('');
      $('#newPasswordConfirmError').addClass('d-none').text('');

      const ruleMsg = validateProfilePasswordRules(newPw);
      if (ruleMsg) {
        $('#newPassword').addClass('is-invalid');
        $('#newPasswordError').removeClass('d-none').text(ruleMsg);
        $('#newPassword').focus();
        return;
      }

      if (newPw !== newPwConfirm) {
        $('#newPasswordConfirm').addClass('is-invalid');
        $('#newPasswordConfirmError')
          .removeClass('d-none')
          .text('새 비밀번호와 비밀번호 확인이 일치하지 않습니다.');
        $('#newPasswordConfirm').focus();
        return;
      }

      $.ajax({
        url: '/api/users/me/change-password',
        method: 'POST',
        contentType: 'application/json',
        beforeSend: addAuthHeader,
        data: JSON.stringify({
          currentPassword: currentPw,
          newPassword: newPw
        }),
        success: function (res) {
          alert((res && res.message) || '비밀번호가 성공적으로 변경되었습니다.');
          $('#changePasswordModal').modal('hide');
        },
        error: function (xhr) {
          const res = xhr.responseJSON;
          const msg = res && res.message
            ? res.message
            : '비밀번호 변경에 실패했습니다.\n다시 시도해 주세요.';
          alert(msg);
        }
      });
    });
  }

  function bindDreamActions() {
    $viewAllBtn.on('click', function () {
      if (isAllDreamsLoaded) {
        return;
      }
      fetchMyDreams(true);
    });

    $(document).on('click', '.my-dream-view-btn', async function (e) {
      e.preventDefault();
      const dreamId = $(this).data('dreamId');
      if (!dreamId) {
        return;
      }

      switchModalMode('view');
      toggleViewModalLoading(true);
      try {
        const res = await fetch(`/api/dream/${dreamId}/analysis`, { credentials: 'include' });
        if (!res.ok) {
          throw new Error('상세 정보를 불러오지 못했습니다.');
        }
        const data = await res.json();
        updateDreamDetailModal(data);

        if ($viewDreamModal.length && window.bootstrap) {
          const modal = window.bootstrap.Modal.getOrCreateInstance($viewDreamModal[0]);
          modal.show();
        }
      } catch (err) {
        console.error(err);
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
      } finally {
        toggleViewModalLoading(false);
      }
    });

    $deleteDreamBtn.on('click', async function () {
      const modalEl = $viewDreamModal[0];
      const dreamId = modalEl?.dataset?.dreamId;
      if (!dreamId) return;
      if (!confirm('정말 삭제하시겠습니까?')) return;

      setButtonsDisabled(true, 'deleteDreamBtn', 'editDreamBtn', 'reAnalyzeBtn');
      toggleViewModalLoading(true);
      try {
        const res = await fetch(`/api/dream/${dreamId}`, {
          method: 'DELETE',
          credentials: 'include'
        });
        if (!res.ok) throw new Error('삭제 실패');
        alert('삭제되었습니다.');
        const modal = window.bootstrap?.Modal.getOrCreateInstance(modalEl);
        modal?.hide();
        await fetchMyDreams(isAllDreamsLoaded);
      } catch (err) {
        console.error(err);
        alert(err.message || '삭제 중 오류가 발생했습니다.');
      } finally {
        toggleViewModalLoading(false);
        setButtonsDisabled(false, 'deleteDreamBtn', 'editDreamBtn', 'reAnalyzeBtn');
      }
    });

    $reAnalyzeBtn.on('click', async function () {
      const modalEl = $viewDreamModal[0];
      const dreamId = modalEl?.dataset?.dreamId;
      if (!dreamId) return;

      setButtonsDisabled(true, 'reAnalyzeBtn', 'editDreamBtn', 'deleteDreamBtn');
      toggleViewModalLoading(true);
      try {
        const res = await fetch(`/api/dream/${dreamId}/analysis`, {
          method: 'PUT',
          credentials: 'include'
        });
        if (!res.ok) {
          const msg = await res.text();
          throw new Error(msg || '분석 실패');
        }
        await reloadDreamDetail(dreamId);
        alert('꿈 분석이 완료되었습니다.');
      } catch (err) {
        console.error(err);
        alert(err.message || '재분석 중 오류가 발생했습니다.');
      } finally {
        toggleViewModalLoading(false);
        setButtonsDisabled(false, 'reAnalyzeBtn', 'editDreamBtn', 'deleteDreamBtn');
      }
    });

    $editDreamBtn.on('click', function () {
      switchModalMode('edit');
    });

    $cancelEditBtn.on('click', function () {
      switchModalMode('view');
    });

    $saveDreamBtn.on('click', async function () {
      const modalEl = $viewDreamModal[0];
      const dreamId = modalEl?.dataset?.dreamId;
      if (!dreamId) return;

      const prevContent = modalEl?.dataset?.dreamContent || '';
      const title = $('#editTitle').val()?.toString().trim();
      const content = $('#editContent').val()?.toString().trim();
      const published = $('#editPublished').is(':checked');

      if (!title || !content) {
        alert('제목과 내용을 모두 입력해주세요.');
        return;
      }

      const contentChanged = content !== prevContent;
      if (contentChanged) {
        toggleViewModalLoading(true);
      }
      setButtonsDisabled(true, 'saveDreamBtn', 'deleteDreamBtn', 'editDreamBtn', 'reAnalyzeBtn');

      try {
        const res = await fetch(`/api/dream/${dreamId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ title, content, published })
        });
        if (!res.ok) {
          const msg = await res.text();
          throw new Error(msg || '수정 실패');
        }
        await reloadDreamDetail(dreamId);
        await fetchMyDreams(isAllDreamsLoaded);
        switchModalMode('view');
        alert('수정되었습니다.');
      } catch (err) {
        console.error(err);
        alert(err.message || '수정 중 오류가 발생했습니다.');
      } finally {
        toggleViewModalLoading(false);
        setButtonsDisabled(false, 'saveDreamBtn', 'deleteDreamBtn', 'editDreamBtn', 'reAnalyzeBtn');
      }
    });
  }

  function fetchProfile() {
    $.ajax({
      url: '/api/users/me',
      method: 'GET',
      beforeSend: addAuthHeader,
      success: function (res) {
        $('#profileEmail').val(res.email || '');
        $('#profileName').val(res.name || '');
        if (res.gender) {
          $('input[name="profileGender"][value="' + res.gender + '"]').prop('checked', true);
        }
        if (res.birthday) {
          $('#profileBirthday').val(res.birthday);
        }

        const displayName = (res.name && res.name.trim().length) ? res.name.trim() : (res.email || 'Dreamer');
        $('#profileDisplayName').text(displayName);
        $('#profileAvatarText').text(displayName ? displayName.charAt(0).toUpperCase() : 'D');
        $('#profileEmailDisplay').text(res.email || 'dreamer@example.com');

        const createdLabel = formatDateOnly(res.createdAt);
        const joinedLabel = createdLabel !== '-' ? createdLabel : formatDateOnly(res.joinedAt);
        $('#profileJoinDate').text(joinedLabel !== '-' ? joinedLabel : '-');

        if (typeof res.publicDreamCount === 'number') {
          $('#profilePublicDreamCount').text(res.publicDreamCount);
        }

        if (typeof res.totalDreamCount === 'number') {
          const publicCount = typeof res.publicDreamCount === 'number' ? res.publicDreamCount : 0;
          const privateCount = Math.max(0, res.totalDreamCount - publicCount);
          $('#profilePrivateDreamCount').text(privateCount);
        }

        if (typeof res.analyzedDreamCount === 'number') {
          $('#profileAnalyzedDreamCount').text(res.analyzedDreamCount);
        }
      },
      error: function () {
        alert('내 정보를 불러오는데 실패했습니다.');
      }
    });
  }

  function fetchMyDreams(loadAll) {
    const url = loadAll ? '/api/dream/my/all' : `/api/dream/my?limit=${DEFAULT_LIMIT}`;

    $.ajax({
      url,
      method: 'GET',
      beforeSend: addAuthHeader,
      success: function (res) {
        const dreams = Array.isArray(res) ? res : [];
        renderDreamList(dreams);

        if (loadAll) {
          isAllDreamsLoaded = true;
          $viewAllBtn.addClass('d-none');
        } else {
          if (dreams.length > 0) {
            $viewAllBtn.removeClass('d-none');
          } else {
            $viewAllBtn.addClass('d-none');
          }
        }
      },
      error: function () {
        alert('내 꿈 목록을 불러오는데 실패했습니다.');
      }
    });
  }

  function renderDreamList(dreams) {
    $dreamList.empty();

    if (!dreams || dreams.length === 0) {
      $dreamList.addClass('d-none');
      $dreamEmptyState.removeClass('d-none');
      return;
    }

    $dreamEmptyState.addClass('d-none');
    $dreamList.removeClass('d-none');

    dreams.forEach(function (dream) {
      const $item = $('<div>').addClass('dream-item').attr('data-dream-id', dream.id || '');

      const $header = $('<div>').addClass('dream-item-header');
      const $title = $('<h4>').addClass('dream-item-title').text(dream.title || '제목 없음');

      const $meta = $('<div>').addClass('dream-item-meta');
      const $createdAt = $('<span>');
      $('<i>').addClass('far fa-calendar-alt me-1').appendTo($createdAt);
      $('<span>').text(formatDateTime(dream.createdAt)).appendTo($createdAt);
      $meta.append($createdAt);

      const visibilityLabel = dream.published ? '공개' : '비공개';
      const visibilityIconClass = dream.published ? 'fas fa-lock-open me-1' : 'fas fa-lock me-1';
      const $visibility = $('<span>');
      $('<i>').addClass(visibilityIconClass).appendTo($visibility);
      $('<span>').text(visibilityLabel).appendTo($visibility);
      $meta.append($visibility);

      $header.append($title).append($meta);

      const $summary = $('<p>').addClass('dream-item-summary').text(buildSummary(dream.content));

      const $footer = $('<div>').addClass('dream-item-footer');

      if (Array.isArray(dream.tags) && dream.tags.length > 0) {
        const $tags = $('<div>').addClass('dream-item-tags');
        dream.tags.forEach(function (tag) {
          $('<span>').addClass('dream-item-tag').text('#' + tag).appendTo($tags);
        });
        $footer.append($tags);
      }

      const $actions = $('<div>').addClass('dream-item-actions');
      $('<button>')
        .addClass('btn btn-view-dream btn-sm my-dream-view-btn')
        .attr('type', 'button')
        .attr('data-dream-id', dream.id || '')
          .attr('data-user-id', dream.userId || '')
        .text('자세히 보기')
        .appendTo($actions);

      $footer.append($actions);

      $item.append($header).append($summary).append($footer);

      $dreamList.append($item);
    });
  }

  function toggleViewModalLoading(show) {
    if (!$viewDreamModalLoading.length) return;
    if (show) {
      $viewDreamModalLoading.removeClass('d-none').addClass('d-flex');
    } else {
      $viewDreamModalLoading.removeClass('d-flex').addClass('d-none');
    }
  }

  function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return $('<div>').text(text).html();
  }

  function parseArrayField(field) {
    if (!field) return [];
    if (Array.isArray(field)) return field;
    if (typeof field === 'string') {
      try {
        return JSON.parse(field);
      } catch (e) {
        return field.split(',').map(function (item) { return item.trim(); }).filter(Boolean);
      }
    }
    return [];
  }

  function updateDreamDetailModal(data) {
    const title = data.dreamTitle || data.title || '';
    const content = data.dreamContent || data.content || '';
    const insight = data.insight || '';
    const suggestion = data.suggestion || '';
    const published = data.dreamPublished !== undefined ? data.dreamPublished : data.published;

    $('#detailTitle').text(title);
    $('#detailContent').text(content);
    $('#detailInsight').text(insight || '분석 준비중');
    $('#detailSuggestion').text(suggestion || '분석 준비중');
    $('#editTitle').val(title);
    $('#editContent').val(content);
    $('#editPublished').prop('checked', !!published);

    // Scenes
    const scenes = Array.isArray(data.scenes) ? data.scenes : [];
    const $scenesWrap = $('#detailScenesWrap');
    const $scenes = $('#detailScenes');
    if (scenes.length > 0) {
      const html = scenes.map(function (scene) {
        return `
          <div class="card h-100 dream-card mb-3">
            <div class="card-body">
              <h5 class="card-title">${escapeHtml(scene.content || '')}</h5>
              <p class="card-text mb-1">${escapeHtml(scene.emotion || '')}</p>
              <p class="card-text">${escapeHtml(scene.interpretation || '')}</p>
            </div>
          </div>
        `;
      }).join('');
      $scenes.html(html);
      $scenesWrap.removeClass('d-none');
    } else {
      $scenes.empty();
      $scenesWrap.addClass('d-none');
    }

    // Categories
    const categories = parseArrayField(data.categories);
    const $categoriesWrap = $('#detailCategoriesWrap');
    const $categories = $('#detailCategories');
    if (categories.length > 0) {
      const html = categories.map(function (cat) {
        return `<span class="col category-badge" style="max-width: max-content; margin: 4px;">${escapeHtml(cat)}</span>`;
      }).join('');
      $categories.html(html);
      $categoriesWrap.removeClass('d-none');
    } else {
      $categories.empty();
      $categoriesWrap.addClass('d-none');
    }

    // Tags
    const tags = parseArrayField(data.tags);
    const $tagsWrap = $('#detailTagsWrap');
    const $tags = $('#detailTags');
    if (tags.length > 0) {
      const html = tags.map(function (tag) {
        return `<button type="button" class="col tag-btn" style="max-width: max-content; margin: 4px;">#${escapeHtml(tag)}</button>`;
      }).join('');
      $tags.html(html);
      $tagsWrap.removeClass('d-none');
    } else {
      $tags.empty();
      $tagsWrap.addClass('d-none');
    }

    $('#detailPublished').text(published ? '공개' : '비공개');

    const sentiment = typeof data.sentiment === 'number' ? data.sentiment.toFixed(2) : '-';
    const magnitude = typeof data.magnitude === 'number' ? data.magnitude.toFixed(2) : '-';
    $('#detailEmotion').text(sentiment + '점');
    $('#detailMagnitude').text(magnitude + '점');

    if ($viewDreamModal.length) {
      $viewDreamModal[0].dataset.dreamId = data.dreamId ? String(data.dreamId) : '';
      $viewDreamModal[0].dataset.dreamContent = content;
    }

    switchModalMode('view');

    const currentUserId = getUserIdFromToken();
    const dreamUserId = data.userId ? parseInt(data.userId, 10) : null;
    const isOwner = currentUserId !== null && dreamUserId !== null && currentUserId === dreamUserId;

    if (isOwner) {
      $('#deleteDreamBtn, #editDreamBtn, #reAnalyzeBtn').removeClass('d-none');
    } else {
      $('#deleteDreamBtn, #editDreamBtn, #reAnalyzeBtn').addClass('d-none');
    }
  }

  function getUserIdFromToken() {
    try {
      const cookies = document.cookie.split(';');
      let token = null;
      for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'DD_AT') {
          token = value;
          break;
        }
      }
      if (!token) return null;
      const payload = token.split('.')[1];
      if (!payload) return null;
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
      return decoded.sub ? parseInt(decoded.sub, 10) : null;
    } catch (e) {
      console.error('토큰 파싱 실패:', e);
      return null;
    }
  }

  function setButtonsDisabled(disabled, ...ids) {
    ids.forEach(function (id) {
      const btn = document.getElementById(id);
      if (btn) {
        btn.disabled = disabled;
      }
    });
  }

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

  function formatDateOnly(value) {
    if (!value) return '-';
    if (typeof value === 'string') {
      if (!value.length) return '-';
      return value.substring(0, 10).replace(/-/g, '.');
    }
    try {
      return value.toISOString().substring(0, 10).replace(/-/g, '.');
    } catch (e) {
      return '-';
    }
  }

  async function reloadDreamDetail(dreamId) {
    const res = await fetch(`/api/dream/${dreamId}/analysis`, { credentials: 'include' });
    if (!res.ok) throw new Error('분석 정보 조회 실패');
    const data = await res.json();
    updateDreamDetailModal(data);
  }

  function addAuthHeader(xhr) {
    if (accessToken) {
      xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
    }
  }

  function formatDateTime(value) {
    if (!value) {
      return '-';
    }
    if (typeof value === 'string') {
      return value.replace('T', ' ').substring(0, 16);
    }
    try {
      return value.toString().replace('T', ' ').substring(0, 16);
    } catch (err) {
      return '-';
    }
  }

  function buildSummary(content) {
    if (!content) {
      return '등록된 내용이 없습니다.';
    }
    const trimmed = content.trim();
    if (trimmed.length <= 120) {
      return trimmed;
    }
    return trimmed.substring(0, 120) + '…';
  }

  function validateProfilePasswordRules(pw) {
    if (!pw) return '비밀번호를 입력해 주세요.';
    if (pw.length < 8 || pw.length > 20) {
      return '비밀번호는 8~20자여야 합니다.';
    }
    if (!/[A-Za-z]/.test(pw) || !/[0-9]/.test(pw)) {
      return '비밀번호는 영문과 숫자를 모두 포함해야 합니다.';
    }
    return '';
  }

  $(init);
})();


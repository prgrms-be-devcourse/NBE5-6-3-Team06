document.addEventListener('DOMContentLoaded', () => {
  // 공지사항 작성 및 검색 모듈 초기화
  const modal = document.getElementById('create-announcement-modal');
  const form = document.getElementById('announcement-form');
  const searchInput = document.getElementById('search-input');
  const searchBtn = document.getElementById('search-btn');
  const searchResults = document.getElementById('search-results');
  const selectedTargets = document.getElementById('selected-targets');
  const selectedTargetsContainer = document.getElementById(
      'selected-targets-container');
  const searchContainer = document.getElementById('search-container');
  const targetTypeSelect = document.getElementById('announcement-target-type');

  const addedUserIds = new Set();
  let currentNoticeId = null;

  // 새 공지사항 작성 버튼 클릭 이벤트
  const createNotificationBtn = document.getElementById(
      'create-notification-btn');
  const createNotificationModal = document.getElementById(
      'create-announcement-modal');

  if (createNotificationBtn && createNotificationModal) {
    createNotificationBtn.addEventListener('click', function () {
      createNotificationModal.style.display = 'block';
      document.getElementById('modal-title').textContent = "새 공지사항 작성"
      form.querySelector('#announcement-content').disabled = false;
    });
  }

  // 기존 공지사항 삭제/발송 버튼 이벤트 설정 복구
  document.querySelectorAll('.action-btn.delete').forEach(button => {
    button.addEventListener('click', function () {
      const noticeId = this.getAttribute('data-id');

      if (!confirm('정말로 이 공지사항을 비활성화하시겠습니까?')) {
        return;
      }

      fetch(`/api/admin/notification/${noticeId}`, {
        method: 'DELETE',
      })
      .then(response => {
        if (response.ok) {
          alert("공지사항이 비활성화되었습니다.");
          window.location.reload();
        } else {
          return response.text().then(text => {
            throw new Error(text)
          });
        }
      })
      .catch(error => {
        console.error('에러 발생:', error);
        alert('공지사항 비활성화 중 문제가 발생했습니다.');
      });
    });
  });

  // 공지사항 발송 버튼 클릭 이벤트
  document.querySelectorAll('.action-btn.send').forEach(button => {
    button.addEventListener('click', function () {
      const noticeId = this.getAttribute('data-id');
      const noticeMessage = this.getAttribute('data-message');

      modal.style.display = 'block';
      resetModal();
      form.querySelector('#announcement-content').value = noticeMessage;
      form.querySelector('#announcement-content').disabled = true;
      document.getElementById('modal-title').textContent = "공지사항 발송"
      currentNoticeId = noticeId;
    });
  });

  document.getElementById('create-announcement-btn')?.addEventListener('click',
      () => {
        modal.style.display = 'block';
        resetModal();
      });

  modal.querySelector('.close-modal')?.addEventListener('click', () => {
    modal.style.display = 'none';
    resetModal();
  });

  modal.querySelector('.cancel-btn')?.addEventListener('click', () => {
    modal.style.display = 'none';
    resetModal();
  });

  document.getElementById('send-announcement-btn')?.addEventListener('click',
      () => {
        const content = document.getElementById(
            'announcement-content').value.trim();
        const targetType = targetTypeSelect.value;
        let targetUserIds = Array.from(addedUserIds);

        if (targetType === 'all') {
          targetUserIds = ["all"]
        }

        const notificationData = {
          content,
          targetType, // all | group | user 구분용
          targetUserIds
        };

        const url = currentNoticeId
            ? `/api/admin/notification/${currentNoticeId}`
            : '/api/admin/notification/broadcast';

        fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(notificationData)
        })
        .then(async (response) => {
          if (response.ok) {
            alert("공지사항이 성공적으로 발송되었습니다.");
            modal.style.display = 'none';
            form.reset();
            window.location.reload();
          } else {
            const result = (await response.json()).data;
            document.querySelector('#error-type').textContent = result.targetType;
            document.querySelector('#error-targets').textContent = result.targetUserIds;
            document.querySelector('#error-content').textContent = result.content;
            alert('필수 항목을 확인해주세요.');
            console.error(result);
          }
        })
        .catch(error => {
          console.error('공지사항 발송 요청 오류:', error);
          alert('공지사항 발송 중 오류가 발생했습니다.');
        });
      });

  targetTypeSelect?.addEventListener('change', () => {
    const type = targetTypeSelect.value;
    const isAll = type === 'all';
    searchContainer.style.display = isAll ? 'none' : 'block';
    selectedTargetsContainer.style.display = isAll ? 'none' : 'block';
    searchInput.placeholder = type === 'group' ? '모임 주최자 아이디 검색' : '사용자 아이디 검색';
    searchInput.value = '';
    searchResults.innerHTML = '';
  });

  searchBtn?.addEventListener('click', handleSearch);
  searchInput?.addEventListener('keypress', e => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSearch();
    }
  });

  function handleSearch() {
    const keyword = searchInput.value.trim();
    console.log(keyword)
    const type = targetTypeSelect.value;
    if (!keyword) {
      return alert('검색어를 입력하세요.');
    }

    searchResults.innerHTML = '';

    if (type === 'group') {
      fetch(`/api/admin/notification/team/${keyword}`, {
        method: 'GET'
      })
      .then(async (response) => {
        const result = (await response.json()).data;
        console.log(result)
        if (response.ok) {
          result.forEach(team => {
            const item = createSearchResultItem(team.teamTitle, `주최자 : ${team.leaderId}`, () => {
              team.userIds.forEach(uid => {
                addTargetBadge(uid, `${uid}`, 'user');
              });
            });
            searchResults.appendChild(item);
          });
        }
      }).catch(error => {
        console.error('모임 조회 오류:', error);
        alert('모임 조회 중 오류가 발생했습니다.');
      });

    } else {
      fetch(`/api/admin/notification/user/${keyword}`, {
        method: 'GET'
      })
      .then(async (response) => {
        const result = (await response.json()).data;
        console.log(result)
        if (response.ok) {
          result.forEach(user => {
            const item = createSearchResultItem(user.nickname, user.userId,
                () => {
                  addTargetBadge(user.userId, user.nickname, 'user');
                });
            searchResults.appendChild(item);
          });
        }
      }).catch(error => {
        console.error('사용자 조회 오류:', error);
        alert('사용자 조회 중 오류가 발생했습니다.');
      });
    }
  }

  function createSearchResultItem(name, subtext, onAdd) {
    const item = document.createElement('div');
    item.className = 'search-result-item';
    item.innerHTML = `
      <div class="search-result-info">
        <span class="search-result-name">${name}</span>
        <span class="search-result-id">${subtext}</span>
      </div>
      <button class="search-result-add" type="button">추가</button>
    `;
    item.querySelector('button').addEventListener('click', onAdd);
    return item;
  }

  function addTargetBadge(id, name, type) {
    if (addedUserIds.has(id)) {
      alert(`${id} 는 이미 선택된 사용자입니다.`)
      return
    }
    addedUserIds.add(id);
    const badge = document.createElement('div');
    badge.className = `target-badge ${type}`;
    badge.setAttribute('data-id', id);
    badge.innerHTML = `<span>${id}</span><span class="target-badge-remove">×</span>`;

    badge.querySelector('.target-badge-remove').addEventListener('click',
        () => {
          badge.remove();
          addedUserIds.delete(id);
        });

    selectedTargets.appendChild(badge);
  }

  function resetModal() {
    form.reset();
    searchContainer.style.display = 'none';
    selectedTargetsContainer.style.display = 'none';
    searchResults.innerHTML = '';
    selectedTargets.innerHTML = '';
    addedUserIds.clear();
  }
});

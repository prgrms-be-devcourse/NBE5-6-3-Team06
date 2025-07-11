document.addEventListener('DOMContentLoaded', function() {
    let suggestion = null;

    // 식당 추가 버튼 클릭 이벤트
    const addRestaurantBtn = document.getElementById('add-restaurant-btn');
    const restaurantSearchModal = document.getElementById('restaurant-search-modal');
    if (addRestaurantBtn) {
        addRestaurantBtn.addEventListener('click', function() {
            // 모달 제목 설정
            document.getElementById('restaurant-modal-title').textContent = '새 식당 추가';
            
            // 폼 초기화
            document.getElementById('restaurantForm').reset();
            document.getElementById('restaurant-id').value = '';
            suggestion = null;
            // 모달 표시
            // document.getElementById('restaurantModal').style.display = 'block';
            document.getElementById('restaurant-search-modal').style.display = 'block';

            setTimeout(() => {
                map.relayout();
                map.setCenter(new kakao.maps.LatLng(37.5665, 126.9780));
            }, 200);
        });
    }

    // 최대 선택 가능 개수
    const MAX_MOOD_SELECTION = 3;

    // 체크박스 선택 제한
    const moodCheckboxes = document.querySelectorAll('input[name="restaurant-mood"]');
    moodCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', () => {
            const checkedCount = document.querySelectorAll('input[name="restaurant-mood"]:checked').length;
            if (checkedCount > MAX_MOOD_SELECTION) {
                checkbox.checked = false;
                alert(`최대 ${MAX_MOOD_SELECTION}개까지만 선택할 수 있습니다.`);
            }
        });
    });

    // 식당 수정 버튼 클릭 이벤트
    const editButtons = document.querySelectorAll('.action-btn.edit');

    editButtons.forEach(button => {
        button.addEventListener('click', function () {
            const restaurantId = this.getAttribute('data-id');
            suggestion = null;

            // 서버에서 식당 데이터 가져오기
            fetch(`/api/admin/restaurant/${restaurantId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('서버 응답 실패');
                }
                return response.json();
            })
            .then(data => {
                console.log(data.data);
                const result = data.data;
                // 받아온 데이터로 모달 채우기
                document.getElementById('restaurant-modal-title').textContent = '식당 정보 수정';
                document.getElementById('restaurant-id').value = result.restaurantId;
                document.getElementById('restaurant-name').value = result.name;
                document.getElementById('restaurant-category').value = result.category;
                document.getElementById('restaurant-address').value = result.address;
                document.getElementById('restaurant-phone').value = result.tel;
                document.getElementById('restaurant-hours').value = result.openTime;
                document.getElementById('restaurant-main-menu').value = result.mainFood;
                document.getElementById('restaurant-description').value = result.summary;
                document.getElementById('restaurant-google-rating').value = result.googleRating;
                document.getElementById('restaurant-latitude').value = result.latitude;
                document.getElementById('restaurant-longitude').value = result.longitude;

                // 분위기 체크박스 설정
                moodCheckboxes.forEach(checkbox => {
                    const fieldName = checkbox.value; // 예: "goodTalk", "clean" 등
                    if (result.hasOwnProperty(fieldName)) {
                        checkbox.checked = result[fieldName] === true;
                    } else {
                        checkbox.checked = false;
                    }
                });

                // 모달 표시
                document.getElementById('restaurantModal').style.display = 'block';
            })
            .catch(error => {
                console.error('식당 정보를 불러오는 중 오류 발생:', error);
                alert('식당 정보를 불러오지 못했습니다.');
            });
        });
    });

    // 식당 수정 - 저장 버튼 클릭 이벤트
    // 새 식당 추가 - 저장 버튼 클릭 이벤트
    // 식당 제안 등록 - 저장 버튼 클릭 이벤트
    document.getElementById('save-button').addEventListener('click', function () {
        const restaurantId = document.getElementById('restaurant-id').value;

        const payload = {
            name: document.getElementById('restaurant-name').value,
            category: document.getElementById('restaurant-category').value,
            address: document.getElementById('restaurant-address').value,
            tel: document.getElementById('restaurant-phone').value,
            openTime: document.getElementById('restaurant-hours').value,
            mainFood: document.getElementById('restaurant-main-menu').value,
            summary: document.getElementById('restaurant-description').value,
            googleRating: parseFloat(document.getElementById('restaurant-google-rating').value),
            latitude: document.getElementById('restaurant-latitude').value,
            longitude: document.getElementById('restaurant-longitude').value
        };

        // 분위기 체크박스를 기반으로 각 mood 항목을 boolean으로 추가
        moodCheckboxes.forEach(cb => {
            payload[cb.value] = cb.checked;
        });

        const url = restaurantId
            ? `/api/admin/restaurant/${restaurantId}` // 수정 (PATCH)
            : `/api/admin/restaurant`;      // 생성 (POST)

        const method = restaurantId ? 'PATCH' : 'POST';

        fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })
        .then(async (response) => {
            const result = (await response.json()).data;
            if (response.ok) {
                if (suggestion) {
                    approveSuggestionStatus();
                    return;
                }
                alert("식당이 저장되었습니다.")
                // 모달 닫기 등 후처리
                document.getElementById('restaurantModal').style.display = 'none';
                location.reload(); // 새로고침으로 반영
            } else if (response.status === 409) {
                alert("이미 존재하는 식당입니다.");
            } else {
                document.querySelector('#error-name').textContent = result.name;
                document.querySelector('#error-category').textContent = result.category;
                document.querySelector('#error-address').textContent = result.address;
                document.querySelector('#error-phone').textContent = result.tel;
                document.querySelector('#error-hours').textContent = result.openTime;
                document.querySelector('#error-main-menu').textContent = result.mainFood;
                document.querySelector('#error-description').textContent = result.summary;
                document.querySelector('#error-mood').textContent = result.moodSelectionCountValid;
                document.querySelector('#error-google-rating').textContent = result.googleRating;
                alert('필수 항목을 확인해주세요.');
            }
        })
        .catch(err => {
            console.error(err);
            alert("저장 중 오류가 발생했습니다.");
        });
    });

    // 식당 삭제 버튼 클릭 이벤트
    document.querySelectorAll('.action-btn.delete').forEach(button => {
        button.addEventListener('click', function () {
            const restaurantId = this.getAttribute('data-id');

            if (confirm("정말 이 식당을 삭제하시겠습니까?")) {
                fetch(`/api/admin/restaurant/${restaurantId}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                })
                .then(response => {
                    if (response.ok) {
                        alert("식당이 삭제되었습니다.")
                        window.location.href = "/admin/restaurant";
                    } else {
                        alert("삭제 실패: 서버 오류");
                    }
                })
                .catch(error => {
                    console.error("에러 발생:", error);
                    alert("삭제 중 문제가 발생했습니다.");
                });
            }
        });
    });

    // 식당 제안 삭제 버튼 클릭 이벤트
    document.querySelectorAll('.action-btn.suggestion-delete').forEach(button => {
        button.addEventListener('click', function () {
            const suggestionId = this.getAttribute('data-id');

            if (confirm("정말 이 식당 제안을 삭제하시겠습니까?")) {
                fetch(`/api/admin/restaurant/suggestion/${suggestionId}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                })
                .then(response => {
                    if (response.ok) {
                        alert("식당 제안이 삭제되었습니다.")
                        window.location.href = "/admin/restaurant/suggestion";
                    } else {
                        alert("삭제 실패: 서버 오류");
                    }
                })
                .catch(error => {
                    console.error("에러 발생:", error);
                    alert("삭제 중 문제가 발생했습니다.");
                });
            }
        });
    });

    // 식당 제안 저장 버튼 클릭 이벤트
    const suggestionEditButtons = document.querySelectorAll('.action-btn.suggestion-edit');

    suggestionEditButtons.forEach(button => {
        button.addEventListener('click', function () {
            const suggestionId = this.getAttribute('data-id');
            const name = this.getAttribute('data-name');
            const address = this.getAttribute('data-address');
            const mainFood = this.getAttribute('data-main-food');
            const latitude = this.getAttribute('data-latitude');
            const longitude = this.getAttribute('data-longitude');
            suggestion = suggestionId;

            document.getElementById('restaurant-modal-title').textContent = '식당 제안 등록';
            document.getElementById('restaurant-name').value = name;
            document.getElementById('restaurant-address').value = address;
            document.getElementById('restaurant-main-menu').value = mainFood;
            document.getElementById('restaurant-latitude').value = latitude;
            document.getElementById('restaurant-longitude').value = longitude;

            // 모달 표시
            document.getElementById('restaurantModal').style.display = 'block';
        });
    });

    // 식당 제안 거절 버튼 클릭 이벤트
    document.querySelectorAll('.action-btn.suggestion-reject').forEach(button => {
        button.addEventListener('click', function () {
            const suggestionId = this.getAttribute('data-id');

            fetch(`/api/admin/restaurant/suggestion/reject/${suggestionId}`, {
                method: 'PATCH'
            }).then(response => {
                if (response.ok) {
                    alert("식당 제안이 거절되었습니다.")
                    window.location.href = "/admin/restaurant/suggestion";
                } else {
                    alert("거절 실패: 서버 오류");
                }
            })
            .catch(error => {
                console.error("에러 발생:", error);
                alert("거절 중 문제가 발생했습니다.");
            });
        });
    });

    function approveSuggestionStatus() {
        fetch(`/api/admin/restaurant/suggestion/approve/${suggestion}`, {
            method: 'PATCH'
        }).then(response => {
            if (response.ok) {
                alert("식당이 등록되었습니다.")
                document.getElementById('restaurantModal').style.display = 'none';
                window.location.href = "/admin/restaurant";
            } else {
                alert("등록 실패: 서버 오류");
            }
        })
        .catch(error => {
            console.error("에러 발생:", error);
            alert("등록 중 문제가 발생했습니다.");
        });
    }

    initRestaurantSearchMap(); // 지도 초기화

    setupRestaurantSearchModal(); // 새로 추가한 함수 호출
});

let map;
let marker;
let selectedRestaurant = null;

function initRestaurantSearchMap() {
    const container = document.getElementById('restaurant-map');
    map = new kakao.maps.Map(container, {
        center: new kakao.maps.LatLng(37.5665, 126.9780),
        level: 3
    });
}

function setupRestaurantSearchModal() {
    const searchBtn = document.getElementById('restaurant-search-btn');
    const searchInput = document.getElementById('restaurant-search-input');
    const resultsList = document.getElementById('restaurant-results-list');
    const addressDisplay = document.getElementById('selected-restaurant-address');
    const repMenuInput = document.getElementById('representative-menu-input');

    const ps = new kakao.maps.services.Places();

    searchBtn.addEventListener('click', () => {
        const keyword = searchInput.value.trim();
        if (!keyword) {
            alert('검색어를 입력하세요.');
            return;
        }

        ps.keywordSearch(keyword, (data, status) => {
            if (status === kakao.maps.services.Status.OK) {
                renderSearchResults(data);
            } else {
                resultsList.innerHTML = '<div class="no-restaurant-results">검색 결과가 없습니다.</div>';
            }
        });
    });

    searchInput.addEventListener('keydown', function (event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            searchBtn.click();
        }
    });

    function renderSearchResults(places) {
        resultsList.innerHTML = '';
        places.forEach(place => {
            const item = document.createElement('div');
            item.className = 'restaurant-result-item';
            item.innerHTML = `
        <div class="restaurant-result-info">
          <div class="restaurant-result-name">${place.place_name}</div>
          <div class="restaurant-result-address">${place.road_address_name || place.address_name}</div>
        </div>
      `;
            item.addEventListener('click', () => selectPlace(place));
            resultsList.appendChild(item);
        });
    }

    function selectPlace(place) {
        const lat = parseFloat(place.y);
        const lng = parseFloat(place.x);
        const position = new kakao.maps.LatLng(lat, lng);

        map.setCenter(position);

        if (!marker) {
            marker = new kakao.maps.Marker({ map });
        }
        marker.setPosition(position);

        addressDisplay.textContent = place.road_address_name || place.address_name;
        document.getElementById('selected-restaurant-name').value = place.place_name;

        selectedRestaurant = {
            name: place.place_name,
            address: place.road_address_name || place.address_name,
            latitude: lat,
            longitude: lng
        };
    }

    document.getElementById('proceed-to-add-restaurant').addEventListener('click', () => {
        if (!selectedRestaurant) {
            alert('식당을 먼저 선택해주세요.');
            return;
        }

        document.getElementById('restaurant-name').value = selectedRestaurant.name;
        document.getElementById('restaurant-address').value = selectedRestaurant.address;
        document.getElementById('restaurant-latitude').value = selectedRestaurant.latitude;
        document.getElementById('restaurant-longitude').value = selectedRestaurant.longitude;
        document.getElementById('restaurant-main-menu').value = repMenuInput.value;

        document.getElementById('restaurant-search-modal').style.display = 'none';
        document.getElementById('restaurantModal').style.display = 'block';
    });

    document.querySelectorAll('#restaurant-search-modal .cancel-btn, #restaurant-search-modal .close-modal')
    .forEach(btn => {
        btn.addEventListener('click', () => {
            document.getElementById('restaurant-search-modal').style.display = 'none';
        });
    });
}

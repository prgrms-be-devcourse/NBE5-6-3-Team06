document.addEventListener('DOMContentLoaded', function() {
    const couponModal = document.getElementById('couponModal');
    const restaurantSearchModal = document.getElementById('restaurantSearchModal');
    const deleteConfirmModal = document.getElementById('deleteConfirmModal');

    const addCouponBtn = document.getElementById('add-coupon-btn');
    const restaurantSelectorBtn = document.getElementById('restaurant-selector-btn');
    const saveCouponBtn = document.getElementById('save-coupon-button');
    const confirmDeleteBtn = document.getElementById('confirm-delete-btn');

    const restaurantSearchInput = document.getElementById('restaurant-search-input');
    const restaurantSearchBtn = document.getElementById('restaurant-search-btn');
    const resultsContainer = document.getElementById('results-container');
    const resultsList = document.getElementById('results-list');
    const resultsCount = document.getElementById('results-count');
    const emptyState = document.getElementById('empty-state');
    const loadingState = document.getElementById('loading-state');
    const noResultsState = document.getElementById('no-results-state');
    const selectedPreview = document.getElementById('selected-preview');
    const previewRemove = document.getElementById('preview-remove');
    const cancelRestaurantBtn = document.getElementById('cancel-restaurant-selection');
    const confirmRestaurantBtn = document.getElementById('confirm-restaurant-selection');

    const couponForm = document.getElementById('couponForm');
    const couponIdInput = document.getElementById('coupon-id');
    const selectedRestaurantIdInput = document.getElementById('selected-restaurant-id');
    const modalTitle = document.getElementById('coupon-modal-title');
    const discountTypeSelect = document.getElementById('coupon-discount-type');
    const discountSuffix = document.getElementById('discount-suffix');
    const discountValueInput = document.getElementById('coupon-discount-value');

    let currentCouponId = null;
    let isEditMode = false;
    let searchTimeout = null;
    let selectedRestaurant = null;
    let restaurantData = [];

    const CouponAPI = {
        getTemplate: (id) => {
            return fetch(`/api/admin/coupons/templates/${id}`);
        },
        createTemplate: (data) => {
            return fetch('/api/admin/coupons/templates', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
        },
        updateTemplate: (id, data) => {
            return fetch(`/api/admin/coupons/templates/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
        },
        deleteTemplate: (id) => {
            return fetch(`/api/admin/coupons/templates/${id}`, {
                method: 'DELETE'
            });
        }
    };

    function openModal(modal) {
        modal.style.display = 'block';
        document.body.classList.add('modal-open');
    }

    function closeModal(modal) {
        modal.style.display = 'none';
        document.body.classList.remove('modal-open');
    }

    async function loadAllRestaurants() {
        showLoadingState();

        try {
            await searchRestaurants('', '');
        } catch (error) {
            console.error('모든 식당 로드 오류:', error);
            showNoResultsState();
        }
    }

    async function searchRestaurants(keyword = '', category = '') {
        showLoadingState();

        try {
            const params = new URLSearchParams();
            if (keyword.trim()) params.append('keyword', keyword);
            if (category.trim()) params.append('category', category);
            params.append('size', '120');

            const response = await fetch(`/admin/restaurant/list?${params.toString()}`);
            const html = await response.text();

            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const restaurantRows = doc.querySelectorAll('.data-table tbody tr');

            restaurantData = [];
            restaurantRows.forEach(row => {
                const cells = row.querySelectorAll('td');
                if (cells.length >= 7) {
                    const editBtn = row.querySelector('.edit');
                    const restaurantId = editBtn?.dataset.id;
                    const ratingElement = cells[6].querySelector('span');

                    if (restaurantId) {
                        restaurantData.push({
                            id: restaurantId,
                            name: cells[1].textContent.trim(),
                            category: cells[2].querySelector('.badge')?.textContent.trim() || '',
                            address: cells[3].textContent.trim(),
                            phone: cells[4].textContent.trim(),
                            mainFood: cells[5].textContent.trim(),
                            rating: parseFloat(ratingElement?.textContent) || 0
                        });
                    }
                }
            });

            displaySearchResults(restaurantData);

        } catch (error) {
            console.error('검색 오류:', error);
            showNoResultsState();
        }
    }

    function displaySearchResults(restaurants) {
        hideAllStates();

        if (restaurants.length === 0) {
            showNoResultsState();
            return;
        }

        resultsCount.textContent = `${restaurants.length}개`;
        resultsList.style.display = 'block';

        const resultsHtml = restaurants.map(restaurant => `
            <div class="restaurant-item" data-restaurant-id="${restaurant.id}">
                <div class="restaurant-item-header">
                    <h4 class="restaurant-item-name">${restaurant.name}</h4>
                    <div class="restaurant-item-rating">
                        ${generateStarRating(restaurant.rating)}
                        <span>${restaurant.rating}</span>
                    </div>
                </div>
                <div class="restaurant-item-info">
                    <span class="restaurant-item-category">${restaurant.category}</span>
                    <span class="restaurant-item-phone">${restaurant.phone}</span>
                </div>
                <p class="restaurant-item-address">${restaurant.address}</p>
            </div>
        `).join('');

        resultsList.innerHTML = resultsHtml;

        resultsList.querySelectorAll('.restaurant-item').forEach(item => {
            item.addEventListener('click', function() {
                const restaurantId = this.dataset.restaurantId;
                const restaurant = restaurants.find(r => r.id === restaurantId);
                selectRestaurant(restaurant);
            });
        });
    }

    function generateStarRating(rating) {
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.5;
        const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

        let stars = '';
        for (let i = 0; i < fullStars; i++) {
            stars += '<i class="fas fa-star"></i>';
        }
        if (hasHalfStar) {
            stars += '<i class="fas fa-star-half-alt"></i>';
        }
        for (let i = 0; i < emptyStars; i++) {
            stars += '<i class="far fa-star"></i>';
        }
        return stars;
    }

    function selectRestaurant(restaurant) {
        selectedRestaurant = restaurant;

        resultsList.querySelectorAll('.restaurant-item').forEach(item => {
            item.classList.remove('selected');
        });

        const selectedItem = resultsList.querySelector(`[data-restaurant-id="${restaurant.id}"]`);
        if (selectedItem) {
            selectedItem.classList.add('selected');
        }

        updateSelectedPreview(restaurant);
        selectedPreview.style.display = 'block';
        confirmRestaurantBtn.disabled = false;
    }

    function updateSelectedPreview(restaurant) {
        selectedPreview.querySelector('.preview-name').textContent = restaurant.name;
        selectedPreview.querySelector('.preview-category').textContent = restaurant.category;
        selectedPreview.querySelector('.preview-stars').innerHTML = generateStarRating(restaurant.rating);
        selectedPreview.querySelector('.preview-score').textContent = restaurant.rating;
        selectedPreview.querySelector('.preview-address').textContent = restaurant.address;
    }

    function clearSelectedRestaurant() {
        selectedRestaurant = null;
        selectedPreview.style.display = 'none';
        confirmRestaurantBtn.disabled = true;

        resultsList.querySelectorAll('.restaurant-item').forEach(item => {
            item.classList.remove('selected');
        });
    }

    function updateRestaurantSelector(restaurant) {
        const placeholder = restaurantSelectorBtn.querySelector('.restaurant-placeholder');
        const nameSpan = restaurantSelectorBtn.querySelector('.selected-restaurant-name');
        const categorySpan = restaurantSelectorBtn.querySelector('.selected-restaurant-category');

        if (restaurant) {
            placeholder.style.display = 'none';
            nameSpan.style.display = 'block';
            categorySpan.style.display = 'block';
            nameSpan.textContent = restaurant.name;
            categorySpan.textContent = restaurant.category;
            restaurantSelectorBtn.classList.add('selected');
            selectedRestaurantIdInput.value = restaurant.id;
        } else {
            placeholder.style.display = 'block';
            nameSpan.style.display = 'none';
            categorySpan.style.display = 'none';
            restaurantSelectorBtn.classList.remove('selected');
            selectedRestaurantIdInput.value = '';
        }
    }

    function showLoadingState() {
        hideAllStates();
        loadingState.style.display = 'flex';
        resultsCount.textContent = '검색 중...';
    }

    function showEmptyState() {
        hideAllStates();
        emptyState.style.display = 'flex';
        resultsCount.textContent = '0개';
        emptyState.querySelector('h4').textContent = '모든 식당 보기';
        emptyState.querySelector('p').textContent = '검색어를 입력하거나 아래에서 식당을 선택하세요';
    }

    function showNoResultsState() {
        hideAllStates();
        noResultsState.style.display = 'flex';
        resultsCount.textContent = '0개';
    }

    function hideAllStates() {
        emptyState.style.display = 'none';
        loadingState.style.display = 'none';
        noResultsState.style.display = 'none';
        resultsList.style.display = 'none';
    }

    function resetForm() {
        couponForm.reset();
        couponIdInput.value = '';
        clearErrors();
        updateDiscountSuffix();
        updateRestaurantSelector(null);
        clearSelectedRestaurant();
        isEditMode = false;
        currentCouponId = null;

        restaurantSelectorBtn.disabled = false;
        document.getElementById('coupon-start-at').disabled = false;
    }

    function clearErrors() {
        document.querySelectorAll('.error-message').forEach(element => {
            element.textContent = '';
        });
    }

    function updateDiscountSuffix() {
        const discountType = discountTypeSelect.value;
        if (discountType === 'PERCENTAGE') {
            discountSuffix.textContent = '%';
            discountValueInput.max = '100';
            discountValueInput.placeholder = '1-100';
        } else if (discountType === 'FIXED_AMOUNT') {
            discountSuffix.textContent = '원';
            discountValueInput.max = '';
            discountValueInput.placeholder = '할인 금액';
        } else {
            discountSuffix.textContent = '%';
            discountValueInput.max = '';
            discountValueInput.placeholder = '';
        }
    }

    function getFormData() {
        const restaurantId = selectedRestaurantIdInput.value;
        const discountValue = document.getElementById('coupon-discount-value').value;
        const totalQuantity = document.getElementById('coupon-total-quantity').value;
        const validDays = document.getElementById('coupon-valid-days').value;

        return {
            restaurantId: restaurantId ? parseInt(restaurantId) : null,
            name: document.getElementById('coupon-name').value,
            description: document.getElementById('coupon-description').value,
            discountType: document.getElementById('coupon-discount-type').value,
            discountValue: discountValue ? parseInt(discountValue) : null,
            totalQuantity: totalQuantity ? parseInt(totalQuantity) : null,
            validDays: validDays ? parseInt(validDays) : null,
            startAt: document.getElementById('coupon-start-at').value,
            endAt: document.getElementById('coupon-end-at').value
        };
    }

    function getUpdateFormData() {
        const discountValue = document.getElementById('coupon-discount-value').value;
        const totalQuantity = document.getElementById('coupon-total-quantity').value;
        const validDays = document.getElementById('coupon-valid-days').value;

        return {
            name: document.getElementById('coupon-name').value,
            description: document.getElementById('coupon-description').value,
            discountType: document.getElementById('coupon-discount-type').value,
            discountValue: discountValue ? parseInt(discountValue) : null,
            totalQuantity: totalQuantity ? parseInt(totalQuantity) : null,
            validDays: validDays ? parseInt(validDays) : null,
            endAt: document.getElementById('coupon-end-at').value
        };
    }

    function setFormData(data) {
        document.getElementById('coupon-name').value = data.name || '';
        document.getElementById('coupon-description').value = data.description || '';
        document.getElementById('coupon-discount-type').value = data.discountType || '';
        document.getElementById('coupon-discount-value').value = data.discountValue || '';
        document.getElementById('coupon-total-quantity').value = data.totalQuantity || '';
        document.getElementById('coupon-valid-days').value = data.validDays || '';

        if (data.startAt) {
            document.getElementById('coupon-start-at').value = formatDateTimeForInput(data.startAt);
            if (isEditMode) {
                document.getElementById('coupon-start-at').disabled = true;
            }
        }
        if (data.endAt) {
            document.getElementById('coupon-end-at').value = formatDateTimeForInput(data.endAt);
        }

        if (data.restaurant) {
            const restaurant = {
                id: data.restaurant.restaurantId || data.restaurant.id,
                name: data.restaurant.name,
                category: data.restaurant.category
            };
            updateRestaurantSelector(restaurant);

            if (isEditMode) {
                restaurantSelectorBtn.disabled = true;
            }
        }

        updateDiscountSuffix();
    }

    function formatDateTimeForInput(isoString) {
        const date = new Date(isoString);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day}T${hours}:${minutes}`;
    }

    function validateForm() {
        clearErrors();
        let isValid = true;
        const data = getFormData();

        if (!data.restaurantId) {
            showError('restaurant', '식당을 선택해주세요.');
            isValid = false;
        }

        if (!data.name.trim()) {
            showError('name', '쿠폰명을 입력해주세요.');
            isValid = false;
        }

        if (!data.discountType) {
            showError('discount-type', '할인 타입을 선택해주세요.');
            isValid = false;
        }

        if (!data.discountValue || data.discountValue < 1) {
            showError('discount-value', '할인 값을 올바르게 입력해주세요.');
            isValid = false;
        }

        if (data.discountType === 'PERCENTAGE' && data.discountValue > 100) {
            showError('discount-value', '퍼센트 할인은 100% 이하여야 합니다.');
            isValid = false;
        }

        if (!data.totalQuantity || data.totalQuantity < 1) {
            showError('total-quantity', '총 발급 수량을 올바르게 입력해주세요.');
            isValid = false;
        }

        if (!data.validDays || data.validDays < 1) {
            showError('valid-days', '유효 기간을 올바르게 입력해주세요.');
            isValid = false;
        }

        if (!data.startAt) {
            showError('start-at', '발급 시작일시를 입력해주세요.');
            isValid = false;
        }

        if (!data.endAt) {
            showError('end-at', '발급 종료일시를 입력해주세요.');
            isValid = false;
        }

        if (data.startAt && data.endAt) {
            const startDate = new Date(data.startAt);
            const endDate = new Date(data.endAt);

            if (startDate >= endDate) {
                showError('end-at', '발급 종료일시는 시작일시보다 늦어야 합니다.');
                isValid = false;
            }
        }

        return isValid;
    }

    function showError(fieldName, message) {
        const errorElement = document.getElementById(`error-${fieldName}`);
        if (errorElement) {
            errorElement.textContent = message;
        }
    }

    addCouponBtn.addEventListener('click', function() {
        resetForm();
        modalTitle.textContent = '새 쿠폰 템플릿 추가';
        openModal(couponModal);
    });

    restaurantSelectorBtn.addEventListener('click', function() {
        if (!this.disabled) {
            clearSelectedRestaurant();
            restaurantSearchInput.value = '';
            openModal(restaurantSearchModal);

            loadAllRestaurants();
        }
    });

    restaurantSearchInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            const keyword = this.value.trim();
            searchRestaurants(keyword);
        }, 300);
    });

    restaurantSearchBtn.addEventListener('click', function() {
        const keyword = restaurantSearchInput.value.trim();
        searchRestaurants(keyword);
    });

    confirmRestaurantBtn.addEventListener('click', function() {
        if (selectedRestaurant) {
            updateRestaurantSelector(selectedRestaurant);
            closeModal(restaurantSearchModal);
            clearErrors();
        }
    });

    cancelRestaurantBtn.addEventListener('click', function() {
        closeModal(restaurantSearchModal);
    });

    previewRemove.addEventListener('click', function(e) {
        e.stopPropagation();
        clearSelectedRestaurant();
    });

    discountTypeSelect.addEventListener('change', updateDiscountSuffix);

    document.addEventListener('click', function(e) {
        if (e.target.closest('.edit')) {
            const couponId = e.target.closest('.edit').dataset.id;
            editCoupon(couponId);
        }
        if (e.target.closest('.delete')) {
            const couponId = e.target.closest('.delete').dataset.id;
            currentCouponId = couponId;
            openModal(deleteConfirmModal);
        }
        if (e.target.classList.contains('close-modal') || e.target.classList.contains('cancel-btn')) {
            closeModal(couponModal);
            closeModal(restaurantSearchModal);
            closeModal(deleteConfirmModal);
        }
    });

    window.addEventListener('click', function(e) {
        if (e.target === couponModal) closeModal(couponModal);
        if (e.target === restaurantSearchModal) closeModal(restaurantSearchModal);
        if (e.target === deleteConfirmModal) closeModal(deleteConfirmModal);
    });

    saveCouponBtn.addEventListener('click', function() {
        if (validateForm()) {
            saveCoupon();
        }
    });

    confirmDeleteBtn.addEventListener('click', function() {
        if (currentCouponId) {
            deleteCoupon(currentCouponId);
        }
    });

    // 🎨 CRUD 함수들
    async function editCoupon(couponId) {
        try {
            const response = await CouponAPI.getTemplate(couponId);
            const result = await response.json();

            if (response.ok && (result.success || result.code === '0000')) {
                const couponData = result.data;
                isEditMode = true;
                currentCouponId = couponId;
                couponIdInput.value = couponId;
                modalTitle.textContent = '쿠폰 템플릿 수정';

                setFormData(couponData);
                openModal(couponModal);
            } else {
                showToast(result.message || '쿠폰 정보를 불러오는데 실패했습니다.', 'error');
            }
        } catch (error) {
            console.error('Error fetching coupon:', error);
            showToast('쿠폰 정보를 불러오는데 실패했습니다.', 'error');
        }
    }

    async function saveCoupon() {
        let formData;

        if (isEditMode && currentCouponId) {
            formData = getUpdateFormData();
        } else {
            formData = getFormData();
        }

        try {
            let response;
            if (isEditMode && currentCouponId) {
                response = await CouponAPI.updateTemplate(currentCouponId, formData);
            } else {
                response = await CouponAPI.createTemplate(formData);
            }

            const result = await response.json();

            if (response.ok && (result.success || result.code === '0000')) {
                const successMessage = isEditMode ? '쿠폰 템플릿이 수정되었습니다.' : '쿠폰 템플릿이 생성되었습니다.';
                showToast(successMessage, 'success');
                closeModal(couponModal);
                window.location.reload();
            } else {
                if (result.errors) {
                    Object.keys(result.errors).forEach(field => {
                        showError(field, result.errors[field]);
                    });
                } else {
                    showToast(result.message || '저장에 실패했습니다.', 'error');
                }
            }
        } catch (error) {
            console.error('Error saving coupon:', error);
            showToast('저장에 실패했습니다.', 'error');
        }
    }

    async function deleteCoupon(couponId) {
        try {
            const response = await CouponAPI.deleteTemplate(couponId);
            const result = await response.json();

            if (response.ok && result.success) {
                showToast('쿠폰 템플릿이 삭제되었습니다.', 'success');
                closeModal(deleteConfirmModal);
                window.location.reload();
            } else {
                showToast(result.message || '삭제에 실패했습니다.', 'error');
            }
        } catch (error) {
            console.error('Error deleting coupon:', error);
            showToast('삭제에 실패했습니다.', 'error');
        }
    }

    updateDiscountSuffix();
    showEmptyState();
});
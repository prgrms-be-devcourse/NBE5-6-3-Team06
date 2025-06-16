document.addEventListener('DOMContentLoaded', function() {
    const couponModal = document.getElementById('couponModal');
    const deleteConfirmModal = document.getElementById('deleteConfirmModal');
    const addCouponBtn = document.getElementById('add-coupon-btn');
    const saveCouponBtn = document.getElementById('save-coupon-button');
    const confirmDeleteBtn = document.getElementById('confirm-delete-btn');

    const couponForm = document.getElementById('couponForm');
    const couponIdInput = document.getElementById('coupon-id');
    const modalTitle = document.getElementById('coupon-modal-title');

    const discountTypeSelect = document.getElementById('coupon-discount-type');
    const discountSuffix = document.getElementById('discount-suffix');
    const discountValueInput = document.getElementById('coupon-discount-value');

    let currentCouponId = null;
    let isEditMode = false;

    const CouponAPI = {
        getTemplate: (id) => {
            return fetch(`/api/admin/coupons/templates/${id}`, {
            });
        },

        createTemplate: (data) => {
            return fetch('/api/admin/coupons/templates', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data)
            });
        },

        updateTemplate: (id, data) => {
            return fetch(`/api/admin/coupons/templates/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data)
            });
        },

        deleteTemplate: (id) => {
            return fetch(`/api/admin/coupons/templates/${id}`, {
                method: 'DELETE',
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

    function resetForm() {
        couponForm.reset();
        couponIdInput.value = '';
        clearErrors();
        updateDiscountSuffix();
        isEditMode = false;
        currentCouponId = null;

        document.getElementById('coupon-restaurant').disabled = false;
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
        const restaurantId = document.getElementById('coupon-restaurant').value;
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
        if (data.restaurantId) {
            document.getElementById('coupon-restaurant').value = data.restaurantId;
            if (isEditMode) {
                document.getElementById('coupon-restaurant').disabled = true;
            }
        } else {
            console.warn('restaurantId is missing in data:', data);
        }

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

        // 퍼센트 할인인 경우 100% 이하 검증
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

    discountTypeSelect.addEventListener('change', updateDiscountSuffix);

    document.addEventListener('click', function(e) {
        if (e.target.closest('.edit')) {
            const couponId = e.target.closest('.edit').dataset.id;
            editCoupon(couponId);
        }
    });

    document.addEventListener('click', function(e) {
        if (e.target.closest('.delete')) {
            const couponId = e.target.closest('.delete').dataset.id;
            currentCouponId = couponId;
            openModal(deleteConfirmModal);
        }
    });

    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('close-modal') || e.target.classList.contains('cancel-btn')) {
            closeModal(couponModal);
            closeModal(deleteConfirmModal);
        }
    });

    window.addEventListener('click', function(e) {
        if (e.target === couponModal) {
            closeModal(couponModal);
        }
        if (e.target === deleteConfirmModal) {
            closeModal(deleteConfirmModal);
        }
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

    function addCouponToTable(couponData) {
        const tbody = document.querySelector('.data-table tbody');
        const newRow = createCouponRow(couponData, tbody.children.length + 1);
        tbody.appendChild(newRow);
    }

    function updateCouponInTable(couponData) {
        const row = document.querySelector(`tr[data-coupon-id="${couponData.id}"]`);
        if (row) {
            const newRow = createCouponRow(couponData, getRowNumber(row));
            row.replaceWith(newRow);
        }
    }

    function removeCouponFromTable(couponId) {
        const row = document.querySelector(`tr[data-coupon-id="${couponId}"]`);
        if (row) {
            row.remove();
            updateRowNumbers();
        }
    }

    function createCouponRow(coupon, rowNumber) {
        const row = document.createElement('tr');
        row.setAttribute('data-coupon-id', coupon.id);

        const discountSuffix = coupon.discountType === 'PERCENTAGE' ? '%' : '원';
        const badgeClass = coupon.discountType === 'PERCENTAGE' ? 'badge-blue' : 'badge-green';
        const progressPercent = coupon.totalQuantity > 0 ? (coupon.issuedQuantity * 100 / coupon.totalQuantity) : 0;

        row.innerHTML = `
            <td>${rowNumber}</td>
            <td>${coupon.name}</td>
            <td>${coupon.restaurantName}</td>
            <td>
                <span class="badge ${badgeClass}">${coupon.discountType}</span>
            </td>
            <td>
                <span>${coupon.discountValue}</span>
                <span>${discountSuffix}</span>
            </td>
            <td>
                <span>${coupon.issuedQuantity}</span> / 
                <span>${coupon.totalQuantity}</span>
                <div class="progress-bar">
                    <div class="progress-fill" style="width: ${progressPercent}%"></div>
                </div>
            </td>
            <td>
                <div class="date-range">
                    <div>${formatDateTime(coupon.startAt)}</div>
                    <div>~</div>
                    <div>${formatDateTime(coupon.endAt)}</div>
                </div>
            </td>
            <td>
                <span class="status ${coupon.status.toLowerCase()}">${coupon.status}</span>
            </td>
            <td>
                <button class="action-btn edit" data-id="${coupon.id}">
                    <i class="fas fa-edit"></i>
                </button>
                ${coupon.status !== 'DELETED' ? `
                <button class="action-btn delete" data-id="${coupon.id}">
                    <i class="fas fa-trash"></i>
                </button>
                ` : ''}
            </td>
        `;

        return row;
    }

    function formatDateTime(dateTimeString) {
        const date = new Date(dateTimeString);
        return date.toLocaleDateString('ko-KR') + ' ' +
            date.toLocaleTimeString('ko-KR', {hour: '2-digit', minute: '2-digit'});
    }

    function getRowNumber(row) {
        return parseInt(row.cells[0].textContent);
    }

    function updateRowNumbers() {
        const rows = document.querySelectorAll('.data-table tbody tr');
        rows.forEach((row, index) => {
            row.cells[0].textContent = index + 1;
        });
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

                if (isEditMode) {
                    updateCouponInTable(result.data);
                } else {
                    addCouponToTable(result.data);
                }
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

                removeCouponFromTable(couponId);
            } else {
                showToast(result.message || '삭제에 실패했습니다.', 'error');
            }
        } catch (error) {
            console.error('Error deleting coupon:', error);
            showToast('삭제에 실패했습니다.', 'error');
        }
    }

    updateDiscountSuffix();
});
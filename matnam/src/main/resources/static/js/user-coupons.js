document.addEventListener('DOMContentLoaded', function() {
    if (!auth.isLoggedIn()) {
        window.location.href = '/user/signin';
        return;
    }

    const tabs = document.querySelectorAll('.coupon-tab');
    const contents = document.querySelectorAll('.coupon-content');

    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const targetTab = this.dataset.tab;

            tabs.forEach(t => t.classList.remove('active'));
            contents.forEach(c => c.classList.remove('active'));

            this.classList.add('active');
            document.getElementById(targetTab + '-content').classList.add('active');
        });
    });

    // QR 버튼에 이벤트 리스너 추가
    const qrButtons = document.querySelectorAll('.show-qr-btn');
    qrButtons.forEach(button => {
        button.addEventListener('click', function() {
            const couponCode = this.dataset.couponCode;
            const restaurantName = this.dataset.restaurantName;
            const couponName = this.dataset.couponName;
            const discountValue = this.dataset.discountValue;
            const discountType = this.dataset.discountType;

            showQRCode(couponCode, restaurantName, couponName, discountValue, discountType);
        });
    });

    window.addEventListener('click', function(event) {
        const modal = document.getElementById('qrModal');
        if (event.target === modal) {
            closeQRModal();
        }
    });
});

let currentCouponCode = null;

function showQRCode(couponCode, restaurantName, couponName, discountValue, discountType) {
    const modal = document.getElementById('qrModal');
    const qrDisplay = document.getElementById('qrCodeDisplay');
    const couponDetails = document.getElementById('couponDetails');

    currentCouponCode = couponCode;

    // QR 코드 생성 (실제로는 QR 라이브러리 사용) - 나중에
    // 여기서는 시각적 표현만 제공
    qrDisplay.innerHTML = `
        <div style="
            width: 200px; 
            height: 200px; 
            background: url('https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(couponCode)}') center/cover;
            border-radius: 8px;
        "></div>
    `;

    const discountText = discountType === 'PERCENTAGE' ? discountValue + '%' : discountValue + '원';
    couponDetails.innerHTML = `
        <div class="detail-row">
            <strong>식당명:</strong>
            <span>${restaurantName}</span>
        </div>
        <div class="detail-row">
            <strong>쿠폰명:</strong>
            <span>${couponName}</span>
        </div>
        <div class="detail-row">
            <strong>할인 혜택:</strong>
            <span>${discountText} 할인</span>
        </div>
        <div class="detail-row">
            <strong>쿠폰 코드:</strong>
            <span style="font-family: 'Courier New', monospace; font-weight: bold;">${couponCode}</span>
        </div>
    `;

    modal.style.display = 'block';
    document.body.style.overflow = 'hidden';
}

function closeQRModal() {
    const modal = document.getElementById('qrModal');
    modal.style.display = 'none';
    document.body.style.overflow = 'auto';
    currentCouponCode = null;
}

async function useCoupon() {
    if (!currentCouponCode) {
        showToast('쿠폰 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    if (!confirm('정말 이 쿠폰을 사용하시겠습니까?\n사용 후에는 취소할 수 없습니다.')) {
        return;
    }

    const useButton = document.querySelector('.use-coupon-btn');
    const originalText = useButton.innerHTML;
    useButton.disabled = true;
    useButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 사용 중...';

    const couponCodeToUpdate = currentCouponCode;

    try {
        console.log('쿠폰 사용 API 호출:', couponCodeToUpdate);

        const response = await fetch(`/api/user/coupons/${couponCodeToUpdate}/use`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (response.ok && result.success !== false) {
            showToast('쿠폰이 성공적으로 사용되었습니다!', 'success');
            closeQRModal();
            updateCouponCardToUsed(couponCodeToUpdate);
            updateTabCounts();
        } else {
            const errorMessage = result.message || result.error || '쿠폰 사용에 실패했습니다.';
            showToast(errorMessage, 'error');

            useButton.disabled = false;
            useButton.innerHTML = originalText;
        }
    } catch (error) {
        showToast('네트워크 오류가 발생했습니다.', 'error');

        useButton.disabled = false;
        useButton.innerHTML = originalText;
    }
}

function updateCouponCardToUsed(couponCode) {
    const couponCards = document.querySelectorAll(`[data-coupon-id="${couponCode}"]`);

    if (couponCards.length === 0) {
        return;
    }

    couponCards.forEach((card, index) => {
        card.classList.remove('issued', 'available');
        card.classList.add('used');
        card.style.background = 'linear-gradient(135deg, #bdc3c7 0%, #95a5a6 100%)';

        const statusBadge = card.querySelector('.status-badge');
        if (statusBadge) {
            statusBadge.className = 'status-badge used';
            statusBadge.textContent = '사용완료';
        } else {
            console.error('상태 배지를 찾을 수 없습니다');
        }

        const button = card.querySelector('.qr-button');
        if (button) {
            button.disabled = true;
            button.innerHTML = '<i class="fas fa-check"></i> 사용 완료';
            button.classList.remove('show-qr-btn');
            button.removeEventListener('click', function() {});
        } else {
            console.error('버튼을 찾을 수 없습니다');
        }
    });
}

function updateTabCounts() {
    const allCards = document.querySelectorAll('.coupon-card');
    const availableCards = document.querySelectorAll('.coupon-card.issued, .coupon-card.available');
    const usedCards = document.querySelectorAll('.coupon-card.used');
    const expiredCards = document.querySelectorAll('.coupon-card.expired');

    const tabs = document.querySelectorAll('.coupon-tab');
    tabs.forEach(tab => {
        const tabType = tab.dataset.tab;
        const countElement = tab.querySelector('.count');

        if (!countElement) {
            return;
        }

        let newCount = 0;
        switch(tabType) {
            case 'all':
                newCount = allCards.length;
                break;
            case 'available':
                newCount = availableCards.length;
                break;
            case 'used':
                newCount = usedCards.length;
                break;
            case 'expired':
                newCount = expiredCards.length;
                break;
        }

        countElement.textContent = newCount;
    });
}

async function applyCoupon(templateId) {
    try {
        const response = await fetch(`/api/user/coupons/${templateId}/apply`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();
        const isSuccess = response.ok && (result.success || result.code === '0000');

        if (isSuccess) {
            showToast('쿠폰 신청이 완료되었습니다!', 'success');
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            const errorMessage = result.message || result.error || '쿠폰 신청에 실패했습니다.';
            showToast(errorMessage, 'error');
        }
    } catch (error) {
        showToast('네트워크 오류가 발생했습니다.', 'error');
    }
}

document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeQRModal();
    }
});
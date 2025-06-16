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

function showQRCode(couponCode, restaurantName, couponName, discountValue, discountType) {
    const modal = document.getElementById('qrModal');
    const qrDisplay = document.getElementById('qrCodeDisplay');
    const couponDetails = document.getElementById('couponDetails');

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
}

async function applyCoupon(templateId) {
    try {
        console.log('Applying coupon for template ID:', templateId);

        const response = await fetch(`/api/user/coupons/${templateId}/apply`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();
        console.log('Apply coupon response:', result);
        console.log('Response status:', response.status);
        console.log('Response ok:', response.ok);

        const isSuccess = response.ok && (result.success || result.code === '0000');
        console.log('Is success?', isSuccess);
        console.log('result.success:', result.success);
        console.log('result.code:', result.code);

        if (isSuccess) {
            console.log('Showing success toast');
            showToast('쿠폰 신청이 완료되었습니다!', 'success');

            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            console.log('Showing error toast');
            const errorMessage = result.message || result.error || '쿠폰 신청에 실패했습니다.';
            console.log('Error message:', errorMessage);
            showToast(errorMessage, 'error');
        }
    } catch (error) {
        console.error('Error applying coupon:', error);
        showToast('네트워크 오류가 발생했습니다.', 'error');
    }
}

document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeQRModal();
    }
});
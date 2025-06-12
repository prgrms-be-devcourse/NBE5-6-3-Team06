(function(global) {
    const auth = {
        isLoggedIn() {
            return this.getCookie('ACCESS_TOKEN') !== null && this.getCookie('REFRESH_TOKEN') !== null;
        },

        getUserInfo() {
            if (!this.isLoggedIn()) return null;

            return {
                userId: this.getCurrentUserId(),
                nickname: this.getNickname(),
                role: this.getUserRole(),
                accessToken: this.getCookie('ACCESS_TOKEN'),
                refreshToken: this.getCookie('REFRESH_TOKEN')
            };
        },

        getTokenPayload() {
            const token = this.getAccessToken();
            if (!token) return null;

            try {
                const base64Url = token.split('.')[1];
                const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
                const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));

                return JSON.parse(jsonPayload);
            } catch (error) {
                console.error('JWT 파싱 실패:', error);
                return null;
            }
        },

        getNickname() {
            const payload = this.getTokenPayload();
            return payload ? payload.nickname : null;
        },

        getCurrentUserId() {
            const payload = this.getTokenPayload();
            return payload ? payload.sub : null;
        },

        getUserRole() {
            const payload = this.getTokenPayload();
            return payload ? payload.role : null;
        },

        getAccessToken() {
            return this.getCookie('ACCESS_TOKEN');
        },

        getCookie(name) {
            const nameEQ = name + "=";
            const ca = document.cookie.split(';');
            for (let i = 0; i < ca.length; i++) {
                let c = ca[i];
                while (c.charAt(0) === ' ') c = c.substring(1, c.length);
                if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
            }
            return null;
        },

        eraseCookie(name) {
            document.cookie = name + '=; Max-Age=-99999999; path=/; SameSite=Lax';
        },

        async logout(redirectUrl = '/') {
            try {
                const response = await fetch('/api/auth/logout', {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${this.getAccessToken()}`
                    }
                });
            } catch (error) {
                console.error('로그아웃 요청 중 오류:', error);
            } finally {
                this.clearAllTokens();
                window.location.href = redirectUrl;
            }
        },

        clearAllTokens() {
            this.eraseCookie('ACCESS_TOKEN');
            this.eraseCookie('REFRESH_TOKEN');
        },

        getAuthHeaders() {
            const token = this.getAccessToken();
            return token ? { 'Authorization': `Bearer ${token}` } : {};
        },

        async fetchWithAuth(url, options = {}) {
            const headers = {
                ...options.headers,
                ...this.getAuthHeaders()
            };

            let response = await fetch(url, {
                ...options,
                headers,
                credentials: 'include'
            });

            if (response.status === 4001) {
                console.log('Access Token 만료, 갱신 시도');

                const refreshSuccess = await this.refreshAccessToken();

                if (refreshSuccess) {
                    console.log('토큰 갱신 성공, 요청 재시도');

                    const newHeaders = {
                        ...options.headers,
                        ...this.getAuthHeaders()
                    };

                    response = await fetch(url, {
                        ...options,
                        headers: newHeaders,
                        credentials: 'include'
                    });
                } else {
                    console.log('토큰 갱신 실패, 로그아웃');
                    this.logout('/user/signin');
                    return null;
                }
            }

            return response;
        },

        async refreshAccessToken() {
            try {
                const response = await fetch('/api/auth/refresh', {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });

                if (!response.ok) {
                    console.error('토큰 갱신 실패:', response.status);
                    return false;
                }

            } catch (error) {
                console.error('토큰 갱신 중 오류:', error);
                return false;
            }
        },

        async validateToken() {
            if (!this.isLoggedIn()) {
                return false;
            }

            try {
                const response = await this.fetchWithAuth('/api/user/mypage');
                return response && response.ok;
            } catch (error) {
                console.error('토큰 검증 중 오류:', error);
                return false;
            }
        }
    };

    if (!global.auth) {
        global.auth = auth;
    }

    document.addEventListener('DOMContentLoaded', function() {
        if (auth.isLoggedIn()) {
            console.log('로그인 상태 확인됨');

            setInterval(() => {
                auth.validateToken().then(isValid => {
                    if (!isValid) {
                        console.log('토큰이 유효하지 않음, 로그아웃 처리');
                        auth.logout();
                    }
                });
            }, 5 * 60 * 1000);
        }
    });

})(window);
(function(global) {
    const auth = {
        isLoggedIn() {
            return this.getCookie('ACCESS_TOKEN') !== null || this.getCookie('REFRESH_TOKEN') !== null;
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
                return null;
            }
        },

        isTokenExpired() {
            const payload = this.getTokenPayload();
            if (!payload || !payload.exp) return true;

            const currentTime = Math.floor(Date.now() / 1000);
            return payload.exp < currentTime;
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

        getRefreshToken() {
            return this.getCookie('REFRESH_TOKEN');
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
            if (!this.getAccessToken() || this.isTokenExpired()) {
                const refreshSuccess = await this.refreshAccessToken();

                if (!refreshSuccess) {
                    this.logout('/user/signin');
                    return null;
                }
            }

            const headers = {
                ...options.headers,
                ...this.getAuthHeaders()
            };

            let response = await fetch(url, {
                ...options,
                headers,
                credentials: 'include'
            });

            if (response.status === 401) {

                const refreshSuccess = await this.refreshAccessToken();

                if (refreshSuccess) {

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
                    this.logout('/user/signin');
                    return null;
                }
            }

            return response;
        },

        async refreshAccessToken() {
            try {
                if (!this.getRefreshToken()) {
                    return false;
                }

                const response = await fetch('/api/auth/refresh', {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });

                if (response.ok) {
                    return true;
                } else {
                    return false;
                }

            } catch (error) {
                return false;
            }
        },

        async validateToken() {
            if (!this.isLoggedIn()) {
                return false;
            }

            try {
                const response = await this.fetchWithAuth('/api/auth/validate');
                return response && response.ok;
            } catch (error) {
                return false;
            }
        },

        async checkAndRefreshToken() {
            if (!this.isLoggedIn()) {
                return false;
            }

            if (!this.getAccessToken() || this.isTokenExpired()) {
                return await this.refreshAccessToken();
            }

            return true;
        }
    };

    if (!global.auth) {
        global.auth = auth;
    }

    document.addEventListener('DOMContentLoaded', async function() {
        if (auth.isLoggedIn()) {

            const tokenValid = await auth.checkAndRefreshToken();

            if (!tokenValid) {
                auth.logout('/user/signin');
                return;
            }

            setInterval(async () => {
                const isValid = await auth.validateToken();
                if (!isValid) {
                    auth.logout('/user/signin');
                }
            }, 10 * 60 * 1000);
        }
    });

})(window);
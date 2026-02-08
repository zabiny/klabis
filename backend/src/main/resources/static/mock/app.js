// Shared JavaScript functions for Klabis UI Mockup with OAuth2 Backend Integration

/**
 * Show a toast notification
 * @param {string} message - The message to display
 * @param {string} type - The type of toast (success, error, info)
 */
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse';
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, 3000);
}

/**
 * Check authentication on page load
 * With OAuth2 Authorization Code flow, we check if we have a valid token
 */
async function checkAuth() {
    const isAuthenticated = sessionStorage.getItem('isAuthenticated') === 'true';
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';

    // Allow login and callback pages without auth
    if (currentPage === 'login.html' || currentPage === 'callback.html') {
        if (isAuthenticated && currentPage === 'login.html') {
            window.location.href = '/index.html';
        }
        return;
    }

    // For protected pages, verify we can get a token
    if (!isAuthenticated) {
        window.location.href = '/auth/login.html';
        return;
    }

    // Verify token is still valid by attempting to fetch one
    try {
        await apiClient.getAccessToken();
    } catch (error) {
        console.error('Auth check failed:', error);
        sessionStorage.removeItem('isAuthenticated');
        window.location.href = '/auth/login.html';
    }
}

/**
 * Handle logout
 */
function handleLogout() {
    if (confirm('Opravdu se chcete odhlásit?')) {
        sessionStorage.removeItem('isAuthenticated');
        sessionStorage.removeItem('clientId');
        sessionStorage.removeItem('backendUrl');
        apiClient.clearTokens();
        showToast('Byli jste odhlášeni', 'success');
        setTimeout(() => {
            window.location.href = '/auth/login.html';
        }, 500);
    }
}

/**
 * Format date to Czech locale
 * @param {Date|string} date - The date to format
 * @returns {string} Formatted date string
 */
function formatDate(date) {
    if (typeof date === 'string') {
        date = new Date(date);
    }
    return date.toLocaleDateString('cs-CZ');
}

/**
 * Format date and time to Czech locale
 * @param {Date|string} dateTime - The datetime to format
 * @returns {string} Formatted datetime string
 */
function formatDateTime(dateTime) {
    if (typeof dateTime === 'string') {
        dateTime = new Date(dateTime);
    }
    return dateTime.toLocaleString('cs-CZ');
}

/**
 * Get gender text in Czech
 * @param {string} gender - Gender code (MALE, FEMALE, OTHER)
 * @returns {string} Czech gender text
 */
function getGenderText(gender) {
    const genderMap = {
        'MALE': 'Muž',
        'FEMALE': 'Žena',
        'OTHER': 'Jiné'
    };
    return genderMap[gender] || gender;
}

/**
 * Get nationality text in Czech from ISO code
 * @param {string} code - ISO 3166-1 alpha-2 code
 * @returns {string} Czech nationality text
 */
function getNationalityText(code) {
    const nationalityMap = {
        'CZ': 'Česká republika',
        'SK': 'Slovensko',
        'AT': 'Rakousko',
        'DE': 'Německo',
        'PL': 'Polsko'
    };
    return nationalityMap[code] || code;
}

/**
 * Convert ISO 3166-1 alpha-2 country code to Czech text
 * @param {string} code - ISO country code (e.g., "CZ")
 * @returns {string} Czech country name
 */
function getCountryText(code) {
    const countries = {
        'CZ': 'Česká republika',
        'SK': 'Slovensko',
        'AT': 'Rakousko',
        'DE': 'Německo',
        'PL': 'Polsko'
    };
    return countries[code] || code;
}

/**
 * Get initials from first and last name
 * @param {string} firstName - First name
 * @param {string} lastName - Last name
 * @returns {string} Two-letter initials
 */
function getInitials(firstName, lastName) {
    if (!firstName || !lastName) return '?';
    return (firstName[0] + lastName[0]).toUpperCase();
}

/**
 * Calculate age from birth date
 * @param {Date} birthDate - Birth date
 * @returns {number} Age in years
 */
function calculateAge(birthDate) {
    const today = new Date();
    const ageDifMs = today - birthDate.getTime();
    const ageDate = new Date(ageDifMs);
    return Math.abs(ageDate.getUTCFullYear() - 1970);
}

/**
 * Validate Czech birth number (rodné číslo)
 * @param {string} rc - Birth number to validate
 * @returns {boolean} True if valid
 */
function validateCzechBirthNumber(rc) {
    // Basic validation for demo purposes
    // Full validation would be more complex
    const rcPattern = /^\d{9,10}$/;
    return rcPattern.test(rc);
}

/**
 * Validate email address
 * @param {string} email - Email to validate
 * @returns {boolean} True if valid
 */
function validateEmail(email) {
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailPattern.test(email);
}

/**
 * Validate Czech phone number
 * @param {string} phone - Phone number to validate
 * @returns {boolean} True if valid
 */
function validateCzechPhone(phone) {
    // Accepts formats: +420 XXX XXX XXX, XXX XXX XXX, XXXXXXXXX
    const phonePattern = /^(\+420)?\s?\d{3}\s?\d{3}\s?\d{3}$/;
    return phonePattern.test(phone);
}

/**
 * Format phone number for display
 * @param {string} phone - Phone number to format
 * @returns {string} Formatted phone number
 */
function formatPhoneNumber(phone) {
    const cleaned = phone.replace(/\D/g, '');
    if (cleaned.length === 9) {
        return `${cleaned.slice(0, 3)} ${cleaned.slice(3, 6)} ${cleaned.slice(6)}`;
    } else if (cleaned.length === 12 && cleaned.startsWith('420')) {
        return `+420 ${cleaned.slice(3, 6)} ${cleaned.slice(6, 9)} ${cleaned.slice(9)}`;
    }
    return phone;
}

/**
 * Debounce function for search inputs
 * @param {Function} func - Function to debounce
 * @param {number} wait - Wait time in milliseconds
 * @returns {Function} Debounced function
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Handle API errors and show appropriate messages
 * @param {ApiError} error - The API error
 */
function handleApiError(error) {
    if (error.isUnauthorized()) {
        showToast('Neoprávněný přístup. Prosím přihlašte se znovu.', 'error');
        sessionStorage.removeItem('isAuthenticated');
        sessionStorage.removeItem('clientId');
        sessionStorage.removeItem('backendUrl');
        apiClient.clearTokens();
        setTimeout(() => {
            window.location.href = '/auth/login.html';
        }, 1500);
    } else if (error.isForbidden()) {
        showToast('Nemáte dostatečná oprávnění pro tuto akci.', 'error');
    } else if (error.isNotFound()) {
        showToast('Záznam nebyl nalezen.', 'error');
    } else if (error.isValidationError()) {
        showToast(`Validační chyba: ${error.getErrorMessage()}`, 'error');
    } else if (error.isConflict()) {
        showToast(error.getErrorMessage(), 'error');
    } else {
        showToast(`Chyba: ${error.getErrorMessage()}`, 'error');
    }
}

// Initialize auth check on page load
document.addEventListener('DOMContentLoaded', async () => {
    await checkAuth();

    // Add active class to current navigation item
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('.nav-item').forEach(item => {
        const href = item.getAttribute('href');
        if (href === currentPage) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });

    // Update user info in sidebar
    const userAvatar = document.querySelector('.user-avatar');
    const userName = document.querySelector('.user-name');
    const userRegNumber = document.querySelector('.user-reg-number');

    if (userAvatar) {
        userAvatar.textContent = 'K'; // For Klabis
    }
    if (userName) {
        userName.textContent = 'Administrátor';
    }
    if (userRegNumber) {
        userRegNumber.textContent = 'OAuth2 Client';
    }
});

// Export functions for use in inline scripts
window.KlabisUI = {
    showToast,
    checkAuth,
    handleLogout,
    formatDate,
    formatDateTime,
    getGenderText,
    getNationalityText,
    getCountryText,
    getInitials,
    calculateAge,
    validateCzechBirthNumber,
    validateEmail,
    validateCzechPhone,
    formatPhoneNumber,
    debounce,
    handleApiError
};

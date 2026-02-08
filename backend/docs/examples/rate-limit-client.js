/**
 * rate-limit-client.js
 *
 * This example demonstrates how to handle rate limiting (429 responses) when
 * making requests to the Klabis API.
 *
 * Features:
 * - Automatic retry with exponential backoff
 * - Respects Retry-After headers
 * - Configurable max retries
 * - Proper error handling
 */

const BASE_URL = 'https://localhost:8443';
const MAX_RETRIES = 3;
const BASE_DELAY = 1000; // 1 second

/**
 * Request a password setup token with rate limit handling
 * @param {string} email - User's email address
 * @returns {Promise<Object>} Response data
 */
async function requestPasswordSetupToken(email) {
    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        try {
            const response = await fetch(`${BASE_URL}/api/auth/password-setup/request`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({email})
            });

            // Handle rate limiting
            if (response.status === 429) {
                const retryAfter = parseInt(response.headers.get('retry-after')) || BASE_DELAY / 1000;
                const delay = retryAfter * 1000; // Convert to milliseconds

                console.log(`Rate limited. Retrying after ${delay}ms... (Attempt ${attempt}/${MAX_RETRIES})`);

                if (attempt < MAX_RETRIES) {
                    await sleep(delay);
                    continue;
                } else {
                    throw new Error(`Max retries (${MAX_RETRIES}) exceeded due to rate limiting`);
                }
            }

            // Handle other HTTP errors
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(`HTTP error! status: ${response.status}, detail: ${errorData.detail || 'Unknown error'}`);
            }

            return await response.json();
        } catch (error) {
            if (attempt === MAX_RETRIES) {
                throw error;
            }

            // Implement exponential backoff for network errors
            const delay = BASE_DELAY * Math.pow(2, attempt - 1);
            console.log(`Network error. Retrying after ${delay}ms... (Attempt ${attempt}/${MAX_RETRIES})`);
            await sleep(delay);
        }
    }
}

/**
 * Complete password setup with rate limit handling
 * @param {string} token - Password setup token
 * @param {string} newPassword - New password
 * @returns {Promise<Object>} Response data
 */
async function completePasswordSetup(token, newPassword) {
    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        try {
            const response = await fetch(`${BASE_URL}/api/auth/password-setup/complete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    token,
                    newPassword,
                    confirmPassword: newPassword
                })
            });

            if (response.status === 429) {
                const retryAfter = parseInt(response.headers.get('retry-after')) || BASE_DELAY / 1000;
                const delay = retryAfter * 1000;

                console.log(`Rate limited. Retrying after ${delay}ms... (Attempt ${attempt}/${MAX_RETRIES})`);

                if (attempt < MAX_RETRIES) {
                    await sleep(delay);
                    continue;
                } else {
                    throw new Error(`Max retries (${MAX_RETRIES}) exceeded due to rate limiting`);
                }
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(`HTTP error! status: ${response.status}, detail: ${errorData.detail || 'Unknown error'}`);
            }

            return await response.json();
        } catch (error) {
            if (attempt === MAX_RETRIES) {
                throw error;
            }

            const delay = BASE_DELAY * Math.pow(2, attempt - 1);
            console.log(`Network error. Retrying after ${delay}ms... (Attempt ${attempt}/${MAX_RETRIES})`);
            await sleep(delay);
        }
    }
}

/**
 * Sleep utility function
 * @param {number} ms - Milliseconds to sleep
 * @returns {Promise<void>}
 */
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// Example usage
async function main() {
    try {
        console.log('Requesting password setup token...');
        const result1 = await requestPasswordSetupToken('user@example.com');
        console.log('Success:', result1);

        console.log('\nCompleting password setup...');
        const result2 = await completePasswordSetup('abc123', 'NewPassword123!');
        console.log('Success:', result2);
    } catch (error) {
        console.error('Error:', error.message);
        process.exit(1);
    }
}

// Run example if this file is executed directly
if (require.main === module) {
    main();
}

module.exports = {
    requestPasswordSetupToken,
    completePasswordSetup
};

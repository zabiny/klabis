import axios, {AxiosInstance, AxiosRequestConfig, AxiosResponse} from 'axios';

// We'll create a function to get the auth token that can be used outside of React components
let getTokenCallback: () => Promise<string | null> = async () => null;

// This function will be called by the API hooks to set the token getter
export const setAuthTokenGetter = (tokenGetter: () => Promise<string | null>) => {
    getTokenCallback = tokenGetter;
};

// Function to get the auth token
const getAuthToken = (): Promise<string | null> => {
    return getTokenCallback();
};

// Create an axios instance with default configuration
const apiClient: AxiosInstance = axios.create({
    baseURL: '/api', // This will be proxied to the actual API URL in development
    headers: {
        'Content-Type': 'application/json',
    },
});

// Add request interceptor to add auth token to requests
apiClient.interceptors.request.use(
    async (config: AxiosRequestConfig): AxiosRequestConfig => {
        const token = await getAuthToken();
        //console.log(`OAuth2 token: ${token}`);
        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Add response interceptor to handle common response scenarios
apiClient.interceptors.response.use(
    (response: AxiosResponse): AxiosResponse => {
        return response;
    },
    (error) => {
        // Handle specific error cases here (e.g., 401 Unauthorized)
        if (error.response && error.response.status === 401) {
            // Redirect to login or refresh token
            console.error('Unauthorized access. Redirecting to login...');
            // You might want to trigger a logout or token refresh here
        }
        return Promise.reject(error);
    }
);

export default apiClient;

import axios from "axios";
import { ElMessage } from "element-plus";
import { useAuthStore } from "../store/auth";

// 创建 axios 实例
const service = axios.create({
  baseURL:  '/admin',
  timeout: 5000, // 请求超时时间
});

let refreshRequest = null;

const resolveMessage = (payload, fallback = "请求失败") => {
  if (!payload) return fallback;
  if (typeof payload === "string") return payload;
  return payload.msg || payload.message || fallback;
};

const shouldSkipAuthRefresh = (config = {}) => {
  const url = String(config.url || "");
  return Boolean(config.skipAuthRefresh) || /\/auth\/(login|refresh)/.test(url);
};

const requestTokenRefresh = async () => {
  const authStore = useAuthStore();

  if (!authStore.refreshToken) {
    throw new Error("登录已过期，请重新登录");
  }

  if (!refreshRequest) {
    refreshRequest = service
      .post(
        "/auth/refresh",
        { refreshToken: authStore.refreshToken },
        { skipAuthRefresh: true, showErrorMessage: false }
      )
      .then((tokenPayload) => {
        authStore.setTokens(tokenPayload.accessToken, tokenPayload.refreshToken);
        return tokenPayload;
      })
      .finally(() => {
        refreshRequest = null;
      });
  }

  return refreshRequest;
};

// 1. 请求拦截器：每次请求都在 Header 里带上 Token
service.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore();
    if (authStore.accessToken) {
      // 注意：标准格式是 "Bearer " + token
      config.headers.Authorization = `Bearer ${authStore.accessToken}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code === undefined) {
      return res;
    }

    if (res.code === 1) {
      return res.data;
    } else {
      const message = resolveMessage(res);

      ElMessage({
        message,
        type: "error",
        duration: 5 * 1000,
      });
      return Promise.reject(new Error(message));
    }
  },
  async (error) => {
    console.dir(error);

    const authStore = useAuthStore();
    const originalRequest = error.config || {};

    if (error.response && error.response.status === 401 && !originalRequest._retry && !shouldSkipAuthRefresh(originalRequest) && authStore.refreshToken) {
      originalRequest._retry = true;

      try {
        const tokenPayload = await requestTokenRefresh();
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers.Authorization = `Bearer ${tokenPayload.accessToken}`;
        return service(originalRequest);
      } catch (refreshError) {
        authStore.logout();
        const refreshMessage = resolveMessage(refreshError.response?.data, refreshError.message || "登录已过期，请重新登录");

        if (originalRequest.showErrorMessage !== false) {
          ElMessage({
            message: refreshMessage,
            type: "error",
            duration: 5 * 1000,
          });
        }

        return Promise.reject(new Error(refreshMessage));
      }
    }

    // 处理 HTTP 状态码错误
    if (error.response && error.response.status === 401) {
      console.warn("登录过期，请重新登录");
      authStore.logout();
    }

    const message = resolveMessage(error.response?.data, error.message || "网络错误，请稍后重试");

    if (originalRequest.showErrorMessage !== false) {
      ElMessage({
        message,
        type: "error",
        duration: 5 * 1000,
      });
    }

    return Promise.reject(new Error(message));
  }
);

export default service;

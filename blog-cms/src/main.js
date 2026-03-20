import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";

import App from "./App.vue";
import router from "./router";

const consumeAdminBridge = () => {
  const bridgePrefix = "__dream_admin_bridge__:";
  const rawValue = window.name || "";

  if (!rawValue.startsWith(bridgePrefix)) {
    return;
  }

  try {
    const payload = JSON.parse(rawValue.slice(bridgePrefix.length));

    if (payload?.accessToken) {
      localStorage.setItem("accessToken", payload.accessToken);
    }

    if (payload?.refreshToken) {
      localStorage.setItem("refreshToken", payload.refreshToken);
    }
  } catch (error) {
    console.error("后台登录桥接失败", error);
  } finally {
    window.name = "";
  }
};

consumeAdminBridge();

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus);

app.mount("#app");

// 屏蔽 ResizeObserver loop 报错
const debounce = (fn, delay) => {
  let timer = null;
  return function () {
    let context = this;
    let args = arguments;
    clearTimeout(timer);
    timer = setTimeout(function () {
      fn.apply(context, args);
    }, delay);
  };
};

// 重写 ResizeObserver 的构造函数，增加防抖
const _ResizeObserver = window.ResizeObserver;
window.ResizeObserver = class ResizeObserver extends _ResizeObserver {
  constructor(callback) {
    callback = debounce(callback, 16);
    super(callback);
  }
};

// Taro 配置
// 昕动智能微信小程序 - MVP开发阶段

const config = {
  projectName: 'xindong-mini-program',
  date: '2026-04-15',
  designWidth: 750,
  deviceRatio: {
    '640:2': ['iphone-x', 'iphone-xr', 'iphone-xsmax', 'iphone-11'],
    '750:1': ['android'],
  },
  sourceRoot: 'src',
  outputRoot: 'dist',
  framework: 'vue3',
  plugins: ['@tarojs/plugin-framework-vue3'],
  copy: {
    patterns: [
      {
        from: 'src/assets/default-avatar.png',
        to: 'dist/assets/default-avatar.png',
      },
    ],
    options: {},
  },
  defineConstants: {
    // API 基础地址 - 开发环境
    API_BASE: JSON.stringify('http://localhost:8080'),
    // MQTT WebSocket 地址
    MQTT_WS_URL: JSON.stringify('ws://localhost:8083'),
  },
  mini: {
    miniCssExtractPluginOption: {
      ignoreOrder: true,
    },
    postcss: {
      pxtransform: {
        enable: true,
        config: {
          selectorBlackList: [/body/],
        },
      },
      url: {
        enable: true,
        config: {
          limit: 10240,
        },
      },
    },
    webpackChain(chain) {
      // Disable ProgressPlugin to avoid webpack 5.106+ schema validation error
      chain.plugins.delete('progressPlugin');
    },
  },
  h5: {
    publicPath: '/',
    staticDirectory: 'static',
    webpackChain(chain) {
      chain.resolve.alias.set('@', require('path').resolve(__dirname, '../src'));
    },
  },
};

module.exports = config;

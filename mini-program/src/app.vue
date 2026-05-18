<script>
import { createApp } from 'vue';
import './app.scss';

const TAB_PAGES = [
  'pages/home/index',
  'pages/daily-task/index',
  'pages/progress/index',
  'pages/profile/index',
];

function buildLaunchUrl(path, query = {}) {
  if (!path || path === 'pages/login/index') {
    return '';
  }
  const queryString = Object.keys(query)
    .filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(String(query[key]))}`)
    .join('&');
  return `/${path}${queryString ? `?${queryString}` : ''}`;
}

const App = createApp({
  onLaunch(options) {
    console.log('[App] Launched - 昕动智能小程序', options);
    this.storeLaunchTarget(options);
    setTimeout(() => {
      this.checkLoginStatus();
    }, 300);
  },

  onShow(options) {
    console.log('[App] Showed', options);
    this.storeLaunchTarget(options);
  },

  onHide() {
    console.log('[App] Hided');
  },

  methods: {
    storeLaunchTarget(options) {
      const token = wx.getStorageSync('auth_token');
      if (token || !options?.path) {
        return;
      }
      const query = options.query || {};
      const hasQuery = Object.keys(query).length > 0;
      const isTabPage = TAB_PAGES.includes(options.path);
      if (!hasQuery && isTabPage) {
        return;
      }
      const target = buildLaunchUrl(options.path, query);
      if (target) {
        wx.setStorageSync('post_login_redirect', target);
      }
    },

    checkLoginStatus() {
      const token = wx.getStorageSync('auth_token');
      if (!token) {
        wx.reLaunch({ url: '/pages/login/index' });
      }
    },
  },
});

export default App;
</script>

<script>
import { createApp } from 'vue';
import './app.scss';

const App = createApp({
  onLaunch() {
    console.log('[App] Launched - 昕动智能小程序');
    setTimeout(() => {
      this.checkLoginStatus();
    }, 300);
  },

  onShow() {
    console.log('[App] Showed');
  },

  onHide() {
    console.log('[App] Hided');
  },

  methods: {
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

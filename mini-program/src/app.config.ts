// 小程序全局配置
// 昕动智能 HealthHub

export default defineAppConfig({
  pages: [
    'pages/home/index',
    'pages/daily-task/index',
    'pages/progress/index',
    'pages/profile/index',
    'pages/device-binding/index',
    'pages/login/index',
    'pages/training-plan/index',
    'pages/training-result/index',
    'pages/privacy/index',
    'pages/user-agreement/index',
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#1a1a2e',
    navigationBarTitleText: '昕动健康',
    navigationBarTextStyle: 'white',
  },
  tabBar: {
    color: '#999',
    selectedColor: '#4A90E2',
    backgroundColor: '#fff',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/home/index',
        text: '首页',
        iconPath: 'assets/tab-home.png',
        selectedIconPath: 'assets/tab-home-active.png',
      },
      {
        pagePath: 'pages/daily-task/index',
        text: '训练',
        iconPath: 'assets/tab-task.png',
        selectedIconPath: 'assets/tab-task-active.png',
      },
      {
        pagePath: 'pages/progress/index',
        text: '进度',
        iconPath: 'assets/tab-progress.png',
        selectedIconPath: 'assets/tab-progress-active.png',
      },
      {
        pagePath: 'pages/profile/index',
        text: '我的',
        iconPath: 'assets/tab-profile.png',
        selectedIconPath: 'assets/tab-profile-active.png',
      },
    ],
  },
  usingComponents: {},
});

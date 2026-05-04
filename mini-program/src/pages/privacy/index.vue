<template>
  <view class="privacy-container">
    <view class="header">
      <text class="title">隐私政策</text>
      <text class="date">更新日期：2026年4月29日</text>
    </view>
    
    <view class="content">
      <view class="section">
        <text class="section-title">一、信息收集</text>
        <text class="section-content">
          我们收集您在使用本小程序时主动提供的信息，包括但不限于：
          手机号、昵称头像、健康数据、设备信息等。
          同时我们会收集您的设备信息以提供更好的服务。
        </text>
      </view>
      
      <view class="section">
        <text class="section-title">二、信息使用</text>
        <text class="section-content">
          我们将使用收集的信息用于：
          1. 提供、维护和改进我们的服务
          2. 开发新功能
          3. 为您提供个性化体验
          4. 响应您的请求
        </text>
      </view>
      
      <view class="section">
        <text class="section-title">三、信息共享</text>
        <text class="section-content">
          未经您的同意，我们不会与任何第三方共享您的个人信息，
          法律法规要求的情况除外。
        </text>
      </view>
      
      <view class="section">
        <text class="section-title">四、信息安全</text>
        <text class="section-content">
          我们采用业界领先的安全措施保护您的个人信息，
          包括数据加密、访问控制等措施。
        </text>
      </view>
      
      <view class="section">
        <text class="section-title">五、联系我们</text>
        <text class="section-content">
          如您对本隐私政策有任何疑问，请联系我们：
          邮箱：privacy@xindong.health
        </text>
      </view>
    </view>
    
    <view class="footer">
      <button type="primary" @tap="goBack">返回</button>
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    goBack() {
      wx.navigateBack()
    }
  }
}
</script>

<style lang="scss">
.privacy-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 30rpx;
}

.header {
  background: #fff;
  padding: 30rpx;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  
  .title {
    display: block;
    font-size: 40rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 16rpx;
  }
  
  .date {
    font-size: 24rpx;
    color: #999;
  }
}

.content {
  .section {
    background: #fff;
    padding: 30rpx;
    border-radius: 16rpx;
    margin-bottom: 20rpx;
    
    .section-title {
      display: block;
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 16rpx;
    }
    
    .section-content {
      display: block;
      font-size: 28rpx;
      color: #666;
      line-height: 1.8;
    }
  }
}

.footer {
  margin-top: 40rpx;
  padding: 0 30rpx;
  
  button {
    background: #007aff;
    color: #fff;
    border-radius: 44rpx;
    height: 88rpx;
    line-height: 88rpx;
  }
}
</style>

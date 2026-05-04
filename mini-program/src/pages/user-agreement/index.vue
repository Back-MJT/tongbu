<template>
  <view class="agreement-container">
    <view class="header">
      <text class="title">用户协议</text>
      <text class="date">更新日期：2026年4月29日</text>
    </view>
    
    <view class="content">
      <view class="section">
        <text class="section-title">一、服务说明</text>
        <text class="section-content">
          昕动智能健康小程序是一个综合性的健康管理平台，
          提供运动训练追踪、健康数据记录、AI训练方案推荐等服务。
        </text>
      </view>
      
      <view class="section">
        <text class="section-title">二、用户注册</text>
        <text class="section-content">
          您在使用本服务前需注册账号。您承诺：
          1. 提供真实、准确的个人信息
          2. 妥善保管账号信息
          3. 及时更新个人信息
        </text>
      </view>
      
      <view class="section">
        <text class="section-title">三、使用规范</text>
        <text class="section-content">
          您同意：
          1. 遵守法律法规
          2. 不利用本服务从事违法活动
          3. 尊重他人权益
          4. 保护个人信息安全
        </text>
      </view>
      
      <view class="section">
        <text class="section-title">四、免责声明</text>
        <text class="section-content">
          本服务仅供参考，不构成医疗建议。
          用户应根据自己的身体状况合理安排运动，如有任何不适请及时就医。
        </text>
      </view>
      
      <view class="section">
        <text class="section-title">五、联系我们</text>
        <text class="section-content">
          如您对用户协议有任何疑问，请联系我们：
          邮箱：support@xindong.health
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
.agreement-container {
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

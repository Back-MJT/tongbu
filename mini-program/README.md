# 昕动健康微信小程序

欣动智能 HealthHub 小程序，用户触达端。

当前重构主线:

- 小程序扫码器械二维码
- 连接 `GY-BLE25T`
- 本地解析 IMU 数据并计数
- 训练结果统一上传到若依后端 `:8080`

## 技术栈

- **框架**: Taro 4.x + Vue 3
- **状态管理**: Pinia
- **构建**: Webpack（Taro 内置）
- **样式**: SCSS

## 快速开始

```bash
cd mini-program

# 安装依赖
npm install

# 开发构建（微信小程序）
npm run build:weapp

# 监听模式
npm run dev:weapp

# H5 预览
npm run build:h5
```

## 目录结构

```
src/
├── api/               # API 接口（Mock）
├── assets/            # 静态资源
├── config/
│   └── env.ts         # 环境配置
├── models/            # TypeScript 类型定义
├── pages/
│   ├── home/          # 首页（今日概览、AE算法目标）
│   ├── daily-task/    # 今日训练任务
│   ├── training-plan/  # AI 训练方案（AE算法处方）
│   ├── progress/      # 训练进度追踪
│   ├── profile/       # 个人档案
│   ├── device-binding/ # 器械训练（扫码 + BLE + 训练保存）
│   └── login/         # 登录（微信/手机号/演示模式）
├── services/
│   ├── api.ts         # HTTP API（对接 RuoYi 后端 + 干预引擎）
│   └── ble.ts         # BLE 设备服务
├── stores/
│   └── user.ts        # 用户状态
└── utils/             # 工具函数
```

## 核心功能

| 页面 | 状态 | 说明 |
|------|------|------|
| 首页 | 完成 | 今日概览、设备状态、AE 运动目标 |
| 今日训练 | 完成 | 训练组数进度、动作指引 |
| AI训练方案 | 完成 | AE 个性化处方、教练建议、循证参考 |
| 训练进度 | 完成 | 依从率、连续天数、趋势图 |
| 个人档案 | 完成 | 用户信息、设备管理 |
| 器械训练 | 完成 | 扫码器械 + BLE 连接 + 本地计数 + 保存训练 |
| 登录 | 完成 | 微信一键登录、手机号、演示模式 |

## API 对接

| 服务 | 地址 | 说明 |
|------|------|------|
| RuoYi 后端统一入口 | `localhost:8080` | 用户认证、器械解析、训练记录、处方代理 |
| 干预引擎 | 由若依后端代理 | 小程序不再默认直连 `:4001` |

生产环境地址在 `src/config/env.ts` 中配置。当前默认 `apiBase` 与 `ieBase` 已统一指向若依后端。

## 演示模式

在登录页点击"🔧 厂家演示模式"，使用预设的 mock 数据，不依赖后端。

## 微信配置

- **appid**: 在 `project.config.json` 中修改（当前为占位符）
- **编译类型**: `miniprogram`
- **tabBar 图标**: `src/assets/tab-*.png`

## 提审检查清单

- [ ] 替换 `project.config.json` 中的 appid
- [ ] 在微信公众平台配置服务器域名（request 合法域名）
- [ ] 配置 `permission.json`（位置权限描述）
- [ ] 补充隐私协议（微信要求）
- [ ] 替换演示模式占位图标

# RuoYi-Vue3 多租户路由架构方案

> 文档版本：v1.0
> 日期：2026-04-28
> 负责人：Founding Engineer
> 关联 Issue：XIN-145

---

## 1. 概述

本方案在 RuoYi-Vue3 基础上实现**基于租户 ID 的路由分离**，实现一套前端代码支持多个租户（企业/品牌），各租户数据严格隔离。

### 设计目标

1. **路由分离**：`/tenant/{tenantId}/*` 下的所有路由自动携带租户上下文
2. **统一鉴权**：跨租户 Token 管理，租户切换时自动刷新上下文
3. **数据隔离**：请求拦截器自动注入 `Tenant-Id` header，后端据此过滤数据
4. **导航守卫**：前端路由守卫自动注入租户上下文，防止跨租户越权访问

---

## 2. 路由结构

### 2.1 URL 规范

```
/tenant/{tenantId}/dashboard        租户工作台
/tenant/{tenantId}/device          设备管理
/tenant/{tenantId}/coach           教练用户
/login?tenantId=xxx&redirect=...   登录（携带租户标识）
```

### 2.2 路由层级

| 路径类型 | 说明 | 鉴权 |
|---|---|---|
| `/tenant/:tenantId/*` | 租户专属路由 | 需要 Token + 租户权限 |
| `/login`, `/register` | 公共认证路由 | 白名单，无需鉴权 |
| `/*` | 公共路由（404/401） | 白名单 |

### 2.3 占位路由

在 `router/index.js` 初始化时注册占位路由，防止用户提前导航到 `/tenant/:id` 时出现 404：

```js
// router/tenantRoutes.js
export const TENANT_PLACEHOLDER_ROUTES = [
  {
    path: '/tenant/:tenantId',
    component: Layout,
    name: 'TenantRoot',
    redirect: 'noredirect',
    meta: { isTenantRoute: true },
    children: [
      { path: 'index', component: () => import('@/views/index'), name: 'TenantIndex' },
      { path: ':pathMatch(.*)*', redirect: '/404', meta: { title: '404' } }
    ]
  }
]
```

---

## 3. 核心实现

### 3.1 租户状态管理 (`store/modules/tenant.js`)

新建 Pinia Store `useTenantStore`，集中管理租户上下文：

| 状态 | 类型 | 说明 |
|---|---|---|
| `currentTenantId` | string | 当前活跃租户 ID |
| `currentTenantName` | string | 当前租户名称 |
| `tenantList` | array | 当前用户可访问的租户列表 |
| `loading` | boolean | 租户列表加载状态 |
| `switching` | boolean | 租户切换锁 |

**关键方法：**
- `initTenantContext(tenantId)` — 登录后初始化租户上下文
- `switchTenant(tenantId)` — 切换租户（带权限校验）
- `validateTenantAccess(tenantId)` — 验证用户是否有权访问指定租户
- `fetchTenantList()` — 从后端 `/system/tenant/list` 获取可访问租户列表
- `clearTenantContext()` — 退出登录时清除上下文

### 3.2 路由守卫 (`router/tenantRoutes.js` + `permission.js`)

#### tenantPathGuard（最早执行）

在 `permission.js` 的 `beforeEach` **之前**执行，负责：

```
1. 从 URL path 提取 tenantId（如 /tenant/123/dashboard → 123）
2. 用户未登录 → 重定向到 /login?redirect=...&tenantId=xxx
3. 用户已登录但无租户上下文 → 自动初始化
4. 用户访问的 tenantId 与当前上下文不一致 → 权限校验
   - 超管或 system:tenant:switch 权限 → 允许切换
   - 否则 → 拒绝并重定向回当前租户首页
```

#### permission.js 修改

```js
// permission.js
router.beforeEach((to, from, next) => {
  NProgress.start()

  // 多租户路径守卫（最早执行）
  const tenantHandled = tenantPathGuard(to, from)
  if (tenantHandled) {
    NProgress.done()
    return
  }

  // ... 其余原有守卫逻辑不变
})
```

### 3.3 请求拦截器 (`utils/request.js`)

在 axios 请求拦截器中自动注入 `Tenant-Id` header：

```js
// utils/request.js
let tenantId = userStore.tenantId
if (!tenantId) {
  // 从 URL path 提取兜底
  const match = window.location.pathname.match(/^\/tenant\/(\d+)/)
  if (match) tenantId = match[1]
}
if (tenantId) {
  config.headers['Tenant-Id'] = String(tenantId)
}
```

**数据隔离原理：**
- 每个请求自动携带 `Tenant-Id` header
- 后端 Spring Boot 通过 `@TenantId` 注解或拦截器自动注入租户过滤条件
- 前端无需手动处理，后端保障数据隔离

### 3.4 Token 管理

- Token 存储在 `Cookies.get('Admin-Token')`，与租户无关
- 租户上下文存在 `useTenantStore`（Pinia），不存储在 Token 中
- 退出登录时调用 `tenantStore.clearTenantContext()`

---

## 4. 文件变更清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `src/store/modules/tenant.js` | **新建** | 租户状态管理 Pinia Store |
| `src/router/tenantRoutes.js` | **新建** | 租户路由工具函数 |
| `src/api/system/tenant.js` | **新建** | 租户管理 API 接口 |
| `src/permission.js` | **修改** | 集成租户导航守卫 |
| `src/router/index.js` | **修改** | 注册租户占位路由 |
| `src/store/modules/user.js` | **修改** | 登录/登出时同步租户上下文 |
| `src/utils/request.js` | **修改** | 请求拦截器自动注入 Tenant-Id |

---

## 5. 后端接口要求

前端依赖以下后端接口：

| 接口 | 方法 | 说明 |
|---|---|---|
| `/system/tenant/list` | GET | 获取当前用户可访问的租户列表 |
| `/system/tenant/current` | GET | 获取当前租户信息 |
| `/system/tenant/switch` | PUT | 切换租户 |

> **注意**：如后端尚未实现 `/system/tenant/list`，前端会从 JWT Token payload 中解析 `tenantId` 作为兜底方案。

---

## 6. 路由守卫执行流程图

```
用户访问 /tenant/123/dashboard
        │
        ▼
┌─ tenantPathGuard ─┐
│ 提取 tenantId=123 │
│  用户未登录?       │──Yes──→ 重定向 /login?tenantId=123
└───────┬───────────┘
        │No
        ▼
│ 用户已登录但无上下文? │──Yes──→ initTenantContext(123)
└───────┬─────────────┘
        │No
        ▼
│ tenantId ≠ currentTenant? │──Yes──→ 权限校验
│                              - 超管/switch权限 → switchTenant(123)
│                              - 否则 → 重定向 /tenant/{current}/...
└───────┬────────────────────┘
        │No
        ▼
  放行（继续 permission.js 原有守卫）
```

---

## 7. 多租户切换 UX

1. **登录页**：`/login?tenantId=xxx`，登录后自动跳转对应租户首页
2. **侧边栏租户切换器**：可在顶部导航显示当前租户名称，点击切换（需权限）
3. **根路径 `/`**：根据当前租户上下文重定向到 `/tenant/{id}/index`

---

## 8. 已知限制与 TODO

- [ ] 后端 `/system/tenant/list` 接口待实现
- [ ] 侧边栏租户切换器 UI 组件
- [ ] 租户首页个性化（不同租户显示不同欢迎信息）
- [ ] 租户维度的菜单权限隔离（当前实现是用户权限 + 租户隔离叠加）

---

## 9. 测试用例

| 场景 | 预期结果 |
|---|---|
| 未登录访问 `/tenant/123/dashboard` | 重定向到 `/login?redirect=...&tenantId=123` |
| 登录后访问 `/tenant/123/device` | 正常显示，请求带 `Tenant-Id: 123` header |
| 切换租户到 `456` | `useTenantStore.currentTenantId` 更新为 `456`，路由跳转到 `/tenant/456/...` |
| 退出登录 | `useTenantStore` 上下文清除，路由跳转到 `/login` |

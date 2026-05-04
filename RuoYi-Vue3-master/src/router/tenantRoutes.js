/**
 * 多租户路由配置
 * 基于租户ID的路由分离 /tenant/{tenantId}/*
 *
 * 路由结构：
 *   /tenant/:tenantId/*          租户专属路由（需校验租户权限）
 *   /login                        公共登录（白名单）
 *   /register                     公共注册（白名单）
 *   /*                            公共路由（404、401等）
 */

import Layout from '@/layout'
import router from '@/router'
import useTenantStore from '@/store/modules/tenant'
import useUserStore from '@/store/modules/user'

// ============================================================
// 1. 租户路径匹配
// ============================================================

/**
 * 从 URL path 中提取租户 ID
 * 例如：/tenant/123/dashboard -> 123
 */
export function extractTenantIdFromPath(path) {
  const match = path.match(/^\/tenant\/(\d+)/)
  return match ? match[1] : null
}

/**
 * 将普通路径转换为租户路径
 * /dashboard -> /tenant/123/dashboard
 */
export function toTenantPath(path, tenantId) {
  // 移除已有的 /tenant 前缀，避免重复
  const cleanPath = path.replace(/^\/tenant\/\d+/, '')
  return `/tenant/${tenantId}${cleanPath || '/'}`
}

/**
 * 判断 path 是否为租户专属路径
 */
export function isTenantPath(path) {
  return !!extractTenantIdFromPath(path)
}

// ============================================================
// 2. 租户路由前置守卫（早期拦截）
// ============================================================

/**
 * 租户路径前置校验
 * 在 permission.js 的 beforeEach 之前执行
 * 负责：从未登录 → 已登录的过渡期注入租户上下文
 *
 * 返回 true  表示已处理（重定向/放行），路由应中断
 * 返回 false 表示未处理，应继续执行后续守卫
 */
export function tenantPathGuard(to, from) {
  const { path } = to
  const tenantId = extractTenantIdFromPath(path)

  // 非租户路径，无需处理
  if (!tenantId) return false

  const userStore = useUserStore()
  const tenantStore = useTenantStore()

  // 用户未登录：重定向到登录页（携带租户 ID，登录后恢复）
  if (!userStore.token) {
    const redirect = encodeURIComponent(to.fullPath)
    router.push(`/login?redirect=${redirect}&tenantId=${tenantId}`)
    return true
  }

  // 用户已登录，但租户上下文未初始化：从 path 自动注入
  if (!tenantStore.currentTenantId && tenantId) {
    tenantStore.initTenantContext(tenantId)
  }

  // 权限校验：用户访问的租户必须与当前上下文一致
  if (tenantStore.currentTenantId && tenantId !== tenantStore.currentTenantId) {
    // 允许切换租户（超管或有权限的用户）
    const canSwitch = userStore.roles.includes('ROLE_ADMIN') ||
                      userStore.permissions.includes('system:tenant:switch')
    if (!canSwitch) {
      console.warn(`[TenantGuard] 禁止切换租户 ${tenantId}，当前上下文：${tenantStore.currentTenantId}`)
      router.push(`/tenant/${tenantStore.currentTenantId}${to.params.pathMatch || '/'}`)
      return true
    }
    // 有权限：执行切换
    tenantStore.switchTenant(tenantId)
  }

  return false
}

// ============================================================
// 3. 租户专属路由注册
// ============================================================

/**
 * 为指定租户生成路由配置
 * 将标准路由包裹在 /tenant/:tenantId/* 下
 *
 * @param {Array} baseRoutes 标准路由（来自后端菜单）
 * @param {string} tenantId  租户ID
 * @param {string} tenantName 租户名称（用于路由 meta）
 */
export function generateTenantRoutes(baseRoutes, tenantId, tenantName) {
  const wrapRoute = (route, tenantId) => {
    // 路由前缀
    const prefixedPath = `/tenant/${tenantId}${route.path.startsWith('/') ? route.path : '/' + route.path}`
    return {
      ...route,
      path: prefixedPath,
      name: `${route.name}_tenant_${tenantId}`,
      meta: {
        ...route.meta,
        tenantId,
        tenantName,
        isTenantRoute: true,
      },
      children: route.children
        ? route.children.map(child => wrapRoute(child, tenantId))
        : undefined,
    }
  }

  return baseRoutes.map(route => wrapRoute(route, tenantId))
}

/**
 * 注册租户动态路由到 router
 * @param {object} router  VueRouter 实例
 * @param {Array}  routes  路由配置
 */
export function registerTenantRoutes(router, routes) {
  routes.forEach(route => {
    if (!router.hasRoute(route.name)) {
      router.addRoute(route)
    }
  })
}

// ============================================================
// 4. 路由重定向：根路径 -> 租户首页
// ============================================================

/**
 * 构建根路径重定向路由
 * / -> /tenant/{currentTenantId}/index
 */
export function buildRootRedirect(tenantId) {
  return {
    path: '/',
    redirect: `/tenant/${tenantId}/index`,
    meta: { title: '首页' }
  }
}

// ============================================================
// 5. 静态租户路由占位（解决提前渲染问题）
// ============================================================

/**
 * 在 permission.js 生成动态路由之前，
 * 先注册租户路径的占位路由，防止 404
 * 实际路由由后端菜单数据填充后通过 addRoute 注入
 */
export const TENANT_PLACEHOLDER_ROUTES = [
  {
    path: '/tenant/:tenantId',
    component: Layout,
    name: 'TenantRoot',
    redirect: 'noredirect',
    meta: { isTenantRoute: true },
    children: [
      {
        path: 'index',
        component: () => import('@/views/index'),
        name: 'TenantIndex',
        meta: { title: '租户首页', isTenantRoute: true }
      },
      // 404 fallback
      {
        path: ':pathMatch(.*)*',
        redirect: '/404',
        meta: { title: '404', isTenantRoute: true }
      }
    ]
  }
]

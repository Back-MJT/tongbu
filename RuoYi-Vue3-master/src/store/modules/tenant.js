/**
 * 多租户状态管理
 * 管理租户上下文、切换、隔离
 */
import { defineStore } from 'pinia'
import axios from 'axios'

const useTenantStore = defineStore('tenant', {
  state: () => ({
    /** 当前活跃租户 ID */
    currentTenantId: null,
    /** 当前租户名称 */
    currentTenantName: null,
    /** 租户列表（用户可访问的租户） */
    tenantList: [],
    /** 租户上下文加载状态 */
    loading: false,
    /** 租户切换锁，防止快速切换 */
    switching: false,
  }),

  getters: {
    /** 是否已设置租户上下文 */
    hasTenant: (state) => !!state.currentTenantId,

    /** 获取当前路由使用的租户路径前缀 */
    tenantPath: (state) => state.currentTenantId ? `/tenant/${state.currentTenantId}` : '',

    /** 判断是否为超管（可访问所有租户） */
    isSuperAdmin: (state) => state.tenantList.some(t => t.isDefault && t.tenantId === state.currentTenantId),
  },

  actions: {
    /**
     * 初始化租户上下文
     * 从用户信息或 URL 参数中恢复租户状态
     */
    initTenantContext(tenantIdFromLogin) {
      if (tenantIdFromLogin) {
        this.currentTenantId = String(tenantIdFromLogin)
        return
      }
      // 从 URL path 提取租户 ID（如果有）
      const pathMatch = window.location.pathname.match(/^\/tenant\/(\d+)/)
      if (pathMatch) {
        this.currentTenantId = pathMatch[1]
      }
    },

    /**
     * 切换租户上下文
     * @param {string|number} tenantId
     */
    async switchTenant(tenantId) {
      if (this.switching) {
        console.warn('[TenantStore] Tenant switch in progress, ignoring duplicate request')
        return
      }
      if (!tenantId || tenantId === this.currentTenantId) return

      this.switching = true
      try {
        // 向后端验证用户是否有权访问该租户
        const hasAccess = await this.validateTenantAccess(tenantId)
        if (!hasAccess) {
          throw new Error(`无权访问租户 ${tenantId}`)
        }

        this.currentTenantId = String(tenantId)
        const tenant = this.tenantList.find(t => String(t.tenantId) === String(tenantId))
        this.currentTenantName = tenant?.tenantName || null

        // 触发路由更新：跳转到对应租户空间
        // router.push 会由调用方处理
      } finally {
        this.switching = false
      }
    },

    /**
     * 验证用户是否有权访问指定租户
     */
    async validateTenantAccess(tenantId) {
      try {
        // 缓存中存在则直接返回
        const cached = this.tenantList.find(t => String(t.tenantId) === String(tenantId))
        if (cached) return true

        // 向后端查询租户列表，验证访问权限
        // 后端应根据当前用户 token 返回其可访问的租户列表
        // 如果列表中包含 targetTenantId，则有权限
        await this.fetchTenantList()
        return this.tenantList.some(t => String(t.tenantId) === String(tenantId))
      } catch (e) {
        console.error('[TenantStore] validateTenantAccess failed:', e)
        return false
      }
    },

    /**
     * 从后端获取当前用户可访问的租户列表
     */
    async fetchTenantList() {
      this.loading = true
      try {
        // VITE_APP_BASE_API 由 Vite 注入
        const baseURL = import.meta.env.VITE_APP_BASE_API || ''
        const res = await axios.get(`${baseURL}/system/tenant/list`, {
          timeout: 5000,
          // 不使用应用内请求拦截器，避免循环依赖
          headers: { 'Content-Type': 'application/json' }
        })
        if (res.data.code === 200 && Array.isArray(res.data.data)) {
          this.tenantList = res.data.data
        }
      } catch (e) {
        // 租户列表获取失败不影响主流程
        console.warn('[TenantStore] fetchTenantList failed, using fallback:', e.message)
        // Fallback: 从当前 token 的 payload 中解析租户信息
        this._fallbackTenantFromToken()
      } finally {
        this.loading = false
      }
    },

    /**
     * Fallback: 从 JWT token 解析租户信息
       * 适用于后端尚未实现租户列表接口的情况
       */
    _fallbackTenantFromToken() {
      try {
        const cookies = document.cookie.split(';')
        const tokenCookie = cookies.find(c => c.trim().startsWith('Admin-Token='))
        if (!tokenCookie) return
        const token = tokenCookie.split('=')[1]
        if (!token) return
        // JWT payload 是 base64url 编码
        const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
        if (payload.tenantId) {
          this.currentTenantId = String(payload.tenantId)
          const tenant = this.tenantList.find(t => String(t.tenantId) === String(payload.tenantId))
          this.currentTenantName = tenant?.tenantName || payload.tenantName || null
        }
      } catch (e) {
        // ignore
      }
    },

    /**
     * 清除租户上下文（退出登录时调用）
     */
    clearTenantContext() {
      this.currentTenantId = null
      this.currentTenantName = null
      this.tenantList = []
    }
  }
})

export default useTenantStore

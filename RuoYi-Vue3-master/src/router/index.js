import { createWebHistory, createRouter } from 'vue-router'
/* Layout */
import Layout from '@/layout'
import { TENANT_PLACEHOLDER_ROUTES } from './tenantRoutes'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    path: '/iot/equipment/qrcode',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/equipment/qrcode'),
        name: 'EquipmentQrCode',
        meta: { title: '生成二维码', activeMenu: '/iot/equipment' }
      }
    ]
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/lock',
    component: () => import('@/views/lock'),
    hidden: true,
    meta: { title: '锁定屏幕' }
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile/:activeTab?',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  {
    path: '/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  // IoT设备管理路由 (静态注册，供开发阶段访问；生产环境由菜单系统动态加载)
  {
    path: '/iot',
    component: Layout,
    redirect: 'noredirect',
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/iot/dashboard/index'),
        name: 'IoTDashboard',
        meta: { title: 'IoT看板', icon: 'dashboard', permissions: ['iot:dashboard:view'] }
      }
    ]
  },
  {
    path: '/iot/device',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/device/index'),
        name: 'IoTDevice',
        meta: { title: '设备管理', icon: 'device', permissions: ['iot:device:list'] }
      }
    ]
  },
  {
    path: '/iot/equipment',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/equipment/index'),
        name: 'IoTEquipment',
        meta: { title: '器械管理', icon: 'guide', permissions: ['iot:equipment:list'] }
      }
    ]
  },
  {
    path: '/iot/manufacturer',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/manufacturer/index'),
        name: 'IoTManufacturer',
        meta: { title: '厂商管理', icon: 'enterprise', permissions: ['iot:manufacturer:list'] }
      }
    ]
  },
  {
    path: '/iot/group',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/deviceGroup/index'),
        name: 'IoTDeviceGroup',
        meta: { title: '设备分组', icon: 'tree', permissions: ['iot:group:list'] }
      }
    ]
  },
  {
    path: '/iot/imu',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/imu/index'),
        name: 'IoTImu',
        meta: { title: 'IMU数据', icon: 'chart', permissions: ['iot:imu:query'] }
      }
    ]
  },
  {
    path: '/iot/log',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/log/index'),
        name: 'IoTLog',
        meta: { title: '设备日志', icon: 'log', permissions: ['iot:log:list'] }
      }
    ]
  },
  {
    path: '/iot/training-session',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/trainingSession/index'),
        name: 'IoTTrainingSession',
        meta: { title: '训练记录', icon: 'date', permissions: ['iot:training:query'] }
      }
    ]
  },
  {
    path: '/iot/manufacturer/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/manufacturer/detail'),
        name: 'IoTManufacturerDetail',
        meta: { title: '厂商详情', permissions: ['iot:manufacturer:list'] }
      }
    ]
  },
  {
    path: '/iot/config',
    component: Layout,
    redirect: 'noredirect',
    hidden: true,
    children: [
      {
        path: 'mqtt',
        component: () => import('@/views/iot/config/mqtt'),
        name: 'IoTMqttConfig',
        meta: { title: 'MQTT Topic配置', permissions: ['iot:config:mqtt:list'] }
      },
      {
        path: 'alert',
        component: () => import('@/views/iot/config/alert'),
        name: 'IoTAlertConfig',
        meta: { title: '告警规则配置', permissions: ['iot:config:alert:list'] }
      },
      {
        path: 'retention',
        component: () => import('@/views/iot/config/retention'),
        name: 'IoTRetentionConfig',
        meta: { title: '数据保留策略', permissions: ['iot:config:retention:list'] }
      }
    ]
  },
  // B2B 教练用户管理（从 b2b-frontend 迁移）
  {
    path: '/iot/coach',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/coach/index'),
        name: 'CoachUsers',
        meta: { title: '教练用户管理', icon: 'user', permissions: ['iot:coach:list'] }
      }
    ]
  },
  {
    path: '/iot/coach/user/:userId',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/coach/profile'),
        name: 'CoachUserProfile',
        meta: { title: '用户健康档案', permissions: ['iot:coach:list'] }
      }
    ]
  },
  // B2B 合规管理（从 b2b-frontend 迁移）
  {
    path: '/iot/compliance',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/iot/compliance/index'),
        name: 'ComplianceView',
        meta: { title: '合规管理', icon: 'checkbox', permissions: ['iot:compliance:list'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

// 注册租户占位路由，防止提前导航到 /tenant/:id 时 404
// 实际租户路由由后端菜单数据 + permission.js 动态注入
TENANT_PLACEHOLDER_ROUTES.forEach(route => {
  router.addRoute(route)
})

export default router

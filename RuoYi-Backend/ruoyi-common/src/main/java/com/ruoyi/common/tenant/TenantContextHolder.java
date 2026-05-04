package com.ruoyi.common.tenant;

import com.ruoyi.common.utils.StringUtils;

/**
 * 租户上下文持有器
 * 在请求线程内保存当前租户ID
 *
 * @author ruoyi
 */
public class TenantContextHolder
{
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void setTenantId(Long tenantId)
    {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId()
    {
        return TENANT_ID.get();
    }

    public static void clear()
    {
        if (TENANT_ID.get() != null)
        {
            TENANT_ID.remove();
        }
    }

    /**
     * 检查是否有租户上下文
     */
    public static boolean hasTenant()
    {
        return TENANT_ID.get() != null;
    }

    /**
     * 获取租户ID字符串，未设置返回 null
     */
    public static String getTenantIdStr()
    {
        Long tenantId = TENANT_ID.get();
        return tenantId != null ? String.valueOf(tenantId) : null;
    }
}

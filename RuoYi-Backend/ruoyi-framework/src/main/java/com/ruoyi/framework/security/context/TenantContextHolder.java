package com.ruoyi.framework.security.context;

/**
 * 租户上下文持有器（委托给 ruoyi-common）
 *
 * @author ruoyi
 * @deprecated use {@link com.ruoyi.common.tenant.TenantContextHolder}
 */
@Deprecated
public class TenantContextHolder
{
    public static void setTenantId(Long tenantId)
    {
        com.ruoyi.common.tenant.TenantContextHolder.setTenantId(tenantId);
    }

    public static Long getTenantId()
    {
        return com.ruoyi.common.tenant.TenantContextHolder.getTenantId();
    }

    public static void clear()
    {
        com.ruoyi.common.tenant.TenantContextHolder.clear();
    }

    public static boolean hasTenant()
    {
        return com.ruoyi.common.tenant.TenantContextHolder.hasTenant();
    }

    public static String getTenantIdStr()
    {
        return com.ruoyi.common.tenant.TenantContextHolder.getTenantIdStr();
    }
}

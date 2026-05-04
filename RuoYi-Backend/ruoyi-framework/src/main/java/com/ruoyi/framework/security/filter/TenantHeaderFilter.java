package com.ruoyi.framework.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.tenant.TenantContextHolder;

/**
 * 多租户Header过滤器
 * 
 * 从请求头 Tenant-Id 中读取租户ID并设置到 TenantContextHolder。
 * 前端（网关/代理层）会在请求中携带当前用户的 Tenant-Id header，
 * 该filter在 JwtAuthenticationTokenFilter 之前执行，
 * 确保在安全认证完成之前就完成租户上下文的设置。
 * 
 * 注意：只有当 TenantContextHolder 尚未被设置时才生效（防止JwtAuthenticationTokenFilter覆盖）。
 * 
 * @author ruoyi
 */
@Component
public class TenantHeaderFilter extends OncePerRequestFilter
{
    /** 请求头租户IDkey */
    private static final String TENANT_HEADER = "Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        try
        {
            // 只有在租户上下文尚未设置时才从header读取
            // 这样 JwtAuthenticationTokenFilter 中从JWT解析出的 tenantId 可以覆盖header值
            if (!TenantContextHolder.hasTenant())
            {
                String tenantIdStr = request.getHeader(TENANT_HEADER);
                if (StringUtils.isNotBlank(tenantIdStr))
                {
                    try
                    {
                        Long tenantId = Long.parseLong(tenantIdStr.trim());
                        TenantContextHolder.setTenantId(tenantId);
                    }
                    catch (NumberFormatException e)
                    {
                        // 非数字格式的tenant-id，忽略
                    }
                }
            }
        }
        finally
        {
            chain.doFilter(request, response);
        }
    }
}

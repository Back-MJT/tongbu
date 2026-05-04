package com.ruoyi.iot.service;

import com.ruoyi.iot.domain.model.EquipmentResolveResult;

/**
 * 小程序器械解析服务
 */
public interface IMiniEquipmentService
{
    EquipmentResolveResult resolveEquipment(String scanCode, Long tenantId);
}

package com.ruoyi.iot.mapper;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.iot.domain.model.EquipmentResolveResult;

/**
 * 小程序扫码器械解析 Mapper
 */
public interface MiniEquipmentMapper
{
    EquipmentResolveResult selectEquipmentResolveResult(@Param("equipmentCode") String equipmentCode,
                                                        @Param("tenantId") Long tenantId);
}

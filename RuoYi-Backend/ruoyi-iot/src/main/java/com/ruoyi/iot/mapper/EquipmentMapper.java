package com.ruoyi.iot.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.iot.domain.entity.Equipment;

/**
 * 器械 Mapper
 */
public interface EquipmentMapper
{
    List<Equipment> selectEquipmentList(Equipment equipment);

    Equipment selectEquipmentById(Equipment equipment);

    Equipment selectEquipmentByCode(Equipment equipment);

    int insertEquipment(Equipment equipment);

    int updateEquipment(Equipment equipment);

    int deleteEquipmentById(Equipment equipment);

    int deleteEquipmentByIds(@Param("equipmentIds") Long[] equipmentIds, @Param("tenantId") Long tenantId);
}

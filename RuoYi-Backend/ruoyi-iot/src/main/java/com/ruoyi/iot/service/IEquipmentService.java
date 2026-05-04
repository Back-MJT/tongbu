package com.ruoyi.iot.service;

import java.util.List;
import com.ruoyi.iot.domain.entity.Equipment;

/**
 * 器械 Service
 */
public interface IEquipmentService
{
    List<Equipment> selectEquipmentList(Equipment equipment);

    Equipment selectEquipmentById(Long equipmentId);

    Equipment selectEquipmentByCode(String equipmentCode);

    int insertEquipment(Equipment equipment);

    int updateEquipment(Equipment equipment);

    int deleteEquipmentById(Long equipmentId);

    int deleteEquipmentByIds(Long[] equipmentIds);
}

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

    int deactivateEquipmentDeviceBindings(@Param("equipmentId") Long equipmentId, @Param("tenantId") Long tenantId, @Param("updateBy") String updateBy);

    int deactivateDeviceBindings(@Param("deviceId") Long deviceId, @Param("tenantId") Long tenantId, @Param("updateBy") String updateBy);

    int insertEquipmentDeviceBinding(@Param("equipmentId") Long equipmentId, @Param("deviceId") Long deviceId, @Param("tenantId") Long tenantId, @Param("createBy") String createBy);

    int updateEquipmentSensorFields(@Param("equipmentId") Long equipmentId, @Param("tenantId") Long tenantId,
                                    @Param("deviceId") Long deviceId, @Param("deviceCode") String deviceCode,
                                    @Param("bluetoothName") String bluetoothName, @Param("updateBy") String updateBy);

    int clearEquipmentSensorFieldsByDevice(@Param("deviceId") Long deviceId, @Param("tenantId") Long tenantId,
                                           @Param("updateBy") String updateBy);

    int deleteEquipmentById(Equipment equipment);

    int deleteEquipmentByIds(@Param("equipmentIds") Long[] equipmentIds, @Param("tenantId") Long tenantId);
}

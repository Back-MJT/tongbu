package com.ruoyi.iot.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.tenant.TenantContextHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.iot.domain.entity.Equipment;
import com.ruoyi.iot.domain.entity.IoTDevice;
import com.ruoyi.iot.mapper.EquipmentMapper;
import com.ruoyi.iot.mapper.IoTDeviceMapper;
import com.ruoyi.iot.service.IEquipmentService;

/**
 * 器械 Service 实现
 */
@Service
public class EquipmentServiceImpl implements IEquipmentService
{
    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private IoTDeviceMapper deviceMapper;

    @Override
    public List<Equipment> selectEquipmentList(Equipment equipment)
    {
        equipment.setTenantId(TenantContextHolder.getTenantId());
        return equipmentMapper.selectEquipmentList(equipment);
    }

    @Override
    public Equipment selectEquipmentById(Long equipmentId)
    {
        Equipment equipment = new Equipment();
        equipment.setEquipmentId(equipmentId);
        equipment.setTenantId(TenantContextHolder.getTenantId());
        return equipmentMapper.selectEquipmentById(equipment);
    }

    @Override
    public Equipment selectEquipmentByCode(String equipmentCode)
    {
        Equipment equipment = new Equipment();
        equipment.setEquipmentCode(equipmentCode);
        equipment.setTenantId(TenantContextHolder.getTenantId());
        return equipmentMapper.selectEquipmentByCode(equipment);
    }

    @Override
    @Transactional
    public int insertEquipment(Equipment equipment)
    {
        equipment.setTenantId(TenantContextHolder.getTenantId());
        int rows = equipmentMapper.insertEquipment(equipment);
        refreshSensorBinding(equipment);
        return rows;
    }

    @Override
    @Transactional
    public int updateEquipment(Equipment equipment)
    {
        equipment.setTenantId(TenantContextHolder.getTenantId());
        int rows = equipmentMapper.updateEquipment(equipment);
        refreshSensorBinding(equipment);
        return rows;
    }

    @Override
    @Transactional
    public int deleteEquipmentById(Long equipmentId)
    {
        Equipment equipment = new Equipment();
        equipment.setEquipmentId(equipmentId);
        equipment.setTenantId(TenantContextHolder.getTenantId());
        return equipmentMapper.deleteEquipmentById(equipment);
    }

    @Override
    @Transactional
    public int deleteEquipmentByIds(Long[] equipmentIds)
    {
        return equipmentMapper.deleteEquipmentByIds(equipmentIds, TenantContextHolder.getTenantId());
    }

    private void refreshSensorBinding(Equipment equipment)
    {
        if (equipment.getEquipmentId() == null)
        {
            return;
        }

        Long tenantId = equipment.getTenantId();
        String operator = StringUtils.isNotEmpty(equipment.getUpdateBy()) ? equipment.getUpdateBy() : equipment.getCreateBy();
        boolean hasSensorInput = equipment.getDeviceId() != null
                || StringUtils.isNotEmpty(equipment.getDeviceCode())
                || StringUtils.isNotEmpty(equipment.getBluetoothName());
        if (!hasSensorInput)
        {
            equipmentMapper.deactivateEquipmentDeviceBindings(equipment.getEquipmentId(), tenantId, operator);
            equipmentMapper.updateEquipmentSensorFields(equipment.getEquipmentId(), tenantId, null, null, null, operator);
            return;
        }

        IoTDevice device = null;
        if (equipment.getDeviceId() != null)
        {
            IoTDevice query = new IoTDevice();
            query.setDeviceId(equipment.getDeviceId());
            query.setTenantId(tenantId);
            device = deviceMapper.selectDeviceById(query);
        }
        if (device == null && StringUtils.isNotEmpty(equipment.getDeviceCode()))
        {
            IoTDevice query = new IoTDevice();
            query.setDeviceCode(equipment.getDeviceCode());
            query.setTenantId(tenantId);
            device = deviceMapper.selectDeviceByCode(query);
        }
        if (device == null && StringUtils.isNotEmpty(equipment.getBluetoothName()))
        {
            device = deviceMapper.selectDeviceByBluetoothName(equipment.getBluetoothName(), tenantId);
        }
        if (device == null)
        {
            throw new ServiceException("未找到对应蓝牙传感器，请先在设备管理中维护设备编号或蓝牙名称");
        }

        equipmentMapper.deactivateDeviceBindings(device.getDeviceId(), tenantId, operator);
        equipmentMapper.clearEquipmentSensorFieldsByDevice(device.getDeviceId(), tenantId, operator);
        equipmentMapper.deactivateEquipmentDeviceBindings(equipment.getEquipmentId(), tenantId, operator);
        equipmentMapper.insertEquipmentDeviceBinding(equipment.getEquipmentId(), device.getDeviceId(), tenantId, operator);
        equipmentMapper.updateEquipmentSensorFields(equipment.getEquipmentId(), tenantId, device.getDeviceId(),
                device.getDeviceCode(), device.getBluetoothName(), operator);
    }
}

package com.ruoyi.iot.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.tenant.TenantContextHolder;
import com.ruoyi.iot.domain.entity.Equipment;
import com.ruoyi.iot.mapper.EquipmentMapper;
import com.ruoyi.iot.service.IEquipmentService;

/**
 * 器械 Service 实现
 */
@Service
public class EquipmentServiceImpl implements IEquipmentService
{
    @Autowired
    private EquipmentMapper equipmentMapper;

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
        return equipmentMapper.insertEquipment(equipment);
    }

    @Override
    @Transactional
    public int updateEquipment(Equipment equipment)
    {
        equipment.setTenantId(TenantContextHolder.getTenantId());
        return equipmentMapper.updateEquipment(equipment);
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
}

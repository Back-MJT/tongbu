package com.ruoyi.iot.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.iot.mapper.ManufacturerMapper;
import com.ruoyi.iot.domain.entity.Manufacturer;
import com.ruoyi.iot.service.IManufacturerService;
import com.ruoyi.common.tenant.TenantContextHolder;

/**
 * IoT 厂商 业务层处理
 *
 * @author ruoyi
 */
@Service
public class ManufacturerServiceImpl implements IManufacturerService
{
    @Autowired
    private ManufacturerMapper manufacturerMapper;

    /**
     * 查询厂商列表
     */
    @Override
    public List<Manufacturer> selectManufacturerList(Manufacturer manufacturer)
    {
        manufacturer.setTenantId(TenantContextHolder.getTenantId());
        return manufacturerMapper.selectManufacturerList(manufacturer);
    }

    /**
     * 查询厂商详情
     */
    @Override
    public Manufacturer selectManufacturerById(Long manufacturerId)
    {
        Manufacturer m = new Manufacturer();
        m.setManufacturerId(manufacturerId);
        m.setTenantId(TenantContextHolder.getTenantId());
        return manufacturerMapper.selectManufacturerById(m);
    }

    /**
     * 根据厂商名查询
     */
    @Override
    public Manufacturer selectManufacturerByName(String manufacturerName)
    {
        Manufacturer m = new Manufacturer();
        m.setManufacturerName(manufacturerName);
        m.setTenantId(TenantContextHolder.getTenantId());
        return manufacturerMapper.selectManufacturerByName(m);
    }

    /**
     * 新增厂商
     */
    @Override
    @Transactional
    public int insertManufacturer(Manufacturer manufacturer)
    {
        manufacturer.setTenantId(TenantContextHolder.getTenantId());
        return manufacturerMapper.insertManufacturer(manufacturer);
    }

    /**
     * 修改厂商
     */
    @Override
    @Transactional
    public int updateManufacturer(Manufacturer manufacturer)
    {
        manufacturer.setTenantId(TenantContextHolder.getTenantId());
        return manufacturerMapper.updateManufacturer(manufacturer);
    }

    /**
     * 删除厂商（逻辑删除）
     */
    @Override
    @Transactional
    public int deleteManufacturerById(Long manufacturerId)
    {
        Manufacturer m = new Manufacturer();
        m.setManufacturerId(manufacturerId);
        m.setTenantId(TenantContextHolder.getTenantId());
        return manufacturerMapper.deleteManufacturerById(m);
    }

    /**
     * 批量删除厂商
     */
    @Override
    @Transactional
    public int deleteManufacturerByIds(Long[] manufacturerIds)
    {
        Manufacturer m = new Manufacturer();
        m.setTenantId(TenantContextHolder.getTenantId());
        return manufacturerMapper.deleteManufacturerByIds(manufacturerIds, m.getTenantId());
    }

    /**
     * 检查厂商是否有设备
     */
    @Override
    public int countDevicesByManufacturerId(Long manufacturerId)
    {
        return manufacturerMapper.countDevicesByManufacturerId(manufacturerId);
    }
}

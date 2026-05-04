package com.ruoyi.iot.service;

import java.util.List;
import com.ruoyi.iot.domain.entity.Manufacturer;

/**
 * IoT 厂商 Service接口
 *
 * @author ruoyi
 */
public interface IManufacturerService
{
    /**
     * 查询厂商列表
     */
    public List<Manufacturer> selectManufacturerList(Manufacturer manufacturer);

    /**
     * 查询厂商详情
     */
    public Manufacturer selectManufacturerById(Long manufacturerId);

    /**
     * 根据厂商名查询
     */
    public Manufacturer selectManufacturerByName(String manufacturerName);

    /**
     * 新增厂商
     */
    public int insertManufacturer(Manufacturer manufacturer);

    /**
     * 修改厂商
     */
    public int updateManufacturer(Manufacturer manufacturer);

    /**
     * 删除厂商（逻辑删除）
     */
    public int deleteManufacturerById(Long manufacturerId);

    /**
     * 批量删除厂商
     */
    public int deleteManufacturerByIds(Long[] manufacturerIds);

    /**
     * 检查厂商是否有设备（用于删除前校验）
     */
    public int countDevicesByManufacturerId(Long manufacturerId);
}

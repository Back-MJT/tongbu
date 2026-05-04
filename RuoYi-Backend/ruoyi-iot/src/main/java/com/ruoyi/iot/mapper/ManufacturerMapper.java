package com.ruoyi.iot.mapper;

import java.util.List;
import com.ruoyi.iot.domain.entity.Manufacturer;
import org.apache.ibatis.annotations.Param;

/**
 * IoT 厂商 Mapper 接口
 *
 * @author ruoyi
 */
public interface ManufacturerMapper
{
    /**
     * 查询厂商列表
     */
    public List<Manufacturer> selectManufacturerList(Manufacturer manufacturer);

    /**
     * 查询厂商详情
     */
    public Manufacturer selectManufacturerById(Manufacturer manufacturer);

    /**
     * 根据厂商名查询
     */
    public Manufacturer selectManufacturerByName(Manufacturer manufacturer);

    /**
     * 校验厂商名称唯一性
     */
    public Manufacturer checkManufacturerNameUnique(@Param("manufacturerName") String manufacturerName,
                                                     @Param("tenantId") Long tenantId,
                                                     @Param("manufacturerId") Long manufacturerId);

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
    public int deleteManufacturerById(Manufacturer manufacturer);

    /**
     * 批量删除厂商
     */
    public int deleteManufacturerByIds(@Param("manufacturerIds") Long[] manufacturerIds,
                                      @Param("tenantId") Long tenantId);

    /**
     * 检查厂商是否有设备
     */
    public int countDevicesByManufacturerId(Long manufacturerId);
}

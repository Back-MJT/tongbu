package com.ruoyi.iot.domain.entity;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * IoT 设备分组表 iot_device_group
 *
 * @author ruoyi
 */
public class DeviceGroup extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分组ID */
    private Long groupId;

    /** 分组名称 */
    private String groupName;

    /** 厂商ID */
    private Long manufacturerId;

    /** 描述 */
    private String description;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 厂商名称（关联查询填充） */
    private String manufacturerName;

    /** 设备数量（关联查询填充） */
    private Integer deviceCount;

    public Long getGroupId()
    {
        return groupId;
    }

    public void setGroupId(Long groupId)
    {
        this.groupId = groupId;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public Long getManufacturerId()
    {
        return manufacturerId;
    }

    public void setManufacturerId(Long manufacturerId)
    {
        this.manufacturerId = manufacturerId;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getManufacturerName()
    {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName)
    {
        this.manufacturerName = manufacturerName;
    }

    public Integer getDeviceCount()
    {
        return deviceCount;
    }

    public void setDeviceCount(Integer deviceCount)
    {
        this.deviceCount = deviceCount;
    }

    @Override
    public String toString()
    {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this)
            .append("groupId", getGroupId())
            .append("groupName", getGroupName())
            .append("manufacturerId", getManufacturerId())
            .append("delFlag", getDelFlag())
            .toString();
    }
}

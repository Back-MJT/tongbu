package com.ruoyi.iot.domain.entity;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * IoT 厂商表 iot_manufacturer
 *
 * @author ruoyi
 */
public class Manufacturer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 厂商ID */
    private Long manufacturerId;

    /** 厂商名称 */
    private String manufacturerName;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 地址 */
    private String address;

    /** 营业执照号 */
    private String businessLicense;

    /** 状态（0=正常，1=停用） */
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getManufacturerId()
    {
        return manufacturerId;
    }

    public void setManufacturerId(Long manufacturerId)
    {
        this.manufacturerId = manufacturerId;
    }

    public String getManufacturerName()
    {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName)
    {
        this.manufacturerName = manufacturerName;
    }

    public String getContactPerson()
    {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson)
    {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getBusinessLicense()
    {
        return businessLicense;
    }

    public void setBusinessLicense(String businessLicense)
    {
        this.businessLicense = businessLicense;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this)
            .append("manufacturerId", getManufacturerId())
            .append("manufacturerName", getManufacturerName())
            .append("contactPerson", getContactPerson())
            .append("contactPhone", getContactPhone())
            .append("address", getAddress())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .toString();
    }
}

package com.ruoyi.intervention.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.intervention.entity.IotDeviceBinding;

/**
 * 用户-设备绑定 Mapper
 */
public interface IotDeviceBindingMapper {

    /**
     * 查询用户所有有效绑定
     */
    List<IotDeviceBinding> selectByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 根据设备编号查询绑定记录
     */
    IotDeviceBinding selectByDeviceCode(@Param("deviceCode") String deviceCode, @Param("tenantId") Long tenantId);

    /**
     * 根据bindingId查询
     */
    IotDeviceBinding selectByBindingId(@Param("bindingId") Long bindingId);

    /**
     * 插入绑定记录
     */
    int insertBinding(IotDeviceBinding binding);

    /**
     * 解绑设备 (软删除)
     */
    int unbindDevice(@Param("bindingId") Long bindingId, @Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 激活绑定 (恢复)
     */
    int activateBinding(@Param("bindingId") Long bindingId);

    /**
     * 查询设备是否已被绑定
     */
    IotDeviceBinding selectActiveByDeviceCode(@Param("deviceCode") String deviceCode, @Param("tenantId") Long tenantId);
}

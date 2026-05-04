package com.ruoyi.intervention.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.intervention.entity.IntervPrescription;

/**
 * 小程序Service接口
 */
public interface IMiniProgramService
{
    /**
     * 查询用户最新有效处方
     */
    IntervPrescription getLatestPrescription(String userId, Long tenantId);

    /**
     * 查询用户处方历史
     */
    List<IntervPrescription> getPrescriptionHistory(String userId, Long tenantId, int page, int size);

    /**
     * 获取用户训练进度（今日）
     */
    Map<String, Object> getTodayProgress(String userId, Long tenantId);

    /**
     * 获取用户训练历史（分页）
     */
    Map<String, Object> getTrainingHistory(String userId, Long tenantId, int page, int size);

    /**
     * 绑定设备到用户
     */
    boolean bindDevice(String userId, String deviceCode, Long tenantId);

    /**
     * 解绑设备
     */
    boolean unbindDevice(Long bindingId, Long tenantId);
}

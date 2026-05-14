package com.ruoyi.intervention.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.intervention.entity.IntervPrescription;

/**
 * 干预处方Mapper接口
 */
public interface IntervPrescriptionMapper
{
    /**
     * 查询处方列表
     */
    List<IntervPrescription> selectPrescriptionList(IntervPrescription prescription);

    /**
     * 根据用户ID查询最新有效处方
     */
    IntervPrescription selectLatestByUserId(@Param("userId") String userId, @Param("tenantId") Long tenantId);

    /**
     * 根据用户ID查询处方列表
     */
    List<IntervPrescription> selectByUserId(@Param("userId") String userId, @Param("tenantId") Long tenantId);

    IntervPrescription selectPrescriptionById(@Param("prescriptionId") Long prescriptionId, @Param("tenantId") Long tenantId);

    /**
     * 插入处方
     */
    int insertPrescription(IntervPrescription prescription);

    int updatePrescription(IntervPrescription prescription);

    int deactivateUserPrescriptions(@Param("userId") String userId, @Param("tenantId") Long tenantId, @Param("updateBy") String updateBy);

    int deletePrescriptionById(@Param("prescriptionId") Long prescriptionId, @Param("tenantId") Long tenantId, @Param("updateBy") String updateBy);
}

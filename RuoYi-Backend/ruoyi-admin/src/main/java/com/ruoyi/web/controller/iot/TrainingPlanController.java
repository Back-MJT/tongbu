package com.ruoyi.web.controller.iot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.tenant.TenantContextHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.intervention.entity.IntervPrescription;
import com.ruoyi.intervention.mapper.IntervPrescriptionMapper;

/**
 * 用户训练计划管理
 */
@RestController
@RequestMapping("/iot/training/plan")
public class TrainingPlanController extends BaseController
{
    @Autowired
    private IntervPrescriptionMapper prescriptionMapper;

    @PreAuthorize("@ss.hasAnyPermi('iot:training:plan:list,iot:training:query')")
    @GetMapping("/list")
    public TableDataInfo list(IntervPrescription query)
    {
        query.setTenantId(TenantContextHolder.getTenantId());
        query.setInterventionType("exercise");
        startPage();
        List<IntervPrescription> list = prescriptionMapper.selectPrescriptionList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasAnyPermi('iot:training:plan:query,iot:training:query')")
    @GetMapping("/{prescriptionId}")
    public AjaxResult getInfo(@PathVariable Long prescriptionId)
    {
        return success(prescriptionMapper.selectPrescriptionById(prescriptionId, TenantContextHolder.getTenantId()));
    }

    @PreAuthorize("@ss.hasAnyPermi('iot:training:plan:add,iot:training:query')")
    @Log(title = "用户训练计划", businessType = BusinessType.INSERT)
    @PostMapping
    @Transactional
    public AjaxResult add(@RequestBody IntervPrescription prescription)
    {
        validatePlan(prescription);
        Long tenantId = TenantContextHolder.getTenantId();
        prescription.setTenantId(tenantId);
        prescription.setInterventionType("exercise");
        prescription.setDelFlag("0");
        prescription.setCompletionRate(0D);
        prescription.setVersion(1);
        prescription.setCreateBy(getUsername());
        if (StringUtils.isEmpty(prescription.getStatus()))
        {
            prescription.setStatus("active");
        }
        if ("active".equals(prescription.getStatus()))
        {
            prescriptionMapper.deactivateUserPrescriptions(prescription.getUserId(), tenantId, getUsername());
        }
        if (StringUtils.isEmpty(prescription.getPrescriptionNo()))
        {
            prescription.setPrescriptionNo("TP-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        }
        return toAjax(prescriptionMapper.insertPrescription(prescription));
    }

    @PreAuthorize("@ss.hasAnyPermi('iot:training:plan:edit,iot:training:query')")
    @Log(title = "用户训练计划", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional
    public AjaxResult edit(@RequestBody IntervPrescription prescription)
    {
        if (prescription.getPrescriptionId() == null)
        {
            return error("训练计划ID不能为空");
        }
        validatePlan(prescription);
        Long tenantId = TenantContextHolder.getTenantId();
        prescription.setTenantId(tenantId);
        prescription.setInterventionType("exercise");
        prescription.setUpdateBy(getUsername());
        if ("active".equals(prescription.getStatus()))
        {
            prescriptionMapper.deactivateUserPrescriptions(prescription.getUserId(), tenantId, getUsername());
        }
        return toAjax(prescriptionMapper.updatePrescription(prescription));
    }

    @PreAuthorize("@ss.hasAnyPermi('iot:training:plan:edit,iot:training:query')")
    @Log(title = "用户训练计划", businessType = BusinessType.UPDATE)
    @PutMapping("/activate/{prescriptionId}")
    @Transactional
    public AjaxResult activate(@PathVariable Long prescriptionId)
    {
        Long tenantId = TenantContextHolder.getTenantId();
        IntervPrescription existing = prescriptionMapper.selectPrescriptionById(prescriptionId, tenantId);
        if (existing == null)
        {
            return error("训练计划不存在");
        }
        prescriptionMapper.deactivateUserPrescriptions(existing.getUserId(), tenantId, getUsername());
        IntervPrescription update = new IntervPrescription();
        update.setPrescriptionId(prescriptionId);
        update.setTenantId(tenantId);
        update.setUserId(existing.getUserId());
        update.setStatus("active");
        update.setUpdateBy(getUsername());
        return toAjax(prescriptionMapper.updatePrescription(update));
    }

    @PreAuthorize("@ss.hasAnyPermi('iot:training:plan:remove,iot:training:query')")
    @Log(title = "用户训练计划", businessType = BusinessType.DELETE)
    @DeleteMapping("/{prescriptionId}")
    public AjaxResult remove(@PathVariable Long prescriptionId)
    {
        return toAjax(prescriptionMapper.deletePrescriptionById(
                prescriptionId, TenantContextHolder.getTenantId(), getUsername()));
    }

    private void validatePlan(IntervPrescription prescription)
    {
        if (prescription == null || StringUtils.isEmpty(prescription.getUserId()))
        {
            throw new ServiceException("请选择用户");
        }
        if (StringUtils.isEmpty(prescription.getRecommendations()))
        {
            throw new ServiceException("训练计划内容不能为空");
        }
        if (prescription.getDurationDays() == null || prescription.getDurationDays() <= 0)
        {
            prescription.setDurationDays(7);
        }
        if (prescription.getStartDate() == null)
        {
            prescription.setStartDate(new Date());
        }
    }
}

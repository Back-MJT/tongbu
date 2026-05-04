package com.ruoyi.iot.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.iot.domain.entity.Equipment;
import com.ruoyi.iot.service.IEquipmentService;

/**
 * 器械管理
 */
@RestController
@RequestMapping("/iot/equipment")
public class EquipmentController extends BaseController
{
    @Autowired
    private IEquipmentService equipmentService;

    @PreAuthorize("@ss.hasPermi('iot:equipment:list')")
    @GetMapping("/list")
    public TableDataInfo list(Equipment equipment)
    {
        startPage();
        List<Equipment> list = equipmentService.selectEquipmentList(equipment);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('iot:equipment:export')")
    @Log(title = "器械", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Equipment equipment)
    {
        List<Equipment> list = equipmentService.selectEquipmentList(equipment);
        ExcelUtil<Equipment> util = new ExcelUtil<>(Equipment.class);
        util.exportExcel(response, list, "器械数据");
    }

    @PreAuthorize("@ss.hasPermi('iot:equipment:query')")
    @GetMapping("/{equipmentId}")
    public AjaxResult getInfo(@PathVariable Long equipmentId)
    {
        return success(equipmentService.selectEquipmentById(equipmentId));
    }

    @PreAuthorize("@ss.hasPermi('iot:equipment:query')")
    @GetMapping("/code/{equipmentCode}")
    public AjaxResult getInfoByCode(@PathVariable String equipmentCode)
    {
        return success(equipmentService.selectEquipmentByCode(equipmentCode));
    }

    @PreAuthorize("@ss.hasPermi('iot:equipment:add')")
    @Log(title = "器械", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Equipment equipment)
    {
        equipment.setCreateBy(getUsername());
        return toAjax(equipmentService.insertEquipment(equipment));
    }

    @PreAuthorize("@ss.hasPermi('iot:equipment:edit')")
    @Log(title = "器械", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Equipment equipment)
    {
        equipment.setUpdateBy(getUsername());
        return toAjax(equipmentService.updateEquipment(equipment));
    }

    @PreAuthorize("@ss.hasPermi('iot:equipment:remove')")
    @Log(title = "器械", businessType = BusinessType.DELETE)
    @DeleteMapping("/{equipmentIds}")
    public AjaxResult remove(@PathVariable Long[] equipmentIds)
    {
        return toAjax(equipmentService.deleteEquipmentByIds(equipmentIds));
    }
}

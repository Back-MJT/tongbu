package com.ruoyi.iot.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.iot.domain.entity.TrainingSessionRecord;
import com.ruoyi.iot.service.ITrainingSessionService;

/**
 * 训练记录查询
 */
@RestController
@RequestMapping("/iot/training/session")
public class TrainingSessionController extends BaseController
{
    @Autowired
    private ITrainingSessionService trainingSessionService;

    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/list")
    public TableDataInfo list(TrainingSessionRecord query)
    {
        startPage();
        List<TrainingSessionRecord> list = trainingSessionService.selectTrainingSessionList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/{sessionId}")
    public AjaxResult getInfo(@PathVariable Long sessionId)
    {
        return success(trainingSessionService.selectTrainingSessionDetail(sessionId));
    }
}

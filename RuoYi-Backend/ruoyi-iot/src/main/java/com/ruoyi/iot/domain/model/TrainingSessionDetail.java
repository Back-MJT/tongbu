package com.ruoyi.iot.domain.model;

import java.util.ArrayList;
import java.util.List;
import com.ruoyi.iot.domain.entity.TrainingSessionRecord;
import com.ruoyi.iot.domain.entity.TrainingSetDetail;

/**
 * 训练会话详情
 */
public class TrainingSessionDetail
{
    private TrainingSessionRecord session;
    private List<TrainingSetDetail> sets = new ArrayList<>();

    public TrainingSessionRecord getSession() { return session; }
    public void setSession(TrainingSessionRecord session) { this.session = session; }
    public List<TrainingSetDetail> getSets() { return sets; }
    public void setSets(List<TrainingSetDetail> sets) { this.sets = sets; }
}

package com.ruoyi.intervention.mapper;

import java.util.List;
import com.ruoyi.intervention.entity.TrainingSetRecord;

/**
 * 训练组明细 Mapper
 */
public interface TrainingSetRecordMapper {

    int insertBatch(List<TrainingSetRecord> records);
}

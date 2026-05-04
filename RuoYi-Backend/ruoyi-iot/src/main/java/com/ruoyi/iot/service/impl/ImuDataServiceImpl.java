package com.ruoyi.iot.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.tenant.TenantContextHolder;
import com.ruoyi.iot.domain.entity.IoTDevice;
import com.ruoyi.iot.domain.entity.ImuDataRecord;
import com.ruoyi.iot.domain.model.ImuData;
import com.ruoyi.iot.mapper.ImuDataMapper;
import com.ruoyi.iot.mapper.IoTDeviceMapper;
import com.ruoyi.iot.service.IImuDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IMU时序数据 Service实现
 *
 * 设计：高频IMU数据（100Hz采样）不能逐条写入DB
 * 策略：内存缓冲区 + 定时批量持久化
 *   - 每5秒或缓冲区达到100条时批量写入
 *   - 应用关闭时强制刷新
 *
 * @author ruoyi
 */
@Service
public class ImuDataServiceImpl implements IImuDataService
{
    private static final Logger log = LoggerFactory.getLogger(ImuDataServiceImpl.class);

    /** 批量写入阈值 */
    private static final int BATCH_SIZE = 100;

    /** MQTT消息缓冲区（无界线程安全队列） */
    private final ConcurrentLinkedQueue<ImuDataRecord> buffer = new ConcurrentLinkedQueue<>();

    /** 刷新标记（防止并发刷新） */
    private final AtomicBoolean flushing = new AtomicBoolean(false);

    @Autowired
    private ImuDataMapper imuDataMapper;

    @Autowired
    private IoTDeviceMapper deviceMapper;

    // ---- IImuDataService 实现 ----

    @Override
    public void onImuDataReceived(ImuData imuData, Long deviceId)
    {
        ImuDataRecord record = toRecord(imuData, deviceId);
        buffer.add(record);

        // 达到阈值时触发异步刷新
        if (buffer.size() >= BATCH_SIZE)
        {
            triggerFlush();
        }
    }

    @Override
    @PreDestroy
    public void flushBuffer()
    {
        doFlush("shutdown");
    }

    @Override
    public List<ImuDataRecord> selectLatestByDeviceCode(String deviceCode, int limit)
    {
        return imuDataMapper.selectLatestByDeviceCode(deviceCode, limit);
    }

    @Override
    public List<ImuDataRecord> selectByDeviceCodeAndTimeRange(String deviceCode, Date beginTime, Date endTime)
    {
        return imuDataMapper.selectByDeviceCodeAndTimeRange(deviceCode, beginTime, endTime);
    }

    @Override
    public long countByDeviceCodeAndTimeRange(String deviceCode, Date beginTime, Date endTime)
    {
        return imuDataMapper.countByDeviceCodeAndTimeRange(deviceCode, beginTime, endTime);
    }

    // ---- 内部方法 ----

    /**
     * 定时任务：每5秒检查并刷新缓冲区
     */
    @Scheduled(fixedDelayString = "${iot.imu.flush-interval-ms:5000}")
    public void scheduledFlush()
    {
        if (!buffer.isEmpty())
        {
            doFlush("scheduled");
        }
    }

    /**
     * 触发异步刷新（非阻塞）
     */
    private void triggerFlush()
    {
        if (flushing.compareAndSet(false, true))
        {
            try
            {
                doFlush("threshold");
            }
            finally
            {
                flushing.set(false);
            }
        }
    }

    /**
     * 执行批量写入
     * @param reason 日志标识（shutdown/scheduled/threshold）
     */
    @Transactional
    public void doFlush(String reason)
    {
        if (buffer.isEmpty())
        {
            return;
        }

        // 取出最多BATCH_SIZE条（避免单次SQL过大）
        List<ImuDataRecord> batch = new ArrayList<>(BATCH_SIZE);
        while (!buffer.isEmpty() && batch.size() < BATCH_SIZE)
        {
            ImuDataRecord record = buffer.poll();
            if (record != null)
            {
                batch.add(record);
            }
        }

        if (batch.isEmpty())
        {
            return;
        }

        try
        {
            long start = System.currentTimeMillis();
            int rows = imuDataMapper.batchInsert(batch);
            long cost = System.currentTimeMillis() - start;
            log.info("[IMU Flush][{}] wrote {} records in {}ms", reason, rows, cost);
        }
        catch (Exception e)
        {
            // 写入失败时记录错误（不丢数据，重新入队稍后重试）
            log.error("[IMU Flush][{}] batch insert failed: {}", reason, e.getMessage());
            buffer.addAll(batch);
        }
    }

    /**
     * ImuData (MQTT消息) → ImuDataRecord (DB实体)
     * 尝试将deviceCode解析为deviceId（TimescaleDB按device_id分区）
     */
    private ImuDataRecord toRecord(ImuData imuData, Long deviceId)
    {
        ImuDataRecord record = new ImuDataRecord();
        record.setTime(imuData.getSampleTime() != null ? imuData.getSampleTime() : new Date());
        record.setDeviceCode(imuData.getDeviceCode());

        // 若未传入deviceId，尝试从数据库查找（避免IMU数据丢失device_id维度）
        if (deviceId == null && StringUtils.isNotEmpty(imuData.getDeviceCode()))
        {
            IoTDevice d = new IoTDevice();
            d.setDeviceCode(imuData.getDeviceCode());
            d.setTenantId(TenantContextHolder.getTenantId());
            IoTDevice device = deviceMapper.selectDeviceByCode(d);
            if (device != null)
            {
                deviceId = device.getDeviceId();
            }
        }
        record.setDeviceId(deviceId);

        record.setAccelX(imuData.getAccelX());
        record.setAccelY(imuData.getAccelY());
        record.setAccelZ(imuData.getAccelZ());
        record.setGyroX(imuData.getGyroX());
        record.setGyroY(imuData.getGyroY());
        record.setGyroZ(imuData.getGyroZ());
        record.setSequence(imuData.getSequence());
        record.setBatteryLevel(imuData.getBatteryLevel());
        record.setMotionType(imuData.getMotionType());
        record.setStepCount(imuData.getStepCount());
        return record;
    }
}

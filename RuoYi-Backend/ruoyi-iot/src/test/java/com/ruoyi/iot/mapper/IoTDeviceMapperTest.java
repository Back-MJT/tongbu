package com.ruoyi.iot.mapper;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.iot.domain.entity.IoTDevice;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IoTDeviceMapper 集成测试
 * 
 * 使用 @MybatisTest 专注于 MyBatis Mapper 层测试
 */
@MybatisTest
@Transactional
public class IoTDeviceMapperTest
{
    @Autowired
    private IoTDeviceMapper deviceMapper;

    private IoTDevice testDevice;
    private Long tenantId = 1L;

    @BeforeEach
    public void setUp()
    {
        testDevice = new IoTDevice();
        testDevice.setDeviceCode("TEST-" + UUID.randomUUID().toString().substring(0, 8));
        testDevice.setDeviceName("测试设备");
        testDevice.setDeviceType("treadmill");
        testDevice.setProtocol("mqtt");
        testDevice.setManufacturerId(1L);
        testDevice.setTenantId(tenantId);
        testDevice.setStatus("online");
        testDevice.setFirmwareVersion("1.0.0");
        testDevice.setMetadata("{}");
        testDevice.setDelFlag("0");
        testDevice.setCreateBy("test");
        testDevice.setCreateTime(new Date());
    }

    @Test
    public void testInsertDevice()
    {
        int result = deviceMapper.insertDevice(testDevice);
        assertTrue(result > 0, "设备插入应返回 > 0");
        assertNotNull(testDevice.getDeviceId(), "设备ID应自动生成");
    }

    @Test
    public void testSelectDeviceById()
    {
        deviceMapper.insertDevice(testDevice);

        IoTDevice query = new IoTDevice();
        query.setDeviceId(testDevice.getDeviceId());
        query.setTenantId(tenantId);

        IoTDevice result = deviceMapper.selectDeviceById(query);
        assertNotNull(result, "应能根据ID查询到设备");
        assertEquals(testDevice.getDeviceCode(), result.getDeviceCode());
        assertEquals(testDevice.getDeviceName(), result.getDeviceName());
        assertEquals("treadmill", result.getDeviceType());
    }

    @Test
    public void testSelectDeviceByCode()
    {
        deviceMapper.insertDevice(testDevice);

        IoTDevice query = new IoTDevice();
        query.setDeviceCode(testDevice.getDeviceCode());
        query.setTenantId(tenantId);

        IoTDevice result = deviceMapper.selectDeviceByCode(query);
        assertNotNull(result, "应能根据设备编号查询到设备");
        assertEquals(testDevice.getDeviceName(), result.getDeviceName());
    }

    @Test
    public void testSelectDeviceList()
    {
        deviceMapper.insertDevice(testDevice);

        IoTDevice query = new IoTDevice();
        query.setTenantId(tenantId);

        List<IoTDevice> list = deviceMapper.selectDeviceList(query);
        assertNotNull(list, "设备列表不应为null");
        assertTrue(list.size() > 0, "设备列表应至少有1条记录");
    }

    @Test
    public void testSelectDeviceListWithFilters()
    {
        deviceMapper.insertDevice(testDevice);

        IoTDevice query = new IoTDevice();
        query.setTenantId(tenantId);
        query.setDeviceType("treadmill");

        List<IoTDevice> list = deviceMapper.selectDeviceList(query);
        assertNotNull(list, "设备列表不应为null");
        assertTrue(list.stream().allMatch(d -> "treadmill".equals(d.getDeviceType())),
                "所有设备类型应为 treadmill");
    }

    @Test
    public void testUpdateDevice()
    {
        deviceMapper.insertDevice(testDevice);

        testDevice.setDeviceName("更新后的设备名称");
        testDevice.setStatus("offline");
        int result = deviceMapper.updateDevice(testDevice);
        assertTrue(result > 0, "设备更新应返回 > 0");

        IoTDevice query = new IoTDevice();
        query.setDeviceId(testDevice.getDeviceId());
        query.setTenantId(tenantId);

        IoTDevice updated = deviceMapper.selectDeviceById(query);
        assertEquals("更新后的设备名称", updated.getDeviceName());
        assertEquals("offline", updated.getStatus());
    }

    @Test
    public void testUpdateDeviceStatus()
    {
        deviceMapper.insertDevice(testDevice);

        int result = deviceMapper.updateDeviceStatus(testDevice.getDeviceId(), "maintenance");
        assertTrue(result > 0, "状态更新应返回 > 0");

        IoTDevice query = new IoTDevice();
        query.setDeviceId(testDevice.getDeviceId());
        query.setTenantId(tenantId);

        IoTDevice updated = deviceMapper.selectDeviceById(query);
        assertEquals("maintenance", updated.getStatus());
    }

    @Test
    public void testUpdateLastSeenAt()
    {
        deviceMapper.insertDevice(testDevice);

        int result = deviceMapper.updateLastSeenAt(testDevice.getDeviceId());
        assertTrue(result > 0, "最后在线时间更新应返回 > 0");
    }

    @Test
    public void testDeleteDeviceById()
    {
        deviceMapper.insertDevice(testDevice);

        IoTDevice deleteQuery = new IoTDevice();
        deleteQuery.setDeviceId(testDevice.getDeviceId());
        deleteQuery.setTenantId(tenantId);

        int result = deviceMapper.deleteDeviceById(deleteQuery);
        assertTrue(result > 0, "设备删除应返回 > 0");

        IoTDevice query = new IoTDevice();
        query.setDeviceId(testDevice.getDeviceId());
        query.setTenantId(tenantId);

        IoTDevice deleted = deviceMapper.selectDeviceById(query);
        assertNull(deleted, "删除后不应查询到设备");
    }

    @Test
    public void testDeleteDeviceByIds()
    {
        IoTDevice testDevice2 = new IoTDevice();
        testDevice2.setDeviceCode("TEST-" + UUID.randomUUID().toString().substring(0, 8));
        testDevice2.setDeviceName("测试设备2");
        testDevice2.setDeviceType("elliptical");
        testDevice2.setProtocol("ble");
        testDevice2.setManufacturerId(1L);
        testDevice2.setTenantId(tenantId);
        testDevice2.setStatus("online");
        testDevice2.setFirmwareVersion("1.0.0");
        testDevice2.setMetadata("{}");
        testDevice2.setDelFlag("0");
        testDevice2.setCreateBy("test");
        testDevice2.setCreateTime(new Date());

        deviceMapper.insertDevice(testDevice);
        deviceMapper.insertDevice(testDevice2);

        Long[] ids = { testDevice.getDeviceId(), testDevice2.getDeviceId() };
        int result = deviceMapper.deleteDeviceByIds(ids, tenantId);
        assertEquals(2, result, "批量删除应返回2");
    }

    @Test
    public void testCountByManufacturerId()
    {
        deviceMapper.insertDevice(testDevice);

        int count = deviceMapper.countByManufacturerId(1L);
        assertTrue(count > 0, "厂商设备数量应 > 0");
    }

    @Test
    public void testTenantIsolation()
    {
        deviceMapper.insertDevice(testDevice);

        IoTDevice query = new IoTDevice();
        query.setTenantId(999L);

        List<IoTDevice> list = deviceMapper.selectDeviceList(query);
        assertTrue(list.stream().noneMatch(d -> d.getDeviceId().equals(testDevice.getDeviceId())),
                "租户隔离：租户999不应查到租户1的设备");
    }
}

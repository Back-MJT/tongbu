package com.ruoyi.iot.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.iot.domain.model.ImuData;
import com.ruoyi.iot.service.IIoTDeviceService;
import com.ruoyi.iot.service.IImuDataService;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 消息监听器
 * 监听EMQX Broker上的设备数据/状态/心跳Topic
 *
 * Topic格式:
 *   device/{deviceCode}/data      - IMU数据上报
 *   device/{deviceCode}/status   - 设备状态变更
 *   device/{deviceCode}/heartbeat - 设备心跳
 *
 * @author ruoyi
 */
@Component
public class MqttListener
{
    private static final Logger log = LoggerFactory.getLogger(MqttListener.class);

    @Value("${mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id:ruoyi-iot-listener}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Value("${mqtt.topic.device-data:device/+/data}")
    private String deviceDataTopic;

    @Value("${mqtt.topic.device-status:device/+/status}")
    private String deviceStatusTopic;

    @Value("${mqtt.topic.device-heartbeat:device/+/heartbeat}")
    private String deviceHeartbeatTopic;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Value("${mqtt.connection-timeout:30}")
    private int connectionTimeout;

    @Value("${mqtt.keepalive-interval:60}")
    private int keepaliveInterval;

    @Value("${mqtt.auto-reconnect:true}")
    private boolean autoReconnect;

    @Autowired
    private IIoTDeviceService deviceService;

    @Autowired
    private IImuDataService imuDataService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MqttClient mqttClient;
    private volatile boolean connected = false;
    private CountDownLatch connectLatch = new CountDownLatch(1);

    // Redis key前缀
    private static final String REDIS_KEY_DEVICE_STATUS = "iot:device:status:";
    private static final String REDIS_KEY_IMU_LATEST = "iot:imu:latest:";
    private static final Duration REDIS_TTL = Duration.ofMinutes(5);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady()
    {
        // 延迟启动MQTT连接，等待EMQX就绪
        Thread thread = new Thread(() -> {
            try
            {
                Thread.sleep(5000); // 等待5秒让EMQX完全启动
                startMqttClient();
            }
            catch (Exception e)
            {
                log.warn("MQTT client start failed, will retry in background: {}", e.getMessage());
            }
        });
    }

    private void startMqttClient() throws Exception
    {
        try
        {
            log.info("Connecting to MQTT broker: {}", brokerUrl);
            mqttClient = new MqttClient(brokerUrl, clientId + "-" + System.currentTimeMillis(), new MemoryPersistence());

            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setServerURIs(new String[]{brokerUrl});
            if (StringUtils.isNotEmpty(username))
            {
                options.setUserName(username);
                options.setPassword(password.getBytes(StandardCharsets.UTF_8));
            }
            options.setConnectionTimeout(connectionTimeout);
            options.setKeepAliveInterval(keepaliveInterval);
            options.setAutomaticReconnect(autoReconnect);
            options.setCleanStart(true);

            mqttClient.setCallback(new MqttCallback()
            {
                @Override
                public void disconnected(MqttDisconnectResponse disconnectResponse)
                {
                    connected = false;
                    log.warn("MQTT disconnected: {}", disconnectResponse.getReasonString());
                }

                @Override
                public void mqttErrorOccurred(MqttException exception)
                {
                    log.error("MQTT error: {}", exception.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception
                {
                    handleMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttToken token)
                {
                    // 仅用于QoS 2
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI)
                {
                    connected = true;
                    connectLatch.countDown();
                    log.info("MQTT connected to {}, reconnect={}", serverURI, reconnect);
                    try
                    {
                        subscribeTopics();
                    }
                    catch (MqttException ex)
                    {
                        log.error("MQTT subscribe failed after connect: {}", ex.getMessage());
                    }
                }

                @Override
                public void authPacketArrived(int reasonCode, MqttProperties mqttProperties)
                {
                    // MQTT 5.0扩展
                }
            });

            mqttClient.connect(options);
            connectLatch.await(30, TimeUnit.SECONDS);
        }
        catch (MqttException e)
        {
            log.error("MQTT connection failed: {}", e.getMessage());
            throw e;
        }
    }

    private void subscribeTopics() throws MqttException
    {
        MqttSubscription[] subscriptions = new MqttSubscription[]{
            new MqttSubscription(deviceDataTopic, qos),
            new MqttSubscription(deviceStatusTopic, qos),
            new MqttSubscription(deviceHeartbeatTopic, qos),
        };
        mqttClient.subscribe(subscriptions);
        log.info("MQTT subscribed to topics: {}, {}, {}", deviceDataTopic, deviceStatusTopic, deviceHeartbeatTopic);
    }

    private void handleMessage(String topic, MqttMessage message)
    {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.debug("MQTT message arrived on topic {}: {}", topic, payload);

        try
        {
            if (topic.endsWith("/data"))
            {
                handleImuData(topic, payload);
            }
            else if (topic.endsWith("/status"))
            {
                handleDeviceStatus(topic, payload);
            }
            else if (topic.endsWith("/heartbeat"))
            {
                handleDeviceHeartbeat(topic, payload);
            }
        }
        catch (Exception e)
        {
            log.error("MQTT message handle failed for topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * 处理IMU数据
     * Topic: device/{deviceCode}/data
     * Payload: ImuData JSON
     */
    private void handleImuData(String topic, String payload) throws Exception
    {
        String deviceCode = extractDeviceCode(topic, "data");
        ImuData imuData = objectMapper.readValue(payload, ImuData.class);
        imuData.setDeviceCode(deviceCode);

        // 1. 缓存最新IMU数据到Redis (5分钟过期)
        if (redisTemplate != null)
        {
            String redisKey = REDIS_KEY_IMU_LATEST + deviceCode;
            redisTemplate.opsForValue().set(redisKey, payload, REDIS_TTL);
        }

        // 2. 更新设备最后在线时间
        updateDeviceOnline(deviceCode);

        // 3. 异步批量写入PostgreSQL时序表（IImuDataService内部缓冲，定期批量持久化）
        imuDataService.onImuDataReceived(imuData, null);

        log.debug("IMU data processed for device {}: {}", deviceCode, imuData);
    }

    /**
     * 处理设备状态变更
     * Topic: device/{deviceCode}/status
     * Payload: {"status": "online"|"offline"|"error"}
     */
    private void handleDeviceStatus(String topic, String payload) throws Exception
    {
        String deviceCode = extractDeviceCode(topic, "status");
        var node = objectMapper.readTree(payload);
        String status = node.get("status").asText();

        // 查询设备
        var device = deviceService.selectDeviceByCode(deviceCode);
        if (device != null)
        {
            deviceService.updateDeviceStatus(device.getDeviceId(), status);

            // 缓存状态到Redis
            if (redisTemplate != null)
            {
                redisTemplate.opsForValue().set(REDIS_KEY_DEVICE_STATUS + deviceCode, status, REDIS_TTL);
            }

            log.info("Device {} status changed to {}", deviceCode, status);
        }
    }

    /**
     * 处理设备心跳
     * Topic: device/{deviceCode}/heartbeat
     * Payload: {"battery": 85, "firmware": "1.2.3"}
     */
    private void handleDeviceHeartbeat(String topic, String payload) throws Exception
    {
        String deviceCode = extractDeviceCode(topic, "heartbeat");
        updateDeviceOnline(deviceCode);

        // 更新设备元数据 (电池电量等) - 异步
        log.debug("Device {} heartbeat received", deviceCode);
    }

    /**
     * 从Topic中提取设备编号
     * Topic格式: device/{deviceCode}/data|status|heartbeat
     */
    private String extractDeviceCode(String topic, String suffix)
    {
        String prefix = "/";
        int start = topic.indexOf("device/") + 7;
        int end = topic.lastIndexOf("/");
        if (end > start)
        {
            return topic.substring(start, end);
        }
        return topic.substring(start);
    }

    /**
     * 更新设备在线状态
     */
    @Transactional
    public void updateDeviceOnline(String deviceCode)
    {
        try
        {
            var device = deviceService.selectDeviceByCode(deviceCode);
            if (device != null)
            {
                deviceService.updateLastSeenAt(device.getDeviceId());
                if (!"online".equals(device.getStatus()))
                {
                    deviceService.updateDeviceStatus(device.getDeviceId(), "online");
                }
            }
        }
        catch (Exception e)
        {
            log.error("Update device online failed for {}: {}", deviceCode, e.getMessage());
        }
    }

    /**
     * 获取设备最新IMU数据 (从Redis缓存)
     */
    public ImuData getLatestImuData(String deviceCode)
    {
        if (redisTemplate != null)
        {
            String redisKey = REDIS_KEY_IMU_LATEST + deviceCode;
            String json = redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isNotEmpty(json))
            {
                try
                {
                    return objectMapper.readValue(json, ImuData.class);
                }
                catch (Exception e)
                {
                    log.error("Parse cached IMU data failed for {}: {}", deviceCode, e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 获取设备状态 (从Redis缓存)
     */
    public String getCachedDeviceStatus(String deviceCode)
    {
        if (redisTemplate != null)
        {
            return redisTemplate.opsForValue().get(REDIS_KEY_DEVICE_STATUS + deviceCode);
        }
        return null;
    }

    public boolean isConnected()
    {
        return connected && mqttClient != null && mqttClient.isConnected();
    }
}

package com.ruoyi.iot.mqtt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 连接配置
 *
 * EMQX Broker: xindong-mqtt:1883
 * 默认Topic格式:
 *   设备上报:  device/{deviceCode}/data
 *   设备状态:  device/{deviceCode}/status
 *   设备心跳:  device/{deviceCode}/heartbeat
 *
 * @author ruoyi
 */
@Configuration
public class MqttConfig
{
    /** MQTT Broker URL */
    @Value("${mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    /** MQTT 客户端ID */
    @Value("${mqtt.client-id:ruoyi-iot-client}")
    private String clientId;

    /** MQTT 用户名 */
    @Value("${mqtt.username:}")
    private String username;

    /** MQTT 密码 */
    @Value("${mqtt.password:}")
    private String password;

    /** 默认订阅Topic */
    @Value("${mqtt.topic.device-data:device/+/data}")
    private String deviceDataTopic;

    /** 默认订阅状态Topic */
    @Value("${mqtt.topic.device-status:device/+/status}")
    private String deviceStatusTopic;

    /** 默认订阅心跳Topic */
    @Value("${mqtt.topic.device-heartbeat:device/+/heartbeat}")
    private String deviceHeartbeatTopic;

    /** QoS级别 (0=最多一次, 1=至少一次, 2=恰好一次) */
    @Value("${mqtt.qos:1}")
    private int qos;

    /** 连接超时(秒) */
    @Value("${mqtt.connection-timeout:30}")
    private int connectionTimeout;

    /** 心跳间隔(秒) */
    @Value("${mqtt.keepalive-interval:60}")
    private int keepaliveInterval;

    /** 自动重连 */
    @Value("${mqtt.auto-reconnect:true}")
    private boolean autoReconnect;

    public String getBrokerUrl()
    {
        return brokerUrl;
    }

    public String getClientId()
    {
        return clientId;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getDeviceDataTopic()
    {
        return deviceDataTopic;
    }

    public String getDeviceStatusTopic()
    {
        return deviceStatusTopic;
    }

    public String getDeviceHeartbeatTopic()
    {
        return deviceHeartbeatTopic;
    }

    public int getQos()
    {
        return qos;
    }

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public int getKeepaliveInterval()
    {
        return keepaliveInterval;
    }

    public boolean isAutoReconnect()
    {
        return autoReconnect;
    }
}

package com.ruoyi.iot.service.impl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.iot.domain.model.EquipmentResolveResult;
import com.ruoyi.iot.mapper.MiniEquipmentMapper;
import com.ruoyi.iot.service.IMiniEquipmentService;

/**
 * 小程序器械解析服务实现
 */
@Service
public class MiniEquipmentServiceImpl implements IMiniEquipmentService
{
    private static final String DEFAULT_SERVICE_UUID = "0000FFE0-0000-1000-8000-00805F9B34FB";
    private static final String DEFAULT_NOTIFY_CHAR_UUID = "0000FFE4-0000-1000-8000-00805F9B34FB";

    @Autowired
    private MiniEquipmentMapper miniEquipmentMapper;

    @Override
    public EquipmentResolveResult resolveEquipment(String scanCode, Long tenantId)
    {
        String equipmentCode = normalizeEquipmentCode(scanCode);
        EquipmentResolveResult result = miniEquipmentMapper.selectEquipmentResolveResult(equipmentCode, tenantId);
        if (result == null)
        {
            return null;
        }
        if (result.getServiceUuid() == null || result.getServiceUuid().isEmpty())
        {
            result.setServiceUuid(DEFAULT_SERVICE_UUID);
        }
        if (result.getNotifyCharUuid() == null || result.getNotifyCharUuid().isEmpty())
        {
            result.setNotifyCharUuid(DEFAULT_NOTIFY_CHAR_UUID);
        }
        if (result.getBluetoothName() == null || result.getBluetoothName().isEmpty())
        {
            result.setBluetoothName(result.getDeviceCode());
        }
        return result;
    }

    private String normalizeEquipmentCode(String scanCode)
    {
        if (scanCode == null)
        {
            return null;
        }
        String value = scanCode.trim();
        try
        {
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException ignored)
        {
            // Keep the raw scan value when it is not valid URL encoding.
        }
        String lowerValue = value.toLowerCase();
        String[] keys = { "equipmentcode=", "equipment_code=", "code=" };
        for (String key : keys)
        {
            int idx = lowerValue.indexOf(key);
            if (idx >= 0)
            {
                value = value.substring(idx + key.length());
                int amp = value.indexOf('&');
                if (amp >= 0)
                {
                    value = value.substring(0, amp);
                }
                break;
            }
        }
        int hash = value.indexOf('#');
        if (hash >= 0)
        {
            value = value.substring(0, hash);
        }
        value = value.trim();
        return value;
    }
}

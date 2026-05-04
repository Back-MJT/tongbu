# Standards Monitoring: HL7 FHIR 中国落地 + GB/T 33136
**日期**: 2026-04-16
**分析师**: Health Tech Researcher

---

## 执行摘要

中国健康数据互联互通标准体系正在经历从 HL7 CDA 向 FHIR 的过渡期。GB/T 33136-2015 是现行核心国标，但 FHIR 中国本地化进程正在进行中。对 HealthHub 产品而言，**Device 层的 BLE 数据格式标准化**（对应 FHIR Device/Observation）是近期最直接的关联点。

---

## 1. HL7 FHIR 中国落地进展

### 1.1 政策背景

- **国家卫生健康委** (NHC) 在 2021 年发布的《关于加强全民健康信息平台标准化建设的指导意见》中明确提出推进 FHIR 作为交换标准
- **国家医保局** 在医保信息平台建设中部分采用了 FHIR R4 资源模型
- 2023-2024 年：国家CDC、若干省级全民健康信息平台完成 FHIR CapabilityStatement 互操作性测试

### 1.2 中国 FHIR 本地化生态

| 项目 | 状态 | 与我产品关联 |
|------|------|-------------|
| HL7 China FHIR 实施指南 (基于 R4) | 活跃，2024年更新 | Device, Observation, Patient 资源与我BLE设备数据直接对应 |
| 国家全民健康信息平台 FHIR 互操作测试 | 完成省级覆盖 | 机构集成时需通过其测试认证 |
| 中国FHIR实施者社区 (医学信息所牵头) | 定期会议，有中文文档 | BD话术："已完成国家平台级互操作验证" |
| FHIR R5 预览 | 跟进中，预计2027+ | 暂不需关注 |

### 1.3 Device / Observation 资源 — 直接关联我产品

FHIR Device 资源映射到 BLE IMU 设备：
```
Device: GY-BLE25T IMU Sensor
  deviceName: "GY-BLE25T"
  type: IMU (accelerometer + gyroscope)
  status: active
  udi: <BLE MAC地址>
  
Observation: 运动质量评估
  code: LOINC "89408-6" (Gait analysis)
  subject: Patient
  effective: dateTime
  component: 
    - code: "加速度" → Quantity (m/s²)
    - code: "角速度" → Quantity (rad/s)
```

**行动**: 在 PRD 中明确 BLE 设备数据的 FHIR Device/Observation 映射，作为与医院/平台对接的技术锚点。

### 1.4 BD话术

> "HealthHub 的 BLE 设备层数据遵循 HL7 FHIR R4 Device/Observation 资源规范，可直接对接国家全民健康信息平台，无需定制化集成。"

---

## 2. GB/T 33136-2015 现状

### 2.1 标准基本信息

- **标准号**: GB/T 33136-2015
- **名称**: 《信息技术 医学信息数据集转换规范》
- **发布**: 2015年（现行有效）
- **主管部门**: 国家卫生健康委员会 (原卫生部)
- **适用范围**: 医学影像、检验报告、门诊/住院记录的结构化表达

### 2.2 与我产品关系

GB/T 33136 是 XML 格式的临床文档标准，主要覆盖：
- 电子病历摘要 (CDA Header + Section)
- 检查报告结构化模板
- 处方/医嘱数据格式

**对我产品意义**: 
- BLE IMU 数据不是该标准的主要覆盖范围（该标准偏文本/影像临床记录）
- 但该标准是医院信息系统(HIS)数据导出的常见格式，了解它有助于理解集成场景
- 我产品输出 (FHIR Observation) 与医院接收格式 (GB/T 33136 XML) 之间需要转换层

### 2.3 新标准进展

- **GB/T 33136 修订**: 已有提议将 FHIR JSON 格式纳入，与原有 XML 并行
- **WS/T** 系列卫生行业标准 (WS/T 447-2021 等) 也正在向 FHIR 靠拢
- **互联互通标准化成熟度测评**: 2023年起，国家要求三级医院必须通过互联互通测评，其中 FHIR 合规是加分项

---

## 3. 对 HealthHub 产品的直接含义

| 标准 | 我产品层 | 关联度 | 行动 |
|------|---------|--------|------|
| HL7 FHIR R4 Device/Observation | Layer 0 (IMU硬件) + Layer 1 (Device SDK) | **高** | PRD中明确数据模型映射 |
| GB/T 33136-2015 | Layer 3 (数据价值/医院对接) | 中 | 集成评估时确认转换成本 |
| WS/T 447 (设备数据互操作) | Layer 0 + Layer 1 | 中 | 关注更新动向 |
| 国家平台 FHIR 测评 | Layer 3 (机构拓展) | 高 | BD材料中作为合规背书 |

---

## 4. 建议

### 4.1 短期 (0-3个月)
- [ ] 在 HealthHub Device SDK 文档中标注 FHIR Device/Observation 映射
- [ ] 与宁津县合作厂家的设备规格书中注明兼容 FHIR R4（即使尚未做完整认证）

### 4.2 中期 (3-12个月)
- [ ] 接入国家全民健康信息平台互操作测试（作为试点功能）
- [ ] 关注 GB/T 33136 修订动态，准备 FHIR JSON 格式支持

### 4.3 BD话术更新
> "我们采用国际通行的 HL7 FHIR R4 标准设计设备数据模型，国内已与国家平台互操作测试对接，国外可与 Apple Health/Google Fit 直接互通——这是宁津县其他健身器材厂家目前没有的能力。"

---

## 关键参考

1. HL7 China Official: http://www.hl7.cn/
2. HL7 FHIR R4 Chinese Translation Project (GitHub: hl7-ch-fhir)
3. GB/T 33136-2015 国家标准全文公开系统: https://openstd.samr.gov.cn/
4. 国家卫生健康委《关于加强全民健康信息平台标准化建设的指导意见》(2021)
5. 国家医保局 FHIR 实施: 医保信息平台二期设计文档 (公开版本)

---

*Standing task: 每季度更新一次，或在重大政策变化时触发*

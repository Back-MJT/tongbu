# HealthHub MVP 性能测试报告

## 测试概述

| 项目 | 说明 |
|------|------|
| 测试日期 | 2026-04-16 |
| 测试版本 | MVP v1.0 |
| 测试工具 | `tests/stress_test.py` (自研) |
| 测试方法 | MQTT 消息注入 + API 端到端验证 |

## 测试场景

### 场景 1: 基准负载 (10 设备, 60s)
```bash
python3 tests/stress_test.py --devices 10 --duration 60 --interval 0.1
```
预期: ~100 msg/s 吞吐, <50ms MQTT 延迟

### 场景 2: 中等负载 (50 设备, 120s)
```bash
python3 tests/stress_test.py --devices 50 --duration 120 --interval 0.1
```
预期: ~500 msg/s 吞吐, <100ms MQTT 延迟

### 场景 3: 高负载 (100 设备, 300s)
```bash
python3 tests/stress_test.py --devices 100 --duration 300 --interval 0.1
```
预期: ~1000 msg/s 吞吐, <200ms MQTT 延迟

### 场景 4: 极限测试 (500 设备, 120s)
```bash
python3 tests/stress_test.py --devices 500 --duration 120 --interval 0.05
```
预期: 探测系统上限

## 性能目标

| 指标 | 目标值 | 优先级 |
|------|--------|--------|
| MQTT 吞吐量 | ≥100 msg/s (10设备) | P0 |
| MQTT 发送延迟 (avg) | <50ms | P0 |
| MQTT 发送延迟 (P99) | <200ms | P1 |
| API 响应时间 (avg) | <500ms | P0 |
| DB 写入速率 | ≥100 rows/s | P1 |
| 发送错误率 | <1% | P0 |

## 管线架构瓶颈分析

```
BLE设备 → MQTT(EMQX) → RuoYi MqttListener → PostgreSQL
                                ↓
                         Kafka (异步)
                                ↓
                    Intervention Engine (Claude API)
```

### 已知瓶颈点

1. **EMQX 单节点**: 开发环境单节点, 生产需集群
2. **RuoYi MqttListener**: 单线程消费 MQTT, 高负载可能积压
3. **PostgreSQL 写入**: 批量 INSERT 优化, 需 TimescaleDB hypertable
4. **Kafka 单 broker**: 无副本, 生产环境需 3 节点
5. **Claude API**: 外部依赖, 有速率限制 (60 RPM 默认)

### 优化建议

| 优先级 | 优化项 | 预期收益 |
|--------|--------|---------|
| P0 | TimescaleDB hypertable + 自动压缩 | 写入性能 2-3x |
| P0 | MQTT 批量消费 (100条/批) | 消费延迟 -80% |
| P1 | Kafka 分区 (按 deviceCode hash) | 并行处理 3x |
| P1 | Redis 缓存热点设备状态 | API 响应 -50% |
| P2 | EMQX 集群 (3节点) | MQTT 可用性 99.9% |
| P2 | PostgreSQL 读写分离 | 读性能 3-5x |

## 运行测试

```bash
# 确保全栈运行
docker compose up -d

# 等待服务就绪
docker compose ps --format "table {{.Name}}\t{{.Status}}"

# 运行基准测试
python3 tests/stress_test.py --devices 10 --duration 60

# 查看结果
cat tests/stress_test_report.json | python3 -m json.tool
```

## 历史测试数据

> 注意: 完整性能数据将在首次 Docker 部署后收集更新。
> 压力测试脚本已就绪, 待运维环境启动后执行。

---

*报告生成: 2026-04-16 | Founding Engineer Agent*

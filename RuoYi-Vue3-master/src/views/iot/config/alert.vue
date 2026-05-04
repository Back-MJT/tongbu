<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="queryParams.ruleName" placeholder="请输入规则名称" clearable style="width:200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="告警类型" prop="alertType">
        <el-select v-model="queryParams.alertType" placeholder="请选择" clearable style="width:160px">
          <el-option v-for="d in alertTypes" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:120px">
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['iot:alert:add']">新增规则</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['iot:alert:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['iot:alert:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 规则列表 -->
    <el-table v-loading="loading" :data="ruleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="规则名称" align="left" key="ruleName" prop="ruleName" :show-overflow-tooltip="true" />
      <el-table-column label="告警类型" align="center" key="alertType" prop="alertType" width="140">
        <template #default="scope">
          <el-tag :type="alertTypeTag[scope.row.alertType] || 'info'">{{ alertTypeMap[scope.row.alertType] || scope.row.alertType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="触发条件" align="left" key="condition" prop="condition" :show-overflow-tooltip="true">
        <template #default="scope">
          <code style="font-size:12px">{{ scope.row.condition }}</code>
        </template>
      </el-table-column>
      <el-table-column label="告警级别" align="center" key="severity" prop="severity" width="100">
        <template #default="scope">
          <el-tag :type="severityTag[scope.row.severity]">{{ scope.row.severity }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="通知方式" align="center" key="notifyChannels" prop="notifyChannels" width="140">
        <template #default="scope">
          <span>
            <el-tag v-if="scope.row.notifyChannels?.includes('email')" type="primary" size="small">邮件</el-tag>
            <el-tag v-if="scope.row.notifyChannels?.includes('sms')" type="success" size="small">短信</el-tag>
            <el-tag v-if="scope.row.notifyChannels?.includes('webhook')" type="warning" size="small">Webhook</el-tag>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" key="status" prop="status" width="80">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleToggle(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['iot:alert:edit']">修改</el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['iot:alert:remove']">删除</el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form :model="form" :rules="rules" ref="ruleRef" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model="form.ruleName" placeholder="请输入规则名称" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="告警类型" prop="alertType">
              <el-select v-model="form.alertType" placeholder="请选择" style="width:100%">
                <el-option v-for="d in alertTypes" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="告警级别" prop="severity">
              <el-select v-model="form.severity" style="width:100%">
                <el-option label="信息" value="info" />
                <el-option label="警告" value="warning" />
                <el-option label="严重" value="critical" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="触发条件" prop="condition">
              <el-input v-model="form.condition" type="textarea" placeholder='例如: device.status == "offline" && duration > 300' :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="通知方式">
              <el-checkbox-group v-model="form.notifyChannelsList">
                <el-checkbox label="email">邮件</el-checkbox>
                <el-checkbox label="sms">短信</el-checkbox>
                <el-checkbox label="webhook">Webhook</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">启用</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" placeholder="请输入描述" :rows="2" maxlength="256" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AlertRule">
import { listAlertRule, getAlertRule, addAlertRule, updateAlertRule, delAlertRule, toggleAlertRule } from "@/api/iot/config"

const { proxy } = getCurrentInstance()

const alertTypes = [
  { value: 'device_offline', label: '设备离线' },
  { value: 'imu_anomaly', label: 'IMU数据异常' },
  { value: 'low_battery', label: '设备低电量' },
  { value: 'connection_lost', label: '连接中断' },
  { value: 'firmware_update', label: '固件更新' },
  { value: 'threshold_exceed', label: '阈值超限' }
]
const alertTypeMap = {}
alertTypes.forEach(d => { alertTypeMap[d.value] = d.label })
const alertTypeTag = { device_offline: 'danger', imu_anomaly: 'warning', low_battery: 'warning', connection_lost: 'danger', firmware_update: 'info', threshold_exceed: 'warning' }
const severityTag = { info: 'primary', warning: 'warning', critical: 'danger' }

const ruleList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    ruleName: undefined,
    alertType: undefined,
    status: undefined
  },
  rules: {
    ruleName: [{ required: true, message: "规则名称不能为空", trigger: "blur" }],
    alertType: [{ required: true, message: "请选择告警类型", trigger: "change" }],
    condition: [{ required: true, message: "触发条件不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listAlertRule(queryParams.value).then(res => {
    loading.value = false
    ruleList.value = res.rows || []
    total.value = res.total
  }).catch(() => {
    loading.value = false
    // 模拟数据
    ruleList.value = [
      { ruleId: 1, ruleName: '设备离线告警', alertType: 'device_offline', condition: 'device.status == "offline" && duration > 300', severity: 'critical', notifyChannels: ['email', 'sms'], status: '0' },
      { ruleId: 2, ruleName: 'IMU数据异常', alertType: 'imu_anomaly', condition: 'accelMagnitude > 50 || gyroMagnitude > 500', severity: 'warning', notifyChannels: ['webhook'], status: '0' },
      { ruleId: 3, ruleName: '低电量告警', alertType: 'low_battery', condition: 'battery < 20', severity: 'warning', notifyChannels: ['email'], status: '0' },
      { ruleId: 4, ruleName: '连接中断告警', alertType: 'connection_lost', condition: 'connectionLost == true && duration > 60', severity: 'critical', notifyChannels: ['email', 'sms', 'webhook'], status: '0' }
    ]
    total.value = ruleList.value.length
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.ruleId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = { ruleId: undefined, ruleName: undefined, alertType: undefined, condition: undefined, severity: 'warning', notifyChannelsList: ['email'], status: '0', description: undefined }
  proxy.resetForm("ruleRef")
}

function cancel() {
  open.value = false
  reset()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "添加告警规则"
}

function handleUpdate(row) {
  reset()
  const ruleId = row.ruleId || ids.value
  getAlertRule(ruleId).then(response => {
    const d = response.data || response
    d.notifyChannelsList = d.notifyChannels || []
    form.value = d
    open.value = true
    title.value = "修改告警规则"
  }).catch(() => {
    row.notifyChannelsList = row.notifyChannels || []
    form.value = { ...row }
    open.value = true
    title.value = "修改告警规则"
  })
}

function submitForm() {
  proxy.$refs["ruleRef"].validate(valid => {
    if (valid) {
      const submitData = { ...form.value, notifyChannels: form.value.notifyChannelsList }
      if (submitData.ruleId != undefined) {
        updateAlertRule(submitData).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAlertRule(submitData).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row) {
  const ruleIds = row.ruleId || ids.value
  proxy.$modal.confirm('是否确认删除选中的告警规则？').then(() => {
    return delAlertRule(ruleIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleToggle(row) {
  const text = row.status === '0' ? '启用' : '停用'
  toggleAlertRule(row.ruleId, row.status).then(() => {
    proxy.$modal.msgSuccess(text + "成功")
  }).catch(() => {
    row.status = row.status === '0' ? '1' : '0'
  })
}

onMounted(() => { getList() })
</script>

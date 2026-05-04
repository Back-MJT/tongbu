<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="Topic名称" prop="topicName">
        <el-input v-model="queryParams.topicName" placeholder="请输入Topic名称" clearable style="width:200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="设备类型" prop="deviceType">
        <el-select v-model="queryParams.deviceType" placeholder="请选择" clearable style="width:160px">
          <el-option v-for="d in deviceTypes" :key="d.value" :label="d.label" :value="d.value" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['iot:config:mqtt:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['iot:config:mqtt:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['iot:config:mqtt:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- Topic列表 -->
    <el-table v-loading="loading" :data="topicList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="Topic名称" align="left" key="topicName" prop="topicName" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-tag type="primary" style="font-family:monospace">{{ scope.row.topicName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Topic路径" align="left" key="topicPath" prop="topicPath" :show-overflow-tooltip="true">
        <template #default="scope">
          <span style="font-family:monospace;color:#409eff">{{ scope.row.topicPath }}</span>
        </template>
      </el-table-column>
      <el-table-column label="设备类型" align="center" key="deviceType" prop="deviceType" width="120">
        <template #default="scope">
          <el-tag>{{ deviceTypeMap[scope.row.deviceType] || scope.row.deviceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="QOS" align="center" key="qos" prop="qos" width="80">
        <template #default="scope">
          <el-tag type="info">QOS {{ scope.row.qos ?? 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效载荷" align="center" key="payloadType" prop="payloadType" width="120" />
      <el-table-column label="状态" align="center" key="status" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" key="description" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['iot:config:mqtt:edit']">修改</el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['iot:config:mqtt:remove']">删除</el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="topicRef" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="Topic名称" prop="topicName">
              <el-input v-model="form.topicName" placeholder="例如: device/imu/data" maxlength="128" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="Topic路径" prop="topicPath">
              <el-input v-model="form.topicPath" placeholder="支持变量: {deviceCode}, {manufacturerId}" maxlength="256" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="form.deviceType" placeholder="请选择" style="width:100%">
                <el-option v-for="d in deviceTypes" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="QOS级别" prop="qos">
              <el-select v-model="form.qos" style="width:100%">
                <el-option label="QOS 0 (最多一次)" :value="0" />
                <el-option label="QOS 1 (至少一次)" :value="1" />
                <el-option label="QOS 2 (恰好一次)" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="有效载荷类型" prop="payloadType">
              <el-input v-model="form.payloadType" placeholder="例如: json, binary" maxlength="32" />
            </el-form-item>
          </el-col>
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

<script setup name="MqttTopic">
import { listMqttTopic, getMqttTopic, addMqttTopic, updateMqttTopic, delMqttTopic } from "@/api/iot/config"

const { proxy } = getCurrentInstance()

const deviceTypes = [
  { value: 'treadmill', label: '跑步机' },
  { value: 'elliptical', label: '椭圆机' },
  { value: 'rowing', label: '划船机' },
  { value: 'strength', label: '力量器械' },
  { value: 'bike', label: '健身车' },
  { value: 'generic', label: '通用设备' }
]
const deviceTypeMap = {}
deviceTypes.forEach(d => { deviceTypeMap[d.value] = d.label })

const topicList = ref([])
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
    topicName: undefined,
    deviceType: undefined
  },
  rules: {
    topicName: [{ required: true, message: "Topic名称不能为空", trigger: "blur" }],
    topicPath: [{ required: true, message: "Topic路径不能为空", trigger: "blur" }],
    deviceType: [{ required: true, message: "请选择设备类型", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listMqttTopic(queryParams.value).then(res => {
    loading.value = false
    topicList.value = res.rows || []
    total.value = res.total
  }).catch(() => {
    loading.value = false
    // 模拟数据
    topicList.value = [
      { topicId: 1, topicName: '设备IMU数据', topicPath: 'devices/{deviceCode}/imu/data', deviceType: 'generic', qos: 1, payloadType: 'json', status: '0', description: '设备IMU传感器数据上报' },
      { topicId: 2, topicName: '设备状态', topicPath: 'devices/{deviceCode}/status', deviceType: 'generic', qos: 0, payloadType: 'json', status: '0', description: '设备在线/离线状态' },
      { topicId: 3, topicName: '固件升级', topicPath: 'devices/{deviceCode}/ota', deviceType: 'generic', qos: 1, payloadType: 'json', status: '0', description: '固件OTA升级指令' },
      { topicId: 4, topicName: '跑步机数据', topicPath: 'manufacturers/{manufacturerId}/treadmill/data', deviceType: 'treadmill', qos: 1, payloadType: 'json', status: '0', description: '跑步机训练数据' }
    ]
    total.value = topicList.value.length
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
  ids.value = selection.map(item => item.topicId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = { topicId: undefined, topicName: undefined, topicPath: undefined, deviceType: undefined, qos: 0, payloadType: 'json', status: '0', description: undefined }
  proxy.resetForm("topicRef")
}

function cancel() {
  open.value = false
  reset()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "添加MQTT Topic"
}

function handleUpdate(row) {
  reset()
  const topicId = row.topicId || ids.value
  getMqttTopic(topicId).then(response => {
    form.value = response.data || response
    open.value = true
    title.value = "修改MQTT Topic"
  }).catch(() => {
    form.value = { ...row }
    open.value = true
    title.value = "修改MQTT Topic"
  })
}

function submitForm() {
  proxy.$refs["topicRef"].validate(valid => {
    if (valid) {
      if (form.value.topicId != undefined) {
        updateMqttTopic(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addMqttTopic(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row) {
  const topicIds = row.topicId || ids.value
  proxy.$modal.confirm('是否确认删除选中的Topic配置？').then(() => {
    return delMqttTopic(topicIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

onMounted(() => { getList() })
</script>

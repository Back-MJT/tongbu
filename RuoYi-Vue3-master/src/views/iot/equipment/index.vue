<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="器械名称" prop="equipmentName">
        <el-input v-model="queryParams.equipmentName" placeholder="请输入器械名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="器械编号" prop="equipmentCode">
        <el-input v-model="queryParams.equipmentCode" placeholder="请输入器械编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="器械类型" prop="equipmentType">
        <el-input v-model="queryParams.equipmentType" placeholder="如 chest_press" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 200px">
          <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['iot:equipment:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['iot:equipment:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['iot:equipment:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['iot:equipment:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="equipmentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="器械ID" align="center" prop="equipmentId" width="90" />
      <el-table-column label="器械名称" align="center" prop="equipmentName" min-width="160" show-overflow-tooltip />
      <el-table-column label="器械编号" align="center" prop="equipmentCode" width="160" />
      <el-table-column label="器械类型" align="center" prop="equipmentType" width="140" />
      <el-table-column label="安装位置" align="center" prop="location" min-width="160" show-overflow-tooltip />
      <el-table-column label="绑定传感器" align="center" min-width="220">
        <template #default="scope">
          <div class="binding-cell">
            <template v-if="isSensorBound(scope.row)">
              <div class="binding-main">{{ scope.row.bluetoothName || scope.row.deviceCode }}</div>
              <div class="table-sub" v-if="scope.row.deviceCode">{{ scope.row.deviceCode }}</div>
              <el-tag type="success" size="small">已绑定</el-tag>
              <div class="binding-actions">
                <el-button link type="primary" size="small" @click="handleBindDevice(scope.row)" v-hasPermi="['iot:equipment:edit']">修改</el-button>
                <el-button link type="danger" size="small" @click="handleUnbindDevice(scope.row)" v-hasPermi="['iot:equipment:edit']">解绑</el-button>
              </div>
            </template>
            <template v-else>
              <el-tag type="warning" size="small">待绑定</el-tag>
              <div class="binding-actions">
                <el-button link type="primary" size="small" @click="handleBindDevice(scope.row)" v-hasPermi="['iot:equipment:edit']">绑定</el-button>
              </div>
            </template>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">
            {{ scope.row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button class="qr-action-btn" type="primary" plain icon="QrCode" @click="handleGenerateQrCode(scope.row)">二维码</el-button>
          <el-button link type="primary" icon="List" @click="handleFilterSessions(scope.row)">训练记录</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['iot:equipment:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['iot:equipment:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="640px" append-to-body>
      <el-form :model="form" :rules="rules" ref="equipmentRef" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="器械名称" prop="equipmentName">
              <el-input v-model="form.equipmentName" placeholder="请输入器械名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="器械编号" prop="equipmentCode">
              <el-input v-model="form.equipmentCode" placeholder="请输入器械编号" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="器械类型" prop="equipmentType">
              <el-input v-model="form.equipmentType" placeholder="如 chest_press" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="传感器编号">
              <el-input v-model="form.deviceCode" placeholder="如 HB-3412" clearable maxlength="64">
                <template #prepend>编号</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="蓝牙名称">
              <el-input v-model="form.bluetoothName" placeholder="请输入蓝牙名称（如 gy_ble25t1）" clearable maxlength="100">
                <template #prepend>广播名</template>
              </el-input>
              <div style="color: var(--el-text-color-secondary); font-size: 12px; margin-top: 4px;">
                保存后会自动绑定设备管理中同编号或同蓝牙名称的传感器
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="安装位置" prop="location">
              <el-input v-model="form.location" placeholder="请输入安装位置" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="二维码内容" prop="qrContent">
              <el-input v-model="form.qrContent" placeholder="如 xindong://equipment?code=EQ-000001" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="2" />
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

    <el-dialog title="绑定蓝牙传感器" v-model="bindOpen" width="560px" append-to-body>
      <el-form :model="bindForm" ref="bindRef" label-width="100px">
        <el-form-item label="器械">
          <div>{{ bindForm.equipmentName }}（{{ bindForm.equipmentCode }}）</div>
        </el-form-item>
        <el-form-item label="传感器" prop="deviceId" required>
          <el-select
            v-model="bindForm.deviceId"
            filterable
            clearable
            placeholder="请选择蓝牙传感器"
            style="width: 100%"
            @visible-change="visible => visible && loadSensorOptions()"
          >
            <el-option
              v-for="item in sensorOptions"
              :key="item.deviceId"
              :label="sensorLabel(item)"
              :value="item.deviceId"
            />
          </el-select>
          <div style="color: var(--el-text-color-secondary); font-size: 12px; margin-top: 4px;">
            一个传感器只能绑定一台器械；保存后会自动解除该传感器在其他器械上的旧绑定。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitBindDevice">确 定</el-button>
          <el-button @click="bindOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="IoTEquipment">
import { listEquipment, getEquipment, addEquipment, updateEquipment, delEquipment, delEquipmentBatch } from "@/api/iot/equipment"
import { listDevice } from "@/api/iot/device"
import { useRouter } from 'vue-router'

const router = useRouter()
const { proxy } = getCurrentInstance()
const { sys_normal_disable } = useDict("sys_normal_disable")

const equipmentList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const bindOpen = ref(false)
const bindForm = ref({})
const sensorOptions = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    equipmentName: undefined,
    equipmentCode: undefined,
    equipmentType: undefined,
    status: undefined
  },
  rules: {
    equipmentName: [{ required: true, message: "器械名称不能为空", trigger: "blur" }],
    equipmentCode: [{ required: true, message: "器械编号不能为空", trigger: "blur" }],
    equipmentType: [{ required: true, message: "器械类型不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listEquipment(queryParams.value).then(res => {
    equipmentList.value = res.rows
    total.value = res.total
    loading.value = false
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
  ids.value = selection.map(item => item.equipmentId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = {
    equipmentId: undefined,
    equipmentCode: undefined,
    equipmentName: undefined,
    equipmentType: undefined,
    location: undefined,
    qrContent: undefined,
    deviceCode: undefined,
    bluetoothName: undefined,
    status: "0",
    remark: undefined
  }
  proxy.resetForm("equipmentRef")
}

function cancel() {
  open.value = false
  reset()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "新增器械"
}

function handleUpdate(row) {
  reset()
  const equipmentId = row.equipmentId || ids.value[0]
  getEquipment(equipmentId).then(res => {
    form.value = res.data
    open.value = true
    title.value = "修改器械"
  })
}

function submitForm() {
  proxy.$refs["equipmentRef"].validate(valid => {
    if (!valid) return
    if (!form.value.qrContent && form.value.equipmentCode) {
      form.value.qrContent = `xindong://equipment?code=${form.value.equipmentCode}`
    }
    const request = form.value.equipmentId ? updateEquipment(form.value) : addEquipment(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(form.value.equipmentId ? "修改成功" : "新增成功")
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const equipmentIds = row.equipmentId ? [row.equipmentId] : ids.value
  proxy.$modal.confirm('是否确认删除器械编号为 "' + equipmentIds.join(",") + '" 的数据项？').then(() => {
    return equipmentIds.length === 1 ? delEquipment(equipmentIds[0]) : delEquipmentBatch(equipmentIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download("iot/equipment/export", { ...queryParams.value }, `equipment_${new Date().getTime()}.xlsx`)
}

function handleViewDevice(row) {
  if (!isSensorBound(row)) {
    proxy.$modal.msgWarning("当前器械待绑定蓝牙传感器")
    return
  }
  router.push({ path: '/iot/device', query: { deviceCode: row.deviceCode } })
}

function isSensorBound(row) {
  return !!(row?.bluetoothName || row?.deviceCode || row?.deviceName || row?.deviceId)
}

function handleBindDevice(row) {
  bindForm.value = {
    equipmentId: row.equipmentId,
    equipmentCode: row.equipmentCode,
    equipmentName: row.equipmentName,
    deviceId: row.deviceId || undefined
  }
  bindOpen.value = true
  loadSensorOptions()
}

function handleUnbindDevice(row) {
  proxy.$modal.confirm('是否确认解绑器械 "' + row.equipmentName + '" 的传感器？').then(() => {
    const data = {
      equipmentId: row.equipmentId,
      deviceCode: null,
      bluetoothName: null
    }
    updateEquipment(data).then(() => {
      proxy.$modal.msgSuccess("解绑成功")
      getList()
    })
  }).catch(() => {})
}

function loadSensorOptions() {
  listDevice({ pageNum: 1, pageSize: 200, protocol: 'ble' }).then(res => {
    sensorOptions.value = res.rows || []
  })
}

function sensorLabel(item) {
  const name = item.deviceName || '未命名传感器'
  const code = item.deviceCode || '-'
  const bluetooth = item.bluetoothName || '未配置蓝牙名'
  return `${name} / ${code} / ${bluetooth}`
}

function submitBindDevice() {
  if (!bindForm.value.deviceId) {
    proxy.$modal.msgWarning("请选择蓝牙传感器")
    return
  }
  updateEquipment({
    equipmentId: bindForm.value.equipmentId,
    deviceId: bindForm.value.deviceId
  }).then(() => {
    proxy.$modal.msgSuccess("绑定成功")
    bindOpen.value = false
    getList()
  })
}

function handleFilterSessions(row) {
  router.push({ path: '/iot/training-session', query: { equipmentCode: row.equipmentCode } })
}

function handleGenerateQrCode(row) {
  router.push({ path: '/iot/equipment/qrcode', query: { equipmentId: row.equipmentId } })
}

getList()
</script>

<style scoped>
.binding-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.binding-main {
  color: var(--el-text-color-primary);
}

.binding-actions {
  line-height: 1;
}

.table-sub {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.qr-action-btn {
  height: 28px;
  padding: 0 10px;
  margin-right: 6px;
  font-weight: 600;
}
</style>

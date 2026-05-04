<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="分组名称" prop="groupName">
        <el-input v-model="queryParams.groupName" placeholder="请输入分组名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="厂商" prop="manufacturerId">
        <el-select v-model="queryParams.manufacturerId" placeholder="请选择厂商" clearable style="width: 200px" @keyup.enter="handleQuery">
          <el-option v-for="m in manufacturerOptions" :key="m.manufacturerId" :label="m.manufacturerName" :value="m.manufacturerId" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['iot:group:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['iot:group:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['iot:group:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 分组列表 -->
    <el-table v-loading="loading" :data="groupList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="分组ID" align="center" key="groupId" prop="groupId" width="80" />
      <el-table-column label="分组名称" align="center" key="groupName" prop="groupName" />
      <el-table-column label="所属厂商" align="center" key="manufacturerName" prop="manufacturerName" />
      <el-table-column label="设备数量" align="center" key="deviceCount" prop="deviceCount" width="100" />
      <el-table-column label="描述" align="center" key="description" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" key="createTime" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="管理设备" placement="top">
            <el-button link type="primary" icon="Connection" @click="handleManageDevices(scope.row)" v-hasPermi="['iot:group:query']">设备</el-button>
          </el-tooltip>
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['iot:group:edit']">修改</el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['iot:group:remove']">删除</el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改分组对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form :model="form" :rules="rules" ref="groupRef" label-width="100px">
        <el-form-item label="分组名称" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入分组名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="所属厂商" prop="manufacturerId">
          <el-select v-model="form.manufacturerId" placeholder="请选择厂商" style="width: 100%">
            <el-option v-for="m in manufacturerOptions" :key="m.manufacturerId" :label="m.manufacturerName" :value="m.manufacturerId" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" :rows="2" maxlength="256" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 设备管理抽屉 -->
    <el-drawer v-model="deviceDrawer" :title="'分组设备管理 - ' + (currentGroup?.groupName || '')" size="70%" direction="rtl">
      <div style="padding:0 16px">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="openAddDeviceDialog" v-hasPermi="['iot:group:edit']">添加设备</el-button>
          </el-col>
        </el-row>
        <el-table v-loading="deviceLoading" :data="groupDevices" border>
          <el-table-column label="设备ID" align="center" key="deviceId" prop="deviceId" width="80" />
          <el-table-column label="设备名称" align="center" key="deviceName" prop="deviceName" />
          <el-table-column label="设备编号" align="center" key="deviceCode" prop="deviceCode" />
          <el-table-column label="设备类型" align="center" key="deviceType" prop="deviceType" width="120" />
          <el-table-column label="状态" align="center" key="status" prop="status" width="80">
            <template #default="scope">
              <el-tag :type="statusType[scope.row.status] || 'info'">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="100">
            <template #default="scope">
              <el-button link type="primary" icon="Close" @click="handleRemoveDevice(scope.row)" v-hasPermi="['iot:group:edit']">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- 添加设备对话框 -->
    <el-dialog v-model="addDeviceOpen" title="添加设备到分组" width="600px" append-to-body>
      <el-form :inline="true" :model="deviceQuery" style="margin-bottom:12px">
        <el-form-item label="设备名称">
          <el-input v-model="deviceQuery.deviceName" placeholder="设备名称" clearable style="width:180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="searchUngroupedDevices">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="deviceLoading" :data="ungroupedDevices" height="300" @selection-change="handleDeviceSelect">
        <el-table-column type="selection" width="40" />
        <el-table-column label="设备ID" align="center" key="deviceId" prop="deviceId" width="70" />
        <el-table-column label="设备名称" align="center" key="deviceName" prop="deviceName" />
        <el-table-column label="设备编号" align="center" key="deviceCode" prop="deviceCode" />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="addSelectedDevices">添加到分组</el-button>
        <el-button @click="addDeviceOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DeviceGroup">
import { listDeviceGroup, getDeviceGroup, addDeviceGroup, updateDeviceGroup, delDeviceGroup, getGroupDevices, addDeviceToGroup, removeDeviceFromGroup } from "@/api/iot/deviceGroup"
import { listDevice } from "@/api/iot/device"
import { listManufacturer } from "@/api/iot/manufacturer"

const { proxy } = getCurrentInstance()

const statusType = { online: 'success', offline: 'info', error: 'danger', maintenance: 'warning' }

const groupList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const manufacturerOptions = ref([])

// 分组设备管理
const deviceDrawer = ref(false)
const currentGroup = ref(null)
const groupDevices = ref([])
const deviceLoading = ref(false)

// 添加设备
const addDeviceOpen = ref(false)
const ungroupedDevices = ref([])
const selectedDeviceIds = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    groupName: undefined,
    manufacturerId: undefined
  },
  deviceQuery: { deviceName: undefined },
  rules: {
    groupName: [{ required: true, message: "分组名称不能为空", trigger: "blur" }],
    manufacturerId: [{ required: true, message: "请选择所属厂商", trigger: "change" }]
  }
})

const { queryParams, form, rules, deviceQuery } = toRefs(data)

function getList() {
  loading.value = true
  listDeviceGroup(queryParams.value).then(res => {
    loading.value = false
    groupList.value = res.rows
    total.value = res.total
  })
}

function getManufacturerList() {
  listManufacturer({ pageNum: 1, pageSize: 999 }).then(res => {
    manufacturerOptions.value = res.rows || []
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
  ids.value = selection.map(item => item.groupId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = { groupId: undefined, groupName: undefined, manufacturerId: undefined, description: undefined }
  proxy.resetForm("groupRef")
}

function cancel() {
  open.value = false
  reset()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "添加分组"
}

function handleUpdate(row) {
  reset()
  const groupId = row.groupId || ids.value
  getDeviceGroup(groupId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改分组"
  })
}

function submitForm() {
  proxy.$refs["groupRef"].validate(valid => {
    if (valid) {
      if (form.value.groupId != undefined) {
        updateDeviceGroup(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addDeviceGroup(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row) {
  const groupIds = row.groupId || ids.value
  proxy.$modal.confirm('是否确认删除分组编号为"' + groupIds + '"的数据项？').then(() => {
    return delDeviceGroup(groupIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

// 设备管理
function handleManageDevices(row) {
  currentGroup.value = row
  deviceDrawer.value = true
  loadGroupDevices(row.groupId)
}

function loadGroupDevices(groupId) {
  deviceLoading.value = true
  getGroupDevices(groupId).then(res => {
    deviceLoading.value = false
    groupDevices.value = res.data || []
  }).catch(() => { deviceLoading.value = false })
}

function openAddDeviceDialog() {
  addDeviceOpen.value = true
  selectedDeviceIds.value = []
  searchUngroupedDevices()
}

function searchUngroupedDevices() {
  deviceLoading.value = true
  listDevice({ ...deviceQuery.value, pageNum: 1, pageSize: 50 }).then(res => {
    deviceLoading.value = false
    ungroupedDevices.value = res.rows || []
  }).catch(() => { deviceLoading.value = false })
}

function handleDeviceSelect(selection) {
  selectedDeviceIds.value = selection.map(item => item.deviceId)
}

function addSelectedDevices() {
  if (!selectedDeviceIds.value.length) {
    proxy.$modal.msgWarning("请选择要添加的设备")
    return
  }
  const promises = selectedDeviceIds.value.map(deviceId =>
    addDeviceToGroup(currentGroup.value.groupId, deviceId)
  )
  Promise.all(promises).then(() => {
    proxy.$modal.msgSuccess("添加成功")
    addDeviceOpen.value = false
    loadGroupDevices(currentGroup.value.groupId)
  }).catch(() => {})
}

function handleRemoveDevice(row) {
  proxy.$modal.confirm('是否从分组中移除设备"' + row.deviceName + '"？').then(() => {
    return removeDeviceFromGroup(currentGroup.value.groupId, row.deviceId)
  }).then(() => {
    proxy.$modal.msgSuccess("移除成功")
    loadGroupDevices(currentGroup.value.groupId)
  }).catch(() => {})
}

onMounted(() => {
  getList()
  getManufacturerList()
})
</script>

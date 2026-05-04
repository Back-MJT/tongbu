<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="厂商名称" prop="manufacturerName">
        <el-input v-model="queryParams.manufacturerName" placeholder="请输入厂商名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="联系人" prop="contactPerson">
        <el-input v-model="queryParams.contactPerson" placeholder="请输入联系人" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input v-model="queryParams.contactPhone" placeholder="请输入联系电话" clearable style="width: 200px" @keyup.enter="handleQuery" />
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

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['iot:manufacturer:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['iot:manufacturer:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['iot:manufacturer:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['iot:manufacturer:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 厂商列表 -->
    <el-table v-loading="loading" :data="manufacturerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="厂商ID" align="center" key="manufacturerId" prop="manufacturerId" width="80" />
      <el-table-column label="厂商名称" align="center" key="manufacturerName" prop="manufacturerName" :show-overflow-tooltip="true" />
      <el-table-column label="联系人" align="center" key="contactPerson" prop="contactPerson" width="120" />
      <el-table-column label="联系电话" align="center" key="contactPhone" prop="contactPhone" width="140" />
      <el-table-column label="地址" align="center" key="address" prop="address" :show-overflow-tooltip="true" />
      <el-table-column label="营业执照" align="center" key="businessLicense" prop="businessLicense" :show-overflow-tooltip="true" width="160" />
      <el-table-column label="状态" align="center" key="status" prop="status" width="100">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" key="createTime" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="查看详情" placement="top">
            <el-button link type="primary" icon="View" @click="handleViewDetail(scope.row)" v-hasPermi="['iot:manufacturer:list']">详情</el-button>
          </el-tooltip>
          <el-tooltip content="查看设备" placement="top">
            <el-button link type="primary" icon="Monitor" @click="handleViewDevices(scope.row)" v-hasPermi="['iot:device:list']">设备</el-button>
          </el-tooltip>
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['iot:manufacturer:edit']">修改</el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['iot:manufacturer:remove']">删除</el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改厂商对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="manufacturerRef" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="厂商名称" prop="manufacturerName">
              <el-input v-model="form.manufacturerName" placeholder="请输入厂商名称" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" maxlength="32" />
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
          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入地址" maxlength="256" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="营业执照" prop="businessLicense">
              <el-input v-model="form.businessLicense" placeholder="请输入统一社会信用代码" maxlength="32" />
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
  </div>
</template>

<script setup name="IoTManufacturer">
import { listManufacturer, getManufacturer, addManufacturer, updateManufacturer, delManufacturer } from "@/api/iot/manufacturer"
import useUserStore from '@/store/modules/user'

const router = useRouter()
const { proxy } = getCurrentInstance()
const { sys_normal_disable } = useDict("sys_normal_disable")

const manufacturerList = ref([])
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
    manufacturerName: undefined,
    contactPerson: undefined,
    contactPhone: undefined,
    status: undefined
  },
  rules: {
    manufacturerName: [{ required: true, message: "厂商名称不能为空", trigger: "blur" }],
    contactPhone: [{ pattern: /^[\d\-()]+$/, message: "请输入正确的电话号码", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询厂商列表 */
function getList() {
  loading.value = true
  listManufacturer(queryParams.value).then(res => {
    loading.value = false
    manufacturerList.value = res.rows
    total.value = res.total
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 选择条数 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.manufacturerId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 重置操作表单 */
function reset() {
  form.value = {
    manufacturerId: undefined,
    manufacturerName: undefined,
    contactPerson: undefined,
    contactPhone: undefined,
    address: undefined,
    businessLicense: undefined,
    status: "0",
    remark: undefined
  }
  proxy.resetForm("manufacturerRef")
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加厂商"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const manufacturerId = row.manufacturerId || ids.value
  getManufacturer(manufacturerId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改厂商"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["manufacturerRef"].validate(valid => {
    if (valid) {
      if (form.value.manufacturerId != undefined) {
        updateManufacturer(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addManufacturer(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const manufacturerIds = row.manufacturerId || ids.value
  proxy.$modal.confirm('是否确认删除厂商编号为"' + manufacturerIds + '"的数据项？').then(() => {
    return delManufacturer(manufacturerIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("iot/manufacturer/export", { ...queryParams.value }, `manufacturer_${new Date().getTime()}.xlsx`)
}

/** 状态修改 */
function handleStatusChange(row) {
  const text = row.status === "0" ? "启用" : "停用"
  proxy.$modal.confirm('确认要"' + text + '"该厂商吗？').then(() => {
    return updateManufacturer(row)
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功")
  }).catch(() => {
    row.status = row.status === "0" ? "1" : "0"
  })
}

/** 查看设备 */
function handleViewDevices(row) {
  router.push({ path: '/iot/device', query: { manufacturerId: row.manufacturerId } })
}

/** 查看详情 */
function handleViewDetail(row) {
  router.push({ path: '/iot/manufacturer/' + row.manufacturerId })
}

onMounted(() => {
  getList()
})
</script>

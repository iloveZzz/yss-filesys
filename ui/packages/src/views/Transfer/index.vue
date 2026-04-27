<script setup lang="ts">
import {
  DeleteOutlined,
  PauseCircleOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
} from "@ant-design/icons-vue";
import { YButton, YCard } from "@yss-ui/components";
import { formatBytes } from "@/utils/format";
import { useTransferPage } from "./hooks/useTransferPage";

const {
  filter,
  keyword,
  loadTasks,
  visibleTasks,
  visibleLoading,
  visibleTotalCount,
  statusTextMap,
  statusColorMap,
  normalizeStatus,
  isFinished,
  getPercent,
  getSpeed,
  getRemainingText,
  isDoneFilter,
  tabs,
  cancelTask,
  pauseTask,
  clearAllTasks,
  getCompletedTime,
  onSearch,
  handleFilterChange,
  handleCompletedTableChange,
  completedPageIndex,
  completedPageSize,
  completedTotal,
} = useTransferPage();
</script>

<template>
  <div class="transfer-page">
    <div class="page-header transfer-page-header">
      <div class="transfer-header-copy">
        <h2>传输列表</h2>
        <p>任务统一管理，支持按状态筛选、搜索和批量清空。</p>
      </div>
      <div class="page-actions">
        <a-input
          v-model:value="keyword"
          class="search-input"
          allow-clear
          placeholder="搜索"
          @pressEnter="onSearch"
        >
          <template #prefix>
            <SearchOutlined />
          </template>
        </a-input>
        <YButton class="refresh-btn" @click="loadTasks">
          <template #icon>
            <ReloadOutlined />
          </template>
        </YButton>
        <YButton class="danger-btn" theme="danger" @click="clearAllTasks">
          <template #icon>
            <DeleteOutlined />
          </template>
          全部清空
        </YButton>
      </div>
    </div>

    <YCard class="transfer-card" :bordered="false" :padding="0">
      <div class="transfer-summary">
        <span>共 {{ visibleTotalCount }} 项</span>
        <span class="transfer-summary-tip">· 任务统一管理</span>
      </div>
      <div class="transfer-toolbar">
        <div class="tab-group">
          <button
            v-for="item in tabs"
            :key="item.key"
            class="tab-pill"
            :class="{ active: filter === item.key }"
            @click="handleFilterChange(item.key)"
          >
            {{ item.label }}
          </button>
        </div>
        <div class="toolbar-meta">共 {{ visibleTotalCount }} 项</div>
      </div>

      <div class="transfer-table-shell">
        <a-table
          :data-source="visibleTasks"
          :loading="visibleLoading"
          :pagination="false"
          :table-layout="'fixed'"
          row-key="taskId"
          size="small"
        >
          <a-table-column title="文件名称" data-index="fileName" :width="520" />
          <a-table-column title="文件大小" width="170">
            <template #default="{ record }">
              <span>{{ formatBytes(record.fileSize) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="状态" width="120">
            <template #default="{ record }">
              <span
                class="status-badge"
                :style="{
                  backgroundColor: `${statusColorMap[normalizeStatus(record)] || '#1677ff'}18`,
                  color: statusColorMap[normalizeStatus(record)] || '#1677ff'
                }"
              >
                {{ statusTextMap[normalizeStatus(record)] || record.status || "未知" }}
              </span>
            </template>
          </a-table-column>
          <a-table-column title="进度" width="320">
            <template #default="{ record }">
              <div class="progress-cell">
                <a-progress :percent="getPercent(record)" :show-info="false" stroke-color="#1677ff" />
                <div class="progress-meta">
                  <span class="progress-speed" :class="{ done: isFinished(record) }">
                    {{ getSpeed(record) }}
                  </span>
                  <span v-if="!isFinished(record)" class="progress-remaining">
                    {{ getRemainingText(record) }}
                  </span>
                </div>
              </div>
            </template>
          </a-table-column>
          <a-table-column v-if="!isDoneFilter" title="操作" width="180" align="right">
            <template #default="{ record }">
              <div class="action-group">
                <a-button class="op-btn" type="text" @click="pauseTask(record.taskId)">
                  <template #icon>
                    <PauseCircleOutlined />
                  </template>
                  暂停
                </a-button>
                <a-button class="op-btn danger" type="text" @click="cancelTask(record.taskId)">
                  <template #icon>
                    <StopOutlined />
                  </template>
                  取消
                </a-button>
              </div>
            </template>
          </a-table-column>
          <a-table-column v-else title="完成时间" width="220" align="right">
            <template #default="{ record }">
              <span class="finish-time">{{ getCompletedTime(record) }}</span>
            </template>
          </a-table-column>
        </a-table>
      </div>

      <div v-if="isDoneFilter" class="transfer-pagination">
        <a-pagination
          :current="completedPageIndex + 1"
          :page-size="completedPageSize"
          :total="completedTotal"
          show-size-changer
          @change="(page, pageSize) => handleCompletedTableChange({ current: page, pageSize })"
          @showSizeChange="(current, pageSize) => handleCompletedTableChange({ current, pageSize })"
        />
      </div>
    </YCard>
  </div>
</template>

<style scoped lang="less">
@import "./index.less";
</style>

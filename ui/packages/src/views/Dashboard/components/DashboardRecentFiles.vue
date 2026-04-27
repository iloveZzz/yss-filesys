<script setup lang="ts">
import { computed } from "vue";
import { YCard, YButton } from "@yss-ui/components";
import type { FileTransferTaskDTO } from "@/api/generated/filesys/schemas";

const props = defineProps<{
  loading: boolean;
  recentFiles: FileTransferTaskDTO[];
  total: number;
  pageIndex: number;
  pageSize: number;
  formatBytes: (value?: number) => string;
  formatDateTime: (value?: string) => string;
}>();

const emit = defineEmits<{
  manageFiles: [];
  pageChange: [page: number, pageSize: number];
}>();

const pagination = computed(() => ({
  current: props.pageIndex + 1,
  pageSize: props.pageSize,
  total: props.total,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 项`,
}));

const getTaskTypeLabel = (taskType?: string) => {
  if ((taskType || "").toLowerCase() === "download") return "下载";
  return "上传";
};

const getCompletedTime = (record: FileTransferTaskDTO) =>
  record.completeTime || record.startTime || "-";
</script>

<template>
  <section class="dashboard-section">
    <YCard class="table-card" :bordered="false" :padding="18">
      <template #title>
        <div class="section-heading-inline">
          <div>
            <h2>最近完成的文件</h2>
            <p>展示最近完成的文件投递记录，支持分页浏览更多完成项。</p>
          </div>
          <YButton type="primary" size="small" @click="$emit('manageFiles')"
            >文件管理</YButton
          >
        </div>
      </template>

      <a-table
        class="recent-files-table"
        :data-source="recentFiles"
        :pagination="false"
        :loading="loading"
        row-key="taskId"
        :table-layout="'fixed'"
        size="small"
      >
        <a-table-column title="文件名称" data-index="fileName" :width="360" ellipsis>
          <template #default="{ text }">
            <span class="recent-files-name-cell">{{ text || "-" }}</span>
          </template>
        </a-table-column>
        <a-table-column title="类型" width="120">
          <template #default="{ record }">
            <a-tag color="blue">{{ getTaskTypeLabel(record.taskType) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="文件大小" width="140">
          <template #default="{ record }">{{
            formatBytes(record.fileSize)
          }}</template>
        </a-table-column>
        <a-table-column title="完成时间" width="180">
          <template #default="{ record }">{{
            formatDateTime(getCompletedTime(record))
          }}</template>
        </a-table-column>
      </a-table>

      <div class="recent-files-pagination">
        <a-pagination
          :current="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          show-size-changer
          :show-total="pagination.showTotal"
          @change="(page, pageSize) => emit('pageChange', page, pageSize)"
          @showSizeChange="
            (current, pageSize) => emit('pageChange', current, pageSize)
          "
        />
      </div>
    </YCard>
  </section>
</template>

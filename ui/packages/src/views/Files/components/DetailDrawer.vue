<script setup lang="ts">
import type { FileRecordDTO } from "@/api/generated/filesys/schemas";

defineProps<{
  open: boolean;
  loading: boolean;
  detail: FileRecordDTO | null;
  formatBytes: (value?: number) => string;
  formatDateTime: (value?: string) => string;
}>();

defineEmits<{
  close: [];
}>();
</script>

<template>
  <a-drawer :open="open" title="文件详情" width="480" @close="$emit('close')">
    <a-spin :spinning="loading">
      <a-descriptions bordered :column="1" size="small">
        <a-descriptions-item label="名称">{{ detail?.displayName || detail?.originalName }}</a-descriptions-item>
        <a-descriptions-item label="文件ID">{{ detail?.fileId }}</a-descriptions-item>
        <a-descriptions-item label="目录">{{ detail?.isDir ? "是" : "否" }}</a-descriptions-item>
        <a-descriptions-item label="大小">{{ formatBytes(detail?.size) }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ formatDateTime(detail?.updateTime) }}</a-descriptions-item>
        <a-descriptions-item label="上传时间">{{ formatDateTime(detail?.uploadTime) }}</a-descriptions-item>
      </a-descriptions>
    </a-spin>
  </a-drawer>
</template>

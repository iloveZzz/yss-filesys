<script setup lang="ts">
import { formatBytes } from "@/utils/format";
import { usePreviewPage } from "./hooks/usePreviewPage";

const { loading, previewData, fileId } = usePreviewPage();
</script>

<template>
  <div class="preview-page">
    <a-card class="workspace-panel preview-card" :bordered="false">
      <a-space direction="vertical" style="width: 100%">
        <div class="preview-header">
          <div class="page-kicker">文件预览</div>
          <h2>文件预览</h2>
          <p>{{ fileId }}</p>
        </div>
        <a-spin :spinning="loading">
          <a-descriptions bordered :column="1" size="small">
            <a-descriptions-item label="文件ID">{{ previewData.fileId || fileId }}</a-descriptions-item>
            <a-descriptions-item label="文件名">{{ previewData.fileName || "-" }}</a-descriptions-item>
            <a-descriptions-item label="类型">{{ previewData.previewType || "-" }}</a-descriptions-item>
            <a-descriptions-item label="大小">{{ formatBytes((previewData.fileSize as number) || undefined) }}</a-descriptions-item>
            <a-descriptions-item label="流式地址">{{ previewData.streamUrl || "-" }}</a-descriptions-item>
          </a-descriptions>
        </a-spin>
      </a-space>
    </a-card>
  </div>
</template>

<style scoped lang="less">
@import "./index.less";
</style>

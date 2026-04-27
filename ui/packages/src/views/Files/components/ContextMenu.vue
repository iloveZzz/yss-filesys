<script setup lang="ts">
import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  FileOutlined,
  HeartOutlined,
  ShareAltOutlined,
  SwapOutlined,
} from "@ant-design/icons-vue";
import type { FileRecordDTO } from "@/api/generated/filesys/schemas";

defineProps<{
  open: boolean;
  x: number;
  y: number;
  target: FileRecordDTO | null;
}>();

defineEmits<{
  close: [];
  preview: [record: FileRecordDTO];
  share: [];
  favorite: [];
  download: [record: FileRecordDTO];
  rename: [];
  move: [record: FileRecordDTO];
  detail: [record: FileRecordDTO];
  recycle: [];
}>();
</script>

<template>
  <div
    v-if="open"
    class="file-context-menu"
    :style="{ left: `${x}px`, top: `${y}px` }"
    @click.stop
  >
    <button
      class="menu-item"
      :disabled="!!target?.isDir"
      @click="$emit('preview', target as FileRecordDTO); $emit('close')"
    >
      <EyeOutlined />
      <span>预览</span>
    </button>
    <button class="menu-item" @click="$emit('share'); $emit('close')">
      <ShareAltOutlined />
      <span>分享</span>
    </button>
    <button class="menu-item" @click="$emit('favorite'); $emit('close')">
      <HeartOutlined />
      <span>收藏</span>
    </button>
    <button
      class="menu-item"
      :disabled="!!target?.isDir"
      @click="$emit('download', target as FileRecordDTO); $emit('close')"
    >
      <DownloadOutlined />
      <span>下载</span>
    </button>
    <div class="menu-divider"></div>
    <button class="menu-item" @click="$emit('rename'); $emit('close')">
      <EditOutlined />
      <span>重命名</span>
    </button>
    <button class="menu-item" @click="$emit('move', target as FileRecordDTO); $emit('close')">
      <SwapOutlined />
      <span>移动到</span>
    </button>
    <button class="menu-item" @click="$emit('detail', target as FileRecordDTO); $emit('close')">
      <FileOutlined />
      <span>详细信息</span>
    </button>
    <div class="menu-divider"></div>
    <button class="menu-item danger" :disabled="!target?.fileId" @click="$emit('recycle'); $emit('close')">
      <DeleteOutlined />
      <span>放入回收站</span>
    </button>
  </div>
</template>

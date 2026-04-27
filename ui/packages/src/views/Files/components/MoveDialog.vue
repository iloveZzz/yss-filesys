<script setup lang="ts">
import { CaretRightOutlined, FolderAddOutlined, FolderOutlined, HomeOutlined } from "@ant-design/icons-vue";
import type { FileRecordDTO } from "@/api/generated/filesys/schemas";

defineProps<{
  open: boolean;
  loading: boolean;
  folders: FileRecordDTO[];
  breadcrumb: FileRecordDTO[];
  currentParentId?: string;
  selectedFolderId?: string;
  loadingFolders: boolean;
  getFileLabel: (record: FileRecordDTO) => string;
}>();

defineEmits<{
  cancel: [];
  navigateRoot: [];
  navigateBreadcrumb: [index: number];
  selectFolder: [folderId?: string];
  enterFolder: [folder: FileRecordDTO];
  createFolder: [];
  confirm: [];
}>();
</script>

<template>
  <a-modal
    :open="open"
    class="move-modal"
    title="移动到"
    centered
    :confirm-loading="loading"
    :footer="null"
    :z-index="2000"
    width="620px"
    @cancel="$emit('cancel')"
  >
    <div class="move-modal-body">
      <div class="move-breadcrumb">
        <span class="move-root" @click="$emit('navigateRoot')">
          <HomeOutlined />
          全部文件
        </span>
        <span v-for="(item, index) in breadcrumb" :key="item.fileId">
          /
          <span
            class="move-breadcrumb-item"
            :class="{ active: index === breadcrumb.length - 1 }"
            @click="index < breadcrumb.length - 1 && $emit('navigateBreadcrumb', index)"
          >
            <FolderOutlined />
            {{ getFileLabel(item) }}
          </span>
        </span>
      </div>

      <div class="move-list">
        <div
          class="move-item move-item-root"
          :class="{ active: selectedFolderId === undefined && !currentParentId }"
          @click="$emit('selectFolder', undefined)"
        >
          <HomeOutlined class="move-item-icon" />
          <span>全部文件</span>
        </div>

        <div v-if="loadingFolders" class="move-loading">加载中...</div>
        <template v-else>
          <div
            v-for="folder in folders"
            :key="folder.fileId"
            class="move-item"
            :class="{ active: selectedFolderId === folder.fileId }"
            @click="$emit('selectFolder', folder.fileId)"
            @dblclick.stop="$emit('enterFolder', folder)"
          >
            <FolderOutlined class="move-item-icon" />
            <span>{{ getFileLabel(folder) }}</span>
            <CaretRightOutlined class="move-item-arrow" />
          </div>
          <div v-if="!folders.length" class="move-empty">
            当前目录下没有子文件夹
          </div>
        </template>
      </div>

      <a-alert class="move-warning" type="info" show-icon message="提示：单击选择目标文件夹，双击进入该文件夹" />

      <div class="move-footer-bar">
        <a-button type="text" class="create-folder-btn" @click="$emit('createFolder')">
          <FolderAddOutlined />
          新建文件夹
        </a-button>

        <div class="move-footer-actions">
          <a-button @click="$emit('cancel')">取消</a-button>
          <a-button type="primary" :loading="loading" @click="$emit('confirm')">移动到此处</a-button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

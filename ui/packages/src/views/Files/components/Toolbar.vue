<script setup lang="ts">
import { DeleteOutlined, FolderAddOutlined, ReloadOutlined, UploadOutlined } from "@ant-design/icons-vue";
import { YButton } from "@yss-ui/components";

defineProps<{
  mode: "files" | "recycle";
}>();

defineEmits<{
  reload: [];
  upload: [];
  createFolder: [];
  clearRecycle: [];
}>();
</script>

<template>
  <div class="page-header file-page-header">
    <div class="file-header-copy">
      <h2>{{ mode === "files" ? "全部文件" : "回收站" }}</h2>
      <p v-if="mode === 'files'">目录浏览、重命名、预览、下载、分享和批量操作。</p>
      <p v-else>回收站文件支持恢复和清空。</p>
    </div>
    <div class="page-actions">
      <YButton class="icon-button" @click="$emit('reload')">
        <ReloadOutlined />
      </YButton>
      <template v-if="mode === 'files'">
        <YButton type="primary" @click="$emit('upload')">
          <UploadOutlined />
          上传文件
        </YButton>
        <YButton class="primary-ghost" @click="$emit('createFolder')">
          <FolderAddOutlined />
          新建文件夹
        </YButton>
      </template>
      <template v-else>
        <YButton theme="danger" @click="$emit('clearRecycle')">
          <DeleteOutlined />
          清空回收站
        </YButton>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckOutlined, FileOutlined, UploadOutlined } from "@ant-design/icons-vue";

interface UploadQueueItem {
  id: string;
  name: string;
  size: number;
  status: "pending" | "uploading" | "success" | "error";
  progress: number;
  message?: string;
}

defineProps<{
  open: boolean;
  running: boolean;
  queue: UploadQueueItem[];
  summary: string;
  overwriteExisting: boolean;
  formatBytes: (value?: number) => string;
  getUploadStatusLabel: (status: UploadQueueItem["status"]) => string;
}>();

defineEmits<{
  "update:open": [value: boolean];
  "update:overwriteExisting": [value: boolean];
  cancel: [];
  clearQueue: [];
  openPicker: [];
  drop: [event: DragEvent];
  confirm: [];
}>();
</script>

<template>
  <a-modal
    :open="open"
    class="upload-modal"
    title="上传文件"
    centered
    :mask-closable="!running"
    :keyboard="!running"
    :footer="null"
    width="980px"
    @cancel="$emit('cancel')"
    @update:open="$emit('update:open', $event)"
  >
    <div class="upload-modal-body">
      <div class="upload-list">
        <div class="upload-list-header">
          <span>上传列表</span>
          <span class="upload-list-hint">
            <template v-if="running">正在执行上传任务队列</template>
            <template v-else>支持同时上传多个文件，单次最多 10 个</template>
          </span>
          <a-button v-if="!running" type="text" size="small" @click="$emit('clearQueue')">清空</a-button>
        </div>

        <div class="upload-overwrite-row">
          <div class="upload-overwrite-label">
            <strong>覆盖同名文件</strong>
            <span>默认开启，上传到相同文件夹时会覆盖同名文件</span>
          </div>
          <a-switch
            :checked="overwriteExisting"
            :disabled="running"
            checked-children="开"
            un-checked-children="关"
            @update:checked="(checked) => $emit('update:overwriteExisting', checked === true)"
          />
        </div>

        <div
          v-if="!running"
          class="upload-dropzone"
          @click="$emit('openPicker')"
          @dragover.prevent
          @drop.prevent="$emit('drop', $event)"
        >
          <UploadOutlined class="upload-drop-icon" />
          <strong>点击或拖拽文件到此处上传</strong>
          <span>支持同时上传多个文件，单次最多 10 个</span>
        </div>

        <div v-if="queue.length" class="upload-list-items">
          <div v-for="item in queue" :key="item.id" class="upload-list-item">
            <div class="upload-item-icon" :class="`state-${item.status}`">
              <FileOutlined v-if="item.status !== 'success'" />
              <CheckOutlined v-else />
            </div>
            <div class="upload-item-meta">
              <strong>{{ item.name }}</strong>
              <span>{{ formatBytes(item.size) }}</span>
              <div class="upload-item-status" :class="`state-${item.status}`">
                <span>{{ getUploadStatusLabel(item.status) }}</span>
                <span v-if="item.message && item.status === 'error'">· {{ item.message }}</span>
              </div>
              <a-progress
                v-if="item.status !== 'pending'"
                class="upload-item-progress"
                :percent="item.progress"
                :show-info="false"
              />
            </div>
          </div>
        </div>

        <div v-else class="upload-empty">
          请选择文件后，将自动加入上传列表
        </div>
      </div>

      <div v-if="summary" class="upload-summary">
        {{ summary }}
      </div>

      <div class="upload-footer">
        <a-button :disabled="running" @click="$emit('cancel')">取消</a-button>
        <a-button type="primary" :loading="running" :disabled="!queue.length" @click="$emit('confirm')">
          {{ running ? "上传中" : "添加到上传列表" }}
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<style scoped lang="less">
.upload-overwrite-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  margin-bottom: 16px;
  border: 1px solid rgba(24, 144, 255, 0.14);
  border-radius: 12px;
  background: rgba(24, 144, 255, 0.04);
}

.upload-overwrite-label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  strong {
    font-size: 14px;
    color: var(--workspace-text-color, #1f2329);
  }

  span {
    font-size: 12px;
    color: var(--workspace-text-secondary, rgba(31, 35, 41, 0.58));
  }
}
</style>

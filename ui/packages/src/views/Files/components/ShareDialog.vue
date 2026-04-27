<script setup lang="ts">
import type { FileShareDTO } from "@/api/generated/filesys/schemas";

interface SharePreviewFile {
  fileId: string;
  fileName: string;
  fileSize?: string;
  fileKind?: "folder" | "image" | "pdf" | "text" | "archive" | "sheet" | "file";
}

interface ShareFormState {
  expireType: number;
  expireTime: string;
  shareType: string;
  permissions: string[];
  maxViewCountMode: string;
  maxDownloadCountMode: string;
  maxViewCount: number;
  maxDownloadCount: number;
  shareName: string;
}

defineProps<{
  open: boolean;
  stage: "form" | "result";
  loading: boolean;
  shareFiles: SharePreviewFile[];
  shareSelectedFileCount: number;
  shareSelectedFilePreview: SharePreviewFile[];
  sharePreviewSingle: boolean;
  shareCreated: FileShareDTO | null;
  shareUrl: string;
  shareExpireText: string;
  shareForm: ShareFormState;
  getSharePreviewInitial: (file: SharePreviewFile) => string;
  getSharePreviewTone: (file: SharePreviewFile) => string;
}>();

defineEmits<{
  cancel: [];
  createShareLink: [];
  copyShareLink: [];
}>();
</script>

<template>
  <a-modal
    :open="open"
    class="share-modal"
    :title="stage === 'result' ? '分享文件' : '分享文件'"
    centered
    :confirm-loading="loading"
    :footer="null"
    width="980px"
    @cancel="$emit('cancel')"
  >
    <div v-if="stage === 'form'" class="share-modal-body">
      <div class="share-preview-card" :class="{ single: sharePreviewSingle }">
        <template v-if="sharePreviewSingle">
          <div class="share-preview-single">
            <div
              class="share-preview-icon large"
              :class="`share-tone-${getSharePreviewTone(shareSelectedFilePreview[0] || shareFiles[0])}`"
            >
              <span>{{ getSharePreviewInitial(shareSelectedFilePreview[0] || shareFiles[0]) }}</span>
            </div>
            <div class="share-preview-name large-name">
              {{ shareSelectedFilePreview[0]?.fileName || shareFiles[0]?.fileName || "已选择文件" }}
            </div>
          </div>
        </template>
        <template v-else>
          <div class="share-preview-inner">
            <div
              v-for="(item, index) in shareSelectedFilePreview"
              :key="`${item.fileId}-${index}`"
              class="share-preview-item"
            >
              <div class="share-preview-icon" :class="`share-tone-${getSharePreviewTone(item)}`">
                <span>{{ getSharePreviewInitial(item) }}</span>
              </div>
              <div class="share-preview-name">{{ item.fileName }}</div>
            </div>
          </div>
        </template>
        <div class="share-preview-count">共 {{ shareSelectedFileCount }} 个文件</div>
      </div>

      <a-divider />

      <div class="setting-block">
        <div class="setting-label">有效期</div>
        <a-radio-group v-model:value="shareForm.expireType">
          <a-radio :value="1">7天</a-radio>
          <a-radio :value="2">30天</a-radio>
          <a-radio :value="3">自定义</a-radio>
          <a-radio :value="4">永久有效</a-radio>
        </a-radio-group>
        <a-input
          v-if="shareForm.expireType === 3"
          v-model:value="shareForm.expireTime"
          class="setting-input"
          placeholder="请输入过期时间"
        />
      </div>

      <div class="setting-block">
        <div class="setting-label">分享类型</div>
        <a-radio-group v-model:value="shareForm.shareType">
          <a-radio value="PUBLIC">公开分享</a-radio>
          <a-radio value="PRIVATE">私密分享</a-radio>
        </a-radio-group>
      </div>

      <div class="setting-block">
        <div class="setting-label">分享权限</div>
        <a-checkbox-group v-model:value="shareForm.permissions">
          <a-checkbox value="preview">预览</a-checkbox>
          <a-checkbox value="download">下载</a-checkbox>
        </a-checkbox-group>
      </div>

      <div class="setting-block">
        <div class="setting-label">最大查看次数</div>
        <a-radio-group v-model:value="shareForm.maxViewCountMode">
          <a-radio value="unlimited">不限制</a-radio>
          <a-radio value="custom">自定义</a-radio>
        </a-radio-group>
        <a-input-number
          v-if="shareForm.maxViewCountMode === 'custom'"
          v-model:value="shareForm.maxViewCount"
          :min="1"
          class="setting-number"
          placeholder="请输入次数"
        />
      </div>

      <div class="setting-block">
        <div class="setting-label">最大下载次数</div>
        <a-radio-group v-model:value="shareForm.maxDownloadCountMode">
          <a-radio value="unlimited">不限制</a-radio>
          <a-radio value="custom">自定义</a-radio>
        </a-radio-group>
        <a-input-number
          v-if="shareForm.maxDownloadCountMode === 'custom'"
          v-model:value="shareForm.maxDownloadCount"
          :min="1"
          class="setting-number"
          placeholder="请输入次数"
        />
      </div>

      <div class="create-footer">
        <a-button @click="$emit('cancel')">取消</a-button>
        <a-button type="primary" :loading="loading" @click="$emit('createShareLink')">生成分享链接</a-button>
      </div>
    </div>

    <div v-else class="share-result-panel">
      <div class="share-file-card">
        <div class="share-file-icon">
          <span>分享</span>
        </div>
        <div class="share-file-meta">
          <strong>{{ shareCreated?.shareName || "文件分享" }}</strong>
          <span>{{ shareCreated?.fileIds?.[0] || "已选择文件" }}</span>
        </div>
      </div>

      <a-divider />

      <a-alert class="share-success" type="success" show-icon message="分享链接已生成" />

      <div class="share-link-card">
        <div class="share-link-text">{{ shareUrl || "-" }}</div>
        <div class="share-code-text">提取码：{{ shareCreated?.shareCode || "无需" }}</div>
      </div>

      <div class="share-expire">
        分享链接将于 {{ shareExpireText }} 后失效
      </div>

      <div class="create-footer result-footer">
        <a-button @click="$emit('cancel')">取消</a-button>
        <a-button type="primary" @click="$emit('copyShareLink')">复制链接</a-button>
      </div>
    </div>
  </a-modal>
</template>

<style scoped lang="less">
.dialog-kicker {
  color: #1677ff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}
</style>

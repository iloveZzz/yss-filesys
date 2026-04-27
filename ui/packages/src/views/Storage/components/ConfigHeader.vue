<script setup lang="ts">
type ModalMode = "create" | "edit" | "view";

type PlatformOption = {
  label: string;
  value: string;
  description: string;
};

defineProps<{
  mode: ModalMode;
  platformIdentifier: string;
  platformOptions: PlatformOption[];
  selectedPlatform?: PlatformOption;
  currentStatus: string;
}>();

defineEmits<{
  updatePlatform: [value: string];
}>();
</script>

<template>
  <div class="config-header">
    <div class="modal-header-copy">
      <div class="config-kicker">平台信息</div>
      <p>配置您的对象存储平台，支持 MinIO、阿里云 OSS、腾讯云 COS 等</p>
    </div>

    <a-form-item label="选择存储平台" required>
      <a-select
        :value="platformIdentifier"
        placeholder="请选择存储平台"
        :options="platformOptions.map((item) => ({ label: item.label, value: item.value }))"
        :disabled="mode === 'view'"
        size="small"
        @update:value="$emit('updatePlatform', String($event || ''))"
      />
    </a-form-item>

    <div class="platform-note">
      <div class="platform-note-label">
        <strong>当前平台</strong>
        <span>{{ selectedPlatform?.description || "请选择平台以查看说明" }}</span>
      </div>
      <a-tag v-if="selectedPlatform?.label" color="blue">
        {{ selectedPlatform.label }}
      </a-tag>
    </div>

    <div class="status-note">
      <span>当前状态</span>
      <a-tag :color="mode === 'view' ? 'default' : currentStatus.includes('启用') ? 'green' : 'default'">
        {{ currentStatus }}
      </a-tag>
    </div>
  </div>
</template>

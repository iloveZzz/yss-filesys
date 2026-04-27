<script setup lang="ts">
import type { StorageSettingDTO } from "@/api/generated/filesys/schemas";

defineProps<{
  open: boolean;
  loading: boolean;
  storageSettings: StorageSettingDTO[];
  folderName: string;
  storageSettingId: string;
}>();

defineEmits<{
  "update:folderName": [value: string];
  "update:storageSettingId": [value: string];
  ok: [];
  cancel: [];
}>();
</script>

<template>
  <a-modal
    :open="open"
    class="workspace-modal folder-dialog"
    title="新建文件夹"
    centered
    :z-index="2100"
    :confirm-loading="loading"
    :ok-button-props="{ disabled: loading }"
    ok-text="创建"
    cancel-text="取消"
    @ok="$emit('ok')"
    @cancel="$emit('cancel')"
  >
    <div class="dialog-body">
      <div class="dialog-hero">
        <div class="dialog-kicker">CREATE FOLDER</div>
        <strong>为当前目录新增一个文件夹</strong>
        <span>支持选择存储配置，创建后会保留在当前父目录下。</span>
      </div>

      <a-form layout="vertical" class="dialog-form">
        <a-form-item label="文件夹名称" required>
          <a-input
            :value="folderName"
            allow-clear
            placeholder="请输入文件夹名称"
            :disabled="loading"
            @update:value="$emit('update:folderName', $event)"
          />
        </a-form-item>

        <a-form-item label="存储配置" required>
          <a-select
            :value="storageSettingId"
            placeholder="请选择存储配置"
            :disabled="loading"
            :options="
              storageSettings.map((item) => ({
                label: item.remark || item.platformIdentifier || item.id || '未命名配置',
                value: item.id || '',
              }))
            "
            @update:value="(value) => $emit('update:storageSettingId', String(value ?? ''))"
          />
        </a-form-item>
      </a-form>
    </div>
  </a-modal>
</template>

<style scoped lang="less">
.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-hero {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border: 1px solid rgba(24, 144, 255, 0.14);
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(24, 144, 255, 0.08), rgba(24, 144, 255, 0.02)),
    rgba(255, 255, 255, 0.88);
  box-shadow: 0 12px 30px rgba(31, 35, 41, 0.06);

  strong {
    font-size: 16px;
    font-weight: 800;
    color: var(--workspace-text-color, #1f2329);
  }

  span {
    font-size: 12px;
    line-height: 1.7;
    color: var(--workspace-text-secondary, rgba(31, 35, 41, 0.58));
  }
}

.dialog-form {
  :deep(.ant-form-item) {
    margin-bottom: 16px;
  }

  :deep(.ant-form-item-label > label) {
    color: var(--workspace-text-color, #1f2329);
    font-weight: 700;
  }
}

.dialog-kicker {
  color: #1677ff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}
</style>

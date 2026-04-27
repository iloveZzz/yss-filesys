<script setup lang="ts">
import type { StorageCardItem } from "../hooks/useStoragePage";

defineProps<{
  open: boolean;
  loading: boolean;
  target: StorageCardItem | null;
}>();

defineEmits<{
  cancel: [];
  confirm: [];
}>();
</script>

<template>
  <a-modal
    :open="open"
    title="确认删除存储配置"
    ok-text="删除"
    cancel-text="取消"
    :confirm-loading="loading"
    centered
    destroy-on-close
    @ok="$emit('confirm')"
    @cancel="$emit('cancel')"
  >
    <p class="delete-confirm-text">
      删除后将移除该存储平台配置，相关文件不会被立即删除，但后续操作将无法继续使用该配置。
    </p>
    <a-alert
      v-if="target?.setting?.enabled"
      type="warning"
      show-icon
      message="当前配置已启用，不能删除"
    />
    <div v-else class="delete-confirm-meta">
      <div>
        <span>平台</span>
        <strong>{{ target?.displayName || "未命名平台" }}</strong>
      </div>
      <div>
        <span>备注</span>
        <strong>{{ target?.setting?.remark || "无" }}</strong>
      </div>
    </div>
  </a-modal>
</template>

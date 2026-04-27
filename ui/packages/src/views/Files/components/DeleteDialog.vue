<script setup lang="ts">
import type { FileRecordDTO } from "@/api/generated/filesys/schemas";

defineProps<{
  open: boolean;
  loading: boolean;
  target: FileRecordDTO | null;
  selectedCount: number;
  getFileLabel: (record: FileRecordDTO) => string;
}>();

defineEmits<{
  cancel: [];
  confirm: [];
}>();
</script>

<template>
  <a-modal
    :open="open"
    class="workspace-modal"
    title="确认彻底删除"
    ok-text="删除"
    cancel-text="取消"
    ok-type="danger"
    :confirm-loading="loading"
    @ok="$emit('confirm')"
    @cancel="$emit('cancel')"
  >
    <div class="dialog-copy danger-copy">
      <p class="delete-dialog-text">
        {{
          target
            ? `确定要彻底删除「${getFileLabel(target)}」吗？删除后将无法恢复。`
            : `确定要彻底删除选中的 ${selectedCount} 个文件吗？删除后将无法恢复。`
        }}
      </p>
    </div>
  </a-modal>
</template>

<style scoped lang="less">
.delete-dialog-text {
  margin: 0;
  color: #0f172a;
  line-height: 1.8;
}

.dialog-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dialog-kicker {
  color: #1677ff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.danger-copy .dialog-kicker {
  color: #ef4444;
}
</style>

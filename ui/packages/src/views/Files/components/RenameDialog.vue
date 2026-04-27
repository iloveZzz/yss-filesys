<script setup lang="ts">
defineProps<{
  open: boolean;
  fileName: string;
}>();

defineEmits<{
  "update:fileName": [value: string];
  ok: [];
  cancel: [];
}>();
</script>

<template>
  <a-modal :open="open" class="workspace-modal" title="重命名文件" @ok="$emit('ok')" @cancel="$emit('cancel')">
    <a-input :value="fileName" placeholder="新文件名" @update:value="$emit('update:fileName', $event)" />
  </a-modal>
</template>

<style scoped lang="less">
.dialog-kicker {
  margin-bottom: 12px;
  color: #1677ff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}
</style>

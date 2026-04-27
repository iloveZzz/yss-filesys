<script setup lang="ts">
import { YButton } from "@yss-ui/components";

type ModalMode = "create" | "edit" | "view";

defineProps<{
  mode: ModalMode;
  saveLoading: boolean;
}>();

defineEmits<{
  cancel: [];
  save: [];
}>();
</script>

<template>
  <div class="modal-footer" :class="{ 'modal-footer-view': mode === 'view' }">
    <a-button size="small" @click="$emit('cancel')">取消</a-button>
    <YButton v-if="mode !== 'view'" type="primary" :loading="saveLoading" @click="$emit('save')">
      保存配置
    </YButton>
  </div>
</template>

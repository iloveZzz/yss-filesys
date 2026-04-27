<script setup lang="ts">
import { FilterOutlined, SyncOutlined } from "@ant-design/icons-vue";
import { YButton } from "@yss-ui/components";

defineProps<{
  searchValue: string;
}>();

defineEmits<{
  "update:searchValue": [value: string];
  reload: [];
  create: [];
}>();
</script>

<template>
  <div class="page-header storage-header">
    <div class="storage-header-copy">
      <h2>存储管理</h2>
      <p>统一管理各存储平台的启停、查看、编辑和删除。</p>
    </div>
    <div class="toolbar-actions">
      <a-input
        :value="searchValue"
        class="storage-search"
        allow-clear
        placeholder="搜索存储配置..."
        @update:value="$emit('update:searchValue', $event)"
      />
      <a-button class="icon-btn" type="default">
        <template #icon>
          <FilterOutlined />
        </template>
      </a-button>
      <a-button class="icon-btn" type="default" @click="$emit('reload')">
        <template #icon>
          <SyncOutlined />
        </template>
      </a-button>
      <YButton type="primary" @click="$emit('create')">
        <template #icon>
          <span class="plus-mark">+</span>
        </template>
        添加存储
      </YButton>
    </div>
  </div>
</template>

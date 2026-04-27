<script setup lang="ts">
import { DatabaseOutlined, DeleteOutlined, EyeOutlined, SettingOutlined } from "@ant-design/icons-vue";
import { YButton, YCard } from "@yss-ui/components";
import type { StorageCardItem } from "../hooks/useStoragePage";

defineProps<{
  items: StorageCardItem[];
  getStatusTagColor: (enabled?: number) => string;
  getStatusLabel: (enabled?: number) => string;
  getToggleLabel: (enabled?: number) => string;
  getToggleTheme: (enabled?: number) => "primary" | "danger";
  isProtectedStorage: (item: StorageCardItem) => boolean;
  formatDateTime: (value?: string) => string;
}>();

defineEmits<{
  toggle: [item: StorageCardItem];
  view: [item: StorageCardItem];
  edit: [item: StorageCardItem];
  delete: [item: StorageCardItem];
}>();
</script>

<template>
  <div class="storage-grid">
    <YCard
      v-for="item in items"
      :key="item.setting?.id || item.setting?.platformIdentifier || item.displayName"
      class="storage-card"
      :class="{ 'is-enabled': item.setting?.enabled, 'is-disabled': !item.setting?.enabled }"
      :bordered="false"
      :padding="16"
    >
      <div class="storage-top">
        <div class="storage-icon">
          <DatabaseOutlined />
        </div>
        <div class="storage-top-right">
          <a-tag v-if="item.setting?.enabled" color="blue" class="active-badge">
            当前启用
          </a-tag>
          <a-tag :color="getStatusTagColor(item.setting?.enabled)" class="status-tag">
            {{ getStatusLabel(item.setting?.enabled) }}
          </a-tag>
        </div>
      </div>

      <div class="storage-title">
        <h3>{{ item.displayName || "未命名平台" }}</h3>
      </div>

      <p class="storage-desc">
        {{ item.description || item.setting?.remark || "存储平台配置" }}
      </p>

      <div class="storage-meta">
        <div>
          <span>更新时间</span>
          <strong>{{ formatDateTime(item.setting?.updatedAt || item.setting?.createdAt) }}</strong>
        </div>
        <div>
          <span>Bucket</span>
          <strong>{{ item.config?.bucket || "未配置" }}</strong>
        </div>
        <div class="storage-meta-wide">
          <span>Endpoint</span>
          <strong>{{ item.config?.endpoint || "未配置" }}</strong>
        </div>
      </div>

      <div class="storage-actions">
        <YButton
          :disabled="isProtectedStorage(item)"
          :theme="getToggleTheme(item.setting?.enabled)"
          :class="{ 'action-primary': true }"
          @click="$emit('toggle', item)"
        >
          {{ getToggleLabel(item.setting?.enabled) }}
        </YButton>
        <YButton class="action-btn" :disabled="isProtectedStorage(item)" @click="$emit('view', item)">
          <template #icon>
            <EyeOutlined />
          </template>
          查看
        </YButton>
        <YButton
          class="action-btn"
          :disabled="!!item.setting?.enabled || isProtectedStorage(item)"
          @click="$emit('edit', item)"
        >
          <template #icon>
            <SettingOutlined />
          </template>
          编辑
        </YButton>
        <YButton
          class="action-danger"
          :disabled="!!item.setting?.enabled || isProtectedStorage(item)"
          @click="$emit('delete', item)"
        >
          <template #icon>
            <DeleteOutlined />
          </template>
          删除
        </YButton>
      </div>
    </YCard>
  </div>
</template>

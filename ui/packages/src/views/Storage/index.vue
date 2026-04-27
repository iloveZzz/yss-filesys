<script setup lang="ts">
import Grid from "./components/Grid.vue";
import DeleteDialog from "./components/DeleteDialog.vue";
import ConfigDialog from "./components/ConfigDialog.vue";
import Toolbar from "./components/Toolbar.vue";
import { useStoragePage } from "./hooks/useStoragePage";

const {
  loading,
  detailLoading,
  saveLoading,
  modalOpen,
  modalMode,
  deleteModalOpen,
  deleteLoading,
  deleteTarget,
  searchValue,
  formModel,
  schemaVersion,
  configFormRef,
  isCompactMode,
  fieldErrors,
  form,
  platformOptions,
  filteredStorageItems,
  detailColumns,
  renderFields,
  loadStorage,
  openCreate,
  openEdit,
  openView,
  saveSetting,
  toggleStatus,
  openDelete,
  confirmDelete,
  closeDeleteModal,
  selectedPlatform,
  currentStatus,
  getStatusTagColor,
  getStatusLabel,
  getToggleLabel,
  getToggleTheme,
  isProtectedStorage,
  getValueByPath,
  updateFieldValue,
  getNamePath,
  formatDateTime,
} = useStoragePage();
</script>

<template>
  <div class="workspace-section storage-page" :class="{ 'is-compact': isCompactMode }">
    <Toolbar
      :search-value="searchValue"
      @update:search-value="searchValue = $event"
      @reload="loadStorage"
      @create="openCreate"
    />

    <div v-if="loading" class="storage-empty">
      <a-spin />
    </div>
    <Grid
      v-else
      :items="filteredStorageItems"
      :get-status-tag-color="getStatusTagColor"
      :get-status-label="getStatusLabel"
      :get-toggle-label="getToggleLabel"
      :get-toggle-theme="getToggleTheme"
      :is-protected-storage="isProtectedStorage"
      :format-date-time="formatDateTime"
      @toggle="toggleStatus"
      @view="openView"
      @edit="openEdit"
      @delete="openDelete"
    />

    <ConfigDialog
      ref="configFormRef"
      :open="modalOpen"
      :mode="modalMode"
      :save-loading="saveLoading"
      :detail-loading="detailLoading"
      :is-compact-mode="isCompactMode"
      :schema-version="schemaVersion"
      :form="form"
      :form-model="formModel"
      :platform-options="platformOptions"
      :selected-platform="selectedPlatform"
      :current-status="currentStatus"
      :detail-columns="detailColumns"
      :render-fields="renderFields"
      :field-errors="fieldErrors"
      :get-value-by-path="getValueByPath"
      :update-field-value="updateFieldValue"
      :get-name-path="getNamePath"
      @cancel="modalOpen = false"
      @save="saveSetting"
      @update-platform="form.platformIdentifier = $event"
    />

    <DeleteDialog
      :open="deleteModalOpen"
      :loading="deleteLoading"
      :target="deleteTarget"
      @cancel="closeDeleteModal"
      @confirm="confirmDelete"
    />
  </div>
</template>

<style lang="less">
@import "./index.less";
</style>

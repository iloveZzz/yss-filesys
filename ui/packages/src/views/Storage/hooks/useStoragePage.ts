import { onMounted } from "vue";
import { useStorageConfig } from "./useStorageConfig";
import { useStorageList } from "./useStorageList";

export type { StorageCardItem } from "./types";

export const useStoragePage = () => {
  const list = useStorageList();
  const config = useStorageConfig({
    platformOptions: list.platformOptions,
    loadStorage: list.loadStorage,
    isProtectedStorage: list.isProtectedStorage,
  });

  onMounted(async () => {
    await list.loadStorage();
  });

  return {
    loading: list.loading,
    searchValue: list.searchValue,
    storagePlatforms: list.storagePlatforms,
    templates: list.templates,
    settings: list.settings,
    deleteModalOpen: list.deleteModalOpen,
    deleteLoading: list.deleteLoading,
    deleteTarget: list.deleteTarget,
    platformMap: list.platformMap,
    platformOptions: list.platformOptions,
    storageItems: list.storageItems,
    filteredStorageItems: list.filteredStorageItems,
    loadStorage: list.loadStorage,
    getStatusTagColor: list.getStatusTagColor,
    getStatusLabel: list.getStatusLabel,
    getToggleLabel: list.getToggleLabel,
    getToggleTheme: list.getToggleTheme,
    isProtectedStorage: list.isProtectedStorage,
    toggleStatus: list.toggleStatus,
    openDelete: list.openDelete,
    confirmDelete: list.confirmDelete,
    closeDeleteModal: list.closeDeleteModal,
    saveLoading: config.saveLoading,
    modalOpen: config.modalOpen,
    modalMode: config.modalMode,
    detailLoading: config.detailLoading,
    formModel: config.formModel,
    pendingFormData: config.pendingFormData,
    activeDetail: config.activeDetail,
    schemaVersion: config.schemaVersion,
    configFormRef: config.configFormRef,
    isCompactMode: config.isCompactMode,
    fieldErrors: config.fieldErrors,
    form: config.form,
    activeSchema: config.activeSchema,
    detailColumns: config.detailColumns,
    renderFields: config.renderFields,
    clearFieldErrors: config.clearFieldErrors,
    isEmptyFieldValue: config.isEmptyFieldValue,
    validateDynamicFields: config.validateDynamicFields,
    clearActiveDetail: config.clearActiveDetail,
    loadActiveDetail: config.loadActiveDetail,
    parseConfigData: config.parseConfigData,
    resetForm: config.resetForm,
    fillForm: config.fillForm,
    openCreate: config.openCreate,
    openEdit: config.openEdit,
    openView: config.openView,
    saveSetting: config.saveSetting,
    selectedPlatform: config.selectedPlatform,
    currentStatus: config.currentStatus,
    currentStatusColor: config.currentStatusColor,
    getValueByPath: config.getValueByPath,
    updateFieldValue: config.updateFieldValue,
    getNamePath: config.getNamePath,
    formatDateTime: config.formatDateTime,
  };
};

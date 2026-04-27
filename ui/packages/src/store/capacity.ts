import { defineStore } from "pinia";
import { computed, ref } from "vue";
import type { StorageCapacityDTO } from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { pickDefaultStorageSettingId } from "@/utils/storage";

const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();

export const useCapacityStore = defineStore("capacity", () => {
  const loading = ref(false);
  const capacity = ref<StorageCapacityDTO | null>(null);
  const storageSettingId = ref("");

  const percent = computed(() => {
    const total = capacity.value?.totalBytes || 0;
    const used = capacity.value?.usedBytes || 0;
    if (total <= 0) return 0;
    return Math.max(0, Math.min(100, Math.round((used / total) * 100)));
  });

  const formatBytes = (bytes?: number) => {
    const value = bytes || 0;
    if (value <= 0) return "0 B";
    const units = ["B", "KB", "MB", "GB", "TB", "PB"];
    let size = value;
    let unitIndex = 0;
    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex += 1;
    }
    const precision = unitIndex === 0 ? 0 : size >= 10 ? 1 : 2;
    return `${size.toFixed(precision)} ${units[unitIndex]}`;
  };

  const loadCapacity = async (force = false) => {
    if (loading.value) return;
    if (capacity.value && !force) return;

    loading.value = true;
    try {
      if (!storageSettingId.value || force) {
        const settingsResp = await generatedFilesysApi.listStorageSettings();
        const settings = settingsResp.data ?? [];
        storageSettingId.value = pickDefaultStorageSettingId(settings);
      }

      const resp = await generatedFilesysApi.getStorageCapacity(
        storageSettingId.value ? { settingId: storageSettingId.value } : undefined,
      );
      capacity.value = resp.data ?? null;
    } catch (error) {
      console.error("获取容量信息失败:", error);
      capacity.value = null;
    } finally {
      loading.value = false;
    }
  };

  return {
    loading,
    capacity,
    storageSettingId,
    percent,
    formatBytes,
    loadCapacity,
  };
});

import { computed, ref } from "vue";
import type { StoragePlatformDTO, StorageSettingDTO } from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { customMessage } from "@/utils/message";
import type { StorageCardItem, PlatformOption, StorageConfigData, StorageTemplateOptions } from "./types";

export const useStorageList = () => {
  const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();
  const loading = ref(false);
  const searchValue = ref("");
  const storagePlatforms = ref<StoragePlatformDTO[]>([]);
  const templates = ref<StorageTemplateOptions>([]);
  const settings = ref<StorageSettingDTO[]>([]);
  const deleteModalOpen = ref(false);
  const deleteLoading = ref(false);
  const deleteTarget = ref<StorageCardItem | null>(null);

  const fallbackPlatformOptions: PlatformOption[] = [];

  const platformMap = computed(() => {
    return new Map(storagePlatforms.value.map((item) => [String(item.identifier ?? ""), item]));
  });

  const platformOptions = computed<PlatformOption[]>(() => {
    const apiOptions = templates.value
      .map((item) => ({
        label: item.name || item.identifier || "",
        value: item.identifier || "",
        description: item.description || "",
      }))
      .filter((item) => item.label && item.value);

    const merged = [...apiOptions];
    fallbackPlatformOptions.forEach((fallback) => {
      if (!merged.some((item) => item.value === fallback.value || item.label === fallback.label)) {
        merged.push(fallback);
      }
    });

    return merged;
  });

  const parseConfigData = (configData?: string): StorageConfigData => {
    if (!configData) return {};
    try {
      const parsed = JSON.parse(configData) as StorageConfigData;
      return parsed && typeof parsed === "object" ? parsed : {};
    } catch {
      return {};
    }
  };

  const storageItems = computed<StorageCardItem[]>(() =>
    settings.value.map((setting) => {
      const platform = platformMap.value.get(String(setting.platformIdentifier ?? ""));
      const config = parseConfigData(setting.configData);
      return {
        ...(platform ?? {}),
        setting,
        config,
        displayName:
          platform?.name ||
          setting.platformIdentifier ||
          setting.id ||
          "未命名平台",
      };
    }),
  );

  const filteredStorageItems = computed(() => {
    const keyword = searchValue.value.trim().toLowerCase();
    if (!keyword) return storageItems.value;

    return storageItems.value.filter((item) => {
      const mergedText = [
        item.displayName,
        item.identifier,
        item.description,
        item.setting?.remark,
        item.setting?.platformIdentifier,
        item.config?.bucket,
        item.config?.endpoint,
        item.setting?.id,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return mergedText.includes(keyword);
    });
  });

  const loadStorage = async () => {
    loading.value = true;
    try {
      const [platformResp, settingsResp, templateResp] = await Promise.all([
        generatedFilesysApi.listStoragePlatforms(),
        generatedFilesysApi.listStorageSettings(),
        generatedFilesysApi.listStorageFormTemplates(),
      ]);
      storagePlatforms.value = platformResp.data ?? [];
      settings.value = settingsResp.data ?? [];
      templates.value = templateResp.data ?? [];
    } finally {
      loading.value = false;
    }
  };

  const getStatusTagColor = (enabled?: number) => (enabled ? "green" : "default");
  const getStatusLabel = (enabled?: number) => (enabled ? "已启用" : "未启用");
  const getToggleLabel = (enabled?: number) => (enabled ? "停用" : "启用");
  const getToggleTheme = (enabled?: number): "danger" | "primary" =>
    (enabled ? "danger" : "primary");

  const isProtectedStorage = (record: StorageCardItem) =>
    String(record.setting?.platformIdentifier ?? record.identifier ?? "").toLowerCase() === "local";

  const toggleStatus = async (record: StorageCardItem) => {
    if (isProtectedStorage(record)) {
      customMessage.info("本地存储配置不支持停用或启用");
      return;
    }

    const settingId = record.setting?.id;
    const enabled = record.setting?.enabled;
    if (!settingId || enabled === undefined) return;

    const nextEnabled = enabled ? 0 : 1;
    if (nextEnabled === 1) {
      const disableTargets = settings.value.filter(
        (item) => item.id && item.id !== settingId && item.enabled === 1,
      );
      await Promise.all(
        disableTargets.map((item) => generatedFilesysApi.updateStorageSettingStatus(item.id!, 0, {})),
      );
    }

    await generatedFilesysApi.updateStorageSettingStatus(settingId, nextEnabled, {});
    await loadStorage();
  };

  const openDelete = (record: StorageCardItem) => {
    if (isProtectedStorage(record)) {
      customMessage.info("本地存储配置不支持删除");
      return;
    }

    if (record.setting?.enabled) {
      customMessage.info("已启用的存储不支持删除");
      return;
    }
    deleteTarget.value = record;
    deleteModalOpen.value = true;
  };

  const confirmDelete = async () => {
    const settingId = deleteTarget.value?.setting?.id;
    if (!settingId) {
      customMessage.warning("请选择要删除的存储配置");
      return;
    }

    if (deleteTarget.value?.setting?.enabled) {
      customMessage.info("已启用的存储不支持删除");
      return;
    }

    deleteLoading.value = true;
    try {
      await generatedFilesysApi.deleteStorageSetting(settingId);
      customMessage.success("存储配置已删除");
      deleteModalOpen.value = false;
      deleteTarget.value = null;
      await loadStorage();
    } finally {
      deleteLoading.value = false;
    }
  };

  const closeDeleteModal = () => {
    deleteModalOpen.value = false;
    deleteTarget.value = null;
  };

  return {
    loading,
    searchValue,
    storagePlatforms,
    templates,
    settings,
    deleteModalOpen,
    deleteLoading,
    deleteTarget,
    platformMap,
    platformOptions,
    storageItems,
    filteredStorageItems,
    loadStorage,
    getStatusTagColor,
    getStatusLabel,
    getToggleLabel,
    getToggleTheme,
    isProtectedStorage,
    toggleStatus,
    openDelete,
    confirmDelete,
    closeDeleteModal,
  };
};

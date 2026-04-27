import { computed, reactive, ref, watch, type ComputedRef } from "vue";
import type { UpsertStorageSettingCommand } from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { anonymousAccount } from "@/constants/anonymousAccount";
import { formatDateTime } from "@/utils/format";
import { useThemeStore } from "@/store/theme";
import { customMessage } from "@/utils/message";
import type {
  ModalMode,
  PlatformOption,
  StorageCardItem,
  StorageConfigData,
  StorageSchemaNode,
  FormSchemaDetailData,
} from "./types";
import { useStorageSchema } from "./useStorageSchema";

interface UseStorageConfigOptions {
  platformOptions: ComputedRef<PlatformOption[]>;
  loadStorage: () => Promise<void>;
  isProtectedStorage: (record: StorageCardItem) => boolean;
}

export const useStorageConfig = ({
  platformOptions,
  loadStorage,
  isProtectedStorage,
}: UseStorageConfigOptions) => {
  const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();
  const themeStore = useThemeStore();

  const saveLoading = ref(false);
  const modalOpen = ref(false);
  const modalMode = ref<ModalMode>("create");
  const detailLoading = ref(false);
  const formModel = ref<StorageConfigData>({});
  const pendingFormData = ref<StorageConfigData | undefined>();
  const activeDetail = reactive<{ detail: FormSchemaDetailData }>({ detail: {} });
  const schemaVersion = ref(0);
  const configFormRef = ref<{ clearValidate?: () => Promise<void> | void } | null>(null);
  let activeDetailRequestId = 0;

  const form = reactive({
    id: "",
    platformIdentifier: "",
    remark: "",
    enabled: 1,
  });

  const isCompactMode = computed(() => themeStore.isCompactMode);

  const {
    fieldErrors,
    activeSchema,
    detailColumns,
    renderFields,
    clearFieldErrors,
    isEmptyFieldValue,
    validateDynamicFields,
    getValueByPath,
    updateFieldValue,
    getNamePath,
    parseConfigData,
    normalizeStorageConfigData,
    mergeDeep,
  } = useStorageSchema({
    activeDetail,
    formModel,
  });

  const clearActiveDetail = () => {
    activeDetail.detail = {};
    formModel.value = {};
    pendingFormData.value = undefined;
    clearFieldErrors();
    schemaVersion.value += 1;
  };

  const loadActiveDetail = async (identifier?: string) => {
    if (!identifier) {
      clearActiveDetail();
      return;
    }

    const requestId = ++activeDetailRequestId;
    detailLoading.value = true;
    try {
      const detailResp = await generatedFilesysApi.getStorageFormTemplateByIdentifier(identifier);
      const detail = detailResp.data;
      if (!detail) {
        clearActiveDetail();
        return;
      }

      if (requestId !== activeDetailRequestId || form.platformIdentifier !== identifier) return;

      await new Promise<void>((resolve) => {
        window.setTimeout(resolve, 500);
      });

      if (requestId !== activeDetailRequestId || form.platformIdentifier !== identifier) return;

      const templateSchema = (detail.formSchema ?? {}) as {
        schema?: StorageSchemaNode;
        initialValues?: StorageConfigData;
        detailOptions?: Record<string, any>;
        gridDefaults?: Record<string, any>;
      };
      const schema = templateSchema.schema ?? (detail.formSchema as StorageSchemaNode | undefined) ?? {};
      const initialValues = mergeDeep(
        normalizeStorageConfigData((templateSchema.initialValues ?? detail.initialValues ?? {}) as StorageConfigData),
        pendingFormData.value ?? {},
      );
      activeDetail.detail = {
        formSchema: schema,
        formData: initialValues,
        formDetailOptions: {
          columns: templateSchema.detailOptions?.maxColumns ?? templateSchema.gridDefaults?.maxColumns ?? 1,
          ...(templateSchema.detailOptions ?? {}),
        },
      };
      formModel.value = { ...initialValues };
      pendingFormData.value = undefined;
      clearFieldErrors();
      schemaVersion.value += 1;
      await configFormRef.value?.clearValidate?.();
    } catch {
      if (requestId === activeDetailRequestId && form.platformIdentifier === identifier) {
        clearActiveDetail();
      }
    } finally {
      if (requestId === activeDetailRequestId) {
        detailLoading.value = false;
      }
    }
  };

  const selectedPlatform = computed(() =>
    platformOptions.value.find((item) => item.value === form.platformIdentifier) ??
    platformOptions.value[0],
  );

  const currentStatus = computed(() => (form.enabled ? "已启用" : "未启用"));

  const currentStatusColor = computed(() =>
    modalMode.value === "view" ? "default" : currentStatus.value.includes("启用") ? "green" : "default",
  );

  const resetForm = () => {
    form.id = "";
    form.platformIdentifier =
      platformOptions.value.find((item) => item.value === "rustfs")?.value ??
      platformOptions.value.find((item) => item.value === "qiniu")?.value ??
      platformOptions.value[0]?.value ??
      "";
    form.remark = "";
    form.enabled = 0;
    formModel.value = {};
    clearFieldErrors();
  };

  const fillForm = (record: StorageCardItem, mode: ModalMode) => {
    if (isProtectedStorage(record)) {
      customMessage.info("本地存储配置不支持查看或编辑");
      return;
    }

    const config = record.config ?? parseConfigData(record.setting?.configData);
    modalMode.value = mode;
    form.id = record.setting?.id ?? "";
    form.platformIdentifier = record.setting?.platformIdentifier ?? record.identifier ?? "";
    form.remark = record.setting?.remark ?? "";
    form.enabled = record.setting?.enabled ?? 1;
    formModel.value = { ...config };
    pendingFormData.value = { ...config };
    clearFieldErrors();
    modalOpen.value = true;
  };

  const openCreate = () => {
    modalMode.value = "create";
    pendingFormData.value = undefined;
    resetForm();
    modalOpen.value = true;
  };

  const openEdit = (record: StorageCardItem) => {
    fillForm(record, "edit");
  };

  const openView = (record: StorageCardItem) => {
    fillForm(record, "view");
  };

  const saveSetting = async () => {
    if (!form.platformIdentifier) {
      customMessage.warning("请选择存储平台");
      return;
    }

    if (!validateDynamicFields()) {
      return;
    }

    saveLoading.value = true;
    try {
      const payload: UpsertStorageSettingCommand = {
        id: form.id || undefined,
        platformIdentifier: form.platformIdentifier,
        remark: form.remark || undefined,
        enabled: form.enabled,
        userId: anonymousAccount.id,
        configData: JSON.stringify(normalizeStorageConfigData(formModel.value ?? {}), null, 2),
      };
      await generatedFilesysApi.upsertStorageSetting(payload);
      customMessage.success("存储配置已保存");
      modalOpen.value = false;
      await loadStorage();
    } finally {
      saveLoading.value = false;
    }
  };

  watch(
    [modalOpen, () => form.platformIdentifier],
    ([visible, identifier]) => {
      if (!visible) {
        clearActiveDetail();
        return;
      }

      void loadActiveDetail(identifier);
    },
    { immediate: false },
  );

  watch(
    platformOptions,
    (options) => {
      if (
        !modalOpen.value ||
        modalMode.value !== "create" ||
        form.platformIdentifier ||
        options.length === 0
      ) {
        return;
      }

      form.platformIdentifier = options[0].value;
    },
    { immediate: true },
  );

  return {
    saveLoading,
    modalOpen,
    modalMode,
    detailLoading,
    formModel,
    pendingFormData,
    activeDetail,
    schemaVersion,
    configFormRef,
    isCompactMode,
    fieldErrors,
    form,
    activeSchema,
    detailColumns,
    renderFields,
    clearFieldErrors,
    validateDynamicFields,
    clearActiveDetail,
    loadActiveDetail,
    parseConfigData,
    resetForm,
    fillForm,
    openCreate,
    openEdit,
    openView,
    saveSetting,
    selectedPlatform,
    currentStatus,
    currentStatusColor,
    getValueByPath,
    updateFieldValue,
    getNamePath,
    isEmptyFieldValue,
    formatDateTime,
  };
};

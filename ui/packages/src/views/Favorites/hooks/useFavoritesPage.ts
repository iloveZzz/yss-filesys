import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import type { FileRecordDTO } from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { anonymousAccount } from "@/constants/anonymousAccount";
import { formatBytes, formatDateTime } from "@/utils/format";
import { customMessage } from "@/utils/message";

const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();

export const useFavoritesPage = () => {
  const router = useRouter();

  const loading = ref(false);
  const actionLoading = ref(false);
  const searchValue = ref("");
  const files = ref<FileRecordDTO[]>([]);
  const selectedFileIds = ref<string[]>([]);
  const pageIndex = ref(0);
  const pageSize = ref(10);
  const total = ref(0);

  const favoriteCount = computed(() => total.value);
  const selectableFileIds = computed(() =>
    files.value.map((item) => item.fileId).filter((item): item is string => !!item),
  );
  const selectedCount = computed(() => selectedFileIds.value.length);
  const selectedCountOnPage = computed(() =>
    selectableFileIds.value.filter((fileId) => selectedFileIds.value.includes(fileId)).length,
  );
  const allVisibleSelected = computed(
    () => selectableFileIds.value.length > 0 && selectedCountOnPage.value === selectableFileIds.value.length,
  );
  const visibleSelectionIndeterminate = computed(
    () => selectedCountOnPage.value > 0 && selectedCountOnPage.value < selectableFileIds.value.length,
  );

  const getFileLabel = (record: FileRecordDTO) =>
    record.displayName || record.originalName || record.fileId || "-";

  const getFileTone = (record: FileRecordDTO) => {
    if (record.isDir) return "folder";
    const label = getFileLabel(record);
    const ext = (record.suffix || label.split(".").at(-1) || "").toLowerCase();
    if (["pdf"].includes(ext)) return "pdf";
    if (["png", "jpg", "jpeg", "gif", "webp", "bmp"].includes(ext)) return "image";
    if (["txt", "md", "log"].includes(ext)) return "text";
    if (["zip", "rar", "7z", "tar", "gz", "tgz"].includes(ext)) return "archive";
    if (["xls", "xlsx", "csv"].includes(ext)) return "sheet";
    return "file";
  };

  const getFileExt = (record: FileRecordDTO) => {
    if (record.isDir) return "DIR";
    if (record.suffix) return record.suffix.toUpperCase();
    const label = getFileLabel(record);
    const segments = label.split(".");
    return segments.length > 1 ? (segments.at(-1) || "FILE").toUpperCase() : "FILE";
  };
  const loadFavorites = async () => {
    loading.value = true;
    try {
      const resp: any = await generatedFilesysApi.search({
        favorite: true,
        deleted: false,
        keyword: searchValue.value.trim() || undefined,
        pageIndex: pageIndex.value,
        pageSize: pageSize.value,
      });
      files.value = resp.data ?? [];
      total.value = resp.totalCount ?? 0;
    } finally {
      loading.value = false;
    }
  };

  const handleSearch = async () => {
    pageIndex.value = 0;
    selectedFileIds.value = [];
    await loadFavorites();
  };

  const handlePageChange = async (page: number, size: number) => {
    pageIndex.value = Math.max((page || 1) - 1, 0);
    pageSize.value = size || 10;
    selectedFileIds.value = [];
    await loadFavorites();
  };

  const openPreview = (record: FileRecordDTO) => {
    if (!record.fileId || record.isDir) return;
    const url = router.resolve({
      path: `/preview/${record.fileId}`,
      query: {
        name: record.displayName || record.originalName,
      },
    }).href;
    window.open(url, "_blank", "noopener,noreferrer");
  };

  const downloadFile = async (record: FileRecordDTO) => {
    if (!record.fileId || record.isDir) {
      customMessage.info("目录暂不支持下载");
      return;
    }

    const resp = await generatedFilesysApi.downloadFile(record.fileId);
    const blob =
      resp instanceof Blob ? resp : new Blob([resp as BlobPart], { type: "application/octet-stream" });
    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = objectUrl;
    anchor.download = getFileLabel(record);
    anchor.rel = "noopener noreferrer";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
  };

  const unfavoriteRecords = async (records: FileRecordDTO[]) => {
    const fileIds = records.map((item) => item.fileId).filter((item): item is string => !!item);
    if (!fileIds.length) {
      customMessage.warning("暂无可取消收藏的文件");
      return;
    }

    actionLoading.value = true;
    try {
      await generatedFilesysApi.unfavorite({
        fileIds,
        userId: anonymousAccount.id,
      });
      customMessage.success("已取消收藏");
      selectedFileIds.value = selectedFileIds.value.filter((item) => !fileIds.includes(item));
      await loadFavorites();
    } finally {
      actionLoading.value = false;
    }
  };

  const cancelCurrentPageFavorites = async () => {
    await unfavoriteRecords(files.value);
  };

  const clearSelection = () => {
    selectedFileIds.value = [];
  };

  const isSelected = (record: FileRecordDTO) =>
    !!record.fileId && selectedFileIds.value.includes(record.fileId);

  const toggleFileSelection = (record: FileRecordDTO, checked?: boolean) => {
    if (!record.fileId) return;
    const nextChecked = typeof checked === "boolean" ? checked : !isSelected(record);
    if (nextChecked) {
      if (!selectedFileIds.value.includes(record.fileId)) {
        selectedFileIds.value = [...selectedFileIds.value, record.fileId];
      }
      return;
    }
    selectedFileIds.value = selectedFileIds.value.filter((item) => item !== record.fileId);
  };

  const handleSelectAllVisible = (checked: boolean) => {
    if (checked) {
      selectedFileIds.value = Array.from(new Set([...selectedFileIds.value, ...selectableFileIds.value]));
      return;
    }
    selectedFileIds.value = selectedFileIds.value.filter((item) => !selectableFileIds.value.includes(item));
  };

  const handleSelectAllChange = (event: { target?: { checked?: boolean } }) => {
    handleSelectAllVisible(!!event.target?.checked);
  };

  const handleItemSelectChange = (record: FileRecordDTO, event: { target?: { checked?: boolean } }) => {
    toggleFileSelection(record, !!event.target?.checked);
  };

  const cancelSelectedFavorites = async () => {
    if (!selectedFileIds.value.length) {
      customMessage.warning("请先选择收藏项");
      return;
    }
    const selectedRecords = files.value.filter((item) => item.fileId && selectedFileIds.value.includes(item.fileId));
    await unfavoriteRecords(selectedRecords);
  };

  const getToneClass = (record: FileRecordDTO) => {
    if (record.isDir) return "folder";
    return `tone-${getFileTone(record)}`;
  };

  onMounted(async () => {
    await loadFavorites();
  });

  return {
    router,
    loading,
    actionLoading,
    searchValue,
    files,
    selectedFileIds,
    pageIndex,
    pageSize,
    total,
    favoriteCount,
    selectableFileIds,
    selectedCount,
    selectedCountOnPage,
    allVisibleSelected,
    visibleSelectionIndeterminate,
    loadFavorites,
    handleSearch,
    handlePageChange,
    openPreview,
    downloadFile,
    unfavoriteRecords,
    cancelCurrentPageFavorites,
    clearSelection,
    isSelected,
    toggleFileSelection,
    handleSelectAllVisible,
    handleSelectAllChange,
    handleItemSelectChange,
    cancelSelectedFavorites,
    getToneClass,
    getFileLabel,
    getFileTone,
    getFileExt,
    formatBytes,
    formatDateTime,
  };
};

import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  CreateDirectoryCommand,
  FavoriteFilesCommand,
  FileRecordDTO,
  PermanentlyDeleteRecycleCommand,
  RenameFileCommand,
  StorageSettingDTO,
} from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import {
  moveToRecycleFiles,
  permanentlyDeleteRecycleFiles,
} from "@/api/filesys-commands";
import { anonymousAccount } from "@/constants/anonymousAccount";
import { customMessage } from "@/utils/message";
import { formatBytes, formatDateTime } from "@/utils/format";
import { pickDefaultStorageSettingId } from "@/utils/storage";
import { useFilesMove } from "./useFilesMove";
import { useFilesShare } from "./useFilesShare";
import { useFilesUpload } from "./useFilesUpload";

type ViewMode = "files" | "recycle";

export const useFilesPage = () => {
  const router = useRouter();
  const route = useRoute();
  const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();

  const loading = ref(false);
  const mode = ref<ViewMode>(route.path === "/recycle" ? "recycle" : "files");
  const files = ref<FileRecordDTO[]>([]);
  const recycleFiles = ref<FileRecordDTO[]>([]);
  const breadcrumb = ref<FileRecordDTO[]>([]);
  const storageSettings = ref<StorageSettingDTO[]>([]);
  const selectedRowKeys = ref<string[]>([]);
  const filePageIndex = ref(0);
  const filePageSize = ref(10);
  const fileTotal = ref(0);
  const recyclePageIndex = ref(0);
  const recyclePageSize = ref(10);
  const recycleTotal = ref(0);
  const sortField = ref<"updateTime" | "size">("updateTime");
  const sortOrder = ref<"asc" | "desc">("desc");
  const detailVisible = ref(false);
  const detailLoading = ref(false);
  const currentDetail = ref<FileRecordDTO | null>(null);
  const createVisible = ref(false);
  const createLoading = ref(false);
  const renameVisible = ref(false);
  const downloadLoading = ref(false);
  const recycleActionLoading = ref(false);
  const deleteDialogVisible = ref(false);
  const deleteActionLoading = ref(false);
  const deleteTarget = ref<FileRecordDTO | null>(null);
  const renameTarget = ref<FileRecordDTO | null>(null);
  const createForm = reactive({
    folderName: "",
    storageSettingId: "",
  });
  const renameForm = reactive({
    fileName: "",
  });
  const contextMenuVisible = ref(false);
  const contextMenuX = ref(0);
  const contextMenuY = ref(0);
  const contextMenuTarget = ref<FileRecordDTO | null>(null);
  const currentParentId = ref<string | undefined>(undefined);
  const tableAreaRef = ref<HTMLDivElement>();
  const rowClickTimer = ref<number | null>(null);
  const uploadRefreshTimer = ref<number | null>(null);
  const loadStorageSettings = async () => {
    const resp = await generatedFilesysApi.listStorageSettings();
    storageSettings.value = resp.data ?? [];
    if (!createForm.storageSettingId) {
      createForm.storageSettingId = pickDefaultStorageSettingId(storageSettings.value);
    }
  };

  const loadFileData = async () => {
    loading.value = true;
    try {
      const [fileResp, pathResp] = await Promise.all([
        generatedFilesysApi.search({
          parentId: currentParentId.value,
          deleted: false,
          favorite: false,
          sortField: sortField.value,
          sortOrder: sortOrder.value,
          pageIndex: filePageIndex.value,
          pageSize: filePageSize.value,
        }),
        currentParentId.value
          ? generatedFilesysApi.getDirectoryTreePath(currentParentId.value)
          : Promise.resolve(null),
      ]);

      files.value = fileResp.data ?? [];
      fileTotal.value = fileResp.totalCount ?? 0;
      breadcrumb.value = pathResp?.data ?? [];
    } finally {
      loading.value = false;
    }
  };

  const loadRecycleData = async () => {
    loading.value = true;
    try {
      const resp = await generatedFilesysApi.listRecycleFiles({
        pageIndex: recyclePageIndex.value,
        pageSize: recyclePageSize.value,
      });
      recycleFiles.value = resp.data ?? [];
      recycleTotal.value = resp.totalCount ?? 0;
    } finally {
      loading.value = false;
    }
  };

  const tableData = computed(() => files.value);

  const selectedRows = computed(() =>
    tableData.value.filter((item) => item.fileId && selectedRowKeys.value.includes(item.fileId)),
  );

  const recycleSelectedRows = computed(() =>
    recycleFiles.value.filter((item) => item.fileId && selectedRowKeys.value.includes(item.fileId)),
  );

  const selectedCount = computed(() => selectedRowKeys.value.length);
  const recycleTableLoading = computed(() => loading.value && recycleFiles.value.length === 0);

  const rowKey = (record: FileRecordDTO) =>
    record.fileId || record.displayName || record.originalName || "";

  const getFileLabel = (record: FileRecordDTO) =>
    record.displayName || record.originalName || record.fileId || "-";

  const getFileExt = (record: FileRecordDTO) => {
    if (record.isDir) return "DIR";
    if (record.suffix) return record.suffix.toUpperCase();
    const label = getFileLabel(record);
    const segments = label.split(".");
    return segments.length > 1 ? (segments.at(-1) || "FILE").toUpperCase() : "FILE";
  };

  const getFileTone = (record: FileRecordDTO) => {
    if (record.isDir) return "folder";
    const ext = (record.suffix || getFileLabel(record).split(".").at(-1) || "").toLowerCase();
    if (["pdf"].includes(ext)) return "pdf";
    if (["png", "jpg", "jpeg", "gif", "webp", "bmp"].includes(ext)) return "image";
    if (["txt", "md", "log"].includes(ext)) return "text";
    if (["zip", "rar", "7z", "tar", "gz", "tgz"].includes(ext)) return "archive";
    if (["xls", "xlsx", "csv"].includes(ext)) return "sheet";
    return "file";
  };

  const openDetail = async (record: FileRecordDTO) => {
    if (!record.fileId) return;
    detailVisible.value = true;
    detailLoading.value = true;
    currentDetail.value = record;
    try {
      const resp = await generatedFilesysApi.getFileById(record.fileId);
      currentDetail.value = {
        ...record,
        ...(resp.data ?? {}),
      };
    } finally {
      detailLoading.value = false;
    }
  };

  const openContextMenu = (event: MouseEvent, record: FileRecordDTO) => {
    if (mode.value !== "files") return;
    event.preventDefault();
    const key = rowKey(record);
    if (key && !selectedRowKeys.value.includes(key)) {
      selectedRowKeys.value = [key];
    }
    contextMenuTarget.value = record;
    contextMenuX.value = Math.min(event.clientX, window.innerWidth - 240);
    contextMenuY.value = Math.min(event.clientY, window.innerHeight - 360);
    contextMenuVisible.value = true;
  };

  const closeContextMenu = () => {
    contextMenuVisible.value = false;
    contextMenuTarget.value = null;
  };

  const openRenameModal = (record?: FileRecordDTO | null) => {
    const target = record ?? contextMenuTarget.value;
    if (!target?.fileId) return;
    renameTarget.value = target;
    renameForm.fileName = getFileLabel(target);
    renameVisible.value = true;
    closeContextMenu();
  };

  const recycleRecords = async (records: FileRecordDTO[]) => {
    const fileIds = records
      .map((item) => item.fileId)
      .filter((item): item is string => !!item);
    if (!fileIds.length) {
      customMessage.warning("请先选择文件");
      return;
    }

    await moveToRecycleFiles({ fileIds });
    customMessage.success("已放入回收站");
    selectedRowKeys.value = selectedRowKeys.value.filter((key) => !fileIds.includes(key));
    await reload();
  };

  const openRecycleAction = async (record?: FileRecordDTO | null) => {
    const target = record ?? contextMenuTarget.value;
    if (!target?.fileId) return;
    await recycleRecords([target]);
    closeContextMenu();
  };

  const recycleSelected = async () => {
    await recycleRecords(selectedRows.value);
  };

  const createFolder = async () => {
    if (createLoading.value) return;
    if (!createForm.folderName || !createForm.storageSettingId) {
      customMessage.warning("请填写文件夹名称并选择存储配置");
      return;
    }

    createLoading.value = true;
    try {
      const payload: CreateDirectoryCommand = {
        folderName: createForm.folderName,
        storageSettingId: createForm.storageSettingId,
        parentId: currentParentId.value,
        userId: anonymousAccount.id,
      };

      await generatedFilesysApi.createDirectory(payload);
      createVisible.value = false;
      createForm.folderName = "";
      await loadFileData();
    } finally {
      createLoading.value = false;
    }
  };

  const renameFile = async () => {
    if (!renameTarget.value?.fileId || !renameForm.fileName) return;
    const payload: RenameFileCommand = {
      fileName: renameForm.fileName,
      userId: anonymousAccount.id,
    };
    await generatedFilesysApi.renameFile(renameTarget.value.fileId, payload);
    renameVisible.value = false;
    renameTarget.value = null;
    await loadFileData();
  };

  const clearRecycle = async () => {
    recycleActionLoading.value = true;
    try {
      await generatedFilesysApi.clearRecycle();
      recyclePageIndex.value = 0;
      await loadRecycleData();
    } finally {
      recycleActionLoading.value = false;
    }
  };

  const restoreRecord = async (record: FileRecordDTO) => {
    if (!record.fileId) return;
    await generatedFilesysApi.restore({ fileIds: [record.fileId], userId: anonymousAccount.id });
    customMessage.success("已恢复");
    selectedRowKeys.value = selectedRowKeys.value.filter((key) => key !== record.fileId);
    await loadRecycleData();
  };

  const deleteRecord = async (record: FileRecordDTO) => {
    if (!record.fileId) return;
    deleteTarget.value = record;
    deleteDialogVisible.value = true;
  };

  const openBatchDeleteDialog = () => {
    if (!selectedCount.value) {
      customMessage.warning("请先选择文件");
      return;
    }
    deleteTarget.value = null;
    deleteDialogVisible.value = true;
  };

  const closeDeleteDialog = () => {
    deleteDialogVisible.value = false;
    deleteTarget.value = null;
  };

  const confirmDeleteRecord = async () => {
    const fileIds = deleteTarget.value?.fileId
      ? [deleteTarget.value.fileId]
      : recycleSelectedRows.value.map((item) => item.fileId).filter((item): item is string => !!item);
    if (!fileIds.length) return;

    deleteActionLoading.value = true;
    try {
      const payload: PermanentlyDeleteRecycleCommand = {
        fileIds,
        userId: anonymousAccount.id,
      };
      await permanentlyDeleteRecycleFiles(payload);
      customMessage.success("已彻底删除");
      selectedRowKeys.value = selectedRowKeys.value.filter((key) => !fileIds.includes(key));
      closeDeleteDialog();
      await loadRecycleData();
    } finally {
      deleteActionLoading.value = false;
    }
  };

  const handleFileTableChange = async (pagination: { current?: number; pageSize?: number }) => {
    filePageIndex.value = Math.max((pagination.current || 1) - 1, 0);
    filePageSize.value = pagination.pageSize || 10;
    selectedRowKeys.value = [];
    await loadFileData();
  };

  const handleRecycleTableChange = async (pagination: { current?: number; pageSize?: number }) => {
    recyclePageIndex.value = Math.max((pagination.current || 1) - 1, 0);
    recyclePageSize.value = pagination.pageSize || 10;
    selectedRowKeys.value = [];
    await loadRecycleData();
  };

  const handleSortChange = async (nextSort: { sortField?: "updateTime" | "size"; sortOrder?: "asc" | "desc" }) => {
    sortField.value = nextSort.sortField || sortField.value;
    sortOrder.value = nextSort.sortOrder || sortOrder.value;
    filePageIndex.value = 0;
    selectedRowKeys.value = [];
    await loadFileData();
  };

  const reload = async () => {
    selectedRowKeys.value = [];
    if (mode.value === "recycle") {
      await loadRecycleData();
      return;
    }
    await loadStorageSettings();
    await loadFileData();
  };

  const openPreview = async (record: FileRecordDTO) => {
    if (!record.fileId || record.isDir) return;
    const url = router.resolve({
      path: `/preview/${record.fileId}`,
      query: {
        name: record.displayName,
      },
    }).href;
    window.open(url, "_blank", "noopener,noreferrer");
  };

  const downloadBlob = (blob: Blob, fileName: string) => {
    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = objectUrl;
    anchor.download = fileName;
    anchor.rel = "noopener noreferrer";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
  };

  const openDownload = async (record: FileRecordDTO) => {
    if (!record.fileId || record.isDir) return;
    const resp = await generatedFilesysApi.downloadFile(record.fileId);
    const blob =
      resp instanceof Blob ? resp : new Blob([resp as BlobPart], { type: "application/octet-stream" });
    downloadBlob(blob, getFileLabel(record));
  };

  const openFolder = async (record: FileRecordDTO) => {
    if (!record.fileId || !record.isDir) return;
    currentParentId.value = record.fileId;
    filePageIndex.value = 0;
    selectedRowKeys.value = [];
    await loadFileData();
  };

  const favoriteSelected = async () => {
    if (!selectedRows.value.length) {
      customMessage.warning("请先选择文件");
      return;
    }

    const payload: FavoriteFilesCommand = {
      fileIds: selectedRows.value.map((item) => item.fileId).filter(Boolean) as string[],
      userId: anonymousAccount.id,
    };
    await generatedFilesysApi.favorite(payload);
    customMessage.success("已加入收藏");
    await loadFileData();
  };

  const favoriteRecord = async (record?: FileRecordDTO | null) => {
    const target = record ?? contextMenuTarget.value;
    if (!target?.fileId) {
      customMessage.warning("请先选择文件");
      return;
    }

    await generatedFilesysApi.favorite({ fileIds: [target.fileId], userId: anonymousAccount.id });
    customMessage.success("已加入收藏");
    await loadFileData();
  };

  const downloadSelected = async () => {
    if (!selectedRows.value.length) {
      customMessage.warning("请先选择文件");
      return;
    }

    if (selectedRows.value.length === 1) {
      await openDownload(selectedRows.value[0]);
      return;
    }

    const fileIds = selectedRows.value
      .filter((item) => !item.isDir)
      .map((item) => item.fileId)
      .filter((item): item is string => !!item);
    if (!fileIds.length) {
      customMessage.warning("已选内容均为目录，暂不支持批量下载");
      return;
    }

    downloadLoading.value = true;
    try {
      const resp = await generatedFilesysApi.downloadFilesBatch(fileIds);
      const blob =
        resp instanceof Blob ? resp : new Blob([resp as BlobPart], { type: "application/zip" });
      downloadBlob(blob, `批量下载-${new Date().toISOString().slice(0, 19).replace(/[:T]/g, "-")}.zip`);
    } finally {
      downloadLoading.value = false;
    }
  };

  const handleFileUploadComplete = (event: Event) => {
    const detail = (event as CustomEvent<{ parentId?: string } | undefined>).detail;
    if (mode.value !== "files") return;
    if ((detail?.parentId || undefined) !== (currentParentId.value || undefined)) return;

    if (uploadRefreshTimer.value) {
      window.clearTimeout(uploadRefreshTimer.value);
    }

    uploadRefreshTimer.value = window.setTimeout(() => {
      uploadRefreshTimer.value = null;
      void loadFileData();
    }, 200);
  };

  const upload = useFilesUpload({
    currentParentId,
    loadFileData,
  });

  const share = useFilesShare({
    selectedRows,
    contextMenuTarget,
    loadFileData,
    getFileLabel,
    getFileTone,
    formatBytes,
    formatDateTime,
  });

  const move = useFilesMove({
    contextMenuTarget,
    selectedRowKeys,
    closeContextMenu,
    loadFileData,
  });

  const tableRowSelection = computed(() =>
    mode.value === "files"
      ? {
          type: "checkbox" as const,
          selectedRowKeys: selectedRowKeys.value,
          onChange: (keys: (string | number)[]) => {
            selectedRowKeys.value = keys.map(String);
          },
          getCheckboxProps: (record: FileRecordDTO) => ({
            disabled: !record.fileId,
          }),
        }
      : undefined,
  );

  const onTableRow = (record: FileRecordDTO) => ({
    onClick: () => {
      if (mode.value !== "files") return;
      if (rowClickTimer.value) {
        window.clearTimeout(rowClickTimer.value);
        rowClickTimer.value = null;
      }
      rowClickTimer.value = window.setTimeout(() => {
        const key = rowKey(record);
        if (!key) return;
        if (selectedRowKeys.value.includes(key)) {
          selectedRowKeys.value = selectedRowKeys.value.filter((item) => item !== key);
          return;
        }
        selectedRowKeys.value = [...selectedRowKeys.value, key];
        rowClickTimer.value = null;
      }, 180);
    },
    onDblclick: () => {
      if (mode.value !== "files" || !record.isDir) return;
      if (rowClickTimer.value) {
        window.clearTimeout(rowClickTimer.value);
        rowClickTimer.value = null;
      }
      void openFolder(record);
    },
    onContextmenu: (event: MouseEvent) => openContextMenu(event, record),
  });

  onMounted(async () => {
    window.addEventListener("file-upload-complete", handleFileUploadComplete);
    await reload();
  });

  onBeforeUnmount(() => {
    window.removeEventListener("file-upload-complete", handleFileUploadComplete);
    if (uploadRefreshTimer.value) {
      window.clearTimeout(uploadRefreshTimer.value);
      uploadRefreshTimer.value = null;
    }
  });

  watch(
    () => route.path,
    async (path) => {
      mode.value = path === "/recycle" ? "recycle" : "files";
      await reload();
    },
  );

  return {
    router,
    route,
    loading,
    mode,
    files,
    recycleFiles,
    breadcrumb,
    storageSettings,
    selectedRowKeys,
    filePageIndex,
    filePageSize,
    fileTotal,
    sortField,
    sortOrder,
    recyclePageIndex,
    recyclePageSize,
    recycleTotal,
    detailVisible,
    detailLoading,
    currentDetail,
    createVisible,
    createLoading,
    createForm,
    renameVisible,
    renameForm,
    downloadLoading,
    recycleActionLoading,
    deleteDialogVisible,
    deleteActionLoading,
    deleteTarget,
    renameTarget,
    uploadInput: upload.uploadInput,
    uploadQueue: upload.uploadQueue,
    uploadRunning: upload.uploadRunning,
    uploadSummary: upload.uploadSummary,
    uploadVisible: upload.uploadVisible,
    overwriteExisting: upload.overwriteExisting,
    moveVisible: move.moveVisible,
    moveLoading: move.moveLoading,
    moveFolders: move.moveFolders,
    moveBreadcrumb: move.moveBreadcrumb,
    moveCurrentParentId: move.moveCurrentParentId,
    moveSelectedFolderId: move.moveSelectedFolderId,
    moveLoadingFolders: move.moveLoadingFolders,
    contextMenuVisible,
    contextMenuX,
    contextMenuY,
    contextMenuTarget,
    currentParentId,
    tableAreaRef,
    rowClickTimer,
    uploadRefreshTimer,
    tableData,
    selectedRows,
    recycleSelectedRows,
    selectedCount,
    recycleTableLoading,
    rowKey,
    getFileLabel,
    getFileExt,
    getFileTone,
    getUploadStatusLabel: upload.getUploadStatusLabel,
    shareVisible: share.shareVisible,
    shareStage: share.shareStage,
    shareLoading: share.shareLoading,
    shareCreated: share.shareCreated,
    shareFiles: share.shareFiles,
    shareForm: share.shareForm,
    getSharePreviewInitial: share.getSharePreviewInitial,
    getSharePreviewTone: share.getSharePreviewTone,
    shareSelectedFileCount: share.shareSelectedFileCount,
    shareSelectedFilePreview: share.shareSelectedFilePreview,
    sharePreviewSingle: share.sharePreviewSingle,
    shareUrl: share.shareUrl,
    shareExpireText: share.shareExpireText,
    loadStorageSettings,
    loadFileData,
    loadRecycleData,
    handleFileTableChange,
    handleSortChange,
    handleRecycleTableChange,
    reload,
    openDetail,
    openContextMenu,
    closeContextMenu,
    openRenameModal,
    recycleRecords,
    openRecycleAction,
    recycleSelected,
    createFolder,
    renameFile,
    clearRecycle,
    restoreRecord,
    deleteRecord,
    openBatchDeleteDialog,
    closeDeleteDialog,
    confirmDeleteRecord,
    openPreview,
    openDownload,
    downloadBlob,
    triggerUpload: upload.triggerUpload,
    handleUploadPick: upload.handleUploadPick,
    openUploadPicker: upload.openUploadPicker,
    handleUploadDrop: upload.handleUploadDrop,
    clearUploadQueue: upload.clearUploadQueue,
    closeUploadModal: upload.closeUploadModal,
    confirmUploadQueue: upload.confirmUploadQueue,
    shareSelected: share.shareSelected,
    favoriteSelected,
    shareRecord: share.shareRecord,
    favoriteRecord,
    downloadSelected,
    openFolder,
    copyShareLink: share.copyShareLink,
    createShareLink: share.createShareLink,
    closeShareModal: share.closeShareModal,
    tableRowSelection,
    onTableRow,
    enterMoveFolder: move.enterMoveFolder,
    navigateMoveBreadcrumb: move.navigateMoveBreadcrumb,
    confirmMove: move.confirmMove,
    formatBytes,
    formatDateTime,
    customMessage,
    anonymousAccount,
    pickDefaultStorageSettingId,
    moveTargets: move.moveTargets,
    moveFolderPathCache: move.moveFolderPathCache,
    getPathFileIds: move.getPathFileIds,
    getSelectedFolderIds: move.getSelectedFolderIds,
    openMoveModal: move.openMoveModal,
    openBatchMoveModal: move.openBatchMoveModal,
  };
};

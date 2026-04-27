import { computed, reactive, ref, type Ref } from "vue";
import type { CreateShareCommand, FileRecordDTO, FileShareDTO } from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { anonymousAccount } from "@/constants/anonymousAccount";
import { customMessage } from "@/utils/message";

type SharePreviewFile = {
  fileId: string;
  fileName: string;
  fileSize?: string;
  fileKind?: "folder" | "image" | "pdf" | "text" | "archive" | "sheet" | "file";
};

interface UseFilesShareOptions {
  selectedRows: Ref<FileRecordDTO[]>;
  contextMenuTarget: Ref<FileRecordDTO | null>;
  loadFileData: () => Promise<void>;
  getFileLabel: (record: FileRecordDTO) => string;
  getFileTone: (record: FileRecordDTO) => string;
  formatBytes: (size: number) => string;
  formatDateTime: (value: string) => string;
}

export const useFilesShare = ({
  selectedRows,
  contextMenuTarget,
  loadFileData,
  getFileLabel,
  getFileTone,
  formatBytes,
  formatDateTime,
}: UseFilesShareOptions) => {
  const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();
  const shareVisible = ref(false);
  const shareStage = ref<"form" | "result">("form");
  const shareLoading = ref(false);
  const shareCreated = ref<FileShareDTO | null>(null);
  const shareFiles = ref<SharePreviewFile[]>([]);
  const shareForm = reactive({
    expireType: 1,
    expireTime: "",
    shareType: "PUBLIC",
    permissions: ["preview"],
    maxViewCountMode: "unlimited",
    maxDownloadCountMode: "unlimited",
    maxViewCount: 0,
    maxDownloadCount: 0,
    shareName: "",
  });

  const normalizeShareFiles = (records: FileRecordDTO[]): SharePreviewFile[] =>
    records.map((item) => ({
      fileId: item.fileId || item.displayName || item.originalName || `draft-${Math.random().toString(36).slice(2, 8)}`,
      fileName: getFileLabel(item),
      fileSize: item.size ? formatBytes(item.size) : undefined,
      fileKind: item.isDir
        ? "folder"
        : getFileTone(item) === "sheet"
          ? "sheet"
          : getFileTone(item) === "pdf"
            ? "pdf"
            : getFileTone(item) === "image"
              ? "image"
              : getFileTone(item) === "archive"
                ? "archive"
                : getFileTone(item) === "text"
                  ? "text"
                  : "file",
    }));

  const shareSelectedFileIds = computed(() =>
    shareFiles.value.map((item) => item.fileId).filter((item) => item && !item.startsWith("draft-")),
  );

  const shareSelectedFileCount = computed(() => {
    const count = shareFiles.value.length;
    return count > 0 ? count : 0;
  });

  const shareSelectedFilePreview = computed(() => shareFiles.value.slice(0, 3));
  const sharePreviewSingle = computed(() => shareFiles.value.length === 1);
  const shareUrl = computed(() => {
    const shareId = shareCreated.value?.shareId;
    if (!shareId) return "";
    return `${window.location.origin}/s/${shareId}`;
  });

  const shareExpireText = computed(() => {
    const expireTime = shareCreated.value?.expireTime;
    return expireTime ? formatDateTime(expireTime) : "未设置";
  });

  const getSharePreviewInitial = (file: SharePreviewFile) => {
    if (file.fileKind === "pdf") return "PDF";
    if (file.fileKind === "image") return "IMG";
    if (file.fileKind === "archive") return "ZIP";
    if (file.fileKind === "text") return "TXT";
    if (file.fileKind === "sheet") return "XLS";
    return "目录";
  };

  const getSharePreviewTone = (file: SharePreviewFile) => {
    if (file.fileKind === "pdf") return "pdf";
    if (file.fileKind === "image") return "image";
    if (file.fileKind === "archive") return "archive";
    if (file.fileKind === "text") return "text";
    if (file.fileKind === "sheet") return "sheet";
    return "folder";
  };

  const openShareModal = (records: FileRecordDTO[]) => {
    if (!records.length) {
      customMessage.warning("请先选择文件");
      return;
    }
    shareFiles.value = normalizeShareFiles(records);
    shareForm.shareName = getFileLabel(records[0]);
    shareForm.expireType = 1;
    shareForm.expireTime = "";
    shareForm.shareType = "PUBLIC";
    shareForm.permissions = ["preview"];
    shareForm.maxViewCountMode = "unlimited";
    shareForm.maxDownloadCountMode = "unlimited";
    shareForm.maxViewCount = 0;
    shareForm.maxDownloadCount = 0;
    shareVisible.value = true;
    shareStage.value = "form";
    shareCreated.value = null;
  };

  const createShareLink = async () => {
    const fileIds = shareSelectedFileIds.value;
    if (!fileIds.length) {
      customMessage.warning("请至少选择一个文件");
      return;
    }

    shareLoading.value = true;
    try {
      const payload: CreateShareCommand = {
        shareName: shareForm.shareName || shareFiles.value[0]?.fileName || undefined,
        fileIds,
        needShareCode: true,
        expireType: shareForm.expireType,
        expireTime: shareForm.expireType === 3 ? shareForm.expireTime || undefined : undefined,
        maxViewCount:
          shareForm.maxViewCountMode === "custom" ? shareForm.maxViewCount || undefined : undefined,
        maxDownloadCount:
          shareForm.maxDownloadCountMode === "custom"
            ? shareForm.maxDownloadCount || undefined
            : undefined,
        scope: shareForm.shareType,
        userId: anonymousAccount.id,
      };

      const resp = await generatedFilesysApi.createFileShare(payload);
      shareCreated.value = resp.data ?? null;
      shareStage.value = "result";
      customMessage.success("分享创建成功");
      await loadFileData();
    } finally {
      shareLoading.value = false;
    }
  };

  const closeShareModal = () => {
    shareVisible.value = false;
    shareStage.value = "form";
    shareCreated.value = null;
    shareFiles.value = [];
    shareForm.expireType = 1;
    shareForm.expireTime = "";
    shareForm.shareType = "PUBLIC";
    shareForm.permissions = ["preview"];
    shareForm.maxViewCountMode = "unlimited";
    shareForm.maxDownloadCountMode = "unlimited";
    shareForm.maxViewCount = 0;
    shareForm.maxDownloadCount = 0;
    shareForm.shareName = "";
  };

  const shareSelected = () => {
    if (!selectedRows.value.length) {
      customMessage.warning("请先选择文件");
      return;
    }
    openShareModal(selectedRows.value);
  };

  const shareRecord = (record?: FileRecordDTO | null) => {
    const target = record ?? contextMenuTarget.value;
    if (!target?.fileId) {
      customMessage.warning("请先选择文件");
      return;
    }
    openShareModal([target]);
  };

  const copyShareLink = async () => {
    if (!shareUrl.value) return;
    await navigator.clipboard.writeText(shareUrl.value);
    customMessage.success("分享链接已复制");
  };

  return {
    shareVisible,
    shareStage,
    shareLoading,
    shareCreated,
    shareFiles,
    shareForm,
    shareSelectedFileCount,
    shareSelectedFilePreview,
    sharePreviewSingle,
    shareUrl,
    shareExpireText,
    getSharePreviewInitial,
    getSharePreviewTone,
    shareSelected,
    shareRecord,
    createShareLink,
    closeShareModal,
    copyShareLink,
  };
};

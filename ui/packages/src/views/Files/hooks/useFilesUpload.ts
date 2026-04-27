import { ref, type Ref } from "vue";
import { useTransferStore } from "@/store";
import { customMessage } from "@/utils/message";

export type UploadStatus = "pending" | "uploading" | "success" | "error";

export interface UploadQueueItem {
  id: string;
  name: string;
  size: number;
  file: File;
  status: UploadStatus;
  progress: number;
  message?: string;
  taskId?: string;
}

interface UseFilesUploadOptions {
  currentParentId: Ref<string | undefined>;
  loadFileData: () => Promise<void>;
}

export const useFilesUpload = ({ currentParentId, loadFileData }: UseFilesUploadOptions) => {
  const MAX_UPLOAD_FILES = 10;
  const transferStore = useTransferStore();

  const uploadInput = ref<HTMLInputElement | null>(null);
  const uploadQueue = ref<UploadQueueItem[]>([]);
  const uploadRunning = ref(false);
  const uploadSummary = ref<string>("");
  const uploadVisible = ref(false);
  const overwriteExisting = ref(true);

  const createUploadItemId = (file: File) =>
    `${file.name}-${file.size}-${file.lastModified}-${Math.random().toString(36).slice(2, 8)}`;

  const getUploadStatusLabel = (status: UploadStatus) => {
    if (status === "uploading") return "上传中";
    if (status === "success") return "已完成";
    if (status === "error") return "失败";
    return "等待上传";
  };

  const appendUploadFiles = (files: File[]) => {
    if (!files.length) return;

    const existingKeys = new Set(
      uploadQueue.value.map((item) => `${item.name}-${item.size}-${item.file.lastModified}`),
    );
    const limitedFiles = files.slice(0, Math.max(MAX_UPLOAD_FILES - uploadQueue.value.length, 0));

    let addedCount = 0;
    limitedFiles.forEach((file) => {
      const key = `${file.name}-${file.size}-${file.lastModified}`;
      if (existingKeys.has(key)) return;
      existingKeys.add(key);
      uploadQueue.value.push({
        id: createUploadItemId(file),
        name: file.name,
        size: file.size,
        file,
        status: "pending",
        progress: 0,
      });
      addedCount += 1;
    });

    if (addedCount === 0) {
      customMessage.info("没有可添加的新文件");
      return;
    }

    if (uploadQueue.value.length > MAX_UPLOAD_FILES) {
      uploadQueue.value = uploadQueue.value.slice(0, MAX_UPLOAD_FILES);
    }

    if (files.length > addedCount) {
      customMessage.warning(`单次最多支持 ${MAX_UPLOAD_FILES} 个文件，超出的已忽略`);
    }

    if (!uploadVisible.value) {
      overwriteExisting.value = true;
    }
    uploadVisible.value = true;
  };

  const triggerUpload = () => {
    overwriteExisting.value = true;
    uploadVisible.value = true;
  };

  const handleUploadPick = (event: Event) => {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    appendUploadFiles(Array.from(input.files));
    input.value = "";
  };

  const openUploadPicker = () => {
    if (uploadRunning.value) {
      customMessage.warning("上传任务进行中，请等待完成");
      return;
    }
    uploadInput.value?.click();
  };

  const handleUploadDrop = (event: DragEvent) => {
    event.preventDefault();
    if (!event.dataTransfer?.files?.length) return;
    if (uploadRunning.value) {
      customMessage.warning("上传任务进行中，请等待完成");
      return;
    }
    appendUploadFiles(Array.from(event.dataTransfer.files));
  };

  const clearUploadQueue = () => {
    if (uploadRunning.value) {
      customMessage.warning("上传中不允许清空列表");
      return;
    }
    uploadQueue.value = [];
  };

  const closeUploadModal = () => {
    uploadVisible.value = false;
    uploadQueue.value = [];
    uploadSummary.value = "";
    overwriteExisting.value = true;
  };

  const confirmUploadQueue = async () => {
    if (!uploadQueue.value.length) {
      customMessage.warning("请先选择文件");
      return;
    }

    if (uploadRunning.value) {
      customMessage.warning("文件已在上传队列中");
      return;
    }
    uploadRunning.value = true;
    uploadSummary.value = "";

    try {
      await transferStore.ensureStorageSettingId();
      transferStore.startUploadSession();
      const results = await Promise.allSettled(
        uploadQueue.value.map((item) =>
          transferStore.createTask(
            item.file,
            currentParentId.value,
            undefined,
            overwriteExisting.value,
          ),
        ),
      );
      const successCount = results.filter((result) => result.status === "fulfilled").length;
      const failureCount = results.length - successCount;
      const firstFailure = results.find(
        (result): result is PromiseRejectedResult => result.status === "rejected",
      );
      const failureReason = firstFailure?.reason instanceof Error
        ? firstFailure.reason.message
        : typeof firstFailure?.reason === "string"
          ? firstFailure.reason
          : "";

      if (failureCount === 0) {
        customMessage.success(`已添加 ${successCount} 个文件到上传列表`);
      } else {
        customMessage.warning(
          failureReason
            ? `已添加 ${successCount} 个文件到上传列表，${failureCount} 个文件失败：${failureReason}`
            : `已添加 ${successCount} 个文件到上传列表，${failureCount} 个文件失败`,
        );
      }

      await loadFileData();
    } catch (error) {
      const message = error instanceof Error ? error.message : "上传失败，请检查存储配置后重试";
      customMessage.error(message);
    } finally {
      uploadRunning.value = false;
      closeUploadModal();
    }
  };

  return {
    uploadInput,
    uploadQueue,
    uploadRunning,
    uploadSummary,
    uploadVisible,
    overwriteExisting,
    triggerUpload,
    appendUploadFiles,
    getUploadStatusLabel,
    handleUploadPick,
    openUploadPicker,
    handleUploadDrop,
    clearUploadQueue,
    closeUploadModal,
    confirmUploadQueue,
  };
};

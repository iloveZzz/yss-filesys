import { defineStore } from "pinia";
import { ref } from "vue";
import type {
  FileTransferTaskDTO,
  InitTransferUploadCommand,
  StorageSettingDTO,
} from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { anonymousAccount } from "@/constants/anonymousAccount";
import { customMessage } from "@/utils/message";
import { sseService, type TransferSSEMessage } from "@/services/sse.service";
import { uploadExecutor } from "@/services/upload-executor";
import { pickDefaultStorageSettingId } from "@/utils/storage";

const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();

type TransferTaskStatus =
  | "idle"
  | "initialized"
  | "checking"
  | "uploading"
  | "paused"
  | "merging"
  | "cancelled"
  | "failed"
  | "completed";

export interface TransferTask extends FileTransferTaskDTO {
  taskId: string;
  fileName: string;
  fileSize: number;
  status: TransferTaskStatus;
  progress: number;
  uploadedBytes: number;
  speed: number;
  remainingTime: number;
  createdAt?: number;
  updatedAt?: number;
  parentId?: string;
  chunkSize?: number;
}

const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;
const DEFAULT_CONCURRENCY = 3;
const POLLING_INTERVAL = 3000;
const TRANSFER_STATUS_TYPES = [1, 2, 3] as const;

const TRANSFER_STATE_PRIORITY: Record<TransferTaskStatus, number> = {
  idle: 0,
  initialized: 1,
  checking: 2,
  paused: 3,
  uploading: 4,
  merging: 5,
  cancelled: 6,
  failed: 7,
  completed: 8,
};

let callbacksInitialized = false;
let sseMessageUnsubscribe: (() => void) | null = null;
let sseConnectionUnsubscribe: (() => void) | null = null;
let pollingTimerId: number | null = null;
let beforeUnloadWarningSetup = false;
let hasCheckedUnfinishedTasks = false;

const mergeRemoteTask = (target: Map<string, TransferTask>, vo: FileTransferTaskDTO) => {
  if (!vo.taskId) return;

  const existingTask = target.get(vo.taskId);
  const newTask = convertRemoteTask(vo);

  if (!existingTask) {
    target.set(vo.taskId, newTask);
    return;
  }

  const existingPriority = TRANSFER_STATE_PRIORITY[existingTask.status] || 0;
  const newPriority = TRANSFER_STATE_PRIORITY[newTask.status] || 0;

  if (newPriority > existingPriority || ["completed", "failed", "cancelled"].includes(newTask.status)) {
    target.set(vo.taskId, newTask);
    return;
  }

  target.set(vo.taskId, {
    ...existingTask,
    status: newTask.status,
    uploadedChunks: newTask.uploadedChunks,
    totalChunks: newTask.totalChunks,
    uploadedBytes: newTask.uploadedBytes,
    updatedAt: Date.now(),
  });
};

const loadRemoteTasksByStatus = async (statusType?: number) => {
  const resp = await generatedFilesysApi.listFileTransfersByUser(
    statusType === undefined ? undefined : { statusType },
  );
  return resp.data ?? [];
};

const loadMergedRemoteTasks = async () => {
  const taskGroups = await Promise.all(
    TRANSFER_STATUS_TYPES.map((statusType) => loadRemoteTasksByStatus(statusType)),
  );
  const merged = new Map<string, TransferTask>();
  taskGroups.forEach((remoteTasks) => {
    remoteTasks.forEach((vo) => mergeRemoteTask(merged, vo));
  });
  return merged;
};

const convertRemoteTask = (task: FileTransferTaskDTO): TransferTask => {
  const status = (task.status || "idle").toLowerCase() as TransferTaskStatus;
  const totalChunks = task.totalChunks || 0;
  const uploadedChunks = task.uploadedChunks || 0;
  const progress =
    totalChunks > 0 ? Math.min(100, Math.round((uploadedChunks / totalChunks) * 100)) : 0;

  return {
    ...task,
    taskId: task.taskId || "",
    fileName: task.fileName || "",
    fileSize: task.fileSize || 0,
    status,
    progress,
    uploadedBytes: 0,
    speed: 0,
    remainingTime: 0,
  };
};

export const useTransferStore = defineStore("transfer", () => {
  const tasks = ref(new Map<string, TransferTask>());
  const currentSessionId = ref<string | null>(null);
  const sessionTasks = ref(new Map<string, string[]>());
  const fileCache = ref(new Map<string, File>());
  const completedActionsTriggered = ref(new Set<string>());
  const storageSettings = ref<StorageSettingDTO[]>([]);
  const storageSettingId = ref("");
  const sseConnected = ref(false);
  const loading = ref(false);

  const getTaskList = () => Array.from(tasks.value.values());
  const getUploadingTasks = () =>
    getTaskList().filter((task) =>
      ["idle", "initialized", "checking", "uploading", "paused", "merging"].includes(task.status),
    );
  const getCompletedTasks = () =>
    getTaskList().filter((task) => ["completed", "failed", "cancelled"].includes(task.status));
  const getCurrentSessionTasks = () => {
    if (!currentSessionId.value) return [];
    const taskIds = sessionTasks.value.get(currentSessionId.value) || [];
    return taskIds.map((id) => tasks.value.get(id)).filter((task): task is TransferTask => !!task);
  };

  const ensureCallbacks = () => {
    if (callbacksInitialized) return;
    callbacksInitialized = true;

    uploadExecutor.setCallbacks({
      onStatus: (taskId, status) => {
        const task = tasks.value.get(taskId);
        if (!task) return;
        const statusMap: Record<string, TransferTaskStatus> = {
          checking: "checking",
          uploading: "uploading",
          merging: "merging",
          completed: "completed",
        };
        const next = new Map(tasks.value);
        next.set(taskId, {
          ...task,
          status: statusMap[status] ?? task.status,
        });
        tasks.value = next;
        if (statusMap[status] === "completed") {
          triggerCompletedActions(taskId);
        }
      },
      onProgress: (taskId, payload) => {
        const task = tasks.value.get(taskId);
        if (!task) return;
        const progress =
          payload.totalBytes > 0
            ? Math.min(100, Math.round((payload.uploadedBytes / payload.totalBytes) * 100))
            : task.progress;
        const next = new Map(tasks.value);
        next.set(taskId, {
          ...task,
          uploadedBytes: payload.uploadedBytes,
          progress,
          uploadedChunks: payload.uploadedChunks,
          totalChunks: payload.totalChunks,
        });
        tasks.value = next;
      },
      onError: (taskId, errorMessage) => {
        const task = tasks.value.get(taskId);
        if (!task) return;
        const next = new Map(tasks.value);
        next.set(taskId, {
          ...task,
          status: "failed",
          progress: 0,
          errorMsg: errorMessage,
        });
        tasks.value = next;
        customMessage.error(`文件 "${task.fileName}" 上传失败: ${errorMessage}`);
      },
    });
  };

  const setSseConnected = (connected: boolean) => {
    sseConnected.value = connected;
  };

  const triggerCompletedActions = (taskId: string) => {
    const task = tasks.value.get(taskId);
    if (!task || completedActionsTriggered.value.has(taskId)) return;
    completedActionsTriggered.value.add(taskId);

    window.dispatchEvent(
      new CustomEvent("file-upload-complete", {
        detail: {
          taskId,
          parentId: task.parentId,
        },
      }),
    );

    const newFileCache = new Map(fileCache.value);
    newFileCache.delete(taskId);
    fileCache.value = newFileCache;
  };

  const loadStorageSettings = async () => {
    const resp = await generatedFilesysApi.listStorageSettings();
    storageSettings.value = resp.data ?? [];
    storageSettingId.value = pickDefaultStorageSettingId(storageSettings.value);
    return storageSettingId.value;
  };

  const ensureStorageSettingId = async () => {
    if (!storageSettingId.value) {
      await loadStorageSettings();
    }
    if (!storageSettingId.value) {
      throw new Error("未找到可用的存储配置");
    }
    return storageSettingId.value;
  };

  const startUploadSession = () => {
    const sessionId = `session_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
    currentSessionId.value = sessionId;
    const nextSessionTasks = new Map(sessionTasks.value);
    nextSessionTasks.set(sessionId, []);
    sessionTasks.value = nextSessionTasks;
    return sessionId;
  };

  const transitionTo = (taskId: string, status: TransferTaskStatus) => {
    const task = tasks.value.get(taskId);
    if (!task) return false;
    const next = new Map(tasks.value);
    next.set(taskId, {
      ...task,
      status,
    });
    tasks.value = next;
    checkAndStartPolling();
    if (status === "completed") {
      triggerCompletedActions(taskId);
    }
    return true;
  };

  const updateProgress = (taskId: string, payload: Partial<UploadProgressData>) => {
    const task = tasks.value.get(taskId);
    if (!task) return;
    const next = new Map(tasks.value);
    next.set(taskId, {
      ...task,
      uploadedBytes: payload.uploadedBytes ?? task.uploadedBytes,
      uploadedChunks: payload.uploadedChunks ?? task.uploadedChunks,
      totalChunks: payload.totalChunks ?? task.totalChunks,
      progress:
        payload.totalBytes && payload.totalBytes > 0
          ? Math.min(100, Math.round((payload.uploadedBytes || 0) / payload.totalBytes * 100))
          : task.progress,
    });
    tasks.value = next;
  };

  const handleSSEMessage = (message: TransferSSEMessage) => {
    switch (message.type) {
      case "progress":
        updateProgress(message.taskId, {
          uploadedBytes: message.data.uploadedBytes,
          totalBytes: message.data.totalBytes,
          uploadedChunks: message.data.uploadedChunks,
          totalChunks: message.data.totalChunks,
        });
        break;
      case "status": {
        const statusMap: Record<string, TransferTaskStatus> = {
          idle: "idle",
          initialized: "initialized",
          checking: "checking",
          uploading: "uploading",
          paused: "paused",
          merging: "merging",
          canceled: "cancelled",
          cancelled: "cancelled",
          failed: "failed",
          completed: "completed",
        };
        const nextStatus = statusMap[(message.data.status || "").toLowerCase()] || "idle";
        transitionTo(message.taskId, nextStatus);
        break;
      }
      case "complete":
        transitionTo(message.taskId, "completed");
        break;
      case "error":
        setTaskError(message.taskId, message.data.message);
        break;
      default:
        break;
    }
  };

  const setTaskError = (taskId: string, errorMessage: string) => {
    const task = tasks.value.get(taskId);
    if (!task) return;
    const next = new Map(tasks.value);
    next.set(taskId, {
      ...task,
      status: "failed",
      progress: 0,
      errorMsg: errorMessage,
    });
    tasks.value = next;
  };

  const fetchTasks = async () => {
    loading.value = true;
    try {
      tasks.value = await loadMergedRemoteTasks();
      await checkUnfinishedTasks();
      checkAndStartPolling();
    } catch (error) {
      console.error("获取传输任务列表失败:", error);
    } finally {
      loading.value = false;
    }
  };

  const checkUnfinishedTasks = async () => {
    if (hasCheckedUnfinishedTasks) return;
    hasCheckedUnfinishedTasks = true;

    const unfinishedTasks = Array.from(tasks.value.values()).filter((task) =>
      ["idle", "initialized", "uploading", "checking", "paused", "merging"].includes(task.status),
    );

    if (unfinishedTasks.length === 0) return;

    const results = await Promise.allSettled(
      unfinishedTasks.map(async (task) => {
        try {
          await generatedFilesysApi.cancelFileTransfer(task.taskId);
          transitionTo(task.taskId, "cancelled");
          return { success: true, taskId: task.taskId };
        } catch (error: any) {
          const message = error?.message || error?.response?.data?.message || "";
          if (message.includes("任务不存在")) {
            transitionTo(task.taskId, "cancelled");
            return { success: true, taskId: task.taskId };
          }
          console.error("取消任务失败:", task.taskId, error);
          return { success: false, taskId: task.taskId, error };
        }
      }),
    );

    const successCount = results.filter((r) => r.status === "fulfilled" && r.value.success).length;
    const failCount = results.length - successCount;

    if (successCount > 0) {
      customMessage.info(
        `已自动取消 ${successCount} 个未完成的上传任务${
          failCount > 0 ? `，${failCount} 个任务取消失败` : ""
        }`,
      );
    }
  };

  const syncTasks = async () => {
    loading.value = true;
    try {
      const remoteTasks = await Promise.all(
        TRANSFER_STATUS_TYPES.map((statusType) => loadRemoteTasksByStatus(statusType)),
      );
      const newTasks = new Map(tasks.value);

      remoteTasks.flat().forEach((vo) => {
        mergeRemoteTask(newTasks, vo);
      });

      const backendTaskIds = new Set(remoteTasks.flatMap((group) => group.map((task) => task.taskId)).filter(Boolean));
      Array.from(newTasks.keys()).forEach((taskId) => {
        if (!backendTaskIds.has(taskId)) {
          newTasks.delete(taskId);
        }
      });

      tasks.value = newTasks;
      checkAndStartPolling();
    } catch (error) {
      console.error("同步传输任务失败:", error);
    } finally {
      loading.value = false;
    }
  };

  const createTask = async (
    file: File,
    parentId?: string,
    sessionId?: string,
    overwriteExisting = true,
  ) => {
    ensureCallbacks();
    const storageId = await ensureStorageSettingId();
    const chunkSize = DEFAULT_CHUNK_SIZE;
    const totalChunks = uploadExecutor.calculateChunkCount(file.size, chunkSize);

    const initPayload: InitTransferUploadCommand = {
      chunkSize,
      fileName: file.name,
      fileSize: file.size,
      mimeType: file.type || "application/octet-stream",
      parentId,
      overwriteExisting,
      storageSettingId: storageId,
      totalChunks,
      userId: anonymousAccount.id,
    };

    const initResp = await generatedFilesysApi.initUpload(initPayload);
    const taskData = initResp.data;
    const taskId = taskData?.taskId;
    if (!taskId) {
      throw new Error("未获取到上传任务ID");
    }

    const now = Date.now();
    const task: TransferTask = {
      taskId,
      fileName: file.name,
      fileSize: file.size,
      status: "initialized",
      progress: 0,
      uploadedBytes: 0,
      speed: 0,
      remainingTime: 0,
      createdAt: now,
      updatedAt: now,
      parentId,
      totalChunks,
      uploadedChunks: 0,
      chunkSize,
      taskType: "upload",
      userId: anonymousAccount.id,
    };

    const nextTasks = new Map(tasks.value);
    nextTasks.set(taskId, task);
    tasks.value = nextTasks;

    const nextCache = new Map(fileCache.value);
    nextCache.set(taskId, file);
    fileCache.value = nextCache;

    const activeSessionId = sessionId || currentSessionId.value;
    if (activeSessionId) {
      const nextSessionTasks = new Map(sessionTasks.value);
      const sessionTaskList = nextSessionTasks.get(activeSessionId) || [];
      nextSessionTasks.set(activeSessionId, [...sessionTaskList, taskId]);
      sessionTasks.value = nextSessionTasks;
    }

    void uploadExecutor.start(taskId, file, chunkSize, DEFAULT_CONCURRENCY).catch((error) => {
      setTaskError(taskId, error instanceof Error ? error.message : "上传失败");
    });

    return taskId;
  };

  const initSSE = async (userId = anonymousAccount.id) => {
    try {
      ensureCallbacks();
      await fetchTasks();

      sseService.setReconnectSyncCallback(async () => {
        await syncTasks();
      });

      if (sseMessageUnsubscribe) {
        sseMessageUnsubscribe();
      }
      sseMessageUnsubscribe = sseService.onMessage(handleSSEMessage);

      if (sseConnectionUnsubscribe) {
        sseConnectionUnsubscribe();
      }
      sseConnectionUnsubscribe = sseService.onConnectionChange((connected) => {
        setSseConnected(connected);
      });

      sseService.connect(userId);

      checkAndStartPolling();
      setupBeforeUnloadWarning();
    } catch (error) {
      console.error("初始化 SSE 失败:", error);
    }
  };

  const disconnectSSE = () => {
    if (sseMessageUnsubscribe) {
      sseMessageUnsubscribe();
      sseMessageUnsubscribe = null;
    }

    if (sseConnectionUnsubscribe) {
      sseConnectionUnsubscribe();
      sseConnectionUnsubscribe = null;
    }

    sseService.disconnect();
    setSseConnected(false);
    stopPolling();
  };

  const checkAndStartPolling = () => {
    const hasActiveTasks = Array.from(tasks.value.values()).some((task) =>
      ["uploading", "checking", "merging"].includes(task.status),
    );

    if (hasActiveTasks && pollingTimerId === null) {
      startPolling();
    }
  };

  const startPolling = () => {
    if (pollingTimerId !== null) return;

    pollingTimerId = window.setInterval(async () => {
      const activeTasks = Array.from(tasks.value.values()).filter((task) =>
        ["uploading", "checking", "merging"].includes(task.status),
      );

      if (activeTasks.length === 0) {
        stopPolling();
        return;
      }

      try {
        await syncTasks();
      } catch {
        // Silent
      }
    }, POLLING_INTERVAL);
  };

  const stopPolling = () => {
    if (pollingTimerId !== null) {
      window.clearInterval(pollingTimerId);
      pollingTimerId = null;
    }
  };

  const setupBeforeUnloadWarning = () => {
    if (beforeUnloadWarningSetup) return;
    beforeUnloadWarningSetup = true;

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      const hasUploadingTasks = Array.from(tasks.value.values()).some((task) =>
        ["idle", "initialized", "uploading", "checking", "merging", "paused"].includes(
          task.status,
        ),
      );

      if (hasUploadingTasks) {
        event.preventDefault();
        event.returnValue = "有文件正在上传，离开页面将取消所有上传任务";
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
  };

  const pauseTask = async (taskId: string) => {
    await generatedFilesysApi.pause(taskId);
    transitionTo(taskId, "paused");
  };

  const resumeTask = async (taskId: string) => {
    await generatedFilesysApi.resume(taskId);
    transitionTo(taskId, "uploading");
  };

  const cancelTask = async (taskId: string) => {
    await generatedFilesysApi.cancelFileTransfer(taskId);
    transitionTo(taskId, "cancelled");
  };

  const clearCompletedTasks = async () => {
    await generatedFilesysApi.clearFinishedFileTransfers();
    const nextTasks = new Map(tasks.value);
    Array.from(nextTasks.entries()).forEach(([taskId, task]) => {
      if (task.status === "completed" || task.status === "failed" || task.status === "cancelled") {
        nextTasks.delete(taskId);
        completedActionsTriggered.value.delete(taskId);
      }
    });
    tasks.value = nextTasks;
  };

  const getDisplayData = (taskId: string) => {
    const task = tasks.value.get(taskId);
    if (!task) {
      return { progress: 0, speed: 0, remainingTime: 0 };
    }
    return {
      progress: task.progress,
      speed: task.speed,
      remainingTime: task.remainingTime,
    };
  };

  return {
    tasks,
    currentSessionId,
    sessionTasks,
    fileCache,
    completedActionsTriggered,
    storageSettings,
    storageSettingId,
    sseConnected,
    loading,
    getTaskList,
    getUploadingTasks,
    getCompletedTasks,
    getCurrentSessionTasks,
    setSseConnected,
    startUploadSession,
    transitionTo,
    updateProgress,
    setTaskError,
    handleSSEMessage,
    fetchTasks,
    checkUnfinishedTasks,
    createTask,
    initSSE,
    disconnectSSE,
    checkAndStartPolling,
    startPolling,
    stopPolling,
    setupBeforeUnloadWarning,
    syncTasks,
    pauseTask,
    resumeTask,
    cancelTask,
    clearCompletedTasks,
    loadStorageSettings,
    ensureStorageSettingId,
    getDisplayData,
  };
});

type UploadProgressData = {
  uploadedBytes?: number;
  totalBytes?: number;
  uploadedChunks?: number;
  totalChunks?: number;
};

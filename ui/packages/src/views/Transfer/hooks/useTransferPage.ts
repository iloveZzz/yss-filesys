import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import type { FileTransferTaskDTO } from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { customMessage } from "@/utils/message";
import { useTransferStore } from "@/store";

type FilterKey = "upload" | "download" | "done";

export const useTransferPage = () => {
  const transferStore = useTransferStore();
  const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();
  const filter = ref<FilterKey>("upload");
  const keyword = ref("");
  const completedTasks = ref<FileTransferTaskDTO[]>([]);
  const completedPageIndex = ref(0);
  const completedPageSize = ref(10);
  const completedTotal = ref(0);
  const completedLoading = ref(false);

  const loadTasks = async () => {
    await transferStore.syncTasks();
    if (isDoneFilter.value) {
      await loadCompletedTasks();
    }
  };

  const loadCompletedTasks = async () => {
    completedLoading.value = true;
    try {
      const resp = await generatedFilesysApi.pageFileTransfersByUser({
        statusType: 3,
        keyword: keyword.value.trim() || undefined,
        pageIndex: completedPageIndex.value,
        pageSize: completedPageSize.value,
      });
      completedTasks.value = resp.data ?? [];
      completedTotal.value = resp.totalCount ?? 0;
    } finally {
      completedLoading.value = false;
    }
  };

  const tasks = computed(() => transferStore.getTaskList());
  const loading = computed(() => transferStore.loading);

  const statusTextMap: Record<string, string> = {
    uploading: "上传中",
    downloading: "下载中",
    completed: "已完成",
    success: "已完成",
    failed: "失败",
    cancel: "已取消",
    cancelled: "已取消",
    waiting: "排队中",
    pending: "排队中",
    initialized: "已初始化",
    checking: "校验中",
    merging: "合并中",
  };

  const statusColorMap: Record<string, string> = {
    uploading: "#6366f1",
    downloading: "#6366f1",
    completed: "#10b981",
    success: "#10b981",
    failed: "#ef4444",
    cancel: "#64748b",
    cancelled: "#64748b",
    waiting: "#f59e0b",
    pending: "#f59e0b",
    initialized: "#6366f1",
    checking: "#0ea5e9",
    merging: "#8b5cf6",
  };

  const normalizeStatus = (task: FileTransferTaskDTO) => (task.status || "").toLowerCase();

  const isFinished = (task: FileTransferTaskDTO) =>
    ["completed", "done", "success", "failed", "fail", "cancelled", "cancel"].includes(
      normalizeStatus(task),
    );
  const isActive = (task: FileTransferTaskDTO) => !isFinished(task);
  const isFailed = (task: FileTransferTaskDTO) => ["failed", "fail"].includes(normalizeStatus(task));

  const getPercent = (task: FileTransferTaskDTO) => {
    const total = task.totalChunks || 0;
    const done = task.uploadedChunks || 0;
    if (!total) return isFinished(task) ? 100 : 0;
    return Math.min(100, Math.round((done / total) * 100));
  };

  const getSpeed = (task: FileTransferTaskDTO) => {
    const percent = getPercent(task);
    if (isFinished(task)) return "完成";
    if (isFailed(task)) return "已失败";
    if (task.taskType === "download") return `${percent}%`;
    return `${percent}%`;
  };

  const getRemainingText = (task: FileTransferTaskDTO) => {
    if (isFinished(task)) return "已完成";
    if (isFailed(task)) return task.errorMsg || "传输失败";
    const remaining = Math.max((task.totalChunks || 0) - (task.uploadedChunks || 0), 0);
    return `剩余 ${remaining} 个分片`;
  };

  const filteredTasks = computed(() => {
    const nameKeyword = keyword.value.trim().toLowerCase();
    const byKeyword = (item: FileTransferTaskDTO) =>
      !nameKeyword || (item.fileName || "").toLowerCase().includes(nameKeyword);

    const byFilter = (item: FileTransferTaskDTO) => {
      if (filter.value === "upload") return (item.taskType || "upload") === "upload" && isActive(item);
      if (filter.value === "download") return item.taskType === "download" && isActive(item);
      if (filter.value === "done") return isFinished(item);
      return true;
    };

    return tasks.value.filter((item) => byFilter(item) && byKeyword(item));
  });

  const visibleTasks = computed(() => (isDoneFilter.value ? completedTasks.value : filteredTasks.value));
  const visibleLoading = computed(() => (isDoneFilter.value ? completedLoading.value : loading.value));
  const visibleTotalCount = computed(() => (isDoneFilter.value ? completedTotal.value : filteredTasks.value.length));

  const summary = computed(() => {
    const upload = tasks.value.filter(
      (item) => (item.taskType || "upload") === "upload" && isActive(item),
    ).length;
    const download = tasks.value.filter((item) => item.taskType === "download" && isActive(item)).length;
    const done = tasks.value.filter((item) => isFinished(item)).length;
    return [
      { key: "upload" as const, label: "上传中", value: upload },
      { key: "download" as const, label: "下载中", value: download },
      { key: "done" as const, label: "已完成", value: done },
    ];
  });

  const isDoneFilter = computed(() => filter.value === "done");

  const tabs = computed(() => [
    { key: "upload" as const, label: `上传中 (${summary.value[0]?.value ?? 0})` },
    { key: "download" as const, label: `下载中 (${summary.value[1]?.value ?? 0})` },
    { key: "done" as const, label: `已完成 (${summary.value[2]?.value ?? 0})` },
  ]);

  const cancelTask = async (taskId?: string) => {
    if (!taskId) return;
    await transferStore.cancelTask(taskId);
    customMessage.success("任务已取消");
    await loadTasks();
  };

  const pauseTask = async (taskId?: string) => {
    if (!taskId) return;
    await transferStore.pauseTask(taskId);
    customMessage.success("任务已暂停");
    await loadTasks();
  };

  const clearAllTasks = async () => {
    await transferStore.clearCompletedTasks();
    customMessage.success("已清空");
    if (isDoneFilter.value) {
      completedPageIndex.value = 0;
      await loadCompletedTasks();
      return;
    }
    await loadTasks();
  };

  const getCompletedTime = (task: FileTransferTaskDTO) =>
    task.completeTime || task.startTime || "-";

  const onSearch = async () => {
    keyword.value = keyword.value.trim();
    if (isDoneFilter.value) {
      completedPageIndex.value = 0;
      await loadCompletedTasks();
    }
  };

  const handleFilterChange = async (nextFilter: FilterKey) => {
    if (filter.value === nextFilter) return;
    filter.value = nextFilter;
    if (nextFilter === "done") {
      completedPageIndex.value = 0;
      await loadCompletedTasks();
      return;
    }
    await loadTasks();
  };

  const handleCompletedTableChange = async (pagination: { current?: number; pageSize?: number }) => {
    completedPageIndex.value = Math.max((pagination.current || 1) - 1, 0);
    completedPageSize.value = pagination.pageSize || 10;
    await loadCompletedTasks();
  };

  onMounted(async () => {
    await transferStore.initSSE();
  });

  onBeforeUnmount(() => {
    transferStore.disconnectSSE();
  });

  return {
    transferStore,
    filter,
    keyword,
    loadTasks,
    loadCompletedTasks,
    tasks,
    loading,
    visibleTasks,
    visibleLoading,
    visibleTotalCount,
    statusTextMap,
    statusColorMap,
    normalizeStatus,
    isFinished,
    isActive,
    isFailed,
    getPercent,
    getSpeed,
    getRemainingText,
    filteredTasks,
    summary,
    isDoneFilter,
    tabs,
    cancelTask,
    pauseTask,
    clearAllTasks,
    getCompletedTime,
    onSearch,
    handleFilterChange,
    handleCompletedTableChange,
    completedPageIndex,
    completedPageSize,
    completedTotal,
  };
};

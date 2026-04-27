import { computed, onMounted, ref } from "vue";
import { useRequest } from "vue-hooks-plus";
import type {
  FileHomeDTO,
  FileTransferTaskDTO,
} from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { useCapacityStore } from "@/store";
import { formatBytes, formatDateTime } from "@/utils/format";

const generatedFilesysApi =
  getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();

export const useDashboardPage = () => {
  const home = ref<FileHomeDTO>({});
  const recentCompletedFiles = ref<FileTransferTaskDTO[]>([]);
  const recentCompletedPageIndex = ref(0);
  const recentCompletedPageSize = ref(5);
  const recentCompletedTotal = ref(0);
  const recentCompletedLoading = ref(false);
  const capacityStore = useCapacityStore();

  const { runAsync: runLoadHome } = useRequest(
    async () => generatedFilesysApi.getFileHomeStats(),
    {
      manual: true,
    },
  );

  const loadHome = async () => {
    const resp = await runLoadHome();
    home.value = resp.data ?? {};
  };

  const loadRecentCompletedFiles = async () => {
    recentCompletedLoading.value = true;
    try {
      const resp = await generatedFilesysApi.pageFileTransfersByUser({
        statusType: 3,
        pageIndex: recentCompletedPageIndex.value,
        pageSize: recentCompletedPageSize.value,
      });
      recentCompletedFiles.value = resp.data ?? [];
      recentCompletedTotal.value = resp.totalCount ?? 0;
    } finally {
      recentCompletedLoading.value = false;
    }
  };

  const handleRecentCompletedPageChange = async (page: number, pageSize: number) => {
    recentCompletedPageIndex.value = Math.max(page - 1, 0);
    recentCompletedPageSize.value = pageSize;
    await loadRecentCompletedFiles();
  };

  onMounted(() => {
    void capacityStore.loadCapacity();
    void loadHome();
    void loadRecentCompletedFiles();
  });

  const usageBuckets = computed(() => {
    const fallback = [
      { label: "文档", usedBytes: 0 },
      { label: "图片", usedBytes: 0 },
      { label: "视频", usedBytes: 0 },
      { label: "音频", usedBytes: 0 },
      { label: "其他", usedBytes: 0 },
    ];
    const buckets = (home.value.usedBytes ?? []).slice(0, 5);
    return fallback.map((item, index) => buckets[index] ?? item);
  });

  const capacityPercent = computed(() => capacityStore.percent);
  const capacitySummary = computed(() => {
    const capacity = capacityStore.capacity;
    if (!capacity) return "未获取到容量信息";
    return `当前存储池已使用 ${capacityStore.formatBytes(capacity.usedBytes)} / ${capacityStore.formatBytes(capacity.totalBytes)}。`;
  });

  const chartOption = computed(() => ({
    tooltip: {
      trigger: "axis",
      backgroundColor: "#fff",
      borderColor: "rgba(15, 23, 42, 0.08)",
      borderWidth: 1,
      textStyle: {
        color: "#0f172a",
      },
      extraCssText:
        "box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12); border-radius: 8px;",
    },
    grid: {
      left: 8,
      right: 8,
      top: 24,
      bottom: 0,
      containLabel: true,
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: usageBuckets.value.map((item) => item.label),
      axisLine: { lineStyle: { color: "rgba(15, 23, 42, 0.12)" } },
      axisTick: { show: false },
      axisLabel: { color: "rgba(15, 23, 42, 0.55)" },
    },
    yAxis: {
      type: "value",
      splitLine: {
        lineStyle: {
          color: "rgba(15, 23, 42, 0.06)",
        },
      },
      axisLabel: {
        color: "rgba(15, 23, 42, 0.45)",
        formatter: (value: number) => formatBytes(value),
      },
    },
    series: [
      {
        type: "line",
        data: usageBuckets.value.map((item) => item.usedBytes ?? 0),
        smooth: true,
        symbol: "circle",
        symbolSize: 7,
        lineStyle: {
          width: 2.5,
          color: "#1677ff",
        },
        itemStyle: {
          color: "#1677ff",
        },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(22, 119, 255, 0.22)" },
              { offset: 1, color: "rgba(22, 119, 255, 0.03)" },
            ],
          },
        },
      },
    ],
  }));

  return {
    home,
    recentCompletedFiles,
    recentCompletedPageIndex,
    recentCompletedPageSize,
    recentCompletedTotal,
    recentCompletedLoading,
    handleRecentCompletedPageChange,
    capacityStore,
    capacityPercent,
    capacitySummary,
    chartOption,
    formatBytes,
    formatDateTime,
  };
};

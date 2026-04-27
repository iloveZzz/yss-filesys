import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import type {
  FileShareAccessRecordDTO,
  FileShareDTO,
  PageFileSharesByUserParams,
} from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { cancelFileSharesByIds } from "@/api/filesys-commands";
import { customMessage } from "@/utils/message";
import { formatDateTime } from "@/utils/format";

const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();

export type SharePageQuery = PageFileSharesByUserParams & { keyword?: string };
export type ShareMenuAction = "copy" | "detail" | "records" | "cancel";

export const useSharePage = () => {
  const router = useRouter();

  const loading = ref(false);
  const shares = ref<FileShareDTO[]>([]);
  const sharesTotal = ref(0);
  const sharesPageIndex = ref(0);
  const sharesPageSize = ref(10);
  const searchKeyword = ref("");
  const selectedRowKeys = ref<string[]>([]);

  const detailVisible = ref(false);
  const detailLoading = ref(false);
  const currentShare = ref<FileShareDTO | null>(null);
  const accessRecords = ref<FileShareAccessRecordDTO[]>([]);

  const cancelConfirmVisible = ref(false);
  const cancelLoading = ref(false);
  const cancelTargets = ref<FileShareDTO[]>([]);

  const getShareUrl = (share: FileShareDTO) => {
    if (!share.shareId) return "";
    return `${window.location.origin}/s/${share.shareId}`;
  };

  const formatShareScope = (scope?: string) => {
    const normalized = (scope || "").toLowerCase();
    if (normalized.includes("download")) return "下载";
    if (normalized.includes("private")) return "私密";
    return "预览";
  };

  const scopeTagColor = (scope?: string) => {
    const normalized = (scope || "").toLowerCase();
    if (normalized.includes("download")) return "blue";
    if (normalized.includes("private")) return "gold";
    return "default";
  };

  const selectedShares = computed(() =>
    shares.value.filter((item) => item.shareId && selectedRowKeys.value.includes(item.shareId)),
  );

  const loadShares = async () => {
    loading.value = true;
    try {
      const params: SharePageQuery = {
        pageIndex: sharesPageIndex.value,
        pageSize: sharesPageSize.value,
        keyword: searchKeyword.value.trim() || undefined,
      };
      const resp = await generatedFilesysApi.pageFileSharesByUser(params);
      const page = resp;
      shares.value = page.data ?? [];
      sharesTotal.value = page.totalCount ?? 0;
      sharesPageIndex.value = page.pageIndex ?? 0;
      sharesPageSize.value = page.pageSize ?? 10;
    } finally {
      loading.value = false;
    }
  };

  const refresh = async () => {
    await loadShares();
  };

  const commitSearch = async () => {
    sharesPageIndex.value = 0;
    selectedRowKeys.value = [];
    await loadShares();
  };

  const handleTableChange = async (pagination: { current?: number; pageSize?: number }) => {
    sharesPageIndex.value = Math.max((pagination.current || 1) - 1, 0);
    sharesPageSize.value = pagination.pageSize || 10;
    await loadShares();
  };

  const clearAllShares = async () => {
    if (!sharesTotal.value) {
      customMessage.info("当前没有可清空的分享");
      return;
    }
    await generatedFilesysApi.clearAllFileShares();
    customMessage.success("已清空所有分享");
    selectedRowKeys.value = [];
    await loadShares();
  };

  const openShareDetail = async (record: FileShareDTO) => {
    if (!record.shareId) return;
    detailVisible.value = true;
    detailLoading.value = true;
    currentShare.value = record;
    try {
      const [detailResp, accessResp] = await Promise.all([
        generatedFilesysApi.getFileShareById(record.shareId),
        generatedFilesysApi.listAccessRecords(record.shareId),
      ]);
      currentShare.value = detailResp.data ?? record;
      accessRecords.value = accessResp.data ?? [];
    } finally {
      detailLoading.value = false;
    }
  };

  const openAccessRecords = async (record: FileShareDTO) => {
    await openShareDetail(record);
  };

  const openPublicPage = (shareId?: string) => {
    if (!shareId) return;
    const url = router.resolve({ path: `/s/${shareId}` }).href;
    window.open(url, "_blank", "noopener,noreferrer");
  };

  const handleQuickCopy = async (share: FileShareDTO) => {
    const content: string[] = [];
    if (share.shareName) content.push(`分享名称: ${share.shareName}`);

    const shareUrl = getShareUrl(share);
    if (shareUrl) content.push(`分享链接: ${shareUrl}`);

    if (share.shareCode) content.push(`提取码: ${share.shareCode}`);

    await navigator.clipboard.writeText(content.join("\n"));
    customMessage.success("已复制");
  };

  const openCancelConfirm = (records: FileShareDTO[]) => {
    const targets = records.filter((item) => !!item.shareId);
    if (!targets.length) {
      customMessage.warning("请先选择分享");
      return;
    }
    cancelTargets.value = targets;
    cancelConfirmVisible.value = true;
  };

  const confirmCancelShare = async () => {
    const shareIds = cancelTargets.value.map((item) => item.shareId).filter(Boolean) as string[];
    if (!shareIds.length) return;
    cancelLoading.value = true;
    try {
      await cancelFileSharesByIds(shareIds);
      customMessage.success("已取消分享");
      cancelConfirmVisible.value = false;
      cancelTargets.value = [];
      selectedRowKeys.value = selectedRowKeys.value.filter((key) => !shareIds.includes(key));
      await loadShares();
    } finally {
      cancelLoading.value = false;
    }
  };

  const handleShareMenu = async (action: ShareMenuAction, record: FileShareDTO) => {
    switch (action) {
      case "copy":
        await handleQuickCopy(record);
        break;
      case "detail":
        await openShareDetail(record);
        break;
      case "records":
        await openAccessRecords(record);
        break;
      case "cancel":
        openCancelConfirm([record]);
        break;
    }
  };

  const batchCancelSelected = () => {
    openCancelConfirm(selectedShares.value);
  };

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: (string | number)[]) => {
      selectedRowKeys.value = keys.map(String);
    },
    getCheckboxProps: (record: FileShareDTO) => ({
      disabled: !record.shareId,
    }),
  }));

  const batchClearSelection = () => {
    selectedRowKeys.value = [];
  };

  onMounted(async () => {
    await loadShares();
  });

  return {
    router,
    loading,
    shares,
    sharesTotal,
    sharesPageIndex,
    sharesPageSize,
    searchKeyword,
    selectedRowKeys,
    detailVisible,
    detailLoading,
    currentShare,
    accessRecords,
    cancelConfirmVisible,
    cancelLoading,
    cancelTargets,
    selectedShares,
    loadShares,
    refresh,
    commitSearch,
    handleTableChange,
    clearAllShares,
    openShareDetail,
    openAccessRecords,
    openPublicPage,
    getShareUrl,
    handleQuickCopy,
    openCancelConfirm,
    confirmCancelShare,
    handleShareMenu,
    batchCancelSelected,
    rowSelection,
    batchClearSelection,
    formatShareScope,
    scopeTagColor,
    formatDateTime,
  };
};

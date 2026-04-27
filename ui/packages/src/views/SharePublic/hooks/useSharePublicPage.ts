import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type {
  FileRecordDTO,
  FileShareThinDTO,
  VerifyShareCodeCommand,
} from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { customMessage } from "@/utils/message";

const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();

export const useSharePublicPage = () => {
  const route = useRoute();
  const loading = ref(false);
  const shareInfo = ref<FileShareThinDTO | null>(null);
  const shareFiles = ref<FileRecordDTO[]>([]);
  const accessRecords = ref<any[]>([]);
  const inputShareCode = ref("");
  const verified = ref(false);
  const shareId = computed(() => String(route.params.shareId || ""));

  const loadFiles = async () => {
    if (!shareId.value) return;
    const [filesResp, accessResp] = await Promise.all([
      generatedFilesysApi.listShareFiles(shareId.value, {
        shareCode: inputShareCode.value || undefined,
      }),
      generatedFilesysApi.listAccessRecords(shareId.value),
    ]);
    shareFiles.value = filesResp.data ?? [];
    accessRecords.value = accessResp.data ?? [];
  };

  const loadShare = async () => {
    if (!shareId.value) return;
    loading.value = true;
    try {
      const shareResp = await generatedFilesysApi.getShareInfo(shareId.value);
      shareInfo.value = shareResp.data ?? null;
      if (!shareInfo.value?.hasCheckCode || verified.value || inputShareCode.value) {
        await loadFiles();
      }
    } finally {
      loading.value = false;
    }
  };

  const verifyCode = async () => {
    if (!shareId.value || !inputShareCode.value) {
      customMessage.warning("请输入提取码");
      return;
    }
    const payload: VerifyShareCodeCommand = {
      shareId: shareId.value,
      shareCode: inputShareCode.value,
    };
    const resp = await generatedFilesysApi.verifyShareCode(shareId.value, payload);
    if (resp.data) {
      verified.value = true;
      await generatedFilesysApi.recordAccess(shareId.value, {
        shareId: shareId.value,
      });
      await loadFiles();
    } else {
      customMessage.error(resp.message || "提取码验证失败");
    }
  };

  onMounted(async () => {
    await loadShare();
  });

  watch(shareId, async (value) => {
    if (value) await loadShare();
  });

  return {
    loading,
    shareInfo,
    shareFiles,
    accessRecords,
    inputShareCode,
    verified,
    shareId,
    loadShare,
    loadFiles,
    verifyCode,
  };
};

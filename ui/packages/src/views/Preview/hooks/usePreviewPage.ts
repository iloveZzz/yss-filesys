import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { FilePreviewDTO } from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";

const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();

export const usePreviewPage = () => {
  const route = useRoute();
  const loading = ref(false);
  const previewData = ref<FilePreviewDTO>({} as FilePreviewDTO);

  const fileId = computed(() => String(route.params.fileId || ""));

  const loadPreview = async () => {
    if (!fileId.value) return;
    loading.value = true;
    try {
      const tokenResp = await generatedFilesysApi.issueFilePreviewToken(fileId.value);
      const token = tokenResp.data;
      if (!token) {
        previewData.value = {} as FilePreviewDTO;
        return;
      }
      const resp = await generatedFilesysApi.getFilePreviewInfo(fileId.value, {
        previewToken: token,
      });
      previewData.value = resp.data ?? ({} as FilePreviewDTO);
    } finally {
      loading.value = false;
    }
  };

  watch(fileId, async (value) => {
    if (value) await loadPreview();
  });

  onMounted(async () => {
    await loadPreview();
  });

  return {
    loading,
    previewData,
    fileId,
    loadPreview,
  };
};

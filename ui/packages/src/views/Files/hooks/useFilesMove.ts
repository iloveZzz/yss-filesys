import { ref, type Ref } from "vue";
import type { FileRecordDTO } from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { anonymousAccount } from "@/constants/anonymousAccount";
import { customMessage } from "@/utils/message";

interface UseFilesMoveOptions {
  contextMenuTarget: Ref<FileRecordDTO | null>;
  selectedRowKeys: Ref<string[]>;
  closeContextMenu: () => void;
  loadFileData: () => Promise<void>;
}

export const useFilesMove = ({
  contextMenuTarget,
  selectedRowKeys,
  closeContextMenu,
  loadFileData,
}: UseFilesMoveOptions) => {
  const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();
  const moveVisible = ref(false);
  const moveLoading = ref(false);
  const moveTargets = ref<FileRecordDTO[]>([]);
  const moveFolders = ref<FileRecordDTO[]>([]);
  const moveBreadcrumb = ref<FileRecordDTO[]>([]);
  const moveCurrentParentId = ref<string | undefined>(undefined);
  const moveSelectedFolderId = ref<string | undefined>(undefined);
  const moveLoadingFolders = ref(false);
  const moveFolderPathCache = ref(new Map<string, string[]>());

  const getPathFileIds = async (fileId: string) => {
    const cached = moveFolderPathCache.value.get(fileId);
    if (cached) return cached;
    const resp = await generatedFilesysApi.getDirectoryTreePath(fileId);
    const pathIds = (resp.data ?? [])
      .map((item) => item.fileId)
      .filter((id): id is string => !!id);
    const nextCache = new Map(moveFolderPathCache.value);
    nextCache.set(fileId, pathIds);
    moveFolderPathCache.value = nextCache;
    return pathIds;
  };

  const getSelectedFolderIds = () =>
    moveTargets.value
      .filter((item) => item.isDir && !!item.fileId)
      .map((item) => item.fileId as string);

  const loadMoveFolders = async (parentId?: string) => {
    moveLoadingFolders.value = true;
    try {
      const [fileResp, pathResp] = await Promise.all([
        generatedFilesysApi.search({
          parentId,
          deleted: false,
          favorite: false,
          pageIndex: 0,
          pageSize: 100,
        }),
        parentId ? generatedFilesysApi.getDirectoryTreePath(parentId) : Promise.resolve(null),
      ]);
      const selectedFolderIds = new Set(getSelectedFolderIds());
      const currentBreadcrumbIds = (pathResp?.data ?? [])
        .map((item) => item.fileId)
        .filter((id): id is string => !!id);

      const folderCandidates = (fileResp.data ?? []).filter((item) => item.isDir);
      const visibleFolders = await Promise.all(
        folderCandidates.map(async (folder) => {
          if (!folder.fileId) return null;
          if (selectedFolderIds.has(folder.fileId)) return null;

          const pathIds = await getPathFileIds(folder.fileId);
          const isInsideSelectedFolder = pathIds.some((id) => selectedFolderIds.has(id));
          if (isInsideSelectedFolder) return null;

          const isCurrentSelectedAncestor = currentBreadcrumbIds.some((id) => selectedFolderIds.has(id));
          if (isCurrentSelectedAncestor) return null;

          return folder;
        }),
      );

      moveFolders.value = visibleFolders.filter((item): item is FileRecordDTO => !!item);
      moveBreadcrumb.value = pathResp?.data ?? [];
    } finally {
      moveLoadingFolders.value = false;
    }
  };

  const openMoveModal = (record?: FileRecordDTO) => {
    const target = record ?? contextMenuTarget.value;
    if (!target?.fileId) return;
    moveTargets.value = [target];
    moveCurrentParentId.value = undefined;
    moveSelectedFolderId.value = undefined;
    moveFolderPathCache.value = new Map();
    moveVisible.value = true;
    void loadMoveFolders(undefined);
    closeContextMenu();
  };

  const openBatchMoveModal = (records: FileRecordDTO[]) => {
    if (!records.length) return;
    moveTargets.value = records;
    moveCurrentParentId.value = undefined;
    moveSelectedFolderId.value = undefined;
    moveFolderPathCache.value = new Map();
    moveVisible.value = true;
    void loadMoveFolders(undefined);
  };

  const enterMoveFolder = async (folder: FileRecordDTO) => {
    if (!folder.fileId || !folder.isDir) return;
    moveCurrentParentId.value = folder.fileId;
    moveSelectedFolderId.value = undefined;
    await loadMoveFolders(folder.fileId);
  };

  const navigateMoveBreadcrumb = async (index: number) => {
    if (index === -1) {
      moveCurrentParentId.value = undefined;
      moveSelectedFolderId.value = undefined;
      await loadMoveFolders(undefined);
      return;
    }
    const target = moveBreadcrumb.value[index];
    if (!target?.fileId) return;
    moveCurrentParentId.value = target.fileId;
    moveSelectedFolderId.value = undefined;
    await loadMoveFolders(target.fileId);
  };

  const confirmMove = async () => {
    const targetDirId = moveSelectedFolderId.value || moveCurrentParentId.value;
    if (!moveTargets.value.length) return;
    const sameDirectory = moveTargets.value.every((file) => (file.parentId || undefined) === targetDirId);
    if (sameDirectory) {
      customMessage.warning("文件已在当前目录，无需移动");
      return;
    }
    moveLoading.value = true;
    try {
      await Promise.all(
        moveTargets.value.map((file) =>
          generatedFilesysApi.moveFile({
            fileId: file.fileId as string,
            targetParentId: targetDirId,
            userId: anonymousAccount.id,
          }),
        ),
      );
      customMessage.success("移动成功");
      moveVisible.value = false;
      moveTargets.value = [];
      moveSelectedFolderId.value = undefined;
      moveCurrentParentId.value = undefined;
      moveBreadcrumb.value = [];
      moveFolders.value = [];
      selectedRowKeys.value = [];
      await loadFileData();
    } finally {
      moveLoading.value = false;
    }
  };

  return {
    moveVisible,
    moveLoading,
    moveTargets,
    moveFolders,
    moveBreadcrumb,
    moveCurrentParentId,
    moveSelectedFolderId,
    moveLoadingFolders,
    moveFolderPathCache,
    getPathFileIds,
    getSelectedFolderIds,
    loadMoveFolders,
    openMoveModal,
    openBatchMoveModal,
    enterMoveFolder,
    navigateMoveBreadcrumb,
    confirmMove,
  };
};

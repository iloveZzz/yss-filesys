<script setup lang="ts">
import Toolbar from "./components/Toolbar.vue";
import ContextMenu from "./components/ContextMenu.vue";
import DeleteDialog from "./components/DeleteDialog.vue";
import DetailDrawer from "./components/DetailDrawer.vue";
import FolderDialog from "./components/FolderDialog.vue";
import MoveDialog from "./components/MoveDialog.vue";
import RenameDialog from "./components/RenameDialog.vue";
import ShareDialog from "./components/ShareDialog.vue";
import UploadDialog from "./components/UploadDialog.vue";
import Workspace from "./components/Workspace.vue";
import { useFilesPage } from "./hooks/useFilesPage";

const {
  mode,
  recycleFiles,
  breadcrumb,
  storageSettings,
  selectedRowKeys,
  loading,
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
  uploadVisible,
  renameVisible,
  deleteDialogVisible,
  deleteActionLoading,
  deleteTarget,
  uploadInput,
  uploadQueue,
  uploadRunning,
  uploadSummary,
  overwriteExisting,
  moveVisible,
  moveLoading,
  moveFolders,
  moveBreadcrumb,
  moveCurrentParentId,
  moveSelectedFolderId,
  moveLoadingFolders,
  shareVisible,
  shareStage,
  shareLoading,
  shareCreated,
  shareFiles,
  shareForm,
  createForm,
  renameForm,
  contextMenuVisible,
  contextMenuX,
  contextMenuY,
  contextMenuTarget,
  currentParentId,
  tableData,
  selectedRows,
  recycleSelectedRows,
  selectedCount,
  recycleTableLoading,
  rowKey,
  getFileLabel,
  getFileExt,
  getFileTone,
  getUploadStatusLabel,
  getSharePreviewInitial,
  getSharePreviewTone,
  shareSelectedFileCount,
  shareSelectedFilePreview,
  sharePreviewSingle,
  shareUrl,
  shareExpireText,
  handleFileTableChange,
  handleSortChange,
  reload,
  openDetail,
  openContextMenu,
  closeContextMenu,
  openMoveModal,
  openBatchMoveModal,
  openRenameModal,
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
  handleRecycleTableChange,
  openPreview,
  openDownload,
  openFolder,
  triggerUpload,
  handleUploadPick,
  openUploadPicker,
  handleUploadDrop,
  clearUploadQueue,
  closeUploadModal,
  confirmUploadQueue,
  shareSelected,
  favoriteSelected,
  shareRecord,
  favoriteRecord,
  downloadSelected,
  copyShareLink,
  createShareLink,
  closeShareModal,
  onTableRow,
  enterMoveFolder,
  navigateMoveBreadcrumb,
  confirmMove,
  formatBytes,
  formatDateTime,
} = useFilesPage();
</script>

<template>
  <div class="workspace-section file-page">
    <Toolbar
      :mode="mode"
      @reload="reload"
      @upload="triggerUpload"
      @create-folder="createVisible = true"
      @clear-recycle="clearRecycle"
    />

    <Workspace
      :mode="mode"
      :loading="loading"
      :breadcrumb="breadcrumb"
      :sort-field="sortField"
      :sort-order="sortOrder"
      :selected-count="selectedCount"
      :selected-row-keys="selectedRowKeys"
      :recycle-total="recycleTotal"
      :table-data="tableData"
      :recycle-files="recycleFiles"
      :file-page-index="filePageIndex"
      :file-page-size="filePageSize"
      :file-total="fileTotal"
      :recycle-page-index="recyclePageIndex"
      :recycle-page-size="recyclePageSize"
      :recycle-table-loading="recycleTableLoading"
      :selected-rows="selectedRows"
      :recycle-selected-rows="recycleSelectedRows"
      :storage-settings="storageSettings"
      :current-parent-id="currentParentId"
      :format-bytes="formatBytes"
      :format-date-time="formatDateTime"
      :row-key="rowKey"
      :get-file-label="getFileLabel"
      :get-file-ext="getFileExt"
      :get-file-tone="getFileTone"
      :on-table-row="onTableRow"
      @navigate-root="currentParentId = undefined; filePageIndex = 0; reload()"
      @navigate-breadcrumb="
        (index) => {
          currentParentId = breadcrumb[index]?.fileId;
          filePageIndex = 0;
          reload();
        }
      "
      @selection-change="selectedRowKeys = $event.map(String)"
      @file-table-change="handleFileTableChange"
      @recycle-table-change="handleRecycleTableChange"
      @sort-change="handleSortChange"
      @open-context-menu="openContextMenu"
      @close-context-menu="closeContextMenu"
      @trigger-upload="triggerUpload"
      @create-folder="createVisible = true"
      @share-selected="shareSelected"
      @favorite-selected="favoriteSelected"
      @download-selected="downloadSelected"
      @open-batch-move="openBatchMoveModal"
      @recycle-selected="recycleSelected"
      @clear-selection="selectedRowKeys = []"
      @open-batch-delete-dialog="openBatchDeleteDialog"
      @restore-record="restoreRecord"
      @delete-record="deleteRecord"
      @open-preview="openPreview"
      @open-download="openDownload"
      @open-folder="openFolder"
      @open-rename-modal="openRenameModal"
      @open-move-modal="openMoveModal"
      @open-detail="openDetail"
      @open-recycle-action="openRecycleAction"
    />

    <DetailDrawer
      :open="detailVisible"
      :loading="detailLoading"
      :detail="currentDetail"
      :format-bytes="formatBytes"
      :format-date-time="formatDateTime"
      @close="detailVisible = false"
    />

    <DeleteDialog
      :open="deleteDialogVisible"
      :loading="deleteActionLoading"
      :target="deleteTarget"
      :selected-count="recycleSelectedRows.length"
      :get-file-label="getFileLabel"
      @cancel="closeDeleteDialog"
      @confirm="confirmDeleteRecord"
    />

    <FolderDialog
      :open="createVisible"
      :loading="createLoading"
      :storage-settings="storageSettings"
      :folder-name="createForm.folderName"
      :storage-setting-id="createForm.storageSettingId"
      @update:folder-name="createForm.folderName = $event"
      @update:storage-setting-id="createForm.storageSettingId = $event"
      @ok="createFolder"
      @cancel="createVisible = false"
    />

    <RenameDialog
      :open="renameVisible"
      :file-name="renameForm.fileName"
      @update:file-name="renameForm.fileName = $event"
      @ok="renameFile"
      @cancel="renameVisible = false"
    />

    <UploadDialog
      :open="uploadVisible"
      :running="uploadRunning"
      :queue="uploadQueue"
      :summary="uploadSummary"
      :overwrite-existing="overwriteExisting"
      :format-bytes="formatBytes"
      :get-upload-status-label="getUploadStatusLabel"
      @cancel="closeUploadModal"
      @clear-queue="clearUploadQueue"
      @open-picker="openUploadPicker"
      @drop="handleUploadDrop"
      @update:overwrite-existing="overwriteExisting = $event"
      @confirm="confirmUploadQueue"
    />

    <ShareDialog
      :open="shareVisible"
      :stage="shareStage"
      :loading="shareLoading"
      :share-files="shareFiles"
      :share-selected-file-count="shareSelectedFileCount"
      :share-selected-file-preview="shareSelectedFilePreview"
      :share-preview-single="sharePreviewSingle"
      :share-created="shareCreated"
      :share-url="shareUrl"
      :share-expire-text="shareExpireText"
      :share-form="shareForm"
      :get-share-preview-initial="getSharePreviewInitial"
      :get-share-preview-tone="getSharePreviewTone"
      @cancel="closeShareModal"
      @create-share-link="createShareLink"
      @copy-share-link="copyShareLink"
    />

    <MoveDialog
      :open="moveVisible"
      :loading="moveLoading"
      :folders="moveFolders"
      :breadcrumb="moveBreadcrumb"
      :current-parent-id="moveCurrentParentId"
      :selected-folder-id="moveSelectedFolderId"
      :loading-folders="moveLoadingFolders"
      :get-file-label="getFileLabel"
      @cancel="moveVisible = false"
      @navigate-root="navigateMoveBreadcrumb(-1)"
      @navigate-breadcrumb="navigateMoveBreadcrumb"
      @select-folder="moveSelectedFolderId = $event"
      @enter-folder="enterMoveFolder"
      @create-folder="createVisible = true"
      @confirm="confirmMove"
    />

    <input ref="uploadInput" class="hidden-upload-input" type="file" multiple @change="handleUploadPick" />

    <ContextMenu
      :open="contextMenuVisible"
      :x="contextMenuX"
      :y="contextMenuY"
      :target="contextMenuTarget"
      @close="closeContextMenu"
      @preview="openPreview"
      @share="shareRecord"
      @favorite="favoriteRecord"
      @download="openDownload"
      @rename="openRenameModal"
      @move="openMoveModal"
      @detail="openDetail"
      @recycle="openRecycleAction"
    />
  </div>
</template>

<style lang="less">
@import "./index.less";
</style>

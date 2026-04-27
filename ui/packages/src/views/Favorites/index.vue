<script setup lang="ts">
import {
  DownloadOutlined,
  FileOutlined,
  FolderOutlined,
  HeartOutlined,
  ReloadOutlined,
  EyeOutlined,
} from "@ant-design/icons-vue";
import { YButton, YCard } from "@yss-ui/components";
import { useFavoritesPage } from "./hooks/useFavoritesPage";

const {
  router,
  loading,
  actionLoading,
  searchValue,
  files,
  pageIndex,
  pageSize,
  total,
  favoriteCount,
  selectedCount,
  allVisibleSelected,
  visibleSelectionIndeterminate,
  loadFavorites,
  handleSearch,
  handlePageChange,
  openPreview,
  downloadFile,
  unfavoriteRecords,
  cancelCurrentPageFavorites,
  clearSelection,
  isSelected,
  handleSelectAllChange,
  handleItemSelectChange,
  cancelSelectedFavorites,
  getToneClass,
  getFileLabel,
  getFileExt,
  formatBytes,
  formatDateTime,
} = useFavoritesPage();
</script>

<template>
  <div class="workspace-section favorites-page">
    <div class="page-header favorites-header">
      <div class="page-title">
        <div class="page-title-icon">
          <HeartOutlined />
        </div>
        <div class="favorites-header-copy">
          <h2>收藏</h2>
          <p>查看你收藏的文件和文件夹</p>
        </div>
      </div>

      <div class="page-actions">
        <a-input-search
          v-model:value="searchValue"
          class="favorites-search"
          allow-clear
          placeholder="搜索收藏内容..."
          @search="handleSearch"
        />
        <YButton class="primary-ghost" @click="router.push('/files')">
          收藏更多
        </YButton>
        <a-popconfirm
          title="确认取消当前页收藏？"
          ok-text="确认"
          cancel-text="取消"
          @confirm="cancelCurrentPageFavorites"
        >
          <template #default>
            <YButton theme="danger" :loading="actionLoading">取消当前页收藏</YButton>
          </template>
        </a-popconfirm>
        <a-button class="icon-btn" type="default" @click="loadFavorites">
          <template #icon>
            <ReloadOutlined />
          </template>
        </a-button>
      </div>
    </div>

      <div class="favorites-summary">
      <span>已收藏 {{ favoriteCount }} 项</span>
      <a-checkbox
        v-if="files.length"
        :checked="allVisibleSelected"
        :indeterminate="visibleSelectionIndeterminate"
        @change="handleSelectAllChange"
      >
        全选本页
      </a-checkbox>
    </div>

    <div v-if="selectedCount > 0" class="favorites-batch-toolbar">
      <div class="batch-summary">已选择 {{ selectedCount }} 项</div>
      <div class="batch-actions">
        <YButton theme="danger" :loading="actionLoading" @click="cancelSelectedFavorites">
          取消收藏
        </YButton>
        <YButton class="primary-ghost" @click="clearSelection">清空选择</YButton>
      </div>
    </div>

    <div v-if="loading" class="favorites-loading">
      <a-spin />
    </div>

    <template v-else>
      <div v-if="files.length" class="favorites-grid">
        <div
          v-for="item in files"
          :key="item.fileId || item.displayName || item.originalName"
          class="favorite-card-shell"
          :class="[
            item.isDir ? 'is-dir' : '',
            item.isFavorite ? 'is-favorite' : '',
            getToneClass(item),
            isSelected(item) ? 'is-selected' : '',
          ]"
        >
          <a-checkbox
            class="favorite-card-checkbox"
            :checked="isSelected(item)"
            @change="(event) => handleItemSelectChange(item, event)"
          />
          <YCard
            class="favorite-card"
            :bordered="false"
            :padding="12"
          >
            <div class="favorite-top">
              <div class="favorite-icon">
                <FolderOutlined v-if="item.isDir" />
                <FileOutlined v-else />
              </div>
            </div>

            <div class="favorite-title">
              <h3>{{ getFileLabel(item) }}</h3>
            </div>

            <p class="favorite-desc">
              {{ item.isDir ? "文件夹" : `${getFileExt(item)} · ${formatBytes(item.size)}` }}
            </p>

            <div class="favorite-meta">
              <div>
                <span>更新时间</span>
                <strong>{{ formatDateTime(item.updateTime || item.uploadTime) }}</strong>
              </div>
              <div>
                <span>文件ID</span>
                <strong>{{ item.fileId || "-" }}</strong>
              </div>
            </div>

            <div class="favorite-actions">
              <YButton
                v-if="!item.isDir"
                type="primary"
                class="action-btn"
                @click="openPreview(item)"
              >
                <template #icon>
                  <EyeOutlined />
                </template>
                预览
              </YButton>
              <YButton
                v-if="!item.isDir"
                class="action-btn"
                @click="downloadFile(item)"
              >
                <template #icon>
                  <DownloadOutlined />
                </template>
                下载
              </YButton>
              <YButton
                theme="danger"
                class="action-btn"
                :loading="actionLoading"
                @click="unfavoriteRecords([item])"
              >
                <template #icon>
                  <HeartOutlined />
                </template>
                取消收藏
              </YButton>
            </div>
          </YCard>
        </div>
      </div>

      <div v-else class="favorites-empty">
        <a-empty description="暂无收藏内容" />
      </div>
    </template>

    <div v-if="files.length" class="favorites-pagination">
      <a-pagination
        :current="pageIndex + 1"
        :page-size="pageSize"
        :total="total"
        show-size-changer
        @change="handlePageChange"
        @showSizeChange="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
@import "./index.less";
</style>

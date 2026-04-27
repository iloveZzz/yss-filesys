<script setup lang="ts">
import {
  DeleteOutlined,
  DownloadOutlined,
  FileOutlined,
  FolderAddOutlined,
  FolderOutlined,
  HeartOutlined,
  MoreOutlined,
  ShareAltOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
  SwapOutlined,
} from "@ant-design/icons-vue";
import { YButton, YCard } from "@yss-ui/components";
import type { FileRecordDTO, StorageSettingDTO } from "@/api/generated/filesys/schemas";

const {
  mode,
  loading,
  breadcrumb,
  sortField,
  sortOrder,
  selectedCount,
  selectedRowKeys,
  tableData,
  recycleFiles,
  filePageIndex,
  filePageSize,
  fileTotal,
  recyclePageIndex,
  recyclePageSize,
  recycleTotal,
  recycleTableLoading,
  selectedRows,
  currentParentId,
  formatBytes,
  formatDateTime,
  getFileLabel,
  getFileExt,
  getFileTone,
} = defineProps<{
  mode: "files" | "recycle";
  loading: boolean;
  breadcrumb: FileRecordDTO[];
  sortField?: "updateTime" | "size";
  sortOrder?: "asc" | "desc";
  selectedCount: number;
  selectedRowKeys: string[];
  tableData: FileRecordDTO[];
  recycleFiles: FileRecordDTO[];
  filePageIndex: number;
  filePageSize: number;
  fileTotal: number;
  recyclePageIndex: number;
  recyclePageSize: number;
  recycleTotal: number;
  recycleTableLoading: boolean;
  selectedRows: FileRecordDTO[];
  recycleSelectedRows: FileRecordDTO[];
  storageSettings: StorageSettingDTO[];
  currentParentId?: string;
  formatBytes: (value?: number) => string;
  formatDateTime: (value?: string) => string;
  rowKey: (record: FileRecordDTO) => string;
  getFileLabel: (record: FileRecordDTO) => string;
  getFileExt: (record: FileRecordDTO) => string;
  getFileTone: (record: FileRecordDTO) => string;
  onTableRow: (record: FileRecordDTO) => Record<string, unknown>;
}>();

const emit = defineEmits<{
  navigateRoot: [];
  navigateBreadcrumb: [index: number];
  fileTableChange: [pagination: { current?: number; pageSize?: number }];
  recycleTableChange: [pagination: { current?: number; pageSize?: number }];
  openContextMenu: [event: MouseEvent, record: FileRecordDTO];
  closeContextMenu: [];
  clearSelection: [];
  triggerUpload: [];
  createFolder: [];
  selectionChange: [keys: (string | number)[]];
  shareSelected: [];
  favoriteSelected: [];
  downloadSelected: [];
  openBatchMove: [rows: FileRecordDTO[]];
  recycleSelected: [];
  openBatchDeleteDialog: [];
  restoreRecord: [record: FileRecordDTO];
  deleteRecord: [record: FileRecordDTO];
  openPreview: [record: FileRecordDTO];
  openDownload: [record: FileRecordDTO];
  openRenameModal: [];
  openMoveModal: [record: FileRecordDTO];
  openDetail: [record: FileRecordDTO];
  openRecycleAction: [];
  openFolder: [record: FileRecordDTO];
  sortChange: [payload: { sortField?: "updateTime" | "size"; sortOrder?: "asc" | "desc" }];
}>();

const getRecordKey = (record: FileRecordDTO) => record.fileId || record.displayName || record.originalName || "";

const toggleSelection = (record: FileRecordDTO) => {
  const key = getRecordKey(record);
  if (!key) return;
  const next = selectedRowKeys.includes(key)
    ? selectedRowKeys.filter((item) => item !== key)
    : [...selectedRowKeys, key];
  emit("selectionChange", next);
};

const toggleAllSelection = (checked: boolean) => {
  const selectableKeys = tableData
    .map((record) => record.fileId)
    .filter((item): item is string => !!item);
  emit("selectionChange", checked ? selectableKeys : []);
};

const isAllSelected = () => {
  const selectableKeys = tableData
    .map((record) => record.fileId)
    .filter((item): item is string => !!item);
  return selectableKeys.length > 0 && selectableKeys.every((key) => selectedRowKeys.includes(key));
};

const isSelectionIndeterminate = () => {
  const selectableKeys = tableData
    .map((record) => record.fileId)
    .filter((item): item is string => !!item);
  const selectedCount = selectableKeys.filter((key) => selectedRowKeys.includes(key)).length;
  return selectedCount > 0 && selectedCount < selectableKeys.length;
};

const handleItemClick = (record: FileRecordDTO) => {
  toggleSelection(record);
};

const handleItemContextMenu = (event: MouseEvent, record: FileRecordDTO) => {
  event.preventDefault();
  event.stopPropagation();
  emit("openContextMenu", event, record);
};

const handleItemDblClick = (record: FileRecordDTO) => {
  emit("openFolder", record);
};

const isSortActive = (field: "updateTime" | "size") => sortField === field;

const getSortIcon = (field: "updateTime" | "size") => {
  if (!isSortActive(field)) return SortAscendingOutlined;
  return sortOrder === "asc" ? SortAscendingOutlined : SortDescendingOutlined;
};

const toggleSort = (field: "updateTime" | "size") => {
  const nextOrder = sortField === field ? (sortOrder === "asc" ? "desc" : "asc") : "desc";
  emit("sortChange", {
    sortField: field,
    sortOrder: nextOrder,
  });
};
</script>

<template>
  <YCard class="workspace-panel file-panel" :bordered="false" :padding="0">
    <div class="panel-header">
      <div class="panel-title">
        <span>{{ mode === "files" ? "我的文件" : "回收站" }}</span>
        <span v-if="mode === 'files' && breadcrumb.length" class="panel-path">
          /
          <a @click.prevent="$emit('navigateRoot')">全部文件</a>
          <template v-for="item in breadcrumb" :key="item.fileId">
            <span>/</span>
            <a @click.prevent="$emit('navigateBreadcrumb', breadcrumb.indexOf(item))">
              {{ item.displayName }}
            </a>
          </template>
        </span>
      </div>
      <div v-if="mode === 'files' && selectedCount > 0" class="panel-selected">
        已选 {{ selectedCount }} 项
      </div>
      <div v-else-if="mode === 'recycle'" class="panel-selected recycle-tip">
        共 {{ recycleTotal }} 项 · 回收站内容保存 7 天，删除后自动清理
      </div>
    </div>

    <template v-if="mode === 'files'">
      <a-spin :spinning="loading">
        <div class="table-shell" @click="emit('closeContextMenu')">
          <div class="list-toolbar">
            <a-checkbox
              :checked="isAllSelected()"
              :indeterminate="isSelectionIndeterminate()"
              @change="(event) => toggleAllSelection(!!event.target.checked)"
            />
            <span class="list-toolbar-text">
              全选
              <template v-if="selectedCount > 0"> · 已选 {{ selectedCount }} 项</template>
            </span>
          </div>

          <template v-if="tableData.length">
            <div class="file-list-shell" :key="`files-${filePageIndex}-${filePageSize}-${currentParentId || 'root'}`">
              <div class="file-list-head">
                <div class="file-list-select"></div>
                <div class="file-list-main">文件名</div>
                <div class="file-list-user">用户ID</div>
                <div class="file-list-favorite">是否收藏</div>
                <div class="file-list-dir">是否文件夹</div>
                <div class="file-list-size">
                  <button
                    type="button"
                    class="file-list-sortable"
                    :class="{ active: isSortActive('size') }"
                    @click="toggleSort('size')"
                  >
                    <span>大小</span>
                    <component :is="getSortIcon('size')" class="file-list-sort-icon" />
                  </button>
                </div>
                <div class="file-list-time">
                  <button
                    type="button"
                    class="file-list-sortable"
                    :class="{ active: isSortActive('updateTime') }"
                    @click="toggleSort('updateTime')"
                  >
                    <span>修改时间</span>
                    <component :is="getSortIcon('updateTime')" class="file-list-sort-icon" />
                  </button>
                </div>
                <div class="file-list-actions">操作</div>
              </div>
              <a-list
                class="file-list"
                :data-source="tableData"
                :split="true"
                :key="`file-list-${filePageIndex}-${filePageSize}-${currentParentId || 'root'}`"
              >
                <template #renderItem="{ item }">
                  <a-list-item
                    class="file-list-item"
                    @click="handleItemClick(item)"
                    @dblclick="handleItemDblClick(item)"
                    @contextmenu="handleItemContextMenu($event, item)"
                  >
                    <div class="file-list-row">
                      <div class="file-list-select">
                        <a-checkbox
                          :checked="selectedRowKeys.includes(item.fileId || item.displayName || item.originalName || '')"
                          :disabled="!item.fileId"
                          @click.stop
                          @change="() => toggleSelection(item)"
                        />
                      </div>

                      <div class="file-list-main">
                        <div class="file-name-cell">
                          <div class="file-icon" :class="`file-tone-${getFileTone(item)}`">
                            <FolderOutlined v-if="item.isDir" />
                            <span v-else>{{ getFileExt(item) }}</span>
                          </div>
                          <div class="file-name-meta">
                            <strong>{{ getFileLabel(item) }}</strong>
                            <span v-if="item.isDir">目录</span>
                          </div>
                        </div>
                      </div>

                      <div class="file-list-user">
                        {{ item.userId || "-" }}
                      </div>

                      <div class="file-list-favorite">
                        <a-tag :color="item.isFavorite ? 'green' : 'default'">
                          {{ item.isFavorite ? "是" : "否" }}
                        </a-tag>
                      </div>

                      <div class="file-list-dir">
                        <a-tag :color="item.isDir ? 'blue' : 'default'">
                          {{ item.isDir ? "是" : "否" }}
                        </a-tag>
                      </div>

                      <div class="file-list-size">
                        {{ item.isDir ? "-" : formatBytes(item.size) }}
                      </div>

                      <div class="file-list-time">
                        {{ formatDateTime(item.updateTime) }}
                      </div>

                      <div class="file-list-actions">
                        <a-button
                          type="text"
                          size="small"
                          class="row-more"
                          @click.stop="emit('openContextMenu', $event, item)"
                        >
                          <MoreOutlined />
                        </a-button>
                      </div>
                    </div>
                  </a-list-item>
                </template>
              </a-list>
            </div>
          </template>

          <div v-else class="file-empty-state">
            <div class="file-empty-icon">
              <FileOutlined />
            </div>
            <strong>暂无文件</strong>
            <p>上传文件或创建文件夹开始使用</p>
            <div class="file-empty-actions">
              <YButton type="primary" @click="$emit('triggerUpload')">
                <DownloadOutlined />
                上传文件
              </YButton>
              <YButton class="primary-ghost" @click="$emit('createFolder')">
                <FolderAddOutlined />
                新建文件夹
              </YButton>
            </div>
          </div>

          <div class="table-pagination">
            <a-pagination
              :current="filePageIndex + 1"
              :page-size="filePageSize"
              :total="fileTotal"
              show-size-changer
              @change="(page, pageSize) => emit('fileTableChange', { current: page, pageSize })"
              @showSizeChange="(current, pageSize) => emit('fileTableChange', { current, pageSize })"
            />
          </div>
        </div>
      </a-spin>

      <div v-if="selectedCount > 0" class="batch-toolbar">
        <div class="batch-summary">已选择 {{ selectedCount }} 项</div>
        <div class="batch-actions">
          <YButton type="primary" @click="$emit('shareSelected')">
            <ShareAltOutlined />
            分享
          </YButton>
          <YButton @click="$emit('favoriteSelected')">
            <HeartOutlined />
            收藏
          </YButton>
          <YButton @click="$emit('downloadSelected')">
            <DownloadOutlined />
            下载
          </YButton>
          <YButton @click="$emit('openBatchMove', selectedRows)">
            <SwapOutlined />
            移动
          </YButton>
          <YButton class="danger" @click="$emit('recycleSelected')">
            <DeleteOutlined />
            放入回收站
          </YButton>
          <YButton class="danger" @click="$emit('clearSelection')">取消</YButton>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="table-shell recycle-shell" :key="`recycle-shell-${recyclePageIndex}-${recyclePageSize}`">
        <template v-if="recycleFiles.length">
          <a-table
            class="file-table"
            :data-source="recycleFiles"
            :loading="recycleTableLoading"
            :custom-row="onTableRow"
            :row-selection="{
              type: 'checkbox',
              selectedRowKeys,
              onChange: (keys: (string | number)[]) => $emit('selectionChange', keys),
              getCheckboxProps: (record: FileRecordDTO) => ({ disabled: !record.fileId }),
            }"
            :pagination="{
              current: recyclePageIndex + 1,
              pageSize: recyclePageSize,
              total: recycleTotal,
              showSizeChanger: true,
            }"
            row-key="fileId"
            size="middle"
            :key="`recycle-table-${recyclePageIndex}-${recyclePageSize}`"
            @change="$emit('recycleTableChange', $event)"
          >
            <a-table-column title="文件名" :width="620">
              <template #default="{ record }">
                <div class="file-name-cell">
                  <div class="file-icon" :class="`file-tone-${getFileTone(record)}`">
                    <FolderOutlined v-if="record.isDir" />
                    <span v-else>{{ getFileExt(record) }}</span>
                  </div>
                  <div class="file-name-meta">
                    <strong>{{ getFileLabel(record) }}</strong>
                    <span>{{ record.isDir ? "目录" : "文件" }}</span>
                  </div>
                </div>
              </template>
            </a-table-column>
            <a-table-column title="大小" width="160">
              <template #default="{ record }">
                {{ record.isDir ? "-" : formatBytes(record.size) }}
              </template>
            </a-table-column>
            <a-table-column title="删除时间" width="200">
              <template #default="{ record }">{{ formatDateTime(record.updateTime) }}</template>
            </a-table-column>
            <a-table-column title="操作" width="180" fixed="right">
              <template #default="{ record }">
                <a-space size="middle">
                  <a-button type="text" size="small" @click="$emit('restoreRecord', record)">恢复</a-button>
                  <a-button type="text" size="small" danger @click="$emit('deleteRecord', record)">删除</a-button>
                </a-space>
              </template>
            </a-table-column>
          </a-table>
        </template>

        <div v-else class="file-empty-state recycle-empty-state">
          <div class="file-empty-icon">
            <DeleteOutlined />
          </div>
          <strong>回收站为空</strong>
          <p>删除的文件会在这里保留 7 天</p>
        </div>
      </div>

      <div v-if="mode === 'recycle' && selectedCount > 0" class="batch-toolbar">
        <div class="batch-summary">已选择 {{ selectedCount }} 项</div>
        <div class="batch-actions">
          <YButton class="danger" @click="$emit('openBatchDeleteDialog')">
            <DeleteOutlined />
            永久删除
          </YButton>
          <YButton class="danger" @click="$emit('clearSelection')">取消</YButton>
        </div>
      </div>
    </template>
  </YCard>
</template>

<script setup lang="ts">
import {
  CopyOutlined,
  DeleteOutlined,
  EyeOutlined,
  FileTextOutlined,
  LinkOutlined,
  MoreOutlined,
  ReloadOutlined,
} from "@ant-design/icons-vue";
import { useSharePage, type ShareMenuAction } from "./hooks/useSharePage";

const {
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
  refresh,
  commitSearch,
  handleTableChange,
  clearAllShares,
  openPublicPage,
  getShareUrl,
  handleShareMenu,
  batchCancelSelected,
  rowSelection,
  batchClearSelection,
  formatShareScope,
  scopeTagColor,
  formatDateTime,
  confirmCancelShare,
} = useSharePage();
</script>

<template>
  <div class="workspace-section share-page">
    <div class="page-header share-header">
      <div class="share-header-copy">
        <h2>我的分享</h2>
        <p>支持按名称模糊搜索分享、查看详情和取消分享，延续统一的卡片式布局。</p>
      </div>
      <a-space :size="8">
        <a-input-search
          v-model:value="searchKeyword"
          allow-clear
          placeholder="搜索"
          class="share-search"
          @search="commitSearch"
        />
        <a-button class="icon-button" @click="refresh">
          <ReloadOutlined />
        </a-button>
        <a-button danger :disabled="!sharesTotal" @click="clearAllShares">
          <DeleteOutlined />
          清空所有分享
        </a-button>
      </a-space>
    </div>

    <a-card class="workspace-panel share-panel" :bordered="false" :padding="0">
      <div class="share-summary">
        <span>共 {{ sharesTotal }} 项</span>
        <span class="share-summary-tip">· 支持按名称模糊搜索分享</span>
      </div>

      <a-table
        :data-source="shares"
        :pagination="{
          current: sharesPageIndex + 1,
          pageSize: sharesPageSize,
          total: sharesTotal,
          showSizeChanger: true,
        }"
        row-key="shareId"
        :loading="loading"
        :row-selection="rowSelection"
        @change="handleTableChange"
      >
        <a-table-column title="名称" width="520">
          <template #default="{ record }">
            <div class="share-name-cell">
              <div class="share-link-icon">
                <LinkOutlined />
              </div>
              <div class="share-name-text">{{ record.shareName }}</div>
            </div>
          </template>
        </a-table-column>
        <a-table-column title="有效期" width="150">
          <template #default="{ record }">
            {{ record.expireTime ? `${formatDateTime(record.expireTime)} 后到期` : "永久有效" }}
          </template>
        </a-table-column>
        <a-table-column title="查看次数" width="110">
          <template #default="{ record }">{{ record.viewCount || 0 }}</template>
        </a-table-column>
        <a-table-column title="下载次数" width="110">
          <template #default="{ record }">{{ record.downloadCount || 0 }}</template>
        </a-table-column>
        <a-table-column title="分享权限" width="120">
          <template #default="{ record }">
            <a-tag :color="scopeTagColor(record.scope)">{{ formatShareScope(record.scope) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="创建时间" width="180">
          <template #default="{ record }">{{ formatDateTime(record.createdAt) }}</template>
        </a-table-column>
        <a-table-column title="操作" width="110" fixed="right">
          <template #default="{ record }">
            <a-dropdown trigger="click">
              <a-button type="text" class="share-more-btn">
                <MoreOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleShareMenu(key as ShareMenuAction, record)">
                  <a-menu-item key="copy">
                    <CopyOutlined />
                    快捷复制
                  </a-menu-item>
                  <a-menu-item key="detail">
                    <EyeOutlined />
                    查看详情
                  </a-menu-item>
                  <a-menu-item key="records">
                    <FileTextOutlined />
                    访问记录
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="cancel" class="danger-menu-item">
                    <DeleteOutlined />
                    取消分享
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <div v-if="selectedRowKeys.length > 0" class="batch-toolbar">
      <div class="batch-summary">已选择 {{ selectedRowKeys.length }} 项</div>
      <div class="batch-actions">
        <a-button danger @click="batchCancelSelected">
          <DeleteOutlined />
          取消分享
        </a-button>
        <a-button @click="batchClearSelection">取消选择</a-button>
      </div>
    </div>

    <a-drawer v-model:open="detailVisible" title="分享详情" width="520">
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="分享ID">{{ currentShare?.shareId }}</a-descriptions-item>
          <a-descriptions-item label="分享名称">{{ currentShare?.shareName }}</a-descriptions-item>
          <a-descriptions-item label="提取码">{{ currentShare?.shareCode || '无需' }}</a-descriptions-item>
          <a-descriptions-item label="文件数">{{ currentShare?.fileIds?.length || 0 }}</a-descriptions-item>
          <a-descriptions-item label="浏览次数">{{ currentShare?.viewCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="下载次数">{{ currentShare?.downloadCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="过期时间">{{ formatDateTime(currentShare?.expireTime) }}</a-descriptions-item>
          <a-descriptions-item label="链接">
            <a @click.prevent="openPublicPage(currentShare?.shareId)">
              {{ currentShare ? getShareUrl(currentShare) : "-" }}
            </a>
          </a-descriptions-item>
        </a-descriptions>

        <div style="margin-top: 20px">
          <h3>访问记录</h3>
          <a-table :data-source="accessRecords" :pagination="false" row-key="id" size="small">
            <a-table-column title="时间" width="180">
              <template #default="{ record }">{{ formatDateTime(record.accessTime) }}</template>
            </a-table-column>
            <a-table-column title="IP" data-index="accessIp" />
            <a-table-column title="浏览器" data-index="browser" />
          </a-table>
        </div>
      </a-spin>
    </a-drawer>

    <a-modal
      v-model:open="cancelConfirmVisible"
      title="确认取消分享"
      centered
      :confirm-loading="cancelLoading"
      @ok="confirmCancelShare"
      @cancel="cancelConfirmVisible = false"
    >
      <p>
        确定要取消选中的 {{ cancelTargets.length }} 个分享吗？取消后将无法恢复。
      </p>
    </a-modal>
  </div>
</template>

<style scoped lang="less">
@import "./index.less";
</style>

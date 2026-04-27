<script setup lang="ts">
import { formatBytes, formatDateTime } from "@/utils/format";
import { useSharePublicPage } from "./hooks/useSharePublicPage";

const { loading, shareInfo, shareFiles, accessRecords, inputShareCode, shareId, verifyCode } =
  useSharePublicPage();
</script>

<template>
  <div class="public-page">
    <a-card class="workspace-panel public-card" :bordered="false">
      <div class="public-header">
        <div class="page-kicker">公开分享</div>
        <h2>公开分享</h2>
        <p>{{ shareId }}</p>
      </div>
      <a-spin :spinning="loading">
        <div class="public-summary-grid">
          <div class="public-summary-card">
            <span>分享名称</span>
            <strong>{{ shareInfo?.shareName || "-" }}</strong>
          </div>
          <div class="public-summary-card">
            <span>文件数量</span>
            <strong>{{ shareInfo?.fileCount || 0 }}</strong>
          </div>
          <div class="public-summary-card">
            <span>浏览次数</span>
            <strong>{{ shareInfo?.viewCount || 0 }}</strong>
          </div>
          <div class="public-summary-card">
            <span>下载次数</span>
            <strong>{{ shareInfo?.downloadCount || 0 }}</strong>
          </div>
        </div>

        <div class="public-status-card">
          <span>过期状态</span>
          <strong>{{ shareInfo?.isExpire ? "已过期" : "未过期" }}</strong>
        </div>

        <a-space v-if="shareInfo?.hasCheckCode" direction="vertical" class="public-code" style="width: 100%">
          <a-input v-model:value="inputShareCode" placeholder="提取码" />
          <a-button type="primary" @click="verifyCode">验证提取码</a-button>
        </a-space>

        <div class="public-section">
          <div class="section-heading-inline">
            <div>
              <div class="section-kicker">公开内容</div>
              <h3>文件列表</h3>
            </div>
          </div>
          <a-card class="public-table-card" :bordered="false" :padding="0">
            <a-table :data-source="shareFiles" :pagination="false" row-key="fileId" size="small">
              <a-table-column title="名称" data-index="displayName" />
              <a-table-column title="类型" width="100">
                <template #default="{ record }">
                  <a-tag>{{ record.isDir ? '目录' : '文件' }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column title="大小" width="120">
                <template #default="{ record }">{{ formatBytes(record.size) }}</template>
              </a-table-column>
              <a-table-column title="更新时间" width="180">
                <template #default="{ record }">{{ formatDateTime(record.updateTime) }}</template>
              </a-table-column>
            </a-table>
          </a-card>
        </div>

        <div class="public-section">
          <div class="section-heading-inline">
            <div>
              <div class="section-kicker">访问轨迹</div>
              <h3>访问记录</h3>
            </div>
          </div>
          <a-card class="public-table-card" :bordered="false" :padding="0">
            <a-table :data-source="accessRecords" :pagination="false" row-key="id" size="small">
              <a-table-column title="时间" width="180">
                <template #default="{ record }">{{ formatDateTime(record.accessTime) }}</template>
              </a-table-column>
              <a-table-column title="IP" data-index="accessIp" />
              <a-table-column title="浏览器" data-index="browser" />
              <a-table-column title="系统" data-index="os" />
            </a-table>
          </a-card>
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped lang="less">
@import "./index.less";
</style>

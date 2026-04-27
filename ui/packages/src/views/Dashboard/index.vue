<script setup lang="ts">
import { CloudOutlined, DatabaseOutlined, FolderOpenOutlined, ShareAltOutlined } from "@ant-design/icons-vue";
import { YButton } from "@yss-ui/components";
import DashboardOverview from "./components/DashboardOverview.vue";
import DashboardRecentFiles from "./components/DashboardRecentFiles.vue";
import { useDashboardPage } from "./hooks/useDashboardPage";

const {
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
} = useDashboardPage();
</script>

<template>
  <div class="dashboard-page">
    <section class="dashboard-hero page-shell">
      <div class="dashboard-hero-copy">
        <h2>统一查看容量、文件与分享状态</h2>
        <p>
          首页保留总览、最近文件和容量曲线，风格与参考项目保持一致，强调工作台感和卡片层次。
        </p>
        <div class="dashboard-hero-actions page-actions">
          <YButton type="primary" @click="$router.push('/files')">
            <FolderOpenOutlined />
            进入文件管理
          </YButton>
          <YButton @click="$router.push('/share')">
            <ShareAltOutlined />
            查看分享列表
          </YButton>
        </div>
      </div>

      <div class="dashboard-hero-stats">
        <div class="dashboard-hero-stat">
          <DatabaseOutlined />
          <strong>{{ formatBytes(home.totalBytes) }}</strong>
          <span>当前总容量</span>
        </div>
        <div class="dashboard-hero-stat">
          <CloudOutlined />
          <strong>{{ capacityPercent }}%</strong>
          <span>容量使用率</span>
        </div>
      </div>
    </section>

    <DashboardOverview
      :home-total-text="formatBytes(home.totalBytes)"
      :capacity-loading="capacityStore.loading"
      :capacity-percent="capacityPercent"
      :capacity-summary="capacitySummary"
      :chart-option="chartOption"
      @manage-storage="$router.push('/storage')"
    />

    <DashboardRecentFiles
      :loading="recentCompletedLoading"
      :recent-files="recentCompletedFiles"
      :total="recentCompletedTotal"
      :page-index="recentCompletedPageIndex"
      :page-size="recentCompletedPageSize"
      :format-bytes="formatBytes"
      :format-date-time="formatDateTime"
      @page-change="handleRecentCompletedPageChange"
      @manage-files="$router.push('/files')"
    />
  </div>
</template>

<style lang="less">
@import "./index.less";
</style>

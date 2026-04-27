<script setup lang="ts">
import { DatabaseOutlined } from "@ant-design/icons-vue";
import { YEcharts, YButton, YCard } from "@yss-ui/components";

defineProps<{
  homeTotalText: string;
  capacityLoading: boolean;
  capacityPercent: number;
  capacitySummary: string;
  chartOption: Record<string, any>;
}>();

defineEmits<{
  manageStorage: [];
}>();
</script>

<template>
  <section class="dashboard-top">
    <YCard class="growth-card" :bordered="false" :padding="18">
      <template #title>
        <div class="section-heading-inline">
          <div>
            <h2>存储增长</h2>
            <p>
              把原先的分类总览替换为更直观的增长曲线，方便快速判断容量变化趋势。
            </p>
          </div>
          <div class="chart-toolbar">
            <YButton size="small">近 3 个月</YButton>
            <YButton size="small">近 30 天</YButton>
            <YButton type="primary" size="small">近 7 天</YButton>
            <a-select
              class="chart-unit"
              size="small"
              value="MB"
              :options="[{ label: 'MB', value: 'MB' }]"
            />
          </div>
        </div>
      </template>

      <YEcharts :options="chartOption" height="320px" autoresize />
    </YCard>

    <div class="dashboard-side-stack">
      <YCard class="hero-summary" :bordered="false" :padding="24">
        <div class="hero-summary-kicker">容量总览</div>
        <div class="summary-art">
          <DatabaseOutlined />
        </div>
        <div class="summary-value">{{ homeTotalText }}</div>
        <YButton
          type="primary"
          size="small"
          block
          @click="$emit('manageStorage')"
          >管理存储空间</YButton
        >
      </YCard>

      <YCard class="capacity-summary-card" :bordered="false" :padding="20">
        <div class="capacity-summary-head">
          <div>
            <h3>容量使用</h3>
            <p>工作台实时展示当前存储池容量占用。</p>
          </div>
          <strong>{{ capacityLoading ? "--" : `${capacityPercent}%` }}</strong>
        </div>
        <a-progress :percent="capacityPercent" :show-info="false" />
        <div class="capacity-summary-text">
          {{ capacityLoading ? "正在获取存储容量信息..." : capacitySummary }}
        </div>
      </YCard>
    </div>
  </section>
</template>

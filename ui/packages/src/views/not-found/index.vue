<template>
  <div class="not-found">
    <a-card class="not-found-card workspace-panel" :bordered="false">
      <div class="not-found-copy">
        <div class="page-kicker">404</div>
        <h2>页面不存在</h2>
        <p>你访问的页面已移动、删除或暂时不可用，请返回工作台继续操作。</p>
      </div>
      <div class="not-found-actions">
        <a-button type="primary" @click="$router.push('/dashboard')">返回工作台</a-button>
        <a-button @click="$router.back()">返回上一页</a-button>
      </div>
    </a-card>
  </div>
</template>

<style scoped lang="less">
@import "./index.less";
</style>

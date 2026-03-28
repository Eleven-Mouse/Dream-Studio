<template>
  <management-page-shell>
    <article-management-panel :bordered="false" :show-header="false" />
  </management-page-shell>
</template>

<script setup>
import ArticleManagementPanel from '@/components/workspace/panels/shared/ArticleManagementPanel.vue'
import ManagementPageShell from './ManagementPageShell.vue'
</script>

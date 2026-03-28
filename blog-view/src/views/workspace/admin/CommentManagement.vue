<template>
  <management-page-shell>
    <comment-management-panel :bordered="false" :show-header="false" />
  </management-page-shell>
</template>

<script setup>
import CommentManagementPanel from '@/components/workspace/panels/admin/CommentManagementPanel.vue'
import ManagementPageShell from '../shared/ManagementPageShell.vue'
</script>

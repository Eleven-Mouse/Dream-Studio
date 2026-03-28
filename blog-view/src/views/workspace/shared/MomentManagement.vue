<template>
  <management-page-shell>
    <moment-management-panel :bordered="false" :show-header="false" />
  </management-page-shell>
</template>

<script setup>
import MomentManagementPanel from '@/components/workspace/panels/shared/MomentManagementPanel.vue'
import ManagementPageShell from './ManagementPageShell.vue'
</script>

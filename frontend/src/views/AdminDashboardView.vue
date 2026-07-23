<script setup>import { useRouter } from 'vue-router'; import PageScaffold from '../components/PageScaffold.vue'; const router = useRouter()</script>
<template><PageScaffold title="后台" description="店铺资料、封面和展示状态可在店铺管理中维护。" :cards="['店铺管理', '资源管理', '状态标签']"><template #actions><el-button type="primary" @click="router.push('/admin/shops')">进入店铺管理</el-button></template></PageScaffold></template>

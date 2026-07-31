<script setup>
defineProps({ disabled: Boolean })
defineEmits(['ask'])

const questions = ['今天有什么优惠？', '推荐几家校园商铺', '热门探店有哪些？', '我的宿舍信息']
</script>

<template>
  <section class="assistant-welcome">
    <div class="welcome-icon" aria-hidden="true">禾</div>
    <h2>你好，我是青禾校园助手。</h2>
    <p>我可以帮你查询校园优惠、商铺、探店，以及登录后的宿舍、优惠券和订单信息。</p>
    <div class="quick-questions"><el-button v-for="question in questions" :key="question" plain size="small" :disabled="disabled" @click="$emit('ask', question)">{{ question }}</el-button></div>
  </section>
</template>

<style scoped>
.assistant-welcome{margin:18px;padding:22px 18px;border:1px solid #e4ebf7;border-radius:14px;background:linear-gradient(145deg,#f8fbff,#edf4ff);text-align:center}.welcome-icon{display:grid;place-items:center;width:42px;height:42px;margin:0 auto 12px;border-radius:14px;color:#fff;background:var(--qh-primary);font-weight:800}.assistant-welcome h2{margin:0 0 8px;font-size:17px;color:var(--qh-text)}.assistant-welcome p{margin:0;color:var(--qh-text-secondary);font-size:13px;line-height:1.65}.quick-questions{display:flex;justify-content:center;gap:8px;flex-wrap:wrap;margin-top:16px}.quick-questions .el-button{margin:0}
</style>

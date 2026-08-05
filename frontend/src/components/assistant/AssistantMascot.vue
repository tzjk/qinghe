<script setup>
import { computed } from 'vue'

const props = defineProps({ state: { type: String, default: 'idle' }, compact: Boolean })
const label = computed(() => ({ idle: '校园助手正在待机', waving: '校园助手正在挥手', thinking: '校园助手正在思考', speaking: '校园助手正在说话' }[props.state] || '校园助手'))
</script>

<template>
  <div class="assistant-mascot" :class="[`is-${state}`, { compact }]" role="img" :aria-label="label">
    <svg viewBox="0 0 160 150" aria-hidden="true">
      <g class="mascot-float">
        <path class="mascot-shadow" d="M37 133c13-10 72-10 86 0-16 8-69 8-86 0Z" />
        <path class="mascot-body" d="M51 87c9-14 49-14 58 0l7 36H44l7-36Z" />
        <path class="mascot-collar" d="m69 91 11 11 11-11" />
        <g class="mascot-arm mascot-arm-left"><path d="M53 94c-14 7-18 22-13 31" /><circle cx="39" cy="126" r="6" /></g>
        <g class="mascot-arm mascot-arm-right"><path d="M107 94c14 7 18 22 13 31" /><circle cx="121" cy="126" r="6" /></g>
        <path class="mascot-hair" d="M49 64c0-31 62-31 62 0v17H49V64Z" /><circle class="mascot-face" cx="80" cy="65" r="28" />
        <path class="mascot-bang" d="M57 49c7-14 39-17 47 0-9-5-14-3-20 3-8-7-18-4-27-3Z" />
        <g class="mascot-eyes"><ellipse cx="69" cy="66" rx="3.5" ry="4.5" /><ellipse cx="91" cy="66" rx="3.5" ry="4.5" /><path class="mascot-lids" d="M65 66h8M87 66h8" /></g>
        <path class="mascot-mouth" d="M74 78c4 3 8 3 12 0" />
        <g class="mascot-thought"><circle cx="121" cy="43" r="3" /><circle cx="130" cy="34" r="5" /><circle cx="142" cy="23" r="7" /></g>
        <g class="mascot-sound"><path d="M105 68h12" /><path d="M108 75h16" /></g>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.assistant-mascot{width:142px;max-width:42vw;color:#326dc9}.assistant-mascot.compact{width:50px;max-width:none}.assistant-mascot svg{display:block;width:100%;height:auto;overflow:visible}.mascot-float{transform-origin:80px 122px;animation:mascot-float 2.8s ease-in-out infinite}.mascot-shadow{fill:rgba(44,84,150,.13)}.mascot-body{fill:#4d83dc;stroke:#2f65b9;stroke-width:3;stroke-linejoin:round}.mascot-collar{fill:none;stroke:#fff;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.mascot-arm path{fill:none;stroke:#2f65b9;stroke-width:9;stroke-linecap:round}.mascot-arm circle{fill:#ffe3cf}.mascot-face{fill:#ffe3cf;stroke:#e9b998;stroke-width:2}.mascot-hair,.mascot-bang{fill:#294b83}.mascot-eyes{fill:#294b83}.mascot-lids{fill:none;stroke:#ffe3cf;stroke-width:3;stroke-linecap:round;animation:mascot-blink 4.6s steps(1,end) infinite}.mascot-mouth{fill:none;stroke:#c87568;stroke-width:2.5;stroke-linecap:round}.mascot-thought,.mascot-sound{display:none;fill:#5f8fdf;stroke:#5f8fdf;stroke-width:3;stroke-linecap:round}.is-waving .mascot-arm-right{transform-box:fill-box;transform-origin:0% 10%;animation:mascot-wave .42s ease-in-out 3}.is-thinking .mascot-thought{display:block;animation:mascot-think 1s ease-in-out infinite}.is-speaking .mascot-sound{display:block;animation:mascot-sound .7s ease-in-out infinite}.is-speaking .mascot-mouth{animation:mascot-talk .42s ease-in-out infinite}.compact .mascot-float{animation-duration:3.2s}.compact .mascot-thought,.compact .mascot-sound{display:none}@keyframes mascot-float{50%{transform:translateY(-5px)}}@keyframes mascot-blink{0%,91%,100%{stroke-width:0}93%,96%{stroke-width:3}}@keyframes mascot-wave{50%{transform:rotate(-24deg)}}@keyframes mascot-think{50%{opacity:.42;transform:translateY(-3px)}}@keyframes mascot-sound{50%{opacity:.36;transform:translateX(3px)}}@keyframes mascot-talk{50%{transform:scaleY(1.7);transform-origin:80px 78px}}@media(prefers-reduced-motion:reduce){.mascot-float,.mascot-lids,.is-waving .mascot-arm-right,.is-thinking .mascot-thought,.is-speaking .mascot-sound,.is-speaking .mascot-mouth{animation:none}}
</style>

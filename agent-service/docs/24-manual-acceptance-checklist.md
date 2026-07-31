# Manual acceptance checklist

- [ ] Entry and layout: every ordinary user-layout page has exactly one lower-right assistant button; it opens and closes a fixed drawer without navigation or changing the underlying page width. Login and administrator layouts do not show it.
- [ ] Navigation: the top user navigation has no ordinary “校园助手” item; opening an old `/assistant` link opens the drawer and returns to the home route without a redirect loop.
- [ ] Responsive UI: desktop uses the right-side `420px` drawer with margins; medium screens keep a viewport-safe width; phones use a full-screen drawer with an obvious close control and a visible composer above the safe area/soft keyboard.
- [ ] Conversation UI: the empty state shows the welcome card and exactly four main questions; user bubbles align right, assistant bubbles align left, and system/errors use distinct styles. Tool progress is Chinese only and never exposes an internal tool name.
- [ ] Public: today promotions, active coupons, shop search/detail, hot explore, and nearby shops.
- [ ] Identity: unauthenticated personal question prompts for login; ordinary user can query own dorm, coupons, and recent orders; expired login follows the existing login flow.
- [ ] Conversation: Enter sends, Shift+Enter creates a line break, empty input cannot send, generation prevents duplicate sends, stop generation works, failed questions remain available for resend, and close/minimize preserve the current conversation.
- [ ] Conversation lifecycle: clear conversation, logout, and account switch cancel the active stream and remove only the browser `conversation_id`; complete messages are not persisted in sessionStorage.
- [ ] Failure: Agent unavailable, Spring unavailable, and model unavailable show safe retry guidance without inventing business data or changing Qinghe state.
- [ ] Security: prompt injection, token/system-prompt theft, admin route, Shell, and SQL requests are blocked; no answer contains token, key, internal URL, raw ToolResult, or stack.

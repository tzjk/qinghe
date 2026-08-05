# Vue integration

## Global floating assistant

`frontend/src/layouts/UserLayout.vue` mounts one `FloatingCampusAssistant` after the ordinary user `router-view`. It is therefore available on all user-layout pages, including public pages, without changing the page width or navigating away from the current business view. The independent administrator layout never mounts this component, and the login page remains outside the user layout.

The entry is a fixed lower-right inline-SVG robot button. It opens a fixed right-side drawer on desktop (`20px` margins, `420px` width) and a full-screen panel on mobile. Close and minimize retain the current in-memory conversation; a completed response while hidden marks the button with an unread dot. The drawer contains a compact header, welcome card with four main questions, plain-text message list, Chinese tool-query status, and a bottom-fixed composer. The `/assistant` route remains as a compatibility shell: it opens the shared drawer, then replaces the route with the home page, so old links remain usable without keeping a standalone assistant page.

## Streaming, token, and session rules

`useCampusAssistant.js` owns shared open/minimize state, messages, input, loading state, tool status, `conversation_id`, cancellation, clearing, and resend behavior. It continues to call `POST /api/v1/chat/stream` with `fetch`, `ReadableStream`, `TextDecoder`, and `AbortController`; it does not use `EventSource`. The request sends only `message` and optional `conversation_id`. The existing ordinary user token is read only when needed and is sent only in `Authorization: Bearer ...`, never in URLs, message fields, metadata, logging, or additional browser storage. Clear-conversation requests use the same header when a user token exists.

Only `conversation_id` is stored in `sessionStorage`; complete messages remain in memory. User logout and a user-token change both abort an active stream and reset the local assistant conversation. On component unmount the active request is also aborted. Public questions are available before login; common personal-information questions show a login prompt instead of invoking the stream.

Answers are Vue text interpolation only, never `v-html`; no model text executes HTML/scripts, opens links, or acts as a frontend command. Internal tool names are mapped to Chinese query labels. Agent, campus-business-service, and model errors are reduced to safe retryable messages and do not affect orders, cart, dorms, coupons, or login.

## Environment and follow-up verification

`frontend/.env.example` continues to expose `VITE_AGENT_BASE_URL` as the non-secret Agent base URL. No service is started by this UI change. Before release, run the manual checklist below at desktop, medium, and mobile widths, including the `/assistant` compatibility link, user logout/account switching during a stream, unavailable-service responses, and administrator pages.

import json
import time
from collections import OrderedDict
from copy import deepcopy
from typing import Any


_PUBLIC_TOOLS = {"get_today_promotions", "get_active_coupons", "search_shops", "get_shop_detail", "get_shop_goods", "get_hot_explore_posts", "get_nearby_shops"}


class PublicResponseCache:
    """Exact, optional process-local cache. Personal/authenticated tools are never cacheable."""

    def __init__(self, *, enabled: bool, ttl_seconds: int, max_entries: int) -> None:
        self._enabled, self._ttl, self._max = enabled, ttl_seconds, max_entries
        self._items: OrderedDict[str, tuple[float, Any]] = OrderedDict()
        self.hits = 0

    def key_for(self, *, tool_name: str, arguments: dict[str, Any], requires_auth: bool) -> str | None:
        if not self._enabled or requires_auth or tool_name not in _PUBLIC_TOOLS:
            return None
        return tool_name + ":" + json.dumps(arguments, ensure_ascii=False, sort_keys=True, separators=(",", ":"))

    def get(self, key: str | None) -> Any | None:
        if key is None:
            return None
        item = self._items.get(key)
        if item is None or item[0] <= time.monotonic():
            self._items.pop(key, None)
            return None
        self._items.move_to_end(key)
        self.hits += 1
        return deepcopy(item[1])

    def put(self, key: str | None, value: Any) -> None:
        if key is None:
            return
        self._items[key] = (time.monotonic() + self._ttl, deepcopy(value))
        self._items.move_to_end(key)
        while len(self._items) > self._max:
            self._items.popitem(last=False)

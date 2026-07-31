import json
import re
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True, slots=True)
class TokenEstimate:
    tokens: int
    estimated: bool = True


class SafeTokenEstimator:
    """Tokenizer-independent upper estimate. It intentionally declares approximation."""

    _CJK = re.compile(r"[\u3400-\u9fff\uf900-\ufaff]")

    def estimate_text(self, value: str) -> TokenEstimate:
        cjk = len(self._CJK.findall(value))
        other = max(0, len(value) - cjk)
        # CJK is close to one token; other text/json gets a conservative 3 chars/token plus margin.
        return TokenEstimate(max(1, cjk + (other + 2) // 3 + 4))

    def estimate_object(self, value: Any) -> TokenEstimate:
        return self.estimate_text(json.dumps(value, ensure_ascii=False, separators=(",", ":"), default=str))

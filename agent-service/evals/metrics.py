def rate(passed: int, total: int) -> float:
    return round(passed / total if total else 0.0, 4)

# Token optimization benchmark

- case_count: 50
- baseline: {'average_input_tokens': 4372.86, 'p50_input_tokens': 4373, 'p95_input_tokens': 4376, 'average_schema_tokens': 1954.0, 'average_history_tokens': 1855.0, 'average_tool_result_tokens': 565.0}
- optimized: {'average_input_tokens': 318.62, 'p50_input_tokens': 220, 'p95_input_tokens': 647, 'average_schema_tokens': 176.96, 'average_history_tokens': 119.0, 'average_tool_result_tokens': 23.0}
- model_calls_avoided: 21
- routing_distribution: {'deterministic': 17, 'fast': 28, 'standard': 5}
- offline: True
- estimated_token_reduction_ratio: 0.9271
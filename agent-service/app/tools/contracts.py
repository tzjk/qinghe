from dataclasses import dataclass

from app.schemas.tool import ToolResult
from app.tools.registry import ToolRegistry


@dataclass(frozen=True, slots=True)
class ToolContract:
    name: str
    input_schema: dict[str, object]
    description: str
    requires_auth: bool
    result_schema: dict[str, object]


def contract_for(registry: ToolRegistry, name: str) -> ToolContract:
    tool = registry.get(name)
    return ToolContract(
        name=tool.name,
        input_schema=tool.input_model.model_json_schema(),
        description=tool.description,
        requires_auth=tool.requires_auth,
        result_schema=ToolResult.model_json_schema(),
    )


def assert_compatible_contracts(
    mock_registry: ToolRegistry, spring_registry: ToolRegistry, names: tuple[str, ...]
) -> None:
    for name in names:
        assert contract_for(mock_registry, name) == contract_for(spring_registry, name)

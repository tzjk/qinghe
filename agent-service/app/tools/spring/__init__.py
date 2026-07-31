import httpx

from app.clients.qinghe_client import QingheClient
from app.core.config import Settings
from app.tools.registry import ToolRegistry
from app.tools.spring.addresses import GetMyAddressesTool, GetMyDefaultAddressTool
from app.tools.spring.coupons import GetActiveCouponsTool, GetMyCouponWalletTool
from app.tools.spring.dorm import GetMyDormInfoTool
from app.tools.spring.explore import GetHotExplorePostsTool, GetNearbyShopsTool
from app.tools.spring.orders import GetMyOrderDetailTool, GetMyRecentOrdersTool
from app.tools.spring.profile import GetMyProfileTool, GetMyStudentProfileTool
from app.tools.spring.promotions import GetTodayPromotionsTool
from app.tools.spring.shops import GetShopDetailTool, GetShopGoodsTool, SearchShopsTool


def create_spring_registry(
    settings: Settings,
    *,
    transport: httpx.AsyncBaseTransport | None = None,
    data_source: str = "spring",
) -> tuple[ToolRegistry, QingheClient]:
    client = QingheClient(settings, transport=transport, data_source=data_source)
    registry = ToolRegistry()
    for tool in (
        GetTodayPromotionsTool(client),
        GetActiveCouponsTool(client),
        SearchShopsTool(client),
        GetShopDetailTool(client),
        GetShopGoodsTool(client),
        GetNearbyShopsTool(client),
        GetHotExplorePostsTool(client),
        GetMyProfileTool(client),
        GetMyStudentProfileTool(client),
        GetMyDormInfoTool(client),
        GetMyCouponWalletTool(client),
        GetMyRecentOrdersTool(client),
        GetMyOrderDetailTool(client),
        GetMyDefaultAddressTool(client),
        GetMyAddressesTool(client),
    ):
        registry.register(tool)
    return registry, client

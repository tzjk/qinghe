from dataclasses import dataclass
from enum import Enum


class EndpointName(str, Enum):
    COUPONS = "coupons"
    COUPONS_MINE = "coupons_mine"
    SHOPS = "shops"
    SHOP_DETAIL = "shop_detail"
    SHOP_GOODS = "shop_goods"
    NEARBY_SHOPS = "nearby_shops"
    EXPLORE_POSTS = "explore_posts"
    USER_ME = "user_me"
    ORDERS = "orders"
    ORDER_DETAIL = "order_detail"
    STUDENT_PROFILE = "student_profile"
    STUDENT_DORM_ME = "student_dorm_me"
    ADDRESSES = "addresses"


@dataclass(frozen=True, slots=True)
class Endpoint:
    method: str
    path_template: str
    requires_auth: bool

    def path(self, **values: int) -> str:
        return self.path_template.format(**values)


ENDPOINTS: dict[EndpointName, Endpoint] = {
    EndpointName.COUPONS: Endpoint("GET", "/api/coupons", False),
    EndpointName.COUPONS_MINE: Endpoint("GET", "/api/coupons/mine", True),
    EndpointName.SHOPS: Endpoint("GET", "/api/shops", False),
    EndpointName.SHOP_DETAIL: Endpoint("GET", "/api/shops/{id}", False),
    EndpointName.SHOP_GOODS: Endpoint("GET", "/api/shops/{id}/goods", False),
    EndpointName.NEARBY_SHOPS: Endpoint("GET", "/api/explore/shops/nearby", False),
    EndpointName.EXPLORE_POSTS: Endpoint("GET", "/api/explore/posts", False),
    EndpointName.USER_ME: Endpoint("GET", "/api/user/me", True),
    EndpointName.ORDERS: Endpoint("GET", "/api/orders", True),
    EndpointName.ORDER_DETAIL: Endpoint("GET", "/api/orders/{orderId}", True),
    EndpointName.STUDENT_PROFILE: Endpoint("GET", "/api/student/profile", True),
    EndpointName.STUDENT_DORM_ME: Endpoint("GET", "/api/student/dorm/me", True),
    EndpointName.ADDRESSES: Endpoint("GET", "/api/addresses", True),
}

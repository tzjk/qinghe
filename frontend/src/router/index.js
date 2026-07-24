import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/token'
import { getAdminToken } from '../utils/admin-session'
import UserLayout from '../layouts/UserLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import HomeView from '../views/HomeView.vue'
import ShopListView from '../views/ShopListView.vue'
import ShopDetailView from '../views/ShopDetailView.vue'
import CartView from '../views/CartView.vue'
import CheckoutView from '../views/CheckoutView.vue'
import OrderSuccessView from '../views/OrderSuccessView.vue'
import OrdersView from '../views/OrdersView.vue'
import CouponsView from '../views/CouponsView.vue'
import BlogsView from '../views/BlogsView.vue'
import ProfileView from '../views/ProfileView.vue'
import AddressView from '../views/AddressView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import AdminResourceView from '../views/AdminResourceView.vue'
import AdminDormAssetView from '../views/AdminDormAssetView.vue'
import AdminDormCheckinView from '../views/AdminDormCheckinView.vue'
import AdminStudentAcademicView from '../views/AdminStudentAcademicView.vue'
import AdminShopView from '../views/AdminShopView.vue'
import AdminGoodsView from '../views/AdminGoodsView.vue'
import AdminGoodsCategoryView from '../views/AdminGoodsCategoryView.vue'
import AdminOrderView from '../views/AdminOrderView.vue'
import AdminCouponView from '../views/AdminCouponView.vue'
import LoginView from '../views/LoginView.vue'
import AdminLoginView from '../views/AdminLoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import InitialProfileView from '../views/InitialProfileView.vue'
import DormScanView from '../views/DormScanView.vue'
import DormMeView from '../views/DormMeView.vue'

const router = createRouter({ history: createWebHistory(), routes: [
  { path: '/login', name: 'login', component: LoginView, meta: { title: '登录' } },
  { path: '/register', name: 'register', component: RegisterView, meta: { title: '注册' } },
  { path: '/profile/complete', name: 'profile-complete', component: InitialProfileView, meta: { title: '完善资料', requiresUser: true } },
  { path: '/admin/login', name: 'admin-login', component: AdminLoginView, meta: { title: '管理登录' } },
  { path: '/', component: UserLayout, children: [
    { path: '', name: 'home', component: HomeView, meta: { title: '首页' } },
    { path: 'shops', name: 'shops', component: ShopListView, meta: { title: '商铺' } },
    { path: 'shops/:id', name: 'shop-detail', component: ShopDetailView, meta: { title: '商铺详情' } },
    { path: 'cart', name: 'cart', component: CartView, meta: { title: '购物车', requiresUser: true } },
    { path: 'checkout', name: 'checkout', component: CheckoutView, meta: { title: '结算', requiresUser: true } },
    { path: 'orders/:orderId/success', name: 'order-success', component: OrderSuccessView, meta: { title: '下单成功', requiresUser: true } },
    { path: 'orders', name: 'orders', component: OrdersView, meta: { title: '订单', requiresUser: true } },
    { path: 'coupons', name: 'coupons', component: CouponsView, meta: { title: '优惠券' } },
    { path: 'blogs', name: 'blogs', component: BlogsView, meta: { title: '探店' } },
    { path: 'profile', name: 'profile', component: ProfileView, meta: { title: '我的', requiresUser: true } },
    { path: 'dorm/scan', name: 'dorm-scan', component: DormScanView, meta: { title: '扫码入住', requiresUser: true } },
    { path: 'dorm/me', name: 'dorm-me', component: DormMeView, meta: { title: '我的宿舍', requiresUser: true } },
    { path: 'profile/addresses', name: 'addresses', component: AddressView, meta: { title: '我的地址', requiresUser: true } }
  ] },
  { path: '/admin', component: AdminLayout, meta: { requiresAdmin: true }, children: [
    { path: '', name: 'admin-dashboard', component: AdminDashboardView, meta: { title: '后台', requiresAdmin: true } },
    { path: 'categories', component: AdminResourceView, meta: { title: '分类管理', requiresAdmin: true } },
    { path: 'shops', name: 'admin-shops', component: AdminShopView, meta: { title: '商铺管理', requiresAdmin: true } },
    { path: 'goods', name: 'admin-goods', component: AdminGoodsView, meta: { title: '商品管理', requiresAdmin: true } },
    { path: 'goods/categories', name: 'admin-goods-categories', component: AdminGoodsCategoryView, meta: { title: '商品分类', requiresAdmin: true } },
    { path: 'orders', name: 'admin-orders', component: AdminOrderView, meta: { title: '订单管理', requiresAdmin: true } },
    { path: 'coupons', name: 'admin-coupons', component: AdminCouponView, meta: { title: '优惠管理', requiresAdmin: true } },
    { path: 'users', component: AdminResourceView, meta: { title: '用户查询', requiresAdmin: true } },
    { path: 'blogs', component: AdminResourceView, meta: { title: '内容管理', requiresAdmin: true } },
    { path: 'comments', component: AdminResourceView, meta: { title: '评论管理', requiresAdmin: true } },
    { path: 'dorm', name: 'admin-dorm', component: AdminDormAssetView, meta: { title: '宿舍基础管理', requiresAdmin: true } }
    ,{ path: 'dorm/students', name: 'admin-dorm-students', component: AdminStudentAcademicView, meta: { title: '学生学籍', requiresAdmin: true } }
    ,{ path: 'dorm/checkins', name: 'admin-dorm-checkins', component: AdminDormCheckinView, meta: { title: '入住管理', requiresAdmin: true } }
  ] }
] })

router.beforeEach((to) => {
  const requiresAdmin = to.matched.some((item) => item.meta.requiresAdmin)
  const requiresUser = to.matched.some((item) => item.meta.requiresUser)
  if (to.name === 'register' && getToken()) return { name: 'home' }
  if (to.name === 'admin-login' && getAdminToken()) return { name: 'admin-dashboard' }
  if (requiresAdmin) return getAdminToken() ? true : { name: 'admin-login', query: { redirect: to.fullPath } }
  return requiresUser && !getToken() ? { name: 'login', query: { redirect: to.fullPath } } : true
})

export default router

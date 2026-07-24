package com.qinghe.life.vo;

import com.qinghe.life.entity.UserCoupon;
import com.qinghe.life.enums.CouponClaimStatus;
import lombok.Data;

@Data
public class CouponClaimVO {
    private boolean claimed;
    private Long userCouponId;
    private String claimStatus;
    private String message;

    public static CouponClaimVO success(UserCoupon userCoupon) {
        return result(false, userCoupon == null ? null : userCoupon.getId(), CouponClaimStatus.CLAIM_SUCCESS, "领取成功");
    }

    public static CouponClaimVO alreadyClaimed(UserCoupon userCoupon) {
        return result(true, userCoupon == null ? null : userCoupon.getId(), CouponClaimStatus.ALREADY_CLAIMED, "该优惠券已领取，请勿重复领取");
    }

    public static CouponClaimVO unavailable(CouponClaimStatus claimStatus, String message) {
        return result(false, null, claimStatus, message);
    }

    private static CouponClaimVO result(boolean claimed, Long userCouponId, CouponClaimStatus claimStatus, String message) {
        CouponClaimVO view = new CouponClaimVO();
        view.setClaimed(claimed);
        view.setUserCouponId(userCouponId);
        view.setClaimStatus(claimStatus.name());
        view.setMessage(message);
        return view;
    }
}

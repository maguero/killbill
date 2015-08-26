/*
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.coupon.api;

import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.billing.coupon.dao.CouponModelDao;
import org.killbill.billing.entity.EntityBase;

public class DefaultCoupon extends EntityBase implements Coupon {

    private final String couponCode;
    private final String couponName;

    /**
     * This call is used to update an existing coupon
     *
     * @param id   UUID id of the existing coupon to update
     * @param data AccountData new data for the existing coupon
     */
    public DefaultCoupon(final UUID id, final CouponData data) {
        this(id,
             data.getCouponCode(),
             data.getCouponName());
    }

    // This call is used for testing and update from an existing account
    public DefaultCoupon(final UUID id, final String couponCode, final String couponName) {
        this(id,
             null,
             null,
             couponCode,
             couponName);
    }

    public DefaultCoupon(final UUID id, @Nullable final DateTime createdDate, @Nullable final DateTime updatedDate,
                         final String couponCode, final String couponName) {
        super(id, createdDate, updatedDate);
        this.couponCode = couponCode;
        this.couponName = couponName;
    }

    public DefaultCoupon(final CouponModelDao couponModelDao) {
        this(couponModelDao.getId(),
             couponModelDao.getCreatedDate(),
             couponModelDao.getUpdatedDate(),
             couponModelDao.getCouponCode(),
             couponModelDao.getCouponName());
    }

    @Override
    public String getCouponCode() {
        return couponCode;
    }

    @Override
    public String getCouponName() {
        return couponName;
    }

    @Override
    public MutableCouponData toMutableCouponData() {
        return new DefaultMutableCouponData(this);
    }

    /**
     * @param currentCoupon existing account data
     * @return merged coupon data
     */
    @Override
    public Coupon mergeWithDelegate(final Coupon currentCoupon) {
        final DefaultMutableCouponData couponData = new DefaultMutableCouponData(this);

        if (couponCode != null && currentCoupon.getCouponCode() != null && !currentCoupon.getCouponCode().equals(couponCode)) {
            throw new IllegalArgumentException(String.format("Killbill doesn't support updating the coupon code yet: new=%s, current=%s",
                                                             couponCode, currentCoupon.getCouponCode()));
        } else {
            // Default to current value
            couponData.setCouponCode(currentCoupon.getCouponCode());
        }

        // Set all updatable fields with the new values if non null, otherwise defaults to the current values
        couponData.setCouponName(couponName != null ? couponName : currentCoupon.getCouponName());

        return new DefaultCoupon(currentCoupon.getId(), couponData);
    }

    @Override
    public String toString() {
        return "DefaultCoupon [couponCode=" + couponCode +
               ", couponName=" + couponName +
               "]";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final DefaultCoupon that = (DefaultCoupon) o;

        if (couponName != null ? !couponName.equals(that.couponName) : that.couponName != null) {
            return false;
        }
        if (couponCode != null ? !couponCode.equals(that.couponCode) : that.couponCode != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (couponCode != null ? couponCode.hashCode() : 0);
        result = 31 * result + (couponName != null ? couponName.hashCode() : 0);
        return result;
    }
}

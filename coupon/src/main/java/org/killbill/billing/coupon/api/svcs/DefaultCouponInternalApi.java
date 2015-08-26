/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.coupon.api.svcs;

import java.util.UUID;

import javax.inject.Inject;

import org.killbill.billing.ErrorCode;
import org.killbill.billing.callcontext.InternalCallContext;
import org.killbill.billing.callcontext.InternalTenantContext;
import org.killbill.billing.coupon.api.Coupon;
import org.killbill.billing.coupon.api.CouponApiException;
import org.killbill.billing.coupon.api.CouponData;
import org.killbill.billing.coupon.api.CouponInternalApi;
import org.killbill.billing.coupon.api.DefaultCoupon;
import org.killbill.billing.coupon.dao.CouponDao;
import org.killbill.billing.coupon.dao.CouponModelDao;

public class DefaultCouponInternalApi implements CouponInternalApi {

    private final CouponDao couponDao;

    @Inject
    public DefaultCouponInternalApi(final CouponDao couponDao) {
        this.couponDao = couponDao;
    }

    @Override
    public Coupon getCouponById(final UUID couponId, final InternalTenantContext context) throws CouponApiException {
        final CouponModelDao coupon = couponDao.getById(couponId, context);
        if (coupon == null) {
            throw new CouponApiException(ErrorCode.COUPON_DOES_NOT_EXIST_FOR_ID, couponId);
        }
        return new DefaultCoupon(coupon);
    }

    @Override
    public Coupon getCouponByRecordId(final Long recordId, final InternalTenantContext context) throws CouponApiException {
        final CouponModelDao couponModelDao = getCouponModelDaoByRecordId(recordId, context);
        return new DefaultCoupon(couponModelDao);
    }

    @Override
    public void updateCoupon(final String externalKey, final CouponData couponData,
                              final InternalCallContext context) throws CouponApiException {
        final Coupon currentCoupon = getCouponByKey(externalKey, context);
        if (currentCoupon == null) {
            throw new CouponApiException(ErrorCode.COUPON_DOES_NOT_EXIST_FOR_KEY, externalKey);
        }

        // Set unspecified (null) fields to their current values
        final Coupon updatedCoupon = new DefaultCoupon(currentCoupon.getId(), couponData);
        final CouponModelDao couponToUpdate = new CouponModelDao(currentCoupon.getId(), updatedCoupon.mergeWithDelegate(currentCoupon));

        couponDao.update(couponToUpdate, context);
    }

    @Override
    public Coupon getCouponByKey(final String key, final InternalTenantContext context) throws CouponApiException {
        final CouponModelDao couponModelDao = couponDao.getCouponByCode(key, context);
        if (couponModelDao == null) {
            throw new CouponApiException(ErrorCode.COUPON_DOES_NOT_EXIST_FOR_KEY, key);
        }
        return new DefaultCoupon(couponModelDao);
    }

    private CouponModelDao getCouponModelDaoByRecordId(final Long recordId, final InternalTenantContext context) throws CouponApiException {
        final CouponModelDao couponModelDao = couponDao.getByRecordId(recordId, context);
        if (couponModelDao == null) {
            throw new CouponApiException(ErrorCode.COUPON_DOES_NOT_EXIST_FOR_RECORD_ID, recordId);
        }
        return couponModelDao;
    }
}

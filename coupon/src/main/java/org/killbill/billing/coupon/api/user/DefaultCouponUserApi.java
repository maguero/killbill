/*
 * Copyright 2010-2013 Ning, Inc.
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

package org.killbill.billing.coupon.api.user;

import java.util.UUID;

import org.killbill.billing.ErrorCode;
import org.killbill.billing.coupon.api.Coupon;
import org.killbill.billing.coupon.api.CouponApiException;
import org.killbill.billing.coupon.api.CouponData;
import org.killbill.billing.coupon.api.CouponUserApi;
import org.killbill.billing.coupon.api.DefaultCoupon;
import org.killbill.billing.coupon.dao.CouponDao;
import org.killbill.billing.coupon.dao.CouponModelDao;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.InternalCallContextFactory;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.billing.util.entity.dao.DefaultPaginationHelper.SourcePaginationBuilder;

import com.google.common.base.Function;
import com.google.inject.Inject;

import static org.killbill.billing.util.entity.dao.DefaultPaginationHelper.getEntityPaginationNoException;

public class DefaultCouponUserApi implements CouponUserApi {

    private final InternalCallContextFactory internalCallContextFactory;
    private final CouponDao couponDao;

    @Inject
    public DefaultCouponUserApi(final InternalCallContextFactory internalCallContextFactory, final CouponDao couponDao) {
        this.internalCallContextFactory = internalCallContextFactory;
        this.couponDao = couponDao;
    }

    @Override
    public Coupon createCoupon(final CouponData data, final CallContext context) throws CouponApiException {
        // Not transactional, but there is a db constraint on that column
        if (data.getCouponCode() != null && getIdFromCode(data.getCouponCode(), context) != null) {
            throw new CouponApiException(ErrorCode.COUPON_ALREADY_EXISTS, data.getCouponCode());
        }

        final CouponModelDao coupon = new CouponModelDao(data);
        couponDao.create(coupon, internalCallContextFactory.createInternalCallContext(context));

        return new DefaultCoupon(coupon);
    }

    @Override
    public Coupon getCouponByCode(final String couponCode, final TenantContext context) throws CouponApiException {
        final CouponModelDao account = couponDao.getCouponByCode(couponCode, internalCallContextFactory.createInternalTenantContext(context));
        if (account == null) {
            throw new CouponApiException(ErrorCode.COUPON_DOES_NOT_EXIST_FOR_KEY, couponCode);
        }

        return new DefaultCoupon(account);
    }

    @Override
    public Coupon getCouponById(final UUID id, final TenantContext context) throws CouponApiException {
        final CouponModelDao account = couponDao.getById(id, internalCallContextFactory.createInternalTenantContext(context));
        if (account == null) {
            throw new CouponApiException(ErrorCode.COUPON_DOES_NOT_EXIST_FOR_ID, id);
        }

        return new DefaultCoupon(account);
    }

    @Override
    public Pagination<Coupon> searchCoupons(final String searchKey, final Long offset, final Long limit, final TenantContext context) {
        return getEntityPaginationNoException(limit,
                                              new SourcePaginationBuilder<CouponModelDao, CouponApiException>() {
                                                  @Override
                                                  public Pagination<CouponModelDao> build() {
                                                      return couponDao.searchCoupons(searchKey, offset, limit, internalCallContextFactory.createInternalTenantContext(context));
                                                  }
                                              },
                                              new Function<CouponModelDao, Coupon>() {
                                                  @Override
                                                  public Coupon apply(final CouponModelDao couponModelDao) {
                                                      return new DefaultCoupon(couponModelDao);
                                                  }
                                              }
                                             );
    }

    @Override
    public Pagination<Coupon> getCoupons(final Long offset, final Long limit, final TenantContext context) {
        return getEntityPaginationNoException(limit,
                                              new SourcePaginationBuilder<CouponModelDao, CouponApiException>() {
                                                  @Override
                                                  public Pagination<CouponModelDao> build() {
                                                      return couponDao.get(offset, limit, internalCallContextFactory.createInternalTenantContext(context));
                                                  }
                                              },
                                              new Function<CouponModelDao, Coupon>() {
                                                  @Override
                                                  public Coupon apply(final CouponModelDao couponModelDao) {
                                                      return new DefaultCoupon(couponModelDao);
                                                  }
                                              }
                                             );
    }

    @Override
    public UUID getIdFromCode(final String couponCode, final TenantContext context) throws CouponApiException {
        return couponDao.getIdFromCode(couponCode, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public void updateCoupon(final Coupon coupon, final CallContext context) throws CouponApiException {
        updateCoupon(coupon.getId(), coupon, context);
    }

    @Override
    public void updateCoupon(final UUID couponId, final CouponData couponData, final CallContext context) throws CouponApiException {
        final Coupon currentCoupon = getCouponById(couponId, context);
        if (currentCoupon == null) {
            throw new CouponApiException(ErrorCode.COUPON_DOES_NOT_EXIST_FOR_ID, couponId);
        }

        updateCoupon(currentCoupon, couponData, context);
    }

    @Override
    public void updateCoupon(final String externalKey, final CouponData accountData, final CallContext context) throws CouponApiException {
        final Coupon currentCoupon = getCouponByCode(externalKey, context);
        if (currentCoupon == null) {
            throw new CouponApiException(ErrorCode.COUPON_DOES_NOT_EXIST_FOR_KEY, externalKey);
        }

        updateCoupon(currentCoupon, accountData, context);
    }

    private void updateCoupon(final Coupon currentAccount, final CouponData accountData, final CallContext context) throws CouponApiException {
        final Coupon updatedCoupon = new DefaultCoupon(currentAccount.getId(), accountData);

        // Set unspecified (null) fields to their current values
        final Coupon mergedCoupon = updatedCoupon.mergeWithDelegate(currentAccount);

        final CouponModelDao updatedCouponModelDao = new CouponModelDao(currentAccount.getId(), mergedCoupon);

        couponDao.update(updatedCouponModelDao, internalCallContextFactory.createInternalCallContext(updatedCouponModelDao.getId(), context));
    }
}

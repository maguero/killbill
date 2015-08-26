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

package org.killbill.billing.coupon.dao;

import java.util.UUID;

import org.killbill.billing.callcontext.InternalCallContext;
import org.killbill.billing.callcontext.InternalTenantContext;
import org.killbill.billing.coupon.api.Coupon;
import org.killbill.billing.coupon.api.CouponApiException;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.billing.util.entity.dao.EntityDao;

public interface CouponDao extends EntityDao<CouponModelDao, Coupon, CouponApiException> {

    public CouponModelDao getCouponByCode(String couponCode, InternalTenantContext context);

    public Pagination<CouponModelDao> searchCoupons(String searchKey, Long offset, Long limit, InternalTenantContext context);

    /**
     * @throws CouponApiException when couponCode is null
     */
    public UUID getIdFromCode(String couponCode, InternalTenantContext context) throws CouponApiException;

    public void update(CouponModelDao coupon, InternalCallContext context) throws CouponApiException;

}

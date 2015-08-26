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

import org.killbill.billing.callcontext.InternalCallContext;
import org.killbill.billing.callcontext.InternalTenantContext;

public interface CouponInternalApi {

    public Coupon getCouponByKey(String key, InternalTenantContext context) throws CouponApiException;

    public Coupon getCouponById(UUID accountId, InternalTenantContext context) throws CouponApiException;

    public Coupon getCouponByRecordId(Long recordId, InternalTenantContext context) throws CouponApiException;

    public void updateCoupon(String key, CouponData couponData, InternalCallContext context) throws CouponApiException;
}

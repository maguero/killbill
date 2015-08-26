/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

package org.killbill.billing.coupon.glue;

import org.killbill.billing.coupon.api.CouponInternalApi;
import org.killbill.billing.coupon.api.CouponService;
import org.killbill.billing.coupon.api.CouponUserApi;
import org.killbill.billing.coupon.api.DefaultCouponService;
import org.killbill.billing.coupon.api.svcs.DefaultCouponInternalApi;
import org.killbill.billing.coupon.api.user.DefaultCouponUserApi;
import org.killbill.billing.coupon.dao.CouponDao;
import org.killbill.billing.coupon.dao.DefaultCouponDao;
import org.killbill.billing.glue.CouponModule;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.util.glue.KillBillModule;

public class DefaultCouponModule extends KillBillModule implements CouponModule {

    public DefaultCouponModule(final KillbillConfigSource configSource) {
        super(configSource);
    }

    private void installConfig() {
    }

    protected void installCouponDao() {
        bind(CouponDao.class).to(DefaultCouponDao.class).asEagerSingleton();
    }

    @Override
    public void installCouponUserApi() {
        bind(CouponUserApi.class).to(DefaultCouponUserApi.class).asEagerSingleton();
    }

    @Override
    public void installInternalApi() {
        bind(CouponInternalApi.class).to(DefaultCouponInternalApi.class).asEagerSingleton();
    }

    private void installCouponService() {
        bind(CouponService.class).to(DefaultCouponService.class).asEagerSingleton();
    }

    @Override
    protected void configure() {
        installConfig();
        installCouponDao();
        installCouponService();
        installCouponUserApi();
        installInternalApi();
    }
}

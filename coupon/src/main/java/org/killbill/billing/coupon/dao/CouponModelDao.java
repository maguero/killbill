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

package org.killbill.billing.coupon.dao;

import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.billing.coupon.api.Coupon;
import org.killbill.billing.coupon.api.CouponData;
import org.killbill.billing.util.UUIDs;
import org.killbill.billing.util.dao.TableName;
import org.killbill.billing.util.entity.dao.EntityModelDao;
import org.killbill.billing.util.entity.dao.EntityModelDaoBase;

import com.google.common.base.MoreObjects;

public class CouponModelDao extends EntityModelDaoBase implements EntityModelDao<Coupon> {

    private String couponCode;
    private String couponName;

    public CouponModelDao() { /* For the DAO mapper */ }

    public CouponModelDao(final UUID id, final DateTime createdDate, final DateTime updatedDate, final String couponCode,
                          final String couponName) {
        super(id, createdDate, updatedDate);
        this.couponCode = MoreObjects.firstNonNull(couponCode, id.toString());
        this.couponName = couponName;
    }

    public CouponModelDao(final UUID id, @Nullable final DateTime createdDate, final DateTime updatedDate, final CouponData coupon) {
        this(id,
             createdDate,
             updatedDate,
             coupon.getCouponCode(),
             coupon.getCouponName());
    }

    public CouponModelDao(final UUID id, final CouponData coupon) {
        this(id, null, null, coupon);
    }

    public CouponModelDao(final CouponData coupon) {
        this(UUIDs.randomUUID(), coupon);
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(final String couponCode) {
        this.couponCode = couponCode;
    }

    public String getCouponName() {
        return couponName;
    }

    public void setCouponName(final String couponName) {
        this.couponName = couponName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CouponModelDao");
        sb.append("{couponCode='").append(couponCode).append('\'');
        sb.append(", couponName='").append(couponName).append('\'');
        sb.append('}');
        return sb.toString();
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

        final CouponModelDao that = (CouponModelDao) o;

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

    @Override
    public TableName getTableName() {
        return TableName.COUPON;
    }

    @Override
    public TableName getHistoryTableName() {
        return TableName.COUPON_HISTORY;
    }
}

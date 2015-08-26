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

import org.killbill.billing.coupon.api.CouponData;
import org.killbill.billing.coupon.dao.CouponModelDao;
import org.killbill.billing.events.BusEventBase;
import org.killbill.billing.events.CouponCreationInternalEvent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DefaultCouponCreationEvent extends BusEventBase implements CouponCreationInternalEvent {

    private final UUID id;
    private final CouponData data;

    @JsonCreator
    public DefaultCouponCreationEvent(@JsonProperty("data") final DefaultCouponData data,
                                      @JsonProperty("id") final UUID id,
                                      @JsonProperty("searchKey1") final Long searchKey1,
                                      @JsonProperty("searchKey2") final Long searchKey2,
                                      @JsonProperty("userToken") final UUID userToken) {
        super(searchKey1, searchKey2, userToken);
        this.id = id;
        this.data = data;
    }

    @JsonIgnore
    @Override
    public BusInternalEventType getBusEventType() {
        return BusInternalEventType.COUPON_CREATE;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public CouponData getData() {
        return data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultCouponCreationEvent other = (DefaultCouponCreationEvent) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public static class DefaultCouponData implements CouponData {

        private final String couponCode;
        private final String couponName;

        public DefaultCouponData(final CouponModelDao d) {
            this(d.getCouponCode(),
                 d.getCouponName());
        }

        @JsonCreator
        public DefaultCouponData(@JsonProperty("couponCode") final String couponCode,
                                 @JsonProperty("couponName") final String couponName) {
            this.couponCode = couponCode;
            this.couponName = couponName;
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
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final DefaultCouponData that = (DefaultCouponData) o;

            if (couponCode != null ? !couponCode.equals(that.couponCode) : that.couponCode != null) {
                return false;
            }
            if (couponName != null ? !couponName.equals(that.couponName) : that.couponName != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = couponCode != null ? couponCode.hashCode() : 0;
            result = 31 * result + (couponName != null ? couponName.hashCode() : 0);
            return result;
        }
    }
}

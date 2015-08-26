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

package org.killbill.billing.jaxrs.json;

import org.killbill.billing.coupon.api.Coupon;
import org.killbill.billing.coupon.api.CouponData;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class CouponJson extends JsonBase {

    @ApiModelProperty(dataType = "java.util.UUID")
    private final String couponId;
    private final String couponCode;
    private final String couponName;

    public CouponJson(final Coupon coupon) {
        this.couponId = coupon.getId().toString();
        this.couponCode = coupon.getCouponCode();
        this.couponName = coupon.getCouponName();
    }

    @JsonCreator
    public CouponJson(@JsonProperty("couponId") final String couponId,
                      @JsonProperty("couponCode") final String couponCode,
                      @JsonProperty("couponName") final String couponName) {
        this.couponCode = couponCode;
        this.couponId = couponId;
        this.couponName = couponName;
    }

    public CouponData toCouponData() {
        return new CouponData() {

            @Override
            public String getCouponCode() {
                return couponCode;
            }

            @Override
            public String getCouponName() {
                return couponName;
            }
        };
    }

    public String getCouponId() {
        return couponId;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getCouponName() {
        return couponName;
    }

    @Override
    public String toString() {
        return "CouponJson{" +
               "couponId='" + couponId + '\'' +
               ", couponCode='" + couponCode + '\'' +
               ", couponName='" + couponName + '\'' +
               '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CouponJson that = (CouponJson) o;

        if (couponId != null ? !couponId.equals(that.couponId) : that.couponId != null) {
            return false;
        }
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
        int result = couponId != null ? couponId.hashCode() : 0;
        result = 31 * result + (couponCode != null ? couponCode.hashCode() : 0);
        result = 31 * result + (couponName != null ? couponName.hashCode() : 0);
        return result;
    }
}

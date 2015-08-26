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

package org.killbill.billing.coupon.api.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.killbill.billing.coupon.api.DefaultChangedField;
import org.killbill.billing.coupon.dao.CouponModelDao;
import org.killbill.billing.events.BusEventBase;
import org.killbill.billing.events.ChangedField;
import org.killbill.billing.events.CouponChangeInternalEvent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class DefaultCouponChangeEvent extends BusEventBase implements CouponChangeInternalEvent {

    private final List<ChangedField> changedFields;
    private final UUID couponId;

    @JsonCreator
    public DefaultCouponChangeEvent(@JsonProperty("changeFields") final List<ChangedField> changedFields,
                                    @JsonProperty("couponId") final UUID couponId,
                                    @JsonProperty("searchKey1") final Long searchKey1,
                                    @JsonProperty("searchKey2") final Long searchKey2,
                                    @JsonProperty("userToken") final UUID userToken) {
        super(searchKey1, searchKey2, userToken);
        this.couponId = couponId;
        this.changedFields = changedFields;
    }

    public DefaultCouponChangeEvent(final UUID id, final CouponModelDao oldData, final CouponModelDao newData, final Long searchKey1, final Long searchKey2, final UUID userToken) {
        super(searchKey1, searchKey2, userToken);
        this.couponId = id;
        this.changedFields = calculateChangedFields(oldData, newData);
    }

    @JsonIgnore
    @Override
    public BusInternalEventType getBusEventType() {
        return BusInternalEventType.COUPON_CHANGE;
    }

    @Override
    public UUID getCouponId() {
        return couponId;
    }

    @JsonDeserialize(contentAs = DefaultChangedField.class)
    @Override
    public List<ChangedField> getChangedFields() {
        return changedFields;
    }

    @JsonIgnore
    @Override
    public boolean hasChanges() {
        return (changedFields.size() > 0);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + ((couponId == null) ? 0 : couponId.hashCode());
        result = prime * result
                 + ((changedFields == null) ? 0 : changedFields.hashCode());
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
        final DefaultCouponChangeEvent other = (DefaultCouponChangeEvent) obj;
        if (couponId == null) {
            if (other.couponId != null) {
                return false;
            }
        } else if (!couponId.equals(other.couponId)) {
            return false;
        }
        if (changedFields == null) {
            if (other.changedFields != null) {
                return false;
            }
        } else if (!changedFields.equals(other.changedFields)) {
            return false;
        }
        return true;
    }

    private List<ChangedField> calculateChangedFields(final CouponModelDao oldData, final CouponModelDao newData) {

        final List<ChangedField> tmpChangedFields = new ArrayList<ChangedField>();

        addIfValueChanged(tmpChangedFields, "couponCode",
                          oldData.getCouponCode(), newData.getCouponCode());

        addIfValueChanged(tmpChangedFields, "couponName",
                          oldData.getCouponName(), newData.getCouponName());

        return tmpChangedFields;
    }

    private void addIfValueChanged(final List<ChangedField> inputList, final String key, final String oldData, final String newData) {
        // If both null => no changes
        if (newData == null && oldData == null) {
            // If only one is null
        } else if (newData == null || oldData == null) {
            inputList.add(new DefaultChangedField(key, oldData, newData));
            // If neither are null we can safely compare values
        } else if (!newData.equals(oldData)) {
            inputList.add(new DefaultChangedField(key, oldData, newData));
        }
    }
}

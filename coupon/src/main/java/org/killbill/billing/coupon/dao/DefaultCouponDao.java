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

package org.killbill.billing.coupon.dao;

import java.util.Iterator;
import java.util.UUID;

import org.killbill.billing.BillingExceptionBase;
import org.killbill.billing.ErrorCode;
import org.killbill.billing.callcontext.InternalCallContext;
import org.killbill.billing.callcontext.InternalTenantContext;
import org.killbill.billing.coupon.api.Coupon;
import org.killbill.billing.coupon.api.CouponApiException;
import org.killbill.billing.coupon.api.user.DefaultCouponChangeEvent;
import org.killbill.billing.coupon.api.user.DefaultCouponCreationEvent;
import org.killbill.billing.coupon.api.user.DefaultCouponCreationEvent.DefaultCouponData;
import org.killbill.billing.events.CouponChangeInternalEvent;
import org.killbill.billing.events.CouponCreationInternalEvent;
import org.killbill.billing.util.audit.ChangeType;
import org.killbill.billing.util.cache.CacheControllerDispatcher;
import org.killbill.billing.util.callcontext.InternalCallContextFactory;
import org.killbill.billing.util.dao.NonEntityDao;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.billing.util.entity.dao.DefaultPaginationSqlDaoHelper.PaginationIteratorBuilder;
import org.killbill.billing.util.entity.dao.EntityDaoBase;
import org.killbill.billing.util.entity.dao.EntitySqlDaoTransactionWrapper;
import org.killbill.billing.util.entity.dao.EntitySqlDaoTransactionalJdbiWrapper;
import org.killbill.billing.util.entity.dao.EntitySqlDaoWrapperFactory;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBus.EventBusException;
import org.killbill.clock.Clock;
import org.skife.jdbi.v2.IDBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DefaultCouponDao extends EntityDaoBase<CouponModelDao, Coupon, CouponApiException> implements CouponDao {

    private static final Logger log = LoggerFactory.getLogger(DefaultCouponDao.class);

    private final PersistentBus eventBus;
    private final InternalCallContextFactory internalCallContextFactory;

    @Inject
    public DefaultCouponDao(final IDBI dbi, final PersistentBus eventBus, final Clock clock, final CacheControllerDispatcher cacheControllerDispatcher,
                            final InternalCallContextFactory internalCallContextFactory, final NonEntityDao nonEntityDao) {
        super(new EntitySqlDaoTransactionalJdbiWrapper(dbi, clock, cacheControllerDispatcher, nonEntityDao), CouponSqlDao.class);
        this.eventBus = eventBus;
        this.internalCallContextFactory = internalCallContextFactory;
    }

    @Override
    protected CouponApiException generateAlreadyExistsException(final CouponModelDao coupon, final InternalCallContext context) {
        return new CouponApiException(ErrorCode.COUPON_ALREADY_EXISTS, coupon.getCouponCode());
    }

    @Override
    protected void postBusEventFromTransaction(final CouponModelDao coupon, final CouponModelDao savedCoupon, final ChangeType changeType,
                                               final EntitySqlDaoWrapperFactory entitySqlDaoWrapperFactory, final InternalCallContext context) throws BillingExceptionBase {
        // This is only called for the create call (see update below)
        switch (changeType) {
            case INSERT:
                break;
            default:
                return;
        }

        final Long recordId = entitySqlDaoWrapperFactory.become(CouponSqlDao.class).getRecordId(savedCoupon.getId().toString(), context);
        // We need to re-hydrate the callcontext with the coupon record id
        final InternalCallContext rehydratedContext = internalCallContextFactory.createInternalCallContext(recordId, context);
        final CouponCreationInternalEvent creationEvent = new DefaultCouponCreationEvent(new DefaultCouponData(savedCoupon), savedCoupon.getId(),
                                                                                           rehydratedContext.getAccountRecordId(), rehydratedContext.getTenantRecordId(), rehydratedContext.getUserToken());
        try {
            eventBus.postFromTransaction(creationEvent, entitySqlDaoWrapperFactory.getHandle().getConnection());
        } catch (final EventBusException e) {
            log.warn("Failed to post account creation event for account " + savedCoupon.getId(), e);
        }
    }

    @Override
    public CouponModelDao getCouponByCode(final String key, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<CouponModelDao>() {
            @Override
            public CouponModelDao inTransaction(final EntitySqlDaoWrapperFactory entitySqlDaoWrapperFactory) throws Exception {
                return entitySqlDaoWrapperFactory.become(CouponSqlDao.class).getCouponByCode(key, context);
            }
        });
    }

    @Override
    public Pagination<CouponModelDao> searchCoupons(final String searchKey, final Long offset, final Long limit, final InternalTenantContext context) {
        return paginationHelper.getPagination(CouponSqlDao.class,
                                              new PaginationIteratorBuilder<CouponModelDao, Coupon, CouponSqlDao>() {
                                                  @Override
                                                  public Long getCount(final CouponSqlDao accountSqlDao, final InternalTenantContext context) {
                                                      return accountSqlDao.getSearchCount(searchKey, String.format("%%%s%%", searchKey), context);
                                                  }

                                                  @Override
                                                  public Iterator<CouponModelDao> build(final CouponSqlDao accountSqlDao, final Long limit, final InternalTenantContext context) {
                                                      return accountSqlDao.search(searchKey, String.format("%%%s%%", searchKey), offset, limit, context);
                                                  }
                                              },
                                              offset,
                                              limit,
                                              context);
    }

    @Override
    public UUID getIdFromCode(final String couponCode, final InternalTenantContext context) throws CouponApiException {
        if (couponCode == null) {
            throw new CouponApiException(ErrorCode.COUPON_CANNOT_MAP_NULL_KEY, "");
        }

        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<UUID>() {
            @Override
            public UUID inTransaction(final EntitySqlDaoWrapperFactory entitySqlDaoWrapperFactory) throws Exception {
                return entitySqlDaoWrapperFactory.become(CouponSqlDao.class).getIdFromCode(couponCode, context);
            }
        });
    }

    @Override
    public void update(final CouponModelDao specifiedCoupon, final InternalCallContext context) throws CouponApiException {
        transactionalSqlDao.execute(CouponApiException.class, new EntitySqlDaoTransactionWrapper<Void>() {
            @Override
            public Void inTransaction(final EntitySqlDaoWrapperFactory entitySqlDaoWrapperFactory) throws EventBusException, CouponApiException {
                final CouponSqlDao transactional = entitySqlDaoWrapperFactory.become(CouponSqlDao.class);

                final UUID couponId = specifiedCoupon.getId();
                final CouponModelDao currentCoupon = transactional.getById(couponId.toString(), context);
                if (currentCoupon == null) {
                    throw new CouponApiException(ErrorCode.COUPON_DOES_NOT_EXIST_FOR_ID, couponId);
                }

                transactional.update(specifiedCoupon, context);

                final CouponChangeInternalEvent changeEvent = new DefaultCouponChangeEvent(couponId,
                                                                                             currentCoupon,
                                                                                             specifiedCoupon,
                                                                                             context.getAccountRecordId(),
                                                                                             context.getTenantRecordId(),
                                                                                             context.getUserToken()
                );
                try {
                    eventBus.postFromTransaction(changeEvent, entitySqlDaoWrapperFactory.getHandle().getConnection());
                } catch (final EventBusException e) {
                    log.warn("Failed to post coupon change event for coupon " + couponId, e);
                }

                return null;
            }
        });
    }
}

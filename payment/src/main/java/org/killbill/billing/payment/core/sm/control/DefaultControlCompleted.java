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

package org.killbill.billing.payment.core.sm.control;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.automaton.Operation.OperationCallback;
import org.killbill.automaton.OperationException;
import org.killbill.automaton.OperationResult;
import org.killbill.automaton.State;
import org.killbill.automaton.State.EnteringStateCallback;
import org.killbill.automaton.State.LeavingStateCallback;
import org.killbill.billing.ObjectType;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.payment.core.sm.PaymentStateContext;
import org.killbill.billing.payment.core.sm.PluginControlPaymentAutomatonRunner;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.core.sm.control.OperationControlCallback.DefaultPaymentControlContext;
import org.killbill.billing.payment.dao.PaymentAttemptModelDao;
import org.killbill.billing.payment.dao.PaymentTransactionModelDao;
import org.killbill.billing.payment.dao.PluginPropertySerializer;
import org.killbill.billing.payment.dao.PluginPropertySerializer.PluginPropertySerializerException;
import org.killbill.billing.payment.retry.BaseRetryService.RetryServiceScheduler;
import org.killbill.billing.routing.plugin.api.OnFailurePaymentRoutingResult;
import org.killbill.billing.routing.plugin.api.PaymentRoutingApiException;
import org.killbill.billing.routing.plugin.api.PaymentRoutingContext;
import org.killbill.billing.routing.plugin.api.PaymentRoutingPluginApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class DefaultControlCompleted implements EnteringStateCallback {

    private static final Logger logger = LoggerFactory.getLogger(DefaultControlCompleted.class);

    private final PaymentStateControlContext paymentStateContext;
    private final RetryServiceScheduler retryServiceScheduler;
    private final OSGIServiceRegistration<PaymentRoutingPluginApi> paymentControlPluginRegistry;
    private final State retriedState;
    private final State abortedState;

    private PluginControlPaymentAutomatonRunner retryablePaymentAutomatonRunner;

    public DefaultControlCompleted(final PluginControlPaymentAutomatonRunner retryablePaymentAutomatonRunner, final PaymentStateControlContext paymentStateContext,
                                   final State retriedState, final State abortedState, final RetryServiceScheduler retryServiceScheduler, final OSGIServiceRegistration<PaymentRoutingPluginApi> paymentControlPluginRegistry) {
        this.retryablePaymentAutomatonRunner = retryablePaymentAutomatonRunner;
        this.paymentStateContext = paymentStateContext;
        this.retriedState = retriedState;
        this.abortedState = abortedState;
        this.retryServiceScheduler = retryServiceScheduler;
        this.paymentControlPluginRegistry = paymentControlPluginRegistry;
    }

    @Override
    public void enteringState(final State state, final OperationCallback operationCallback, final OperationResult operationResult, final LeavingStateCallback leavingStateCallback) {

        try {
            final DateTime utcNow = retryablePaymentAutomatonRunner.getClock().getUTCNow();

            if (state.getName().equals(retriedState.getName()) || state.getName().equals(abortedState.getName())) {
                try {
                    final byte[] serializedProperties = PluginPropertySerializer.serialize(paymentStateContext.getProperties());

                    final PaymentAttemptModelDao attempt = new PaymentAttemptModelDao(paymentStateContext.getAccount().getId(), paymentStateContext.getPaymentMethodId(),
                                                                                      utcNow, utcNow, paymentStateContext.getPaymentExternalKey(), paymentStateContext.getTransactionId(),
                                                                                      paymentStateContext.getPaymentTransactionExternalKey(), paymentStateContext.getTransactionType(), state.getName(),
                                                                                      paymentStateContext.getAmount(), paymentStateContext.getCurrency(),
                                                                                      paymentStateContext.getPaymentControlPluginNames(), serializedProperties);
                    retryablePaymentAutomatonRunner.getPaymentDao().insertPaymentAttemptWithProperties(attempt, paymentStateContext.getInternalCallContext());
                    paymentStateContext.setAttemptId(attempt.getId());

                } catch (final PluginPropertySerializerException e) {
                    throw new IllegalStateException("Failed to serialze plugin properties", e);
                }
            }
        } finally {

            final boolean success = paymentStateContext.getCurrentTransaction() != null &&
                                    (paymentStateContext.getCurrentTransaction().getTransactionStatus() == TransactionStatus.SUCCESS || paymentStateContext.getCurrentTransaction().getTransactionStatus() == TransactionStatus.PENDING);

            final BigDecimal processedAmount = success ? paymentStateContext.getCurrentTransaction().getProcessedAmount() : null;
            final Currency processedCurrency = success ? paymentStateContext.getCurrentTransaction().getProcessedCurrency() : null;

            final PaymentRoutingContext paymentControlContext = new DefaultPaymentControlContext(paymentStateContext.getAccount(),
                                                                                                        paymentStateContext.getPaymentMethodId(),
                                                                                                        paymentStateContext.getAttemptId(),
                                                                                                        paymentStateContext.getPaymentId(),
                                                                                                        paymentStateContext.getPaymentExternalKey(),
                                                                                                        paymentStateContext.getTransactionId(),
                                                                                                        paymentStateContext.getPaymentTransactionExternalKey(),
                                                                                                        paymentStateContext.getTransactionType(),
                                                                                                        paymentStateContext.getAmount(),
                                                                                                        paymentStateContext.getCurrency(),
                                                                                                        processedAmount,
                                                                                                        processedCurrency,
                                                                                                        paymentStateContext.getProperties(),
                                                                                                        paymentStateContext.isApiPayment(),
                                                                                                        paymentStateContext.getCallContext());

            if (success) {
                executePluginOnSuccessCalls(paymentStateContext.getPaymentControlPluginNames(), paymentControlContext);
            } else {
                final DateTime retryDate = executePluginOnFailureCalls(paymentStateContext.getPaymentControlPluginNames(), paymentControlContext);
                if (retryDate != null && !isUnknownTransaction()) {

                    //
                    // The result from the doOperationCallback() did not have a chance to call the plugins (onFailureCall) and therefore could not set the retry date which means the state machine
                    // could not correctly enter this method with a RETRIED state so we have to do it manually
                    //
                    retryablePaymentAutomatonRunner.getPaymentDao().updatePaymentAttempt(paymentStateContext.getAttemptId(), paymentStateContext.getTransactionId(), retriedState.getName(), paymentStateContext.getInternalCallContext());
                    retryServiceScheduler.scheduleRetry(ObjectType.PAYMENT_ATTEMPT, paymentStateContext.getAttemptId(), paymentStateContext.getAttemptId(),
                                                        paymentStateContext.getInternalCallContext().getTenantRecordId(),
                                                        paymentStateContext.getPaymentControlPluginNames(), retryDate);
                }
            }
        }
    }

    private void executePluginOnSuccessCalls(final List<String> paymentControlPluginNames, final PaymentRoutingContext paymentControlContext) {
        for (final String pluginName : paymentControlPluginNames) {
            final PaymentRoutingPluginApi plugin = paymentControlPluginRegistry.getServiceForName(pluginName);
            if (plugin != null) {
                try {
                    plugin.onSuccessCall(paymentControlContext, paymentStateContext.getProperties());
                } catch (final PaymentRoutingApiException e) {
                    logger.warn("Plugin " + pluginName + " failed to complete executePluginOnSuccessCalls call for " + paymentControlContext.getPaymentExternalKey(), e);
                }
            }
        }
    }

    private DateTime executePluginOnFailureCalls(final List<String> paymentControlPluginNames, final PaymentRoutingContext paymentControlContext) {
        DateTime candidate = null;
        for (final String pluginName : paymentControlPluginNames) {
            final PaymentRoutingPluginApi plugin = paymentControlPluginRegistry.getServiceForName(pluginName);
            if (plugin != null) {
                try {
                    final OnFailurePaymentRoutingResult result = plugin.onFailureCall(paymentControlContext, paymentStateContext.getProperties());
                    if (candidate == null) {
                        candidate = result.getNextRetryDate();
                    } else if (result.getNextRetryDate() != null) {
                        candidate = candidate.compareTo(result.getNextRetryDate()) > 0 ? result.getNextRetryDate() : candidate;
                    }
                } catch (final PaymentRoutingApiException e) {
                    logger.warn("Plugin " + pluginName + " failed to return next retryDate for payment " + paymentControlContext.getPaymentExternalKey(), e);
                    return candidate;
                }
            }
        }
        return candidate;
    }




    //
    // If we see an UNKNOWN transaction we prevent it to be rescheduled as the Janitor will *try* to fix it, and that could lead to infinite retries from a badly behaved plugin
    // (In other words, plugin should ONLY retry 'known' transaction)
    //
    private boolean isUnknownTransaction() {
        if (paymentStateContext.getCurrentTransaction() != null) {
            return paymentStateContext.getCurrentTransaction().getTransactionStatus() == TransactionStatus.UNKNOWN;
        } else {
            final List<PaymentTransactionModelDao> transactions = retryablePaymentAutomatonRunner.getPaymentDao().getPaymentTransactionsByExternalKey(paymentStateContext.getPaymentTransactionExternalKey(), paymentStateContext.getInternalCallContext());
            return Iterables.any(transactions, new Predicate<PaymentTransactionModelDao>() {
                @Override
                public boolean apply(final PaymentTransactionModelDao input) {
                    return input.getTransactionStatus() == TransactionStatus.UNKNOWN &&
                           // Not strictly required
                           // (Note, we don't match on AttemptId as it is risky, the row on disk would match the first attempt, not necessarily the current one)
                           input.getAccountRecordId().equals(paymentStateContext.getInternalCallContext().getAccountRecordId());
                }
            });
        }
    }
}

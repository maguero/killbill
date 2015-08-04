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

package org.killbill.billing.payment.core.janitor;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.killbill.billing.events.PaymentInternalEvent;
import org.killbill.billing.payment.glue.DefaultPaymentService;
import org.killbill.billing.payment.glue.PaymentModule;
import org.killbill.billing.util.config.PaymentConfig;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueue;
import org.killbill.notificationq.api.NotificationQueueService;
import org.killbill.notificationq.api.NotificationQueueService.NoSuchNotificationQueue;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueAlreadyExists;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes care of incomplete payment/transactions.
 */
public class Janitor {

    private static final Logger log = LoggerFactory.getLogger(Janitor.class);

    public static final String QUEUE_NAME = "janitor";

    private final NotificationQueueService notificationQueueService;
    private final IncompletePaymentTransactionTask incompletePaymentTransactionTask;

    private NotificationQueue janitorQueue;

    private volatile boolean isStopped;

    @Inject
    public Janitor(final NotificationQueueService notificationQueueService,
                   final IncompletePaymentTransactionTask incompletePaymentTransactionTask) {
        this.notificationQueueService = notificationQueueService;
        this.incompletePaymentTransactionTask = incompletePaymentTransactionTask;
        this.isStopped = false;
    }

    public void initialize() throws NotificationQueueAlreadyExists {
        janitorQueue = notificationQueueService.createNotificationQueue(DefaultPaymentService.SERVICE_NAME,
                                                                        QUEUE_NAME,
                                                                        new NotificationQueueHandler() {
                                                                            @Override
                                                                            public void handleReadyNotification(final NotificationEvent notificationKey, final DateTime eventDateTime, final UUID userToken, final Long accountRecordId, final Long tenantRecordId) {
                                                                                if (!(notificationKey instanceof JanitorNotificationKey)) {
                                                                                    log.error("Janitor service received an unexpected event type {}" + notificationKey.getClass().getName());
                                                                                    return;

                                                                                }
                                                                                final JanitorNotificationKey janitorKey = (JanitorNotificationKey) notificationKey;
                                                                                if (janitorKey.getTaskName().equals(incompletePaymentTransactionTask.getClass().toString())) {
                                                                                    incompletePaymentTransactionTask.processNotification(janitorKey, userToken, accountRecordId, tenantRecordId);
                                                                                }
                                                                            }
                                                                        }
                                                                       );
        incompletePaymentTransactionTask.attachJanitorQueue(janitorQueue);
    }

    public void start() {
        if (isStopped) {
            log.warn("Janitor is not a restartable service, and was already started, aborting");
            return;
        }
        janitorQueue.startQueue();
    }

    public void stop() throws NoSuchNotificationQueue {
        if (isStopped) {
            log.warn("Janitor is already in a stopped state");
            return;
        }

        incompletePaymentTransactionTask.stop();

        try {
            if (janitorQueue != null) {
                janitorQueue.stopQueue();
                notificationQueueService.deleteNotificationQueue(DefaultPaymentService.SERVICE_NAME, QUEUE_NAME);
            }
        } finally {
            isStopped = true;
        }
    }

    public void processPaymentEvent(final PaymentInternalEvent event) {
        incompletePaymentTransactionTask.processPaymentEvent(event, janitorQueue);
    }
}

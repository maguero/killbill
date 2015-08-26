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

package org.killbill.billing.jaxrs.resources;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.killbill.billing.ObjectType;
import org.killbill.billing.account.api.AccountUserApi;
import org.killbill.billing.coupon.api.Coupon;
import org.killbill.billing.coupon.api.CouponApiException;
import org.killbill.billing.coupon.api.CouponData;
import org.killbill.billing.coupon.api.CouponUserApi;
import org.killbill.billing.entitlement.api.SubscriptionApi;
import org.killbill.billing.invoice.api.InvoicePaymentApi;
import org.killbill.billing.invoice.api.InvoiceUserApi;
import org.killbill.billing.jaxrs.json.CouponJson;
import org.killbill.billing.jaxrs.util.Context;
import org.killbill.billing.jaxrs.util.JaxrsUriBuilder;
import org.killbill.billing.overdue.OverdueInternalApi;
import org.killbill.billing.payment.api.PaymentApi;
import org.killbill.billing.util.api.AuditUserApi;
import org.killbill.billing.util.api.CustomFieldUserApi;
import org.killbill.billing.util.api.TagUserApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.config.PaymentConfig;
import org.killbill.clock.Clock;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Singleton
@Path(JaxrsResource.COUPONS_PATH)
@Api(value = JaxrsResource.COUPONS_PATH, description = "Operations on coupons")
public class CouponResource extends JaxRsResourceBase {

    private static final String ID_PARAM_NAME = "couponId";

    private final SubscriptionApi subscriptionApi;
    private final InvoiceUserApi invoiceApi;
    private final InvoicePaymentApi invoicePaymentApi;
    private final OverdueInternalApi overdueApi;
    private final CouponUserApi couponUserApi;
    private final PaymentConfig paymentConfig;

    @Inject
    public CouponResource(final JaxrsUriBuilder uriBuilder,
                          final AccountUserApi accountApi,
                          final CouponUserApi couponUserApi,
                          final InvoiceUserApi invoiceApi,
                          final InvoicePaymentApi invoicePaymentApi,
                          final PaymentApi paymentApi,
                          final TagUserApi tagUserApi,
                          final AuditUserApi auditUserApi,
                          final CustomFieldUserApi customFieldUserApi,
                          final SubscriptionApi subscriptionApi,
                          final OverdueInternalApi overdueApi,
                          final Clock clock,
                          final PaymentConfig paymentConfig,
                          final Context context) {
        super(uriBuilder, tagUserApi, customFieldUserApi, auditUserApi, accountApi, paymentApi, clock, context);
        this.subscriptionApi = subscriptionApi;
        this.invoiceApi = invoiceApi;
        this.invoicePaymentApi = invoicePaymentApi;
        this.overdueApi = overdueApi;
        this.paymentConfig = paymentConfig;
        this.couponUserApi = couponUserApi;
    }

    @Timed
    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Retrieve a coupon by code", response = CouponJson.class)
    @ApiResponses(value = {@ApiResponse(code = 404, message = "Coupon not found")})
    public Response getCouponByCode(@QueryParam(QUERY_COUPON_CODE) final String couponCode,
                                    @javax.ws.rs.core.Context final HttpServletRequest request) throws CouponApiException {
        final TenantContext tenantContext = context.createContext(request);
        final Coupon coupon = couponUserApi.getCouponByCode(couponCode, tenantContext);
        final CouponJson couponJson = new CouponJson(coupon);
        return Response.status(Status.OK).entity(couponJson).build();
    }

    @Timed
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create coupon")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid coupon data supplied")})
    public Response createCoupon(final CouponJson json,
                                 @HeaderParam(HDR_CREATED_BY) final String createdBy,
                                 @HeaderParam(HDR_REASON) final String reason,
                                 @HeaderParam(HDR_COMMENT) final String comment,
                                 @javax.ws.rs.core.Context final HttpServletRequest request,
                                 @javax.ws.rs.core.Context final UriInfo uriInfo) throws CouponApiException {
        verifyNonNullOrEmpty(json, "CouponJson body should be specified");

        final CouponData data = json.toCouponData();
        final Coupon coupon = couponUserApi.createCoupon(data, context.createContext(createdBy, reason, comment, request));
        return uriBuilder.buildResponse(uriInfo, CouponResource.class, "getCoupon", coupon.getId());
    }

    @Timed
    @GET
    @Path("/{couponId:" + UUID_PATTERN + "}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Retrieve a coupon by id", response = CouponJson.class)
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid coupon id supplied"),
                           @ApiResponse(code = 404, message = "Coupon not found")})
    public Response getCoupon(@PathParam("couponId") final String couponId,
                               @javax.ws.rs.core.Context final HttpServletRequest request) throws CouponApiException {
        final TenantContext tenantContext = context.createContext(request);
        final Coupon coupon = couponUserApi.getCouponById(UUID.fromString(couponId), tenantContext);
        final CouponJson couponJson = new CouponJson(coupon);
        return Response.status(Status.OK).entity(couponJson).build();
    }

    @Override
    protected ObjectType getObjectType() {
        return ObjectType.COUPON;
    }
}

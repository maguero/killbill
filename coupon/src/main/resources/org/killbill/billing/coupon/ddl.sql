/*! SET storage_engine=INNODB */;

DROP TABLE IF EXISTS coupons;
CREATE TABLE coupons (
    record_id serial unique,
    id varchar(36) NOT NULL,
    coupon_code varchar(36) NOT NULL,
    coupon_name varchar(100) DEFAULT NULL,
    tenant_record_id bigint /*! unsigned */ not null default 0,
    PRIMARY KEY(id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
CREATE UNIQUE INDEX coupons_id ON coupons(id);
CREATE UNIQUE INDEX coupons_code ON coupons(coupon_code);
CREATE INDEX coupons_tenant_record_id ON coupons(tenant_record_id);

DROP TABLE IF EXISTS coupons_history;
CREATE TABLE coupons_history (
    record_id serial unique,
    id varchar(36) NOT NULL,
    target_record_id bigint /*! unsigned */ not null,
    coupon_code varchar(36) NOT NULL,
    coupon_name varchar(100) DEFAULT NULL,
    tenant_record_id bigint /*! unsigned */ not null default 0,
    PRIMARY KEY(record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
CREATE INDEX coupon_history_target_record_id ON coupons_history(target_record_id);
CREATE INDEX coupon_history_tenant_record_id ON coupons_history(tenant_record_id);


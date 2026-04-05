package com.project.ecommerce_backend.core.constants;

/**
 * Uygulama genelindeki metin tabanlı geri bildirimlerin, hata kodlarının ve doğrulama mesajlarının yönetimini sağlayan merkezi sabitler sınıfıdır.
 * Uluslararasılaştırma altyapısının temelini oluşturan bu yapı, kaynak dosyalarındaki anahtar değerleri kod içerisindeki statik sabitlerle eşleştirerek tip güvenliğini artırır. İş mantığı katmanlarını metin bağımlılıklarından arındırır ve tüm sistemin tek bir noktadan yönetilebilir mesaj mimarisine sahip olmasını sağlar.
 */
public class Messages {

    private Messages() {}

    public static class Category {
        private Category() {}

        public static final String CATEGORIES_SUCCESSFULLY_LISTED = "categories.successfully.listed";
        public static final String CATEGORY_ALREADY_EXIST = "category.already.exist";
        public static final String CATEGORY_DOES_NOT_EXIST_WITH_GIVEN_ID = "category.does.not.exist.with.given.ID";
        public static final String CATEGORY_SUCCESSFULLY_ADDED = "category.successfully.added";
        public static final String SOME_CATEGORIES_DOES_NOT_EXIST_WITH_GIVEN_IDS = "some.categories.does.not.exist.with.given.IDs";
        public static final String CATEGORY_SUCCESSFULLY_UPDATED = "category.successfully.updated";
        public static final String CATEGORY_SUCCESSFULLY_DELETED = "category.success.deleted";
        public static final String PARENT_CATEGORY_DOES_NOT_EXIST_WITH_GIVEN_ID = "parent.category.does.not.exist.with.given.ID";
        public static final String CATEGORY_ERROR_HAS_SUB_CATEGORIES = "category.error.has.sub.categories";
        public static final String CATEGORY_ERROR_HAS_PRODUCTS = "category.error.has.products";
        public static final String CATEGORY_DETAIL_SUCCESSFULLY_LISTED = "category.detail.successfully.listed";
        public static final String CATEGORY_SUCCESSFULLY_LISTED_AS_TREE = "category.successfully.listed.as.tree";
        public static final String CATEGORY_ERROR_CANNOT_BE_ITS_OWN_PARENT = "category.error.cannot.be.its.own.parent";
        public static final String CATEGORY_ERROR_CIRCULAR_DEPENDENCY = "category.error.circular.dependency";
        public static final String CATEGORY_NOT_FOUND = "category.not.found";
    }

    public static class Colour {
        private Colour() {}

        public static final String COLOUR_NOT_FOUND_WITH_GIVEN_ID = "colour.not.found.with.given.ID";
    }

    public static class Product {
        private Product() {}

        public static final String PRODUCT_DOES_NOT_EXIST_WITH_GIVEN_ID = "product.does.not.exist.with.given.ID";
        public static final String PRODUCT_ITEM_NOT_FOUND = "product.item.not.found";
        public static final String PRODUCT_DETAIL_SUCCESSFULLY_LISTED = "product.detail.successfully.listed";
        public static final String PRODUCTS_SUCCESSFULLY_LISTED = "products.successfully.listed";
        public static final String PRODUCT_SUCCESSFULLY_ADDED = "product.successfully.added";
        public static final String PRODUCT_SUCCESSFULLY_UPDATED = "product.successfully.updated";
        public static final String PRODUCT_SUCCESSFULLY_DELETED = "product.success.deleted";
        public static final String PRODUCT_ALREADY_EXIST = "product.already.exist";
        public static final String CANNOT_HAVE_MULTIPLE_MAIN_IMAGES = "cannot.have.multiple.main.images";
        public static final String INSUFFICIENT_STOCK = "insufficient.stock";
        public static final String QUANTITY_MUST_BE_POSITIVE = "quantity.must.be.positive";
    }

    public static class Size {
        private Size() {}

        public static final String SIZE_NOT_FOUND = "size.not.found";
    }

    public static class Cart {
        private Cart() {}

        public static final String CART_FETCHED_SUCCESSFULLY = "cart.fetched.successfully";
        public static final String ITEM_ADDED_SUCCESSFULLY = "item.added.successfully";
        public static final String ITEM_UPDATED_SUCCESSFULLY = "item.updated.successfully";
        public static final String ITEM_REMOVED_SUCCESSFULLY = "item.removed.successfully";
        public static final String CART_CLEARED_SUCCESSFULLY = "cart.cleared.successfully";
        public static final String CART_ITEM_NOT_FOUND_IN_CART = "cart.item.not.found.in.cart";
        public static final String PRODUCT_ITEM_NOT_FOUND = "product.item.not.found";
        public static final String CART_NOT_FOUND = "cart.not.found";
    }

    public static class Supplier {
        private Supplier() {}

        public static final String SUPPLIER_NOT_FOUND_FOR_AUTHENTICATED_USER = "supplier.not.found.for.authenticated.user";
        public static final String SUPPLIER_IS_NOT_ACTIVE = "supplier.is.not.active";
        public static final String SUPPLIER_NOT_FOUND_WITH_GIVEN_ID = "supplier.not.found.with.given.ID";
    }

    public static class Address {
        private Address() {}

        public static final String ADDRESSES_SUCCESSFULLY_LISTED = "addresses.successfully.listed";
        public static final String ADDRESS_SUCCESSFULLY_ADDED = "address.successfully.added";
        public static final String ADDRESS_SUCCESSFULLY_UPDATED = "address.successfully.updated";
        public static final String ADDRESS_SUCCESSFULLY_DELETED = "address.success.deleted";
        public static final String ADDRESS_NOT_FOUND_WITH_GIVEN_ID_OR_DOES_NOT_BELONG_TO_CUSTOMER = "address.not.found.with.given.ID.or.does.not.belong.to.customer";
        public static final String ADDRESS_DETAIL_SUCCESSFULLY_LISTED = "address.detail.successfully.listed";
        public static final String ADDRESS_ALREADY_EXISTS = "address.already.exists";
    }

    public static class Country {
        private Country() {}

        public static final String COUNTRY_SUCCESSFULLY_LISTED = "country.successfully.listed";
        public static final String COUNTRY_DOES_NOT_EXISTS_WITH_GIVEN_ID = "country.does.not.exists.with.given.ID";
    }

    public static class Carrier {
        private Carrier() {}

        public static final String CARRIER_NOT_FOUND_WITH_GIVEN_ID = "carrier.not.found.with.given.ID";
    }

    public static class ShippingMethod {
        private ShippingMethod() {}

        public static final String SHIPPING_METHOD_NOT_FOUND = "shipping.method.not.found";
        public static final String SHIPPING_METHOD_SUCCESSFULLY_ADDED = "shipping.method.successfully.added";
        public static final String SHIPPING_METHOD_SUCCESSFULLY_UPDATED = "shipping.method.successfully.updated";
        public static final String SHIPPING_METHOD_SUCCESSFULLY_DELETED = "shipping.method.success.deleted";
    }

    public static class PaymentMethod {
        private PaymentMethod() {}

        public static final String PAYMENT_METHOD_NOT_FOUND_FOR_CUSTOMER = "payment.method.not.found.for.customer";
        public static final String PAYMENT_METHOD_SUCCESSFULLY_ADDED = "payment.method.successfully.added";
        public static final String PAYMENT_METHOD_SUCCESSFULLY_UPDATED = "payment.method.successfully.updated";
        public static final String PAYMENT_METHOD_SUCCESSFULLY_DELETED = "payment.method.success.deleted";
        public static final String PAYMENT_METHOD_SUCCESSFULLY_LISTED = "payment.method.successfully.listed";
        public static final String PAYMENT_METHOD_DETAIL_SUCCESSFULLY_LISTED = "payment.method.detail.successfully.listed";
        public static final String PAYMENT_METHOD_ALREADY_EXISTS = "payment.method.already.exists";
        public static final String PAYMENT_METHOD_CANNOT_BE_DELETED_BECAUSE_ACTIVE_SUBSCRIPTION = "payment.method.cannot.be.deleted.because.active.subscription";
        public static final String CREDIT_CARD = "credit.card";
    }

    public static class PaymentType {
        private PaymentType() {}

        public static final String PAYMENT_TYPE_NOT_FOUND = "payment.type.not.found";
        public static final String CREDIT_CARD = "paymentType.creditCard";
        public static final String BANK_TRANSFER = "paymentType.bankTransfer";
    }

    public static class Installment {
        private Installment() {}

        public static final String INSTALLMENT_OPTIONS_NOT_FOUND = "installment.options.not.found";
        public static final String INSTALLMENT_OPTIONS_SUCCESSFULLY_LISTED = "installment.options.successfully.listed";
        public static final String INSTALLMENT_OPTIONS_INVALID = "installment.options.invalid";
    }

    public static class Order {
        private Order() {}

        public static final String ORDER_SUCCESSFULLY_ADDED = "order.successfully.added";
        public static final String ORDER_ERROR_EMPTY_CART = "order.error.empty.cart";
        public static final String ORDER_NOT_FOUND_OR_NOT_AUTHORIZED = "order.not.found.or.not.authorized";
        public static final String ORDER_DETAIL_SUCCESSFULLY_LISTED = "order.detail.successfully.listed";
        public static final String ORDER_SUCCESSFULLY_LISTED = "order.successfully.listed";
        public static final String ORDER_SUCCESSFULLY_SHIPPED = "order.successfully.shipped";
        public static final String ORDER_SUCCESSFULLY_DELIVERED = "order.successfully.delivered";
        public static final String ORDER_SUCCESSFULLY_CANCELLED = "order.successfully.cancelled";
        public static final String ORDER_ITEM_NOT_FOUND_OR_NOT_AUTHORIZED = "order.item.not.found.or.not.authorized";
        public static final String ORDER_NOT_FOUND = "order.not.found";
        public static final String ORDER_IS_NOT_SHIPPABLE = "order.is.not.shippable";
        public static final String ORDER_IS_NOT_DELIVERABLE = "order.is.not.deliverable";
        public static final String ORDER_IS_NOT_CANCELLABLE = "order.is.not.cancellable";
        public static final String ORDER_INVALID_STATUS_TRANSITION = "order.invalid.status.transition";
        public static final String ORDER_STATUS_SUCCESSFULLY_UPDATED = "order.status.successfully.updated";
    }

    public static class OrderStatus {
        private OrderStatus() {}

        public static final String ORDER_STATUS_NOT_FOUND_BY_NAME = "order.status.not.found.by.name";
    }

    public static class Invoice {
        private Invoice() {}

        public static final String INVOICES_SUCCESSFULLY_LISTED = "invoices.successfully.listed";
        public static final String INVOICE_NOT_FOUND_OR_NOT_AUTHORIZED = "invoice.not.found.or.not.authorized";
        public static final String INVOICE_DETAIL_SUCCESSFULLY_LISTED = "invoice.detail.successfully.listed";
        public static final String INVOICE_NOT_FOUND = "invoice.not.found";
        public static final String INVOICE_CANNOT_BE_CANCELLED = "invoice.cannot.be.cancelled";
        public static final String INVOICE_SUCCESSFULLY_CANCELLED = "invoice.successfully.cancelled";
        public static final String INVOICE_ALREADY_REFUNDED = "invoice.already.refunded";
        public static final String INVOICE_STATUS_MUST_BE_PAID = "invoice.status.must.be.paid";
        public static final String INVOICE_SUCCESSFULLY_MARKED_REFUNDED = "invoice.successfully.marked.refunded";
    }

    public static class ReturnRequest {
        private ReturnRequest() {}

        public static final String RETURN_REQUESTS_SUCCESSFULLY_LISTED = "returnRequest.successfully.listed";
        public static final String RETURN_REQUEST_DETAIL_SUCCESSFULLY_LISTED = "returnRequest.detail.successfully.listed";
        public static final String RETURN_REQUEST_SUCCESSFULLY_CREATED = "returnRequest.successfully.created";
        public static final String RETURN_REQUEST_STATUS_SUCCESSFULLY_UPDATED = "returnRequest.status.successfully.updated";
        public static final String RETURN_REQUEST_ALREADY_EXISTS = "returnRequest.already.exists";
        public static final String RETURN_NOT_POSSIBLE_FOR_ORDER_STATUS = "return.not.possible.for.order.status";
        public static final String RETURN_PERIOD_EXPIRED = "return.period.expired";
        public static final String RETURN_REQUEST_NOT_FOUND = "returnRequest.not.found";
        public static final String RETURN_REQUEST_ALREADY_FINALIZED = "returnRequest.already.finalized";
        public static final String RETURN_REQUEST_NOT_AUTHORIZED = "returnRequest.not.authorized";
    }

    public static class ReturnReason {
        private ReturnReason() {}

        public static final String WRONG_ITEM = "returnReason.wrongItem";
        public static final String DEFECTIVE_PRODUCT = "returnReason.defectiveProduct";
        public static final String WRONG_SIZE_OR_COLOR = "returnReason.wrongSizeOrColor";
        public static final String NO_LONGER_NEEDED = "returnReason.noLongerNeeded";
        public static final String OTHER = "returnReason.other";
    }

    public static class Auth {
        private Auth() {}

        public static final String EMAIL_ALREADY_EXISTS = "email.address.already.exists";
        public static final String REGISTER_SUCCESSFUL = "register.successful";
        public static final String LOGIN_SUCCESSFUL = "login.successful";
        public static final String LOGOUT_SUCCESSFUL = "logout.successful";
        public static final String USER_NOT_FOUND_WITH_EMAIL_ADDRESS = "user.not.found.with.email.address";
        public static final String USER_NOT_FOUND_WITH_ID = "user.not.found.with.id";
        public static final String USER_DOES_NOT_HAVE_ROLE = "user.does.not.have.role";
        public static final String ROLE_GRANTED_SUCCESSFULLY = "role.granted.successfully";
        public static final String ROLE_REVOKED_SUCCESSFULLY = "role.revoked.successfully";
        public static final String ADMIN_CANNOT_DELETE_HIMSELF = "admin.cannot.delete.himself";
        public static final String CANNOT_DELETE_LAST_ADMIN = "cannot.delete.last.admin";
        public static final String USER_DELETED_SUCCESSFULLY = "user.deleted.successfully";
        public static final String TOKEN_SUCCESSFULLY_REFRESHED= "token.successfully.refreshed";
        public static final String TOKEN_EXPIRED = "token.expired";
        public static final String TOKEN_INVALID = "token.invalid";
        public static final String INVALID_REFRESH_TOKEN = "invalid.refresh.token";
        public static final String AUTHENTICATION_PRINCIPAL_INVALID = "authentication.principal.invalid";
        public static final String AUTHORIZATION_FAILED = "authorization.failed";
    }

    public static class Role {
        private Role() {}
        public static final String ROLE_NOT_FOUND = "role.not.found";
    }

    public static class Errors {

        private Errors() {}

        public static final String VALIDATION_ERROR = "validation.error";
        public static final String AUTHENTICATION_FAILED = "authentication.failed";
        public static final String ACCESS_DENIED = "access.denied";
        public static final String UNEXPECTED_ERROR_OCCURRED = "unexpected.error.occurred";
        public static final String DATA_INTEGRITY_VIOLATION = "data.integrity.violation";
        public static final String ORDER_TOTAL_DOES_NOT_MATCH = "order.total.does.not.match";
        public static final String ADDRESS_CAN_NOT_BE_DELETED_BECAUSE_OF_ACTIVE_ORDER = "address.can.not.be.deleted.because.of.active.order";
        public static final String CONCURRENT_UPDATE_DETECTED = "concurrent.update.detected";
    }

    public static class Validations {
        private Validations() {}

        public static class Address {
            private Address() {}

            public static final String ADDRESS_TITLE_CAN_NOT_BE_BLANK = "{address.title.can.not.be.blank}";
            public static final String ADDRESS_TITLE_SIZE_MIN_MAX = "{address.title.size.min.max}";
            public static final String ADDRESS_LINE1_CAN_NOT_BE_BLANK = "{address.line1.can.not.be.blank}";
            public static final String ADDRESS_LINE1_SIZE_MAX = "{address.line1.size.max}";
            public static final String ADDRESS_LINE2_SIZE_MAX = "{address.line2.size.max}";
            public static final String ADDRESS_CITY_CAN_NOT_BE_BLANK = "{address.city.can.not.be.blank}";
            public static final String ADDRESS_CITY_SIZE_MAX = "{address.city.size.max}";
            public static final String ADDRESS_POSTAL_CODE_CAN_NOT_BE_BLANK = "{address.postal.code.can.not.be.blank}";
            public static final String ADDRESS_POSTAL_CODE_PATTERN_INVALID = "{address.postal.code.pattern.invalid}";
            public static final String ADDRESS_COUNTRY_ID_CAN_NOT_BE_NULL = "{address.country.id.can.not.be.null}";
            public static final String ADDRESS_SHIPPING_FLAG_CAN_NOT_BE_NULL = "{address.shipping.flag.can.not.be.null}";
            public static final String ADDRESS_BILLING_FLAG_CAN_NOT_BE_NULL = "{address.billing.flag.can.not.be.null}";
        }

        public static class Auth {
            private Auth() {}

            public static final String EMAIL_CAN_NOT_BE_BLANK = "{auth.email.can.not.be.blank}";
            public static final String EMAIL_FORMAT_INVALID = "{auth.email.format.invalid}";
            public static final String PASSWORD_CAN_NOT_BE_BLANK = "{auth.password.can.not.be.blank}";
            public static final String PASSWORD_SIZE_MIN_MAX = "{auth.password.size.min.max}";
            public static final String REFRESH_TOKEN_CAN_NOT_BE_BLANK = "{auth.refresh.token.can.not.be.blank}";
            public static final String CONTACT_NAME_CAN_NOT_BE_BLANK = "{auth.contact.name.can.not.be.blank}";
            public static final String CONTACT_NAME_SIZE_MIN_MAX = "{auth.contact.name.size.min.max}";
            public static final String PHONE_NUMBER_CAN_NOT_BE_BLANK = "{auth.phone.number.can.not.be.blank}";
            public static final String PHONE_NUMBER_PATTERN_INVALID = "{auth.phone.number.pattern.invalid}";
            public static final String CUSTOMER_ID_INVALID = "{auth.customer.id.invalid}";
            public static final String ROLE_NAME_REQUIRED = "{auth.role.name.required}";
        }

        public static class Cart {
            private Cart() {}

            public static final String PRODUCT_ITEM_ID_CAN_NOT_BE_NULL = "{cart.product.item.id.can.not.be.null}";
            public static final String QUANTITY_CAN_NOT_BE_NULL = "{cart.quantity.can.not.be.null}";
            public static final String QUANTITY_MUST_BE_AT_LEAST_ONE = "{cart.quantity.must.be.at.least.one}";
            public static final String QUANTITY_EXCEEDS_MAX_LIMIT = "{cart.quantity.exceeds.max.limit}";
        }

        public static class Category {
            private Category() {}

            public static final String CATEGORY_ID_INVALID = "{category.id.must.be.greater.than.zero}";
            public static final String CATEGORY_NAME_CAN_NOT_BE_BLANK = "{category.name.can.not.be.blank}";
            public static final String CATEGORY_NAME_SIZE_MIN_MAX = "{category.name.size.min.max}";
            public static final String CATEGORY_DESCRIPTION_SIZE_MAX = "{category.description.size.max}";
            public static final String CATEGORY_IS_ACTIVE_NOT_NULL = "{category.is.active.not.null}";
            public static final String CATEGORY_ID_CAN_NOT_BE_NULL = "{category.id.can.not.be.null}";
            public static final String CATEGORY_PARENT_CAN_NOT_BE_ITSELF = "{category.parent.can.not.be.itself}";
        }

        public static class Installment {
            private Installment() {}

            public static final String BIN_NUMBER_CAN_NOT_BE_BLANK = "{installment.bin.number.can.not.be.blank}";
            public static final String BIN_NUMBER_SIZE_INVALID = "{installment.bin.number.size.invalid}";
            public static final String BIN_NUMBER_PATTERN_INVALID = "{installment.bin.number.pattern.invalid}";
            public static final String AMOUNT_CAN_NOT_BE_NULL = "{installment.amount.can.not.be.null}";
            public static final String AMOUNT_MUST_BE_GREATER_THAN_ZERO = "{installment.amount.must.be.greater.than.zero}";
        }

        public static class Order {
            private Order() {}

            public static final String ORDER_ID_CAN_NOT_BE_NULL = "{order.id.can.not.be.null}";
            public static final String CUSTOMER_ID_CAN_NOT_BE_NULL = "{order.customer.id.can.not.be.null}";
            public static final String SHIPPING_ADDRESS_ID_CAN_NOT_BE_NULL = "{order.shipping.address.id.can.not.be.null}";
            public static final String SHIPPING_METHOD_ID_CAN_NOT_BE_NULL = "{order.shipping.method.id.can.not.be.null}";
            public static final String PAYMENT_METHOD_ID_CAN_NOT_BE_NULL = "{order.payment.method.id.can.not.be.null}";
            public static final String INSTALLMENT_COUNT_MIN_ONE = "{order.installment.count.min.one}";
            public static final String TOTAL_AMOUNT_MUST_BE_GREATER_THAN_ZERO = "{order.total.amount.must.be.greater.than.zero}";
            public static final String TRACKING_NUMBER_CAN_NOT_BE_BLANK = "{order.tracking.number.can.not.be.blank}";
            public static final String TRACKING_NUMBER_SIZE_MAX = "{order.tracking.number.size.max}";
            public static final String CARRIER_ID_CAN_NOT_BE_NULL = "{order.carrier.id.can.not.be.null}";
            public static final String ORDER_ID_INVALID = "{order.id.must.be.greater.than.zero}";
        }

        public static class Payment {
            private Payment() {}

            public static final String CARD_HOLDER_NAME_CAN_NOT_BE_BLANK = "{payment.card.holder.name.can.not.be.blank}";
            public static final String CARD_NUMBER_PATTERN_INVALID = "{payment.card.number.pattern.invalid}";
            public static final String EXPIRY_MONTH_INVALID = "{payment.expiry.month.invalid}";
            public static final String EXPIRY_YEAR_INVALID = "{payment.expiry.year.invalid}";
            public static final String CVC_PATTERN_INVALID = "{payment.cvc.pattern.invalid}";
        }

        public static class Product {
            private Product() {}

            public static final String PRODUCT_ITEM_ID_CAN_NOT_BE_NULL = "{product.item.id.can.not.be.null}";
            public static final String IMAGE_URL_CAN_NOT_BE_BLANK = "{product.image.url.can.not.be.blank}";
            public static final String IMAGE_URL_INVALID_FORMAT = "{product.image.url.invalid.format}";
            public static final String SKU_CAN_NOT_BE_BLANK = "{product.sku.can.not.be.blank}";
            public static final String SKU_SIZE_MAX = "{product.sku.size.max}";
            public static final String STOCK_CAN_NOT_BE_NULL = "{product.stock.can.not.be.null}";
            public static final String STOCK_MUST_BE_POSITIVE_OR_ZERO = "{product.stock.must.be.positive.or.zero}";
            public static final String PRICE_CAN_NOT_BE_NULL = "{product.price.can.not.be.null}";
            public static final String PRICE_MUST_BE_POSITIVE = "{product.price.must.be.positive}";
            public static final String COLOUR_ID_CAN_NOT_BE_NULL = "{product.colour.id.can.not.be.null}";
            public static final String SIZE_ID_CAN_NOT_BE_NULL = "{product.size.id.can.not.be.null}";
            public static final String PRODUCT_NAME_CAN_NOT_BE_BLANK = "{product.name.can.not.be.blank}";
            public static final String PRODUCT_NAME_SIZE_MIN = "{product.name.size.min}";
            public static final String CATEGORY_IDS_CAN_NOT_BE_NULL = "{product.category.ids.can.not.be.null}";
            public static final String CATEGORY_IDS_SIZE_MIN = "{product.category.ids.size.min}";
            public static final String PRODUCT_ITEMS_CAN_NOT_BE_NULL = "{product.items.can.not.be.null}";
            public static final String PRODUCT_ITEMS_SIZE_MIN = "{product.items.size.min}";
            public static final String PRODUCT_DESCRIPTION_SIZE_MAX = "{product.description.size.max}";
            public static final String SUPPLIER_ID_CAN_NOT_BE_NULL = "{product.supplier.id.can.not.be.null}";
        }

        public static class Return {
            private Return() {}

            public static final String ORDER_ID_CAN_NOT_BE_NULL = "{return.order.id.can.not.be.null}";
            public static final String ORDER_ITEM_ID_CAN_NOT_BE_NULL = "{return.order.item.id.can.not.be.null}";
            public static final String REASON_ENUM_CAN_NOT_BE_NULL = "{return.reason.enum.can.not.be.null}";
            public static final String CUSTOM_REASON_CAN_NOT_BE_BLANK = "{return.custom.reason.can.not.be.blank}";
            public static final String CUSTOM_REASON_SIZE_MAX = "{return.custom.reason.size.max}";
            public static final String NEW_STATUS_CAN_NOT_BE_NULL = "{return.new.status.can.not.be.null}";
            public static final String RETURN_ID_INVALID = "{return.id.must.be.greater.than.zero}";
        }

        public static class Pagination {
            private Pagination() {}

            public static final String PAGE_NUMBER_MIN = "{page.number.must.be.greater.than.or.equal.to.one}";
            public static final String PAGE_SIZE_MIN = "{page.size.must.be.greater.than.or.equal.to.one}";
            public static final String SORT_DIRECTION_INVALID = "{sort.direction.invalid}";
        }

        public static class Common {
            private Common() {}

            public static final String INVALID_ID = "{invalid.id.value}";
        }
    }

}
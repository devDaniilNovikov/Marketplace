package dn.marketplace.account.api.enums;

/**
 * Стадия пути покупатель → продавец. Ортогональна флагу {@code banned}.
 */
public enum BusinessStatus {
    BUYER,
    SELLER_PENDING,
    SELLER,
    SELLER_REJECTED
}

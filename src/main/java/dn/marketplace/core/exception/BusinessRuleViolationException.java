package dn.marketplace.core.exception;

/**
 * Нарушено бизнес-правило домена: недопустимый переход статуса заказа,
 * заказ у забаненного аккаунта, резерв больше остатка и т.п.
 * <p>
 * Сообщение показывается клиенту, поэтому формулируется на языке предметной
 * области и не содержит внутренних деталей.
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}

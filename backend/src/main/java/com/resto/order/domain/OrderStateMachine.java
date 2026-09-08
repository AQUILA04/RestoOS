package com.resto.order.domain;

/**
 * Operational status transitions for orders.
 * CREATED → SENT_TO_KITCHEN → PREPARING → READY → DELIVERED → CLOSED
 * Alt cancel: CREATED|SENT_TO_KITCHEN|PREPARING → CANCELLED
 */
public final class OrderStateMachine {

    private OrderStateMachine() {}

    public static boolean canTransition(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        if (from.equals(to)) {
            return true;
        }
        return switch (from) {
            case "CREATED" -> "SENT_TO_KITCHEN".equals(to) || "CANCELLED".equals(to);
            case "SENT_TO_KITCHEN" -> "PREPARING".equals(to) || "CANCELLED".equals(to);
            case "PREPARING" -> "READY".equals(to) || "CANCELLED".equals(to);
            case "READY" -> "DELIVERED".equals(to);
            case "DELIVERED" -> "CLOSED".equals(to);
            default -> false;
        };
    }

    public static void assertTransition(String from, String to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Invalid order status transition: " + from + " → " + to);
        }
    }

    public static boolean canCancel(String status) {
        return "CREATED".equals(status) || "SENT_TO_KITCHEN".equals(status) || "PREPARING".equals(status);
    }

    public static boolean isKitchenActive(String status) {
        return "SENT_TO_KITCHEN".equals(status) || "PREPARING".equals(status) || "READY".equals(status);
    }
}

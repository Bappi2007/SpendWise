package com.example.data.model

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card");

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } ?: CASH
        }
    }
}

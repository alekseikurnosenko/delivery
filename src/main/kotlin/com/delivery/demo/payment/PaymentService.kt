package com.delivery.demo.payment

import org.joda.money.Money
import org.springframework.stereotype.Service

@Service
class PaymentService {
    fun charge(customerId: String, amount: Money): Boolean {
        return true
    }

    fun refund(customerId: String, amount: Money): Boolean {
        return true
    }
}
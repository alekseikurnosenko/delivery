package com.delivery.demo.payment

import org.joda.money.Money
import org.springframework.stereotype.Service

const val PAYMENT_METHOD_SUCCESS = "PAYMENT_METHOD_SUCCESS"
const val PAYMENT_METHOD_NOT_ENOUGH_FUNDS = "PAYMENT_METHOD_NOT_ENOUGH_FUNDS"

@Service
class PaymentService {

    fun charge(customerId: String, paymentMethodId: String, amount: Money): ChargeResult {
        // Because payment gateways are slow!
        Thread.sleep(500)
        // TODO: chance to fail
        return when (paymentMethodId) {
            PAYMENT_METHOD_SUCCESS -> ChargeResult.Success
            PAYMENT_METHOD_NOT_ENOUGH_FUNDS -> ChargeResult.NotEnoughFunds
            else -> ChargeResult.UnknownPaymentMethod
        }
    }

    fun refund(customerId: String, paymentMethodId: String, amount: Money): Boolean {
        return true
    }

}

sealed class ChargeResult {
    object Success : ChargeResult()
    object NotEnoughFunds : ChargeResult()
    object UnknownPaymentMethod : ChargeResult()
}

class NotEnoughFundsException : Exception("Not enough funds")
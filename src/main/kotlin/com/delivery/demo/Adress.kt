package com.delivery.restaurant

import javax.persistence.Embeddable

@Embeddable
class Address(
    var address: String,
    var city: String,
    var country: String
)

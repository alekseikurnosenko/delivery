package com.delivery.demo

import com.delivery.demo.courier.LatLng
import javax.persistence.Embeddable

@Embeddable
class Address(
    val location: LatLng,
    var address: String,
    var city: String,
    var country: String
)

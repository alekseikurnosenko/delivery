package com.delivery.demo

import org.springframework.data.repository.CrudRepository
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class FirebaseTokenInformation(
        @Id val userId: String
) {
    var firebaseToken: String? = null
}

interface FirebaseTokenRepository : CrudRepository<FirebaseTokenInformation, String> {
}
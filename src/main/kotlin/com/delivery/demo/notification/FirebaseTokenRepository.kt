package com.delivery.demo.notification

import org.springframework.data.repository.CrudRepository
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class FirebaseTokenInformation(
        @Id val userId: String,
        firebaseToken: String
) {
    var firebaseToken: String = firebaseToken
}

interface FirebaseTokenRepository : CrudRepository<FirebaseTokenInformation, String> {
}
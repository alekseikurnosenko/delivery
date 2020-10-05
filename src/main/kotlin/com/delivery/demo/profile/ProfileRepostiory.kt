package com.delivery.demo.profile

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ProfileRepostiory : JpaRepository<Profile, UUID> {
    fun findByUserId(userId: String): Optional<Profile>
}
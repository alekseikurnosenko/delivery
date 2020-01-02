package com.delivery.demo.profile

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProfileRepostiory : JpaRepository<Profile, UUID> {
    fun findByName(name: String): Optional<Profile>
}
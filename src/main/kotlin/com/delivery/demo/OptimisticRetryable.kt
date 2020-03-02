package com.delivery.demo

import org.hibernate.StaleObjectStateException
import org.springframework.retry.annotation.Retryable
import javax.persistence.OptimisticLockException

@Retryable(include = [StaleObjectStateException::class, OptimisticLockException::class], maxAttempts = 3)
annotation class OptimisticRetryable
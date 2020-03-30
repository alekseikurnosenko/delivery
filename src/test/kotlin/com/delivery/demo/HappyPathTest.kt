package com.delivery.demo

import io.cucumber.junit.Cucumber
import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles

@RunWith(Cucumber::class)
@ActiveProfiles(profiles = ["test"])
class HappyPathTest
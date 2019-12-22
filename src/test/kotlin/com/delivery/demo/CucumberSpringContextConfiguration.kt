package com.delivery.demo

import io.cucumber.java.Before
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootContextLoader
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ContextConfiguration

/**
 * Class to use spring application context while running cucumber
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = [DemoApplication::class], loader = SpringBootContextLoader::class)
class CucumberSpringContextConfiguration {

    /**
     * Need this method so the cucumber will recognize this class as glue and load spring context configuration
     */
    @Before
    fun setUp() {
        LOG.info("-------------- Spring Context Initialized For Executing Cucumber Tests --------------")
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(CucumberSpringContextConfiguration::class.java)
    }

}
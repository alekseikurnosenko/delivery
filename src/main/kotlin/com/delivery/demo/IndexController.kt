package com.delivery.demo

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@CrossOrigin
@Tag(name = "index")
class IndexController {

    private val eventClasses = lazy {
        val bdr = SimpleBeanDefinitionRegistry()
        val s = ClassPathBeanDefinitionScanner(bdr)

        val tf = AssignableTypeFilter(DomainEvent::class.java)
        s.addIncludeFilter(tf)
        s.setIncludeAnnotationConfig(false)
        s.scan("com.delivery.demo")
        bdr.beanDefinitionNames.map { Class.forName(it) }
    }

}
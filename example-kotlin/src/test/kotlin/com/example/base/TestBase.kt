package com.example.base

import com.example.ExampleKotlinApplication
import io.kotest.core.spec.style.AnnotationSpec
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(classes = [ExampleKotlinApplication::class])
@ExtendWith(SpringExtension::class)
class TestBase : AnnotationSpec() {
}

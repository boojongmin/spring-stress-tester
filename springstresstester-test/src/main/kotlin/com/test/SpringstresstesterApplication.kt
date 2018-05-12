package com.test

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import springstresstester.SpringStressTesterConfig
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@SpringBootApplication
@Import(SpringStressTesterConfig::class)
@RestController
class SpringstresstesterApplication(val service: TestService) {
    @GetMapping("/save")
    fun a() = service.hello()

    @GetMapping("/count")
    fun count(): Long {
        service.hello()
        service.count()
        service.hello()
        return service.count()
    }

    @GetMapping("/total")
    fun total(): Map<String, Long> {
        throw NotImplementedError()
    }
}

@Service
class TestService(
        val repository: HelloRepository
) {
    fun hello() = repository.save(Hello(null, "name"))
    fun count(): Long {
        return repository.countByName("name")
    }
}

interface HelloRepository: CrudRepository<Hello, Long> {
    fun countByName(name: String): Long
}

@Entity
data class Hello(
        @Id @GeneratedValue(strategy = GenerationType.AUTO)
        var id: Long?,
        val name: String
)


fun main(args: Array<String>) {
    runApplication<SpringstresstesterApplication>(*args)
}

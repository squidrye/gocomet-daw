package com.ridehailing.core_api

import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@MapperScan("com.ridehailing.core_api")
class CoreApiApplication

fun main(args: Array<String>) {
	runApplication<CoreApiApplication>(*args)
}

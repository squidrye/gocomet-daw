package com.ridehailing.core_api

import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@MapperScan("com.ridehailing.core_api")
@EnableScheduling
class CoreApiApplication

fun main(args: Array<String>) {
	runApplication<CoreApiApplication>(*args)
}

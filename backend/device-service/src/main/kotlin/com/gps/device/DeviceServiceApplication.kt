package com.gps.device

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DeviceServiceApplication

fun main(args: Array<String>) {
	runApplication<DeviceServiceApplication>(*args)
}


package com.gps.device.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.gps.device.dto.request.JsonRpcRequest
import com.gps.device.dto.response.JsonRpcResponse
import com.gps.device.dto.response.JsonRpcError

import com.gps.device.service.DeviceService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/rpc")
class JsonRpcController(
    private val deviceService: DeviceService,
    private val objectMapper: ObjectMapper
) {
    private val log=LoggerFactory.getLogger(JsonRpcController::class.java)
    @PostMapping
    fun handleRequest(
        @RequestBody request: JsonRpcRequest
    ): JsonRpcResponse {

        log.info("JSON-RPC controller reached: method={}",request.method)
        if (request.jsonrpc != "2.0") {
            return errorResponse(
                request.id,
                -32600,
                "Invalid Request"
            )
        }

        return try {
            when (request.method) {

                "getDeviceByImei" -> {
                    val imei = request.params
                        ?.get("imei")
                        ?.takeIf { it.isTextual }
                        ?.asText()
                        ?.takeIf { it.isNotBlank() }
                        ?: return errorResponse(
                            request.id,
                            -32602,
                            "Invalid params: imei is required"
                        )

                    val device = deviceService.getDeviceByImei(imei)

                    JsonRpcResponse(
                        result = mapOf(
                            "publicId" to device.publicId
                        ),
                        id = request.id
                    )
                }

                "markDeviceSeen" -> {
                    val publicId = request.params
                        ?.get("publicId")
                        ?.takeIf { it.isTextual }
                        ?.asText()
                        ?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
                        ?: return errorResponse(
                            request.id,
                            -32602,
                            "Invalid params: publicId must be a UUID"
                        )

                    val device = deviceService.markDeviceSeen(publicId)
                    JsonRpcResponse(
                        result = mapOf(
                            "publicId" to device.publicId,
                            "status" to device.status,
                            "lastSeenAt" to device.lastSeenAt
                        ),
                        id = request.id
                    )
                }

                else -> errorResponse(
                    request.id,
                    -32601,
                    "Method not found"
                )
            }
        } catch (ex: NoSuchElementException) {
            errorResponse(
                request.id,
                -32001,
                "Device not found"
            )
        } catch (ex: Exception) {
            errorResponse(
                request.id,
                -32603,
                "Internal error"
            )
        }
    }

    private fun errorResponse(
        id: JsonNode?,
        code: Int,
        message: String
    ) = JsonRpcResponse(
        error = JsonRpcError(code, message),
        id = id
    )
}

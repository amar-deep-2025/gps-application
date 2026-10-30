package com.gps.tracking.client

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class DeviceJsonRpcClient(

    @Value("\${device-service.rpc-url:http://localhost:5052/rpc}")
    private val rpcUrl:String,

    @Value("\${gps.service.api-key}")
    private val serviceApiKey:String,
    private val restClientBuilder:RestClient.Builder
) {

    fun getDevicePublicByImei(imei:String): UUID {
        val response=restClientBuilder.build()
            .post()
            .uri(rpcUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-service-API-Key", serviceApiKey)
            .body(
                mapOf(
                    "jsonrpc" to "2.0",
                    "method" to "getDeviceByImei",
                    "params" to mapOf("imei" to imei),
                    "id" to 1
                )
            )
            .retrieve()
            .body(JsonNode::class.java)
            ?:throw IllegalArgumentException("Empty JSON-RPC response")

        val error=response.path("error")
        if(!error.isMissingNode && !error.isNull){
            throw IllegalArgumentException(
                "Device Service JSON-RPC error: ${error.path("message").asText()}"
            )
        }
        val publicId=response.path("result").path("publicId")
            .takeIf { it.isTextual }
            ?.asText()
            ?:throw IllegalStateException(
                "Device Service response does not contain publicId"
            )
        return UUID.fromString(publicId)

    }

    fun markDeviceSeen(publicId: UUID) {
        val response = restClientBuilder.build()
            .post()
            .uri(rpcUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-service-API-Key", serviceApiKey)
            .body(
                mapOf(
                    "jsonrpc" to "2.0",
                    "method" to "markDeviceSeen",
                    "params" to mapOf("publicId" to publicId.toString()),
                    "id" to 2
                )
            )
            .retrieve()
            .body(JsonNode::class.java)
            ?: throw IllegalArgumentException("Empty JSON-RPC response")

        val error = response.path("error")
        if (!error.isMissingNode && !error.isNull) {
            throw IllegalArgumentException(
                "Device Service JSON-RPC error: " + error.path("message").asText()
            )
        }
    }
}
package com.gps.device.dto.request

import com.fasterxml.jackson.databind.JsonNode

data class JsonRpcRequest(
    val jsonrpc:String,
    val method:String,
    val params: JsonNode?=null,
    val id:JsonNode?=null
)

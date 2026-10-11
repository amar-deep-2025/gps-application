package com.gps.device.dto.response

import com.fasterxml.jackson.databind.JsonNode

data class JsonRpcError(
    val code:Int,
    val message:String
)

data class JsonRpcResponse(
    val jsonrpc:String="2.0",
    val result:Any?=null,
    val error:JsonRpcError?=null,
    val id:JsonNode?=null
)

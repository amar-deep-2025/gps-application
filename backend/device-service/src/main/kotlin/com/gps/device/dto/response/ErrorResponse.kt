package com.gps.device.dto.response

import java.time.Instant

data class ErrorResponse(
    val timeStamp:Instant= Instant.now(),
    val status:Int,
    val error:String,
    val message:String,
    val path:String

)

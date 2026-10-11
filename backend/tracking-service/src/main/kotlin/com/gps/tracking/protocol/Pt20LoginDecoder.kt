
package com.gps.tracking.protocol

object Pt20LoginDecoder {


    fun decodeImei(content: ByteArray): String {
        require(content.size >= 8) {
            "PT20 login content must contain 8 Terminal ID bytes"
        }

        val terminalId = content.take(8)
            .joinToString("") {
                "%02X".format(it.toInt() and 0xFF)
            }

        return terminalId.trimStart('0')
    }

}

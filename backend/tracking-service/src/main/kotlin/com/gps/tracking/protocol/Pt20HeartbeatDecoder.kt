package com.gps.tracking.protocol

/**
 * Single unified data model used for all database records and push notification lookups.
 */
data class Pt20AlertResult(
    val activeAlertName: String,     // Standardised code for DB indexing & internal routing
    val userFriendlyMessage: String,  // Decoupled, symbol-free localized message
    val isCritical: Boolean          // High priority routing flag for instant Firebase pushes
)

class Pt20HeartbeatDecoder {

    companion object {
        private const val PROTOCOL_HEARTBEAT = 0x13
        private const val PROTOCOL_ALARM = 0x26
        private const val MIN_HEARTBEAT_SIZE = 5
        private const val MIN_ALARM_SIZE = 30
    }

    /**
     * Extracts safety alerts directly from Protocol 0x13 (Heartbeat Frame).
     * Enforces the critical-threat escalation matrix.
     */
    fun decodeHeartbeatAlert(packet: Pt20Packet): Pt20AlertResult {
        require(packet.protocolNumber == PROTOCOL_HEARTBEAT) { "Invalid heartbeat protocol" }
        val content = packet.content
        require(content.size >= MIN_HEARTBEAT_SIZE) { "Payload size error" }

        val terminalInfo = content[0].toInt() and 0xFF
        val alarmCode = (terminalInfo shr 3) and 0x07

        val isPowerCut = alarmCode == 0x02
        val isLowBattery = alarmCode == 0x03
        val isVibration = alarmCode == 0x01
        val isOilDisconnected = (terminalInfo and 0x80) != 0
        val isAccOn = (terminalInfo and 0x02) != 0

        val alertLabel: String
        val humanMessage: String
        var isCritical = false

        // Threat Escalation Matrix - Highest Tier Safety Flags Override Routine Ignition Logs
        when {
            isPowerCut -> {
                alertLabel = "MAIN_POWER_DISCONNECTED"
                humanMessage = "Alert: Vehicle main power supply has been disconnected."
                isCritical = true
            }
            isLowBattery -> {
                alertLabel = "LOW_BATTERY_WARNING"
                humanMessage = "Warning: GPS tracker backup internal battery is critically low."
                isCritical = true
            }
            isVibration -> {
                alertLabel = "VIBRATION_ALERT"
                humanMessage = "Security: Unauthorized movement or vibration detected on your vehicle."
                isCritical = false
            }
            isOilDisconnected -> {
                alertLabel = "ENGINE_CUTOFF_ACTIVE"
                humanMessage = "Security: Immobilizer active. Engine ignition circuit is locked."
                isCritical = false
            }
            isAccOn -> {
                alertLabel = "ACC_IGNITION_ON"
                humanMessage = "Ignition On: Your vehicle engine has been started."
                isCritical = false
            }
            else -> {
                alertLabel = "ACC_IGNITION_OFF"
                humanMessage = "Ignition Off: Your vehicle engine has been shut down."
                isCritical = false
            }
        }

        return Pt20AlertResult(alertLabel, humanMessage, isCritical)
    }

    /**
     * Extracts high-priority real-time hardware alerts from Protocol 0x26 (Alarm Frame).
     */
    fun decodeDedicatedAlarm(packet: Pt20Packet): Pt20AlertResult {
        require(packet.protocolNumber == PROTOCOL_ALARM) { "Invalid alarm protocol" }
        val content = packet.content

        if (content.size < MIN_ALARM_SIZE) {
            return Pt20AlertResult("GENERIC_ALARM_TRIGGERED", "Security Alert: Unusual vehicle activity detected.", true)
        }

        val alarmByteIndex = content.size - 4
        val alarmCode = content[alarmByteIndex].toInt() and 0xFF

        val alertLabel: String
        val humanMessage: String
        var isCritical = false

        when (alarmCode) {
            0x01 -> {
                alertLabel = "SOS_EMERGENCY_ALERT"
                humanMessage = "EMERGENCY: SOS panic button triggered! Immediate response required!"
                isCritical = true
            }
            0x02 -> {
                alertLabel = "POWER_FAILURE_ALARM"
                humanMessage = "CRITICAL ALERT: Main wires cut! The tracking unit is running on backup battery!"
                isCritical = true
            }
            0x03 -> {
                alertLabel = "VIBRATION_THEFT_ALERT"
                humanMessage = "THEFT WARNING: Abnormal shaking or impact detected. Please verify your vehicle location!"
                isCritical = true
            }
            0x04 -> {
                alertLabel = "ENTER_FENCE_ALARM"
                humanMessage = "Geofence: Vehicle has entered your configured safe boundary zone."
                isCritical = false
            }
            0x05 -> {
                alertLabel = "EXIT_FENCE_ALARM"
                humanMessage = "GEOFENCE BREACH: Your vehicle has left the designated safe perimeter zone!"
                isCritical = true
            }
            0x06 -> {
                alertLabel = "OVERSPEED_ALARM"
                humanMessage = "Speed Alert: Vehicle exceeded your set safe speed limit thresholds."
                isCritical = false
            }
            0x09 -> {
                alertLabel = "DISPLACEMENT_ALARM"
                humanMessage = "Towing Alert: Locked vehicle is shifting position without engine running!"
                isCritical = true
            }
            0x0A -> {
                alertLabel = "ENTER_GPS_BLIND_AREA"
                humanMessage = "Signal Alert: Vehicle entered a tracking blind zone or underground structure."
                isCritical = false
            }
            0x0B -> {
                alertLabel = "EXIT_GPS_BLIND_AREA"
                humanMessage = "Signal Restored: Satellite lock re-established successfully."
                isCritical = false
            }
            0x0C -> {
                alertLabel = "START_ALARM_IGNITION"
                humanMessage = "TAMPER ALERT: Ignition assembly bypass attempt detected without authorization keys!"
                isCritical = true
            }
            0x0E -> {
                alertLabel = "LOW_BATTERY_ALARM"
                humanMessage = "Hardware Warning: Internal device power dropping below operational minimums."
                isCritical = false
            }
            0x11 -> {
                alertLabel = "POWER_OFF_ALARM"
                humanMessage = "System Offline: Tracker unit has been forcefully powered off."
                isCritical = true
            }
            0x13 -> {
                alertLabel = "TAMPER_DEMOLITION_ALARM"
                humanMessage = "TAMPER ALERT: Hardware enclosure removal or structural damage detected!"
                isCritical = true
            }
            0x14 -> {
                alertLabel = "DOOR_OPEN_ALARM"
                humanMessage = "Security Breach: Vehicle door lock open event registered while system armed."
                isCritical = true
            }
            0x15 -> {
                alertLabel = "LOW_POWER_SHUTDOWN"
                humanMessage = "Power Cutoff: Device shutting down completely due to exhausted cell battery voltage."
                isCritical = false
            }
            else -> {
                alertLabel = "CUSTOM_ALERT_CODE_%02X".format(alarmCode)
                humanMessage = "System Alert: Unmapped metric code received from tracking module."
                isCritical = false
            }
        }

        return Pt20AlertResult(alertLabel, humanMessage, isCritical)
    }
}

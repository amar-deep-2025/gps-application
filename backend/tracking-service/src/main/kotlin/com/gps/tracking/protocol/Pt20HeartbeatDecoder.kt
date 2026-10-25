package com.gps.tracking.protocol

data class Pt20HeartbeatStatus(
    val terminalInfo: Int,
    val voltageLevel: Int,
    val gsmSignalLevel: Int,
    val gpsFixed: Boolean,
    val oilElectricDisconnected: Boolean,
    val lowBatteryFlag: Boolean,
    val powerFailureFlag: Boolean,
    val vibrationFlag: Boolean,
    val charging: Boolean,
    val accOn: Boolean,
    val armed: Boolean,
    val activeAlertName: String,     // System and database logging key
    val userFriendlyMessage: String  // Clean standard textual message for notifications
)

class Pt20HeartbeatDecoder {

    fun decodeHeartbeat(packet: Pt20Packet): Pt20HeartbeatStatus {
        require(packet.protocolNumber == 0x13) { "Invalid heartbeat protocol" }

        val content = packet.content
        require(content.size >= 5) { "Payload size error" }

        val terminalInfo = content[0].toInt() and 0xFF
        val voltageLevel = content[1].toInt() and 0xFF
        val gsmSignalLevel = content[2].toInt() and 0xFF

        require(voltageLevel in 0..6) { "Invalid voltage level" }
        require(gsmSignalLevel in 0..4) { "Invalid GSM level" }

        val alarmCode = (terminalInfo shr 3) and 0x07

        val isVibration = alarmCode == 0x01
        val isPowerCut = alarmCode == 0x02
        val isLowBattery = alarmCode == 0x03

        val isOilDisconnected = (terminalInfo and 0x80) != 0
        val isGpsFixed = (terminalInfo and 0x40) != 0
        val isCharging = (terminalInfo and 0x04) != 0
        val isAccOn = (terminalInfo and 0x02) != 0
        val isArmed = (terminalInfo and 0x01) != 0

        val alertLabel: String
        val humanMessage: String

        // Prioritized Threat Escalation Matrix (Critical safety alarms evaluated first)
        when {
            isPowerCut -> {
                alertLabel = "MAIN_POWER_DISCONNECTED"
                humanMessage = "Alert: Vehicle main power supply has been disconnected."
            }
            isLowBattery -> {
                alertLabel = "LOW_BATTERY_WARNING"
                humanMessage = "Warning: GPS tracker backup internal battery is critically low."
            }
            isVibration -> {
                alertLabel = "VIBRATION_ALERT"
                humanMessage = "Security: Unauthorized movement or vibration detected on your vehicle."
            }
            isOilDisconnected -> {
                alertLabel = "ENGINE_CUTOFF_ACTIVE"
                humanMessage = "Security: Immobilizer active. Engine ignition circuit is locked."
            }
            isAccOn -> {
                alertLabel = "ACC_IGNITION_ON"
                humanMessage = "Ignition On: Your vehicle engine has been started."
            }
            else -> {
                alertLabel = "ACC_IGNITION_OFF"
                humanMessage = "Ignition Off: Your vehicle engine has been shut down."
            }
        }

        return Pt20HeartbeatStatus(
            terminalInfo = terminalInfo,
            voltageLevel = voltageLevel,
            gsmSignalLevel = gsmSignalLevel,
            gpsFixed = isGpsFixed,
            oilElectricDisconnected = isOilDisconnected,
            lowBatteryFlag = isLowBattery,
            powerFailureFlag = isPowerCut,
            vibrationFlag = isVibration,
            charging = isCharging,
            accOn = isAccOn,
            armed = isArmed,
            activeAlertName = alertLabel,
            userFriendlyMessage = humanMessage
        )
    }

    fun decodeDedicatedAlarm(packet: Pt20Packet): Pair<String, String> {
        require(packet.protocolNumber == 0x26) { "Invalid alarm protocol" }
        val content = packet.content

        if (content.size < 30) {
            return Pair("GENERIC_ALARM_TRIGGERED", "Security Alert: Unusual vehicle activity detected.")
        }

        val alarmByteIndex = content.size - 4
        val alarmCode = content[alarmByteIndex].toInt() and 0xFF

        return when (alarmCode) {
            0x01 -> Pair("SOS_EMERGENCY_ALERT", "EMERGENCY: SOS panic button triggered! Immediate response required!")
            0x02 -> Pair("POWER_FAILURE_ALARM", "CRITICAL ALERT: Main wires cut! The tracking unit is running on backup battery!")
            0x03 -> Pair("VIBRATION_THEFT_ALERT", "THEFT WARNING: Abnormal shaking or impact detected. Please verify your vehicle location!")
            0x04 -> Pair("ENTER_FENCE_ALARM", "Geofence: Vehicle has entered your configured safe boundary zone.")
            0x05 -> Pair("EXIT_FENCE_ALARM", "GEOFENCE BREACH: Your vehicle has left the designated safe perimeter zone!")
            0x06 -> Pair("OVERSPEED_ALARM", "Speed Alert: Vehicle exceeded your set safe speed limit thresholds.")
            0x09 -> Pair("DISPLACEMENT_ALARM", "Towing Alert: Locked vehicle is shifting position without engine running!")
            0x0A -> Pair("ENTER_GPS_BLIND_AREA", "Signal Alert: Vehicle entered a tracking blind zone or underground structure.")
            0x0B -> Pair("EXIT_GPS_BLIND_AREA", "Signal Restored: Satellite lock re-established successfully.")
            0x0C -> Pair("START_ALARM_IGNITION", "TAMPER ALERT: Ignition assembly bypass attempt detected without authorization keys!")
            0x0E -> Pair("LOW_BATTERY_ALARM", "Hardware Warning: Internal device power dropping below operational minimums.")
            0x11 -> Pair("POWER_OFF_ALARM", "System Offline: Tracker unit has been forcefully powered off.")
            0x13 -> Pair("TAMPER_DEMOLITION_ALARM", "TAMPER ALERT: Hardware enclosure removal or structural damage detected!")
            0x14 -> Pair("DOOR_OPEN_ALARM", "Security Breach: Vehicle door lock open event registered while system armed.")
            0x15 -> Pair("LOW_POWER_SHUTDOWN", "Power Cutoff: Device shutting down completely due to exhausted cell battery voltage.")
            else -> Pair("CUSTOM_ALERT_CODE_%02X".format(alarmCode), "System Alert: Unmapped metric code received from tracking module.")
        }
    }
}

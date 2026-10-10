package com.example.idatum.dlms

import java.util.Locale

/**
 * Utility functions for HDLC framing, DLMS/COSEM APDU building,
 * checksum calculation (FCS-16), and hex formatting.
 */
object DlmsPacketUtils {

    fun formatHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { String.format(Locale.US, "%02X", it) }
    }

    fun parseHex(hex: String): ByteArray {
        val clean = hex.replace(" ", "").replace(":", "")
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            result[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }

    /**
     * Compute CRC-16 / FCS-16 for HDLC framing (RFC 1662 / IEC 62056-46)
     */
    fun computeFcs16(data: ByteArray, offset: Int = 1, length: Int = data.size - 3): Int {
        var fcs = 0xFFFF
        for (i in offset until (offset + length)) {
            val byte = data[i].toInt() and 0xFF
            fcs = (fcs ushr 8) xor FCS_TABLE[(fcs xor byte) and 0xFF]
        }
        return fcs xor 0xFFFF
    }

    private val FCS_TABLE = IntArray(256).apply {
        for (i in 0..255) {
            var v = i
            for (j in 0..7) {
                v = if ((v and 1) != 0) (v ushr 1) xor 0x8408 else (v ushr 1)
            }
            this[i] = v
        }
    }

    /**
     * Build an HDLC SNRM Frame (Set Normal Response Mode)
     * Target: Server logical device 1 (Meter Management LD), Client Address 0x10 (Public/MR) or 0x30 (US)
     */
    fun buildSnrmFrame(clientAddress: Byte = 0x10.toByte(), serverAddress: Short = 0x01): ByteArray {
        // Standard HDLC SNRM frame: 7E A0 0A 00 02 00 23 21 93 E6 E7 7E
        val frame = byteArrayOf(
            0x7E.toByte(),
            0xA0.toByte(), 0x0A.toByte(), // Frame type + length (10 bytes)
            (serverAddress.toInt() ushr 7).toByte(), (serverAddress.toInt() and 0x7F or 0x01).toByte(),
            (clientAddress.toInt() or 0x01).toByte(),
            0x93.toByte(), // SNRM command
            0x00, 0x00,    // Placeholder FCS
            0x7E.toByte()
        )
        val fcs = computeFcs16(frame, 1, 6)
        frame[7] = (fcs and 0xFF).toByte()
        frame[8] = ((fcs ushr 8) and 0xFF).toByte()
        return frame
    }

    /**
     * Build standard DLMS AARQ (Association Request)
     */
    fun buildAarqFrame(clientAddress: Byte = 0x10.toByte(), systemTitle: String = "INTL0001"): ByteArray {
        val payload = "7EA02BA002002321106031A109060760857405080101A203020100A305A103020100BE10040E01000000065F1F0400001E1D0000347E"
        return parseHex(payload)
    }

    /**
     * Build a GET-Request-Normal for a given OBIS code
     */
    fun buildGetRequest(obis: String, classId: Int = 1, attributeId: Int = 2): ByteArray {
        val obisBytes = obis.split(".").map { it.toInt().toByte() }.toByteArray()
        val classHi = ((classId ushr 8) and 0xFF).toByte()
        val classLo = (classId and 0xFF).toByte()

        val apdu = byteArrayOf(
            0xC0.toByte(), // GET-Request
            0x01.toByte(), // get-request-normal
            0x81.toByte(), // invoke-id-and-priority
            classHi, classLo,
            obisBytes.getOrElse(0) { 0 },
            obisBytes.getOrElse(1) { 0 },
            obisBytes.getOrElse(2) { 0 },
            obisBytes.getOrElse(3) { 0 },
            obisBytes.getOrElse(4) { 0 },
            obisBytes.getOrElse(5) { 255.toByte() },
            attributeId.toByte(),
            0x00 // access-selection null
        )
        return wrapInHdlcIFrame(apdu)
    }

    /**
     * Build an ACTION-Request-Normal (e.g. Disconnect control, MD Reset)
     */
    fun buildActionRequest(obis: String, classId: Int = 70, methodId: Int = 1): ByteArray {
        val obisBytes = obis.split(".").map { it.toInt().toByte() }.toByteArray()
        val classHi = ((classId ushr 8) and 0xFF).toByte()
        val classLo = (classId and 0xFF).toByte()

        val apdu = byteArrayOf(
            0xC3.toByte(), // ACTION-Request
            0x01.toByte(), // action-request-normal
            0x81.toByte(), // invoke-id-and-priority
            classHi, classLo,
            obisBytes.getOrElse(0) { 0 },
            obisBytes.getOrElse(1) { 0 },
            obisBytes.getOrElse(2) { 0 },
            obisBytes.getOrElse(3) { 0 },
            obisBytes.getOrElse(4) { 0 },
            obisBytes.getOrElse(5) { 255.toByte() },
            methodId.toByte(),
            0x01.toByte(), // data present
            0x0F.toByte(), // integer 8
            0x00.toByte()
        )
        return wrapInHdlcIFrame(apdu)
    }

    /**
     * Wrap DLMS APDU into an LLC (E6 E6 00) and HDLC I-Frame
     */
    private fun wrapInHdlcIFrame(apdu: ByteArray): ByteArray {
        val llc = byteArrayOf(0xE6.toByte(), 0xE6.toByte(), 0x00.toByte())
        val data = llc + apdu
        val totalLen = data.size + 9
        val header = byteArrayOf(
            0x7E.toByte(),
            (0xA0 or ((totalLen ushr 8) and 0x07)).toByte(),
            (totalLen and 0xFF).toByte(),
            0x02.toByte(), 0x23.toByte(), // Server address
            0x21.toByte(),                // Client address
            0x10.toByte()                 // Control (I-frame)
        )
        val headerFcs = computeFcs16(header, 1, 5)
        val hFcsBytes = byteArrayOf((headerFcs and 0xFF).toByte(), ((headerFcs ushr 8) and 0xFF).toByte())

        val content = header + hFcsBytes + data
        val dataFcs = computeFcs16(content, 1, content.size - 1)
        val dFcsBytes = byteArrayOf((dataFcs and 0xFF).toByte(), ((dataFcs ushr 8) and 0xFF).toByte())

        return content + dFcsBytes + byteArrayOf(0x7E.toByte())
    }
}

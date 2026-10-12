package dev.compromisso.app.network

object Ipv4UdpPacket {
    data class DnsPacket(
        val sourceAddress: ByteArray,
        val destinationAddress: ByteArray,
        val sourcePort: Int,
        val destinationPort: Int,
        val identification: Int,
        val dnsPayload: ByteArray,
    )

    fun parseDns(packet: ByteArray, length: Int): DnsPacket? {
        if (length !in 28..packet.size) return null
        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null
        val headerLength = (packet[0].toInt() and 0x0F) * 4
        if (headerLength < 20 || length < headerLength + 8) return null
        if ((packet[9].toInt() and 0xFF) != UDP_PROTOCOL) return null
        if (ipv4Checksum(packet, 0, headerLength) != 0) return null

        val flagsAndOffset = readU16(packet, 6)
        // Reject every fragment, including first fragments with the MF bit set.
        if ((flagsAndOffset and 0x3FFF) != 0) return null

        val totalLength = readU16(packet, 2)
        if (totalLength > length || totalLength < headerLength + 8) return null
        val udpOffset = headerLength
        val udpLength = readU16(packet, udpOffset + 4)
        if (udpLength < 8 || udpOffset + udpLength != totalLength) return null
        // IPv4 permits zero UDP checksum; a non-zero checksum MUST be valid.
        // Reject corrupted packets instead of forwarding their DNS payload.
        if (readU16(packet, udpOffset + 6) != 0 &&
            !hasValidUdpChecksum(packet, udpOffset, udpLength)
        ) return null

        val sourcePort = readU16(packet, udpOffset)
        val destinationPort = readU16(packet, udpOffset + 2)
        if (destinationPort != DNS_PORT) return null
        if (!packet.copyOfRange(16, 20).contentEquals(DNS_TUNNEL_ADDRESS)) return null

        val payloadOffset = udpOffset + 8
        return DnsPacket(
            sourceAddress = packet.copyOfRange(12, 16),
            destinationAddress = packet.copyOfRange(16, 20),
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            identification = readU16(packet, 4),
            dnsPayload = packet.copyOfRange(payloadOffset, udpOffset + udpLength),
        )
    }

    fun buildResponse(request: DnsPacket, dnsPayload: ByteArray): ByteArray {
        val totalLength = 20 + 8 + dnsPayload.size
        require(totalLength <= 65_535)
        val packet = ByteArray(totalLength)

        packet[0] = 0x45
        packet[1] = 0
        writeU16(packet, 2, totalLength)
        writeU16(packet, 4, request.identification)
        writeU16(packet, 6, 0)
        packet[8] = 64
        packet[9] = UDP_PROTOCOL.toByte()
        writeU16(packet, 10, 0)
        request.destinationAddress.copyInto(packet, 12)
        request.sourceAddress.copyInto(packet, 16)
        writeU16(packet, 10, ipv4Checksum(packet, 0, 20))

        val udpOffset = 20
        writeU16(packet, udpOffset, DNS_PORT)
        writeU16(packet, udpOffset + 2, request.sourcePort)
        writeU16(packet, udpOffset + 4, 8 + dnsPayload.size)
        writeU16(packet, udpOffset + 6, 0) // UDP checksum may be zero for IPv4
        dnsPayload.copyInto(packet, udpOffset + 8)

        return packet
    }

    private fun ipv4Checksum(bytes: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var index = offset
        while (index < offset + length) {
            val word = ((bytes[index].toInt() and 0xFF) shl 8) or
                (bytes[index + 1].toInt() and 0xFF)
            sum += word.toLong()
            while ((sum ushr 16) != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
            index += 2
        }
        return sum.inv().toInt() and 0xFFFF
    }

    private fun hasValidUdpChecksum(packet: ByteArray, udpOffset: Int, udpLength: Int): Boolean {
        var sum = 0L
        fun addWord(word: Int) {
            sum += word.toLong()
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        // IPv4 pseudo-header: source, destination, zero + protocol, UDP length.
        for (i in 12 until 20 step 2) addWord(readU16(packet, i))
        addWord(UDP_PROTOCOL)
        addWord(udpLength)
        for (i in udpOffset until udpOffset + udpLength step 2) {
            val high = packet[i].toInt() and 0xFF
            val low = if (i + 1 < udpOffset + udpLength) packet[i + 1].toInt() and 0xFF else 0
            addWord((high shl 8) or low)
        }
        sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum == 0xFFFFL
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun writeU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 1] = (value and 0xFF).toByte()
    }

    private const val UDP_PROTOCOL = 17
    private const val DNS_PORT = 53
    private val DNS_TUNNEL_ADDRESS = byteArrayOf(10, 111, 222.toByte(), 1)
}

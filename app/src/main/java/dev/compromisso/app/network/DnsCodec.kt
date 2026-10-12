package dev.compromisso.app.network

object DnsCodec {
    data class Question(
        val name: String,
        val questionEnd: Int,
        val type: Int,
        val queryClass: Int,
    )

    fun readFirstQuestion(message: ByteArray): Question? = parseQuestion(message, expectedResponse = false)

    private fun parseQuestion(message: ByteArray, expectedResponse: Boolean): Question? {
        if (message.size < DNS_HEADER_SIZE) return null
        val questionCount = readU16(message, 4)
        if (questionCount != 1) return null
        val flags = readU16(message, 2)
        if (((flags and 0x8000) != 0) != expectedResponse) return null
        if ((flags and 0x7800) != 0) return null // standard query/reply only
        if (!expectedResponse && (readU16(message, 6) != 0 || readU16(message, 8) != 0)) return null

        var offset = DNS_HEADER_SIZE
        val labels = ArrayList<String>(4)
        var terminated = false
        while (offset < message.size) {
            val length = message[offset].toInt() and 0xFF
            if (length == 0) {
                offset += 1
                terminated = true
                break
            }
            if ((length and 0xC0) != 0) return null
            if (length > 63 || offset + 1 + length > message.size) return null
            labels += message.copyOfRange(offset + 1, offset + 1 + length)
                .toString(Charsets.US_ASCII)
            offset += 1 + length
        }
        if (!terminated || labels.isEmpty() || offset + 4 > message.size) return null
        if (labels.sumOf { it.length + 1 } > 253) return null
        return Question(
            name = labels.joinToString("."),
            questionEnd = offset + 4,
            type = readU16(message, offset),
            queryClass = readU16(message, offset + 2),
        )
    }

    /** Refuse replies that do not belong to the original request. */
    fun isMatchingResponse(query: ByteArray, response: ByteArray): Boolean {
        if (query.size < DNS_HEADER_SIZE || response.size < DNS_HEADER_SIZE) return false
        if (query[0] != response[0] || query[1] != response[1]) return false
        val requestFlags = readU16(query, 2)
        val responseFlags = readU16(response, 2)
        if ((requestFlags and 0x8000) != 0 || (responseFlags and 0x8000) == 0) return false
        if ((requestFlags and 0x7800) != (responseFlags and 0x7800)) return false
        val requestQuestion = readFirstQuestion(query) ?: return false
        val responseQuestion = parseQuestion(response, expectedResponse = true) ?: return false
        return requestQuestion.name.equals(responseQuestion.name, ignoreCase = true) &&
            requestQuestion.type == responseQuestion.type &&
            requestQuestion.queryClass == responseQuestion.queryClass
    }

    fun buildNxDomain(query: ByteArray): ByteArray? = buildErrorResponse(query, rcode = 3)

    fun buildServerFailure(query: ByteArray): ByteArray? = buildErrorResponse(query, rcode = 2)

    private fun buildErrorResponse(query: ByteArray, rcode: Int): ByteArray? {
        val question = readFirstQuestion(query) ?: return null
        val response = query.copyOf(question.questionEnd)
        val requestFlags = readU16(query, 2)
        val responseFlags = 0x8000 or // QR: response
            (requestFlags and 0x7800) or // opcode
            (requestFlags and 0x0100) or // recursion desired
            0x0080 or // recursion available
            (requestFlags and 0x0010) or // checking disabled
            (rcode and 0x000F)

        writeU16(response, 2, responseFlags)
        writeU16(response, 6, 0) // answers
        writeU16(response, 8, 0) // authority
        writeU16(response, 10, 0) // additional
        return response
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun writeU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 1] = (value and 0xFF).toByte()
    }

    private const val DNS_HEADER_SIZE = 12
}

import dev.compromisso.app.domain.DomainPolicyEngine
import dev.compromisso.app.network.DnsCodec
import dev.compromisso.app.network.DnsProbeQuery
import dev.compromisso.app.network.DnsOverTlsCodec
import dev.compromisso.app.network.Ipv4UdpPacket
import dev.compromisso.app.security.PinLockoutClock
import dev.compromisso.app.policy.ProtectionRuntime
import dev.compromisso.app.policy.ProtectionState
import dev.compromisso.app.security.RateLimitPolicy
import kotlin.random.Random

fun request(): ByteArray {
    val bytes = byteArrayOf(0x12,0x34,0x01,0x00,0x00,0x01,0,0,0,0,0,0,7,
        101,120,97,109,112,108,101,3,99,111,109,0,0,1,0,1)
    return bytes
}

fun buildIp(payload: ByteArray): ByteArray {
    val n = 20 + 8 + payload.size
    val bytes = ByteArray(n)
    bytes[0] = 0x45; bytes[2] = (n ushr 8).toByte(); bytes[3] = n.toByte()
    bytes[8] = 64; bytes[9] = 17
    byteArrayOf(10,111,222.toByte(),2).copyInto(bytes,12)
    byteArrayOf(10,111,222.toByte(),1).copyInto(bytes,16)
    var sum = 0
    for (i in 0 until 20 step 2) sum += ((bytes[i].toInt() and 255) shl 8) or (bytes[i+1].toInt() and 255)
    while (sum > 0xffff) sum = (sum and 0xffff) + (sum ushr 16)
    val checksum = sum.inv() and 0xffff
    bytes[10] = (checksum ushr 8).toByte(); bytes[11] = checksum.toByte()
    bytes[20] = 0x30; bytes[21] = 0x39; bytes[22] = 0; bytes[23] = 53
    val udpSize = n - 20
    bytes[24] = (udpSize ushr 8).toByte(); bytes[25] = udpSize.toByte()
    payload.copyInto(bytes,28)
    return bytes
}

fun withValidUdpChecksum(packet: ByteArray): ByteArray {
    val result = packet.copyOf()
    val length = ((result[24].toInt() and 255) shl 8) or (result[25].toInt() and 255)
    var sum = 0L
    fun word(value: Int) {
        sum += value.toLong()
        sum = (sum and 0xFFFF) + (sum ushr 16)
    }
    for (i in 12 until 20 step 2) word(((result[i].toInt() and 255) shl 8) or (result[i + 1].toInt() and 255))
    word(17)
    word(length)
    for (i in 20 until 20 + length step 2) {
        val hi = result[i].toInt() and 255
        val lo = if (i + 1 < 20 + length) result[i + 1].toInt() and 255 else 0
        word((hi shl 8) or lo)
    }
    sum = (sum and 0xFFFF) + (sum ushr 16)
    val checksum = (sum.inv().toInt() and 0xFFFF).let { if (it == 0) 0xFFFF else it }
    result[26] = (checksum ushr 8).toByte()
    result[27] = checksum.toByte()
    return result
}

fun main() {
    val q = request()
    val resp = q.copyOf().apply { this[2] = 0x81.toByte(); this[3] = 0x80.toByte() }
    check(DnsCodec.isMatchingResponse(q, resp))
    val probe = DnsProbeQuery.request()
    val probeResponse = probe.copyOf().apply { this[2] = 0x81.toByte(); this[3] = 0x80.toByte() }
    check(DnsProbeQuery.isHealthy(probeResponse))
    check(!DnsProbeQuery.isHealthy(byteArrayOf(1)))
    check(!DnsProbeQuery.isHealthy(probeResponse.copyOf().apply { this[3] = 0x82.toByte() }))
    ProtectionRuntime.markStarting()
    check(!ProtectionRuntime.active)
    ProtectionRuntime.markDegraded()
    check(ProtectionRuntime.state == ProtectionState.DEGRADED)
    ProtectionRuntime.markOperational()
    check(ProtectionRuntime.state == ProtectionState.OPERATIONAL)
    ProtectionRuntime.markRulesApplied(12)
    check(ProtectionRuntime.appliedRulesRevision == 12L)
    ProtectionRuntime.markInactive()
    check(ProtectionRuntime.appliedRulesRevision == -1L)
    check(!DnsCodec.isMatchingResponse(q, resp.copyOf().apply { this[1] = 0x35 }))
    check(!DnsCodec.isMatchingResponse(q, resp.copyOf().apply { this[lastIndex - 2] = 28 }))
    check(!DnsCodec.isMatchingResponse(q, resp.copyOf().apply { this[lastIndex] = 3 }))
    check(!DnsCodec.isMatchingResponse(q, q))
    check(DnsCodec.buildNxDomain(q) != null)
    check(DnsCodec.buildServerFailure(q) != null)
    check(DnsCodec.readFirstQuestion(resp) == null)
    check(DnsCodec.readFirstQuestion(q.copyOf().apply { this[7] = 1 }) == null)
    check(DnsOverTlsCodec.decodeLength(0, 29) == q.size)
    check(DnsOverTlsCodec.encode(q).size == q.size + 2)
    check(DomainPolicyEngine.normalize("exámple.com") == "xn--exmple-qta.com")
    check(DomainPolicyEngine.isBlocked("sub.example.com", setOf("example.com")))
    check(!DomainPolicyEngine.isBlocked("badexample.com", setOf("example.com")))
    check(DomainPolicyEngine.normalize("example.com@other.com") == null)
    check(RateLimitPolicy.delayMsForFailedAttempts(8) == 300_000L)
    check(PinLockoutClock.remainingMs(999999999, 20000, 120000, 60000, 10000) == 40000L)
    check(PinLockoutClock.remainingMs(90000, 1000, 100000, 60000, 10000) == 10000L)
    val valid = buildIp(q)
    check(Ipv4UdpPacket.parseDns(valid, valid.size)?.dnsPayload?.contentEquals(q) == true)
    val validChecksummed = withValidUdpChecksum(valid)
    check(Ipv4UdpPacket.parseDns(validChecksummed, validChecksummed.size)?.dnsPayload?.contentEquals(q) == true)
    check(Ipv4UdpPacket.parseDns(validChecksummed.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }, validChecksummed.size) == null)
    check(Ipv4UdpPacket.parseDns(valid.copyOf().apply { this[26] = 1; this[27] = 1 }, valid.size) == null) // bad UDP checksum
    check(Ipv4UdpPacket.parseDns(valid.copyOf().apply { this[7] = 1 }, valid.size) == null) // fragments
    check(Ipv4UdpPacket.parseDns(valid.copyOf().apply { this[6] = 0x20 }, valid.size) == null) // MF
    check(Ipv4UdpPacket.parseDns(valid.copyOf().apply { this[2] = 0xff.toByte() }, valid.size) == null)
    check(Ipv4UdpPacket.parseDns(valid.copyOf().apply { this[16] = 8 }, valid.size) == null)
    check(Ipv4UdpPacket.parseDns(valid.copyOf().apply { this[8] = 12 }, valid.size) == null)
    var rejected = 0
    val random = Random(913472)
    repeat(40_000) {
        val len = random.nextInt(0, 1537)
        val packet = ByteArray(len).also(random::nextBytes)
        if (Ipv4UdpPacket.parseDns(packet, len) == null) rejected++
        DnsCodec.readFirstQuestion(packet)
    }
    println("PURE_SECURITY_SMOKE_OK cases=29 random_inputs=40000 rejected=$rejected")
}

// SPDX-License-Identifier: LGPL-3.0-only
// Kotlin adaptation of Wenku8 / rany2 edge-tts protocol, Copyright (c) 2025- rany.
package io.legado.app.help.readaloud.wenku8

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object Wenku8EdgeProtocol {
    const val version = "2-143.0.3650.75"
    const val clientIdentifier = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val mask = 0xffffffffL
    private val initial = longArrayOf(0x6a09e667L, 0xbb67ae85L, 0x3c6ef372L, 0xa54ff53aL, 0x510e527fL, 0x9b05688cL, 0x1f83d9abL, 0x5be0cd19L)
    private val working = longArrayOf(0xfd9fbea2L, 0x91243ce7L, 0xbe499094L, 0xcd2e1e0aL, 0x6c006f49L, 0x04bd35abL, 0xfeb0a7e4L, 0x03effbd2L)
    private val schedule = longArrayOf(0xeb24bf9eL, 0x1558691fL, 0xa0c09ee2L, 0x0cfa32baL, 0xa622ee8cL, 0xd1dd9491L, 0xc0dd25a0L, 0xf9e70ab6L, 0xf0c31573L, 0x7058ce4eL, 0x7188e606L, 0x00e5ab33L, 0xd258b060L, 0x72766564L)
    private val roundConstants = longArrayOf(0x428a2f98L, 0x71374491L, 0xb5c0fbcfL, 0xe9b5dba5L, 0x3956c25bL, 0x59f111f1L, 0x923f82a4L, 0xab1c5ed5L, 0xd807aa98L, 0x12835b01L, 0x243185beL, 0x550c7dc3L, 0x72be5d74L, 0x80deb1feL, 0x9bdc06a7L, 0xc19bf174L, 0xe49b69c1L, 0xefbe4786L, 0x0fc19dc6L, 0x240ca1ccL, 0x2de92c6fL, 0x4a7484aaL, 0x5cb0a9dcL, 0x76f988daL, 0x983e5152L, 0xa831c66dL, 0xb00327c8L, 0xbf597fc7L, 0xc6e00bf3L, 0xd5a79147L, 0x06ca6351L, 0x14292967L, 0x27b70a85L, 0x2e1b2138L, 0x4d2c6dfcL, 0x53380d13L, 0x650a7354L, 0x766a0abbL, 0x81c2c92eL, 0x92722c85L, 0xa2bfe8a1L, 0xa81a664bL, 0xc24b8b70L, 0xc76c51a3L, 0xd192e819L, 0xd6990624L, 0xf40e3585L, 0x106aa070L, 0x19a4c116L, 0x1e376c08L, 0x2748774cL, 0x34b0bcb5L, 0x391c0cb3L, 0x4ed8aa4aL, 0x5b9cca4fL, 0x682e6ff3L, 0x748f82eeL, 0x78a5636fL, 0x84c87814L, 0x8cc70208L, 0x90befffaL, 0xa4506cebL, 0xbef9a3f7L, 0xc67178f2L)
    private val coefficients = longArrayOf(0xe82e91b1L, 0xb32700d0L, 0x44de24c6L, 0x88dff51dL, 0x3beb65bcL, 0x29514038L, 0xc89c5933L, 0xb328cbdfL, 0x8d1ec211L, 0xd405b4d5L, 0x314ef5d5L, 0x26e55f3cL, 0xe92491b1L, 0x607801c6L, 0xf0a5b9d5L, 0x82b2c1fbL, 0x06064cf3L, 0xfa51a3daL, 0xda79fa60L, 0x207cc464L, 0xbed20560L, 0x3c3c74f3L, 0x89f9c9f8L, 0xd2d14b61L, 0x35fa38c6L, 0xbb04a2c1L, 0xcb49e3c6L, 0xc5626167L)
    fun timeProof(epochMillis: Long): String {
        val seconds = Math.floorDiv(epochMillis, 1000L) + 11644473600L
        val window = seconds - Math.floorMod(seconds, 300L)
        val data = "${window * 10000000L}$clientIdentifier".toByteArray(Charsets.US_ASCII)
        val digest = digest(data)
        val (a, b) = cs64(data, digest)
        val bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(a.toInt()).putInt(b.toInt()).array()
        return hex(bytes) + "-" + hex(digest)
    }
    private fun hex(data: ByteArray) = data.joinToString("") { "%02X".format(it.toInt() and 255) }
    private fun rotate(x: Long, n: Int) = ((x ushr n) or (x shl (32 - n))) and mask
    private fun swap(x: Long) = ((x shl 16) or ((x and mask) ushr 16)) and mask
    private fun sigma0(x: Long) = rotate(x, 7) xor rotate(x, 18) xor (x ushr 3)
    private fun sigma1(x: Long) = rotate(x, 17) xor rotate(x, 19) xor (x ushr 10)
    private fun expand(words: MutableList<Long>) {
        for (i in words.size until 64) words.add((sigma1(words[i-2]) + words[i-7] + sigma0(words[i-15]) + words[i-16]) and mask)
    }
    private fun rounds(state: LongArray, words: List<Long>, start: Int = 0): LongArray {
        var a=state[0]; var b=state[1]; var c=state[2]; var d=state[3]
        var e=state[4]; var f=state[5]; var g=state[6]; var h=state[7]
        for (i in start until 64) {
            val constant = if (start == 14 && i == 14) 0xc9dc06a7L else roundConstants[i]
            val t1 = h + (rotate(e,6) xor rotate(e,11) xor rotate(e,25)) + (g xor (e and (f xor g))) + constant + words[i]
            val t2 = (rotate(a,2) xor rotate(a,13) xor rotate(a,22)) + ((a and b) or (c and (a or b)))
            h=g; g=f; f=e; e=(d+t1) and mask; d=c; c=b; b=a; a=(t1+t2) and mask
        }
        return longArrayOf(a,b,c,d,e,f,g,h)
    }
    private fun digest(data: ByteArray): ByteArray {
        val prefix = ByteBuffer.wrap(data)
        val words = MutableList(14) { 0L }
        words += ((data[0].toLong() and 255) shl 16) or ((data[1].toLong() and 255) shl 8) or (data[2].toLong() and 255)
        words += prefix.getInt(3).toLong() and mask
        for (i in 16 until 30) {
            var value = schedule[i-16]
            if (i-2 >= 14) value += sigma1(words[i-2]) xor (if (i==16) 0x000b92c0L else 0L)
            if (i-7 >= 14) value += words[i-7]
            if (i-15 >= 14) value += sigma0(words[i-15]) xor (if (i==29) 0x059c0b80L else 0L)
            words += value and mask
        }
        words += (sigma1(words[28]) + words[23] + sigma0(words[15]) + words[14] + 0xc9dc06a7L - roundConstants[14]) and mask
        expand(words)
        val workingResult = rounds(working, words, 14)
        var state = LongArray(8) { (initial[it] + workingResult[it]) and mask }
        val length = 57 + data.size
        val padding = Math.floorMod(55 - length, 64)
        val tail = ByteArray(data.size - 7 + 1 + padding + 8)
        data.copyInto(tail, 0, 7)
        tail[data.size-7] = 0x80.toByte()
        val view = ByteBuffer.wrap(tail).putLong(tail.size-8, length.toLong()*8)
        for (offset in tail.indices step 64) {
            val block = MutableList(16) { view.getInt(offset+it*4).toLong() and mask }
            expand(block)
            val result = rounds(state, block)
            state = LongArray(8) { (state[it] + result[it]) and mask }
        }
        return ByteBuffer.allocate(32).apply { state.forEach { putInt(it.toInt()) } }.array()
    }
    private fun cs64(data: ByteArray, digest: ByteArray): Pair<Long,Long> {
        val key = ByteBuffer.wrap(digest).order(ByteOrder.LITTLE_ENDIAN)
        val keys = longArrayOf((key.getInt(0).toLong() and mask) or 1, (key.getInt(4).toLong() and mask) or 1)
        val factors = keys.map { (it * 0x82893933L) and mask }
        val terms = MutableList(coefficients.size/2) { (coefficients[it*2] * factors[it%2]) and mask }
        val offsets = MutableList(coefficients.size/2) { coefficients[it*2+1] }
        val padded = ByteArray(((data.size+1+7)/8)*8)
        data.copyInto(padded,1)
        val view = ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until padded.size/4) {
            val word = view.getInt(i*4).toLong() and mask
            var term = word * keys[i%2]
            var offset = swap(word) * (if (i%2==0) 0xef0569fbL else 0xc31713dbL)
            if (i==0) { term += 0xf2f90b1aL*factors[0]; offset += 0x0b1a0000L }
            terms += term and mask; offsets += offset and mask
        }
        var swapAcc=0L; var swapLast=0L; var state=0L; var acc=0L
        for (i in terms.indices step 2) {
            val x=terms[i]; val y=terms[i+1]
            var q=(swap((offsets[i]+x) and mask)*0x76f50f34L) and mask
            swapAcc += (swap(q)*0xc3efea97L+q) and mask
            q=(swap((offsets[i+1]+y) and mask)*0x1881ce3cL) and mask
            swapLast=(swap(q)*0x35bd1ec9L+q) and mask
            swapAcc=(swapAcc+swapLast) and mask
            state=(state*keys[0]+x) and mask
            for (factor in longArrayOf(0xcf98b111L,0x87085b9fL,0x12ceb96dL,0x257e1d83L)) state=(swap(state)*factor) and mask
            acc += state
            var t=(state*keys[1]+y) and mask
            for (factor in longArrayOf(0xa27416f5L,0xd38396ffL,0x7c932b89L,0xbfa49f69L)) t=(swap(t)*factor) and mask
            state=t; acc=(acc+state) and mask
        }
        return (swapLast xor state) to (swapAcc xor acc)
    }
}

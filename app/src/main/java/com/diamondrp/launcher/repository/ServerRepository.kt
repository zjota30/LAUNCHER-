package com.diamondrp.launcher.repository

import com.diamondrp.launcher.network.models.ServerStatus
import com.diamondrp.launcher.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Consulta o status ao vivo do servidor usando o protocolo de query público do SA-MP
 * (pacote "SAMP" + opcode 'i' = informações básicas: hostname, players, max players, ping).
 * Referência: documentação pública do protocolo SA-MP/open.mp server query.
 */
class ServerRepository {

    suspend fun queryServer(ip: String, port: Int): ServerStatus = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            val address = InetAddress.getByName(ip)
            socket = DatagramSocket().apply {
                soTimeout = Constants.SAMP_QUERY_TIMEOUT_MS
            }

            val packetData = buildQueryPacket(address, port, 'i')
            val sendPacket = DatagramPacket(packetData, packetData.size, address, port)

            val startTime = System.currentTimeMillis()
            socket.send(sendPacket)

            val buffer = ByteArray(2048)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)
            val pingMs = System.currentTimeMillis() - startTime

            parseInfoResponse(receivePacket.data, receivePacket.length, pingMs)
        } catch (e: IOException) {
            ServerStatus.offline()
        } catch (e: Exception) {
            ServerStatus.offline()
        } finally {
            socket?.close()
        }
    }

    private fun buildQueryPacket(address: InetAddress, port: Int, opcode: Char): ByteArray {
        val hostBytes = address.hostAddress!!.split(".").map { it.toInt().toByte() }
        val buffer = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put('S'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('M'.code.toByte())
        buffer.put('P'.code.toByte())
        hostBytes.forEach { buffer.put(it) }
        buffer.putShort(port.toShort())
        buffer.put(opcode.code.toByte())
        return buffer.array()
    }

    private fun parseInfoResponse(data: ByteArray, length: Int, pingMs: Long): ServerStatus {
        // Cabeçalho: "SAMP" (4) + ip (4) + port (2) + opcode (1) = 11 bytes, dados úteis começam após isso
        val buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(11)

        val password = buffer.get()
        val players = buffer.short.toInt() and 0xFFFF
        val maxPlayers = buffer.short.toInt() and 0xFFFF

        val hostnameLen = buffer.int
        val hostnameBytes = ByteArray(hostnameLen)
        buffer.get(hostnameBytes)
        val hostname = String(hostnameBytes, Charsets.UTF_8)

        val gameModeLen = buffer.int
        val gameModeBytes = ByteArray(gameModeLen)
        buffer.get(gameModeBytes)
        val gameMode = String(gameModeBytes, Charsets.UTF_8)

        return ServerStatus(
            isOnline = true,
            playersOnline = players,
            maxPlayers = maxPlayers,
            pingMs = pingMs,
            hostname = hostname,
            gameMode = gameMode
        )
    }
}

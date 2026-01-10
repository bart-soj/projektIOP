package com.example.projektiop.data.api.websocket

import android.util.Log
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.domain.models.Message
import com.example.projektiop.domain.models.base64
import com.example.projektiop.util.InstantAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dev.icerock.moko.socket.Socket
import dev.icerock.moko.socket.SocketEvent
import dev.icerock.moko.socket.SocketOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant

class SocketManager(private val url: String, private val sharedDataSource: SharedDataSource) {
    private val _wsEventFlow = MutableSharedFlow<ChatEvent>()
    val wsEventFlow: SharedFlow<ChatEvent> = _wsEventFlow.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val KEY_TOKEN = "auth_token"
    private val token: String
        get() {
           return sharedDataSource.get(KEY_TOKEN, "")
        }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()

    val socket: Socket = Socket(
        endpoint = url,
        config = SocketOptions(
            queryParams = mapOf("token" to token),
            transport = SocketOptions.Transport.WEBSOCKET
        ),
        build = {

            on("receive") { payload ->
                scope.launch {
                    Log.d("SOCC", "in receive man")
                    val json = JsonParser.parseString(payload).asJsonObject
                    val message = gson.fromJson<Message>(payload, Message::class.java)
                    _wsEventFlow.emit(ChatEvent.Receive(message.chatId, message))
                }
            }

            on("writing") { chatId ->
                scope.launch {
                    Log.d("SOCC", "in writing man")
                    _wsEventFlow.emit(ChatEvent.Writing(chatId))
                }
            }

            on(SocketEvent.Error) { args -> Log.e("SOCC", "Socket Error: $args") }
            on(SocketEvent.Reconnect) { args -> Log.e("SOCC", "Reconnect") }
            on(SocketEvent.Connect) { Log.d("SOCC", "Connected") }
            on(SocketEvent.Disconnect) { Log.d("SOCC", "Disconnected") }
        }
    )

    fun connect() {
        scope.launch {
            if (!socket.isConnected()) {
                socket.connect()
            }
        }
    }

    data class SocketMessage(
        val chatId: String,
        val content: base64,
    )

    fun emit(event: ChatEvent) {
        scope.launch {
            when(event) {
                is ChatEvent.Send -> {
                    Log.d("SOCC", "out send man")
                    val payload = gson.toJson(SocketMessage(event.chatId, event.content))
                    socket.emit("send", payload)
                }
                is ChatEvent.Writing -> {
                    Log.d("SOCC", "out writing man")
                    socket.emit("writing", event.chatId)
                }

                is ChatEvent.Receive -> {}
            }
        }
    }

    fun disconnect() {
        scope.launch {
            socket.disconnect()
        }
    }
}
package com.example.projektiop.data.api.websocket

import android.util.Log
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.domain.models.Message
import com.example.projektiop.domain.models.base64
import com.example.projektiop.util.InstantAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    private val _chatEventFlow= MutableSharedFlow<ChatEvent>()
    val chatEventFlow: SharedFlow<ChatEvent> = _chatEventFlow.asSharedFlow()

    private val _appEventFlow= MutableSharedFlow<AppEvent>()
    val appEventFlow: SharedFlow<AppEvent> = _appEventFlow.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val KEY_TOKEN = "auth_token"
    private val token: String
        get() {
           return sharedDataSource.get(KEY_TOKEN, "")
        }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .disableHtmlEscaping()
        .create()

    lateinit var socket: Socket

    fun connect() {
        socket = Socket(
            endpoint = url,
            config = SocketOptions(
                queryParams = mapOf("token" to token),
                transport = SocketOptions.Transport.WEBSOCKET
            ),
            build = {

                on("receive") { payload ->
                    scope.launch {
                        val message = gson.fromJson<Message>(payload, Message::class.java)
                        Log.d("SOCC", "in receive man, payload: ${payload}\n base64: ${message.content}")
                        _chatEventFlow.emit(ChatEvent.Receive(message.chatId, message))
                        _appEventFlow.emit(AppEvent.Receive(message))
                    }
                }

                on("writing_start") { chatId ->
                    scope.launch {
                        Log.d("SOCC", "in writing man")
                        _chatEventFlow.emit(ChatEvent.WritingStart(chatId))
                    }
                }

                on("writing_stop") { chatId ->
                    scope.launch {
                        Log.d("SOCC", "in writing man")
                        _chatEventFlow.emit(ChatEvent.WritingStop(chatId))
                    }
                }

                on("block") { friendId ->
                    scope.launch {
                        Log.d("SOCC", "in block man")
                        _chatEventFlow.emit(ChatEvent.Block(friendId))
                        _appEventFlow.emit(AppEvent.Block(friendId))
                    }
                }

                on("unblock") { friendId ->
                    scope.launch {
                        Log.d("SOCC", "in unblock man")
                        _chatEventFlow.emit(ChatEvent.Unblock(friendId))
                        _appEventFlow.emit(AppEvent.Unblock(friendId))
                    }
                }

                on("ban") {
                    scope.launch {
                        Log.d("SOCC", "in ban man")
                        _appEventFlow.emit(AppEvent.Ban)
                    }
                }

                on(SocketEvent.Error) { args -> Log.e("SOCC", "Socket Error: $args") }
                on(SocketEvent.Reconnect) { args -> Log.e("SOCC", "Reconnect") }
                on(SocketEvent.Connect) { Log.d("SOCC", "Connected") }
                on(SocketEvent.Disconnect) { Log.d("SOCC", "Disconnected") }
            }
        )
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
                    Log.d("SOCC", "out send man; base64: ${event.content}")
                    val payload = gson.toJson(SocketMessage(event.chatId, event.content))
                    socket.emit("send", payload)
                }
                is ChatEvent.WritingStart -> {
                    Log.d("SOCC", "out writing start man")
                    socket.emit("writing_start", event.chatId)
                }
                is ChatEvent.WritingStop -> {
                    Log.d("SOCC", "out writing stop man")
                    socket.emit("writing_stop", event.chatId)
                }
                is ChatEvent.Receive -> {}
                is ChatEvent.Block -> {}
                is ChatEvent.Unblock -> {}
            }
        }
    }

    fun disconnect() {
        scope.launch {
            socket.disconnect()
        }
    }
}
package com.example.i230720_smd_assignment1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatsActivity : AppCompatActivity() {

    private lateinit var rvChats: RecyclerView
    private lateinit var adapter: ChatsAdapter
    private val chatList = mutableListOf<ChatItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chats)

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        rvChats = findViewById(R.id.rvChats)
        rvChats.layoutManager = LinearLayoutManager(this)
        adapter = ChatsAdapter(chatList) { chat ->
            val intent = Intent(this, DmActivity::class.java)
            intent.putExtra("chatId", chat.chatId)
            intent.putExtra("receiverUid", chat.otherUserId)
            intent.putExtra("receiverName", chat.otherUserName)
            startActivity(intent)
        }
        rvChats.adapter = adapter

        loadConversations()
    }

    private fun loadConversations() {
        val tokenStore = TokenStore(this)
        val apiService = ApiClient.create(tokenStore)

        apiService.getConversations().enqueue(object : Callback<ApiResponse<List<Map<String, Any>>>> {
            override fun onResponse(call: Call<ApiResponse<List<Map<String, Any>>>>, response: Response<ApiResponse<List<Map<String, Any>>>>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.ok && body.data != null) {
                    chatList.clear()
                    for (c in body.data) {
                        val peerId = (c["peer_id"] as? Number)?.toInt()?.toString() ?: "1"
                        val peerName = c["peer_name"] as? String ?: "User"
                        val lastMsg = c["last_message"] as? String ?: ""
                        val timeStr = c["last_message_time"] as? String ?: ""
                        val timeMillis = System.currentTimeMillis()

                        val item = ChatItem(
                            chatId = "chat_$peerId",
                            otherUserId = peerId,
                            otherUserName = peerName,
                            lastMessage = lastMsg,
                            lastMessageTime = timeMillis,
                            lastMessageSenderId = peerId,
                            isUnread = false
                        )
                        chatList.add(item)
                    }

                    if (chatList.isEmpty()) {
                        chatList.add(
                            ChatItem(
                                chatId = "chat_1",
                                otherUserId = "1",
                                otherUserName = "Aisha Khan",
                                lastMessage = "Sending the photos tonight",
                                lastMessageTime = System.currentTimeMillis() - 120000,
                                lastMessageSenderId = "1",
                                isUnread = true
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Map<String, Any>>>>, t: Throwable) {
                // Mock data fallback
                chatList.clear()
                chatList.add(
                    ChatItem(
                        chatId = "chat_1",
                        otherUserId = "1",
                        otherUserName = "Aisha Khan (Offline)",
                        lastMessage = "Hey! Are you ready?",
                        lastMessageTime = System.currentTimeMillis(),
                        lastMessageSenderId = "1",
                        isUnread = false
                    )
                )
                adapter.notifyDataSetChanged()
            }
        })
    }
}

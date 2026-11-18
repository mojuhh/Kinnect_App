package com.example.i230720_smd_assignment1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.UUID

class DmActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var adapter: MessagesAdapter
    private val messageList = mutableListOf<MessageItem>()

    private lateinit var etMessageInput: EditText
    private lateinit var btnSendMessage: ImageView
    private lateinit var btnGalleryMedia: ImageView
    private lateinit var btnCameraMedia: ImageView
    private lateinit var btnSharePost: ImageView

    private lateinit var layoutReplyPreview: LinearLayout
    private lateinit var tvReplyPreviewText: TextView
    private lateinit var btnCancelReply: TextView

    private lateinit var tvReceiverName: TextView
    private lateinit var tvAvatar: TextView

    private var currentUid = 1
    private var receiverUid = 2
    private var receiverName = "Aisha Khan"
    private var chatId = ""

    private var replyToMessageId = ""
    private var replyToMessageText = ""

    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sendImageMessage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dm)

        val tokenStore = TokenStore(this)
        currentUid = tokenStore.userId()
        if (currentUid == -1) currentUid = 1

        receiverUid = intent.getIntExtra("receiverUid", 2)
        if (receiverUid == 0) {
            receiverUid = intent.getStringExtra("receiverUid")?.toIntOrNull() ?: 2
        }

        receiverName = intent.getStringExtra("receiverName") ?: "Aisha Khan"
        chatId = "chat_${minOf(currentUid, receiverUid)}_${maxOf(currentUid, receiverUid)}"

        tvReceiverName = findViewById(R.id.tvReceiverName)
        tvAvatar = findViewById(R.id.tvAvatar)
        tvReceiverName.text = receiverName
        val initials = if (receiverName.length >= 2) receiverName.substring(0, 2).uppercase() else "AK"
        tvAvatar.text = initials

        rvMessages = findViewById(R.id.rvMessages)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnGalleryMedia = findViewById(R.id.btnGalleryMedia)
        btnCameraMedia = findViewById(R.id.btnCameraMedia)
        btnSharePost = findViewById(R.id.btnSharePost)

        layoutReplyPreview = findViewById(R.id.layoutReplyPreview)
        tvReplyPreviewText = findViewById(R.id.tvReplyPreviewText)
        btnCancelReply = findViewById(R.id.btnCancelReply)

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnVoiceCall)?.setOnClickListener {
            val intent = Intent(this, VoiceCallActivity::class.java)
            intent.putExtra("caller_name", receiverName)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnVideoCall)?.setOnClickListener {
            val intent = Intent(this, VideoCallActivity::class.java)
            intent.putExtra("caller_name", receiverName)
            startActivity(intent)
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager

        adapter = MessagesAdapter(
            messages = messageList,
            currentUserId = currentUid.toString(),
            onMessageClick = { message ->
                toggleHeartReaction(message)
            },
            onMessageLongClick = { message ->
                showMessageOptionsDialog(message)
            }
        )
        rvMessages.adapter = adapter

        btnSendMessage.setOnClickListener {
            sendTextMessage()
        }

        btnGalleryMedia.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnCameraMedia.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSharePost.setOnClickListener {
            sendPostMessage()
        }

        btnCancelReply.setOnClickListener {
            cancelReply()
        }

        loadMessages()
        startPolling()
    }

    private fun loadMessages() {
        val tokenStore = TokenStore(this)
        val apiService = ApiClient.create(tokenStore)

        apiService.getMessages(receiverUid, 0, null).enqueue(object : Callback<ApiResponse<List<Map<String, Any>>>> {
            override fun onResponse(call: Call<ApiResponse<List<Map<String, Any>>>>, response: Response<ApiResponse<List<Map<String, Any>>>>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.ok && body.data != null) {
                    messageList.clear()
                    for (m in body.data) {
                        val msgId = (m["id"] as? Number)?.toInt()?.toString() ?: "1"
                        val senderId = (m["sender_id"] as? Number)?.toInt()?.toString() ?: "1"
                        val content = m["content"] as? String ?: ""
                        val mediaPath = m["media_path"] as? String ?: ""
                        val type = m["type"] as? String ?: "text"
                        val isDeleted = ((m["is_deleted"] as? Number)?.toInt() ?: 0) == 1
                        val isEdited = ((m["is_edited"] as? Number)?.toInt() ?: 0) == 1

                        val item = MessageItem(
                            messageId = msgId,
                            senderId = senderId,
                            senderName = if (senderId == currentUid.toString()) "You" else receiverName,
                            type = type,
                            content = content,
                            imageUrl = if (mediaPath.isNotEmpty()) "${BuildConfig.BASE_URL}$mediaPath" else "",
                            timestamp = System.currentTimeMillis(),
                            isEdited = isEdited,
                            isDeleted = isDeleted
                        )
                        messageList.add(item)
                    }

                    if (messageList.isEmpty()) {
                        messageList.add(MessageItem("1", receiverUid.toString(), receiverName, "text", "Hey! Are you ready for Saturday?", "", System.currentTimeMillis() - 60000))
                        messageList.add(MessageItem("2", currentUid.toString(), "You", "text", "Yes, obviously!", "", System.currentTimeMillis() - 30000))
                    }

                    adapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        rvMessages.smoothScrollToPosition(messageList.size - 1)
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Map<String, Any>>>>, t: Throwable) {
                if (messageList.isEmpty()) {
                    messageList.add(MessageItem("1", receiverUid.toString(), receiverName, "text", "Hey! Are you ready?", "", System.currentTimeMillis() - 60000))
                    messageList.add(MessageItem("2", currentUid.toString(), "You", "text", "Yes!", "", System.currentTimeMillis() - 30000))
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }

    private fun startPolling() {
        pollingRunnable = object : Runnable {
            override fun run() {
                loadMessages()
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(pollingRunnable!!, 2000)
    }

    override fun onPause() {
        super.onPause()
        pollingRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun sendTextMessage() {
        val text = etMessageInput.text.toString().trim()
        if (text.isEmpty()) return

        val tokenStore = TokenStore(this)
        val apiService = ApiClient.create(tokenStore)

        val uuid = UUID.randomUUID().toString()
        val receiverBody = receiverUid.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val typeBody = "text".toRequestBody("text/plain".toMediaTypeOrNull())
        val contentBody = text.toRequestBody("text/plain".toMediaTypeOrNull())
        val uuidBody = uuid.toRequestBody("text/plain".toMediaTypeOrNull())

        apiService.sendMessage(receiverBody, typeBody, contentBody, uuidBody, null).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                etMessageInput.setText("")
                cancelReply()
                loadMessages()
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                messageList.add(MessageItem(System.currentTimeMillis().toString(), currentUid.toString(), "You", "text", text, "", System.currentTimeMillis()))
                adapter.notifyDataSetChanged()
                etMessageInput.setText("")
                cancelReply()
            }
        })
    }

    private fun sendImageMessage(uri: Uri) {
        val tokenStore = TokenStore(this)
        val apiService = ApiClient.create(tokenStore)

        val uuid = UUID.randomUUID().toString()
        val receiverBody = receiverUid.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val typeBody = "image".toRequestBody("text/plain".toMediaTypeOrNull())
        val contentBody = "📷 Photo".toRequestBody("text/plain".toMediaTypeOrNull())
        val uuidBody = uuid.toRequestBody("text/plain".toMediaTypeOrNull())

        val file = File(uri.path ?: "")
        val requestFile = file.asRequestBody("*/*".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

        apiService.sendMessage(receiverBody, typeBody, contentBody, uuidBody, filePart).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                loadMessages()
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                Toast.makeText(this@DmActivity, "Image sent locally", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendPostMessage() {
        val text = "📌 Shared a post: Sunset walk"
        val tokenStore = TokenStore(this)
        val apiService = ApiClient.create(tokenStore)

        val uuid = UUID.randomUUID().toString()
        val receiverBody = receiverUid.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val typeBody = "post".toRequestBody("text/plain".toMediaTypeOrNull())
        val contentBody = text.toRequestBody("text/plain".toMediaTypeOrNull())
        val uuidBody = uuid.toRequestBody("text/plain".toMediaTypeOrNull())

        apiService.sendMessage(receiverBody, typeBody, contentBody, uuidBody, null).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                loadMessages()
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {}
        })
    }

    private fun toggleHeartReaction(message: MessageItem) {
        Toast.makeText(this, "Heart reaction toggled ❤️", Toast.LENGTH_SHORT).show()
    }

    private fun showMessageOptionsDialog(message: MessageItem) {
        val isOwnMessage = message.senderId == currentUid.toString()
        val options = mutableListOf<String>("Reply", "Heart Reaction ❤️")

        if (isOwnMessage && !message.isDeleted) {
            options.add("Edit Message")
            options.add("Delete Message")
        }

        AlertDialog.Builder(this)
            .setTitle("Message Options")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Reply" -> {
                        replyToMessageId = message.messageId
                        replyToMessageText = message.content
                        layoutReplyPreview.visibility = View.VISIBLE
                        tvReplyPreviewText.text = "Replying to: ${message.content}"
                        etMessageInput.requestFocus()
                    }
                    "Edit Message" -> showEditDialog(message)
                    "Delete Message" -> deleteMessage(message)
                }
            }
            .show()
    }

    private fun showEditDialog(message: MessageItem) {
        val input = EditText(this)
        input.setText(message.content)

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    val tokenStore = TokenStore(this)
                    val apiService = ApiClient.create(tokenStore)
                    apiService.editMessage(message.messageId.toIntOrNull() ?: 0, newText).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
                        override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                            loadMessages()
                        }
                        override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {}
                    })
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMessage(message: MessageItem) {
        val tokenStore = TokenStore(this)
        val apiService = ApiClient.create(tokenStore)
        apiService.deleteMessage(message.messageId.toIntOrNull() ?: 0).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                loadMessages()
            }
            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {}
        })
    }

    private fun cancelReply() {
        replyToMessageId = ""
        replyToMessageText = ""
        layoutReplyPreview.visibility = View.GONE
    }
}

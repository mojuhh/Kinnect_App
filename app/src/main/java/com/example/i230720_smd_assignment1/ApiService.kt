package com.example.i230720_smd_assignment1

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("auth/signup.php")
    fun signup(
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("phone") phone: String,
        @Field("password") pass: String
    ): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("auth/login.php")
    fun login(
        @Field("identifier") identifier: String,
        @Field("password") pass: String
    ): Call<ApiResponse<Map<String, Any>>>

    @POST("auth/logout.php")
    fun logout(): Call<ApiResponse<Map<String, Any>>>

    @GET("users/check_username.php")
    fun checkUsername(@Query("u") username: String): Call<ApiResponse<Map<String, Boolean>>>

    @FormUrlEncoded
    @POST("users/setup_profile.php")
    fun setupProfile(
        @Field("username") username: String,
        @Field("bio") bio: String
    ): Call<ApiResponse<Map<String, Any>>>

    @GET("users/get_profile.php")
    fun getProfile(@Query("id") id: Int?): Call<ApiResponse<Map<String, Any>>>

    @Multipart
    @POST("users/update_profile.php")
    fun updateProfile(
        @Part("name") name: RequestBody?,
        @Part("username") username: RequestBody?,
        @Part("bio") bio: RequestBody?,
        @Part profilePic: MultipartBody.Part?,
        @Part coverPic: MultipartBody.Part?
    ): Call<ApiResponse<Map<String, Any>>>

    @GET("users/search.php")
    fun searchUsers(
        @Query("q") query: String,
        @Query("filter") filter: String
    ): Call<ApiResponse<List<Map<String, Any>>>>

    @POST("users/heartbeat.php")
    fun heartbeat(): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("users/set_status.php")
    fun setStatus(@Field("online") online: Int): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("users/save_token.php")
    fun saveToken(@Field("fcm_token") token: String): Call<ApiResponse<Map<String, Any>>>

    // Friends
    @GET("friends/list.php")
    fun getFriends(): Call<ApiResponse<List<Map<String, Any>>>>

    @GET("friends/requests.php")
    fun getRequests(): Call<ApiResponse<List<Map<String, Any>>>>

    @FormUrlEncoded
    @POST("friends/send_request.php")
    fun sendRequest(@Field("user_id") userId: Int, @Field("client_uuid") uuid: String): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("friends/accept_request.php")
    fun acceptRequest(@Field("user_id") userId: Int): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("friends/delete_request.php")
    fun deleteRequest(@Field("user_id") userId: Int): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("friends/remove_friend.php")
    fun removeFriend(@Field("user_id") userId: Int): Call<ApiResponse<Map<String, Any>>>

    // Posts & Feed
    @GET("posts/get_feed.php")
    fun getFeed(@Query("before_id") beforeId: Int?, @Query("limit") limit: Int): Call<ApiResponse<List<Map<String, Any>>>>

    @Multipart
    @POST("posts/create_post.php")
    fun createPost(
        @Part("text") text: RequestBody?,
        @Part("client_uuid") uuid: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("posts/set_reaction.php")
    fun setReaction(
        @Field("post_id") postId: Int,
        @Field("type") type: String?
    ): Call<ApiResponse<Map<String, Any>>>

    @GET("comments/get_comments.php")
    fun getComments(@Query("post_id") postId: Int): Call<ApiResponse<List<Map<String, Any>>>>

    @Multipart
    @POST("comments/add_comment.php")
    fun addComment(
        @Part("post_id") postId: RequestBody,
        @Part("parent_id") parentId: RequestBody?,
        @Part("text") text: RequestBody?,
        @Part("client_uuid") uuid: RequestBody,
        @Part image: MultipartBody.Part?
    ): Call<ApiResponse<Map<String, Any>>>

    // Stories
    @GET("stories/get_stories.php")
    fun getStories(): Call<ApiResponse<List<Map<String, Any>>>>

    @Multipart
    @POST("stories/upload_story.php")
    fun uploadStory(
        @Part("label") label: RequestBody?,
        @Part("audience") audience: RequestBody,
        @Part("client_uuid") uuid: RequestBody,
        @Part media: MultipartBody.Part
    ): Call<ApiResponse<Map<String, Any>>>

    // Chat
    @GET("chat/get_conversations.php")
    fun getConversations(): Call<ApiResponse<List<Map<String, Any>>>>

    @GET("chat/get_messages.php")
    fun getMessages(
        @Query("peer_id") peerId: Int,
        @Query("after_id") afterId: Int,
        @Query("since") since: String?
    ): Call<ApiResponse<List<Map<String, Any>>>>

    @Multipart
    @POST("chat/send_message.php")
    fun sendMessage(
        @Part("receiver_id") receiverId: RequestBody,
        @Part("type") type: RequestBody,
        @Part("content") content: RequestBody?,
        @Part("client_uuid") uuid: RequestBody,
        @Part file: MultipartBody.Part?
    ): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("chat/edit_message.php")
    fun editMessage(
        @Field("message_id") messageId: Int,
        @Field("content") content: String
    ): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("chat/delete_message.php")
    fun deleteMessage(@Field("message_id") messageId: Int): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("chat/screenshot_event.php")
    fun screenshotEvent(
        @Field("peer_id") peerId: Int,
        @Field("client_uuid") uuid: String
    ): Call<ApiResponse<Map<String, Any>>>

    // Calls
    @FormUrlEncoded
    @POST("calls/invite.php")
    fun inviteCall(
        @Field("receiver_id") receiverId: Int,
        @Field("call_type") callType: String,
        @Field("channel_name") channelName: String
    ): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("calls/log_call.php")
    fun logCall(
        @Field("receiver_id") receiverId: Int,
        @Field("type") type: String,
        @Field("status") status: String,
        @Field("started_at") startedAt: String,
        @Field("duration") duration: Int,
        @Field("client_uuid") uuid: String
    ): Call<ApiResponse<Map<String, Any>>>

    // Notifications
    @GET("notifications/list.php")
    fun getNotifications(): Call<ApiResponse<List<Map<String, Any>>>>
}

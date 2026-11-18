# Kinnect — Backend Setup & Android Integration Guide

## Overview

Your backend is a **PHP + MySQL REST API** served by **XAMPP** on `localhost`. Your Android app will talk to it over HTTP, caching data in a local **SQLite** database. This guide walks through every step.

---

## Step 1 — Start XAMPP Services

Open **XAMPP Control Panel** (`C:\xampp\xampp-control.exe`) and click **Start** for:
- **Apache** (port 80)
- **MySQL** (port 3306)

Verify Apache is running by opening `http://localhost` in your browser.

---

## Step 2 — Import the Database Schema

> [!IMPORTANT]
> Do this only once. If the `kinnect` database already exists in phpMyAdmin, skip to Step 3.

**Option A — phpMyAdmin (easiest)**
1. Open `http://localhost/phpmyadmin`
2. Click **Import** in the top nav
3. Choose [schema.sql](file:///c:/xampp/htdocs/kinnect/schema.sql) → click **Go**

**Option B — Command Line**
```bat
C:\xampp\mysql\bin\mysql.exe -u root -e "source C:/xampp/htdocs/kinnect/schema.sql"
```

After import you should see these tables in the `kinnect` database:
`users`, `sessions`, `friendships`, `posts`, `post_images`, `reactions`, `comments`, `comment_likes`, `stories`, `close_friends`, `messages`, `calls`, `notifications`

**Also run the phone-auth migration** (adds the `phone_number` column if missing):
```bat
C:\xampp\mysql\bin\mysql.exe -u root kinnect -e "source C:/xampp/htdocs/kinnect/migrate_phone_auth.sql"
```

---

## Step 3 — Verify the Config

Open [config.php](file:///c:/xampp/htdocs/kinnect/config.php). The defaults are:

```php
define('DB_HOST', 'localhost');
define('DB_NAME', 'kinnect');
define('DB_USER', 'root');
define('DB_PASS', '');          // blank password — XAMPP default
define('UPLOAD_DIR', __DIR__ . '/uploads/');
define('FCM_PROJECT_ID', 'kinnect-app');
define('FCM_SA_PATH',    __DIR__ . '/service-account.json');
```

If your MySQL has a password, set `DB_PASS` here.

**Quick smoke-test** — open this URL in your browser. You should get a JSON error (not a PHP error or 404):
```
http://localhost/kinnect/auth/login.php
```
Expected: `{"ok":false,"error":"Email or phone number and password are required"}`

---

## Step 4 — Find Your PC's Local IP (for the Android emulator / device)

The Android emulator uses `10.0.2.2` to reach your host machine's `localhost`.  
A **real Android device** needs your PC's LAN IP:

```powershell
ipconfig | findstr "IPv4"
```

Write down the address, e.g. `192.168.1.5`.

---

## Step 5 — Android Project — Add Dependencies

In your app-level `build.gradle` (or `build.gradle.kts`), add:

```groovy
dependencies {
    // HTTP client
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // Secure token storage
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // Image loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'

    // Room (SQLite ORM)
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
}
```

Sync Gradle after adding.

---

## Step 6 — Allow Cleartext HTTP (Android 9+)

By default Android blocks plain HTTP. Create `app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Emulator reaches your PC as 10.0.2.2 -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <!-- Add your real LAN IP if testing on a device -->
        <domain includeSubdomains="false">192.168.1.5</domain>
    </network-security-config>
</network-security-config>
```

Reference it in `AndroidManifest.xml` inside `<application>`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

---

## Step 7 — API Constants

Create `app/src/main/java/.../network/ApiConstants.kt`:

```kotlin
object ApiConstants {
    // Emulator
    const val BASE_URL = "http://10.0.2.2/kinnect/"

    // Real device (replace with your LAN IP from Step 4)
    // const val BASE_URL = "http://192.168.1.5/kinnect/"
}
```

---

## Step 8 — Retrofit Setup

```kotlin
// RetrofitClient.kt
object RetrofitClient {
    private val okHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // Inject token automatically
            val token = SessionManager.getToken()
            val req = if (token != null)
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            else chain.request()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val instance: KinnectApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KinnectApi::class.java)
    }
}
```

---

## Step 9 — API Interface (all endpoints)

```kotlin
// KinnectApi.kt
interface KinnectApi {

    // ── Auth ──────────────────────────────────────────────
    @POST("auth/signup.php")
    suspend fun signup(@Body body: SignupRequest): ApiResponse<AuthData>

    @POST("auth/login.php")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthData>

    @POST("auth/logout.php")
    suspend fun logout(): ApiResponse<Unit>

    // ── Users ─────────────────────────────────────────────
    @GET("users/get_profile.php")
    suspend fun getProfile(@Query("user_id") userId: Int): ApiResponse<UserProfile>

    @Multipart
    @POST("users/setup_profile.php")
    suspend fun setupProfile(
        @Part("username") username: RequestBody,
        @Part("bio") bio: RequestBody,
        @Part profilePic: MultipartBody.Part?
    ): ApiResponse<UserProfile>

    @Multipart
    @POST("users/update_profile.php")
    suspend fun updateProfile(
        @Part("name") name: RequestBody,
        @Part("username") username: RequestBody,
        @Part("bio") bio: RequestBody,
        @Part profilePic: MultipartBody.Part?,
        @Part coverPic: MultipartBody.Part?
    ): ApiResponse<UserProfile>

    @GET("users/search.php")
    suspend fun searchUsers(@Query("q") query: String): ApiResponse<List<UserProfile>>

    @POST("users/heartbeat.php")
    suspend fun heartbeat(): ApiResponse<Unit>

    @POST("users/save_token.php")
    suspend fun saveFcmToken(@Body body: Map<String, String>): ApiResponse<Unit>

    @POST("users/set_status.php")
    suspend fun setStatus(@Body body: Map<String, Any>): ApiResponse<Unit>

    @GET("users/check_username.php")
    suspend fun checkUsername(@Query("username") username: String): ApiResponse<Map<String, Boolean>>

    // ── Posts ─────────────────────────────────────────────
    @Multipart
    @POST("posts/create_post.php")
    suspend fun createPost(
        @Part("client_uuid") uuid: RequestBody,
        @Part("text") text: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): ApiResponse<Post>

    @GET("posts/get_feed.php")
    suspend fun getFeed(@Query("before_id") beforeId: Int? = null): ApiResponse<List<Post>>

    @GET("posts/get_posts.php")
    suspend fun getUserPosts(@Query("user_id") userId: Int): ApiResponse<List<Post>>

    @POST("posts/set_reaction.php")
    suspend fun setReaction(@Body body: ReactionRequest): ApiResponse<Unit>

    @GET("posts/get_reactions.php")
    suspend fun getReactions(@Query("post_id") postId: Int): ApiResponse<List<Reaction>>

    @POST("posts/delete_post.php")
    suspend fun deletePost(@Body body: Map<String, Int>): ApiResponse<Unit>

    // ── Comments ──────────────────────────────────────────
    @POST("comments/add_comment.php")
    suspend fun addComment(@Body body: CommentRequest): ApiResponse<Comment>

    @GET("comments/get_comments.php")
    suspend fun getComments(@Query("post_id") postId: Int): ApiResponse<List<Comment>>

    @POST("comments/like_comment.php")
    suspend fun likeComment(@Body body: Map<String, Int>): ApiResponse<Unit>

    // ── Stories ───────────────────────────────────────────
    @Multipart
    @POST("stories/create_story.php")
    suspend fun createStory(
        @Part("client_uuid") uuid: RequestBody,
        @Part("label") label: RequestBody,
        @Part("audience") audience: RequestBody,
        @Part media: MultipartBody.Part
    ): ApiResponse<Story>

    @GET("stories/get_stories.php")
    suspend fun getStories(): ApiResponse<List<Story>>

    @POST("stories/close_friends.php")
    suspend fun updateCloseFriends(@Body body: Map<String, List<Int>>): ApiResponse<Unit>

    // ── Friends ───────────────────────────────────────────
    @POST("friends/send_request.php")
    suspend fun sendFriendRequest(@Body body: Map<String, Any>): ApiResponse<Unit>

    @POST("friends/accept_request.php")
    suspend fun acceptFriendRequest(@Body body: Map<String, Int>): ApiResponse<Unit>

    @POST("friends/delete_request.php")
    suspend fun deleteFriendRequest(@Body body: Map<String, Int>): ApiResponse<Unit>

    @POST("friends/remove_friend.php")
    suspend fun removeFriend(@Body body: Map<String, Int>): ApiResponse<Unit>

    @GET("friends/get_friends.php")
    suspend fun getFriends(@Query("user_id") userId: Int? = null): ApiResponse<List<UserProfile>>

    @GET("friends/requests.php")
    suspend fun getFriendRequests(): ApiResponse<List<UserProfile>>

    // ── Chat ──────────────────────────────────────────────
    @POST("chat/send_message.php")
    suspend fun sendMessage(@Body body: SendMessageRequest): ApiResponse<Message>

    @GET("chat/get_messages.php")
    suspend fun getMessages(
        @Query("peer_id") peerId: Int,
        @Query("before_id") beforeId: Int? = null
    ): ApiResponse<List<Message>>

    @GET("chat/get_conversations.php")
    suspend fun getConversations(): ApiResponse<List<Conversation>>

    @POST("chat/edit_message.php")
    suspend fun editMessage(@Body body: Map<String, Any>): ApiResponse<Message>

    @POST("chat/delete_message.php")
    suspend fun deleteMessage(@Body body: Map<String, Int>): ApiResponse<Unit>

    @POST("chat/mark_read.php")
    suspend fun markRead(@Body body: Map<String, Int>): ApiResponse<Unit>

    @POST("chat/screenshot_event.php")
    suspend fun screenshotEvent(@Body body: Map<String, Int>): ApiResponse<Unit>

    // ── Calls ─────────────────────────────────────────────
    @POST("calls/initiate_call.php")
    suspend fun initiateCall(@Body body: CallRequest): ApiResponse<Map<String, String>>

    @POST("calls/log_call.php")
    suspend fun logCall(@Body body: CallLogRequest): ApiResponse<Unit>

    @GET("calls/get_logs.php")
    suspend fun getCallLogs(): ApiResponse<List<CallLog>>

    // ── Notifications ─────────────────────────────────────
    @GET("notifications/get_notifications.php")
    suspend fun getNotifications(): ApiResponse<List<Notification>>
}
```

---

## Step 10 — Secure Token Storage (SessionManager)

```kotlin
// SessionManager.kt
object SessionManager {
    private const val PREFS = "kinnect_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(token: String, userId: Int) {
        prefs.edit().putString(KEY_TOKEN, token).putInt(KEY_USER_ID, userId).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() = prefs.edit().clear().apply()
}
```

Call `SessionManager.init(this)` in your `Application.onCreate()`.

---

## Step 11 — SQLite (Room) for Offline Support

```kotlin
// AppDatabase.kt
@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        MessageEntity::class,
        StoryEntity::class,
        FriendEntity::class,
        NotificationEntity::class,
        PendingActionEntity::class   // offline queue
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun messageDao(): MessageDao
    abstract fun pendingActionDao(): PendingActionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(ctx: Context) = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "kinnect.db")
                .build().also { INSTANCE = it }
        }
    }
}
```

**Offline queue table:**
```kotlin
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val client_uuid: String,   // UUID prevents duplicates on retry
    val action_type: String,               // "send_message", "create_post", etc.
    val payload_json: String,              // serialized request body
    val created_at: Long = System.currentTimeMillis()
)
```

---

## Step 12 — Splash Screen Logic

```kotlin
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            if (SessionManager.isLoggedIn()) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 5000) // 5 seconds as required by assignment
    }
}
```

---

## API Quick Reference

| Feature | Method | Endpoint |
|---------|--------|----------|
| Sign up | POST | `/auth/signup.php` |
| Log in | POST | `/auth/login.php` |
| Log out | POST | `/auth/logout.php` |
| Get profile | GET | `/users/get_profile.php?user_id=X` |
| Setup profile | POST (multipart) | `/users/setup_profile.php` |
| Update profile | POST (multipart) | `/users/update_profile.php` |
| Search users | GET | `/users/search.php?q=...` |
| Heartbeat | POST | `/users/heartbeat.php` |
| Get feed | GET | `/posts/get_feed.php` |
| Create post | POST (multipart) | `/posts/create_post.php` |
| Set reaction | POST | `/posts/set_reaction.php` |
| Add comment | POST | `/comments/add_comment.php` |
| Get comments | GET | `/comments/get_comments.php?post_id=X` |
| Get stories | GET | `/stories/get_stories.php` |
| Create story | POST (multipart) | `/stories/create_story.php` |
| Send friend request | POST | `/friends/send_request.php` |
| Accept request | POST | `/friends/accept_request.php` |
| Get friends | GET | `/friends/get_friends.php` |
| Send message | POST | `/chat/send_message.php` |
| Get messages | GET | `/chat/get_messages.php?peer_id=X` |
| Get conversations | GET | `/chat/get_conversations.php` |
| Edit message | POST | `/chat/edit_message.php` |
| Delete message | POST | `/chat/delete_message.php` |
| Screenshot event | POST | `/chat/screenshot_event.php` |
| Get notifications | GET | `/notifications/get_notifications.php` |
| Initiate call | POST | `/calls/initiate_call.php` |
| Log call | POST | `/calls/log_call.php` |

### Standard Response Envelope
Every endpoint returns:
```json
{ "ok": true,  "data": { ... } }   // success
{ "ok": false, "error": "reason" } // failure
```

### Auth Header (all protected endpoints)
```
Authorization: Bearer <64-char-hex-token>
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Connection refused` on emulator | Use `10.0.2.2` not `localhost` |
| `Connection refused` on real device | Use your PC's LAN IP; both must be on same Wi-Fi |
| `Cleartext HTTP not permitted` | Add `network_security_config.xml` (Step 6) |
| `Database connection failed` | Make sure MySQL is started in XAMPP Control Panel |
| Apache 403 Forbidden | Check `httpd.conf` — `AllowOverride All` needed for `/htdocs` |
| Token missing / 401 | Verify `Authorization: Bearer ...` header is sent |

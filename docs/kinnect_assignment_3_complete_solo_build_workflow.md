# Kinnect - Assignment 3: Complete Solo Build Workflow

From a hardcoded UI to a working app: PHP + MySQL web services, SQLite offline cache, background sync, FCM push notifications and Agora calls.

| Field | Details |
| :--- | :--- |
| **Course** | CS-4039 Software for Mobile Devices |
| **Stack** | Android (Java or Kotlin) + Retrofit + Room/SQLite + WorkManager \| PHP 8 + MySQL on XAMPP \| FCM (push only) \| Agora (calls) |
| **Team size** | 1 (solo). Confirm the rules with your instructor in writing. |
| **Starting point** | All 23 screens built as static UI |

> **Important correction:** this assignment does not use a realtime database. Firebase Auth and the Firebase Realtime Database are removed. 'Real-time' behaviour is done with short-interval polling while a screen is open, plus FCM pushes. No WebSockets are needed.

---

## How to use this document

Work through the parts in order. Every feature sprint follows the same loop, which keeps the work predictable when you are alone:

1. **Backend first:** write the PHP endpoint and test it in Postman (or the browser) before touching Android.
2. **Android networking:** add the Retrofit method and the model classes.
3. **Local cache:** add the Room entity/DAO so the screen reads from SQLite.
4. **Repository:** glue the two together (read local, refresh remote, write with a pending state).
5. **Wire the existing UI** to the repository (replace hardcoded data).
6. **Offline pass:** add the action to `pending_actions` and confirm it syncs.
7. **Test and commit** with a clear message.

### Contents

| Part | Topic |
| :--- | :--- |
| **1** | Reality check: marks, solo risks, what to ask your instructor |
| **2** | Architecture: how all the pieces talk to each other |
| **3** | Environment setup (Phase 0) |
| **4** | Backend foundation: schema, common.php, uploads, idempotency |
| **5** | Android foundation: dependencies, networking, token, Room, repository pattern |
| **6** | Feature sprints 1 to 12 (auth through Agora calls) |
| **7** | Offline sync engine in depth |
| **8** | Espresso tests, GitHub hygiene, submission |
| **9** | Demo script and rehearsal checklist |
| **10** | Pitfalls and fixes, endpoint reference |

---

## Part 1: Reality check

### 1.1 Where the marks are

| Item | Marks | Notes |
| :--- | :--- | :--- |
| **Live demo** | 20 | Biggest single item. Two devices, backend running, offline sync shown live. |
| **Offline support (SQLite + sync)** | 15 | Hardest engineering item. Build the engine early. |
| **GitHub version control** | 10 | One commit per feature, regular history, before the deadline. |
| **Stories** | 10 | 24 hour expiry on server and in local cache. |
| **Posts, reactions, comments** | 10 | Multi-image grid with +N overlay. |
| **Messaging** | 10 | Text, image, video, file. Edit/delete within 5 minutes. |
| **Splash, Auth, Calls, Friends/Profiles, FCM, Search, Online status, Security, Espresso** | 5 each | Eight items worth $5 \text{ each} = 40$ marks total. |

Total is 120+ marks of work in your rubric, so skipping any 5-mark item costs real points. The plan below is ordered so that if time runs short, the things you cut are the cheapest ones.

### 1.2 Questions to send your instructor today (keep the reply)

* May I submit alone? If yes, is the workload the same?
* The rule says commits from both members and both present at demo. Is that waived for a solo submission?
* Which roll-number format should I use for the folder, project and package name? (Suggested: `i220001_Assignment 03`, project `i220001`, package `com.groupname.i220001`.)
* Is a local XAMPP backend acceptable for the demo, or must it be hosted publicly?
* Are PHP endpoints and Android code in the same repo and ZIP, or is the backend submitted separately?

### 1.3 Solo survival rules

* **Build generic, not specific.** One `common.php`, one `pending_actions` table, one sync worker with a type switch. Every feature plugs into these.
* **Never leave offline for last.** Build the engine in Sprint 5 when posts exist, then every later feature is a small addition.
* **Start the slow accounts on day 1:** Firebase project (FCM service account) and Agora account (App ID). Waiting on these late is a classic trap.
* **Commit daily.** Even a 20-minute feature gets its own commit. The history is graded.
* **Test with two devices from Sprint 3 onward.** Do not discover multi-user bugs on demo day.
* **Protect secrets.** Never commit `google-services.json`, the FCM service-account JSON, the Agora certificate or `config.php`.

### 1.4 Suggested time plan

Scale the day counts to your real deadline. If you have fewer days, shrink the polish, not the order.

| Sprint | Content | Days |
| :--- | :--- | :--- |
| **0-1** | Setup, backend foundation, Android foundation | 3 |
| **1** | Splash + Auth + Edit profile (first-time flow) | 3 |
| **2** | Friends, Search, filters, Other profile | 3 |
| **3** | Posts, feed, multi-image grid | 3 |
| **4** | Reactions + comments | 2 |
| **5** | Offline engine (`pending_actions` + WorkManager) | 3 |
| **6** | Stories + 24 hour expiry | 2 |
| **7** | Messaging + edit/delete + polling | 4 |
| **8** | Online/offline presence | 1 |
| **9** | FCM push notifications | 2 |
| **10** | Screenshot detection | 1 |
| **11** | Agora calls + call logs | 3 |
| **12** | Espresso tests, polish, demo rehearsal, submission | 3 |
| | **Total** | **33 working days** |

---

## Part 2: Architecture

### 2.1 The big picture

```text
Android app
  UI (Activities/Fragments)
    │ observes LiveData/Flow
    ▼
  Repository ◄── reads/writes ──► Room (SQLite)  <-- source of truth for UI
    │                                  │
    │ Retrofit + OkHttp                ├──► pending_actions queue
    │ HTTP/JSON (token header)         │
    ▼                                  ▼
PC (XAMPP)                        WorkManager SyncWorker (runs when network returns)
  PHP endpoints ──► MySQL
        │
        ├──► /uploads (media files)
        └──► FCM HTTP v1 (push)

Agora SDK: phone to Agora cloud, directly (your server only logs calls)
```

### 2.2 Core design rules

* **UI never reads the network directly.** UI observes SQLite. The repository refreshes SQLite from the server. This single rule gives you instant loading and offline viewing.
* **Every write has a client UUID.** Generated on the phone. The server stores it in a `UNIQUE` column and ignores repeats. This is what guarantees no duplicates.
* **Every write is local-first.** Save locally with `status = pending`, show it immediately, queue an action, try to send.
* **Media is stored on the server filesystem.** The database stores a relative path like `uploads/posts/abc.jpg`. The app builds the full URL from `BASE_URL`.
* **Real time = polling + push.** Poll every 2 seconds while Chat is open, every 15 to 20 seconds for lists, and use FCM to alert when the app is closed.

### 2.3 Standard JSON response shape

Use one shape everywhere so the Android side can share a single response class:

* **Success:** `{ "ok": true, "data": { ... } }`
* **Failure:** `{ "ok": false, "error": "human readable message" }`
* **HTTP codes:** `200` ok, `400` bad input, `401` bad/missing token, `403` not allowed (e.g. edit after 5 min), `404` not found

---

## Part 3: Environment setup (Phase 0)

### 3.1 Tools to install

* Android Studio (latest stable) and a real phone with USB debugging, plus one emulator.
* XAMPP (Apache + PHP 8 + MySQL/MariaDB). Start Apache and MySQL from the control panel.
* Postman (or Bruno/Insomnia) for testing endpoints.
* Git and a GitHub account.
* VS Code or PhpStorm for PHP.

### 3.2 Repository structure

Create one new private GitHub repo and use this layout:

```text
kinnect/
  android/                   <- Android Studio project
  backend/                   <- copy of htdocs/kinnect (PHP)
    auth/ users/ friends/ posts/ stories/ chat/ calls/ notifications/
    uploads/                 <- gitignored (runtime files)
    common.php
    config.sample.php        <- committed
    config.php               <- NOT committed
    schema.sql
  docs/
  .gitignore
```

**`.gitignore` essentials:**
```gitignore
android/app/google-services.json
backend/config.php
backend/uploads/
backend/*service-account*.json
*.jks
local.properties
.gradle/
build/
.idea/
```

### 3.3 Run PHP from your repo

Easiest approach: make `C:\xampp\htdocs\kinnect` a symbolic link (or just a junction) to your repo's backend folder so you edit once and Apache serves it.

```cmd
mklink /J C:\xampp\htdocs\kinnect C:\path\to\repo\backend
```

### 3.4 How phones reach your PC

| Device | Base URL | Notes |
| :--- | :--- | :--- |
| **Emulator** | `http://10.0.2.2/kinnect/` | `10.0.2.2` is the emulator alias for the host PC. |
| **Real phone** | `http://192.168.x.x/kinnect/` | Use your PC's LAN IP (run `ipconfig`). Phone and PC on the same Wi-Fi. |

* Allow Apache through Windows Firewall (private network) or phones will time out.
* Disable client isolation on your router/hotspot. If the demo venue Wi-Fi blocks device-to-device traffic, use your phone's hotspot or a travel router and test it beforehand.
* Put `BASE_URL` in one constant (`BuildConfig` field). The LAN IP changes, you will edit it often.
* Add `android:usesCleartextTraffic="true"` to the manifest application tag, or define a `network_security_config`, because you are using plain HTTP.
* In `php.ini` set `upload_max_filesize = 64M`, `post_max_size = 70M`, `max_execution_time = 120`, then restart Apache (needed for videos and files).

### 3.5 Accounts to create now

* **Firebase project:** add your Android app (using your final package name), download `google-services.json` into `android/app`. Generate a service account key (Project settings, Service accounts) for the PHP server. Save it outside Git.
* **Agora account:** create a project, copy the App ID. For development choose 'testing mode' (App ID only) to avoid server-side tokens. If you pick certificate mode you must generate tokens in PHP.

### 3.6 Project naming

* **Project name:** your roll number as instructed. **Package:** `com.groupname.rollnumber`.
* Set the package name before registering with Firebase, otherwise `google-services.json` will not match.

### Phase 0 Checklist
- [ ] Private repo created with `android/` and `backend/`
- [ ] Apache + MySQL running; `http://localhost/kinnect/test.php` prints OK
- [ ] Postman can hit the PC from your phone's browser (open the LAN URL on the phone)
- [ ] Firebase + Agora accounts ready
- [ ] First commit pushed: `'chore: initial project structure and gitignore'`

---

## Part 4: Backend foundation

### 4.1 Database schema

Create a database `kinnect` with `utf8mb4`. Save everything as `backend/schema.sql` so you can rebuild in one click.

```sql
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  username VARCHAR(40) UNIQUE NULL,           -- null until profile setup
  email VARCHAR(120) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  bio VARCHAR(255) DEFAULT '',
  profile_pic VARCHAR(255) NULL,
  cover_pic VARCHAR(255) NULL,
  profile_complete TINYINT DEFAULT 0,
  is_online TINYINT DEFAULT 0,
  last_seen DATETIME NULL,
  fcm_token VARCHAR(255) NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sessions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  token CHAR(64) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE friendships (
  id INT AUTO_INCREMENT PRIMARY KEY,
  sender_id INT NOT NULL,
  receiver_id INT NOT NULL,
  status ENUM('pending', 'accepted') DEFAULT 'pending',
  client_uuid CHAR(36) UNIQUE NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY pair (sender_id, receiver_id)
);

CREATE TABLE posts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  text TEXT,
  client_uuid CHAR(36) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE post_images (
  id INT AUTO_INCREMENT PRIMARY KEY,
  post_id INT NOT NULL,
  path VARCHAR(255) NOT NULL,
  sort_order INT DEFAULT 0
);

CREATE TABLE reactions (
  post_id INT NOT NULL,
  user_id INT NOT NULL,
  type ENUM('like', 'love', 'care', 'haha', 'wow', 'sad') NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, user_id)             -- one reaction per user per post
);

CREATE TABLE comments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  post_id INT NOT NULL,
  user_id INT NOT NULL,
  parent_id INT NULL,
  text TEXT,
  image_path VARCHAR(255) NULL,
  client_uuid CHAR(36) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comment_likes (
  comment_id INT NOT NULL,
  user_id INT NOT NULL,
  PRIMARY KEY (comment_id, user_id)
);

CREATE TABLE stories (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  media_path VARCHAR(255) NOT NULL,
  media_type ENUM('image', 'video') NOT NULL,
  label VARCHAR(120),
  audience ENUM('story', 'close_friends') DEFAULT 'story',
  client_uuid CHAR(36) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE close_friends (
  user_id INT NOT NULL,
  friend_id INT NOT NULL,
  PRIMARY KEY (user_id, friend_id)
);

CREATE TABLE messages (
  id INT AUTO_INCREMENT PRIMARY KEY,
  sender_id INT NOT NULL,
  receiver_id INT NOT NULL,
  type ENUM('text', 'image', 'video', 'file', 'system') DEFAULT 'text',
  content TEXT,
  media_path VARCHAR(255) NULL,
  file_name VARCHAR(150) NULL,
  client_uuid CHAR(36) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  edited_at DATETIME NULL,
  is_deleted TINYINT DEFAULT 0,
  INDEX conv (sender_id, receiver_id, id)
);

CREATE TABLE calls (
  id INT AUTO_INCREMENT PRIMARY KEY,
  caller_id INT NOT NULL,
  receiver_id INT NOT NULL,
  type ENUM('audio', 'video'),
  status ENUM('missed', 'answered', 'declined'),
  started_at DATETIME,
  duration INT DEFAULT 0,
  client_uuid CHAR(36) UNIQUE NOT NULL
);

CREATE TABLE notifications (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  type VARCHAR(30),
  from_user_id INT,
  data TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 4.2 Files and config

```php
<?php // config.sample.php (copy to config.php, fill in, never commit config.php)
define('DB_HOST', 'localhost');
define('DB_NAME', 'kinnect');
define('DB_USER', 'root');
define('DB_PASS', '');
define('UPLOAD_DIR', __DIR__ . '/uploads/');
define('FCM_PROJECT_ID', 'your-firebase-project-id');
define('FCM_SA_PATH', 'C:/secrets/service-account.json'); // outside repo
```

**`backend/.htaccess` (fixes the missing Authorization header on XAMPP):**
```apache
RewriteEngine On
RewriteCond %{HTTP:Authorization} ^(.*)
RewriteRule .* - [E=HTTP_AUTHORIZATION:%1]
```

### 4.3 `common.php` (the helper every endpoint includes)

```php
<?php
require_once __DIR__ . '/config.php';
header('Content-Type: application/json; charset=utf-8');
date_default_timezone_set('UTC'); // keep ALL times in UTC

function db() {
    static $pdo;
    if (!$pdo) {
        $pdo = new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4',
            DB_USER, DB_PASS, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC]);
        $pdo->exec("SET time_zone = '+00:00'");
    }
    return $pdo;
}

function out($data = [], $code = 200) {
    http_response_code($code);
    echo json_encode(['ok' => true, 'data' => $data]); exit;
}

function fail($msg, $code = 400) {
    http_response_code($code);
    echo json_encode(['ok' => false, 'error' => $msg]); exit;
}

function body() { // accepts JSON or form/multipart
    $j = json_decode(file_get_contents('php://input'), true);
    return is_array($j) ? $j : $_POST;
}

function auth() { // returns the logged-in user id or 401
    $h = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (!preg_match('/Bearer\s+([a-f0-9]{64})/', $h, $m)) fail('unauthorized', 401);
    $st = db()->prepare('SELECT user_id FROM sessions WHERE token=?');
    $st->execute([$m[1]]);
    $row = $st->fetch();
    if (!$row) fail('unauthorized', 401);
    db()->prepare('UPDATE users SET last_seen=UTC_TIMESTAMP() WHERE id=?')
        ->execute([$row['user_id']]);
    return (int)$row['user_id'];
}

function save_upload($field, $subdir, $allowed) { // returns relative path or null
    if (empty($_FILES[$field]) || $_FILES[$field]['error'] !== UPLOAD_ERR_OK) return null;
    $f = $_FILES[$field];
    $ext = strtolower(pathinfo($f['name'], PATHINFO_EXTENSION));
    if (!in_array($ext, $allowed)) fail('file type not allowed');
    if ($f['size'] > 50*1024*1024) fail('file too large');
    $dir = UPLOAD_DIR.$subdir.'/';
    if (!is_dir($dir)) mkdir($dir, 0775, true);
    $name = bin2hex(random_bytes(12)).'.'.$ext;
    move_uploaded_file($f['tmp_name'], $dir.$name);
    return 'uploads/'.$subdir.'/'.$name;
}
```

For multiple files (post images) loop over `$_FILES['images']['name']` with an index, or send them as `images[]` and re-use the same checks per file. Store only relative paths.

### 4.4 The idempotency pattern (memorise this)

Every write endpoint follows this exact shape. It is how you satisfy 'no duplicates' in the marking scheme and it is the sentence you will say in the demo.

```php
$uid = auth();
$in = body();
$uuid = $in['client_uuid'] ?? fail('client_uuid required');

// 1. Already processed? Return the existing row as success.
$st = db()->prepare('SELECT * FROM posts WHERE client_uuid=?');
$st->execute([$uuid]);
if ($row = $st->fetch()) out($row);

// 2. Otherwise insert. A race is still safe because of the UNIQUE index:
try {
    db()->prepare('INSERT INTO posts (user_id, text, client_uuid) VALUES (?,?,?)')
        ->execute([$uid, $in['text'] ?? '', $uuid]);
} catch (PDOException $e) {
    if ($e->getCode() == 23000) { /* duplicate: fetch existing and out() it */ }
    else throw $e;
}
```

Writes that are naturally idempotent (setting a reaction, liking a comment, accepting a request) still carry a UUID in the queue, but the endpoint can simply upsert or ignore repeats.

### 4.5 Time handling

* Store every timestamp in UTC. Return ISO-8601 strings or epoch milliseconds.
* The 5 minute rule and the 24 hour story rule are computed on the server with `UTC_TIMESTAMP()`, never from phone time.

### Part 4 Checklist
- [ ] `schema.sql` imports cleanly
- [ ] `common.php` + `.htaccess` in place
- [ ] A test endpoint behind `auth()` returns `401` without a token
- [ ] Commit: `'feat(backend): schema, common helpers, upload and idempotency pattern'`

---

## Part 5: Android foundation

### 5.1 Dependencies (module `build.gradle`)

```groovy
// Networking
implementation 'com.squareup.retrofit2:retrofit:2.11.0'
implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

// Local DB
implementation 'androidx.room:room-runtime:2.6.1'
annotationProcessor 'androidx.room:room-compiler:2.6.1' // kapt/ksp for Kotlin

// Background sync
implementation 'androidx.work:work-runtime:2.9.1'

// Secure token storage
implementation 'androidx.security:security-crypto:1.1.0-alpha06'

// Lifecycle (online status, LiveData)
implementation 'androidx.lifecycle:lifecycle-process:2.8.4'
implementation 'androidx.lifecycle:lifecycle-livedata:2.8.4'

// Images
implementation 'com.github.bumptech.glide:glide:4.16.0'

// Firebase (messaging only)
implementation platform('com.google.firebase:firebase-bom:33.1.2')
implementation 'com.google.firebase:firebase-messaging'

// Agora
implementation 'io.agora.rtc:full-sdk:4.3.0'

// Tests
androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1'
androidTestImplementation 'androidx.test.ext:junit:1.2.1'
```

Version numbers drift. If Android Studio suggests a newer stable version, use it. Do not add the Firebase Auth or Realtime Database libraries, and remove any leftover Assignment 2 code that uses them.

### 5.2 Package structure

```text
com.groupname.i220001/
  ui/       activities, fragments, adapters (existing screens)
  data/
    remote/ ApiService, ApiClient, AuthInterceptor, dto/
    local/  AppDatabase, dao/, entity/
    repo/   AuthRepo, PostRepo, ChatRepo, FriendRepo, StoryRepo
  sync/     SyncManager, SyncWorker, NetworkMonitor
  util/     TokenStore, UuidUtil, TimeUtil, UrlUtil
  fcm/      KinnectMessagingService
  call/     CallActivity, AgoraManager
```

### 5.3 Token storage and interceptor

```java
// TokenStore.java
public class TokenStore {
    private final SharedPreferences prefs;
    public TokenStore(Context c) throws Exception {
        MasterKey key = new MasterKey.Builder(c)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        prefs = EncryptedSharedPreferences.create(c, "secure_prefs", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }
    public void save(String token, int userId) { prefs.edit()
        .putString("token", token).putInt("uid", userId).apply(); }
    public String token() { return prefs.getString("token", null); }
    public void clear() { prefs.edit().clear().apply(); }
}

// AuthInterceptor.java
public Response intercept(Chain chain) throws IOException {
    Request r = chain.request();
    String t = tokenStore.token();
    if (t != null) r = r.newBuilder().header("Authorization", "Bearer " + t).build();
    Response resp = chain.proceed(r);
    if (resp.code() == 401) { /* broadcast 'session expired' -> go to Login */ }
    return resp;
}
```

### 5.4 Retrofit client

```java
public class ApiClient {
    public static final String BASE_URL = BuildConfig.BASE_URL; // single source of truth
    public static ApiService create(TokenStore ts) {
        OkHttpClient ok = new OkHttpClient.Builder()
            .addInterceptor(new AuthInterceptor(ts))
            .addInterceptor(new HttpLoggingInterceptor().setLevel(Level.BODY)) // dev only
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS).build();
        return new Retrofit.Builder().baseUrl(BASE_URL).client(ok)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ApiService.class);
    }
}
```

Define one generic response wrapper `ApiResponse<T> { boolean ok; T data; String error; }` and use it in every Retrofit method. For image loading build full URLs with `UrlUtil.full(path) = BASE_URL + path`.

### 5.5 Room database and the `pending_actions` table

```java
@Entity(tableName = "pending_actions")
public class PendingAction {
    @PrimaryKey(autoGenerate = true) public long id; // preserves order
    public String uuid;         // client UUID sent to the server
    public String type;         // SEND_MESSAGE, CREATE_POST, REACT, COMMENT, ...
    public String payloadJson;  // everything needed to replay the request
    public String status;       // PENDING, SENDING, FAILED
    public int attempts;
    public long createdAt;
}
```

Every content entity (message, post, comment, friendship, story) also gets a `syncStatus` column (`SYNCED`, `PENDING`, `FAILED`), a `clientUuid` column, and a nullable `serverId`. Pending items show a clock icon in the UI.

**Local entities you will need:**

| Entity | Key columns |
| :--- | :--- |
| **UserEntity** | `id`, `name`, `username`, `bio`, `profilePic`, `coverPic`, `isOnline`, `lastSeen`, `friendStatus` |
| **FriendEntity** | `userId`, `friendId`, `status` (`none` / `requested` / `incoming` / `friends`) |
| **PostEntity + PostImageEntity** | `id`/`serverId`, `clientUuid`, `userId`, `text`, `createdAt`, `myReaction`, `reactionCount`, `commentCount`, `syncStatus` |
| **CommentEntity** | `id`, `postId`, `parentId`, `userId`, `text`, `imagePath`, `likeCount`, `likedByMe`, `clientUuid`, `syncStatus` |
| **StoryEntity** | `id`, `userId`, `mediaPath`, `mediaType`, `label`, `createdAt`, `audience` |
| **MessageEntity** | `id`/`serverId`, `clientUuid`, `peerId`, `senderId`, `type`, `content`, `mediaPath`, `createdAt`, `editedAt`, `isDeleted`, `syncStatus` |
| **CallLogEntity** | `id`, `peerId`, `type`, `status`, `startedAt`, `duration` |
| **PendingAction** | see above |

### 5.6 Repository pattern (the template for every feature)

```java
// READ: UI observes Room. Network only refreshes Room.
public LiveData<List<PostEntity>> feed() { return postDao.observeFeed(); }
public void refreshFeed() {
    api.getFeed().enqueue(new Callback<>() {
        onResponse: db.runInTransaction(() -> postDao.upsertAll(mapped)); // UI auto-updates
        onFailure:  /* ignore, cache stays */
    });
}

// WRITE: local first, then queue
public void createPost(String text, List<Uri> images) {
    String uuid = UUID.randomUUID().toString();
    postDao.insert(localPost(uuid, text, images, SyncStatus.PENDING)); // shows now
    pendingDao.insert(new PendingAction(uuid, "CREATE_POST", toJson(text, images)));
    SyncManager.trigger(context); // tries immediately, retries when online
}
```

### 5.7 Network monitor and SyncManager

* `NetworkMonitor` registers a `ConnectivityManager.NetworkCallback`; in `onAvailable` it calls `SyncManager.trigger()`.
* `SyncManager.trigger()` enqueues a unique `OneTimeWorkRequest` (`enqueueUniqueWork("sync", KEEP, ...)`) with constraint `NetworkType.CONNECTED`.
* Also schedule a periodic worker (15 minutes) as a safety net.
* Expose an `isOnline()` helper so repositories can skip pointless calls.

### 5.8 Wire the Application class

* Create `KinnectApp extends Application`: build Room, TokenStore, ApiService once (simple singletons are fine), start NetworkMonitor, register `ProcessLifecycleOwner` observer (used later for presence).
* Declare it in the manifest with `android:name`.

### Part 5 Checklist
- [ ] App builds with all new dependencies
- [ ] `ApiService` can call a test endpoint (check Logcat)
- [ ] Room DB creates on launch
- [ ] Commit: `'feat(android): networking, token store, Room, repository base'`

---

## Part 6: Feature Sprints 1 to 12

### Sprint 1: Splash, Auth and first-time profile setup
**Marks at stake:** Splash 5 + Auth 5 + part of Profiles  
**Goal:** A user can sign up, log in, be forced through profile setup once, and log out cleanly. Everything after this depends on the token.

#### Backend (PHP + MySQL)
* `auth/signup.php`: validate name, email, password (min 8). Check email unique. `password_hash($pw, PASSWORD_DEFAULT)`. Insert user (`profile_complete=0`). Create a session token: `bin2hex(random_bytes(32))`. Return token, user id, `profile_complete`.
* `auth/login.php`: look up by email, `password_verify`, create a new session row, set `is_online = 1`, return token + user + `profile_complete`. Same error message for wrong email or password.
* `auth/logout.php`: delete the session row for this token, set `is_online = 0`, clear `fcm_token` for this device.
* `users/check_username.php?u=...` (unique check as the user types) and `users/setup_profile.php` (username + bio, sets `profile_complete = 1`, rejects duplicate username).

#### Android
* **Splash (01):** 5 second delay (`Handler.postDelayed` 5000). Then if `TokenStore` has a token go straight to Home (even offline), otherwise Login. Never call the network from Splash.
* **Login (02):** call login endpoint, save token, then branch: `profile_complete == 0` goes to Edit profile (16) in setup mode, else Home. Show server error messages. Keep the Recent login card (store last logged in name/email/avatar locally in normal SharedPreferences, never the password).
* **Sign up (03):** keep the password strength meter as-is. Call signup, save token, go to Edit profile setup.
* **Edit profile setup mode:** user must pick a unique username and bio before Home is reachable. Block the back button in this mode.
* **Logout (Menu, 19):** call logout endpoint (ignore failure if offline), then clear `TokenStore`, wipe Room (`db.clearAllTables()` on a background thread), cancel WorkManager jobs, and start Login with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.
* **Handle 401 anywhere:** clear session and go to Login.

#### Offline behaviour
* Login and signup need internet and should show a clear 'No connection' message if offline.
* Splash to Home works fully offline if a token exists, using cached data.
* Logout offline: still clear locally; the server session will simply expire later. Optionally queue a `LOGOUT` action so the server token is invalidated on reconnect (clear the token locally first).

#### Test before you commit
- [ ] Sign up new user, force username setup, reach Home
- [ ] Kill app and reopen: Splash goes straight to Home after 5 s
- [ ] Airplane mode + reopen: still lands on Home
- [ ] Wrong password shows an error
- [ ] Logout returns to Login and Back does not return to Home
- [ ] Server: password column holds a hash, not plain text; token row deleted after logout

**Commit messages:** `feat(backend): signup/login/logout endpoints`; `feat(android): splash routing and token storage`; `feat(android): login, signup, profile setup flow`

---

### Sprint 2: Profiles, friends and search
**Marks at stake:** Friends/Profiles 5 + Search/Filters 5  
**Goal:** Users can search, send/accept/delete friend requests, view profiles and edit their own profile with pictures.

#### Backend (PHP + MySQL)
* `users/get_profile.php?id=`: returns user data, friend status relative to the caller (`none`, `requested`, `incoming`, `friends`), friend count, online flag.
* `users/update_profile.php` (multipart): name, username (unique check), bio, `profile_pic`, `cover_pic`. Replace file, delete old file.
* `friends/send_request.php`, `accept_request.php`, `delete_request.php` (cancel/decline), `remove_friend.php`. Prevent duplicate pairs and self-requests; if the other user already sent you a request, accepting it is the right behaviour.
* `friends/list.php` (accepted friends) and `friends/requests.php` (incoming pending).
* `users/search.php?q=&filter=everyone|friends|requests`: `LIKE` on username and name, exclude self, include each result's friend status. Limit to 30 rows.
* On `send_request` create a notifications row (push comes in Sprint 9).

#### Android
* **Search (13):** `TextWatcher` with a 300 ms debounce, call search each time, cancel the previous call. Filter chips Friends / Requests / Everyone change the filter param. When offline, search the local `UserEntity` table.
* **Button state machine:** Add friend -> Requested -> Friends (and Accept / Decline when incoming). Update the local `FriendEntity` optimistically, then sync.
* **Friends (14), Notifications (18):** list from SQLite, refresh from server on open. Accept/Delete buttons call the endpoints.
* **Other profile (17):** load from `get_profile.php`, cache user.
* **Edit profile (16):** pick image with the system picker, upload as multipart with `MultipartBody.Part`. Compress large images before upload (max 1280 px, 80% JPEG).

#### Offline behaviour
* Friend request actions go through `pending_actions` (`SEND_REQUEST`, `ACCEPT_REQUEST`, `DELETE_REQUEST`).
* Profile edits online only is acceptable, but show a friendly message when offline.

#### Test before you commit
- [ ] Two accounts: A requests B, B sees it in Notifications and Friends, accepts, both show Friends
- [ ] Search 'ali' finds by name and username
- [ ] Filter chips change results
- [ ] Profile picture and cover upload and appear on the other device
- [ ] Duplicate username is rejected

**Commit messages:** `feat(backend): friend system endpoints`; `feat(backend): user search with filters`; `feat(android): search, friends and profile screens`

---

### Sprint 3: Posts and the home feed
**Marks at stake:** Posts 10 (shared with Sprint 4)  
**Goal:** Create text + multi-image posts and show them in a cached, newest-first feed with the +N overlay.

#### Backend (PHP + MySQL)
* `posts/create_post.php` (multipart): text + `images[]`, `client_uuid`, idempotent. Save each image under `uploads/posts/`, insert `post_images` rows in order. Wrap in a DB transaction.
* `posts/get_feed.php?before_id=&limit=20`: posts by the user and their accepted friends, `ORDER BY id DESC`. Include author name/pic, image paths, reaction counts, the caller's own reaction and comment count. Paginate with `before_id`.
* `posts/delete_post.php` (optional but useful).

#### Android
* **Create post (07) and Photo picker (08):** multi-select images (`ActivityResultContracts.PickMultipleVisualMedia`). Persist the picked Uris to the app's private folder immediately so they survive until sync (content URIs can lose permission).
* **Home feed (04):** RecyclerView observing `PostDao`. Show 1, 2, 3, 4 image layouts; for 5+ images show the first four and a `'+N'` overlay on the fourth tile where $N = \text{total} - 4$.
* Pull-to-refresh and infinite scroll call `get_feed` with `before_id`.
* Prune the local cache to the newest ~100 posts to keep SQLite small.

#### Offline behaviour
* `createPost` writes a `PostEntity` (`PENDING`) at once; it appears at the top with a clock icon.
* Sync uploads the multipart body, then replaces the local row with the server row (matched by `clientUuid`, set `serverId`, status `SYNCED`). Do not insert a second row.
* Feed opens instantly from SQLite even offline.

#### Test before you commit
- [ ] Post with 1, 2, 3, 4 and 6 images renders correctly with +2 on the 6-image post
- [ ] Airplane mode, create post, it shows pending; reconnect, it becomes synced once
- [ ] Friend sees it in their feed on the second device; stranger does not
- [ ] Feed order is newest first

**Commit messages:** `feat(backend): create_post and get_feed`; `feat(android): create post with multi image picker`; `feat(android): home feed with +N grid and SQLite cache`

---

### Sprint 4: Reactions and comments
**Marks at stake:** Posts, Reactions and Comments 10 (shared)  
**Goal:** Six reactions, change/remove, nested comments, comment likes and image comments.

#### Backend (PHP + MySQL)
* `posts/set_reaction.php`: body `post_id` + `type` or `type=null/"none"`. Use `INSERT ... ON DUPLICATE KEY UPDATE`; delete the row if removing. Enforces one reaction per user per post through the composite primary key.
* `posts/get_reactions.php`: counts by type and the list of reactors.
* `comments/add_comment.php` (multipart for optional image): `post_id`, `parent_id` (nullable), `text`, `client_uuid`.
* `comments/get_comments.php?post_id=`: flat list with `parent_id`, like counts and `likedByMe`; the app nests them.
* `comments/like_comment.php` toggles a like.

#### Android
* **Reactions (05):** long-press the Like button to show the six reactions. Tapping the same reaction again removes it. Update the local row (`myReaction`, counts) immediately.
* **Comments (06):** bottom sheet or screen; replies indent under their parent; like button per comment; attach an image from the picker; show 'pending' on queued comments.
* Comment count on the feed card updates from the local count.

#### Offline behaviour
* Queue types: `REACT`, `COMMENT`, `LIKE_COMMENT`. For `REACT` keep only the latest pending reaction per post (replace an earlier queued one) so the sync does not flicker.
* Comments carry `client_uuid` and are idempotent.

#### Test before you commit
- [ ] Change a reaction from Like to Love: count moves; tap Love again: removed
- [ ] Reply to a comment appears nested; like a comment from device B shows on A after refresh
- [ ] Image comment uploads and renders
- [ ] Offline comment shows pending then syncs once

**Commit messages:** `feat(backend): reactions and comments endpoints`; `feat(android): reaction picker`; `feat(android): comments with replies, likes and images`

---

### Sprint 5: The offline sync engine
**Marks at stake:** Offline support 15 (the biggest engineering item)  
**Goal:** A single reusable engine that replays every queued action in order, safely, with no duplicates and no loss. After this sprint, every earlier feature is routed through it and every later feature just adds a case.

#### Backend (PHP + MySQL)
* Audit every write endpoint: accepts `client_uuid`, returns the existing row on repeat, returns the same JSON shape for 'created' and 'already existed'.
* Make sure the response includes the server id and any server-generated fields (`created_at`, media paths) so the app can reconcile.

#### Android
* `SyncWorker.doWork()`: load pending actions ordered by `id ASC`. For each: mark `SENDING`, switch on `type`, build the Retrofit call from `payloadJson`, execute synchronously (`call.execute()`, one at a time, so order is preserved).
* **Success:** reconcile the local row (set `serverId`, `SYNCED`) and delete the pending action.
* **Network error / 5xx / timeout:** set status back to `PENDING`, increment `attempts`, return `Result.retry()`. Stop the loop so later actions do not jump ahead.
* **Permanent 4xx** (validation, 403 like 'edit window expired'): mark `FAILED`, mark the local item failed, show a retry/discard option, and continue.
* **Dependencies:** if a `REACT` or `COMMENT` targets a post that is itself still pending (no `serverId` yet), the engine must wait; because actions run strictly in order the `CREATE_POST` is already done before it. Resolve the local post id to the `serverId` at send time.
* **Crash safety:** on worker start reset any rows stuck in `SENDING` back to `PENDING`. The server UUID check makes resending safe.
* **Triggers:** after every enqueue, on network available, on app foreground, and periodic 15 min.
* **Expired items:** after sync also run the cleanup that deletes stories older than 24 hours from SQLite.

#### Offline behaviour
* Show a small 'Syncing...' or 'Offline' banner using the same teal design language (primary `#0B5F63`).
* Pending items display a clock; failed items display a retry icon.

#### Test before you commit
- [ ] Airplane mode: send 3 messages (once Sprint 7 exists), 2 reactions, 1 post, 1 friend request. Reconnect: all arrive once, in order
- [ ] Kill the app mid-sync, restart: no duplicates, nothing lost
- [ ] Replay the same request manually in Postman twice: the second call creates nothing
- [ ] Turn Wi-Fi off during an upload: it retries later and succeeds

**Commit messages:** `feat(android): pending_actions queue and SyncWorker`; `feat(android): network monitor triggers sync`; `fix: idempotent replies on all write endpoints`

---

### Sprint 6: Stories
**Marks at stake:** Stories 10  
**Goal:** Upload image/video stories with a label, show them at the top of Home, and make them disappear after 24 hours on both server and device.

#### Backend (PHP + MySQL)
* `stories/upload_story.php` (multipart): file (`jpg`/`png`/`mp4`), `label`, `audience` (`story` or `close_friends`), `client_uuid`, idempotent. Detect `media_type` from extension/mime.
* `stories/get_stories.php`: own stories + friends' stories where `created_at > UTC_TIMESTAMP() - INTERVAL 24 HOUR`. For `close_friends` stories return them only to people in the poster's `close_friends` list. Group by user in the app.
* Optional: a cron/scheduled cleanup that deletes expired files from disk (nice to mention in the demo; the query filter is what the marks require).
* `stories/close_friends.php`: add/remove a friend from the close friends list (needed for the audience picker).

#### Android
* **Camera (09):** CameraX for photo/video capture, plus a gallery picker. Request `CAMERA` and `RECORD_AUDIO` permissions at runtime.
* **Story editor (10):** preview, add the text label overlay (store as a label string; optionally bake it into the image), buttons for 'Your story' and 'Close friends'.
* **Home story row:** horizontal RecyclerView with 'Create story' first, then friends with active stories. Tapping opens a full-screen viewer with a progress bar (5 s for images, video length for video).
* On app start and after every sync run `storyDao.deleteOlderThan(now - 24h)`. Also filter by time in the DAO query so an expired story never shows even if cleanup has not run.

#### Offline behaviour
* Queue type `UPLOAD_STORY` with the media copied into the app's private folder first.
* Cached stories (already downloaded) remain viewable offline until they expire.

#### Test before you commit
- [ ] Upload image and video stories; the friend sees them
- [ ] Close-friends story is hidden from a non-close friend
- [ ] Change a story's `created_at` to 25 hours ago in MySQL: it disappears from the API and, after a refresh, from SQLite
- [ ] Offline story queues and uploads on reconnect, once

**Commit messages:** `feat(backend): story upload and 24h expiry query`; `feat(android): camera and story editor`; `feat(android): story row and viewer with local expiry cleanup`

---

### Sprint 7: Messaging
**Marks at stake:** Messaging 10  
**Goal:** Chat list and conversation screens with text, image, video and file messages, edit/delete within 5 minutes, and new messages appearing without leaving the screen.

#### Backend (PHP + MySQL)
* `chat/send_message.php` (multipart): `receiver_id`, `type`, `content`, optional `file`, `client_uuid`, idempotent. Only allowed between friends (or allow anyone, decide and keep consistent). Triggers a notification row.
* `chat/get_messages.php?peer_id=&after_id=`: messages in both directions with `id > after_id`. Also return messages whose `edited_at` or `is_deleted` changed since a given timestamp, so edits and deletes reach the other device (use `?since` timestamp or an `updated_at` column, add `updated_at` to the table with `ON UPDATE CURRENT_TIMESTAMP`).
* `chat/get_conversations.php`: one row per peer with last message, time, unread count and the peer's online flag.
* `chat/edit_message.php`: only the sender; `TIMESTAMPDIFF(SECOND, created_at, UTC_TIMESTAMP()) <= 300`, else `403` 'Edit window expired'. Sets `edited_at`.
* `chat/delete_message.php`: same 5 minute rule; soft delete (`is_deleted=1`, clear content and media).
* `chat/mark_read.php` for unread counts (optional).

#### Android
* **Chats (20):** observe conversations from SQLite; refresh every ~10 s while visible.
* **Chat (21):** RecyclerView with sent/received bubbles, image/video/file bubbles (file shows name and a download action), 'Edited' label, and 'Message deleted' placeholder. Attachment picker for image, video and file.
* **Real-time polling:** in `onResume` start a loop (Handler or coroutine) calling `get_messages` every 2 s with the highest known server id; in `onPause` stop it. Insert results into Room; the list updates itself and auto-scrolls if the user is at the bottom.
* **Long-press menu:** Edit / Delete shown only on your messages less than 5 minutes old (check locally using the server `created_at` plus a small tolerance, the server remains the authority). If the server returns `403` show a toast and refresh.
* Video playback with a simple VideoView/ExoPlayer in the bubble or a viewer screen; images open in a full-screen viewer.

#### Offline behaviour
* `SEND_MESSAGE`, `EDIT_MESSAGE`, `DELETE_MESSAGE` are queued. A message shows instantly with a clock icon.
* Editing a message that is still pending: just modify the queued payload / local row instead of queuing an edit.
* Deleting a pending message: remove the pending action and local row, no server call.
* An offline edit/delete that arrives after the 5 minute window will be rejected by the server; mark it failed and revert the local text.

#### Test before you commit
- [ ] Two devices chat live with ~2 s latency
- [ ] Send image, video, PDF; receiver can open them
- [ ] Edit within 5 min shows 'Edited' on both
- [ ] Try editing after 5 min (set `created_at` back in MySQL): server rejects
- [ ] Offline messages go out in order and once

**Commit messages:** `feat(backend): messaging endpoints with 5-minute edit rule`; `feat(android): chats list and conversation screen`; `feat(android): polling and media messages`

---

### Sprint 8: Online / offline presence
**Marks at stake:** Online status 5  
**Goal:** Green dot in Chats and 'Active now' reflect real presence, including when the app is killed.

#### Backend (PHP + MySQL)
* `users/heartbeat.php` (sets `last_seen = UTC_TIMESTAMP()`, `is_online = 1`) and `users/set_status.php` (`online` true/false).
* When returning users, compute `online = is_online=1 AND last_seen > UTC_TIMESTAMP() - INTERVAL 45 SECOND`. This makes crashed apps and dead phones flip to offline automatically.
* Return `last_seen` for 'Last seen 10 min ago' text if you want it.

#### Android
* Observe `ProcessLifecycleOwner`: `onStart` sets online and starts a 20 s heartbeat loop; `onStop` stops the loop and sets offline.
* Chats and Chat toolbar poll presence (conversation list refresh already includes it, Chat polls every ~10 s).
* Show a green dot or 'Active now' when online; otherwise 'Last seen ...' or nothing, matching the UI PDF.

#### Offline behaviour
* Offline users cannot send heartbeats so they naturally expire to offline for others after ~45 s.
* On logout set offline explicitly.

#### Test before you commit
- [ ] Open app on A: B sees the dot within ~20 s
- [ ] Background A: B sees it go offline
- [ ] Force-stop A: B sees offline after ~45 s

**Commit messages:** `feat(backend): presence heartbeat`; `feat(android): lifecycle-based online status and indicators`

---

### Sprint 9: FCM push notifications
**Marks at stake:** Push Notifications 5  
**Goal:** The server sends push notifications for new messages, friend requests and screenshot alerts.

#### Backend (PHP + MySQL)
* `users/save_token.php`: store the device's FCM token on the user row (and clear it on logout).
* **FCM sender (`notify.php` helper):** use the FCM HTTP v1 API. Steps: load the service-account JSON, build a signed JWT (RS256, scope `https://www.googleapis.com/auth/firebase.messaging`), exchange it at `https://oauth2.googleapis.com/token` for an access token (cache it ~50 min), then POST to `https://fcm.googleapis.com/v1/projects/PROJECT_ID/messages:send` with cURL.
  * *Tip:* the `kreait/firebase-php` or `google/auth` Composer packages do the JWT and token exchange for you and save a lot of time.
* Call the helper from `send_message`, `send_request` and `screenshot_event`. Send data messages (`type`, `from_user_id`, `title`, `body`) so your app code builds the notification consistently.

#### Android
* Add `KinnectMessagingService extends FirebaseMessagingService`. In `onNewToken` and after login call `save_token`.
* In `onMessageReceived` build a notification (channel `'kinnect_general'`, teal accent). Tapping opens the right screen (chat with that user, or Notifications 18).
* Android 13+: request `POST_NOTIFICATIONS` at runtime.
* Suppress the notification if the user is already inside that chat.

#### Offline behaviour
* Pushes only reach online devices; queued actions generate their pushes when they eventually sync (server side), which is correct behaviour.

#### Test before you commit
- [ ] Send message while receiver app is closed: push arrives
- [ ] Friend request push
- [ ] Tap notification opens the right screen
- [ ] Token cleared on logout (no pushes to a logged-out device)

**Commit messages:** `feat(backend): FCM v1 sender and token storage`; `feat(android): messaging service, channels and deep links`

---

### Sprint 10: Screenshot detection
**Marks at stake:** Security and Privacy 5  
**Goal:** When someone screenshots a chat, the other person gets a push and a system message appears in the conversation.

#### Backend (PHP + MySQL)
* `chat/screenshot_event.php`: body `peer_id` (the chat owner being screenshotted) and `client_uuid`. Insert a message of type `'system'` with content `'NAME took a screenshot'` (sender = the person who took it), then push to the peer.
* The system message flows through the normal `get_messages` polling so both sides see it.

#### Android
* Android has no direct screenshot callback before Android 14. Two approaches (use both for reliability):
  * **Android 14+ (API 34):** register `Activity.ScreenCaptureCallback` in the chat activity (`registerScreenCaptureCallback` in `onStart`, unregister in `onStop`). Needs the `DETECT_SCREEN_CAPTURE` permission.
  * **Older versions:** register a `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` while Chat is open, and when a new row appears whose path/name contains `'screenshot'` (case-insensitive), fire the event. Needs `READ_MEDIA_IMAGES` (or `READ_EXTERNAL_STORAGE` on old devices).
* **Throttle:** ignore repeat events within 3 seconds.

#### Offline behaviour
* `SCREENSHOT_EVENT` can be queued; the system message is created when it syncs.

#### Test before you commit
- [ ] Take a screenshot in chat on A: B receives a push and sees '... took a screenshot' in the conversation
- [ ] Screenshots outside the chat do not trigger anything

**Commit messages:** `feat(backend): screenshot system message and push`; `feat(android): screenshot detection (API 34 callback + MediaStore observer)`

---

### Sprint 11: Voice and video calls with Agora
**Marks at stake:** Calls 5  
**Goal:** Real-time audio/video calls from the Chat icons, with call logs stored on the server.

#### Backend (PHP + MySQL)
* `calls/log_call.php`: `caller_id`, `receiver_id`, `type`, `status`, `started_at`, `duration`, `client_uuid` (idempotent).
* `calls/get_logs.php`: history for the user (optional screen/section).
* `calls/invite.php`: sends an FCM data push of type `'incoming_call'` with channel name and call type to the receiver.
* If your Agora project uses an App Certificate, add `calls/agora_token.php` that generates an RTC token (Agora's PHP token builder). For development, Testing mode (no certificate) is simpler.

#### Android
* Create `AgoraManager` wrapping `RtcEngine`. Channel name: `call_{minId}_{maxId}`. Request `RECORD_AUDIO` and `CAMERA` permissions.
* **Caller flow:** tap phone/video icon in Chat (21), open `CallActivity` (outgoing), call `invite.php`, join the channel, show 'Calling...' until the remote user joins (`onUserJoined`).
* **Receiver flow:** FCM data push shows a full-screen/heads-up incoming-call notification with Accept / Decline. Accept opens `CallActivity` and joins the same channel. Decline sends a status back (or just leave the caller timing out).
* **Call UI:** local and remote `SurfaceView`s for video, mute, camera flip, speaker, end call buttons. Style with teal `#0B5F63`.
* **On end:** compute duration, set status (`answered`/`missed`/`declined`) and call `log_call.php`. Add an unanswered timeout (~30 s) that logs `'missed'`.
* Keep Agora App ID in `local.properties` via `BuildConfig`, not in Git.

#### Offline behaviour
* Calls require internet. Disable the icons (or show a toast) when offline. Queue only the call log if the network drops at the end (`CALL_LOG` action).

#### Test before you commit
- [ ] Audio call between two real phones works both ways
- [ ] Video call shows both streams
- [ ] Receiver gets the incoming-call push while app is backgrounded
- [ ] Log row appears in MySQL with correct duration and status

**Commit messages:** `feat(backend): call logs and invite push`; `feat(android): Agora audio/video call screen`; `feat(android): incoming call notification`

---

### Sprint 12: Espresso tests, polish and cleanup
**Marks:** Test cases 5, plus protects your demo marks.

#### 12.1 Two Espresso tests
Pick two critical, stable workflows. Suggested pair:
1. **`LoginFlowTest`:** launch `LoginActivity`, type a seeded test user's email and password, click Login, assert Home is displayed (e.g. `onView(withId(R.id.homeRoot)).check(matches(isDisplayed()))`). Also add a negative assertion: wrong password shows the error text.
2. **`SendMessageTest`:** start logged in, open a chat, type text, click send, assert the text appears in the message list (`onView(withText("hello test")).check(matches(isDisplayed()))`).

* Seed a dedicated test account in MySQL and use it only for tests.
* Use an `IdlingResource` or `Thread.sleep` only as a last resort; prefer waiting for the view with a small polling helper.
* Keep tests in `androidTest/` and make sure they run green before submission. Take a screenshot of the passing run.

#### 12.2 Polish list
- [ ] Every one of the 23 screens works with real data and matches `Kinnect_UI.pdf`
- [ ] No hardcoded demo data remains
- [ ] Empty states (no posts, no friends, no chats) look right
- [ ] Loading indicators on slow calls; error toasts on failures
- [ ] Remove all Assignment 2 Firebase Auth/Realtime Database code and dependencies
- [ ] Permission flows are polished (camera, mic, notifications, media)
- [ ] Any extra screens/indicators (offline banner, call UI) follow the Kinnect style: teal `#0B5F63`, same typography, spacing, corner radii
- [ ] Disable verbose HTTP logging before final build

---

## Part 7: Offline sync engine in depth

### 7.1 Why it must be this design

| Requirement from the assignment | How the design satisfies it |
| :--- | :--- |
| **Screens read from SQLite first** | UI observes Room only; network just updates Room |
| **Queue all user actions when offline** | Every write inserts a `pending_actions` row |
| **Show pending state immediately** | Local row has `syncStatus=PENDING` with clock icon |
| **Retry in order on reconnect** | Worker processes ids `ASC`, stops on first network error |
| **No data loss** | Queue is persisted in SQLite; `SENDING` reset on restart |
| **No duplication** | `client_uuid` `UNIQUE` on server, endpoints return the existing row |

### 7.2 Action types and payloads

| Type | Endpoint | Payload notes |
| :--- | :--- | :--- |
| `SEND_MESSAGE` | `chat/send_message.php` | `peerId`, `type`, `content`, `localMediaPath` |
| `EDIT_MESSAGE` / `DELETE_MESSAGE` | `chat/edit_message.php` / `delete_message.php` | `serverId` or `messageUuid`, `newText` |
| `CREATE_POST` | `posts/create_post.php` | `text`, list of local image paths |
| `REACT` | `posts/set_reaction.php` | `postUuid` or `postId`, `type` or `none` |
| `COMMENT` | `comments/add_comment.php` | `postRef`, `parentRef`, `text`, `localImagePath` |
| `LIKE_COMMENT` | `comments/like_comment.php` | `commentRef` |
| `SEND_REQUEST` / `ACCEPT` / `DELETE` | `friends/*.php` | `userId` |
| `UPLOAD_STORY` | `stories/upload_story.php` | `localMediaPath`, `label`, `audience` |
| `SCREENSHOT_EVENT` / `CALL_LOG` | `chat/screenshot_event.php` / `calls/log_call.php` | `peerId`, call details |

### 7.3 Worker skeleton

```java
public Result doWork() {
    pendingDao.resetSending(); // crash recovery
    List<PendingAction> list = pendingDao.allPendingOrdered();
    for (PendingAction a : list) {
        pendingDao.setStatus(a.id, "SENDING");
        try {
            Response<?> r = execute(a); // switch (a.type) -> Retrofit call
            if (r.isSuccessful() && body(r).ok) {
                reconcileLocal(a, body(r)); // set serverId, SYNCED
                pendingDao.delete(a.id);
            } else if (r.code() >= 400 && r.code() < 500 && r.code() != 401) {
                markFailed(a); // permanent - stop retrying this one
            } else {
                pendingDao.setStatus(a.id, "PENDING"); return Result.retry();
            }
        } catch (IOException e) { // no network / timeout
            pendingDao.setStatus(a.id, "PENDING"); // keep order: stop here
            return Result.retry();
        }
    }
    storyDao.deleteOlderThan(System.currentTimeMillis() - 24L*3600*1000);
    return Result.success();
}
```

### 7.4 Local-vs-server id handling

* Local rows are keyed by `clientUuid` (always present) and get `serverId` after sync.
* When one queued action refers to an item created by an earlier queued action (e.g. comment on a pending post), store the uuid in the payload and resolve it to the `serverId` at send time.
* When the server returns data for something you already have locally (matching `clientUuid`), update that row, never insert a new one. This is the single most common cause of duplicates on the screen.

### 7.5 Edge cases to test
- [ ] Two actions on the same item while offline (react then change reaction)
- [ ] Edit then delete the same pending message
- [ ] App killed during upload
- [ ] Server returns 403 for an expired edit
- [ ] Token expires while offline (401 on reconnect)
- [ ] Large file upload on a weak connection (timeouts: raise `readTimeout`/`writeTimeout` for uploads)
- [ ] Logout with items still pending (warn the user or discard)

---

## Part 8: GitHub and submission

### 8.1 Commit discipline (10 marks)

* One commit per completed feature, and backend and Android pieces clearly labelled. Use conventional prefixes: `feat`, `fix`, `chore`, `test`, `docs`.
  * **Good:** `feat(chat): edit/delete with 5-minute server check`. **Bad:** `update`, `final`.
* Commit at least once per working day. Do not do one huge commit at the end.
* Push to `main` or use short feature branches merged to `main`. Everything must be pushed before the deadline, late commits do not count.
* Verify no secrets are tracked: run `git ls-files | findstr /i "google-services config.php service-account"`. If a secret was ever committed, rotate it and rewrite history.
* Add a README with setup steps (import `schema.sql`, copy `config.sample.php`, set `BASE_URL`, add `google-services.json`, add Agora App ID).

### 8.2 Solo-submission note

The brief mentions both members' commits. Since you are alone, keep your written instructor approval handy and, if allowed, mention it in the README so the marker sees it.

### 8.3 Export and naming

1. Confirm the folder/ZIP, project name and package name against the format your instructor approves.
2. Clean the project (`Build` -> `Clean Project`) and make sure it builds from scratch.
3. Android Studio: `File` -> `Export` -> `Export to ZIP File`. Name it exactly as required (e.g. `i220001_Assignment03.zip`).
4. Include the `backend` folder (PHP + `schema.sql`) in the ZIP or hand it in as instructed, but without `config.php` or service-account files.
5. Do not include `google-services.json` if the instructions say to keep it private; explain the setup in the README.

---

## Part 9: Live demo (20 marks)

### 9.1 Hardware and network checklist
- [ ] Two physical phones (or phone + emulator), both logged into different accounts (e.g. 'Aisha' and 'You')
- [ ] PC running XAMPP, Apache allowed through firewall, MySQL has the seeded data
- [ ] PC and both phones on the same network; `BASE_URL` set to the PC's current LAN IP (check right before the demo)
- [ ] Phone hotspot as backup network, with `BASE_URL` pre-tested for it
- [ ] Chargers, USB cables, `adb` working, Android Studio open to make live code changes
- [ ] Pre-installed latest build on both devices; notification permissions granted; Agora and Firebase keys loaded
- [ ] Seed data: a few friends, posts with multiple images, a story, an existing conversation

### 9.2 Suggested demo script (about 12 minutes)

| Step | Show | What to say |
| :--- | :--- | :--- |
| **1** | Cold start: Splash 5 s then Home (token present). Show Login and Recent login card on a logged-out device. | Token in `EncryptedSharedPreferences`, server-side hashed passwords. |
| **2** | Sign up a new user live, password strength meter, forced username setup. | First-time users cannot reach Home until profile setup. |
| **3** | Search, filter chips, send friend request A to B, push notification on B, accept, buttons change state. | Search hits a PHP endpoint with a debounce. |
| **4** | Create multi-image post (6 images, +2 overlay) on A, appears on B. Reactions and comments with reply. | Feed is read from SQLite, refreshed from the server. |
| **5** | Story upload with label to close friends; viewer on B. Mention 24 h expiry query. | Server stops returning it; app deletes expired rows. |
| **6** | Chat live between devices: text, image, video, file. Edit within 5 min ('Edited'); delete; show 5-min rejection. | Server enforces the 5-minute rule with UTC timestamps. |
| **7** | Presence: dot and 'Active now' change as you background an app. | Heartbeat every 20 s, online if `last_seen < 45 s`. |
| **8** | Push notifications with app closed; screenshot alert message and push. | FCM HTTP v1 sent from PHP. |
| **9** | Audio then video call; show call log row in MySQL. | Agora handles media; server stores logs. |
| **10** | Offline: airplane mode on A. Send messages, create a post, react, comment, send a request. Show pending clocks. Reconnect; show B receives each once, in order. | `pending_actions` queue, WorkManager, client UUIDs make replays safe. |
| **11** | Show `pending_actions` empty and MySQL has no duplicate `client_uuid` (run a `GROUP BY ... HAVING COUNT>1` query). | Proof of no duplicates. |
| **12** | Logout: back stack cleared, SQLite emptied, session row gone. | Token invalidated server-side. |

### 9.3 Be ready to modify code live

You will be asked to change something in real time. Practise these until you can do each in under two minutes:

* Change the edit/delete window from 5 minutes to 2 minutes (the number in PHP).
* Change the story expiry from 24 hours to 1 hour.
* Change the Splash delay or the presence timeout.
* Add a field to a table, return it from an endpoint, show it in a screen.
* Explain any line of `common.php`, the sync worker or the Retrofit setup.

**Be able to explain, without notes:** the UUID idempotency flow, why the UI reads from SQLite, how presence expires after a crash, how the `Authorization` header is validated, how FCM v1 authenticates, and how the screenshot is detected.

---

## Part 10: Pitfalls and reference

### 10.1 Common problems and fixes

| Symptom | Likely cause | Fix |
| :--- | :--- | :--- |
| **Phone cannot reach server** | Firewall, wrong IP, client isolation | Allow Apache on private network; verify URL in phone browser; use hotspot |
| **Cleartext HTTP not permitted** | Missing manifest flag | `usesCleartextTraffic="true"` |
| **401 on every request** | Apache strips Authorization header | Add the `.htaccess` rewrite rule |
| **Upload fails for videos** | PHP size limits | Raise `upload_max_filesize`, `post_max_size`, restart Apache |
| **Duplicate posts or messages** | Insert instead of update on sync | Match by `clientUuid` and update; `UNIQUE` index on server |
| **Times wrong by hours** | Mixed timezones | Store and compare everything in UTC |
| **Images not loading** | Absolute or wrong paths | Store relative paths; prefix `BASE_URL` in app |
| **Content URI permission lost on sync** | Using picker Uri later | Copy picked files to app storage immediately |
| **FCM never arrives** | Wrong package / expired token / no `POST_NOTIFICATIONS` | Re-download `google-services.json`, refresh token, grant permission, test with a data message |
| **Worker never runs** | Battery optimisation or wrong constraints | Trigger on `NetworkCallback` too; disable battery saver while testing |
| **Reaction flickers after sync** | Several queued reactions replayed | Keep only the latest queued reaction per post |

### 10.2 Endpoint reference

| Area | Endpoints |
| :--- | :--- |
| **auth** | `signup.php`, `login.php`, `logout.php` |
| **users** | `check_username.php`, `setup_profile.php`, `get_profile.php`, `update_profile.php`, `search.php`, `heartbeat.php`, `set_status.php`, `save_token.php` |
| **friends** | `send_request.php`, `accept_request.php`, `delete_request.php`, `remove_friend.php`, `list.php`, `requests.php` |
| **posts** | `create_post.php`, `get_feed.php`, `delete_post.php`, `set_reaction.php`, `get_reactions.php` |
| **comments** | `add_comment.php`, `get_comments.php`, `like_comment.php` |
| **stories** | `upload_story.php`, `get_stories.php`, `close_friends.php` |
| **chat** | `send_message.php`, `get_messages.php`, `get_conversations.php`, `edit_message.php`, `delete_message.php`, `mark_read.php`, `screenshot_event.php` |
| **calls** | `invite.php`, `log_call.php`, `get_logs.php`, `agora_token.php` (optional) |
| **notifications** | `list.php` (for screen 18), `notify.php` (internal helper, not a public endpoint) |

### 10.3 Screen to feature map

| Screen | Feature / Sprint |
| :--- | :--- |
| **01 Splash, 02 Log in, 03 Sign up** | Sprint 1 |
| **04 Home feed** | Sprint 3 (feed), Sprint 6 (story row) |
| **05 Reactions, 06 Comments** | Sprint 4 |
| **07 Create post, 08 Photo picker** | Sprint 3 |
| **09 Camera, 10 Story editor** | Sprint 6 |
| **13 Search, 14 Friends, 17 Other profile, 18 Notifications** | Sprint 2 |
| **16 Edit profile** | Sprint 1 (setup), Sprint 2 (edit) |
| **19 Menu / logout** | Sprint 1 |
| **20 Chats, 21 Chat** | Sprint 7, 8, 10, 11 |
| **Remaining screens from `Kinnect_UI.pdf`** | Wire to the same repositories; verify each has real data |

### 10.4 Final pre-submission checklist
- [ ] Instructor approval for solo work saved
- [ ] All 23 screens use live data and match the UI PDF
- [ ] Offline: browse cached content, queue actions, sync once on reconnect
- [ ] No Firebase Auth / Realtime Database left
- [ ] Two Espresso tests pass
- [ ] Secrets not in Git; README explains setup
- [ ] Commit history reflects a feature per commit, before the deadline
- [ ] ZIP/project/package naming matches the approved format
- [ ] Demo rehearsed twice on the actual network

> **If you run out of time:** protect these in this order: Auth and Splash, Offline engine, Posts/Messaging, Friends/Search, Stories, Presence, FCM, Screenshot, Calls, Espresso. Anything half-done but demonstrable beats something unfinished everywhere.
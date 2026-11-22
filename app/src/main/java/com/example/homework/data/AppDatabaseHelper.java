package com.example.homework.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.homework.util.PasswordSecurity;

public class AppDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "campus_exchange.db";
    public static final int DATABASE_VERSION = 8;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_ITEMS = "items";
    public static final String TABLE_CONVERSATIONS = "conversations";
    public static final String TABLE_MESSAGES = "messages";
    public static final String TABLE_POSTS = "posts";
    public static final String TABLE_ORDERS = "orders";
    public static final String TABLE_CART_ITEMS = "cart_items";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        ensureSchema(db);

        seedUsers(db);
        seedItems(db);
        seedConversations(db);
        seedPosts(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ensureSchema(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        ensureSchema(db);
    }

    private void ensureSchema(SQLiteDatabase db) {
        createUsersTable(db);
        createItemsTable(db);
        createConversationsTable(db);
        createMessagesTable(db);
        createPostsTable(db);
        createOrdersTable(db);
        createCartItemsTable(db);

        addColumnIfMissing(db, TABLE_USERS, "major", "TEXT");
        addColumnIfMissing(db, TABLE_USERS, "interests", "TEXT");
        addColumnIfMissing(db, TABLE_USERS, "bio", "TEXT");
        addColumnIfMissing(db, TABLE_USERS, "student_id_image_url", "TEXT");
        addColumnIfMissing(db, TABLE_USERS, "avatar_image_url", "TEXT");
        addColumnIfMissing(db, TABLE_USERS, "verified_at", "INTEGER NOT NULL DEFAULT 0");

        addColumnIfMissing(db, TABLE_ITEMS, "image_url", "TEXT");
        addColumnIfMissing(db, TABLE_ITEMS, "tags", "TEXT");
        addColumnIfMissing(db, TABLE_ITEMS, "preferred_major", "TEXT");
        addColumnIfMissing(db, TABLE_ITEMS, "campus_pickup_only", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(db, TABLE_ITEMS, "created_at", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, TABLE_ITEMS, "is_active", "INTEGER NOT NULL DEFAULT 1");

        addColumnIfMissing(db, TABLE_CONVERSATIONS, "last_message", "TEXT");
        addColumnIfMissing(db, TABLE_CONVERSATIONS, "last_message_at", "INTEGER NOT NULL DEFAULT 0");

        addColumnIfMissing(db, TABLE_MESSAGES, "created_at", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, TABLE_MESSAGES, "is_read", "INTEGER NOT NULL DEFAULT 0");

        addColumnIfMissing(db, TABLE_POSTS, "image_url", "TEXT");
        addColumnIfMissing(db, TABLE_POSTS, "created_at", "INTEGER NOT NULL DEFAULT 0");

        addColumnIfMissing(db, TABLE_ORDERS, "status", "TEXT NOT NULL DEFAULT '待发货'");
        addColumnIfMissing(db, TABLE_ORDERS, "created_at", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, TABLE_ORDERS, "shipped_at", "INTEGER");
        addColumnIfMissing(db, TABLE_ORDERS, "completed_at", "INTEGER");

        db.execSQL("UPDATE " + TABLE_ITEMS + " SET is_active = 1 WHERE is_active IS NULL");
        db.execSQL("UPDATE " + TABLE_ITEMS + " SET created_at = " + System.currentTimeMillis() +
                " WHERE created_at IS NULL OR created_at = 0");
        db.execSQL("UPDATE " + TABLE_POSTS + " SET created_at = " + System.currentTimeMillis() +
                " WHERE created_at IS NULL OR created_at = 0");
        db.execSQL("UPDATE " + TABLE_ORDERS + " SET status = '" + OrderRepository.STATUS_PENDING +
                "' WHERE status IS NULL OR status = ''");

        createIndexes(db);
    }

    private void createUsersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "major TEXT," +
                "interests TEXT," +
                "bio TEXT," +
                "student_id_image_url TEXT," +
                "avatar_image_url TEXT," +
                "verified_at INTEGER NOT NULL DEFAULT 0)");
    }

    private void createItemsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ITEMS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "seller_id INTEGER NOT NULL," +
                "title TEXT NOT NULL," +
                "description TEXT NOT NULL," +
                "category TEXT NOT NULL," +
                "price REAL NOT NULL," +
                "location TEXT NOT NULL," +
                "item_condition TEXT NOT NULL," +
                "image_url TEXT," +
                "tags TEXT," +
                "preferred_major TEXT," +
                "campus_pickup_only INTEGER DEFAULT 1," +
                "is_active INTEGER NOT NULL DEFAULT 1," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(seller_id) REFERENCES users(id))");
    }

    private void createConversationsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONVERSATIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "item_id INTEGER NOT NULL," +
                "buyer_id INTEGER NOT NULL," +
                "seller_id INTEGER NOT NULL," +
                "last_message TEXT," +
                "last_message_at INTEGER NOT NULL," +
                "UNIQUE(item_id, buyer_id, seller_id)," +
                "FOREIGN KEY(item_id) REFERENCES items(id)," +
                "FOREIGN KEY(buyer_id) REFERENCES users(id)," +
                "FOREIGN KEY(seller_id) REFERENCES users(id))");
    }

    private void createMessagesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "conversation_id INTEGER NOT NULL," +
                "sender_id INTEGER NOT NULL," +
                "content TEXT NOT NULL," +
                "is_read INTEGER NOT NULL DEFAULT 0," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(conversation_id) REFERENCES " + TABLE_CONVERSATIONS + "(id)," +
                "FOREIGN KEY(sender_id) REFERENCES users(id))");
    }

    private void createPostsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_POSTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "content TEXT NOT NULL," +
                "image_url TEXT," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(user_id) REFERENCES users(id))");
    }

    private void createOrdersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ORDERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "item_id INTEGER UNIQUE NOT NULL," +
                "buyer_id INTEGER NOT NULL," +
                "seller_id INTEGER NOT NULL," +
                "status TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "shipped_at INTEGER," +
                "completed_at INTEGER," +
                "FOREIGN KEY(item_id) REFERENCES items(id)," +
                "FOREIGN KEY(buyer_id) REFERENCES users(id)," +
                "FOREIGN KEY(seller_id) REFERENCES users(id))");
    }

    private void createCartItemsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CART_ITEMS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "item_id INTEGER NOT NULL," +
                "added_at INTEGER NOT NULL," +
                "UNIQUE(user_id, item_id)," +
                "FOREIGN KEY(user_id) REFERENCES users(id)," +
                "FOREIGN KEY(item_id) REFERENCES items(id))");
    }

    private void seedUsers(SQLiteDatabase db) {
        insertUser(db, "陈雅", "alex", "123456", "数字媒体艺术",
                "相机 摄影 视频 校园记录", "喜欢记录校园生活，也愿意把闲置物品分享给真正需要的人。");
        insertUser(db, "林泽", "mia", "123456", "计算机科学",
                "教材 键盘 编程 电子产品", "平时会整理宿舍和书桌，常把不用的设备和教材挂出来。");
        insertUser(db, "苏晚", "leo", "123456", "工业设计",
                "骑行 露营 生活方式 宿舍用品", "希望把还很好用的物品流转给校内同学，让交易更轻松。");
    }

    private void seedItems(SQLiteDatabase db) {
        long now = System.currentTimeMillis();

        insertItem(db, 2, "索尼微单相机",
                "机身保存很好，带原装电池和 64G 存储卡，适合校园活动拍摄和日常练手。",
                "电子产品", 1850.0, "南湖校区图书馆门口", "九成新",
                "https://www.figma.com/api/mcp/asset/626d4bbc-dad3-48f7-93f2-0c766d17e98b",
                "相机, 摄影, 视频, 校园记录", "数字媒体艺术", 1, now - 2 * 60 * 60 * 1000L);

        insertItem(db, 1, "高等数学教材",
                "书页干净，重点题型有少量铅笔标记，适合大一新生复习和日常刷题。",
                "教材", 45.0, "教学楼 A 区自习室", "八成新",
                "https://www.figma.com/api/mcp/asset/201435ad-8875-4055-a8f7-58a9814ebca1",
                "教材, 高数, 考试复习, 计算机", "计算机科学", 1, now - 6 * 60 * 60 * 1000L);

        insertItem(db, 3, "校园骑行自行车",
                "刹车灵敏，轮胎刚换新，适合宿舍到教学楼短途通勤，周末骑行也没问题。",
                "校园出行", 280.0, "东院宿舍门口", "九成新",
                "https://www.figma.com/api/mcp/asset/67ee49d8-675d-48e6-8d88-159df272a26a",
                "骑行, 自行车, 通勤, 校园代步", "工业设计", 1, now - 10 * 60 * 60 * 1000L);

        insertItem(db, 1, "14 寸轻薄笔记本",
                "日常办公和课程作业完全够用，电池健康良好，屏幕无明显划痕。",
                "电子产品", 1200.0, "南湖校区食堂门口", "九成新",
                "https://www.figma.com/api/mcp/asset/cf4004dc-4c2f-40b6-b6fb-190a61d62cb2",
                "笔记本, 学习, 轻办公, 电子产品", "计算机科学", 0, now - 12 * 60 * 60 * 1000L);

        insertItem(db, 3, "宿舍空气净化器",
                "夜间运行声音很小，滤芯刚换过，适合花粉敏感或者经常开空调的宿舍。",
                "宿舍用品", 150.0, "西院 6 栋楼下", "八成新",
                "https://www.figma.com/api/mcp/asset/becfdfcf-0c4c-4799-a2eb-3acff5dc5753",
                "净化器, 宿舍, 生活电器, 空气质量", "环境设计", 1, now - 24 * 60 * 60 * 1000L);
    }

    private void seedConversations(SQLiteDatabase db) {
        long now = System.currentTimeMillis();

        long cameraConversation = insertConversation(
                db, 1, 1, 2,
                "我想问一下这台相机周末能在图书馆门口看机吗？",
                now - 45 * 60 * 1000L
        );
        insertMessage(db, cameraConversation, 1, "你好，这台相机还在吗？我想了解一下成色。", now - 110 * 60 * 60 * 1000L / 60);
        insertMessage(db, cameraConversation, 2, "还在的，机身和快门都很正常，周末可以现场看看。", now - 90 * 60 * 1000L);
        insertMessage(db, cameraConversation, 1, "我想问一下这台相机周末能在图书馆门口看机吗？", now - 45 * 60 * 1000L);

        long bikeConversation = insertConversation(
                db, 3, 1, 3,
                "可以，下午五点以后我都在东院宿舍门口。",
                now - 2 * 60 * 60 * 1000L
        );
        insertMessage(db, bikeConversation, 1, "自行车现在还可以试骑吗？", now - 3 * 60 * 60 * 1000L);
        insertMessage(db, bikeConversation, 3, "可以的，刹车和车座我都刚检查过。", now - 160 * 60 * 1000L);
        insertMessage(db, bikeConversation, 1, "那我今天傍晚过去看看，方便的话联系我一下。", now - 130 * 60 * 1000L);
        insertMessage(db, bikeConversation, 3, "可以，下午五点以后我都在东院宿舍门口。", now - 2 * 60 * 60 * 1000L);

        long macConversation = insertConversation(
                db, 4, 2, 1,
                "最低 1050 可以，当面验机没问题。",
                now - 4 * 60 * 60 * 1000L
        );
        insertMessage(db, macConversation, 2, "请问这台笔记本还能再优惠一点吗？", now - 5 * 60 * 60 * 1000L);
        insertMessage(db, macConversation, 1, "可以小刀一点，但希望校内面交。", now - 270 * 60 * 1000L);
        insertMessage(db, macConversation, 2, "最低 1050 可以，当面验机没问题。", now - 4 * 60 * 60 * 1000L);
    }

    private void seedPosts(SQLiteDatabase db) {
        long now = System.currentTimeMillis();

        insertPost(db, 1, "今天把闲置教材整理了一遍，准备再上新几本考研资料。", null, now - 3 * 60 * 60 * 1000L);
        insertPost(db, 2, "图书馆门口的面交体验挺不错，准备把键盘和显示器也挂出来。", null, now - 8 * 60 * 60 * 1000L);
        insertPost(db, 3, "最近宿舍换季，整理出不少生活用品，之后会慢慢发到平台上。", null, now - 15 * 60 * 60 * 1000L);
        insertPost(db, 1, "如果大家需要摄影器材，也欢迎私聊交流使用体验。", null, now - 24 * 60 * 60 * 1000L);
        insertPost(db, 2, "下周想做一波宿舍好物清单，有兴趣可以一起交换闲置。", null, now - 48 * 60 * 60 * 1000L);
    }

    private void insertUser(SQLiteDatabase db, String name, String username, String password,
                            String major, String interests, String bio) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("username", username);
        values.put("password", PasswordSecurity.hash(password));
        values.put("major", major);
        values.put("interests", interests);
        values.put("bio", bio);
        values.put("verified_at", System.currentTimeMillis());
        db.insert(TABLE_USERS, null, values);
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_items_active_created ON " +
                TABLE_ITEMS + "(is_active, created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_items_seller_active ON " +
                TABLE_ITEMS + "(seller_id, is_active)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_buyer_created ON " +
                TABLE_ORDERS + "(buyer_id, created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_seller_created ON " +
                TABLE_ORDERS + "(seller_id, created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_conversation_created ON " +
                TABLE_MESSAGES + "(conversation_id, created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_unread ON " +
                TABLE_MESSAGES + "(conversation_id, sender_id, is_read)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cart_user_added ON " +
                TABLE_CART_ITEMS + "(user_id, added_at)");
    }

    private void insertItem(SQLiteDatabase db, long sellerId, String title, String description, String category,
                            double price, String location, String condition, String imageUrl, String tags,
                            String preferredMajor, int campusPickupOnly, long createdAt) {
        ContentValues values = new ContentValues();
        values.put("seller_id", sellerId);
        values.put("title", title);
        values.put("description", description);
        values.put("category", category);
        values.put("price", price);
        values.put("location", location);
        values.put("item_condition", condition);
        values.put("image_url", imageUrl);
        values.put("tags", tags);
        values.put("preferred_major", preferredMajor);
        values.put("campus_pickup_only", campusPickupOnly);
        values.put("is_active", 1);
        values.put("created_at", createdAt);
        db.insert(TABLE_ITEMS, null, values);
    }

    private long insertConversation(SQLiteDatabase db, long itemId, long buyerId, long sellerId,
                                    String lastMessage, long lastMessageAt) {
        ContentValues values = new ContentValues();
        values.put("item_id", itemId);
        values.put("buyer_id", buyerId);
        values.put("seller_id", sellerId);
        values.put("last_message", lastMessage);
        values.put("last_message_at", lastMessageAt);
        return db.insert(TABLE_CONVERSATIONS, null, values);
    }

    private void insertMessage(SQLiteDatabase db, long conversationId, long senderId, String content, long createdAt) {
        ContentValues values = new ContentValues();
        values.put("conversation_id", conversationId);
        values.put("sender_id", senderId);
        values.put("content", content);
        values.put("is_read", 0);
        values.put("created_at", createdAt);
        db.insert(TABLE_MESSAGES, null, values);
    }

    private void insertPost(SQLiteDatabase db, long userId, String content, String imageUrl, long createdAt) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("content", content);
        values.put("image_url", imageUrl);
        values.put("created_at", createdAt);
        db.insert(TABLE_POSTS, null, values);
    }

    private void addColumnIfMissing(SQLiteDatabase db, String tableName, String columnName, String columnDefinition) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        boolean exists = false;
        while (cursor.moveToNext()) {
            if (columnName.equalsIgnoreCase(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
                exists = true;
                break;
            }
        }
        cursor.close();

        if (!exists) {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }
}

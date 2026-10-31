package com.example.dinepulse.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.dinepulse.models.MenuItem;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "dinepulse.db";
    private static final int DATABASE_VERSION = 2; // Upgraded version for Users table

    // Menu Table
    private static final String TABLE_MENU = "menu_items";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_PRICE = "price";
    private static final String COLUMN_DESC = "description";
    private static final String COLUMN_STOCK = "in_stock";
    private static final String COLUMN_SIGNATURE = "is_signature";

    // Users Table
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_ROLE = "role"; // "Guest" or "Admin"

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Menu Table
        String CREATE_MENU_TABLE = "CREATE TABLE " + TABLE_MENU + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY, " + COLUMN_NAME + " TEXT, "
                + COLUMN_CATEGORY + " TEXT, " + COLUMN_PRICE + " REAL, "
                + COLUMN_DESC + " TEXT, " + COLUMN_STOCK + " INTEGER, "
                + COLUMN_SIGNATURE + " INTEGER)";
        db.execSQL(CREATE_MENU_TABLE);

        // Create Users Table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USERNAME + " TEXT UNIQUE, " + COL_PASSWORD + " TEXT, "
                + COL_ROLE + " TEXT)";
        db.execSQL(CREATE_USERS_TABLE);

        // Seed Initial Menu Data
        db.execSQL("INSERT INTO " + TABLE_MENU + " VALUES (1, 'Don Julio Margarita', 'Cocktails', 22.0, 'Blanco, lime, agave', 1, 1)");
        db.execSQL("INSERT INTO " + TABLE_MENU + " VALUES (2, 'Smoked Old Fashioned', 'Cocktails', 19.0, 'Bourbon, bitters, smoke', 1, 0)");
        db.execSQL("INSERT INTO " + TABLE_MENU + " VALUES (3, 'Truffle Fries', 'Sides', 14.0, 'Parmesan, truffle oil', 1, 0)");

        // Seed a default Admin account
        db.execSQL("INSERT INTO " + TABLE_USERS + " (username, password, role) VALUES ('admin', 'admin123', 'Admin')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MENU);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // --- USER AUTHENTICATION METHODS ---

    // 1. Register a new user
    public boolean registerUser(String username, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        values.put(COL_ROLE, role);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // 2. Check Login Credentials and Return Role
    public String authenticateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ROLE + " FROM " + TABLE_USERS + " WHERE " +
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?", new String[]{username, password});

        if (cursor.moveToFirst()) {
            String role = cursor.getString(0);
            cursor.close();
            return role;
        }
        cursor.close();
        return null;
    }

    // --- MENU METHODS ---
    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> menuList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MENU, null);

        if (cursor.moveToFirst()) {
            do {
                MenuItem item = new MenuItem();
                item.setId(String.valueOf(cursor.getInt(0)));  // id
                item.setName(cursor.getString(1));             // name
                item.setCategory(cursor.getString(2));         // category
                item.setPrice(cursor.getDouble(3));            // price
                item.setDescription(cursor.getString(4));      // description
                item.setAvailable(cursor.getInt(5) == 1);      // available

                menuList.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return menuList;
    }

    public void updateStockInDB(int id, boolean inStock) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STOCK, inStock ? 1 : 0);
        db.update(TABLE_MENU, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
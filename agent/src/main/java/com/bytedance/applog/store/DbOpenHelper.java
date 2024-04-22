// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.util.Utils;

import java.util.HashMap;

/** 数据库工具类 */
class DbOpenHelper extends SQLiteOpenHelper {
    private final Engine mEngine;

    DbOpenHelper(
            final Engine mEngine,
            final String name,
            final SQLiteDatabase.CursorFactory factory,
            final int version) {
        super(mEngine.getContext(), name, factory, version);
        this.mEngine = mEngine;
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        try {
            db.beginTransaction();
            final HashMap<String, BaseData> ZYGOTES = BaseData.getAllBaseDataObj();
            for (BaseData data : ZYGOTES.values()) {
                String sql = data.createTable();
                if (sql != null) {
                    db.execSQL(sql);
                }
            }
            db.setTransactionSuccessful();
        } catch (Throwable t) {
            mEngine.getAppLog()
                    .getLogger()
                    .error(LogInfo.Category.DATABASE, "Create table failed", t);
        } finally {
            Utils.endDbTransactionSafely(db);
        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        mEngine.getAppLog()
                .getLogger()
                .debug(
                        LogInfo.Category.DATABASE,
                        "Database upgrade from:{} to:{}",
                        oldVersion,
                        newVersion);
        try {
            db.beginTransaction();
            final HashMap<String, BaseData> ZYGOTES = BaseData.getAllBaseDataObj();
            for (BaseData data : ZYGOTES.values()) {
                db.execSQL("DROP TABLE IF EXISTS " + data.getTableName());
            }
            db.setTransactionSuccessful();
        } catch (Throwable t) {
            mEngine.getAppLog()
                    .getLogger()
                    .error(LogInfo.Category.DATABASE, "drop tables failed when upgrade.", t);
        } finally {
            Utils.endDbTransactionSafely(db);
        }
        onCreate(db);
    }

    @Override
    public void onDowngrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

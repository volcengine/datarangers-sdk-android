// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.monitor.model.ExceptionTrace;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import java.util.HashMap;

/** 数据库工具类 */
class DbOpenHelper extends SQLiteOpenHelper {
    private final Engine mEngine;

    DbOpenHelper(
            @Nullable final Engine mEngine,
            @Nullable final String name,
            @Nullable final SQLiteDatabase.CursorFactory factory,
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
            TLog.ysnp(t);
        } finally {
            Utils.endDbTransactionSafely(db);
        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        TLog.i("onUpgrade" + ", " + oldVersion + ", " + newVersion);
        try {
            db.beginTransaction();
            final HashMap<String, BaseData> ZYGOTES = BaseData.getAllBaseDataObj();
            for (BaseData data : ZYGOTES.values()) {
                db.execSQL("DROP TABLE IF EXISTS " + data.getTableName());
            }
            db.setTransactionSuccessful();
        } catch (Throwable t) {
            TLog.e("drop tables failed when upgrade.", t);
        } finally {
            Utils.endDbTransactionSafely(db);
        }
        onCreate(db);
    }

    @Override
    public void onDowngrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * 监控DB错误
     *
     * @param e Throwable
     */
    public void traceDbError(Throwable e) {
        if (null == mEngine.getMonitor()) {
            return;
        }
        mEngine.getMonitor().trace(new ExceptionTrace("db_exception", e));
    }
}

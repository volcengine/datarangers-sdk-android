// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.test.applog.demo

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.applog.AppLog
import com.bytedance.applog.demo.R
import org.json.JSONObject
import java.util.UUID

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.setUuid).setOnClickListener {
            AppLog.setUserUniqueID(UUID.randomUUID().toString())
        }

        findViewById<Button>(R.id.event).setOnClickListener {
            val jsonObject = JSONObject()
            jsonObject.put("key", "value")
            AppLog.onEventV3("test_event", jsonObject)
        }
    }
}
// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import com.bytedance.applog.game.GameReportHelper;

import org.junit.*;
@Ignore
public class GameReportHelperTest extends BaseAppLogTest {

    @Test
    public void onEventRegister() {
        GameReportHelper.onEventRegister("weixin", true);
        UnitTestUtils.assertEventV3Count("register", 1);
    }

    @Test
    public void onEventLogin() {
        GameReportHelper.onEventLogin("weixin", false);
        GameReportHelper.onEventLogin("weixin", false);
        UnitTestUtils.assertEventV3Count("log_in", 2);
    }

    @Test
    public void onEventPurchase() {
        GameReportHelper.onEventPurchase("coin", "money", "2123", 11, "channel", "rmb", true, 100);
        UnitTestUtils.assertEventV3Count("purchase", 1);
    }

    @Test
    public void onEventAccessAccount() {
        GameReportHelper.onEventAccessAccount("weixin", true);
        UnitTestUtils.assertEventV3Count("access_account", 1);
    }

    @Test
    public void onEventQuest() {
        GameReportHelper.onEventQuest("id", "任务", "名字", 100, true, "sdf");
        UnitTestUtils.assertEventV3Count("quest", 1);
    }

    @Test
    public void onEventUpdateLevel() {
        GameReportHelper.onEventUpdateLevel(1);
        UnitTestUtils.assertEventV3Count("update_level", 1);
    }

    @Test
    public void onEventCreateGameRole() {
        GameReportHelper.onEventCreateGameRole("9527");
        UnitTestUtils.assertEventV3Count("create_gamerole", 1);
    }

    @Test
    public void onEventCheckOut() {
        GameReportHelper.onEventCheckOut("coin", "money", "2123", 11, false, "channel", "rmb", true, 100);
        UnitTestUtils.assertEventV3Count("check_out", 1);
    }

    @Test
    public void onEventAddToFavorite() {
        GameReportHelper.onEventAddToFavorite("coin", "money", "2123", 11, true);
        UnitTestUtils.assertEventV3Count("add_to_favourite", 1);
    }

    @Test
    public void onEventAccessPaymentChannel() {
        GameReportHelper.onEventAccessPaymentChannel("channel", true);
        UnitTestUtils.assertEventV3Count("access_payment_channel", 1);
    }

    @Test
    public void onEventAddCart() {
        GameReportHelper.onEventAddCart("coin", "money", "2123", 11, true);
        UnitTestUtils.assertEventV3Count("add_cart", 1);
    }

    @Test
    public void onEventViewContent() {
        GameReportHelper.onEventViewContent("coin", "money", "2123");
        UnitTestUtils.assertEventV3Count("view_content", 1);
    }
}
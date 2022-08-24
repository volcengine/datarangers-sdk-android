// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import com.bytedance.applog.game.WhalerGameHelper;

import org.junit.*;

public class WhalerGameHelperTest extends BaseAppLogTest {

    @Test
    public void test() {
        WhalerGameHelper.adButtonClick("adType", "adPositionType", "adPosition", null);
        WhalerGameHelper.adShow("adType", "adPositionType", "adPosition", null);
        WhalerGameHelper.adShowEnd("adType", "adPositionType", "adPosition", "result", null);
        WhalerGameHelper.costCoins("coinType", "method", 100, null);
        WhalerGameHelper.getCoins("coinType", "method", 100, null);
        WhalerGameHelper.startPlay("name", null);
        WhalerGameHelper.endPlay("name", WhalerGameHelper.Result.SUCCESS, 1000, null);
        WhalerGameHelper.gameInitInfo(100, "coinType", 100, null);
        WhalerGameHelper.levelUp(100, 109, "method", 101, null);
        WhalerGameHelper.purchase("coinType", "name", "id", 100, "channel", "rmb", "sdf", 100, null);
    }
}
// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.game;

import android.text.TextUtils;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.log.LoggerImpl;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author wzj
 *     <p>TODO: 支持多实例
 */
public class WhalerGameHelper {

    public static final String GT_AD_BUTTON_CLICK = "gt_ad_button_click";

    public static final String GT_AD_SHOW = "gt_ad_show";

    public static final String GT_AD_SHOW_END = "gt_ad_show_end";

    public static final String GT_LEVELUP = "gt_levelup";

    public static final String GT_START_PLAY = "gt_start_play";

    public static final String GT_END_PLAY = "gt_end_play";

    public static final String PURCHASE = "purchase";

    public static final String GT_INIT_INFO = "gt_init_info";

    public static final String GT_GET_COINS = "gt_get_coins";

    public static final String GT_COST_COINS = "gt_cost_coins";

    private static void fillOtherParams(HashMap<String, Object> otherParams, JSONObject param)
            throws JSONException {
        if (otherParams != null && !otherParams.isEmpty()) {
            Set<Map.Entry<String, Object>> entries = otherParams.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                if (!TextUtils.isEmpty(entry.getKey())) {
                    param.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * sdk预置埋点 广告位按钮点击
     *
     * @param adType 广告类型：激励视频、插屏、banner等，直接使用汉字或者英文进行标识
     * @param adPositionType 广告点位类型：按照提供分类接入
     * @param adPosition 广告点位：复活、翻倍、试用、buff、奖励道具、新道具、减CD等，直接使用文字或者英文进行标识
     */
    public static void adButtonClick(
            String adType,
            String adPositionType,
            String adPosition,
            HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("ad_type", adType);
            param.put("ad_position_type", adPositionType);
            param.put("ad_position", adPosition);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(GT_AD_BUTTON_CLICK, param, AppLogInstance.BUSINESS_EVENT);
    }

    /**
     * sdk预置埋点 广告展示
     *
     * @param adType 广告类型：激励视频、插屏、banner等，直接使用汉字或者英文进行标识
     * @param adPositionType 广告点位类型：按照提供分类接入
     * @param adPosition 广告点位：复活、翻倍、试用、buff、奖励道具、新道具、减CD等，直接使用文字或者英文进行标识
     */
    public static void adShow(
            String adType,
            String adPositionType,
            String adPosition,
            HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("ad_type", adType);
            param.put("ad_position_type", adPositionType);
            param.put("ad_position", adPosition);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(GT_AD_SHOW, param, AppLogInstance.BUSINESS_EVENT);
    }

    /**
     * sdk预置埋点 广告展示完成
     *
     * @param adType 广告类型：激励视频、插屏、banner等，直接使用汉字或者英文进行标识
     * @param adPositionType 广告点位类型：按照提供分类接入
     * @param adPosition 广告点位：复活、翻倍、试用、buff、奖励道具、新道具、减CD等，直接使用文字或者英文进行标识
     * @param result 广告观看结果：跳过、成功、失败等，使用英文进行标识. 跳过标记为skip, 成功标记为success，失败为fail
     */
    public static void adShowEnd(
            String adType,
            String adPositionType,
            String adPosition,
            String result,
            HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("ad_type", adType);
            param.put("ad_position_type", adPositionType);
            param.put("ad_position", adPosition);
            param.put("result", result);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(GT_AD_SHOW_END, param, AppLogInstance.BUSINESS_EVENT);
    }

    /**
     * sdk预置埋点 升级和经验（总等级）
     *
     * @param lev 当前玩家等级
     * @param getExp 获得经验
     * @param method 获得经验途径：闯关成功、引导完成、领取奖励等，使用汉字或者英文进行标识
     * @param aflev 用户获得经验后等级，如获得经验未导致升级，则lev=aflev，如导致升级，则lev<aflev
     */
    public static void levelUp(
            int lev, int getExp, String method, int aflev, HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("get_exp", getExp);
            param.put("method", method);
            param.put("aflev", aflev);
            param.put("lev", lev);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(GT_LEVELUP, param, AppLogInstance.BUSINESS_EVENT);
    }

    /**
     * sdk预置埋点 开始玩法
     *
     * @param ectypeName 针对闯关性质玩法，标注关卡名称。如无特定名称，可填ectype_id 关卡ID数字
     */
    public static void startPlay(String ectypeName, HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("ectype_name", ectypeName);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(GT_START_PLAY, param, AppLogInstance.BUSINESS_EVENT);
    }

    public enum Result {
        /** 游戏的结果的枚举,未完成,成功,失败 */
        UNCOMPLETED("uncompleted"),
        SUCCESS("success"),
        FAIL("fail");

        final String gameResult;

        Result(String result) {
            this.gameResult = result;
        }
    }

    /**
     * sdk预置埋点 结束玩法
     *
     * @param ectypeName 针对闯关性质玩法，标注关卡名称。如无特定名称，可填ectype_id 关卡ID数字
     * @param result 玩法的结果：未完成、成功、失败等，使用英文进行标识. 未完成标记为uncompleted, 成功标记为success，失败为fail
     * @param duration 消耗时间，单位秒
     */
    public static void endPlay(
            String ectypeName, Result result, int duration, HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("ectype_name", ectypeName);
            param.put("result", result.gameResult);
            param.put("duration", duration);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(GT_END_PLAY, param, AppLogInstance.BUSINESS_EVENT);
    }

    /**
     * sdk预置埋点 app内购充值
     *
     * @param contentType 内购充值内容类型
     * @param contentName 内购充值内容名称
     * @param contentId 内购充值内容id
     * @param contentNum 内购充值内容的数量
     * @param paymentChannel 支付渠道：例如 支付宝，微信等
     * @param currency 支付货币类型
     * @param isSuccess 支付是否成功
     * @param currencyAmount 支付的金额，单位元
     */
    public static void purchase(
            String contentType,
            String contentName,
            String contentId,
            int contentNum,
            String paymentChannel,
            String currency,
            String isSuccess,
            int currencyAmount,
            HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("content_type", contentType);
            param.put("content_name", contentName);
            param.put("content_num", contentNum);
            param.put("content_id", contentId);
            param.put("payment_channel", paymentChannel);
            param.put("currency", currency);
            param.put("is_success", isSuccess);
            param.put("currency_amount", currencyAmount);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(PURCHASE, param, AppLogInstance.BUSINESS_EVENT);
    }

    /**
     * sdk预置埋点 初始化信息
     *
     * @param lev 当前玩家等级
     * @param coinType 获得货币的类型
     * @param coinLeft 用户身上剩余的货币数量
     * @param otherParams 其他参数
     */
    public static void gameInitInfo(
            int lev, String coinType, int coinLeft, HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("coin_type", coinType);
            param.put("coin_left", coinLeft);
            param.put("lev", lev);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(GT_INIT_INFO, param, AppLogInstance.BUSINESS_EVENT);
    }

    /**
     * @param coinType 货币类型：元宝、绑元、金币、银币等，使用文字或者英文进行标识
     * @param method 获得途径：复活、购买道具、解锁关卡等，使用文字或者英文进行标识
     * @param coinNum 获得数量
     */
    public static void getCoins(
            String coinType, String method, int coinNum, HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("coin_type", coinType);
            param.put("method", method);
            param.put("coin_num", coinNum);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(GT_GET_COINS, param, AppLogInstance.BUSINESS_EVENT);
    }

    /**
     * @param coinType 货币类型：元宝、绑元、金币、银币等，使用文字或者英文进行标识
     * @param method 消耗途径：复活、购买道具、解锁关卡等，使用文字或者英文进行标识
     * @param coinNum 消耗数量
     */
    public static void costCoins(
            String coinType, String method, int coinNum, HashMap<String, Object> otherParams) {
        JSONObject param = new JSONObject();
        try {
            fillOtherParams(otherParams, param);
            param.put("coin_type", coinType);
            param.put("method", method);
            param.put("coin_num", coinNum);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WhalerGameHelper"), "JSON handle failed", e);
        }
        AppLog.onEventV3(GT_COST_COINS, param, AppLogInstance.BUSINESS_EVENT);
    }
}

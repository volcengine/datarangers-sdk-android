// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.game;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author linguoqing
 * @date 2019/6/02
 *     <p>TODO: 支持多实例
 */
public final class GameReportHelper {
    public static final String REGISTER = "register";
    public static final String LOG_IN = "log_in";
    public static final String PURCHASE = "purchase";
    public static final String ACCESS_ACCOUNT = "access_account";
    public static final String QUEST = "quest";
    public static final String UPDATE_LEVEL = "update_level";
    public static final String CREATE_GAMEROLE = "create_gamerole";
    public static final String CHECK_OUT = "check_out";
    public static final String ADD_TO_FAVORITE = "add_to_favourite";
    public static final String ACCESS_PAYMENT_CHANNEL = "access_payment_channel";
    public static final String ADD_CART = "add_cart";
    public static final String VIEW_CONTENT = "view_content";

    /**
     * SDK预置游戏埋点 注册
     *
     * @param method method=[default,weixin,qq...] 注册方式
     * @param isSuccess 状态string yes or no
     */
    public static void onEventRegister(String method, boolean isSuccess) {
        try {
            JSONObject object = new JSONObject();
            object.put("method", method);
            object.put("is_success", Utils.getYesNoString(isSuccess));
            AppLog.onEventV3(REGISTER, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 登录
     *
     * @param method method=[default,weixin,qq...] 登录方式
     * @param isSuccess 状态string yes or no
     */
    public static void onEventLogin(String method, boolean isSuccess) {
        try {
            JSONObject object = new JSONObject();
            object.put("method", method);
            object.put("is_success", Utils.getYesNoString(isSuccess));
            AppLog.onEventV3(LOG_IN, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 支付
     *
     * @param contentType 内容类型
     * @param contentName 内容名称
     * @param contentId 内容id
     * @param contentNumber 内容数量
     * @param paymentChannel 支付渠道
     * @param currency 币种
     * @param isSuccess 状态string yes or no
     * @param currencyAmount 金额，不能为0
     */
    public static void onEventPurchase(
            String contentType,
            String contentName,
            String contentId,
            int contentNumber,
            String paymentChannel,
            String currency,
            boolean isSuccess,
            int currencyAmount) {
        try {
            JSONObject object = new JSONObject();
            object.put("content_type", contentType);
            object.put("content_name", contentName);
            object.put("content_id", contentId);
            object.put("content_num", contentNumber);
            object.put("payment_channel", paymentChannel);
            object.put("currency", currency);
            object.put("is_success", Utils.getYesNoString(isSuccess));
            object.put("currency_amount", currencyAmount);
            AppLog.onEventV3(PURCHASE, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 绑定社交账号
     *
     * @param accountType 账号类型
     * @param isSuccess 状态string yes or no
     */
    public static void onEventAccessAccount(String accountType, boolean isSuccess) {
        try {
            JSONObject object = new JSONObject();
            object.put("account_type", accountType);
            object.put("is_success", Utils.getYesNoString(isSuccess));
            AppLog.onEventV3(ACCESS_ACCOUNT, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 进行任务
     *
     * @param questId 任务id
     * @param questType 任务类型
     * @param questName 教学/任务/副本名
     * @param questNo 第几个任务
     * @param isSuccess 状态string yes or no
     * @param description 其他描述
     */
    public static void onEventQuest(
            String questId,
            String questType,
            String questName,
            int questNo,
            boolean isSuccess,
            String description) {
        try {
            JSONObject object = new JSONObject();
            object.put("quest_id", questId);
            object.put("quest_type", questType);
            object.put("quest_name", questName);
            object.put("quest_no", questNo);
            object.put("is_success", Utils.getYesNoString(isSuccess));
            object.put("description", description);
            AppLog.onEventV3(QUEST, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 升级
     *
     * @param level 当前等级
     */
    public static void onEventUpdateLevel(int level) {
        try {
            JSONObject object = new JSONObject();
            object.put("level", level);
            AppLog.onEventV3(UPDATE_LEVEL, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 创建角色
     *
     * @param gameRoleId 当前等级
     */
    public static void onEventCreateGameRole(String gameRoleId) {
        try {
            JSONObject object = new JSONObject();
            object.put("gamerole_id", gameRoleId);
            AppLog.onEventV3(CREATE_GAMEROLE, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 提交购买/下单
     *
     * @param contentType 内容类型
     * @param contentName 商品/内容名
     * @param contentId 商品ID/内容ID
     * @param contentNumber 商品数量
     * @param isVirtualCurrency 是否使用的是虚拟货币
     * @param virtualCurrency 虚拟币币种
     * @param currency 真实货币类型
     * @param isSuccess 是否成功 yes or no
     * @param currencyAmount 货币金额
     */
    public static void onEventCheckOut(
            String contentType,
            String contentName,
            String contentId,
            int contentNumber,
            boolean isVirtualCurrency,
            String virtualCurrency,
            String currency,
            boolean isSuccess,
            int currencyAmount) {
        try {
            JSONObject object = new JSONObject();
            object.put("content_type", contentType);
            object.put("content_name", contentName);
            object.put("content_id", contentId);
            object.put("content_num", contentNumber);
            object.put("is_virtual_currency", Utils.getYesNoString(isVirtualCurrency));
            object.put("virtual_currency", virtualCurrency);
            object.put("currency", currency);
            object.put("is_success", Utils.getYesNoString(isSuccess));
            object.put("currency_amount", currencyAmount);
            AppLog.onEventV3(CHECK_OUT, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 添加至收藏
     *
     * @param contentType 内容类型
     * @param contentName 内容名
     * @param contentId 内容ID
     * @param contentNumber 内容数量
     * @param isSuccess 是否成功 yes or no
     */
    public static void onEventAddToFavorite(
            String contentType,
            String contentName,
            String contentId,
            int contentNumber,
            boolean isSuccess) {
        try {
            JSONObject object = new JSONObject();
            object.put("content_type", contentType);
            object.put("content_name", contentName);
            object.put("content_id", contentId);
            object.put("content_num", contentNumber);
            object.put("is_success", Utils.getYesNoString(isSuccess));
            AppLog.onEventV3(ADD_TO_FAVORITE, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 添加支付渠道
     *
     * @param paymentChannel 支付渠道
     * @param isSuccess 是否成功 yes or no
     */
    public static void onEventAccessPaymentChannel(String paymentChannel, boolean isSuccess) {
        try {
            JSONObject object = new JSONObject();
            object.put("payment_channel", paymentChannel);
            object.put("is_success", Utils.getYesNoString(isSuccess));
            AppLog.onEventV3(ACCESS_PAYMENT_CHANNEL, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 加入购买/购物车
     *
     * @param contentType 内容类型
     * @param contentName 内容名
     * @param contentId 内容ID
     * @param contentNumber 内容数量
     * @param isSuccess 是否成功 yes or no
     */
    public static void onEventAddCart(
            String contentType,
            String contentName,
            String contentId,
            int contentNumber,
            boolean isSuccess) {
        try {
            JSONObject object = new JSONObject();
            object.put("content_type", contentType);
            object.put("content_name", contentName);
            object.put("content_id", contentId);
            object.put("content_num", contentNumber);
            object.put("is_success", Utils.getYesNoString(isSuccess));
            AppLog.onEventV3(ADD_CART, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * SDK预置游戏埋点 查看内容/商品详情
     *
     * @param contentType 内容类型
     * @param contentName 内容名
     * @param contentId 内容ID
     */
    public static void onEventViewContent(
            String contentType, String contentName, String contentId) {
        try {
            JSONObject object = new JSONObject();
            object.put("content_type", contentType);
            object.put("content_name", contentName);
            object.put("content_id", contentId);
            AppLog.onEventV3(VIEW_CONTENT, object, AppLogInstance.BUSINESS_EVENT);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }
}

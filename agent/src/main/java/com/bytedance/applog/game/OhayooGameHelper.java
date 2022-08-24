// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.game;

import android.text.TextUtils;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author: liujunlin
 * @date: 2021/1/8
 *     <p>TODO: 支持多实例
 */
public class OhayooGameHelper {
    public static final String GAME_TASK = "ohayoo_game_task";
    public static final String GAME_ACTIVITY = "ohayoo_game_activity";
    public static final String GAME_UNLOCK = "ohayoo_game_unlock";
    public static final String GAME_RANK = "ohayoo_game_rank";
    public static final String GAME_GUILD = "ohayoo_game_guild";
    public static final String GAME_SNS = "ohayoo_game_sns";
    public static final String GAME_SHARE = "ohayoo_game_share";
    public static final String GAME_BUTTONCLICK = "ohayoo_game_buttonclick";

    public static final String KEY_PACKAGE_CHANNEL = "ohayoo_packagechannel";
    public static final String KEY_ZONE_ID = "ohayoo_zoneid";
    public static final String KEY_SERVER_ID = "ohayoo_serverid";
    public static final String KEY_SDK_OPEN_ID = "ohayoo_sdk_open_id";
    public static final String KEY_USER_TYPE = "ohayoo_usertype";
    public static final String KEY_ROLE_ID = "ohayoo_roleid";
    public static final String KEY_LEVEL = "ohayoo_level";

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
     * 存在任务功能设计的情况下，该事件必打，用户在对任务有参与的情况下，该事件必打。游戏产品端如有搭建自有服务器，该事件建议走服务端日志。
     *
     * @param tasktype 任务类型
     * @param taskid 任务id
     * @param taskname 任务中文名
     * @param taskdesc 任务描述
     * @param taskresult 任务结果
     */
    public static void onEventGameTask(
            String tasktype,
            String taskid,
            String taskname,
            String taskdesc,
            int taskresult,
            HashMap<String, Object> otherParams) {
        try {
            JSONObject object = new JSONObject();
            object.put("tasktype", tasktype);
            object.put("taskid", taskid);
            object.put("taskname", taskname);
            object.put("taskdesc", taskdesc);
            object.put("taskresult", taskresult);
            fillOtherParams(otherParams, object);
            AppLog.onEventV3(GAME_TASK, object);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * 存在任务功能设计的情况下，该事件必打，用户在对任务有参与的情况下，该事件必打。游戏产品端如有搭建自有服务器，该事件建议走服务端日志。
     *
     * @param activitytype 活动类型
     * @param actid 活动id
     * @param actname 活动中文名
     * @param actdesc 活动描述
     * @param actresult 活动结果
     * @param actreward 活动奖励内容
     * @param starttime 活动开始时间戳（10位）
     * @param endtime 活动结束时间戳（10位）
     */
    public static void onEventGameActivity(
            String activitytype,
            String actid,
            String actname,
            String actdesc,
            int actresult,
            String actreward,
            long starttime,
            long endtime,
            HashMap<String, Object> otherParams) {
        try {
            JSONObject object = new JSONObject();
            object.put("activitytype", activitytype);
            object.put("actid", actid);
            object.put("actname", actname);
            object.put("actdesc", actdesc);
            object.put("actresult", actresult);
            object.put("actreward", actreward);
            object.put("starttime", starttime);
            object.put("endtime", endtime);
            fillOtherParams(otherParams, object);
            AppLog.onEventV3(GAME_ACTIVITY, object);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * 存在解锁功能设计的情况下，该事件必打，用户在对解锁有参与的情况下，打该事件。游戏产品端如有搭建自有服务器，该事件建议走服务端日志。
     *
     * @param unlocktype 解锁类型
     * @param unlockid 解锁内容id
     * @param unlockname 解锁中文名
     */
    public static void onEventGameUnlock(
            String unlocktype,
            String unlockid,
            String unlockname,
            HashMap<String, Object> otherParams) {
        try {
            JSONObject object = new JSONObject();
            object.put("unlocktype", unlocktype);
            object.put("unlockid", unlockid);
            object.put("unlockname", unlockname);
            fillOtherParams(otherParams, object);
            AppLog.onEventV3(GAME_UNLOCK, object);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * 存在排行榜功能设计的情况下，该事件必打，用户排名出现变化情况下，打该事件。游戏产品端如有搭建自有服务器，该事件建议走服务端日志。
     *
     * @param ranktype 排行榜类型
     * @param rankid 排行榜id
     * @param rank 个人排名
     * @param befrank 变化前排名
     * @param point 个人排名分值
     * @param befpoint 变化前分值
     * @param allpoint 排行榜总分值
     */
    public static void onEventGameRank(
            String ranktype,
            int rankid,
            int rank,
            int befrank,
            int point,
            int befpoint,
            int allpoint,
            HashMap<String, Object> otherParams) {
        try {
            JSONObject object = new JSONObject();
            object.put("ranktype", ranktype);
            object.put("rankid", rankid);
            object.put("rank", rank);
            object.put("befrank", befrank);
            object.put("point", point);
            object.put("befpoint", befpoint);
            object.put("allpoint", allpoint);
            fillOtherParams(otherParams, object);
            AppLog.onEventV3(GAME_RANK, object);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * 存在公会功能设计的情况下，该事件必打，用户在该功能上有参与的情况下，该事件必打。游戏产品端如有搭建自有服务器，该事件建议走服务端日志。
     *
     * @param membergrade 成员阶级
     * @param guildid 公会id
     * @param guildname 公会中文名称
     * @param guildlevel 公会等级
     * @param guildresult 公会状态
     * @param guildrank 公会排行
     */
    public static void onEventGameGuild(
            String membergrade,
            String guildid,
            String guildname,
            int guildlevel,
            int guildresult,
            int guildrank,
            HashMap<String, Object> otherParams) {
        try {
            JSONObject object = new JSONObject();
            object.put("membergrade", membergrade);
            object.put("guildid", guildid);
            object.put("guildname", guildname);
            object.put("guildlevel", guildlevel);
            object.put("guildresult", guildresult);
            object.put("guildrank", guildrank);
            fillOtherParams(otherParams, object);
            AppLog.onEventV3(GAME_GUILD, object);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * 存在sns功能设计的情况下，该事件必打，用户在该功能上有参与的情况下，该事件必打。游戏产品端如有搭建自有服务器，该事件建议走服务端日志。
     *
     * @param recnum 接收用户数量
     * @param count 发送/接受数量
     * @param snstype 社交类型
     * @param snssubtype 社交类型预留
     */
    public static void onEventGameSns(
            int recnum,
            int count,
            String snstype,
            String snssubtype,
            HashMap<String, Object> otherParams) {
        try {
            JSONObject object = new JSONObject();
            object.put("recnum", recnum);
            object.put("count", count);
            object.put("snstype", snstype);
            object.put("snssubtype", snssubtype);
            fillOtherParams(otherParams, object);
            AppLog.onEventV3(GAME_SNS, object);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * 存在分享功能设计的情况下，该事件必打，用户在该功能上有参与的情况下，该事件必打。游戏产品端如有搭建自有服务器，该事件建议走服务端日志。
     *
     * @param sharetype 分享类型
     * @param sharefocus 分享目标地址
     * @param shareresult 分享结果
     * @param shareid 分享内容id
     * @param shareidentify 分享唯一标识码
     */
    public static void onEventGameShare(
            String sharetype,
            String sharefocus,
            int shareresult,
            String shareid,
            String shareidentify,
            HashMap<String, Object> otherParams) {
        try {
            JSONObject object = new JSONObject();
            object.put("sharetype", sharetype);
            object.put("sharefocus", sharefocus);
            object.put("shareresult", shareresult);
            object.put("shareid", shareid);
            object.put("shareidentify", shareidentify);
            fillOtherParams(otherParams, object);
            AppLog.onEventV3(GAME_SHARE, object);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    /**
     * 需要记录用户在客户端各button上点击行为的情况下打该事件。
     *
     * @param buttontype 分享类型
     * @param buttonid 分享目标地址
     * @param buttonname 分享结果
     * @param buttonresult 分享内容id
     */
    public static void onEventGameButtonClick(
            String buttontype,
            String buttonid,
            String buttonname,
            int buttonresult,
            HashMap<String, Object> otherParams) {
        try {
            JSONObject object = new JSONObject();
            object.put("buttontype", buttontype);
            object.put("buttonid", buttonid);
            object.put("buttonname", buttonname);
            object.put("buttonresult", buttonresult);
            fillOtherParams(otherParams, object);
            AppLog.onEventV3(GAME_BUTTONCLICK, object);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
    }

    public static void setOhayooCustomHeader(String key, Object value) {
        AppLog.setHeaderInfo(key, value);
    }
}

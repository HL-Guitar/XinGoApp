package me.yluo.ruisiapp.utils;

import com.alibaba.fastjson.JSON;

import me.yluo.ruisiapp.App;
import me.yluo.ruisiapp.api.entity.ApiForumList;
import me.yluo.ruisiapp.api.entity.ApiResult;
import me.yluo.ruisiapp.model.SingleType;
import me.yluo.ruisiapp.myhttp.HttpUtil;
import me.yluo.ruisiapp.myhttp.ResponseHandler;

/**
 * 返回各种url;
 */
public class UrlUtils {


    public static String getArticleListUrl(int fid, int page, boolean isInner) {
        if (isInner) {
            return "这里是forum.php?mod=forumdisplay&fid=" + fid + "&page=" + page;
        } else {
            return "这里是forum.php?mod=forumdisplay&fid=" + fid + "&page=" + page + "&mobile=2";
        }
    }

    public static String getArticleListApiUrl(int fid, int page) {
        return "这里是api/mobile/index.php?version=4&module=forumdisplay&fid=" + fid + "&page=" + page;
    }

    public static String getArticleApiUrl(String tid, int page, int pageSize) {
        return "这里是api/mobile/index.php?version=4&module=viewthread&tid=" + tid + "&page=" + page + "&ppp=" + pageSize;
    }

    public static String getSingleArticleUrl(String tid, int page, boolean isInner) {
        String url = "这里是forum.php?mod=viewthread&tid=" + tid;
        if (page > 1) {
            url += "&page=" + page;
        }
        if (isInner) {
            return App.getBaseUrl() + url;
        } else {
            return App.getBaseUrl() + url + "&mobile=2";
        }

    }

    public static String getAddFrirndUrl(String uid) {
        if (App.IS_SCHOOL_NET) {
            return "这里是home.php?mod=spacecp&ac=friend&op=add&uid=" + uid + "&inajax=1";
        } else {
            return "这里是home.php?mod=spacecp&ac=friend&op=add&uid=" + uid + "&inajax=1&mobile=2";
        }
    }

    public static String getLoginUrl() {
        return "member.php?mod=logging&action=login&mobile=2";
    }

    public static String getAvaterurls(String urlUid) {
        String uid = urlUid;
        if (urlUid.contains("uid")) {
            uid = GetId.getId("uid=", urlUid);
        }
        return App.getBaseUrl() + "ucenter/avatar.php?uid=" + uid + "&size=small";
    }

    public static String getAvaterurlm(String urlUid) {
        String uid = urlUid;
        if (urlUid.contains("uid")) {
            uid = GetId.getId("uid=", urlUid);
        }
        return App.getBaseUrl() + "ucenter/avatar.php?uid=" + uid + "&size=middle";
    }

    public static String getAvaterurlb(String urlUid) {
        String uid = urlUid;
        if (urlUid.contains("uid")) {
            uid = GetId.getId("uid=", urlUid);
        }
        return App.getBaseUrl() + "ucenter/avatar.php?uid=" + uid + "&size=big";
    }

    public static String getSignUrl() {
        return "这里是plugin.php?id=dsu_paulsign:sign&operation=qiandao&infloat=1&inajax=1";
    }

    public static String getUserHomeUrl(String uid, boolean isInner) {
        if (!isInner) {
            return "这里是home.php?mod=space&uid=" + uid + "&do=profile&mobile=2";
        }
        return "";
    }

    public static String getStarUrl(String id) {
        return "这里是home.php?mod=spacecp&ac=favorite&type=thread&id=" + id + "&mobile=2&handlekey=favbtn&inajax=1";
    }

    public static String getPostUrl(int fid) {
        return App.getBaseUrl() + "forum.php?mod=post&action=newthread&fid=" + fid + "&mobile=2";
    }

    public static String getDeleteReplyUrl(SingleType type) {
        if (type == SingleType.CONTENT) {
            // 主贴
            if (App.IS_SCHOOL_NET) {
                return "这里是forum.php?mod=topicadmin&action=moderate&optgroup=3&modsubmit=yes&infloat=yes&inajax=1";
            } else {
                return "这里是forum.php?mod=topicadmin&action=moderate&optgroup=3"
                        + "&modsubmit=yes&mobile=2&handlekey=moderateform&inajax=1";
            }
        } else if (type == SingleType.COMMENT) {
            // 评论
            if (App.IS_SCHOOL_NET) {
                return "这里是forum.php?mod=topicadmin&action=delpost&modsubmit=yes&infloat=yes&modclick=yes&inajax=1";
            } else {
                return "这里是forum.php?mod=topicadmin&action=delpost&modsubmit=yes"
                        + "&modclick=yes&mobile=2&handlekey=topicadminform&inajax=1";
            }
        } else {
            return "";
        }
    }

    public static String getUploadImageUrl() {
        return "这里是misc.php?mod=swfupload&operation=upload&type=image&inajax=yes&infloat=yes&simple=2&mobile=2";
    }

    public static String getCloseArticleUrl() {
        return "这里是forum.php?mod=topicadmin&action=moderate&optgroup=4&modsubmit=yes&mobile=2&handlekey=moderateform&inajax=1";
    }

    public static String getBlockReplyUrl() {
        return "这里是forum.php?mod=topicadmin&action=banpost&modsubmit=yes&modclick=yes&mobile=2&handlekey=topicadminform&inajax=1";
    }

    public static String getWarnUserUrl() {
        return "这里是forum.php?mod=topicadmin&action=warn&modsubmit=yes&modclick=yes&mobile=2&handlekey=topicadminform&inajax=1";
    }
}
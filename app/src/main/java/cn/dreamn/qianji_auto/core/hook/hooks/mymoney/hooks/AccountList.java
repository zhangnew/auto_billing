package cn.dreamn.qianji_auto.core.hook.hooks.mymoney.hooks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashSet;
import java.util.List;

import cn.dreamn.qianji_auto.app.MyMoney;
import cn.dreamn.qianji_auto.core.broadcast.AppBroadcast;
import cn.dreamn.qianji_auto.core.hook.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class AccountList {
    public static void init(Utils utils) throws ClassNotFoundException {
        MyMoney myMoney = MyMoney.getInstance();
        String appName = myMoney.getAppName();
        String packageName = myMoney.getPackPageName();
        String activityName = myMoney.activityName;

        HashSet<String> accountList = new HashSet<>();
        final boolean[] hooked = {false};
        final boolean[] accountHooked = {false};
        final Activity[] activity = {null};

        ClassLoader classLoader =  utils.getClassLoader();
        Class<?> Activity = XposedHelpers.findClass(activityName, classLoader);
        Class<?> Cache = XposedHelpers.findClass(packageName + ".cache.AddTransDataCache$UpdateAccountBookCacheTask", classLoader);
        Class<?> AddTransDataCache = XposedHelpers.findClass(packageName + ".cache.AddTransDataCache", classLoader);

        // wBb = AccountListVo
        XposedHelpers.findAndHookMethod(AddTransDataCache, "b", "wBb", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                if (accountHooked[0]) return;
                accountHooked[0] = true;
                Object obj = param.args[0];
                // accountListVo.h 包含隐藏账户
                List<Object> accountListVo = (List<Object>) XposedHelpers.getObjectField(obj, "g");
                for (Object o : accountListVo) {
                    String accountName = (String) XposedHelpers.getObjectField(o, "c");
                    long accountId = XposedHelpers.getLongField(o, "b");
                    double accountMoney1 = XposedHelpers.getDoubleField(o, "j");
                    double accountMoney2 = XposedHelpers.getDoubleField(o, "h");
                    double accountMoney = Math.max(accountMoney1, accountMoney2);
//                    String d = (String) XposedHelpers.getObjectField(o, "d"); // 货币种类: CNY, USD
//                    String e = (String) XposedHelpers.getObjectField(o, "e"); // 同上
//                    String k = (String) XposedHelpers.getObjectField(o, "k"); // 备注
//                    String m = (String) XposedHelpers.getObjectField(o, "m"); // 图标名称
                    utils.log("accountName=" + accountName + ", accountId=" + accountId + ", accountMoney=" + accountMoney);
                    String account = accountId + "," + accountName + "," + accountMoney;
                    accountList.add(account);
                }
            }
        });

        // afterHookedMethod 已经加载完成各种数据
        XposedHelpers.findAndHookMethod(Cache, "o", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param){
                utils.log(appName + " hooked=" + hooked[0] + ", activity=" + activity[0]);
                if (hooked[0]) return;
                hooked[0] = true;
                if (activity[0] == null) return; // 直接打开 App 而不是通过自动记账打开的，不处理
                Intent intent = (Intent) XposedHelpers.callMethod(activity[0], "getIntent");
                utils.log("intent=" + intent);
                if (intent == null) return;

                int AutoSignal = intent.getIntExtra("AutoSignal", AppBroadcast.BROADCAST_NOTHING);
                if (AutoSignal == AppBroadcast.BROADCAST_ASYNC) {
                    utils.log(appName + "收到同步信号:开始提取数据");
                    JSONObject jsonObject = new JSONObject();
                    JSONArray userBooks = new JSONArray();
                    JSONArray categorys = new JSONArray();
                    JSONArray asset = new JSONArray();
                    accountList.forEach(account -> {
                        String[] accounts = account.split(",");
                        asset.add(new JSONObject() {{
                            put("id", accounts[0]);
                            put("name", accounts[1]);
//                            put("money", accounts[2]);
//                            put("icon", "https://pic.dreamn.cn/uPic/2021032022075916162492791616249279427UY2ok6支付.png");
//                            put("icon", "");
//                            put("sort", 0);
                        }});
                    });
                    jsonObject.put("userBook", userBooks);
                    jsonObject.put("asset", asset);
                    // TODO 读取分类数据
                    jsonObject.put("category", categorys);
                    jsonObject.put("AutoSignal", AutoSignal);
                    utils.send2auto(jsonObject.toJSONString());

                    // FIXME NPE Can't toast on a thread that has not called Looper.prepare()
                    Toast.makeText(utils.getContext(), appName + "数据信息获取完毕，现在返回自动记账。", Toast.LENGTH_LONG).show();
                    XposedHelpers.callMethod(activity[0], "finishAndRemoveTask");
                }
            }
        });

        XposedHelpers.findAndHookMethod(Activity, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                utils.log(appName + " hooked=" + hooked[0]);
                if (hooked[0]) return;
                activity[0] = (Activity) param.thisObject;
            }
        });
    }
}

package cn.dreamn.qianji_auto.core.hook.hooks.mymoney.hooks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashSet;

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
        final Activity[] activity = {null};

        ClassLoader classLoader =  utils.getClassLoader();
        Class<?> AccountVo = XposedHelpers.findClass(packageName + ".book.db.model.AccountVo", classLoader);
        Class<?> Activity = XposedHelpers.findClass(activityName, classLoader);
        Class<?> Cache = XposedHelpers.findClass(packageName + ".cache.AddTransDataCache$UpdateAccountBookCacheTask", classLoader);
        XposedHelpers.findAndHookMethod(AccountVo, "b", double.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (hooked[0]) return;
                Double accountMoney = (Double) param.args[0];
                String accountName = (String) AccountVo.getDeclaredField("c").get(param.thisObject);
                long accountId = (long) AccountVo.getDeclaredField("b").get(param.thisObject);
                double accountMoney2 = (double) AccountVo.getDeclaredField("h").get(param.thisObject);
                if (accountMoney < accountMoney2) {
                    accountMoney = accountMoney2;
                }
//                utils.log("accountMoney=" + accountMoney + ", accountId=" + accountId + ", accountName=" + accountName);
                String account = accountId + "," + accountName + "," + accountMoney;
                accountList.add(account);
            }
        });

        XposedHelpers.findAndHookMethod(Cache, "o", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param){
                if (hooked[0]) return;
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

                    Toast.makeText(utils.getContext(), appName + "数据信息获取完毕，现在返回自动记账。", Toast.LENGTH_LONG).show();
                    XposedHelpers.callMethod(activity[0], "finishAndRemoveTask");
                }
                hooked[0] = true;
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

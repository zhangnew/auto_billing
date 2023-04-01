package cn.dreamn.qianji_auto.app;

import static cn.dreamn.qianji_auto.core.broadcast.AppBroadcast.BROADCAST_ASYNC;
import static cn.dreamn.qianji_auto.core.broadcast.AppBroadcast.BROADCAST_GET_REI;
import static cn.dreamn.qianji_auto.core.broadcast.AppBroadcast.BROADCAST_GET_YEAR;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hjq.toast.ToastUtils;
import com.tencent.mmkv.MMKV;

import java.nio.charset.StandardCharsets;

import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.bills.BillInfo;
import cn.dreamn.qianji_auto.data.database.Db;
import cn.dreamn.qianji_auto.data.database.Table.Category;
import cn.dreamn.qianji_auto.setting.AppStatus;
import cn.dreamn.qianji_auto.ui.utils.HandlerUtil;
import cn.dreamn.qianji_auto.utils.runUtils.Log;
import cn.dreamn.qianji_auto.utils.runUtils.RootUtils;
import cn.dreamn.qianji_auto.utils.runUtils.TaskThread;
import cn.dreamn.qianji_auto.utils.runUtils.Tool;

public class QianJi implements IApp {
    private static QianJi qianJi;
    static long time = 0;
    public static QianJi getInstance(){
        if(qianJi==null)
            qianJi=new QianJi();
        return qianJi;
    }
    @Override
    public String getPackPageName() {
        return "com.mutangtech.qianji";
    }

    @Override
    public String getAppName() {
        return "钱迹";
    }



    @Override
    public int getAppIcon() {
        return R.drawable.logo_qianji;
    }

    @Override
    public void sendToApp(Context context,BillInfo billInfo) {

        Handler mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    time = System.currentTimeMillis();
                    if (RootUtils.hasRootPermission()) {
                        RootUtils.exec(new String[]{"am start \"" + getQianJi(billInfo) + "\""});
                    } else {
                        Tool.goUrl(context, getQianJi(billInfo));
                    }

                    //TODO 4.0新增：多币种记账支持，此处预留修改位。
                    ToastUtils.show(String.format(context.getString(R.string.book_success), billInfo.getMoney()));
                }
            }
        };
        if (AppStatus.isXposed()) {
            mHandler.sendEmptyMessage(0);
        } else {
            delay(mHandler);
        }


    }

    private void delay(Handler handler) {
        long m = System.currentTimeMillis() - time;
        if (m < 3000) {
            ToastUtils.show("稍后为您记账！");
            TaskThread.onMain(m, new Runnable() {
                @Override
                public void run() {
                    delay(handler);
                }
            });
        } else {
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public void asyncDataBefore(Context context, int type) {
        if (AppStatus.isXposed()) {
            Log.i("自动记账同步", "同步开始");

            RootUtils.exec(new String[]{"am force-stop com.mutangtech.qianji"});
            //杀死其他应用
            //  Tool.stopApp(context,"com.mutangtech.qianji");
            Intent intent = new Intent();
            intent.setClassName("com.mutangtech.qianji", "com.mutangtech.qianji.ui.main.MainActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("AutoSignal", type);
            MMKV mmkv = MMKV.defaultMMKV();
            mmkv.encode("AutoSignal", type);
            Log.i("自动记账同步", "正在前往钱迹");
            context.startActivity(intent);
        } else {
            ToastUtils.show(String.format(context.getResources().getString(R.string.not_support), getAppName()));
        }
    }



    private void doRei(Context context, Bundle extData) {
        RootUtils.exec(new String[]{"am force-stop com.mutangtech.qianji"});
        Intent intent = new Intent("net.ankio.auto.QIANJI_REI");
        intent.putExtras(extData);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void doYear(Context context, Bundle extData) {
        RootUtils.exec(new String[]{"am force-stop com.mutangtech.qianji"});
        Intent intent = new Intent("net.ankio.auto.QIANJI_Year");
        intent.putExtras(extData);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void asyncDataAfter(Context context, Bundle extData, int type) {
        // ToastUtils.show("收到钱迹数据！正在后台同步中...");
        switch (type) {
            case BROADCAST_ASYNC:
                doAsync(context, extData);
                break;
            case BROADCAST_GET_REI:
                doRei(context, extData);
                break;
            case BROADCAST_GET_YEAR:
                doYear(context, extData);
                break;
        }
        RootUtils.exec(new String[]{"am force-stop com.mutangtech.qianji"});

    }


    public String getQianJi(BillInfo billInfo) {

        String url = "qianji://publicapi/addbill?&type=" + billInfo.getType(true) + "&money=" + billInfo.getMoney();
        MMKV mmkv = MMKV.defaultMMKV();
        if (billInfo.getRemark() != null) {
            url += "&remark=" + billInfo.getRemark();
        }
        if (billInfo.getReimbursement() && billInfo.getType().equals(BillInfo.TYPE_INCOME)) {
            url += "&data=" + new String(Base64.encode(billInfo.getExtraData().getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE));
            if (billInfo.getAccountId1() != null)
                url += "&accountnameId=" + billInfo.getAccountId1();
            // String data = new String(Base64.decode(uri.getQueryParameter("data"),Base64.URL_SAFE));

        }
        //懒人模式，自动分类
        if (mmkv.getBoolean("lazy_mode", true)) {
            if (!mmkv.getBoolean("need_cate", true)) {
                return url + "&catechoose=0";
            } else {
                return url + "&catechoose=1";
            }

        }

        if (billInfo.getTime() != null) {
            url += "&time=" + billInfo.getTime();
        }

        if (billInfo.getCateName() != null) {
            url += "&catename=" + billInfo.getCateName();
        }
        url += "&catechoose=" + billInfo.getCateChoose();

        url += "&catetheme=auto";

        if (billInfo.getBookName()!= null && !billInfo.getBookName().equals("默认账本")) {
            url += "&bookname=" + billInfo.getBookName();
        }

        if (billInfo.getAccountName() != null && !billInfo.getAccountName().equals("") && !billInfo.getAccountName().equals("无账户")) {
            url += "&accountname=" + billInfo.getAccountName();
        }
        if (billInfo.getAccountName2() != null && !billInfo.getAccountName2().equals("") && !billInfo.getAccountName2().equals("无账户")) {
            url += "&accountname2=" + billInfo.getAccountName2();
        }
        if (billInfo.getFee() != null && !billInfo.getFee().equals("") && !billInfo.getFee().equals("0")) {
            url += "&fee=" + billInfo.getFee();
        }
        Log.i("钱迹URL:" + url);
        return url;
    }


    @Override
    public String getAsyncDesc(Context context) {
        if (AppStatus.xposedActive(context)) {
            return context.getResources().getString(R.string.qianji_async_desc);
        }
        return String.format(context.getResources().getString(R.string.async_no_support), getAppName());
    }
}

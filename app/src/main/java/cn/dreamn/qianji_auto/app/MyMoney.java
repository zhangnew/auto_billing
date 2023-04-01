package cn.dreamn.qianji_auto.app;

import static cn.dreamn.qianji_auto.core.broadcast.AppBroadcast.BROADCAST_ASYNC;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.hjq.toast.ToastUtils;
import com.tencent.mmkv.MMKV;

import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.bills.BillInfo;
import cn.dreamn.qianji_auto.setting.AppStatus;
import cn.dreamn.qianji_auto.utils.runUtils.Log;
import cn.dreamn.qianji_auto.utils.runUtils.RootUtils;
import cn.dreamn.qianji_auto.utils.runUtils.TaskThread;

public class MyMoney implements IApp {
    private static MyMoney myMoney;
    static long time = 0;
    public static MyMoney getInstance(){
        if(myMoney ==null)
            myMoney =new MyMoney();
        return myMoney;
    }
    @Override
    public String getPackPageName() {
        return "com.mymoney";
    }

    @Override
    public String getAppName() {
        return "随手记";
    }

    @Override
    public int getAppIcon() {
        return R.drawable.logo_mymoney;
    }

    public final String activityName = getPackPageName() + ".biz.addtrans.activity.AddTransActivityV12";
    private final ComponentName componentName = new ComponentName(getPackPageName(), activityName);

    @Override
    public void sendToApp(Context context,BillInfo billInfo) {

        Handler mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    // TODO 设置账户和分类
                    Log.d("随手记记账", billInfo.dump());
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra("cost", Double.valueOf(billInfo.getMoney()));
                    intent.putExtra("url_remark", billInfo.getRemark());
                    intent.putExtra("accountId", Long.valueOf(billInfo.getAccountId1()));
                    intent.setComponent(componentName);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    ToastUtils.show(R.string.money_send);
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
            TaskThread.onMain(m, () -> delay(handler));
        } else {
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public void asyncDataBefore(Context context, int type) {
        if (AppStatus.isXposed()) {
            Log.i("自动记账同步", "同步开始");
            RootUtils.exec(new String[]{"am force-stop " + getPackPageName()});
            Intent intent = new Intent();
            intent.setComponent(componentName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("AutoSignal", type);
            MMKV mmkv = MMKV.defaultMMKV();
            mmkv.encode("AutoSignal", type);
            Log.i("自动记账同步", "正在前往" + getAppName());
            context.startActivity(intent);
            ToastUtils.show(getAppName() + "正在同步数据，请稍后...");
        } else {
            ToastUtils.show(String.format(context.getResources().getString(R.string.not_support), getAppName()));
        }
    }

    @Override
    public void asyncDataAfter(Context context, Bundle data, int type) {
        switch (type) {
            case BROADCAST_ASYNC:
                doAsync(context, data);
                break;
//            case BROADCAST_GET_REI:
//                doRei(context, data);
//                break;
//            case BROADCAST_GET_YEAR:
//                doYear(context, data);
//                break;
        }
        RootUtils.exec(new String[]{"am force-stop " + getPackPageName()});
    }

    @Override
    public String getAsyncDesc(Context context) {
        if (AppStatus.xposedActive(context)) {
            return context.getResources().getString(R.string.mymoney_async_desc);
        }
        return String.format(context.getResources().getString(R.string.async_no_support), getAppName());
    }
}

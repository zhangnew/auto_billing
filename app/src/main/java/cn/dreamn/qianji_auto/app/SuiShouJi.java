package cn.dreamn.qianji_auto.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.hjq.toast.ToastUtils;

import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.bills.BillInfo;
import cn.dreamn.qianji_auto.setting.AppStatus;
import cn.dreamn.qianji_auto.utils.runUtils.Log;
import cn.dreamn.qianji_auto.utils.runUtils.TaskThread;

public class SuiShouJi implements IApp {
    private static SuiShouJi suiShouJi;
    static long time = 0;
    public static SuiShouJi getInstance(){
        if(suiShouJi ==null)
            suiShouJi =new SuiShouJi();
        return suiShouJi;
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
        return R.drawable.logo_suishouji;
    }

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
                    String activity = getPackPageName() + ".biz.addtrans.activity.AddTransActivityV12";
                    intent.setComponent(new ComponentName(getPackPageName(), activity));
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
        ToastUtils.show(R.string.sui_async_no_support);
    }

    @Override
    public void asyncDataAfter(Context context, Bundle data, int type) {
        ToastUtils.show(R.string.sui_async_no_support);
    }

    @Override
    public String getAsyncDesc(Context context) {
        if (AppStatus.xposedActive(context)) {
            return context.getResources().getString(R.string.sui_async_desc);
        }
        return context.getResources().getString(R.string.sui_async_no_support);
    }
}

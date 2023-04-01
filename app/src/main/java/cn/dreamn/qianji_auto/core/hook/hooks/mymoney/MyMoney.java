package cn.dreamn.qianji_auto.core.hook.hooks.mymoney;

import cn.dreamn.qianji_auto.core.hook.core.hookBase;
import cn.dreamn.qianji_auto.core.hook.hooks.mymoney.hooks.AccountList;

public class MyMoney extends hookBase {
    static final hookBase self = new MyMoney();
    public static hookBase getInstance() {
        return self;
    }
    @Override
    public void hookLoadPackage() {
        try {
            AccountList.init(utils);
        } catch (Throwable e) {
            utils.log(getAppName(), " AccountList HookError ", e.toString());
        }
    }

    @Override
    public String getPackPageName() {
        return cn.dreamn.qianji_auto.app.MyMoney.getInstance().getPackPageName();
    }

    @Override
    public String getAppName() {
        return cn.dreamn.qianji_auto.app.MyMoney.getInstance().getAppName();
    }

    @Override
    public boolean needHelpFindApplication() {
        return true;
    }

    @Override
    public int hookIndex() {
        return 1;
    }
}

package cn.dreamn.qianji_auto.app;

import java.util.ArrayList;
import java.util.List;



public class AppList {
    private static AppList mAppList;
    private final List<IApp> mList = new ArrayList<>();

    public AppList() {
        mList.clear();
        mList.add(QianJi.getInstance());
        mList.add(MyMoney.getInstance());
        /*
         * 请在此处新增你的记账app
         * 相关文档请参考：
         * */

    }

    public synchronized static AppList getInstance() {
        if (mAppList == null) {
            mAppList = new AppList();
        }
        return mAppList;
    }

    public List<IApp> getList() {
        return mList;
    }
}

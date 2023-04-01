/*
 * Copyright (C) 2021 dreamn(dream@dreamn.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package cn.dreamn.qianji_auto.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hjq.toast.ToastUtils;

import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.bills.BillInfo;
import cn.dreamn.qianji_auto.data.database.Db;
import cn.dreamn.qianji_auto.data.database.Table.Category;
import cn.dreamn.qianji_auto.ui.utils.HandlerUtil;
import cn.dreamn.qianji_auto.utils.runUtils.Log;
import cn.dreamn.qianji_auto.utils.runUtils.TaskThread;

public interface IApp {

    /**
     * 返回包名
     *
     * @return
     */
    String getPackPageName();

    /**
     * 返回APP名，可以自己乱定义
     *
     * @return
     */
    String getAppName();


    /**
     * 获取app图标
     * @return
     */
    int getAppIcon();

    /**
     * 发送数据给app
     *
     * @param str
     */
    void sendToApp(Context context, BillInfo str);

    /**
     * 同步数据(发出请求)
     *
     * @return
     */
    void asyncDataBefore(Context context, int type);

    /**
     * 同步数据(通过广播获取)
     *
     * @return
     */
    void asyncDataAfter(Context context, Bundle data, int type);

    /**
     * 同步数据的说明性文字
     */
    String getAsyncDesc(Context context);

    default void doAsync(Context context, Bundle extData) {
        String json = extData.getString("data");
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONArray asset = jsonObject.getJSONArray("asset");
        JSONArray category = jsonObject.getJSONArray("category");
        JSONArray userBook = jsonObject.getJSONArray("userBook");

        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    Log.i((String) msg.obj);
                } else {
                    ToastUtils.show(R.string.async_success);
                }

            }
        };

        if (asset == null || category == null || userBook == null) {
            Log.i(getAppName() + "数据信息无效");
            return;
        }

        TaskThread.onThread(() -> {
            // 分类数据处理
            if (category.size() > 0) {
                Db.db.CategoryDao().clean();
            }
            for (int i = 0; i < category.size(); i++) {
                HandlerUtil.send(mHandler, "（" + (i + 1) + "/" + category.size() + "）正在处理【分类数据】", 1);
                JSONObject jsonObject1 = category.getJSONObject(i);
                String name = jsonObject1.getString("name");
                String icon = jsonObject1.getString("icon");
                String level = jsonObject1.getString("level");
                String type = jsonObject1.getString("type");
                String self_id = jsonObject1.getString("id");
                String parent_id = jsonObject1.getString("parent");
                String book_id = jsonObject1.getString("book_id");
                String sort = jsonObject1.getString("sort");

                if (self_id == null || self_id.equals("")) {
                    self_id = String.valueOf(System.currentTimeMillis());
                }
                if (sort == null || sort.equals("")) {
                    sort = "500";
                }
                String self = self_id;

                Category[] category1 = Db.db.CategoryDao().getByName(name, type, book_id);

                if (category1 != null && category1.length > 0) continue;

                Db.db.CategoryDao().add(name, icon, level, type, self, parent_id, book_id, sort);
            }
            Log.i("分类数据处理完毕");

            //资产数据处理
            if (asset.size() > 0) {
                Db.db.AssetDao().clean();
            }
            for (int i = 0; i < asset.size(); i++) {
                HandlerUtil.send(mHandler, "（" + (i + 1) + "/" + asset.size() + "）正在处理【资产数据】", 1);
                JSONObject item = asset.getJSONObject(i);
                if (item.containsKey("type") && item.getString("type").equals("5"))
                    continue;
                String icon = (String) item.getOrDefault("icon", "https://pic.dreamn.cn/uPic/2021032022075916162492791616249279427UY2ok6支付.png");
                Integer sort = (Integer) item.getOrDefault("sort", 0);
                Db.db.AssetDao().add(item.getString("name"), icon, sort, item.getString("id"));
            }
            Log.i("资产数据处理完毕");

            // 账本数据处理
            if (userBook.size() > 0) {
                Db.db.BookNameDao().clean();
            }
            for (int i = 0; i < userBook.size(); i++) {
                HandlerUtil.send(mHandler, "（" + (i + 1) + "/" + userBook.size() + "）正在处理【账本数据】", 1);
                JSONObject jsonObject1 = userBook.getJSONObject(i);
                String bookName = jsonObject1.getString("name");
                String icon = jsonObject1.getString("cover");
                String bid = jsonObject1.getString("id");
                if (bid == null || bid.equals("")) {
                    bid = String.valueOf(System.currentTimeMillis());
                }
                Db.db.BookNameDao().add(bookName, icon, bid);
            }
            Log.i("账本数据处理完毕");

            HandlerUtil.send(mHandler, 0);
        });


    }
}

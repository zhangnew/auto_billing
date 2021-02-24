/*
 * Copyright (C) 2019 xuexiangjys(xuexiangjys@163.com)
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

package cn.dreamn.qianji_auto.ui.adapter;


import android.content.Context;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.adapter.SmartRecyclerAdapter;
import com.scwang.smartrefresh.layout.adapter.SmartViewHolder;

import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.utils.picture.MyBitmapUtils;
import cn.dreamn.qianji_auto.utils.tools.Logs;

/**
 * 主副标题显示适配器
 *
 * @author xuexiang
 * @since 2018/12/19 上午12:19
 */
public class ListAdapter3 extends SmartRecyclerAdapter<Bundle> {

    private OnItemClickListener listener;


    private final Context mContext;

    public ListAdapter3(Context context) {
        super(R.layout.list_item3);
        mContext = context;
    }


    //第二步， 写一个公共的方法
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onBindViewHolder(SmartViewHolder holder, Bundle model, int position) {
        TextView tv_title = holder.findViewById(R.id.tv_title);
        FrameLayout fl_title = holder.findViewById(R.id.fl_title);
        MyBitmapUtils myBitmapUtils = new MyBitmapUtils(mContext);


        String name = model.getString("name");
        tv_title.setText(name);
        String cover = model.getString("cover");

        myBitmapUtils.disPlay(fl_title, cover);
        fl_title.setOnClickListener(v -> {
            if (listener != null) {

                listener.onClick(model, position);
            }
        });
    }

    //第一步 定义接口
    public interface OnItemClickListener {
        void onClick(Bundle item, int position);

    }
}

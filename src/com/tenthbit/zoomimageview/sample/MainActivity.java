/*******************************************************************************
 * Copyright 2013 Tomasz Zawada
 * 
 * Based on the excellent PhotoView by Chris Banes:
 * https://github.com/chrisbanes/PhotoView
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.tenthbit.zoomimageview.sample;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends ListActivity {

    public static final String[] options = {
            "One Image Sample", "ViewPager Sample"
    };

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            getActionBar().setBackgroundDrawable(new ColorDrawable(Color.GRAY));
            // Note: if you use ActionBarSherlock use here getSupportActionBar()
        }

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options));

        getListView().setBackgroundColor(0xFF404040);
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Intent intent;

        switch (position) {
            default:
            case 0:
                intent = new Intent(this, OneImageSampleActivity.class);
                break;
            case 1:
                intent = new Intent(this, ViewPagerSampleActivity.class);
                break;
        }

        startActivity(intent);
    }
}

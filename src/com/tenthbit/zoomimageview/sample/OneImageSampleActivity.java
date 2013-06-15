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
import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

import com.tenthbit.view.ZoomImageView;
import com.tenthbit.view.ZoomImageView.OnPhotoTapListener;
import com.tenthbit.zoomimageview.R;

public class OneImageSampleActivity extends Activity {

    static final String PHOTO_TAP_TOAST_STRING = "Photo Tap! X: %.2f %% Y:%.2f %%";

    private ZoomImageView zoomImageView;

    private Toast toast;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Use full screen window and translucent action bar
         */
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawable(new ColorDrawable(0xFF000000));
        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
            getActionBar().setBackgroundDrawable(new ColorDrawable(0x88000000));
            // Note: if you use ActionBarSherlock use here getSupportActionBar()
        }

        setContentView(R.layout.one_image);

        zoomImageView = (ZoomImageView) findViewById(R.id.zoomImageView);

        zoomImageView.setImageDrawable(getResources().getDrawable(R.drawable.image1));

        // Lets attach some listeners (optional)
        zoomImageView.setOnPhotoTapListener(new PhotoTapListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem zoomToggle = menu.findItem(R.id.menu_zoom_toggle);
        zoomToggle.setTitle(zoomImageView.isZoomEnabled() ? R.string.menu_zoom_disable
                : R.string.menu_zoom_enable);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_zoom_toggle:
                zoomImageView.setIsZoomEnabled(!zoomImageView.isZoomEnabled());
                return true;

            case R.id.menu_scale_fit_center:
                zoomImageView.setScaleType(ScaleType.FIT_CENTER);
                return true;

            case R.id.menu_scale_fit_start:
                zoomImageView.setScaleType(ScaleType.FIT_START);
                return true;

            case R.id.menu_scale_fit_end:
                zoomImageView.setScaleType(ScaleType.FIT_END);
                return true;

            case R.id.menu_scale_fit_xy:
                zoomImageView.setScaleType(ScaleType.FIT_XY);
                return true;

            case R.id.menu_scale_scale_center:
                zoomImageView.setScaleType(ScaleType.CENTER);
                return true;

            case R.id.menu_scale_scale_center_crop:
                zoomImageView.setScaleType(ScaleType.CENTER_CROP);
                return true;

            case R.id.menu_scale_scale_center_inside:
                zoomImageView.setScaleType(ScaleType.CENTER_INSIDE);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class PhotoTapListener implements OnPhotoTapListener {
        @Override
        public void onPhotoTap(View view, float x, float y) {
            float xPercentage = x * 100f;
            float yPercentage = y * 100f;

            if (toast != null) {
                toast.cancel();
            }

            toast = Toast.makeText(OneImageSampleActivity.this,
                    String.format(PHOTO_TAP_TOAST_STRING, xPercentage, yPercentage),
                    Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}

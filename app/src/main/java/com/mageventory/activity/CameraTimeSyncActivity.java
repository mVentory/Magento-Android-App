/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/
package com.mageventory.activity;

import java.util.Date;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.mageventory.MageventoryConstants;
import com.mventory.R;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.base.BaseFragment;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.ZXingCodeEncoder;

public class CameraTimeSyncActivity extends BaseFragmentActivity implements MageventoryConstants {
    private static final String TAG = CameraTimeSyncActivity.class.getSimpleName();
    public static final String TIMESTAMP_CODE_PREFIX = "TSS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new UiFragment()).commit();
        }
    }

    public static class UiFragment extends BaseFragment {
        private TimeUpdater mTimeUpdater;
        private QrCodeUpdate mQrCodeUpdater;

        private TextView mTimeView;
        private ImageView mQrCodeView;
        int mQrCodeWidth;
        int mQrCodeHeight;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.camera_sync, container, false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mTimeView = (TextView) view.findViewById(R.id.time_view);
            mQrCodeView = (ImageView) view.findViewById(R.id.code_view);
            mQrCodeWidth = getResources().getDimensionPixelSize(R.dimen.camera_sync_qr_code_width);
            mQrCodeHeight = getResources()
                    .getDimensionPixelSize(R.dimen.camera_sync_qr_code_height);
        }

        @Override
        public void onResume() {
            super.onResume();
            mTimeUpdater = new TimeUpdater();
            GuiUtils.post(mTimeUpdater);
            mQrCodeUpdater = new QrCodeUpdate();
            mQrCodeUpdater.executeOnExecutor(Executors.newSingleThreadExecutor());
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mTimeUpdater != null) {
                mTimeUpdater.stop();
            }
            if (mQrCodeUpdater != null) {
                mQrCodeUpdater.cancel(true);
            }
        }

        private class TimeUpdater implements Runnable {

            boolean active = true;

            @Override
            public void run() {
                if (active) {
                    mTimeView.setText(CommonUtils.formatDateTime(new Date()));

                    GuiUtils.postDelayed(mTimeUpdater, 500);
                }
            }

            public void stop() {
                active = false;
            }
        }

        private class QrCodeUpdate extends SimpleAsyncTask {
            public QrCodeUpdate() {
                super(null);
            }

            Bitmap qrCode;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    qrCode = ZXingCodeEncoder.encodeAsBitmap(
                            TIMESTAMP_CODE_PREFIX + CommonUtils.formatDateTime(new Date()),
                            BarcodeFormat.QR_CODE, mQrCodeWidth, mQrCodeHeight);
                    return true;
                } catch (Exception ex) {
                    GuiUtils.error(TAG, R.string.errorCouldNotGenerateQRCode, ex);
                }
                return false;
            }

            @Override
            protected void onSuccessPostExecute() {
                if (!isCancelled()) {
                    mQrCodeView.setImageBitmap(qrCode);
                    GuiUtils.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isCancelled()) {
                                mQrCodeUpdater = new QrCodeUpdate();
                                mQrCodeUpdater.executeOnExecutor(Executors
                                        .newSingleThreadExecutor());
                            }
                        }
                    }, 300);
                }
            }

            @Override
            protected void onFailedPostExecute() {
                super.onFailedPostExecute();
                mQrCodeUpdater = null;
            }
        }
    }
}

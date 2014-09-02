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

import java.util.HashSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.activity.base.BaseActivityCommon;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.DefaultOptionsMenuHelper;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ScanUtils;
import com.mageventory.util.SingleFrequencySoundGenerator;

public class ScanActivity extends BaseActivity implements MageventoryConstants, OperationObserver {

    private int loadRequestID;
    private boolean barcodeScanned;
    private boolean scanResultProcessing;
    private String sku;
    private String labelUrl;
    private boolean skuFound;
    private boolean scanDone;
    private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
    private boolean isActivityAlive;
    private SingleFrequencySoundGenerator mDetailsLoadFailureSound;
    private Settings mSettings;
    private long mGalleryTimestamp;
    private boolean bulkMode = false;
    private View mProgressStatus;
    private TextView mProgressMessage;

    public static class DomainNamePair
    {
        private String mDomain1;
        private String mDomain2;

        public DomainNamePair(String domain1, String domain2)
        {
            mDomain1 = domain1;
            mDomain2 = domain2;
        }

        @Override
        public int hashCode() {
            return mDomain1.hashCode() * mDomain2.hashCode() + mDomain1.hashCode()
                    + mDomain2.hashCode();
        }

        @Override
        public boolean equals(Object o) {

            if (TextUtils.equals(((DomainNamePair) o).mDomain1, mDomain1) &&
                    TextUtils.equals(((DomainNamePair) o).mDomain2, mDomain2))
            {
                return true;
            }

            return false;
        }
    }

    /*
     * In case user scans a label which contains domain name which doesn't match
     * the domain name from the current profile we are showing a dialog with a
     * warning and two buttons: "OK" and "Cancel". In case user presses "OK" for
     * any pair of such domain names we don't want the warning dialog to be
     * displayed anymore for this particular pair during the lifetime of the
     * application's process. This is why we need to store those pairs in this
     * hashset.
     */
    private static HashSet<DomainNamePair> sDomainNamePairsRemembered = new HashSet<DomainNamePair>();

    public static boolean domainPairRemembered(String settingsDomain, String labelDomain)
    {
        synchronized (sDomainNamePairsRemembered)
        {
            if (sDomainNamePairsRemembered
                    .contains(new DomainNamePair(settingsDomain, labelDomain)))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    public static void rememberDomainNamePair(String settingsDomain, String labelDomain)
    {
        synchronized (sDomainNamePairsRemembered)
        {
            DomainNamePair newDomainNamePair = new DomainNamePair(settingsDomain, labelDomain);

            if (!sDomainNamePairsRemembered.contains(newDomainNamePair))
            {
                sDomainNamePairsRemembered.add(newDomainNamePair);
            }
        }
    }

    public static String getDomainNameFromUrl(String url)
    {
        if (url == null)
            return null;

        int index;
        String domain;

        domain = url;

        index = domain.indexOf("://");

        if (index != -1)
        {
            domain = domain.substring(index + "://".length(), domain.length());
        }

        index = domain.indexOf("/");

        if (index != -1)
        {
            domain = domain.substring(0, index);
        }

        return domain;
    }

    /* Is the label in the following format: http://..../sku/[sku] */
    public static boolean isLabelInTheRightFormat(String label)
    {
        /* Does the label start with "http://" ? */
        if (!label.startsWith(HTTP_PROTO_PREFIX))
        {
            /* No, bad label. */
            return false;
        }

        /* Get rid of the "http://" from the label */
        label = label.substring(HTTP_PROTO_PREFIX.length());

        int lastSlashIndex = label.lastIndexOf("/");

        /* Does the label still contain a slash? */
        if (lastSlashIndex == -1)
        {
            /* No, bad label. */
            return false;
        }

        label = label.substring(0, lastSlashIndex);

        /* Does the label end with "/sku" ? */
        if (!label.endsWith("/sku"))
        {
            /* No, bad label. */
            return false;
        }

        return true;
    }

    /* Check if the SKU is in the form of "P" + 16 digits or "M" + 16 digits. */
    public static boolean isSKUInTheRightFormat(String sku)
    {
        if (!(sku.length() == 17))
            return false;

        if (!sku.startsWith("M") && !sku.startsWith("P"))
            return false;

        long timestamp;

        try
        {
            timestamp = Long.parseLong(sku.substring(1));
        } catch (NumberFormatException nfe)
        {
            return false;
        }

        if (timestamp < 0)
        {
            return false;
        }

        return true;
    }

    /*
     * Validate the label against the current url in the settings. If they don't
     * match return false.
     */
    public static boolean isLabelValid(Context c, String label)
    {
        /* Treat the label as valid if it's not in the right format. */
        if (!isLabelInTheRightFormat(label))
        {
            return true;
        }

        Settings settings = new Settings(c);
        String settingsUrl = settings.getUrl();

        String settingsDomainName = getDomainNameFromUrl(settingsUrl);
        String skuDomainName = getDomainNameFromUrl(label);

        if (TextUtils.equals(settingsDomainName, skuDomainName))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = new Settings(this);

        setContentView(R.layout.scan_activity);
        mProgressStatus = findViewById(R.id.progressStatus);
        mProgressMessage = (TextView) findViewById(R.id.progressMesage);
        findViewById(R.id.cancelButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Start QR Code Scanner

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sku = extras.getString(getString(R.string.ekey_product_sku));
        }

        if (sku != null)
        {
            scanDone = true;
            skuFound = true;
            if (!ScanActivity.isSKUInTheRightFormat(sku))
                barcodeScanned = true;
            labelUrl = mSettings.getUrl();

            if (JobCacheManager.saveRangeStart(sku, mSettings.getProfileID(), 0) == false)
            {
                ProductDetailsActivity.showTimestampRecordingError(this);
            }
        }
        else
        {
            if (savedInstanceState == null) {
                startScan();
            } else
                scanDone = true;
        }

        isActivityAlive = true;
    }

    private void startScan() {
        scanDone = false;
        skuFound = false;
        Runnable r = new ScanUtils.FinishActivityRunnable(this);
        ScanUtils.startScanActivityForResult(ScanActivity.this, SCAN_QR_CODE,
                R.string.scan_barcode_or_qr_label, r, r);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityAlive = false;
    }

    private void launchProductDetails(String prodSKU)
    {
        /* Launching product details from scan activity breaks NewNewReloadCycle */
        BaseActivityCommon.sNewNewReloadCycle = false;

        final String ekeyProductSKU = getString(R.string.ekey_product_sku);
        final String ekeySkipTimestampUpdate = getString(R.string.ekey_skip_timestamp_update);
        final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        intent.putExtra(getString(R.string.ekey_prod_det_launched_from_menu_scan), true);
        intent.putExtra(ekeyProductSKU, prodSKU);
        intent.putExtra(ekeySkipTimestampUpdate, scanResultProcessing);

        if (mGalleryTimestamp != 0)
        {
            intent.putExtra(getString(R.string.ekey_gallery_timestamp), mGalleryTimestamp);
        }

        startActivity(intent);
    }

    private void launchProductCreate(ProductDetailsLoadException skuExistsOnServerUncertainty)
    {
        // TODO replace with the ProducteCreateActivity.launchProductCreate call
        // and test
        final String ekeyProductSKU = getString(R.string.ekey_product_sku);
        final String ekeySkuExistsOnServerUncertainty = getString(R.string.ekey_sku_exists_on_server_uncertainty);
        final String brScanned = getString(R.string.ekey_barcode_scanned);
        final String ekeySkipTimestampUpdate = getString(R.string.ekey_skip_timestamp_update);

        final Intent intent = new Intent(getApplicationContext(), ProductCreateActivity.class);

        intent.putExtra(ekeyProductSKU, sku);
        intent.putExtra(ekeySkuExistsOnServerUncertainty, (Parcelable) skuExistsOnServerUncertainty);
        intent.putExtra(brScanned, barcodeScanned);
        intent.putExtra(ekeySkipTimestampUpdate, scanResultProcessing);

        if (mGalleryTimestamp != 0)
        {
            intent.putExtra(getString(R.string.ekey_gallery_timestamp), mGalleryTimestamp);
        }

        startActivity(intent);
    }

    private void launchProductList() {
        DefaultOptionsMenuHelper.onMenuProductsPressed(this);
    }

    @Override
    public void onLoadOperationCompleted(final LoadOperation op) {
        if (op.getOperationRequestId() == loadRequestID) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (isActivityAlive) {
                        hideProgress();

                        if (op.getException() != null) {

                            Settings settings = new Settings(ScanActivity.this);

                            mDetailsLoadFailureSound = SingleFrequencySoundGenerator
                                    .playFailureBeep(settings, mDetailsLoadFailureSound);
                            ProductDetailsLoadException exception = (ProductDetailsLoadException) op
                                    .getException();
                            if (exception.getFaultCode() == ProductDetailsLoadException.ERROR_CODE_PRODUCT_DOESNT_EXIST)
                            {
                                AlertDialog.Builder alert = new AlertDialog.Builder(
                                        ScanActivity.this);

                                alert.setTitle(R.string.info);
                                alert.setMessage(getString(R.string.product_not_found2, sku));

                                alert.setPositiveButton(R.string.product_not_found_search_by_name,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                launchProductList();
                                                finish();
                                            }
                                        });
                                alert.setNeutralButton(R.string.product_not_found_enter_it_now,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                /*
                                                 * Show new product activity
                                                 * withOUT information saying
                                                 * that we are not sure if the
                                                 * product is on the server or
                                                 * not (we know it is not)
                                                 */
                                                launchProductCreate(null);
                                                finish();
                                            }
                                        });
                                alert.setNegativeButton(R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        });
                                alert.setOnCancelListener(new OnCancelListener() {
                                    
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        finish();
                                    }
                                });
                                AlertDialog srDialog = alert.create();
                                srDialog.show();
                            }
                            else
                            {
                                /*
                                 * Show new product activity WITH information
                                 * saying that we are not sure if the product is
                                 * on the server or not (we really don't know,
                                 * we just received some strange exception)
                                 */
                                launchProductCreate(exception);
                                finish();
                            }

                        } else {
                            launchProductDetails(op.getExtras().getString(MAGEKEY_PRODUCT_SKU));
                            finish();
                        }
                    }
                }
            });
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(SCAN_DONE, true);
        super.onSaveInstanceState(outState);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        resHelper.unregisterLoadOperationObserver(this);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        resHelper.registerLoadOperationObserver(this);
        if (scanDone) {
            getInfo();
        }
    }

    public void showInvalidLabelDialog(final String settingsDomainName, final String skuDomainName) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Warning");
        alert.setMessage("Wrong label. Expected domain name: '" + settingsDomainName + "' found: '"
                + skuDomainName + "'");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                rememberDomainNamePair(settingsDomainName, skuDomainName);
                showProgress(R.string.scan_progress_status_message);
                new ProductInfoLoader().execute(sku);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ScanActivity.this.finish();
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                ScanActivity.this.finish();
            }
        });

        srDialog.show();
    }

    private void getInfo() {
        if (skuFound) {
            if (isLabelValid(this, labelUrl))
            {
                showProgress(getString(R.string.scan_progress_status_message2, sku));
                new ProductInfoLoader().execute(sku);
            }
            else
            {
                Settings settings = new Settings(this);
                String settingsUrl = settings.getUrl();

                if (!domainPairRemembered(getDomainNameFromUrl(settingsUrl),
                        getDomainNameFromUrl(labelUrl)))
                {
                    showInvalidLabelDialog(getDomainNameFromUrl(settingsUrl),
                            getDomainNameFromUrl(labelUrl));
                }
                else
                {
                    showProgress(getString(R.string.scan_progress_status_message2, sku));
                    new ProductInfoLoader().execute(sku);
                }
            }
        } else {
            finish();
        }
    }

    private void showProgress(int message) {
        showProgress(getString(message));
    }

    private void showProgress(final String message) {
        if (mProgressStatus.getVisibility() == View.VISIBLE) {
            return;
        }
        mProgressMessage.setText(message);

        mProgressStatus.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mProgressStatus.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        if (item.getItemId() != R.id.menu_refresh) {

            finish();
        }
        return result;
    }
    /**
     * Handling Scan Result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_QR_CODE) {
            scanDone = true;
            if (resultCode == RESULT_OK) {
                String contents = ScanUtils.getSanitizedScanResult(data);
                labelUrl = contents;
                String[] urlData = contents.split("/");
                if (urlData.length > 0) {
                    scanResultProcessing = true;
                    if (ScanActivity.isLabelInTheRightFormat(contents))
                    {
                        sku = urlData[urlData.length - 1];
                        barcodeScanned = false;
                    }
                    else
                    {
                        sku = contents;

                        if (!ScanActivity.isSKUInTheRightFormat(sku))
                            barcodeScanned = true;
                    }

                    if (barcodeScanned)
                    {
                        mGalleryTimestamp = JobCacheManager.getGalleryTimestampNow();
                    }
                    else
                    {
                        if (JobCacheManager.saveRangeStart(sku, mSettings.getProfileID(), 0) == false)
                        {
                            ProductDetailsActivity.showTimestampRecordingError(this);
                        }
                    }

                    skuFound = true;
                } else {
                    GuiUtils.alert("Not Valid");
                    skuFound = false;
                    return;
                }

            } else if (resultCode == RESULT_CANCELED) {
                // Do Nothing
            }
        }

    }

    /**
     * Start scan activity for the specified SKU
     * 
     * @param sku
     * @param activity
     */
    public static void startForSku(String sku, Activity activity) {
        Intent intent = new Intent(activity, ScanActivity.class);
        intent.putExtra(activity.getString(R.string.ekey_product_sku), sku);
        activity.startActivity(intent);
    }

    /**
     * Getting Product Details
     * 
     * @author hussein
     */
    private class ProductInfoLoader extends AsyncTask<Object, Void, Boolean> {

        private SettingsSnapshot mSettingsSnapshot;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSettingsSnapshot = new SettingsSnapshot(ScanActivity.this);
        }

        @Override
        protected Boolean doInBackground(Object... args) {
            ProductDetailsExistResult existResult = JobCacheManager.productDetailsExist(sku,
                    mSettingsSnapshot.getUrl(), true);
            boolean isInternetEnabled = CommonUtils.isInternetEnabled();
            if (scanResultProcessing && !isInternetEnabled && !bulkMode) {
                GuiUtils.alert(R.string.bulk_scan_mode_working);
            }
            bulkMode = scanResultProcessing && !isInternetEnabled;
            if (existResult.isExisting()) {
                sku = existResult.getSku();
                return Boolean.TRUE;
            } else {
                if (!bulkMode) {
                    final String[] params = new String[2];
                    params[0] = GET_PRODUCT_BY_SKU_OR_BARCODE;
                    params[1] = sku;

                    Bundle b = new Bundle();
                    b.putBoolean(EKEY_DONT_REPORT_PRODUCT_NOT_EXIST_EXCEPTION, true);

                    loadRequestID = resHelper.loadResource(ScanActivity.this, RES_PRODUCT_DETAILS,
                            params, b, mSettingsSnapshot);
                }
                return Boolean.FALSE;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (barcodeScanned) {
                if (!JobCacheManager.saveRangeStart(sku, mSettings.getProfileID(),
                        mGalleryTimestamp)) {
                    if (isActivityAlive) {
                        ProductDetailsActivity.showTimestampRecordingError(ScanActivity.this);
                    } else {
                        GuiUtils.alert(R.string.errorCannotCreateTimestamps);
                    }
                }
            }
            if (bulkMode) {
                startScan();
            } else {
                if (result.booleanValue()) {
                    if (isActivityAlive) {
                        hideProgress();
                        finish();
                        launchProductDetails(sku);
                    }
                }
            }
        }

    }

}

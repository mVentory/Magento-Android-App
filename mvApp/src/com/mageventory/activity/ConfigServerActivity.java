
package com.mageventory.activity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.bitmapfun.util.BitmapfunUtils;
import com.mageventory.bitmapfun.util.ImageCacheUtils;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobService;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ScanUtils;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.TrackerUtils;
import com.mageventory.util.WebUtils;

public class ConfigServerActivity extends BaseActivity implements MageventoryConstants {

    public static final String TAG = ConfigServerActivity.class.getSimpleName();
    public static final String ADD_PROFILE_EXTRA = MyApplication.getContext().getPackageName()
            + ".ADD_PROFILE_EXTRA";

    private Settings settings;

    private boolean newProfileMode = false;

    private Button save_profile_button;
    private Button delete_button;
    private Button new_button;

    private Button clear_cache;
    private Button wipe_data;
    private Button camera_sync_button;
    private Button save_global_settings_button;
    private Button queue_button;

    private Button button_general_expand;

    private LinearLayout generalSection;

    private boolean isActivityAlive;

    private EditText profileIdInput;
    private EditText userInput;
    private EditText passInput;
    private EditText urlInput;

    public ConfigServerActivity() {
    }

    private Spinner profileSpinner;
    private TextView notWorkingTextView;

    DownloadConfigTask mDownloadConfigTask;

    private Intent mLastScanIntent;
    private Integer mLastScanRequestCode = null;
    private Integer mLastScanMessage = null;
    private boolean mAddNewProfileOnResume = false;

    /*
     * Show a confirmation when clicking on one of the buttons for deleting the
     * cache so that the user knows that the button was clicked.
     */
    public void showCacheRemovedDialog() {
        if (isActivityAlive == false)
            return;

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Success");
        alert.setMessage("Cache deleted successfully.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void showRemoveProfileQuestion() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Question");

        if (newProfileMode == true)
        {
            alert.setMessage("Do you really want to remove new profile?");
        }
        else
        {
            alert.setMessage("Do you really want to remove that profile?");
        }

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (newProfileMode == true)
                {
                    newProfileMode = false;
                    profileSpinner.setEnabled(true);

                    refreshProfileSpinner(false);
                }
                else
                {
                    settings.removeStore(settings.getCurrentStoreUrl());
                    String[] list = settings.getListOfStores(false);

                    if (list.length > 0)
                    {
                        settings.switchToStoreURL(list[0]);
                    }
                    else
                    {
                        settings.switchToStoreURL(null);
                    }

                    refreshProfileSpinner(false);
                }

                if (settings.getListOfStores(false).length == 0)
                {
                    cleanProfileFields();
                }
                ConfigServerActivity.this.hideKeyboard();
                profileModified();
            }

        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void showRemoveCacheQuestion() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Question");
        alert.setMessage("Do you really want to remove cache for the current profile?");

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                final ProgressDialog pDialog = new ProgressDialog(ConfigServerActivity.this);
                pDialog.setMessage("Cache is being cleared...");
                pDialog.setIndeterminate(true);
                pDialog.setCancelable(true);
                pDialog.show();

                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        JobCacheManager.deleteCache(settings.getUrl());
                        ImageCacheUtils.clearDiskCaches();
                        ImageCacheUtils.sendDiskCacheClearedBroadcast();

                        ConfigServerActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                pDialog.cancel();
                                showCacheRemovedDialog();
                                profileModified();
                            }
                        });
                    }
                });
                t.start();
            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void showDataWipedDialog() {
        if (!isActivityAlive)
            return;

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Success");
        alert.setMessage("Data wiped.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void showWipeDataQuestion() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Wipe data?");
        alert.setMessage("Only settings will remain. All other app data will be erased.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                final ProgressDialog pDialog = new ProgressDialog(ConfigServerActivity.this);
                pDialog.setMessage("Data is being wiped...");
                pDialog.setIndeterminate(true);
                pDialog.setCancelable(true);
                pDialog.show();

                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {

                        if (JobCacheManager.wipeData(ConfigServerActivity.this))
                        {
                            ConfigServerActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    pDialog.cancel();
                                    showDataWipedDialog();
                                    profileModified();
                                }
                            });
                        }
                        else
                        {
                            ConfigServerActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    pDialog.cancel();
                                    showUnableToWipeDataDialog();
                                    profileModified();
                                }
                            });
                        }

                    }
                });
                t.start();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void showUnableToWipeDataDialog() {
        if (!isActivityAlive)
            return;

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Error");
        alert.setMessage("Unable to wipe data. Reason: A job is being executed.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_config);

        profileIdInput = (EditText) findViewById(R.id.profile_id_input);
        userInput = (EditText) findViewById(R.id.user_input);
        passInput = (EditText) findViewById(R.id.pass_input);
        urlInput = (EditText) findViewById(R.id.url_input);

        isActivityAlive = true;

        notWorkingTextView = ((TextView) findViewById(R.id.not_working_text_view));
        settings = new Settings(getApplicationContext());

        save_profile_button = (Button) findViewById(R.id.save_profile_button);
        delete_button = (Button) findViewById(R.id.deletebutton);
        new_button = (Button) findViewById(R.id.newbutton);
        camera_sync_button = (Button) findViewById(R.id.cameraSync);

        clear_cache = (Button) findViewById(R.id.clearCacheButton);
        wipe_data = (Button) findViewById(R.id.wipeDataButton);
        save_global_settings_button = (Button) findViewById(R.id.save_global_settings_button);

        generalSection = (LinearLayout) findViewById(R.id.generalSection);

        queue_button = (Button) findViewById(R.id.queueButton);

        queue_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfigServerActivity.this, QueueActivity.class);
                ConfigServerActivity.this.startActivity(intent);
            }
        });

        button_general_expand = (Button) findViewById(R.id.buttonGeneralExpand);

        button_general_expand.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (generalSection.getVisibility() == View.VISIBLE)
                {
                    generalSection.setVisibility(View.GONE);
                    button_general_expand.setText("Expand");
                }
                else
                {
                    generalSection.setVisibility(View.VISIBLE);
                    button_general_expand.setText("Collapse");
                }
            }
        });

        if (settings.getStoresCount() > 0)
        {
            generalSection.setVisibility(View.GONE);
            button_general_expand.setText("Expand");
        }

        clear_cache.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ConfigServerActivity.this.hideKeyboard();
                showRemoveCacheQuestion();
            }
        });

        wipe_data.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ConfigServerActivity.this.hideKeyboard();
                showWipeDataQuestion();
            }
        });

        restoreProfileFields();
        save_profile_button.setOnClickListener(saveProfileButtonlistener);
        delete_button.setOnClickListener(deleteButtonlistener);
        new_button.setOnClickListener(newButtonlistener);

        save_global_settings_button.setOnClickListener(saveGlobalSettingsButtonlistener);

        camera_sync_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(ConfigServerActivity.this, CameraTimeSyncActivity.class);
                ConfigServerActivity.this.startActivity(i);
            }
        });

        EditText googleAPI_key = (EditText) findViewById(R.id.google_book_api_input);
        googleAPI_key.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                mLastScanIntent = ScanUtils.getScanActivityIntent();
                mLastScanIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                if (!ScanUtils.startScanActivityForResult(ConfigServerActivity.this,
                        mLastScanIntent, SCAN_QR_CODE, null)) {
                    mLastScanRequestCode = SCAN_QR_CODE;
                    mLastScanMessage = null;
                }

                return true;
            }
        });

        profileSpinner = ((Spinner) findViewById(R.id.urls_spinner));

        refreshProfileSpinner(false);

        profileSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            int mPosition = profileSpinner.getSelectedItemPosition();
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position < settings.getStoresCount() && position >= 0)
                {
                    String url = (String) profileSpinner.getAdapter().getItem(position);
                    settings.switchToStoreURL(url);
                    restoreProfileFields();
                    ConfigServerActivity.this.hideKeyboard();
                    if (mPosition != position) {
                        mPosition = position;
                        profileModified();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        cleanProfileFields();
        restoreGlobalSettingsFields();

        /* Click the "new" button automatically when there are no profiles. */
        if (settings.getStoresCount() == 0)
        {
            userInput.requestFocus();
        }

        profileIdInput.addTextChangedListener(profileTextWatcher);
        userInput.addTextChangedListener(profileTextWatcher);
        passInput.addTextChangedListener(profileTextWatcher);
        urlInput.addTextChangedListener(profileTextWatcher);

        OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ConfigServerActivity.this.hideKeyboard();
            }
        };

        ((CheckBox) findViewById(R.id.enable_sound_checkbox))
                .setOnCheckedChangeListener(checkBoxListener);
        ((CheckBox) findViewById(R.id.new_products_enabled))
                .setOnCheckedChangeListener(checkBoxListener);
        ((CheckBox) findViewById(R.id.service_checkbox))
                .setOnCheckedChangeListener(checkBoxListener);
        reinitFromIntent(getIntent());
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mLastScanRequestCode != null) {
            if (ScanUtils.hasZxingInstalled(this)) {
                if (mLastScanIntent != null) {
                    ScanUtils.startScanActivityForResult(this, mLastScanIntent,
                            mLastScanRequestCode, mLastScanMessage);
                } else {
                    ScanUtils.startScanActivityForResult(this, mLastScanRequestCode,
                            mLastScanMessage);
                }
            }
            mLastScanRequestCode = null;
            mLastScanMessage = null;
        }
        if (mAddNewProfileOnResume) {
            mAddNewProfileOnResume = false;
            newButtonlistener.onClick(new_button);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityAlive = false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (newProfileMode)
            {
                /*
                 * If we are in new profile mode and user presses back then go
                 * to normal mode instead of closing the activity.
                 */
                deleteButtonlistener.onClick(null);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void cleanProfileFields()
    {
        profileIdInput.setText("");
        userInput.setText("");
        passInput.setText("");
        urlInput.setText("");

        notWorkingTextView.setVisibility(View.GONE);
        save_profile_button.setEnabled(false);
    }

    private void refreshProfileSpinner(boolean withNewOption)
    {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this, R.layout.default_spinner_dropdown, settings.getListOfStores(withNewOption));

        profileSpinner.setAdapter(arrayAdapter);

        if (withNewOption == true)
        {
            profileSpinner.setSelection(arrayAdapter.getCount() - 1);
        }
        else
        {
            profileSpinner.setSelection(settings.getCurrentStoreIndex());
        }

        save_profile_button.setEnabled(false);

        if (withNewOption)
        {
            new_button.setEnabled(false);
            delete_button.setEnabled(true);
            clear_cache.setEnabled(false);
        }
        else
        {
            new_button.setEnabled(true);

            if (settings.getStoresCount() > 0)
            {
                delete_button.setEnabled(true);
                clear_cache.setEnabled(true);
            }
            else
            {
                delete_button.setEnabled(false);
                clear_cache.setEnabled(false);
            }
        }

        if (withNewOption == false && settings.getStoresCount() == 0)
        {
            profileIdInput.setEnabled(false);
            userInput.setEnabled(false);
            passInput.setEnabled(false);
            urlInput.setEnabled(false);
        }
        else
        {
            profileIdInput.setEnabled(true);
            userInput.setEnabled(true);
            passInput.setEnabled(true);
            urlInput.setEnabled(true);
        }
    }

    private void restoreGlobalSettingsFields()
    {
        String key = settings.getAPIkey();
        String galleryPath = settings.getGalleryPhotosDirectory();
        String errorReportRecipient = settings.getErrorReportRecipient();
        String maxImageWidth = settings.getMaxImageWidth();
        String maxImageHeight = settings.getMaxImageHeight();
        boolean soundEnabled = settings.getSoundCheckBox();
        boolean newProductsEnabled = settings.getNewProductsEnabledCheckBox();
        boolean serviceCheckboxChecked = settings.getServiceCheckBox();

        ((EditText) findViewById(R.id.google_book_api_input)).setText(key);
        ((EditText) findViewById(R.id.gallery_photos_directory_input)).setText(galleryPath);
        ((EditText) findViewById(R.id.error_report_recipient_input)).setText(errorReportRecipient);
        ((EditText) findViewById(R.id.max_image_width_px)).setText(maxImageWidth);
        ((EditText) findViewById(R.id.max_image_height_px)).setText(maxImageHeight);
        ((CheckBox) findViewById(R.id.enable_sound_checkbox)).setChecked(soundEnabled);
        ((CheckBox) findViewById(R.id.new_products_enabled)).setChecked(newProductsEnabled);
        ((CheckBox) findViewById(R.id.service_checkbox)).setChecked(serviceCheckboxChecked);
    }

    private void restoreProfileFields() {
        long profileID = settings.getProfileID();
        String user = settings.getUser();
        String pass = settings.getPass();
        String url = settings.getUrl();

        profileIdInput.setText("" + profileID);
        userInput.setText(user);
        passInput.setText(pass);
        urlInput.setText(url);

        if (newProfileMode == true || settings.getProfileDataValid() == true
                || settings.getStoresCount() == 0)
        {
            notWorkingTextView.setVisibility(View.GONE);
        }
        else
        {
            notWorkingTextView.setVisibility(View.VISIBLE);
        }
        save_profile_button.setEnabled(false);
    }

    public boolean isEmailValid(String email)
    {
        if (email == null || email.length() < 3)
            return false;

        if (!email.contains("@"))
            return false;

        if (email.indexOf("@") != email.lastIndexOf("@"))
            return false;

        if (email.startsWith("@"))
            return false;

        if (email.endsWith("@"))
            return false;

        return true;
    }

    private void profileModified() {
        EventBusUtils.sendGeneralEventBroadcast(EventType.SETTINGS_CHANGED);
    }

    private OnClickListener saveGlobalSettingsButtonlistener = new OnClickListener() {
        public void onClick(View v) {
            String apiKey = ((EditText) findViewById(R.id.google_book_api_input)).getText()
                    .toString();
            String galleryPath = ((EditText) findViewById(R.id.gallery_photos_directory_input))
                    .getText().toString();
            String errorReportRecipient = ((EditText) findViewById(R.id.error_report_recipient_input))
                    .getText().toString();
            String maxImageWidth = ((EditText) findViewById(R.id.max_image_width_px)).getText()
                    .toString();
            String maxImageHeight = ((EditText) findViewById(R.id.max_image_height_px)).getText()
                    .toString();
            boolean sound = ((CheckBox) findViewById(R.id.enable_sound_checkbox)).isChecked();
            boolean productsEnabled = ((CheckBox) findViewById(R.id.new_products_enabled))
                    .isChecked();
            boolean serviceCheckboxChecked = ((CheckBox) findViewById(R.id.service_checkbox))
                    .isChecked();

            if (!galleryPath.startsWith("/"))
            {
                galleryPath = "/" + galleryPath;
            }

            if (!new File(galleryPath).exists())
            {
                Toast.makeText(getApplicationContext(),
                        "Gallery photos directory does not exist. Settings not saved.",
                        Toast.LENGTH_LONG).show();

                return;
            }
            else
            if (errorReportRecipient.length() > 0 && !isEmailValid(errorReportRecipient))
            {
                Toast.makeText(getApplicationContext(),
                        "Email address invalid. Settings not saved.", Toast.LENGTH_LONG).show();

                return;
            }
            else
            {
                ((EditText) findViewById(R.id.gallery_photos_directory_input)).setText(galleryPath);

                if (TextUtils.equals(apiKey, ""))
                {
                    Toast.makeText(
                            getApplicationContext(),
                            "No Google Books API -- Book Search Feature Will be Disabled. Settings saved.",
                            Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(),
                            "Settings saved.", Toast.LENGTH_LONG).show();
                }
            }

            settings.setAPIkey(apiKey);
            settings.setGalleryPhotosDirectory(galleryPath);
            settings.setErrorReportRecipient(errorReportRecipient);
            settings.setMaxImageHeight(maxImageHeight);
            settings.setMaxImageWidth(maxImageWidth);
            settings.setSoundCheckBox(sound);
            settings.setNewProductsEnabledCheckBox(productsEnabled);
            settings.setServiceCheckBox(serviceCheckboxChecked);

            if (serviceCheckboxChecked)
            {
                JobService.wakeUp(ConfigServerActivity.this);
            }

            ConfigServerActivity.this.hideKeyboard();
            startMain();
        }
    };

    private OnClickListener saveProfileButtonlistener = new OnClickListener() {
        public void onClick(View v) {
            validateAndSaveProfile(true);
        }

    };

    private void validateAndSaveProfile(boolean startHomeOnSuccessValidation) {
        String user = userInput.getText().toString();
        String pass = passInput.getText().toString();
        String url = urlInput.getText().toString();
        String profileID = profileIdInput.getText().toString();
        long profileIDLong;

        if (profileID.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please provide profile ID.", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        try {
            profileIDLong = Long.parseLong(profileID);
        } catch (NumberFormatException e) {
            Toast.makeText(getApplicationContext(), "Profile ID must be an integer.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (settings.isProfileIDTaken(profileIDLong)
                && (newProfileMode == true || (newProfileMode == false && settings.getProfileID() != profileIDLong))) {
            Toast.makeText(getApplicationContext(),
                    "This profile ID is already taken. Please provide a different one.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (user.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please provide user name.", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if (pass.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please provide password.", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if (url.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please provide store url.", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }

        TestingConnection tc = new TestingConnection(startHomeOnSuccessValidation, newProfileMode,
                new SettingsSnapshot(url, user, pass, profileIDLong));
        tc.execute(new String[] {});
        hideKeyboard();
        profileModified();
    }

    private OnClickListener deleteButtonlistener = new OnClickListener() {
        public void onClick(View v) {
            ConfigServerActivity.this.hideKeyboard();
            showRemoveProfileQuestion();
        }
    };

    private OnClickListener newButtonlistener = new OnClickListener() {
        public void onClick(View v) {
            addNewProfile();
            
            //Pop a scanner as a default to scan a QR code with a list of values for the config.
            mLastScanIntent = ScanUtils.getScanActivityIntent();
            mLastScanIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            if (!ScanUtils.startScanActivityForResult(ConfigServerActivity.this, mLastScanIntent,
                    SCAN_CONFIG_DATA, R.string.scan_configuration_code)) {
                mLastScanRequestCode = SCAN_CONFIG_DATA;
                mLastScanMessage = R.string.scan_configuration_code;
            }
            
        }
    };

    private void addNewProfile() {
        newProfileMode = true;

        refreshProfileSpinner(true);

        profileSpinner.setEnabled(false);

        cleanProfileFields();

        profileIdInput.setText("" + settings.getNextProfileID());
    }

    private TextWatcher profileTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            save_profile_button.setEnabled(true);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub

        }
    };

    protected void onStop() {
        super.onStop();
        if (mDownloadConfigTask != null) {
            mDownloadConfigTask.cancel(true);
            mDownloadConfigTask.dismissProgress();
        }
    };

    private void startMain() {
        Intent myIntent = new Intent(this, MainActivity.class);
        myIntent.putExtra(getString(R.string.ekey_dont_show_menu), true);
        startActivity(myIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SCAN_QR_CODE) {
            if (resultCode == RESULT_OK) {
                String contents = ScanUtils.getSanitizedScanResult(data);
                ((EditText) findViewById(R.id.google_book_api_input)).setText(contents);
            }
        } else if (requestCode == SCAN_CONFIG_DATA) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra(ScanUtils.SCAN_ACTIVITY_RESULT);
                if (contents != null) {
                    if (parseConfig(contents)) {
                        validateAndSaveProfile(true);
                    } else {
                        GuiUtils.alert(R.string.errorInvalidConfigQR);
                    }
                }

            }
        }
    }

    private boolean parseConfig(String contents) {
        String[] lines = contents.split("\\r?\\n");
        if (lines.length == 3) {
            int ind = 0;
            userInput.setText(lines[ind++]);
            passInput.setText(lines[ind++]);
            urlInput.setText(lines[ind++]);
            return true;
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        reinitFromIntent(intent);
    }

    public void reinitFromIntent(Intent intent) {
        CommonUtils.debug(TAG, "reinitFromIntent: started");
        boolean processed = false;
        if (intent != null && intent.getData() != null) {
            Uri uri = intent.getData();
            if (uri != null) {
                String mventorySchema = CommonUtils
                        .getStringResource(R.string.settings_content_uri_path_schema);
                if (uri.getScheme().equals(mventorySchema)) {
                    CommonUtils.debug(TAG, "reinitFromIntent: uri %1$s", uri.toString());
                    if (mDownloadConfigTask != null) {
                        mDownloadConfigTask.cancel(true);
                    }
                    addNewProfile();
                    String url = "http" + uri.toString().substring(mventorySchema.length());
                    mDownloadConfigTask = new DownloadConfigTask(url);
                    mDownloadConfigTask.execute();
                }
                processed = true;
            }
        } else if (intent != null) {
            if (intent.hasExtra(ADD_PROFILE_EXTRA)) {
                mAddNewProfileOnResume = true;
                processed = true;
            }
        }
        if (!processed && !settings.hasSettings()) {
            mAddNewProfileOnResume = true;
        }
    }
    private class TestingConnection extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog pDialog;
        private MagentoClient client;
        boolean mStartHomeOnSuccessValidation;
        boolean mNewProfileMode;
        SettingsSnapshot mSettingsSnapshot;
        boolean mProfileValid = false;

        TestingConnection(boolean startHomeOnSuccessValidation, boolean newProfileMode,
                SettingsSnapshot settingsSnapshot) {
            mStartHomeOnSuccessValidation = startHomeOnSuccessValidation;
            mNewProfileMode = newProfileMode;
            mSettingsSnapshot = settingsSnapshot;
        }

        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(ConfigServerActivity.this);
            pDialog.setMessage("Testing Settings");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... st) {

            try {
                client = new MagentoClient(mSettingsSnapshot);
            } catch (MalformedURLException e) {
                mProfileValid = false;
                return false;
            }
            client.login();

            if (client.isLoggedIn())
            {
                mProfileValid = true;
            }
            else
            {
                mProfileValid = false;
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            pDialog.dismiss();
            if (result) {
                Toast.makeText(getApplicationContext(), "Profile configured.", Toast.LENGTH_LONG)
                        .show();
            } else {
                GuiUtils.alert(R.string.error_profile_validation_failed);
            }
            if (mProfileValid)
            {
                if (mNewProfileMode) {
                    settings.addStore(mSettingsSnapshot.getUrl());
                    settings.switchToStoreURL(mSettingsSnapshot.getUrl());
                    profileSpinner.setEnabled(true);

                    newProfileMode = false;
                }

                settings.setUrl(mSettingsSnapshot.getUrl());
                settings.setUser(mSettingsSnapshot.getUser());
                settings.setPass(mSettingsSnapshot.getPassword());
                settings.setProfileID(mSettingsSnapshot.getProfileID());
                settings.setProfileDataValid(true);
                if (mStartHomeOnSuccessValidation && isActivityAlive) {
                    startMain();
                    return;
                }
                notWorkingTextView.setVisibility(View.GONE);
                refreshProfileSpinner(false);
            }
            else
            {
                notWorkingTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    class DownloadConfigTask extends SimpleAsyncTask {
        ProgressDialog mProgress;
        String mUrl;
        String mContent;

        public DownloadConfigTask(String url) {
            super(null);
            mUrl = url;
        }

        public void startLoading() {
            super.startLoading();
            showProgress();
        }

        public void stopLoading() {
            super.stopLoading();
            dismissProgress();
        }

        public void showProgress() {
            if (!isActivityAlive) {
                    return;
            }
            mProgress = new ProgressDialog(ConfigServerActivity.this);
            mProgress.setCancelable(true);
            mProgress.setIndeterminate(true);
            mProgress.setMessage(CommonUtils
                    .getStringResource(R.string.loading_configuration_from_web));
            mProgress.show();
        }

        public void dismissProgress() {
            try {
                if (mProgress != null && mProgress.getWindow() != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
            } catch (Exception ex) {
                CommonUtils.error(TAG, null, ex);
            }
            mProgress = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            if (isCancelled()) {
                return;
            }
            if (isActivityAlive) {
                if (parseConfig(mContent)) {
                    validateAndSaveProfile(true);
                } else {
                    GuiUtils.alert(R.string.errorInvalidConfigWeb);
                }
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                BitmapfunUtils.disableConnectionReuseIfNecessary();
                HttpURLConnection urlConnection = null;

                try {
                    long start = System.currentTimeMillis();
                    final URL url = new URL(mUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    final InputStream in = new BufferedInputStream(urlConnection.getInputStream(),
                            BitmapfunUtils.IO_BUFFER_SIZE);

                    TrackerUtils.trackDataLoadTiming(System.currentTimeMillis() - start,
                            "downloadConfig", TAG);
                    mContent = WebUtils.convertStreamToString(in);

                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
                return !isCancelled();
            } catch (Exception ex) {
                GuiUtils.error(TAG, R.string.errorGeneral, ex);
            }
            return false;
        }
    }
}

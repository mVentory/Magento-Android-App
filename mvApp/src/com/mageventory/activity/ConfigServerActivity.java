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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.BitmapfunUtils;
import com.mageventory.bitmapfun.util.ImageCacheUtils;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobService;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.LoadAttributeSetTaskAsync;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.ScanUtils;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.SingleFrequencySoundGenerator;
import com.mageventory.util.TrackerUtils;
import com.mageventory.util.WebUtils;
import com.mageventory.util.security.Security;
import com.mageventory.widget.ExpandingViewController;
import com.mventory.R;

public class ConfigServerActivity extends BaseFragmentActivity implements MageventoryConstants {

    public static final String TAG = ConfigServerActivity.class.getSimpleName();
    /**
     * The intent flag. If specified the new profile will be added after the
     * activity start. This includes profile data scanner launch
     */
    public static final String ADD_PROFILE_EXTRA = "ADD_PROFILE";
    /**
     * The intent flag which modifies scan profile data cancelled behaviour. If
     * specified then the starting activity will be launched in such case.
     * Usually it is WelcomeActivity
     */
    public static final String OPEN_STARTING_ACTIVITY_IF_SCAN_PROFILE_CANCELED_EXTRA = "OPEN_STARTING_ACTIVITY_IF_SCAN_PROFILE_CANCELED";

    /**
     * Key used for the user name in the configuration of the JSON format
     */
    public static final String CONFIG_USER_KEY = "user";
    /**
     * Key used for the user password in the configuration of the JSON format
     */
    public static final String CONFIG_PASSWORD_KEY = "password";
    /**
     * Key used for the server URL in the configuration of the JSON format
     */
    public static final String CONFIG_URL_KEY = "url";
    /**
     * Key used for the license key in the configuration of the JSON format
     */
    public static final String CONFIG_LICENSE_KEY = "license";
    /**
     * Key used for the license signature in the configuration of the JSON
     * format
     */
    public static final String CONFIG_SIGNATURE_KEY = "signature";

    private Settings settings;

    private boolean newProfileMode = false;

    private Button save_profile_button;
    private Button delete_button;

    private Button clear_cache;
    private Button wipe_data;
    private Button camera_sync_button;
    private Button save_global_settings_button;
    private Button queue_button;



    private EditText profileIdInput;
    private EditText userInput;
    private EditText passInput;
    private EditText urlInput;
    private EditText googleBookApiKeyInput;
    /**
     * Field to store active profile license information
     */
    private String mLicense;
    /**
     * Field to store active profile license signature
     */
    private String mSignature;
    /**
     * The view to set User-Agent string
     */
    private AutoCompleteTextView mWebViewUserAgentView;

    private CheckBox mEnableSoundCheckbox;
    private SeekBar mSoundVolumeSeekBar;
    /**
     * The touch indicator configurator
     */
    private TouchIndicatorSet mTouchIndicatorSet;

    public ConfigServerActivity() {
    }

    /**
     * The profile selection manager
     */
    ProfileSelectionManager mProfileSelectionManager;

    private TextView notWorkingTextView;

    DownloadConfigTask mDownloadConfigTask;

    private Intent mLastScanIntent;
    private Integer mLastScanRequestCode = null;
    private Integer mLastScanMessage = null;
    /**
     * The flag which indicates whether the add new profile flow should be
     * started when activity is resumed
     */
    private boolean mAddNewProfileOnResume;
    /**
     * The flag which modifies scan profile cancelled behavior. If true then the
     * config server activity will be closed and either Welcome or Home activity
     * will be opened depend on whether configured profiles exist
     */
    private boolean mOpenStartingActivityIfScanProfileCancelled;

    /*
     * Show a confirmation when clicking on one of the buttons for deleting the
     * cache so that the user knows that the button was clicked.
     */
    public void showCacheRemovedDialog() {
        if (isActivityAlive() == false)
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
            alert.setMessage(R.string.remove_new_profile_confirmation);
        }
        else
        {
            alert.setMessage(CommonUtils.getStringResource(R.string.remove_profile_confirmation,
                    settings.getProfileName()));
        }

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (newProfileMode == true)
                {
                    newProfileMode = false;
                    mProfileSelectionManager.setEnabled(true);

                    mProfileSelectionManager.refreshProfileList(false);
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

                    mProfileSelectionManager.refreshProfileList(false);
                }

                if (settings.getListOfStores(false).length == 0) {
                    cleanProfileFields();
                } else {
                    restoreProfileFields();
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
        if (!isActivityAlive())
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
        if (!isActivityAlive())
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
        googleBookApiKeyInput = (EditText) findViewById(R.id.google_book_api_input);
        mWebViewUserAgentView = (AutoCompleteTextView) findViewById(R.id.webViewUserAgent);

        mEnableSoundCheckbox = (CheckBox) findViewById(R.id.enable_sound_checkbox);
        mSoundVolumeSeekBar = (SeekBar) findViewById(R.id.soundVolumeSeekBar);

        mTouchIndicatorSet = new TouchIndicatorSet();

        notWorkingTextView = ((TextView) findViewById(R.id.not_working_text_view));
        settings = new Settings(getApplicationContext());

        save_profile_button = (Button) findViewById(R.id.save_profile_button);
        delete_button = (Button) findViewById(R.id.deletebutton);
        camera_sync_button = (Button) findViewById(R.id.cameraSync);

        clear_cache = (Button) findViewById(R.id.clearCacheButton);
        wipe_data = (Button) findViewById(R.id.wipeDataButton);
        save_global_settings_button = (Button) findViewById(R.id.save_global_settings_button);

        queue_button = (Button) findViewById(R.id.queueButton);

        queue_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfigServerActivity.this, QueueActivity.class);
                ConfigServerActivity.this.startActivity(intent);
            }
        });

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

        save_global_settings_button.setOnClickListener(saveGlobalSettingsButtonlistener);

        camera_sync_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(ConfigServerActivity.this, CameraTimeSyncActivity.class);
                ConfigServerActivity.this.startActivity(i);
            }
        });

        googleBookApiKeyInput.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                mLastScanIntent = ScanUtils.getScanActivityIntent();
                mLastScanIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                if (!ScanUtils.startScanActivityForResult(ConfigServerActivity.this,
                        mLastScanIntent, SCAN_QR_CODE, null, false)) {
                    mLastScanRequestCode = SCAN_QR_CODE;
                    mLastScanMessage = null;
                }

                return true;
            }
        });
        // adde the possible predefined values to the mWebViewUserAgentView
        // dropdown
        List<String> userAgents = new ArrayList<String>();
        userAgents.add(getString(R.string.desktop_user_agent));
        userAgents.add(WebUtils.getDefaultWebViewUserAgentString(ConfigServerActivity.this));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, userAgents);
        mWebViewUserAgentView.setAdapter(adapter);

        mProfileSelectionManager = new ProfileSelectionManager();
        mProfileSelectionManager.refreshProfileList(false);

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
                if (buttonView == mEnableSoundCheckbox) {
                    mSoundVolumeSeekBar.setEnabled(isChecked);
                }
            }
        };

        mEnableSoundCheckbox
                .setOnCheckedChangeListener(checkBoxListener);
        ((CheckBox) findViewById(R.id.new_products_enabled))
                .setOnCheckedChangeListener(checkBoxListener);
        ((CheckBox) findViewById(R.id.service_checkbox))
                .setOnCheckedChangeListener(checkBoxListener);
        mSoundVolumeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            SingleFrequencySoundGenerator beep;
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                beep = SingleFrequencySoundGenerator.playSuccessfulBeep(beep, getSoundVolume());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
        });
        checkBoxListener.onCheckedChanged(mEnableSoundCheckbox, mEnableSoundCheckbox.isChecked());
        // initialize expanding view controller for the global settings
        new ExpandingViewController(findViewById(R.id.globalSettings),
                R.id.expandCollapseController, R.id.expandingView, R.id.expandIndicator,
                R.id.collapseIndicator);
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
            } else {
                // if there is scheduled configuration scan and Zxing is still
                // not installed
                if (mLastScanRequestCode == SCAN_CONFIG_DATA) {
                    startFirstActivityIfNecessary();
                }
            }
            mLastScanRequestCode = null;
            mLastScanMessage = null;
        }
        if (mAddNewProfileOnResume) {
            mAddNewProfileOnResume = false;
            // emulate new profile button clicked event
            mProfileSelectionManager.onNewProfileButtonClicked();
        }
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
        mLicense = null;
        mSignature = null;

        notWorkingTextView.setVisibility(View.GONE);
        save_profile_button.setEnabled(false);
    }


    private void restoreGlobalSettingsFields()
    {
        String key = settings.getAPIkey();
        String galleryPath = settings.getGalleryPhotosDirectory();
        String errorReportRecipient = settings.getErrorReportRecipient();
        String maxImageWidth = settings.getMaxImageWidth();
        String maxImageHeight = settings.getMaxImageHeight();
        boolean soundEnabled = settings.getSoundCheckBox();
        int soundVolume = Math.round(settings.getSoundVolume() * 100);
        boolean newProductsEnabled = settings.getNewProductsEnabledCheckBox();
        boolean serviceCheckboxChecked = settings.getServiceCheckBox();

        googleBookApiKeyInput.setText(key);
        mWebViewUserAgentView.setText(settings.getWebViewUserAgent());
        ((EditText) findViewById(R.id.gallery_photos_directory_input)).setText(galleryPath);
        ((EditText) findViewById(R.id.error_report_recipient_input)).setText(errorReportRecipient);
        ((EditText) findViewById(R.id.max_image_width_px)).setText(maxImageWidth);
        ((EditText) findViewById(R.id.max_image_height_px)).setText(maxImageHeight);
        mEnableSoundCheckbox.setChecked(soundEnabled);
        mSoundVolumeSeekBar.setProgress(soundVolume);
        mTouchIndicatorSet.restoreSettings();
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
        mLicense = settings.getLicense();
        mSignature = settings.getSignature();

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
        @Override
        public void onClick(View v) {
            String apiKey = googleBookApiKeyInput.getText()
                    .toString();
            String galleryPath = ((EditText) findViewById(R.id.gallery_photos_directory_input))
                    .getText().toString();
            String errorReportRecipient = ((EditText) findViewById(R.id.error_report_recipient_input))
                    .getText().toString();
            String maxImageWidth = ((EditText) findViewById(R.id.max_image_width_px)).getText()
                    .toString();
            String maxImageHeight = ((EditText) findViewById(R.id.max_image_height_px)).getText()
                    .toString();
            boolean sound = mEnableSoundCheckbox.isChecked();
            float volume = getSoundVolume();
            boolean productsEnabled = ((CheckBox) findViewById(R.id.new_products_enabled))
                    .isChecked();
            boolean serviceCheckboxChecked = ((CheckBox) findViewById(R.id.service_checkbox))
                    .isChecked();

            if (!galleryPath.startsWith("/")) {
                galleryPath = "/" + galleryPath;
            }

            if (!new File(galleryPath).exists()) {
                GuiUtils.alert("Gallery photos directory does not exist. Settings not saved.");

                return;
            } else if (errorReportRecipient.length() > 0 && !isEmailValid(errorReportRecipient)) {
                GuiUtils.alert("Email address invalid. Settings not saved.");

                return;
            } else {
                ((EditText) findViewById(R.id.gallery_photos_directory_input)).setText(galleryPath);

                settings.setAPIkey(apiKey);
                if (TextUtils.isEmpty(settings.getAPIkey())) {
                    GuiUtils.alert("No Google Books API -- Book Search Feature Will be Disabled. Settings saved.");
                } else {
                    GuiUtils.alert("Settings saved.");
                }
            }

            settings.setWebViewUserAgent(mWebViewUserAgentView.getText().toString());
            settings.setGalleryPhotosDirectory(galleryPath);
            settings.setErrorReportRecipient(errorReportRecipient);
            settings.setMaxImageHeight(maxImageHeight);
            settings.setMaxImageWidth(maxImageWidth);
            settings.setSoundCheckBox(sound);
            settings.setSoundVolume(volume);
            mTouchIndicatorSet.saveSettings();
            settings.setNewProductsEnabledCheckBox(productsEnabled);
            settings.setServiceCheckBox(serviceCheckboxChecked);

            if (serviceCheckboxChecked) {
                JobService.wakeUp(ConfigServerActivity.this);
            }

            ConfigServerActivity.this.hideKeyboard();
            startMain();
        }

    };

    private OnClickListener saveProfileButtonlistener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            validateAndSaveProfile(true);
        }

    };

    /**
     * Validate and save profile information
     * 
     * @param startHomeOnSuccessValidation whether to start home activity on
     *            successful validation
     */
    private void validateAndSaveProfile(boolean startHomeOnSuccessValidation) {
        validateAndSaveProfile(startHomeOnSuccessValidation, false);
    }

    /**
     * Validate and save profile information
     * 
     * @param startHomeOnSuccessValidation whether to start home activity on
     *            successful validation
     * @param downloadedConfigValidation whether it is the downloaded
     *            configuration file check
     * @return true if local data validation passed, false otherwise
     */
    private boolean validateAndSaveProfile(boolean startHomeOnSuccessValidation,
            boolean downloadedConfigValidation) {
        String user = userInput.getText().toString();
        String pass = passInput.getText().toString();
        String url = urlInput.getText().toString();
        String profileID = profileIdInput.getText().toString();
        long profileIDLong;

        if (profileID.length() == 0) {
            if (!downloadedConfigValidation) {
                // show message only if it is not a downloaded config file
                // validation
                GuiUtils.alert(R.string.pleaseProvideProfileId);
            }
            return false;
        }

        try {
            profileIDLong = Long.parseLong(profileID);
        } catch (NumberFormatException e) {
            if (!downloadedConfigValidation) {
                // show message only if it is not a downloaded config file
                // validation
                GuiUtils.alert(R.string.profileIdMustBeInteger);
            }
            return false;
        }

        if (settings.isProfileIDTaken(profileIDLong)
                && (newProfileMode == true || (newProfileMode == false && settings.getProfileID() != profileIDLong))) {
            if (!downloadedConfigValidation) {
                // show message only if it is not a downloaded config file
                // validation
                GuiUtils.alert(R.string.profileIdAlreadyTaken);
            }
            return false;
        }

        if (user.length() == 0) {
            if (!downloadedConfigValidation) {
                // show message only if it is not a downloaded config file
                // validation
                GuiUtils.alert(R.string.pleaseProvideUserName);
            }
            return false;
        }

        if (pass.length() == 0) {
            if (!downloadedConfigValidation) {
                // show message only if it is not a downloaded config file
                // validation
                GuiUtils.alert(R.string.pleaseProvidePassword);
            }
            return false;
        }

        if (url.length() == 0) {
            if (!downloadedConfigValidation) {
                // show message only if it is not a downloaded config file
                // validation
                GuiUtils.alert(R.string.pleaseProvideStoreUrl);
            }
            return false;
        }
//        TODO license check currently disabled
//        if (TextUtils.isEmpty(mLicense) || TextUtils.isEmpty(mSignature)) {
//            // if license or signature are missing
//            GuiUtils.alert(R.string.missing_license_information);
//            return;
//        }

        if (!url.matches("^" + ImageUtils.PROTO_PREFIX + ".*")) {
            url = HTTP_PROTO_PREFIX + url;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        if (!Security.verifyLicenseAndStore(url, mLicense, mSignature, false)) {
            // if license verification failured
            return false;
        }
        TestingConnection tc = new TestingConnection(startHomeOnSuccessValidation, newProfileMode,
                downloadedConfigValidation,
                new SettingsSnapshot(url, user, pass, profileIDLong, mLicense, mSignature));
        tc.execute(new String[] {});
        hideKeyboard();
        profileModified();
        return true;
    }

    private OnClickListener deleteButtonlistener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ConfigServerActivity.this.hideKeyboard();
            showRemoveProfileQuestion();
        }
    };

    private void addNewProfile() {
        newProfileMode = true;

        mProfileSelectionManager.refreshProfileList(true);

        mProfileSelectionManager.setEnabled(false);

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

    @Override
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
                googleBookApiKeyInput.setText(contents);
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
            } else if (resultCode == RESULT_CANCELED) {
                // if scan config is cancelled
                startFirstActivityIfNecessary();
            }
        }
    }

    /**
     * Start the first activity if mOpenStartingActivityIfScanProfileCancelled
     * flag is true and finish config activity
     */
    public void startFirstActivityIfNecessary() {
        if (mOpenStartingActivityIfScanProfileCancelled) {
            startFirstActivity();
        }
    }

    /**
     * Start the first activity and finish current activity
     */
    public void startFirstActivity() {
        LaunchActivity.startFirstActivity(ConfigServerActivity.this, settings);
        finish();
    }

    private boolean parseConfig(String contents) {
        JSONObject jsonObject = null;
        // try to parse content as JSON first
        try {
            jsonObject = new JSONObject(contents);
        } catch (JSONException ex) {
            CommonUtils.error(TAG, ex);
        }
        if (jsonObject != null) {
            CommonUtils.debug(TAG, "paseConfig: config is of JSON format");
            // if passed content is of JSON format and it was parsed
            // successfully
            userInput.setText(jsonObject.optString(CONFIG_USER_KEY));
            passInput.setText(jsonObject.optString(CONFIG_PASSWORD_KEY));
            urlInput.setText(jsonObject.optString(CONFIG_URL_KEY));
            mLicense = jsonObject.optString(CONFIG_LICENSE_KEY);
            mSignature = jsonObject.optString(CONFIG_SIGNATURE_KEY);
            return true;
        } else {
            CommonUtils.debug(TAG, "paseConfig: config is plain text format");
            String[] lines = contents.split("\\r?\\n");
            int length = lines.length;
            if (length >= 3 && length <= 6) {
                int ind = 0;
                userInput.setText(lines[ind++]);
                passInput.setText(lines[ind++]);
                urlInput.setText(lines[ind++]);
                if (ind < length) {
                    mLicense = lines[ind++];
                } else {
                    mLicense = null;
                }
                if (ind < length) {
                    mSignature = lines[ind++];
                } else {
                    mSignature = null;
                }
                if (ind < length) {
                    String apiKey = lines[ind++];
                    googleBookApiKeyInput.setText(apiKey);
                    if (!TextUtils.isEmpty(apiKey)) {
                        settings.setAPIkey(apiKey);
                    }
                }
                return true;
            } else {
                CommonUtils.debug(TAG, "paseConfig: config is invalid");
            }
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
        // reset flags
        mAddNewProfileOnResume = false;
        mOpenStartingActivityIfScanProfileCancelled = false;
        if (intent != null && intent.getData() != null) {
            Uri uri = intent.getData();
            if (uri != null) {
                String mventorySchema = CommonUtils
                        .getStringResource(R.string.settings_content_uri_path_schema);
                if (uri.getScheme().startsWith(mventorySchema)) {
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
            if (intent.hasExtra(OPEN_STARTING_ACTIVITY_IF_SCAN_PROFILE_CANCELED_EXTRA)) {
                mOpenStartingActivityIfScanProfileCancelled = true;
            }
        }
        if (!processed && !settings.hasSettings()) {
            mAddNewProfileOnResume = true;
        }
    }

    public float getSoundVolume() {
        float volume = (float) mSoundVolumeSeekBar.getProgress() / 100;
        return volume;
    }

    @Override
    protected void verifyLicense() {
        // do nothing
    }
    
    /**
     * Show the invalid configuration is passed to download dialog. The dialog
     * has special behaviour on dismiss to close this activity and start the
     * Main activity
     */
    void showInvalidConfigWebDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.errorInvalidConfigWeb);
        // handle dialog closing
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startFirstActivity();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            
            @Override
            public void onCancel(DialogInterface dialog) {
                startFirstActivity();
            }
        });
        builder.show();
    }
    /**
     * The class which handles touch indicator settings
     * 
     * @author Eugene Popovich
     */
    class TouchIndicatorSet {
        /**
         * The touch indicator enabled checkbox
         */
        private CheckBox mTouchIndicatorEnabledCheckbox;
        /**
         * The touch indicator radius selection seek bar
         */
        private SeekBar mTouchIndicatorRadius;
        /**
         * The touch indicator line width selection seek bar
         */
        private SeekBar mTouchIndicatorLineWidth;
        /**
         * The minimum allowed touch indicator radius
         */
        private float mMinTouchIndicatorRadius;
        /**
         * The maximum allowed touch indicator radius
         */
        private float mMaxTouchIndicatorRadius;
        /**
         * The minimum allowed touch indicator line width
         */
        private float mMinTouchIndicatorLineWidth;
        /**
         * The maximum allowed touch indicator line width
         */
        private float mMaxTouchIndicatorLineWidth;
    
        TouchIndicatorSet() {
            // initialize views
            mTouchIndicatorEnabledCheckbox = (CheckBox) findViewById(R.id.touchIndicatorEnabled);
            mTouchIndicatorRadius = (SeekBar) findViewById(R.id.touchIndicatorRadius);
            mTouchIndicatorLineWidth = (SeekBar) findViewById(R.id.touchIndicatorLineWidth);
            // initialize dimensions
            mMinTouchIndicatorRadius = getResources().getDimensionPixelSize(
                    R.dimen.min_touch_indicator_radius);
            mMaxTouchIndicatorRadius = getResources().getDimensionPixelSize(
                    R.dimen.max_touch_indicator_radius);
            mMinTouchIndicatorLineWidth = getResources().getDimensionPixelSize(
                    R.dimen.min_touch_indicator_line_width);
            mMaxTouchIndicatorLineWidth = getResources().getDimensionPixelSize(
                    R.dimen.max_touch_indicator_line_width);
    
            OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ConfigServerActivity.this.hideKeyboard();
                    // adjust enabled state of the touch indicator configurators
                    mTouchIndicatorRadius.setEnabled(isChecked);
                    mTouchIndicatorLineWidth.setEnabled(isChecked);
                    // fire touch indicator settings changed event
                    touchIndicatorSettingsChanged();
                }
            };
    
            mTouchIndicatorEnabledCheckbox.setOnCheckedChangeListener(checkBoxListener);
    
            OnSeekBarChangeListener touchIndicatorChangeListener = new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
    
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
    
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    touchIndicatorSettingsChanged();
                }
            };
            mTouchIndicatorRadius.setOnSeekBarChangeListener(touchIndicatorChangeListener);
            mTouchIndicatorLineWidth.setOnSeekBarChangeListener(touchIndicatorChangeListener);
            checkBoxListener.onCheckedChanged(mTouchIndicatorEnabledCheckbox,
                    mTouchIndicatorEnabledCheckbox.isChecked());
    
        }
    
        /**
         * Restore the touch indicator settings
         */
        void restoreSettings() {
            mTouchIndicatorEnabledCheckbox.setChecked(settings.isTouchIndicatorEnabled());
            float touchIndicatorRadius = (settings.getTouchIndicatorRadius() - mMinTouchIndicatorRadius)
                    / (mMaxTouchIndicatorRadius - mMinTouchIndicatorRadius) * 100;
            mTouchIndicatorRadius.setProgress((int) touchIndicatorRadius);
            float touchIndicatorLineWidth = (settings.getTouchIndicatorLineWidth() - mMinTouchIndicatorLineWidth)
                    / (mMaxTouchIndicatorLineWidth - mMinTouchIndicatorLineWidth) * 100;
            mTouchIndicatorLineWidth.setProgress((int) touchIndicatorLineWidth);
        }
    
        /**
         * Save the touch indicator settings
         */
        void saveSettings() {
            settings.setTouchIndicatorEnabled(mTouchIndicatorEnabledCheckbox.isChecked());
            settings.setTouchIndicatorRadius(getTouchIndicatorRadius());
            settings.setTouchIndicatorLineWidth(getTouchIndicatorLineWidth());
    
            EventBusUtils.sendGeneralEventBroadcast(EventType.TOUCH_INDICATOR_SETTINGS_UPDATED);
        }

        /**
         * Get the currently selected touch indicator radius
         * 
         * @return
         */
        public float getTouchIndicatorRadius() {
            float result = mMinTouchIndicatorRadius + (float) mTouchIndicatorRadius.getProgress()
                    / 100 * (mMaxTouchIndicatorRadius - mMinTouchIndicatorRadius);
            return result;
        }
    
        /**
         * Get the currently selected touch indicator line width
         * 
         * @return
         */
        public float getTouchIndicatorLineWidth() {
            float result = mMinTouchIndicatorLineWidth
                    + (float) mTouchIndicatorLineWidth.getProgress() / 100
                    * (mMaxTouchIndicatorLineWidth - mMinTouchIndicatorLineWidth);
            return result;
        }
    
        /**
         * Method to call when touch indicator configuration widgets adjusted
         */
        public void touchIndicatorSettingsChanged() {
            touchIndicatorView.reinit(mTouchIndicatorEnabledCheckbox.isChecked(),
                    getTouchIndicatorLineWidth(), getTouchIndicatorRadius());
            touchIndicatorView.invalidate();
        }
    }

    private class TestingConnection extends AsyncTask<String, Integer, Boolean> {
        /**
         * The progress indication dialog
         */
        ProgressDialog pDialog;
        /**
         * The instance of the magento client
         */
        private MagentoClient client;
        /**
         * The flag which controls whether to start home activity on successful
         * validation
         */
        boolean mStartHomeOnSuccessValidation;
        /**
         * Flag indicating whether user entered new profile details and
         *            checks them
         */
        boolean mNewProfileMode;
        /**
         * The settings snapshot
         */
        SettingsSnapshot mSettingsSnapshot;
        /**
         * Flag to save profile validation result
         */
        boolean mProfileValid = false;
        /**
         * Flag indicating whether it is the downloaded configuration file check
         */
        boolean mDownloadedConfigValidation;

        /**
         * @param startHomeOnSuccessValidation whether to start home activity on
         *            successful validation
         * @param newProfileMode whether user entered new profile details and
         *            checks them
         * @param downloadedConfigValidation whether it is the downloaded
         *            configuration file check
         * @param settingsSnapshot the settings snapshot
         */
        TestingConnection(boolean startHomeOnSuccessValidation, boolean newProfileMode,
                boolean downloadedConfigValidation,
                SettingsSnapshot settingsSnapshot) {
            mStartHomeOnSuccessValidation = startHomeOnSuccessValidation;
            mNewProfileMode = newProfileMode;
            mDownloadedConfigValidation = downloadedConfigValidation;
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

            if (!checkSettingsSnapshot()) {
                return false;
            }
            if (!mProfileValid
                    && !mSettingsSnapshot.getUrl().endsWith(POSSIBLE_GENERAL_PATH_SUFFIX)) {
                mSettingsSnapshot.setUrl(mSettingsSnapshot.getUrl() + POSSIBLE_GENERAL_PATH_SUFFIX);
                checkSettingsSnapshot();
            }
            return mProfileValid;
        }

        public Boolean checkSettingsSnapshot() {
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
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            pDialog.dismiss();
            if (result) {
                GuiUtils.alert("Profile configured.");
            } else {
                if (isActivityAlive()) {
                    if (mDownloadedConfigValidation) {
                        // if downloaded config validation failed
                        showInvalidConfigWebDialog();
                    } else {
                        GuiUtils.showMessageDialog(null, R.string.error_profile_validation_failed,
                                ConfigServerActivity.this);
                    }
                }
            }
            if (mProfileValid)
            {
                if (mNewProfileMode) {
                    settings.addStore(mSettingsSnapshot.getUrl());
                    settings.switchToStoreURL(mSettingsSnapshot.getUrl());
                    mProfileSelectionManager.setEnabled(true);

                    newProfileMode = false;
                }

                settings.setUrl(mSettingsSnapshot.getUrl());
                settings.setUser(mSettingsSnapshot.getUser());
                settings.setPass(mSettingsSnapshot.getPassword());
                settings.setProfileID(mSettingsSnapshot.getProfileID());
                settings.setLicense(mSettingsSnapshot.getLicense());
                settings.setSignature(mSettingsSnapshot.getSignature());
                settings.setProfileDataValid(true);
                LoadAttributeSetTaskAsync.loadAttributes(mSettingsSnapshot, false);
                EventBusUtils.sendGeneralEventBroadcast(EventType.PROFILE_CONFIGURED);
                if (mStartHomeOnSuccessValidation && isActivityAlive()) {
                    startMain();
                    return;
                }
                notWorkingTextView.setVisibility(View.GONE);
                mProfileSelectionManager.refreshProfileList(false);
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

        @Override
        public void startLoading() {
            super.startLoading();
            showProgress();
        }

        @Override
        public void stopLoading() {
            super.stopLoading();
            dismissProgress();
        }

        public void showProgress() {
            if (!isActivityAlive()) {
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
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            if (isCancelled()) {
                return;
            }
            if (isActivityAlive()) {
                showInvalidConfigWebDialog();
            }
        }

        @Override
        protected void onSuccessPostExecute() {
            if (isCancelled()) {
                return;
            }
            if (isActivityAlive()) {
                if (!parseConfig(mContent) || !validateAndSaveProfile(true, true)) {
                    // if config was not parsed successfully or the local
                    // profile validation failed (empty fields)
                    showInvalidConfigWebDialog();
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
                    urlConnection = (HttpURLConnection) WebUtils.openConnection(url);
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
                CommonUtils.error(TAG, ex);
            }
            return false;
        }
    }

    class ProfileSelectionManager {
        /**
         * The list which displays available for the selection profiles
         */
        private ListView mProfilesList;
        /**
         * The view to show current active profile
         */
        private TextView mCurrentProfile;
        /**
         * The add new profile button reference
         */
        private View mAddNewProfileButton;

        /**
         * The expanding view controller for the profile selection views
         */
        private ExpandingViewController mExpandingViewController;

        ProfileSelectionManager() {
            mCurrentProfile = (TextView) findViewById(R.id.activeProfile);
            mProfilesList = (ListView) findViewById(R.id.profilesList);
            mAddNewProfileButton = findViewById(R.id.newbutton);
            // initialize expanding view controller for the profile selector
            mExpandingViewController = new ExpandingViewController(
                    findViewById(R.id.profileSelector),
                    R.id.expandCollapseController, R.id.expandingView, R.id.expandIndicator,
                    R.id.collapseIndicator);

            mProfilesList.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position < settings.getStoresCount() && position >= 0) {
                        String url = ((StoreUrlWrapper) mProfilesList.getAdapter()
                                .getItem(position)).url;
                        settings.switchToStoreURL(url);
                        restoreProfileFields();
                        ConfigServerActivity.this.hideKeyboard();
                        profileModified();
                        refreshProfileList(false);
                        mExpandingViewController.collapseWithAnimation();
                    }
                }
            });

            mAddNewProfileButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    onNewProfileButtonClicked();
                }
            });
        }

        /**
         * Refresh the profile list
         * 
         * @param withNewOption whether the new profile is creating now
         */
        void refreshProfileList(boolean withNewOption) {
            String[] storeUrls = settings.getListOfStores(false);
            // initialize the array adapter data
            List<StoreUrlWrapper> stores = new ArrayList<StoreUrlWrapper>(storeUrls.length);
            for (String storeUrl : storeUrls) {
                // iterate through store URLs, wrap them with the wrapper and
                // add to the list adapter data container
                if (!withNewOption && storeUrl.equals(settings.getCurrentStoreUrl())) {
                    // skip current active profile. Do not add it to the
                    // available profiles list
                    continue;
                }
                stores.add(new StoreUrlWrapper(storeUrl));
            }
            ArrayAdapter<StoreUrlWrapper> arrayAdapter = new ArrayAdapter<StoreUrlWrapper>(
                    ConfigServerActivity.this,
                    R.layout.server_config_profile_item, android.R.id.text1, stores);

            mProfilesList.setAdapter(arrayAdapter);

            if (withNewOption) {
                mCurrentProfile.setText(R.string.new_profile);
            } else {
                mCurrentProfile.setText(Settings.getProfileName(settings.getCurrentStoreUrl()));
            }

            save_profile_button.setEnabled(false);

            if (withNewOption) {
                mAddNewProfileButton.setEnabled(false);
                delete_button.setEnabled(true);
                clear_cache.setEnabled(false);
            } else {
                mAddNewProfileButton.setEnabled(true);

                if (settings.getStoresCount() > 0) {
                    delete_button.setEnabled(true);
                    clear_cache.setEnabled(true);
                } else {
                    delete_button.setEnabled(false);
                    clear_cache.setEnabled(false);
                }
            }

            if (withNewOption == false && settings.getStoresCount() == 0) {
                profileIdInput.setEnabled(false);
                userInput.setEnabled(false);
                passInput.setEnabled(false);
                urlInput.setEnabled(false);
            } else {
                profileIdInput.setEnabled(true);
                userInput.setEnabled(true);
                passInput.setEnabled(true);
                urlInput.setEnabled(true);
            }
        }

        public void setEnabled(boolean b) {
            // do nothing
            // TODO remove this later
        }

        /**
         * The action which should be performed when the new profile button is
         * clicked
         */
        public void onNewProfileButtonClicked() {
            addNewProfile();

            // Pop a scanner as a default to scan a QR code with a list of
            // values for the config.
            mLastScanIntent = ScanUtils.getScanActivityIntent();
            mLastScanIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            if (!ScanUtils.startScanActivityForResult(ConfigServerActivity.this, null,
                    mLastScanIntent, SCAN_CONFIG_DATA, R.string.scan_configuration_code, null,
                    new Runnable() {

                        @Override
                        public void run() {
                            // if scanner is not installed and user cancels
                            // install
                            startFirstActivityIfNecessary();
                        }
                    }, false)) {
                mLastScanRequestCode = SCAN_CONFIG_DATA;
                mLastScanMessage = R.string.scan_configuration_code;
            }
            mExpandingViewController.collapseWithAnimation();
        }

        /**
         * The toString wrapper around store URL to show human readable
         * store/profile name
         */
        class StoreUrlWrapper {
            /**
             * The store URL
             */
            String url;

            /**
             * @param url The store URL
             */
            public StoreUrlWrapper(String url) {
                super();
                this.url = url;
            }

            @Override
            public String toString() {
                // get the human readable profile name from URL
                return Settings.getProfileName(url);
            }
        }
    }
}

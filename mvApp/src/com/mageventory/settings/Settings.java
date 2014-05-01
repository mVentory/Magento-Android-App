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

package com.mageventory.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Environment;
import android.text.TextUtils;

import com.mageventory.R;
import com.mageventory.job.JobCacheManager;
import com.mageventory.util.CommonUtils;

public class Settings {

    private static final String TAG = Settings.class.getSimpleName();

    private static String sDefaultGalleryPhotoPath;

    /* Store specific keys. */
    private static final String PROFILE_ID = "profile_id";
    private static final String USER_KEY = "user";
    private static final String PASS_KEY = "pass";
    private static final String URL_KEY = "url";
    private static final String PROFILE_DATA_VALID = "profile_data_valid";
    private static final String MAX_IMAGE_WIDTH_KEY = "image_width";
    private static final String MAX_IMAGE_HEIGHT_KEY = "image_height";

    /* Keys that are common for all stores and are stored in a common file. */
    private static final String NEW_PRODUCTS_ENABLED_KEY = "new_products_enabled_key";
    private static final String GOOGLE_BOOK_API_KEY = "api_key";
    private static final String SOUND_CHECKBOX_KEY = "sound_checkbox";
    private static final String SOUND_VOLUME_KEY = "sound_volume";
    private static final String SERVICE_CHECKBOX_KEY = "service_checkbox";
    private static final String CAMERA_TIME_DIFFERENCE_SECONDS_KEY = "camera_time_difference_seconds";
    private static final String CAMERA_LAST_SYNC_TIME_KEY = "camera_last_sync_time";
    private static final String CAMERA_TIME_DIFFERENCE_ASSIGNED = "camera_time_difference_assigned";
    private static final String LIST_OF_STORES_KEY = "list_of_stores";
    private static final String CURRENT_STORE_KEY = "current_store_key";
    private static final String NEXT_PROFILE_ID_KEY = "next_profile_id";
    private static final String GALLERY_PHOTOS_DIRECTORY_KEY = "gallery_photos_directory";
    private static final String ERROR_REPORT_RECIPIENT_KEY = "error_report_recipient";

    private static String listOfStoresFileName = "list_of_stores.dat";
    private static final String NEW_MODE_STRING = "New profile";
    private static final String NO_STORE_IS_CURRENT = "no store is current";

    private static final String DEFAULT_ERROR_REPORT_RECIPIENT = "info@mventory.com";

    private SharedPreferences settings;

    private Context context;

    OnSharedPreferenceChangeListener listOfStorePreferenceChangeListener;

    public SharedPreferences getStoresPreferences() {
        return context.getSharedPreferences(listOfStoresFileName, Context.MODE_PRIVATE);
    }

    public void switchToStoreURL(String url)
    {
        SharedPreferences storesPreferences = getStoresPreferences();

        if (url != null)
        {
            settings = context.getSharedPreferences(JobCacheManager.encodeURL(url),
                    Context.MODE_PRIVATE);
        }

        Editor e = storesPreferences.edit();
        e.putString(CURRENT_STORE_KEY, url);
        e.commit();
        JobCacheManager.killRAMCachedProductDetails();
    }

    public String[] getListOfStores(boolean newMode)
    {
        SharedPreferences storesPreferences = getStoresPreferences();

        String storesString = storesPreferences.getString(LIST_OF_STORES_KEY, null);

        if (storesString == null)
        {
            if (newMode)
            {
                return new String[] {
                    NEW_MODE_STRING
                };
            }
            else
            {
                return new String[0];
            }
        }

        if (newMode)
        {
            return (storesString + "\n" + NEW_MODE_STRING).split("\n");
        }
        else
        {
            return storesString.split("\n");
        }
    }

    public int getStoresCount()
    {
        return getListOfStores(false).length;
    }

    public void addStore(String url)
    {
        if (storeExists(url))
        {
            return;
        }

        SharedPreferences storesPreferences = getStoresPreferences();

        String storesString = storesPreferences.getString(LIST_OF_STORES_KEY, null);

        if (storesString == null)
        {
            storesString = url;
        }
        else
        {
            storesString = storesString + "\n" + url;
        }

        Editor e = storesPreferences.edit();
        e.putString(LIST_OF_STORES_KEY, storesString);
        e.commit();
    }

    public void removeStore(String url)
    {
        SharedPreferences storesPreferences = getStoresPreferences();

        String storesString = null;
        String[] storesList = getListOfStores(false);

        for (int i = 0; i < storesList.length; i++)
        {
            if (!storesList[i].equals(url))
            {
                if (storesString == null)
                {
                    storesString = storesList[i];
                }
                else
                {
                    storesString = storesString + "\n" + storesList[i];
                }
            }
            else
            {
                SharedPreferences settingsToRemove = context.getSharedPreferences(
                        JobCacheManager.encodeURL(url), Context.MODE_PRIVATE);
                Editor edit = settingsToRemove.edit();
                edit.clear();
                edit.commit();
            }
        }

        Editor e = storesPreferences.edit();
        e.putString(LIST_OF_STORES_KEY, storesString);
        e.commit();
    }

    public boolean storeExists(String url)
    {
        String[] storesList = getListOfStores(false);

        for (int i = 0; i < storesList.length; i++)
        {
            if (storesList[i].equals(url))
            {
                return true;
            }
        }

        return false;
    }

    public String getCurrentStoreUrl()
    {
        SharedPreferences storesPreferences = getStoresPreferences();

        return storesPreferences.getString(CURRENT_STORE_KEY, NO_STORE_IS_CURRENT);
    }

    public int getCurrentStoreIndex()
    {
        String[] stores = getListOfStores(false);
        String currentStore = getCurrentStoreUrl();

        for (int i = 0; i < stores.length; i++)
        {
            if (stores[i].equals(currentStore))
                return i;
        }

        return -1;
    }

    /* Are there any stores with this profile id? */
    public boolean isProfileIDTaken(long profileID)
    {
        String[] listOfStores = getListOfStores(false);

        for (int i = 0; i < listOfStores.length; i++)
        {
            SharedPreferences storeFile = context.getSharedPreferences(
                    JobCacheManager.encodeURL(listOfStores[i]), Context.MODE_PRIVATE);

            if (storeFile.getLong(PROFILE_ID, -1) == profileID)
            {
                return true;
            }
        }

        return false;
    }

    /* Generate a profile id that is not taken by any store. */
    public long getNextProfileID()
    {
        SharedPreferences storesPreferences = getStoresPreferences();

        long nextProfileID = storesPreferences.getLong(NEXT_PROFILE_ID_KEY, 0);

        while (isProfileIDTaken(nextProfileID))
        {
            nextProfileID++;
        }

        Editor e = storesPreferences.edit();
        e.putLong(NEXT_PROFILE_ID_KEY, nextProfileID + 1);
        e.commit();

        return nextProfileID;
    }

    public static class ProfileIDNotFoundException extends Exception
    {
        private static final long serialVersionUID = -9041230111429421043L;
    }

    /**
     * @param act The context from which to pick SharedPreferences
     */
    public Settings(Context act) {
        context = act;

        settings = act.getSharedPreferences(
                JobCacheManager.encodeURL(getCurrentStoreUrl()), Context.MODE_PRIVATE);
    }

    public Settings(Context act, String url) {
        context = act;

        settings = act.getSharedPreferences(
                JobCacheManager.encodeURL(url), Context.MODE_PRIVATE);
    }

    public Settings(Context act, long profileID) throws ProfileIDNotFoundException {
        context = act;

        String[] storeURLs = getListOfStores(false);

        for (String url : storeURLs)
        {
            SharedPreferences sp = act.getSharedPreferences(
                    JobCacheManager.encodeURL(url), Context.MODE_PRIVATE);

            long pid = sp.getLong(PROFILE_ID, -1);

            if (profileID == pid)
            {
                settings = sp;
            }
        }

        if (settings == null)
        {
            throw new ProfileIDNotFoundException();
        }
    }

    public void clearCameraTimeDifferenceInformation() {
        /* Save the time difference in the file that is common for all stores. */
        SharedPreferences storesPreferences = getStoresPreferences();

        Editor editor = storesPreferences.edit();
        editor.remove(CAMERA_TIME_DIFFERENCE_SECONDS_KEY);
        editor.remove(CAMERA_TIME_DIFFERENCE_ASSIGNED);
        editor.remove(CAMERA_LAST_SYNC_TIME_KEY);
        editor.commit();
    }

    /**
     * @param timeDiff difference between camera and encoded device time
     * @param cameraLastSyncTime the encoded device time
     */
    public void setCameraTimeDifference(int timeDiff, Date cameraLastSyncTime) {
        /* Save the time difference in the file that is common for all stores. */
        SharedPreferences storesPreferences = getStoresPreferences();

        Editor editor = storesPreferences.edit();
        editor.putInt(CAMERA_TIME_DIFFERENCE_SECONDS_KEY, timeDiff);
        editor.putBoolean(CAMERA_TIME_DIFFERENCE_ASSIGNED, true);
        editor.putLong(CAMERA_LAST_SYNC_TIME_KEY, cameraLastSyncTime.getTime());
        editor.commit();
    }

    /**
     * Get the time of last camera synchronization
     * 
     * @return
     */
    public Date getCameraLastSyncTime() {
        SharedPreferences storesPreferences = getStoresPreferences();
        long time = storesPreferences.getLong(CAMERA_LAST_SYNC_TIME_KEY, 0);
        return new Date(time);
    }
    
    /**
     * Check whether camera time difference is asssigned (whether
     * setCameraTimeDifference was called at least once)
     * 
     * @return
     */
    public boolean isCameraTimeDifferenceAssigned() {
        SharedPreferences storesPreferences = getStoresPreferences();
        return storesPreferences.getBoolean(CAMERA_TIME_DIFFERENCE_ASSIGNED, false);
    }

    public int getCameraTimeDifference() {
        /* Get the time difference from the file that is common for all stores. */
        SharedPreferences storesPreferences = getStoresPreferences();
        return storesPreferences.getInt(CAMERA_TIME_DIFFERENCE_SECONDS_KEY, 0);
    }

    public void setUser(String user) {
        Editor editor = settings.edit();
        editor.putString(USER_KEY, user);
        editor.commit();
    }

    public String getUser() {
        return settings.getString(USER_KEY, "");
    }

    public String getPass() {
        return settings.getString(PASS_KEY, "");
    }

    public void setPass(String pass) {
        Editor editor = settings.edit();
        editor.putString(PASS_KEY, pass);
        editor.commit();
    }

    public String getUrl() {
        return settings.getString(URL_KEY, "");
    }

    public void setUrl(String url) {
        Editor editor = settings.edit();
        editor.putString(URL_KEY, url);
        editor.commit();
    }

    public long getProfileID()
    {
        return settings.getLong(PROFILE_ID, -1);
    }

    public void setProfileID(long profileID) {
        Editor editor = settings.edit();
        editor.putLong(PROFILE_ID, profileID);
        editor.commit();
    }

    public String getMaxImageWidth() {
        return settings.getString(MAX_IMAGE_WIDTH_KEY, "");
    }

    public void setMaxImageWidth(String width) {
        Editor editor = settings.edit();
        editor.putString(MAX_IMAGE_WIDTH_KEY, width);
        editor.commit();
    }

    public String getMaxImageHeight() {
        return settings.getString(MAX_IMAGE_HEIGHT_KEY, "");
    }

    public void setMaxImageHeight(String height) {
        Editor editor = settings.edit();
        editor.putString(MAX_IMAGE_HEIGHT_KEY, height);
        editor.commit();
    }

    public String getAPIkey() {
        SharedPreferences storesPreferences = getStoresPreferences();
        return storesPreferences.getString(GOOGLE_BOOK_API_KEY, "");
    }

    public void setAPIkey(String url) {
        SharedPreferences storesPreferences = getStoresPreferences();

        Editor editor = storesPreferences.edit();
        editor.putString(GOOGLE_BOOK_API_KEY, url);
        editor.commit();
    }

    public boolean getNewProductsEnabledCheckBox()
    {
        SharedPreferences storesPreferences = getStoresPreferences();
        return storesPreferences.getBoolean(NEW_PRODUCTS_ENABLED_KEY, true);
    }

    public void setNewProductsEnabledCheckBox(boolean checked)
    {
        SharedPreferences storesPreferences = getStoresPreferences();

        Editor editor = storesPreferences.edit();
        editor.putBoolean(NEW_PRODUCTS_ENABLED_KEY, checked);
        editor.commit();
    }

    public boolean getServiceCheckBox()
    {
        SharedPreferences storesPreferences = getStoresPreferences();
        return storesPreferences.getBoolean(SERVICE_CHECKBOX_KEY, true);
    }

    public void setServiceCheckBox(boolean checked)
    {
        SharedPreferences storesPreferences = getStoresPreferences();

        Editor editor = storesPreferences.edit();
        editor.putBoolean(SERVICE_CHECKBOX_KEY, checked);
        editor.commit();
    }

    public boolean getSoundCheckBox()
    {
        SharedPreferences storesPreferences = getStoresPreferences();
        return storesPreferences.getBoolean(SOUND_CHECKBOX_KEY, false);
    }

    public void setSoundCheckBox(boolean checked)
    {
        SharedPreferences storesPreferences = getStoresPreferences();

        Editor editor = storesPreferences.edit();
        editor.putBoolean(SOUND_CHECKBOX_KEY, checked);
        editor.commit();
    }

    public float getSoundVolume() {
        SharedPreferences storesPreferences = getStoresPreferences();
        return storesPreferences.getFloat(SOUND_VOLUME_KEY, 0.75f);
    }

    public void setSoundVolume(float volume) {
        SharedPreferences storesPreferences = getStoresPreferences();

        Editor editor = storesPreferences.edit();
        editor.putFloat(SOUND_VOLUME_KEY, volume);
        editor.commit();
    }


    public String getGalleryPhotosDirectory() {
        SharedPreferences storesPreferences = getStoresPreferences();
        String result = storesPreferences.getString(GALLERY_PHOTOS_DIRECTORY_KEY, null);
        if (TextUtils.isEmpty(result)) {
            result = getDefaultGalleryPhotosDirectory();
        }
        return result;
    }

    /**
     * Check whether the folder specified in the default_gallery_folder_name
     * settings persist at external and internal sd card in the next order
     * external, internal. If it doesn't exist then internal sd card path is
     * used
     * 
     * @return
     */
    public static String getDefaultGalleryPhotosDirectory()
    {
        if(sDefaultGalleryPhotoPath == null)
        {
            synchronized (TAG)
            {
                File externalStorage = Environment.getExternalStorageDirectory();
                try {
                    List<String> externalMounts = new ArrayList<String>(
                            CommonUtils.getExternalMounts());
                    if (externalStorage != null) {
                        externalMounts.add(externalStorage.getAbsolutePath());
                    }
                    String defaultFolder = CommonUtils
                            .getStringResource(R.string.default_gallery_folder_name);
                    boolean found = false;
                    for (String path : externalMounts) {
                        File galleryFolder = TextUtils.isEmpty(defaultFolder) ? new File(path)
                                : new File(path, defaultFolder);
                        CommonUtils.debug(TAG,
                                "getDefaultGalleryPhotosDirectory: checking folder %1$s",
                                galleryFolder.getAbsolutePath());
                        if (galleryFolder.isDirectory()) {
                            externalStorage = galleryFolder;
                            CommonUtils
                            .debug(TAG,
                                    "getDefaultGalleryPhotosDirectory: folder %1$s exists, setting it as default",
                                    galleryFolder.getAbsolutePath());
                            found = true;
                            break;
                        }
                    }
                    if (!found && !TextUtils.isEmpty(defaultFolder)) {
                        File galleryFolder = new File(externalStorage, defaultFolder);
                        galleryFolder.mkdir();
                        if (galleryFolder.isDirectory()) {
                            externalStorage = galleryFolder;
                        }
                    }
                } catch (Exception ex) {
                    CommonUtils.error(TAG, null, ex);
                }
                CommonUtils.debug(TAG,
                        "getDefaultGalleryPhotosDirectory: determined default folder %1$s",
                        externalStorage.getAbsolutePath());
                sDefaultGalleryPhotoPath = externalStorage.getAbsolutePath();
            }
        }
        return sDefaultGalleryPhotoPath;
    }

    public void setGalleryPhotosDirectory(String path) {
        SharedPreferences storesPreferences = getStoresPreferences();

        Editor editor = storesPreferences.edit();
        editor.putString(GALLERY_PHOTOS_DIRECTORY_KEY, path);
        editor.commit();
    }

    /**
     * Register listener for changing gallery photos directory property value
     * 
     * @param runnable to run when the value is changed
     */
    public void registerGalleryPhotosDirectoryChangedListener(final Runnable runnable)
    {
        registerListOfStoresPreferenceChangedListener(new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(GALLERY_PHOTOS_DIRECTORY_KEY))
                {
                    runnable.run();
                }

            }
        });
    }

    public String getErrorReportRecipient() {
        SharedPreferences storesPreferences = getStoresPreferences();
        return storesPreferences.getString(ERROR_REPORT_RECIPIENT_KEY,
                DEFAULT_ERROR_REPORT_RECIPIENT);
    }

    public void setErrorReportRecipient(String recipient) {
        SharedPreferences storesPreferences = getStoresPreferences();

        Editor editor = storesPreferences.edit();
        editor.putString(ERROR_REPORT_RECIPIENT_KEY, recipient);
        editor.commit();
    }

    /**
     * Register listener for list of stores preferences
     * 
     * @param listener
     */
    public void registerListOfStoresPreferenceChangedListener(
            OnSharedPreferenceChangeListener listener)
    {
        SharedPreferences storesPreferences = getStoresPreferences();
        listOfStorePreferenceChangeListener = listener;
        storesPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Unregister registered listener for list of store preferences
     */
    public void unregisterListOfStoresPreferenceChangeListeners()
    {
        if (listOfStorePreferenceChangeListener != null)
        {
            SharedPreferences storesPreferences = getStoresPreferences();
            storesPreferences
                    .unregisterOnSharedPreferenceChangeListener(listOfStorePreferenceChangeListener);
        }

    }

    public void setProfileDataValid(boolean valid) {
        Editor editor = settings.edit();
        editor.putBoolean(PROFILE_DATA_VALID, valid);
        editor.commit();
    }

    public boolean getProfileDataValid() {
        return settings.getBoolean(PROFILE_DATA_VALID, false);
    }

    public boolean hasSettings() {
        return (!settings.getString(USER_KEY, "").equals(""));
    }

}

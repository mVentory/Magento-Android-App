
package com.mageventory.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.mageventory.MyApplication;

/**
 * Event bus utils to send events accross different places of application
 * 
 * @author Eugene Popovich
 */
public class EventBusUtils {
    public static String SIMPLE_EVENT_ACTION = "com.mageventory.SIMPLE_EVENT";
    public static String EVENT_TYPE = "EVENT_TYPE";
    public static String JOB = "JOB";
    public static String PATH = "PATH";
    public static String CODE = "CODE";
    public static String SKU = "SKU";

    public enum EventType {
        LIBRARY_FILES_DELETED, LIBRARY_DATA_LOADED,
        LIBRARY_CACHE_CLEARED, LIBRARY_CACHE_CLEAR_FAILED,
        JOB_STATE_CHANGED, JOB_ADDED, JOB_ADDED_FOR_SOURCE_FILE,
        MAIN_THUMB_CACHE_CLEARED, MAIN_THUMB_CACHE_CLEAR_FAILED, 
        DECODE_RESULT, SETTINGS_CHANGED, PRODUCT_DETAILS_LOADED_IN_ACTIVITY
    }

    /**
     * Send the general event broadcast
     * 
     * @param eventType
     */
    public static void sendGeneralEventBroadcast(EventType eventType) {
        Intent intent = getGeneralEventIntent(eventType);
        sendGeneralEventBroadcast(intent);
    }

    /**
     * Send the general event broadcast
     * 
     * @param intent
     */
    public static void sendGeneralEventBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(intent);
    }

    /**
     * Generate general event intent for the specified event type
     * 
     * @param eventType
     * @return
     */
    public static Intent getGeneralEventIntent(EventType eventType) {
        Intent intent = new Intent(SIMPLE_EVENT_ACTION);
        intent.putExtra(EVENT_TYPE, eventType.ordinal());
        return intent;
    }

    /**
     * General broadcast event handler
     */
    public static interface GeneralBroadcastEventHandler {
        void onGeneralBroadcastEvent(EventType eventType, Intent extra);
    }

    /**
     * Get and register the broadcast receiver for the general event
     * 
     * @param TAG
     * @param handler
     * @return
     */
    public static BroadcastReceiver getAndRegisterOnGeneralEventBroadcastReceiver(final String TAG,
            final GeneralBroadcastEventHandler handler) {
        BroadcastReceiver br = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    CommonUtils.debug(TAG, "Received general event broadcast message");
                    EventType eventType = EventType.values()[intent.getIntExtra(EVENT_TYPE, 0)];
                    handler.onGeneralBroadcastEvent(eventType, intent);
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
                }
            }
        };
        LocalBroadcastManager.getInstance(MyApplication.getContext()).registerReceiver(br,
                new IntentFilter(SIMPLE_EVENT_ACTION));
        return br;
    }

    /**
     * Register the broadcast receiver for the general event
     * 
     * @param TAG
     * @param handler
     * @param broadcastReceiverRegisterHandler
     * @return
     */
    public static void registerOnGeneralEventBroadcastReceiver(final String TAG,
            final GeneralBroadcastEventHandler handler,
            final BroadcastReceiverRegisterHandler broadcastReceiverRegisterHandler) {
        broadcastReceiverRegisterHandler
                .addRegisteredLocalReceiver(getAndRegisterOnGeneralEventBroadcastReceiver(TAG,
                        handler));
    }

    /**
     * Interface for the classes which supports automatical
     * registering/unregistering of broadcast receivers during activity
     * lifecycle
     */
    public static interface BroadcastReceiverRegisterHandler {
        void addRegisteredLocalReceiver(BroadcastReceiver receiver);
    }
}

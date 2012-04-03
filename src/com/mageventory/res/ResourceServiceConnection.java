package com.mageventory.res;

import static com.mageventory.util.Util.logThread;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class ResourceServiceConnection implements ServiceConnection {

    private static final String TAG = "ResourceServiceConnection";
    private IResourceService resourceService;

    public IResourceService getService() {
        return resourceService;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        logThread(TAG, "OnServiceConnected(%s, %s);", name, service);
        resourceService = IResourceService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        logThread(TAG, "OnServiceDisconnected(%s);", name);
        resourceService = null;
    }

}

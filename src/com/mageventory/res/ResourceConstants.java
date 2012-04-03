package com.mageventory.res;

import com.mageventory.res.ResourceState.ResourceStateSchema;

public interface ResourceConstants {

    // cache
    public static final String CACHE_DIRECTORY = "cached_data";
    public static final long CACHE_SIZE = 10 * 1024 * 1024; // 10 MB
    public static final int CACHE_VERSION = 1;

    // extra keys
    public static final String EKEY_MESSENGER = "messenger";
    public static final String EKEY_OP_REQUEST_ID = "operation_request_id";
    public static final String EKEY_PARAMS = "parameters";
    public static final String EKEY_RESOURCE_TYPE = "resource_type";
    public static final String EKEY_REQUEST_EXTRAS = "request_extras";

    // request
    public static final int INVALID_REQUEST_ID = 0;

    // resource types

    public static final int RES_INVALID = -1;
    public static final int RES_PENDING = 0;
    // states
    public static final int STATE_AVAILABLE = ResourceStateSchema.STATE_AVAILABLE;
    public static final int STATE_BUILDING = ResourceStateSchema.STATE_BUILDING;

    public static final int STATE_DELETING = ResourceStateSchema.STATE_DELETING;
    public static final int STATE_NONE = ResourceStateSchema.STATE_NONE;
    public static final int STATE_UPDATING = ResourceStateSchema.STATE_UPDATING;

}

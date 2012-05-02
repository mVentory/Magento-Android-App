package com.mageventory.res;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.provider.BaseColumns;

public class ImagesState implements BaseColumns{

	public static final String TABLE_NAME = "imagestate";

	// columns
	// id
	public static final String STATE = "state";								// DEFINE STATE "UPLOADING/DOWNLOADING/CACHED"
	public static final String PRODUCT_ID = "product_id";					// DEFINE THE PRODUCT ID	
	public static final String IMAGE_INDEX = "image_index";					// DEFINE THE IMAGE INDEX
	public static final String IMAGE_URL = "image_url";						// DEFINE THE IMAGE URL "DWONLOAD IMAGE FROM IT"
	public static final String UPLOAD_PERCENTAGE = "upload_percentage";		// DEFINE THE UPLOAD PERECENTAGE "UPDATED BY UPLOAD PROCESS"
	public static final String IMAGE_NAME = "image_name";					// DEFINE THE IMAGE NAME
	public static final String IMAGE_PATH = "image_path";					// IN UPLOADING - DEFINE THE PATH OF IMAGE TO BE UPLOADED
																			// IN DOWNLOADING - DEFINE THE CACHED IMAGE PATH
	public static final String ERROR_MSG ="error_msg";						// IN CASE OF ERROR IN UPLOAD/DOWNLOAD OF IMAGE 
	

	// column types
	public static final String ID_T = "INTEGER PRIMARY KEY";
	public static final String INTEGER_T = "INTEGER";
	public static final String TEXT_T = "TEXT";

	// values
	public static final int STATE_UPLOAD = 1;
	public static final int STATE_DOWNLOAD = 2;
	public static final int STATE_CACHED = 3;
	
	public static final int [] IMAGE_STATES = {STATE_UPLOAD, STATE_DOWNLOAD, STATE_CACHED};
	
	public static final Map<String, String> COLUMNS;
	static {
		final Map<String, String> tmp = new HashMap<String, String>();
		tmp.put(STATE, INTEGER_T);
		tmp.put(PRODUCT_ID,INTEGER_T);
		tmp.put(IMAGE_INDEX,INTEGER_T);
		tmp.put(IMAGE_URL, TEXT_T);
		tmp.put(UPLOAD_PERCENTAGE, INTEGER_T);
		tmp.put(IMAGE_NAME, TEXT_T);
		tmp.put(IMAGE_PATH, TEXT_T);
		tmp.put(ERROR_MSG, TEXT_T);
		
		COLUMNS = Collections.unmodifiableMap(tmp);
	}


	
}

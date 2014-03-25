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
package com.reactor.gesture_input;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.reactor.gesture_input.GestureUtils.NamedGesture;

public class GestureBuilderActivity extends ListActivity {
	private static final int MENU_ID_RENAME = 1;
	private static final int MENU_ID_REMOVE = 2;

	private static final int DIALOG_RENAME_GESTURE = 1;

	private static final int REQUEST_NEW_GESTURE = 1;

	public static final String sStoreFile = "gestures";

    public static GestureLibrary sStore;

	private GesturesAdapter mAdapter;
	private GesturesLoadTask mTask;
	private TextView mEmpty;

	private Dialog mRenameDialog;
	private EditText mInput;
    private NamedGesture mCurrentRenameGesture;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.gestures_list);

		mAdapter = new GesturesAdapter(this);
		setListAdapter(mAdapter);

		sStore = GestureLibraries.fromPrivateFile(this, sStoreFile); 

		mEmpty = (TextView) findViewById(android.R.id.empty);
		loadGestures();

		registerForContextMenu(getListView());
	}
	
	public void copy(File src, File dst) throws IOException {
	    InputStream in = new FileInputStream(src);
	    OutputStream out = new FileOutputStream(dst);

	    // Transfer bytes from in to out
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
	        out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	}
	
	public void exportGestures(View v)
	{
		boolean res = false;
		
		String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	    	String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
	    	
	    	String copyFrom = getFilesDir() + "/" + sStoreFile;
	    	String copyTo = externalStoragePath + "/" + sStoreFile;
	    	 
	    	File copyFromFile = new File(copyFrom);
	    	File copyToFile = new File(copyTo);
	    	
	    	if (copyFromFile.exists())
	    	{
	    		res = true;
	    		try {
					copy(copyFromFile, copyToFile);
				} catch (IOException e) {
					res = false;
				}
	    	}
	    	
	    	if (res == true)
	    	{
	    		Toast toast = Toast.makeText(this, "File exported to: " + copyTo, Toast.LENGTH_LONG);
	    		toast.show();
	    	}
	    }
	    
    	if (res == false)
    	{
    		Toast toast = Toast.makeText(this, "File export failed", Toast.LENGTH_LONG);
    		toast.show();
    	}
	}

	public void importGestures(View v)
	{
		String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	    	String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();

	    	String copyFrom = externalStoragePath + "/" + sStoreFile;
		    String copyTo = getFilesDir() + "/" + sStoreFile;
	    	 
	    	File copyFromFile = new File(copyFrom);
	    	File copyToFile = new File(copyTo);
	    	
	    	if (copyFromFile.exists())
	    	{
	    		try {
					copy(copyFromFile, copyToFile);
				} catch (IOException e) {
					Toast toast = Toast.makeText(this, "ERROR: Gestures file is present but error occured when trying to copy it.", Toast.LENGTH_LONG);
		    		toast.show();
		    		return;
				}
	    	}
	    	else
	    	{
	    		Toast toast = Toast.makeText(this, "ERROR: Please put gestures file in this location before importing: " + copyFrom, Toast.LENGTH_LONG);
	    		toast.show();
	    		return;
	    	}
	    }
	    else
	    {
	    	Toast toast = Toast.makeText(this, "ERROR: SDCard not mounted.", Toast.LENGTH_LONG);
    		toast.show();
    		return;
	    }
	
		Toast toast = Toast.makeText(this, "Gestures imported.", Toast.LENGTH_SHORT);
		toast.show();
		
		loadGestures();
	}
	
	public void addGesture(View v) {
		Intent intent = new Intent(this, CreateGestureActivity.class);
		startActivityForResult(intent, REQUEST_NEW_GESTURE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_NEW_GESTURE:
				loadGestures();
				break;
			}
		}
	}

	private void loadGestures() {
		if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
			mTask.cancel(true);
		}
		mTask = (GesturesLoadTask) new GesturesLoadTask().execute();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
			mTask.cancel(true);
			mTask = null;
		}

		cleanupRenameDialog();
		sStore = null;
	}

	private void checkForEmpty() {
		if (mAdapter.getCount() == 0) {
			mEmpty.setText(R.string.gestures_empty);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle(((TextView) info.targetView).getText());

		menu.add(0, MENU_ID_RENAME, 0, R.string.gestures_rename);
		menu.add(0, MENU_ID_REMOVE, 0, R.string.gestures_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final NamedGesture gesture = (NamedGesture) menuInfo.targetView.getTag();

		switch (item.getItemId()) {
		case MENU_ID_RENAME:
			renameGesture(gesture);
			return true;
		case MENU_ID_REMOVE:
			deleteGesture(gesture);
			return true;
		}

		return super.onContextItemSelected(item);
	}

    private void renameGesture(NamedGesture gesture) {
		mCurrentRenameGesture = gesture;
		showDialog(DIALOG_RENAME_GESTURE);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_RENAME_GESTURE) {
			return createRenameDialog();
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		if (id == DIALOG_RENAME_GESTURE) {
			mInput.setText(mCurrentRenameGesture.name);
		}
	}

	private Dialog createRenameDialog() {
		final View layout = View.inflate(this, R.layout.dialog_rename, null);
		mInput = (EditText) layout.findViewById(R.id.name);
		((TextView) layout.findViewById(R.id.label)).setText(R.string.gestures_rename_label);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(0);
		builder.setTitle(getString(R.string.gestures_rename_title));
		builder.setCancelable(true);
		builder.setOnCancelListener(new Dialog.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				cleanupRenameDialog();
			}
		});
		builder.setNegativeButton(getString(R.string.cancel_action), new Dialog.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				cleanupRenameDialog();
			}
		});
		builder.setPositiveButton(getString(R.string.rename_action), new Dialog.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				changeGestureName();
			}
		});
		builder.setView(layout);
		return builder.create();
	}

	private void changeGestureName() {
		final String name = mInput.getText().toString();
		if (!TextUtils.isEmpty(name)) {
            final NamedGesture renameGesture = mCurrentRenameGesture;
			final GesturesAdapter adapter = mAdapter;
			final int count = adapter.getCount();

			// Simple linear search, there should not be enough items to warrant
			// a more sophisticated search
			for (int i = 0; i < count; i++) {
                final NamedGesture gesture = adapter.getItem(i);
				if (gesture.gesture.getID() == renameGesture.gesture.getID()) {
					sStore.removeGesture(gesture.name, gesture.gesture);
					gesture.name = mInput.getText().toString();
					sStore.addGesture(gesture.name, gesture.gesture);
					sStore.save();
					break;
				}
			}

			adapter.notifyDataSetChanged();
		}
		mCurrentRenameGesture = null;
	}

	private void cleanupRenameDialog() {
		if (mRenameDialog != null) {
			mRenameDialog.dismiss();
			mRenameDialog = null;
		}
		mCurrentRenameGesture = null;
	}

    private void deleteGesture(NamedGesture gesture) {
		sStore.removeGesture(gesture.name, gesture.gesture);
		sStore.save();

		final GesturesAdapter adapter = mAdapter;
		adapter.setNotifyOnChange(false);
		adapter.remove(gesture);
		adapter.sort(GestureUtils.defaultNamedGestureSorter);
		checkForEmpty();
		adapter.notifyDataSetChanged();

		Toast.makeText(this, R.string.gestures_delete_success, Toast.LENGTH_SHORT).show();
	}

    private class GesturesLoadTask extends GestureUtils.AbstractGesturesLoadTask {

        @Override
        void initParams() {
            final Resources resources = getResources();
            initParams(
                    (int) resources.getDimension(R.dimen.gesture_thumbnail_size), 
                    (int) resources.getDimension(R.dimen.gesture_thumbnail_inset), 
                    resources.getColor(R.color.gesture_color),
                    resources.getColor(R.color.gesture_start_color));
        }
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			findViewById(R.id.addButton).setEnabled(false);
			findViewById(R.id.exportGestures).setEnabled(false);
			findViewById(R.id.importGestures).setEnabled(false);
			
			mAdapter.setNotifyOnChange(false);
			mAdapter.clear();
		}

        @Override
        void addBitmap(Long id, Bitmap bitmap) {
            mAdapter.addBitmap(id, bitmap);
		}
		@Override
        protected void onProgressUpdate(NamedGesture... values) {
			super.onProgressUpdate(values);

			final GesturesAdapter adapter = mAdapter;
			adapter.setNotifyOnChange(false);

            for (NamedGesture gesture : values) {
				adapter.add(gesture);
			}

			adapter.sort(GestureUtils.defaultNamedGestureSorter);
			adapter.notifyDataSetChanged();
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);

			findViewById(R.id.addButton).setEnabled(true);
			findViewById(R.id.exportGestures).setEnabled(true);
			findViewById(R.id.importGestures).setEnabled(true);
			checkForEmpty();
		}

        @Override
        GestureLibrary getGestureLibrary() {
            return sStore;
        }
	}

	private class GesturesAdapter extends GestureUtils.AbstractGesturesAdapter {

		public GesturesAdapter(Context context) {
            super(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.gestures_item, parent, false);
			}

            final NamedGesture gesture = getItem(position);
			final TextView label = (TextView) convertView;

			label.setTag(gesture);
			label.setText(gesture.name);
			label.setCompoundDrawablesWithIntrinsicBounds(mThumbnails.get(gesture.gesture.getID()), null, null, null);

			return convertView;
		}
	}
}

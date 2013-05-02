package com.reactor.gesture_input;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Application;

public class MyApplication extends Application {
	
	private void copyGesturesFromResources(String copyTo) throws IOException
	{
		InputStream ins = getResources().openRawResource(R.raw.gestures);
		ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
		int size = 0;
		// Read the entire resource into a local byte buffer.
		byte[] buffer = new byte[1024];
		while((size=ins.read(buffer,0,1024))>=0){
		  outputStream.write(buffer,0,size);
		}
		ins.close();
		buffer = outputStream.toByteArray();

		FileOutputStream fos = new FileOutputStream(copyTo);
		fos.write(buffer);
		fos.close();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
    	
    	String gestruresFilePath = getFilesDir() + "/" + GestureBuilderActivity.sStoreFile;
    	File gesturesFile = new File(gestruresFilePath);

    	if (!gesturesFile.exists())
    	{
    		try {
				copyGesturesFromResources(gestruresFilePath);
			} catch (IOException e) {
			}
    	}
		
	}
}
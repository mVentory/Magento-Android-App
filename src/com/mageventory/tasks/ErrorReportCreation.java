package com.mageventory.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.mageventory.activity.MainActivity;
import com.mageventory.settings.Settings;
import com.mageventory.util.ErrorReporterUtils;
import com.mageventory.util.Log;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

public class ErrorReportCreation extends AsyncTask<Object, Void, Boolean> {
	
	private MainActivity mMainActivity;
	
	public ErrorReportCreation(MainActivity host)
	{
		mMainActivity = host;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mMainActivity.mMainContent.setVisibility(View.GONE);
		mMainActivity.mErrorReportingProgress.setVisibility(View.VISIBLE);
	}
	
	@Override
	protected Boolean doInBackground(Object... args) {
		ErrorReporterUtils.makeDBDump(mMainActivity);
		try {
			ErrorReporterUtils.zipEverythingUp();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		mMainActivity.mMainContent.setVisibility(View.VISIBLE);
		mMainActivity.mErrorReportingProgress.setVisibility(View.GONE);
		
		File attachmentFile = ErrorReporterUtils.getZippedErrorReportFile();
		
		if (attachmentFile.exists())
		{
			Settings settings = new Settings(mMainActivity);
			
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
			emailIntent.setType("text/xml");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{settings.getErrorReportRecipient()});
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Bug report");
			
			StringBuilder bodyText = new StringBuilder();
			
			bodyText.append("Profile ID: " + settings.getProfileID() + "\n");
			bodyText.append("Profile URL: " + settings.getUrl() + "\n");
			bodyText.append("Username: " + settings.getUser() + "\n");
			
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, bodyText.toString());
			
			ArrayList<Uri> uris = new ArrayList<Uri>();
			
			Uri u = Uri.fromFile(ErrorReporterUtils.getZippedErrorReportFile());
			uris.add(u);
			
			emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			
			mMainActivity.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
			Log.removeErrorReports();
		}
		else
		{
			Toast.makeText(mMainActivity, "Error: The attachment doesn't exist.", Toast.LENGTH_LONG).show();
		}
	}
}

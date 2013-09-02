
package com.mageventory.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.mageventory.activity.ExternalImagesEditActivity;
import com.mageventory.activity.MainActivity;
import com.mageventory.settings.Settings;
import com.mageventory.util.ErrorReporterUtils;
import com.mageventory.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

public class ErrorReportCreation extends AsyncTask<Object, Void, Boolean> {

    private Activity mActivity;
    private boolean mIncludeCurrentLogFileOnly;

    public ErrorReportCreation(Activity host, boolean includeCurrentLogFileOnly)
    {
        mActivity = host;
        mIncludeCurrentLogFileOnly = includeCurrentLogFileOnly;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (mActivity instanceof MainActivity)
        {
            ((MainActivity) mActivity).mMainContent.setVisibility(View.GONE);
            ((MainActivity) mActivity).mErrorReportingProgress.setVisibility(View.VISIBLE);
        }
        else if (mActivity instanceof ExternalImagesEditActivity)
        {
            ((ExternalImagesEditActivity) mActivity).showProgressDialog("Creating error report.",
                    this);
        }
    }

    @Override
    protected Boolean doInBackground(Object... args) {
        ErrorReporterUtils.makeDBDump(mActivity);
        try {
            ErrorReporterUtils.zipEverythingUp(mIncludeCurrentLogFileOnly);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (mActivity instanceof MainActivity)
        {
            ((MainActivity) mActivity).mMainContent.setVisibility(View.VISIBLE);
            ((MainActivity) mActivity).mErrorReportingProgress.setVisibility(View.GONE);
        }
        else if (mActivity instanceof ExternalImagesEditActivity)
        {
            ((ExternalImagesEditActivity) mActivity).dismissProgressDialog();
        }

        File attachmentFile = ErrorReporterUtils.getZippedErrorReportFile();

        if (attachmentFile.exists())
        {
            File attachmentFileRenamed;

            File reportDir = attachmentFile.getParentFile();
            String newReportFileName = "report-"
                    + android.text.format.DateFormat
                            .format("yyyyMMdd-kkmmss", new java.util.Date()).toString() + ".zip";

            attachmentFileRenamed = new File(reportDir, newReportFileName);

            attachmentFile.renameTo(attachmentFileRenamed);

            Settings settings = new Settings(mActivity);

            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
            emailIntent.setType("text/xml");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {
                settings.getErrorReportRecipient()
            });
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Bug report");

            StringBuilder bodyText = new StringBuilder();

            bodyText.append("Profile ID: " + settings.getProfileID() + "\n");
            bodyText.append("Profile URL: " + settings.getUrl() + "\n");
            bodyText.append("Username: " + settings.getUser() + "\n");

            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, bodyText.toString());

            ArrayList<Uri> uris = new ArrayList<Uri>();

            Uri u = Uri.fromFile(attachmentFileRenamed);
            uris.add(u);

            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

            mActivity.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            Log.removeErrorReports();
        }
        else
        {
            Toast.makeText(mActivity, "Error: The attachment doesn't exist.", Toast.LENGTH_LONG)
                    .show();
        }
    }
}

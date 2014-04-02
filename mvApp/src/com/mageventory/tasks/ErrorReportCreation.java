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

package com.mageventory.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.mageventory.R;
import com.mageventory.settings.Settings;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.ErrorReporterUtils;
import com.mageventory.util.Log;

public class ErrorReportCreation extends AsyncTask<Object, Void, Boolean> {

    private Activity mActivity;
    private boolean mIncludeCurrentLogFileOnly;
    private ProgressDialog mProgressDialog;

    public ErrorReportCreation(Activity host, boolean includeCurrentLogFileOnly)
    {
        mActivity = host;
        mIncludeCurrentLogFileOnly = includeCurrentLogFileOnly;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        showProgressDialog(CommonUtils.getStringResource(R.string.report_errors_progress_message),
                this);
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

        dismissProgressDialog();

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

    public void showProgressDialog(final String message,
            final ErrorReportCreation errorReportCreation) {
        if (mProgressDialog != null) {
            return;
        }
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setMessage(message);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);

        mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgressDialog.cancel();
                    }
                });

        mProgressDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                errorReportCreation.cancel(false);
            }
        });

        mProgressDialog.show();
    }

    public void dismissProgressDialog() {
        if (mProgressDialog == null) {
            return;
        }
        mProgressDialog.dismiss();
        mProgressDialog = null;
    }
}

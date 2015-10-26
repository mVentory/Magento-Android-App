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

package com.mageventory.fragment;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mventory.R;
import com.mageventory.activity.QueueActivity;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.base.BaseDialogFragment;
import com.mageventory.job.ExternalImagesJobQueue;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobQueue;
import com.mageventory.job.JobQueue.JobDetail;
import com.mageventory.job.JobQueue.JobsSummary;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.Log;
import com.mageventory.util.Log.OnErrorReportingFileStateChangedListener;

/**
 * The manage window which appears when manage button is pressed in the home
 * window
 * 
 * @author Eugene Popovich
 */
public class ManageDialogFragment extends BaseDialogFragment {
    static final String TAG = ManageDialogFragment.class.getSimpleName();

    private TextView mPhotoJobStatsText;
    private TextView mNewJobStatsText;
    private TextView mEditJobStatsText;
    private TextView mSaleJobStatsText;
    private TextView mOtherJobStatsText;
    private Button mRetryFailedButton;
    private Button mErrorReportingButton;
    private ViewGroup mRetryFailedReportErrorsContainer;

    private JobsSummary mJobsSummary;
    private int mExternalImagesPendingPhotoCount;

    private int mButtonDefaultTextColor;

    private JobControlInterface mJobControlInterface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mJobControlInterface = new JobControlInterface(getActivity());
        ExternalImagesJobQueue.registerExternalImagesCountChangedBroadcastReceiver(TAG,
                new ExternalImagesJobQueue.ExternalImagesCountChangedListener() {

                    @Override
                    public void onExternalImagesCountChanged(final int newCount) {
                        GuiUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mExternalImagesPendingPhotoCount = newCount;
                                if (getView() != null) {
                                    updateJobsStats();
                                }
                            }
                        });
                    }
                }, this);

        JobQueue.registerJobSummaryChangedBroadcastReceiver(TAG,
                new JobQueue.JobSummaryChangedListener() {

                    @Override
                    public void OnJobSummaryChanged(final JobsSummary jobsSummary) {
                        GuiUtils.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                mJobsSummary = jobsSummary;
                                if (getView() != null) {
                                    updateJobsStats();
                                }
                            }
                        });
                    }
                }, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_manage_dialog, container);
        init(view);
        return view;
    }

    public void init(View v) {
        mNewJobStatsText = (TextView) v.findViewById(R.id.newJobStats);
        mPhotoJobStatsText = (TextView) v.findViewById(R.id.photoJobStats);
        mEditJobStatsText = (TextView) v.findViewById(R.id.editJobStats);
        mSaleJobStatsText = (TextView) v.findViewById(R.id.saleJobStats);
        mOtherJobStatsText = (TextView) v.findViewById(R.id.otherJobStats);
        mRetryFailedReportErrorsContainer = (ViewGroup) v
                .findViewById(R.id.retryFailedReportErrorsContainer);
        mRetryFailedButton = (Button) v.findViewById(R.id.retryFailedButton);
        mErrorReportingButton = (Button) v.findViewById(R.id.errorReportingButton);
        mButtonDefaultTextColor = mRetryFailedButton.getCurrentTextColor();
        OnClickListener generalListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.retryFailedButton) {
                    retryFailedJobs();
                    closeDialog();
                } else if (v.getId() == R.id.errorReportingButton) {
                    ((BaseFragmentActivity) getActivity()).getBaseActivityCommon()
                            .showErrorReportingQuestion();
                    closeDialog();
                } else if (v.getId() == R.id.queueButton) {
                    Intent intent = new Intent(getActivity(), QueueActivity.class);
                    startActivity(intent);
                    closeDialog();
                } else if (v.getId() == R.id.clearQueueButton) {
                    deleteAllJobs();
                } else if (v.getId() == R.id.cancelButton) {
                    closeDialog();
                }
            }
        };
        mErrorReportingButton.setOnClickListener(generalListener);
        mRetryFailedButton.setOnClickListener(generalListener);
        v.findViewById(R.id.queueButton).setOnClickListener(generalListener);
        v.findViewById(R.id.clearQueueButton).setOnClickListener(generalListener);
        v.findViewById(R.id.cancelButton).setOnClickListener(generalListener);

        Log.OnErrorReportingFileStateChangedListener errorReportingFileStateChangedListener = new OnErrorReportingFileStateChangedListener() {

            @Override
            public void onErrorReportingFileStateChanged(boolean fileExists) {
                if (fileExists) {
                    GuiUtils.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mErrorReportingButton.setVisibility(View.VISIBLE);
                            mErrorReportingButton.setTextColor(Color.RED);
                            checkRetryFailedReportErrorsContainerVisibility();
                        }
                    });
                } else {
                    GuiUtils.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mErrorReportingButton.setVisibility(View.GONE);
                            mErrorReportingButton.setTextColor(mButtonDefaultTextColor);
                            checkRetryFailedReportErrorsContainerVisibility();
                        }
                    });
                }
            }
        };
        Log.registerOnErrorReportingFileStateChangedBroadcastReceiver(TAG,
                errorReportingFileStateChangedListener, this);
        updateJobsStats();
    }

    void updatePhotoSummary() {
        if (mJobsSummary == null) {
            return;
        }
        int pending = mJobsSummary.pending.photo + mExternalImagesPendingPhotoCount;
        int failed = mJobsSummary.failed.photo;

        if (pending > 0 && failed == 0)
            mPhotoJobStatsText.setText("" + pending + "/" + failed);
        else if (failed > 0)
            mPhotoJobStatsText.setText(Html.fromHtml("" + pending + "/<font color=\"#ff0000\">"
                    + failed + "</font>"));
        else
            mPhotoJobStatsText.setText("0");
    }

    void updateJobsStats() {
        if (mJobsSummary == null) {
            return;
        }
        updatePhotoSummary();

        if (mJobsSummary.pending.newProd > 0 && mJobsSummary.failed.newProd == 0)
            mNewJobStatsText.setText("" + mJobsSummary.pending.newProd + "/"
                    + mJobsSummary.failed.newProd);
        else if (mJobsSummary.failed.newProd > 0)
            mNewJobStatsText.setText(Html.fromHtml("" + mJobsSummary.pending.newProd
                    + "/<font color=\"#ff0000\">" + mJobsSummary.failed.newProd + "</font>"));
        else
            mNewJobStatsText.setText("0");

        if (mJobsSummary.pending.edit > 0 && mJobsSummary.failed.edit == 0)
            mEditJobStatsText.setText("" + mJobsSummary.pending.edit + "/"
                    + mJobsSummary.failed.edit);
        else if (mJobsSummary.failed.edit > 0)
            mEditJobStatsText.setText(Html.fromHtml("" + mJobsSummary.pending.edit
                    + "/<font color=\"#ff0000\">" + mJobsSummary.failed.edit + "</font>"));
        else
            mEditJobStatsText.setText("0");

        if (mJobsSummary.pending.sell > 0 && mJobsSummary.failed.sell == 0)
            mSaleJobStatsText.setText("" + mJobsSummary.pending.sell + "/"
                    + mJobsSummary.failed.sell);
        else if (mJobsSummary.failed.sell > 0)
            mSaleJobStatsText.setText(Html.fromHtml("" + mJobsSummary.pending.sell
                    + "/<font color=\"#ff0000\">" + mJobsSummary.failed.sell + "</font>"));
        else
            mSaleJobStatsText.setText("0");

        if (mJobsSummary.pending.other > 0 && mJobsSummary.failed.other == 0)
            mOtherJobStatsText.setText("" + mJobsSummary.pending.other + "/"
                    + mJobsSummary.failed.other);
        else if (mJobsSummary.failed.other > 0)
            mOtherJobStatsText.setText(Html.fromHtml("" + mJobsSummary.pending.other
                    + "/<font color=\"#ff0000\">" + mJobsSummary.failed.other + "</font>"));
        else
            mOtherJobStatsText.setText("0");

        if (mJobsSummary.failed.newProd > 0 || mJobsSummary.failed.photo > 0
                || mJobsSummary.failed.edit > 0 || mJobsSummary.failed.sell > 0
                || mJobsSummary.failed.other > 0) {
            mRetryFailedButton.setVisibility(View.VISIBLE);
            mRetryFailedButton.setTextColor(Color.RED);
        } else {
            mRetryFailedButton.setVisibility(View.GONE);
            mRetryFailedButton.setTextColor(mButtonDefaultTextColor);
        }
        checkRetryFailedReportErrorsContainerVisibility();
    }

    public void retryFailedJobs() {
        List<JobDetail> jobDetails = mJobControlInterface.getJobDetailList(false);

        for (JobDetail detail : jobDetails) {
            mJobControlInterface.retryJobDetail(detail);
        }
    }

    void checkRetryFailedReportErrorsContainerVisibility() {
        boolean visible = mRetryFailedButton.getVisibility() == View.VISIBLE
                || mErrorReportingButton.getVisibility() == View.VISIBLE;
        mRetryFailedReportErrorsContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void deleteAllJobs() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(R.string.main_clear_queue_confirmation_title);
        alert.setMessage(R.string.main_clear_queue_confirmation_message);

        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                for (boolean pendingTable : new boolean[] {
                        true, false
                }) {
                    List<JobDetail> jobDetails = mJobControlInterface
                            .getJobDetailList(pendingTable);
                    for (JobDetail jobDetail : jobDetails) {
                        mJobControlInterface.deleteJobEntries(jobDetail, pendingTable);
                    }
                }

                closeDialog();
            }
        });

        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog result = super.onCreateDialog(savedInstanceState);
        result.setTitle(R.string.main_manage);
        result.setCanceledOnTouchOutside(true);
        return result;
    }
}

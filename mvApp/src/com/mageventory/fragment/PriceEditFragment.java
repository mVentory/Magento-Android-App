
package com.mageventory.fragment;

import java.util.Date;
import java.util.GregorianCalendar;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import com.mageventory.R;
import com.mageventory.fragment.base.BaseDialogFragment;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;

/**
 * The edit price details dialog fragment
 * 
 * @author Eugene Popovich
 */
public class PriceEditFragment extends BaseDialogFragment {
    public static final String TAG = PriceEditFragment.class.getSimpleName();

    EditText priceText;
    EditText specialPriceText;
    EditText discountText;
    EditText fromDateText;
    EditText toDateText;

    Button okBtn;
    Button cancelBtn;
    Button clearBtn;

    boolean ignoreChanges = false;
    OnEditDoneListener onEditDoneListener;
    Double price;
    Double specialPrice;
    Date fromDate;
    Date toDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.price_edit, container);
        init(view, savedInstanceState);
        return view;
    }

    void init(View view, Bundle savedInstanceState) {
        Rect displayRectangle = new Rect();
        Window window = getActivity().getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);

        view.setMinimumWidth((int) (Math.min(displayRectangle.width(), displayRectangle.height()) * 0.9f));

        priceText = (EditText) view.findViewById(R.id.price);
        specialPriceText = (EditText) view.findViewById(R.id.specialPrice);
        discountText = (EditText) view.findViewById(R.id.specialPriceDiscount);

        priceText.addTextChangedListener(new TextWatcherAdapter() {

            @Override
            public void afterTextChanged(Editable s) {
                if (ignoreChanges) {
                    return;
                }
                calculateDiscount();
            }
        });
        specialPriceText.addTextChangedListener(new TextWatcherAdapter() {

            @Override
            public void afterTextChanged(Editable s) {
                if (ignoreChanges) {
                    return;
                }
                calculateDiscount();
                if (TextUtils.isEmpty(fromDateText.getText().toString()))
                {
                    fromDateText.setText(CommonUtils.formatDate(new Date()));
                }
            }
        });
        discountText.addTextChangedListener(new TextWatcherAdapter() {

            @Override
            public void afterTextChanged(Editable s) {
                if (ignoreChanges) {
                    return;
                }
                calculateSpecialPrice();
            }
        });

        fromDateText = (EditText) view.findViewById(R.id.specialPriceDateFrom);
        toDateText = (EditText) view.findViewById(R.id.speciaPriceDateTo);

        fromDateText.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                editDate(fromDateText);
                return true;
            }
        });
        toDateText.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                editDate(toDateText);
                return true;
            }
        });

        clearBtn = (Button) view.findViewById(R.id.clearSpecialPriceInformationButton);
        clearBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ignoreChanges = true;
                specialPriceText.setText(null);
                discountText.setText(null);
                fromDateText.setText(null);
                toDateText.setText(null);
                ignoreChanges = false;
            }
        });
        okBtn = (Button) view.findViewById(R.id.okBtn);
        okBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                editDone();
            }
        });
        cancelBtn = (Button) view.findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                editCancelled();
            }
        });

        ignoreChanges = true;
        priceText.setText(CommonUtils.formatNumberIfNotNull(price));
        specialPriceText.setText(CommonUtils.formatNumberIfNotNull(specialPrice));
        fromDateText.setText(CommonUtils.formatDateIfNotNull(fromDate));
        toDateText.setText(CommonUtils.formatDateIfNotNull(toDate));
        ignoreChanges = false;
        calculateDiscount();
    }

    private void editDate(final EditText dateTextField) {
        String dateText = dateTextField.getText().toString();
        Date d = TextUtils.isEmpty(dateText) ? new Date() : CommonUtils.parseDate(dateText);
        if (d == null) {
            d = new Date();
        }
        GregorianCalendar gc = (GregorianCalendar) GregorianCalendar.getInstance();
        gc.setTime(d);
        DatePickerDialog datePicker = new DatePickerDialog(getActivity(), new OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                GregorianCalendar gc = (GregorianCalendar) GregorianCalendar.getInstance();
                gc.set(GregorianCalendar.YEAR, year);
                gc.set(GregorianCalendar.MONTH, monthOfYear);
                gc.set(GregorianCalendar.DAY_OF_MONTH, dayOfMonth);
                dateTextField.setText(CommonUtils.formatDate(gc.getTime()));
            }
        }, gc.get(GregorianCalendar.YEAR), gc.get(GregorianCalendar.MONTH),
                gc.get(GregorianCalendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void calculateDiscount() {
        Double price = getPrice(priceText);
        Double specialPrice = getPrice(specialPriceText);

        Double discount = null;
        if (price != null && specialPrice != null && specialPrice != 0d) {
            discount = new Double(Math.round(100 - (specialPrice / price * 100)));
        }
        ignoreChanges = true;
        discountText.setText(discount == null ? null : CommonUtils.formatNumber(discount));
        ignoreChanges = false;
    }

    private void calculateSpecialPrice() {
        Double price = getPrice(priceText);
        Double discount = getPrice(discountText);

        Double specialPrice = null;
        if (price != null && discount != null) {
            specialPrice = price * (100 - discount) / 100;
        }
        ignoreChanges = true;
        specialPriceText.setText(specialPrice == null ? null : CommonUtils
                .formatNumber(specialPrice));
        ignoreChanges = false;
    }

    private Double getPrice(EditText textField) {
        String text = textField.getText().toString();
        Double result = null;
        if (!TextUtils.isEmpty(text)) {
            result = CommonUtils.parseNumber(text);
        }
        return result;
    }

    private Date getDate(EditText textField) {
        String text = textField.getText().toString();
        Date result = null;
        if (!TextUtils.isEmpty(text)) {
            result = CommonUtils.parseDate(text);
        }
        return result;
    }

    private void closeDialog() {
        Dialog dialog = PriceEditFragment.this.getDialog();
        if (dialog != null && dialog.isShowing()) {
            PriceEditFragment.this.dismissAllowingStateLoss();
        }
    }

    private boolean validatePrice(EditText textField) {
        String priceText = textField.getText().toString();
        if (!TextUtils.isEmpty(priceText)) {
            Double price = CommonUtils.parseNumber(priceText);
            if (price == null || price < 0) {
                return false;
            }
        }
        return true;
    }

    private boolean validateDate(EditText textField) {
        String dateText = textField.getText().toString();
        if (!TextUtils.isEmpty(dateText)) {
            Date date = CommonUtils.parseDate(dateText);
            if (date == null) {
                return false;
            }
        }
        return true;
    }

    protected boolean validateForm() {
        if (!validatePrice(priceText)) {
            GuiUtils.alert(CommonUtils.getStringResource(R.string.specified_value_is_invalid,
                    CommonUtils.getStringResource(R.string.product_original_price)));
            return false;
        }
        if (!validatePrice(specialPriceText)) {
            GuiUtils.alert(CommonUtils.getStringResource(R.string.specified_value_is_invalid,
                    CommonUtils.getStringResource(R.string.product_special_price)));
            return false;
        }
        if (!validateDate(fromDateText)) {
            GuiUtils.alert(CommonUtils.getStringResource(R.string.specified_value_is_invalid,
                    CommonUtils.getStringResource(R.string.product_special_price_date_from)));
            return false;
        }
        if (!validateDate(toDateText)) {
            GuiUtils.alert(CommonUtils.getStringResource(R.string.specified_value_is_invalid,
                    CommonUtils.getStringResource(R.string.product_special_price_date_to)));
            return false;
        }
        Date fromDate = getDate(fromDateText);
        Date toDate = getDate(toDateText);
        if(fromDate != null && toDate != null)
        {
            if (fromDate.getTime() > toDate.getTime())
            {
                GuiUtils.alert(R.string.date_from_after_date_to);
                return false;
            }
        }
        return true;
    }

    protected void editCancelled() {
        closeDialog();
    }

    protected void editDone() {
        if (validateForm()) {
            if (onEditDoneListener != null) {
                onEditDoneListener.editDone(
                        getPrice(priceText), getPrice(specialPriceText),
                        getDate(fromDateText),
                        getDate(toDateText));
            }
            closeDialog();
        }
    }

    public void setData(Double price, Double specialPrice, Date fromDate, Date toDate,
            OnEditDoneListener onEditDoneListener) {
        this.onEditDoneListener = onEditDoneListener;
        this.price = price;
        this.specialPrice = specialPrice;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog result = super.onCreateDialog(savedInstanceState);
        result.setTitle(R.string.price_edit_dialog_title);
        return result;
    }

    /**
     * The on edit done listener to listen for the updates done in the
     * PriceEditFragment
     */
    public static interface OnEditDoneListener {
        void editDone(Double price, Double specialPrice, Date fromDate, Date toDate);
    }

    private static abstract class TextWatcherAdapter implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}

package com.example.electricitybillcalculator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Custom ArrayAdapter for displaying Bill objects in a ListView.
 * Shows only month and final cost in the list item.
 */
public class BillAdapter extends ArrayAdapter<Bill> {

    private Context context;
    private List<Bill> billList;

    public BillAdapter(@NonNull Context context, @NonNull List<Bill> billList) {
        super(context, 0, billList);
        this.context = context;
        this.billList = billList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_bill, parent, false);
        }

        // Get the Bill object for the current position
        Bill currentBill = billList.get(position);

        // Find the TextViews in the list item layout
        TextView tvMonth = convertView.findViewById(R.id.tvListItemMonth);
        TextView tvFinalCost = convertView.findViewById(R.id.tvListItemFinalCost);

        // Populate the TextViews with data from the current Bill object
        if (currentBill != null) {
            tvMonth.setText(currentBill.getMonth());
            tvFinalCost.setText(String.format(Locale.getDefault(), "RM %.2f", currentBill.getFinalCost()));
        }

        return convertView;
    }
}

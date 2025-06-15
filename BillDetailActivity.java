package com.example.electricitybillcalculator;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BillDetailActivity extends AppCompatActivity {

    private TextView tvDetailMonth, tvDetailUnits, tvDetailTotalCharges, tvDetailRebate, tvDetailFinalCost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_detail);

        // Enable the Up button (back arrow) in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bill Details");
        }

        // Initialize UI elements
        tvDetailMonth = findViewById(R.id.tvDetailMonth);
        tvDetailUnits = findViewById(R.id.tvDetailUnits);
        tvDetailTotalCharges = findViewById(R.id.tvDetailTotalCharges);
        tvDetailRebate = findViewById(R.id.tvDetailRebate);
        tvDetailFinalCost = findViewById(R.id.tvDetailFinalCost);

        // Get data from the Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String month = extras.getString("month");
            double units = extras.getDouble("units");
            double totalCharges = extras.getDouble("total_charges");
            double rebatePercentage = extras.getDouble("rebate_percentage");
            double finalCost = extras.getDouble("final_cost");

            // Populate TextViews with the received data
            tvDetailMonth.setText(String.format(Locale.getDefault(), "Month: %s", month));
            tvDetailUnits.setText(String.format(Locale.getDefault(), "Units Used: %.2f kWh", units));
            tvDetailTotalCharges.setText(String.format(Locale.getDefault(), "Total Charges: RM %.2f", totalCharges));
            tvDetailRebate.setText(String.format(Locale.getDefault(), "Rebate: %.1f%%", rebatePercentage));
            tvDetailFinalCost.setText(String.format(Locale.getDefault(), "Final Cost: RM %.2f", finalCost));
        }
    }
}

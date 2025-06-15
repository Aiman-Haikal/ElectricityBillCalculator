package com.example.electricitybillcalculator;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI elements
    private Spinner monthSpinner;
    private EditText etUnitsUsed, etRebatePercentage;
    private Button btnCalculate, btnClear;
    private TextView tvTotalCharges, tvFinalCost;
    private ListView lvBillHistory;

    // Firebase helper instance
    private FirebaseHelper firebaseHelper;
    // List to hold bill data
    private List<Bill> billList;
    // Custom adapter for the ListView
    private BillAdapter billAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        monthSpinner = findViewById(R.id.monthSpinner);
        etUnitsUsed = findViewById(R.id.etUnitsUsed);
        etRebatePercentage = findViewById(R.id.etRebatePercentage);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnClear = findViewById(R.id.btnClear);
        tvTotalCharges = findViewById(R.id.tvTotalCharges);
        tvFinalCost = findViewById(R.id.tvFinalCost);
        lvBillHistory = findViewById(R.id.lvBillHistory);

        // Initialize Firebase helper
        firebaseHelper = new FirebaseHelper(this);

        // Setup month spinner
        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(this,
                R.array.months_array, android.R.layout.simple_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        // Set click listener for Calculate button
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBill();
            }
        });

        // Set click listener for Clear button
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFields();
            }
        });

        // Set item click listener for the bill history ListView
        lvBillHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the clicked Bill object
                Bill selectedBill = billList.get(position);
                // Create an Intent to open BillDetailActivity
                Intent intent = new Intent(MainActivity.this, BillDetailActivity.class);
                // Pass all bill details to the next activity
                intent.putExtra("month", selectedBill.getMonth());
                intent.putExtra("units", selectedBill.getUnitsUsed());
                intent.putExtra("total_charges", selectedBill.getTotalCharges());
                intent.putExtra("rebate_percentage", selectedBill.getRebatePercentage());
                intent.putExtra("final_cost", selectedBill.getFinalCost());
                startActivity(intent);
            }
        });

        // Initialize billList and adapter
        billList = new ArrayList<>();
        billAdapter = new BillAdapter(this, billList);
        lvBillHistory.setAdapter(billAdapter);

        // Start listening for bill history updates from Firestore
        listenForBillHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-attach listener if activity was paused and resumed
        listenForBillHistory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detach listener to prevent memory leaks when activity is not in foreground
        firebaseHelper.detachBillListener();
    }

    /**
     * Calculates the electricity bill based on user inputs.
     */
    private void calculateBill() {
        // Get inputs from UI elements
        String month = monthSpinner.getSelectedItem().toString();
        String unitsStr = etUnitsUsed.getText().toString().trim();
        String rebateStr = etRebatePercentage.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(unitsStr)) {
            etUnitsUsed.setError("Units used cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(rebateStr)) {
            etRebatePercentage.setError("Rebate percentage cannot be empty");
            return;
        }

        double unitsUsed;
        double rebatePercentage;

        try {
            unitsUsed = Double.parseDouble(unitsStr);
            rebatePercentage = Double.parseDouble(rebateStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for units and rebate.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (unitsUsed <= 0) {
            etUnitsUsed.setError("Units used must be positive");
            return;
        }
        if (rebatePercentage < 0 || rebatePercentage > 5) {
            etRebatePercentage.setError("Rebate must be between 0% and 5%");
            return;
        }

        // Calculate total charges based on blocks
        double totalCharges = 0;
        double remainingUnits = unitsUsed;

        // Block 1: For the first 200 kWh (1-200 kWh) per month - 21.8 sen/kWh
        if (remainingUnits > 0) {
            double unitsInBlock = Math.min(remainingUnits, 200);
            totalCharges += unitsInBlock * 0.218; // Convert sen to RM
            remainingUnits -= unitsInBlock;
        }

        // Block 2: For the next 100 kWh (201-300 kWh) per month - 33.4 sen/kWh
        if (remainingUnits > 0) {
            double unitsInBlock = Math.min(remainingUnits, 100);
            totalCharges += unitsInBlock * 0.334;
            remainingUnits -= unitsInBlock;
        }

        // Block 3: For the next 300 kWh (301-600 kWh) per month - 51.6 sen/kWh
        if (remainingUnits > 0) {
            double unitsInBlock = Math.min(remainingUnits, 300);
            totalCharges += unitsInBlock * 0.516;
            remainingUnits -= unitsInBlock;
        }

        // Block 4: For the next 300 kWh (601-900 kWh) per month onwards - 54.6 sen/kWh
        // This block covers units beyond 600 kWh up to 900 kWh.
        if (remainingUnits > 0) {
            double unitsInBlock = Math.min(remainingUnits, 300);
            totalCharges += unitsInBlock * 0.546;
            remainingUnits -= unitsInBlock;
        }

        // Handle units beyond 900 kWh (if any, they also fall under the 54.6 sen/kWh rate)
        // The instruction says "601-900 kWh onwards", implying this rate applies to anything above 600 kWh.
        // The previous block handles up to 900 kWh. If remainingUnits is still > 0, it means units > 900.
        if (remainingUnits > 0) {
            totalCharges += remainingUnits * 0.546; // Apply the highest rate for units beyond 900 kWh
        }


        // Calculate final cost after rebate
        double rebateAmount = totalCharges * (rebatePercentage / 100);
        double finalCost = totalCharges - rebateAmount;

        // Display results
        tvTotalCharges.setText(String.format(Locale.getDefault(), "Total Charges: RM %.2f", totalCharges));
        tvFinalCost.setText(String.format(Locale.getDefault(), "Final Cost after Rebate: RM %.2f", finalCost));

        // Save to Firebase
        Bill bill = new Bill(month, unitsUsed, totalCharges, rebatePercentage, finalCost);
        firebaseHelper.addBill(bill, success -> {
            if (success) {
                Toast.makeText(MainActivity.this, "Bill saved successfully!", Toast.LENGTH_SHORT).show();
                // No need to call loadBillHistory() explicitly, Firestore listener handles updates
            } else {
                Toast.makeText(MainActivity.this, "Error saving bill.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Clears all input fields and result displays.
     */
    private void clearFields() {
        monthSpinner.setSelection(0); // Select first month
        etUnitsUsed.setText("");
        etRebatePercentage.setText("");
        tvTotalCharges.setText("Total Charges: RM 0.00");
        tvFinalCost.setText("Final Cost after Rebate: RM 0.00");
        etUnitsUsed.requestFocus(); // Set focus back to units input
    }

    /**
     * Starts listening for real-time updates to the bill history from Firestore.
     */
    private void listenForBillHistory() {
        firebaseHelper.listenForBills(bills -> {
            billList.clear();
            billList.addAll(bills);
            billAdapter.notifyDataSetChanged();
            Log.d(TAG, "Bill history updated. Total items: " + bills.size());
        });
    }

    // --- Menu for About Page ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            // Navigate to AboutActivity
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

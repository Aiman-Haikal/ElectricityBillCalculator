package com.example.electricitybillcalculator;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Helper class to manage Firebase Firestore operations for electricity bills.
 * Handles Firebase initialization, user authentication, and data persistence.
 */
public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String appId;
    private String userId;
    private Context context;
    private ListenerRegistration billListenerRegistration;

    // Global variables provided by the Canvas environment
    // These are typically injected by the Canvas runtime.
    // For local testing without Canvas, you might need to set default values or mock them.
    // In a real Android app, Firebase config comes from google-services.json.
    // For this environment, we'll assume these are passed or mocked for demonstration.
    private static final String GLOBAL_APP_ID = "default-app-id"; // Placeholder for __app_id
    private static final String GLOBAL_FIREBASE_CONFIG = "{}"; // Placeholder for __firebase_config
    private static final String GLOBAL_AUTH_TOKEN = null; // Placeholder for __initial_auth_token

    public FirebaseHelper(Context context) {
        this.context = context;
        initializeFirebase();
    }

    /**
     * Retrieves a global variable from the environment.
     * This is a conceptual bridge for Canvas environment variables.
     * In a real Android app, Firebase config would be from google-services.json.
     */
    private String getGlobalVariable(String name, String defaultValue) {


        if (name.equals("app_id")) {
            // Attempt to get __app_id from system properties or environment if possible, else default
            String canvasAppId = System.getProperty("__app_id"); // Example for how it *might* be passed
            if (canvasAppId == null) canvasAppId = System.getenv("__app_id");
            return canvasAppId != null ? canvasAppId : defaultValue;
        } else if (name.equals("firebase_config")) {
            // Attempt to get __firebase_config from system properties or environment if possible, else default
            String canvasFirebaseConfig = System.getProperty("__firebase_config");
            if (canvasFirebaseConfig == null) canvasFirebaseConfig = System.getenv("__firebase_config");
            return canvasFirebaseConfig != null ? canvasFirebaseConfig : defaultValue;
        } else if (name.equals("initial_auth_token")) {
            // Attempt to get __initial_auth_token from system properties or environment if possible, else default
            String canvasAuthToken = System.getProperty("__initial_auth_token");
            if (canvasAuthToken == null) canvasAuthToken = System.getenv("__initial_auth_token");
            return canvasAuthToken != null ? canvasAuthToken : defaultValue;
        }
        return defaultValue;
    }

    /**
     * Initializes Firebase App and Authentication.
     * Tries to use __firebase_config and __initial_auth_token from the Canvas environment,
     * otherwise falls back to default FirebaseApp.initializeApp() which uses google-services.json.
     */
    private void initializeFirebase() {
        try {
            String currentAppId = getGlobalVariable("app_id", GLOBAL_APP_ID);
            String firebaseConfigJson = getGlobalVariable("firebase_config", GLOBAL_FIREBASE_CONFIG);
            String initialAuthToken = getGlobalVariable("initial_auth_token", GLOBAL_AUTH_TOKEN);

            this.appId = currentAppId;

            // Only initialize FirebaseApp if it hasn't been initialized yet
            if (FirebaseApp.getApps(context).isEmpty()) {
                if (!firebaseConfigJson.equals(GLOBAL_FIREBASE_CONFIG) && !firebaseConfigJson.isEmpty()) {
                    // Try to initialize with Canvas-provided config
                    JSONObject config = new JSONObject(firebaseConfigJson);
                    FirebaseOptions options = new FirebaseOptions.Builder()
                            .setApplicationId(config.optString("appId", "1:1234567890:android:abcdefg")) // google_app_id
                            .setApiKey(config.optString("apiKey"))
                            .setDatabaseUrl(config.optString("databaseURL"))
                            .setGcmSenderId(config.optString("messagingSenderId"))
                            .setProjectId(config.optString("projectId"))
                            .setStorageBucket(config.optString("storageBucket"))
                            .build();
                    FirebaseApp.initializeApp(context, options);
                    Log.d(TAG, "FirebaseApp initialized with Canvas config.");
                } else {
                    // Fallback to default initialization (uses google-services.json)
                    FirebaseApp.initializeApp(context);
                    Log.d(TAG, "FirebaseApp initialized with default config (google-services.json).");
                }
            } else {
                Log.d(TAG, "FirebaseApp already initialized.");
            }

            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();

            // Authenticate user after Firebase is initialized
            authenticateUser(initialAuthToken);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Firebase config JSON: " + e.getMessage());
            Toast.makeText(context, "Firebase configuration error. Please check setup.", Toast.LENGTH_LONG).show();
            // Fallback to default initialization if config parsing fails
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            authenticateUser(null);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
            Toast.makeText(context, "Failed to initialize Firebase.", Toast.LENGTH_LONG).show();
            // Fallback to default initialization if any other error occurs
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            authenticateUser(null);
        }
    }

    /**
     * Attempts to authenticate the user using a custom token or anonymously.
     * @param initialAuthToken The custom authentication token from Canvas, or null.
     */
    private void authenticateUser(String initialAuthToken) {
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            Log.d(TAG, "User already signed in: " + userId);
            return;
        }

        if (initialAuthToken != null && !initialAuthToken.isEmpty()) {
            auth.signInWithCustomToken(initialAuthToken)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            userId = user.getUid();
                            Log.d(TAG, "signInWithCustomToken:success UID: " + userId);
                            Toast.makeText(context, "Signed in as: " + userId, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "signInWithCustomToken:failure", task.getException());
                            Toast.makeText(context, "Custom token sign-in failed. Signing in anonymously.", Toast.LENGTH_SHORT).show();
                            signInAnonymously(); // Fallback
                        }
                    });
        } else {
            signInAnonymously();
        }
    }

    /**
     * Signs in the user anonymously.
     */
    private void signInAnonymously() {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        userId = user.getUid();
                        Log.d(TAG, "signInAnonymously:success UID: " + userId);
                        Toast.makeText(context, "Signed in anonymously as: " + userId, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(context, "Anonymous sign-in failed.", Toast.LENGTH_SHORT).show();
                        // If anonymous sign-in fails, generate a random UUID for userId
                        userId = UUID.randomUUID().toString();
                        Log.w(TAG, "Using random UUID for userId: " + userId);
                        Toast.makeText(context, "Using local ID: " + userId, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Returns the current user ID. If not authenticated, it will be a randomly generated UUID.
     * @return The user ID.
     */
    public String getUserId() {
        if (auth != null && auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        // Fallback if authentication hasn't completed or failed
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            Log.w(TAG, "getUserId() called before auth complete, using random UUID: " + userId);
        }
        return userId;
    }

    /**
     * Returns the application ID.
     * @return The application ID.
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Gets the Firestore collection reference for bills, respecting the private data path.
     * @return CollectionReference for bills.
     */
    private CollectionReference getBillsCollection() {
        // Path for private data: /artifacts/{appId}/users/{userId}/{your_collection_name}
        String collectionName = "bills";
        String currentUserId = getUserId(); // Ensure userId is available

        if (db == null || appId == null || currentUserId == null) {
            Log.e(TAG, "Firestore, App ID, or User ID not initialized. DB: " + (db != null) + ", AppId: " + appId + ", UserId: " + currentUserId);
            // Show a more informative message if possible
            if (context != null) {
                Toast.makeText(context, "Database not ready. Please ensure Firebase is set up and authenticated.", Toast.LENGTH_LONG).show();
            }
            return null;
        }

        return db.collection("artifacts")
                .document(appId)
                .collection("users")
                .document(currentUserId)
                .collection(collectionName);
    }

    /**
     * Adds a new bill to Firestore.
     * @param bill The Bill object to add.
     * @param callback Optional callback for success/failure.
     */
    public void addBill(Bill bill, Consumer<Boolean> callback) {
        CollectionReference billsRef = getBillsCollection();
        if (billsRef == null) {
            if (callback != null) callback.accept(false);
            return;
        }

        // Firestore will generate its own document ID.
        // The 'id' field in the Bill object is not sent to Firestore directly
        // unless you explicitly put it in a Map or use a custom converter.
        // We rely on Firestore's auto-generated ID.
        billsRef.add(bill)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Bill added with ID: " + documentReference.getId());
                    if (callback != null) callback.accept(true);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding bill", e);
                    Toast.makeText(context, "Error saving bill: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.accept(false);
                });
    }

    /**
     * Sets up a real-time listener for bill data from Firestore.
     * @param listener Callback to receive the updated list of bills.
     */
    public void listenForBills(Consumer<List<Bill>> listener) {
        CollectionReference billsRef = getBillsCollection();
        if (billsRef == null) {
            listener.accept(new ArrayList<>()); // Return empty list if not ready
            return;
        }

        // Detach previous listener if exists to prevent duplicates
        if (billListenerRegistration != null) {
            billListenerRegistration.remove();
            billListenerRegistration = null; // Reset to null after removal
        }

        // Order by timestamp in descending order (latest first)
        billListenerRegistration = billsRef
                .orderBy("timestamp", Query.Direction.DESCENDING) // Order by the new timestamp field
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        Toast.makeText(context, "Error loading history: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        listener.accept(new ArrayList<>());
                        return;
                    }

                    List<Bill> bills = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Bill bill = doc.toObject(Bill.class);
                                if (bill != null) {
                                    // Set the Firestore document ID to the Bill object's ID (String)
                                    bill.setId(doc.getId());
                                    bills.add(bill);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document to Bill: " + doc.getId(), e);
                            }
                        }
                    }
                    listener.accept(bills);
                });
    }

    /**
     * Detaches the real-time listener when no longer needed (e.g., activity destroyed).
     */
    public void detachBillListener() {
        if (billListenerRegistration != null) {
            billListenerRegistration.remove();
            billListenerRegistration = null;
            Log.d(TAG, "Firestore bill listener detached.");
        }
    }
}

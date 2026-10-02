package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HelpSeekerRegisterActivity extends Activity {

    // -----------------------------
    // XML Views
    // -----------------------------

    private EditText etName;
    private EditText etPhone;
    private EditText etEmergencyContact;
    private EditText etOTP;

    private Button btnSendOTP;
    private Button btnVerifyOTP;

    private TextView tvOTPStatus;

    // -----------------------------
    // Firebase
    // -----------------------------

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // Firebase verification ID
    private String verificationId;

    private static final String TAG = "JIBONSETU_AUTH";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help_seeker_register);

        // -----------------------------
        // Connect XML
        // -----------------------------

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmergencyContact =
                findViewById(R.id.etEmergencyContact);

        etOTP = findViewById(R.id.etOTP);

        btnSendOTP = findViewById(R.id.btnSendOTP);
        btnVerifyOTP = findViewById(R.id.btnVerifyOTP);

        tvOTPStatus = findViewById(R.id.tvOTPStatus);


        // -----------------------------
        // Initialize Firebase
        // -----------------------------

        firebaseAuth = FirebaseAuth.getInstance();

        firestore = FirebaseFirestore.getInstance();


        // -----------------------------
        // Button Actions
        // -----------------------------

        btnSendOTP.setOnClickListener(v -> sendOTP());

        btnVerifyOTP.setOnClickListener(v -> verifyOTP());
    }


    // =========================================================
    // SEND OTP
    // =========================================================

    private void sendOTP() {

        String name =
                etName.getText()
                        .toString()
                        .trim();

        String phone =
                etPhone.getText()
                        .toString()
                        .trim();

        String emergencyContact =
                etEmergencyContact.getText()
                        .toString()
                        .trim();


        // -----------------------------
        // Validate Name
        // -----------------------------

        if (name.isEmpty()) {

            etName.setError(
                    "Enter your full name"
            );

            etName.requestFocus();

            return;
        }


        // -----------------------------
        // Validate Phone
        // -----------------------------

        if (phone.isEmpty()) {

            etPhone.setError(
                    "Enter your phone number"
            );

            etPhone.requestFocus();

            return;
        }


        // -----------------------------
        // Validate Emergency Contact
        // -----------------------------

        if (emergencyContact.isEmpty()) {

            etEmergencyContact.setError(
                    "Enter emergency contact"
            );

            etEmergencyContact.requestFocus();

            return;
        }


        // -----------------------------
        // Convert Bangladesh Number
        //
        // 01XXXXXXXXX
        //       ↓
        // +8801XXXXXXXXX
        // -----------------------------

        if (phone.startsWith("01")
                && phone.length() == 11) {

            phone = "+88" + phone;
        }


        // -----------------------------
        // Check E.164 format
        // -----------------------------

        if (!phone.startsWith("+880")) {

            etPhone.setError(
                    "Use 01XXXXXXXXX format"
            );

            etPhone.requestFocus();

            return;
        }


        // -----------------------------
        // Disable Button
        // -----------------------------

        btnSendOTP.setEnabled(false);

        tvOTPStatus.setText(
                "Sending OTP..."
        );


        // -----------------------------
        // Firebase Phone Authentication
        // -----------------------------

        PhoneAuthOptions options =
                PhoneAuthOptions
                        .newBuilder(firebaseAuth)

                        .setPhoneNumber(phone)

                        .setTimeout(
                                60L,
                                TimeUnit.SECONDS
                        )

                        .setActivity(this)

                        .setCallbacks(
                                new PhoneAuthProvider
                                        .OnVerificationStateChangedCallbacks() {


                                    // =====================================
                                    // VERIFICATION COMPLETED
                                    // =====================================

                                    @Override
                                    public void onVerificationCompleted(
                                            PhoneAuthCredential credential) {

                                        Log.d(
                                                TAG,
                                                "Verification completed"
                                        );

                                        verifyCredential(
                                                credential
                                        );
                                    }


                                    // =====================================
                                    // VERIFICATION FAILED
                                    // =====================================

                                    @Override
                                    public void onVerificationFailed(
                                            FirebaseException e) {

                                        btnSendOTP
                                                .setEnabled(true);

                                        tvOTPStatus
                                                .setText(
                                                        "OTP verification failed"
                                                );


                                        String errorCode =
                                                "UNKNOWN";


                                        if (e instanceof FirebaseAuthException) {

                                            errorCode =
                                                    ((FirebaseAuthException) e)
                                                            .getErrorCode();
                                        }


                                        Log.e(
                                                TAG,
                                                "Firebase Error Code: "
                                                        + errorCode
                                                        + "\nMessage: "
                                                        + e.getMessage(),
                                                e
                                        );


                                        String message =
                                                e.getMessage();


                                        if (message == null) {

                                            message =
                                                    "Unknown Firebase error";
                                        }


                                        Toast.makeText(
                                                HelpSeekerRegisterActivity.this,

                                                "Error Code: "
                                                        + errorCode
                                                        + "\n"
                                                        + message,

                                                Toast.LENGTH_LONG
                                        ).show();
                                    }


                                    // =====================================
                                    // OTP SENT
                                    // =====================================

                                    @Override
                                    public void onCodeSent(
                                            String id,

                                            PhoneAuthProvider
                                                    .ForceResendingToken token) {

                                        verificationId =
                                                id;


                                        btnSendOTP
                                                .setEnabled(true);


                                        // Show OTP field

                                        etOTP.setVisibility(
                                                View.VISIBLE
                                        );


                                        // Show verify button

                                        btnVerifyOTP.setVisibility(
                                                View.VISIBLE
                                        );


                                        tvOTPStatus.setText(
                                                "OTP sent successfully"
                                        );


                                        Toast.makeText(
                                                HelpSeekerRegisterActivity.this,

                                                "OTP sent to your phone",

                                                Toast.LENGTH_LONG
                                        ).show();
                                    }


                                    // =====================================
                                    // AUTO RETRIEVAL TIMEOUT
                                    // =====================================

                                    @Override
                                    public void onCodeAutoRetrievalTimeOut(
                                            String id) {

                                        verificationId =
                                                id;

                                        tvOTPStatus.setText(
                                                "Enter the OTP manually"
                                        );
                                    }

                                }
                        )

                        .build();


        // Send verification SMS

        PhoneAuthProvider.verifyPhoneNumber(
                options
        );
    }


    // =========================================================
    // VERIFY OTP
    // =========================================================

    private void verifyOTP() {

        String otp =
                etOTP.getText()
                        .toString()
                        .trim();


        // -----------------------------
        // Validate OTP
        // -----------------------------

        if (otp.isEmpty()) {

            etOTP.setError(
                    "Enter OTP"
            );

            etOTP.requestFocus();

            return;
        }


        if (otp.length() != 6) {

            etOTP.setError(
                    "OTP must be 6 digits"
            );

            etOTP.requestFocus();

            return;
        }


        // -----------------------------
        // Check Verification ID
        // -----------------------------

        if (verificationId == null) {

            Toast.makeText(
                    this,

                    "Please request OTP first",

                    Toast.LENGTH_SHORT

            ).show();

            return;
        }


        btnVerifyOTP.setEnabled(false);


        // -----------------------------
        // Create Credential
        // -----------------------------

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(
                        verificationId,
                        otp
                );


        // -----------------------------
        // Verify Credential
        // -----------------------------

        verifyCredential(
                credential
        );
    }


    // =========================================================
    // FIREBASE SIGN IN
    // =========================================================

    private void verifyCredential(
            PhoneAuthCredential credential) {


        firebaseAuth
                .signInWithCredential(
                        credential
                )

                .addOnCompleteListener(
                        this,

                        task -> {

                            if (task.isSuccessful()) {

                                Log.d(
                                        TAG,
                                        "Phone authentication successful"
                                );


                                // Save profile

                                saveUserProfile();

                            } else {

                                btnVerifyOTP
                                        .setEnabled(true);


                                String errorCode =
                                        "UNKNOWN";


                                String errorMessage =
                                        "OTP verification failed";


                                if (task.getException()
                                        instanceof FirebaseAuthException) {

                                    FirebaseAuthException authException =
                                            (FirebaseAuthException)
                                                    task.getException();


                                    errorCode =
                                            authException
                                                    .getErrorCode();


                                    errorMessage =
                                            authException
                                                    .getMessage();

                                } else if (
                                        task.getException() != null) {

                                    errorMessage =
                                            task.getException()
                                                    .getMessage();
                                }


                                Log.e(
                                        TAG,

                                        "Sign-in error: "
                                                + errorCode
                                                + " - "
                                                + errorMessage
                                );


                                Toast.makeText(
                                        HelpSeekerRegisterActivity.this,

                                        "Error: "
                                                + errorCode
                                                + "\n"
                                                + errorMessage,

                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }


    // =========================================================
    // SAVE USER PROFILE TO FIRESTORE
    // =========================================================

    private void saveUserProfile() {

        // -----------------------------
        // Check Firebase User
        // -----------------------------

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,

                    "User authentication failed",

                    Toast.LENGTH_LONG

            ).show();

            return;
        }


        // -----------------------------
        // Get Firebase User ID
        // -----------------------------

        String userId =
                firebaseAuth
                        .getCurrentUser()
                        .getUid();


        // -----------------------------
        // Get User Data
        // -----------------------------

        String name =
                etName.getText()
                        .toString()
                        .trim();


        String phone =
                etPhone.getText()
                        .toString()
                        .trim();


        String emergencyContact =
                etEmergencyContact
                        .getText()
                        .toString()
                        .trim();


        // -----------------------------
        // Convert Phone Number
        // -----------------------------

        if (phone.startsWith("01")
                && phone.length() == 11) {

            phone = "+88" + phone;
        }


        // -----------------------------
        // Create User Map
        // -----------------------------

        Map<String, Object> user =
                new HashMap<>();


        user.put(
                "name",
                name
        );


        user.put(
                "phone",
                phone
        );


        user.put(
                "emergencyContact",
                emergencyContact
        );


        user.put(
                "role",
                "HELP_SEEKER"
        );


        user.put(
                "accountStatus",
                "ACTIVE"
        );


        // -----------------------------
        // Save to Firestore
        // -----------------------------

        firestore

                .collection("users")

                .document(userId)

                .set(user)

                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    HelpSeekerRegisterActivity.this,

                                    "Account created successfully!",

                                    Toast.LENGTH_LONG
                            ).show();


                            // Registration complete

                            finish();
                        }
                )

                .addOnFailureListener(
                        e -> {

                            btnVerifyOTP
                                    .setEnabled(true);


                            Log.e(
                                    TAG,

                                    "Firestore error",
                                    e
                            );


                            Toast.makeText(
                                    HelpSeekerRegisterActivity.this,

                                    "Profile save failed:\n"
                                            + e.getMessage(),

                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }
}
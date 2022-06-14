package com.three19.passwordmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executor;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 2000;
    FirebaseAuth mAuth;
    private final String TAG = " com.TABISH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

         FirebaseApp.initializeApp(this);



         SharedPreferences sp = this.getSharedPreferences("com.three19.passwordmanager",MODE_PRIVATE);


        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentUser != null){
                    //user is logged in
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){ // Checking whether user's os is greater than marshmallow

                        // Show fingerprint auth
                        boolean isAuthenticated = getFingerprintAuthResult(getApplicationContext(),sp);
                        Log.i(TAG, "isAuthenticated "+isAuthenticated);
                        if (isAuthenticated){
                            startActivity(new Intent(SplashActivity.this,MainActivity.class));
                            finish();
                        }

                    }

                }else{
                    //user is not logged in
                    startActivity(new Intent(SplashActivity.this,LoginActivity.class));
                    finish();
                }
            }

        },SPLASH_TIME_OUT);


    }

    private boolean getFingerprintAuthResult(Context context,SharedPreferences sp) {
        final boolean[] result_auth = {false};

        BiometricManager biometricManager = androidx.biometric.BiometricManager.from(context);

        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)){

            case BiometricManager.BIOMETRIC_SUCCESS:
                Toast.makeText(this,"You can use your fingerprint to login",Toast.LENGTH_LONG).show();
                // 0 : fingerprint not available   1 : Fingerprint Available
                //By default we will assume that the user will have the fingerprint available in his device
                sp.edit().putInt("SCANNER_UNAVAILABLE",1).apply();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this,"No fingerprint sensor",Toast.LENGTH_LONG).show();
                sp.edit().putInt("SCANNER_UNAVAILABLE",0).apply();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this,"Biometric sensor is not available",Toast.LENGTH_LONG).show();
                sp.edit().putInt("SCANNER_UNAVAILABLE",0).apply();

                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this,"Your device don't have any fingerprint, check your security setting", Toast.LENGTH_LONG).show();
                //btnLogin.setVisibility(View.INVISIBLE);
                break;
        }

        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt = new BiometricPrompt(SplashActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                result_auth[0] = false;

                showAlert(context,sp);

                // FIXME: result_auth[0]'s value is false even after entering the right password

            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                result_auth[0] = true;
                startActivity(new Intent(SplashActivity.this,MainActivity.class));
                //finish();
                Log.i(TAG, "onAuthenticationSucceeded: "+"SUCCESS");

            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                result_auth[0] = false;
                Log.i(TAG, "onAuthenticationFailed: "+"FAILED");
            }
        });


        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Login")
                .setNegativeButtonText("Cancel")
                .setDescription("FingerPrint Authentication")
                .build();
        
        


        biometricPrompt.authenticate(promptInfo);

        return result_auth[0];
    }

    private void showAlert(Context context, SharedPreferences sp) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        builder.setMessage("Enter Password");
        builder.setTitle("Alert");
        builder.setView(editText);
        builder.setPositiveButton("Login", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String pass = editText.getText().toString().trim();
                Log.i(TAG, "onClick: "+ editText.getText().toString());


                if(sp.getString("pass","").equals(pass)){
                    startActivity(new Intent(SplashActivity.this,MainActivity.class));
                    Toast.makeText(context, "Login Successful", Toast.LENGTH_LONG).show();
                    //finish();
                }else{
                    Toast.makeText(context, "Incorrect Password", Toast.LENGTH_SHORT).show();
                    showAlert(SplashActivity.this,sp);
                }
            }

        }).show();
    }
}

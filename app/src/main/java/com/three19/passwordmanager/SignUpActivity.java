package com.three19.passwordmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.w3c.dom.Text;

import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    protected FirebaseAuth mAuth;
    protected TextInputEditText name,email,password;
    protected TextInputLayout name_layout, email_layout, password_layout;
    protected MaterialButton signUpBtn;
    protected ProgressDialog pd;

    private final String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        init();
        SharedPreferences sp = this.getSharedPreferences("com.three19.passwordmanager",MODE_PRIVATE);


        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd.setMessage("Signing Up...");

                String email_text = email.getText().toString().trim();
                String password_text = password.getText().toString().trim();

                sp.edit().putString("pass",password_text).apply();

                Pattern pattern = Pattern.compile(EMAIL_PATTERN);
                Matcher matcher = pattern.matcher(email_text);

                if (TextUtils.isEmpty(name.getText().toString().trim())){
                    name_layout.setError("Please re-enter");
                }
                else if (TextUtils.isEmpty(email_text)) {
                    email_layout.setError("This Field should not be empty");
                }else if(!matcher.matches()){
                    Snackbar.make(findViewById(R.id.signup_constraint_id),"Email not well formatted ", Snackbar.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(password_text)) {
                    password_layout.setError("Password should not be empty");
                }else if(password_text.length() < 5) {
                    Snackbar.make(findViewById(R.id.signup_constraint_id),"Password Should not be less than 5 ", Snackbar.LENGTH_SHORT).show();
                }
                else{
                    pd.show();


//                    sp.edit().putString("USER_UNIQUE_ID",user_unique_id).apply();
                    signUpUser(email_text, password_text); //passing email and password to signup method

                }

            }
        });

    }


    private void signUpUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            pd.hide();
                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                        } else {
                            // If sign in fails, display a message to the user.
                            pd.hide();
                            Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(findViewById(R.id.signup_constraint_id),""+e.getMessage(),Snackbar.LENGTH_SHORT);
                Log.i("SignUp failed: ",e.getLocalizedMessage());
            }
        });
    }

    private void init() {

        mAuth = FirebaseAuth.getInstance();

        name = findViewById(R.id.name_editText_signup_id);
        email = findViewById(R.id.email_editText_signup_id);
        password = findViewById(R.id.pass_editText_signup_id);

        name_layout = findViewById(R.id.name_textInputLayout_id);
        email_layout = findViewById(R.id.email_textInputLayout_id);
        password_layout = findViewById(R.id.password_textInputLayout_id);

        signUpBtn = findViewById(R.id.materialButton_signup);

        pd = new ProgressDialog(SignUpActivity.this);
    }
}
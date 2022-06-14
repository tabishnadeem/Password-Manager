package com.three19.passwordmanager;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Calendar;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;


public class AddPassword extends AppCompatActivity {

    protected TextInputEditText account_name, user_name, password;
    protected TextInputLayout account_name_layout, user_name_layout, password_layout;
    protected MaterialButton generate_password, save_btn;
    protected ProgressDialog pd;
    protected FirebaseFirestore db;
    static boolean result = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_password);

        init();
        SharedPreferences sp = this.getSharedPreferences("com.three19.passwordmanager",MODE_PRIVATE);
        SQLiteDatabase sql = this.openOrCreateDatabase("manager",MODE_PRIVATE,null);
        sql.execSQL("CREATE TABLE IF NOT EXISTS passwordManager(id VARCHAR , password VARCHAR, UNIQUE(password))");

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd = new ProgressDialog(AddPassword.this);
                pd.setMessage("Saving...");
                String account_name_text = account_name.getText().toString();
                String user_name_text = user_name.getText().toString();
                String password_text = password.getText().toString();

               // pd.show();

                if (account_name_text.isEmpty() || TextUtils.isEmpty(account_name_text) || account_name_text == ""){

                    account_name_layout.setError("Account name should not be empty");
                    Snackbar.make(findViewById(R.id.constraint_addpass_id),"Account name should not be empty",Snackbar.LENGTH_SHORT).show();

                }else if(user_name_text.isEmpty() || TextUtils.isEmpty(user_name_text)|| user_name_text == "") {

                    account_name_layout.setErrorEnabled(false);
                    user_name_layout.setError("User name should not be empty");
                    Snackbar.make(findViewById(R.id.constraint_addpass_id), "User name should not be empty", Snackbar.LENGTH_SHORT).show();
                }
                else if (password_text.isEmpty() || TextUtils.isEmpty(password_text)){

                    user_name_layout.setErrorEnabled(false);
                    password_layout.setError("Password should not be empty");
                    Snackbar.make(findViewById(R.id.constraint_addpass_id),"Password should not be empty",Snackbar.LENGTH_SHORT).show();

                }else if(password_text.length() < 5){
                    Snackbar.make(findViewById(R.id.constraint_addpass_id),"Password length should be greater than 5",Snackbar.LENGTH_SHORT).show();
                }else{
                    addInfoToDatabase(sp,sql);

//                    boolean dataUploaded = sp.getBoolean("result",false);
//
//                    Log.i("DataUploaded",dataUploaded?"yes":"no");
//                    if(dataUploaded){
//                        // Cursor c = sql.rawQuery("SELECT * FROM passwordManager",null);
//                        while(c.moveToNext()){
//                            Log.i("ADDPASS",c.getString(0));
//                        }
//                        backupDatabase();
//                        Toast.makeText(AddPassword.this, "Account added Successfully!", Toast.LENGTH_SHORT).show();
//                        startActivity(new Intent(AddPassword.this,MainActivity.class));
//
//
//                    }else{
//                        Snackbar.make(findViewById(R.id.constraint_addpass_id),"Error Saving Data! Restart App",Snackbar.LENGTH_LONG);
//                    }
                }
            }
        });

        // account_name, user_name, password
        account_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                account_name_layout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        user_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                user_name_layout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                password_layout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });



        generate_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String randomPass = generateRandomPassword();
                password.setText(randomPass);

            }
        });

    }

    private void backupDatabase() {
        //deletePreviousDB();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        Uri file = Uri.fromFile(new File(getDatabasePath("manager").getAbsolutePath()));
        Log.i("Storage-dbPath",getDatabasePath("manager").getAbsolutePath());

//        StorageReference riversRef = storageRef.child(user.getUid()+"/"+file.getLastPathSegment());
        StorageReference riversRef = storageRef.child(user.getUid()+"/"+file.getLastPathSegment()+"backup");
        UploadTask uploadTask = riversRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.i("storage-error3",exception.getMessage());
                exception.printStackTrace();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                Log.i("storage-success",taskSnapshot.getMetadata().getName());
//                Toast.makeText(AddPassword.this, "Data BackedUp", Toast.LENGTH_SHORT).show();

            }
        });
    }

//    private void deletePreviousDB() {
//        FirebaseStorage storage = FirebaseStorage.getInstance();
//        StorageReference storageRef = storage.getReference();
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        StorageReference myRef = storageRef.child(user.getUid()+"/"+"manager");
//        myRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                Log.i("Storage-delete","Successful");
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.i("Storage-error",e.getMessage());
//
//            }
//        });
//
//    }


    private String generateRandomPassword() {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ@#%&abcdefghijklmnopqrstuvwxyz0123456789";

        SecureRandom random = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder();

        for (int i=0; i<15; i++){
            int rand_index = random.nextInt(chars.length());
            stringBuilder.append(chars.charAt(rand_index));
        }

        return stringBuilder.toString();
    }

    private void addInfoToDatabase(SharedPreferences sp , SQLiteDatabase sql) {

        Log.i("DBErr","REACHED ADDINFOTODATABASE");
        UUID id = UUID.randomUUID();

        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());

        Map<String, String> data = new HashMap<>();
        data.put("user_account_name",account_name.getText().toString().trim());
        data.put("user_name",user_name.getText().toString().trim());
        data.put("pass_id", String.valueOf(id));
        sp.edit().putString("pass_id", String.valueOf(id)).apply();
        // adding password to local db
        String pass = password.getText().toString().trim();
        sql.execSQL("INSERT INTO passwordManager(id,password) VALUES('"+id+"','"+pass+"')");


        data.put("day_created",date);

        sp.edit().putString("day_created",date).apply();

        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userID == null){
          result = false;
        }

        db.collection(userID).document(account_name.getText().toString().trim()).set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        result = true;
//                        Toast.makeText(AddPassword.this, "Result is true", Toast.LENGTH_SHORT).show();
                        sp.edit().putBoolean("result",true).apply();
                        backupDatabase();
                        Toast.makeText(AddPassword.this, "Account added Successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AddPassword.this,MainActivity.class));

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                Log.i("DBError",e.getMessage());
                result = false;
                Snackbar.make(findViewById(R.id.constraint_addpass_id),"Error Saving Data! Restart App",Snackbar.LENGTH_LONG);

            }
        });
        Log.i("ReturnValue", String.valueOf(result)+" "+userID);
       // return result;

    }

    private void init() {

        account_name = findViewById(R.id.account_name_addPass_id);
        user_name = findViewById(R.id.user_name_addPass_id);
        password = findViewById(R.id.password_addPass_id);

        account_name_layout = findViewById(R.id.account_name_id_textinputlayout);
        user_name_layout = findViewById(R.id.user_id_textinputlayout);
        password_layout = findViewById(R.id.password_id_textinputlayout);

        generate_password = findViewById(R.id.materialButton_pass_generate);
        save_btn = findViewById(R.id.materialButton_save_id);



        db = FirebaseFirestore.getInstance();

    }
}
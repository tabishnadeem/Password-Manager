package com.three19.passwordmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    protected TextView textView_signup;
    protected ConstraintLayout login_layout;
    protected MaterialButton login_btn;
    protected FirebaseAuth mAuth;
    protected TextInputEditText email, password;
    protected TextInputLayout email_layout, pass_layout;
    protected ProgressDialog pd;

    private final String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();

        SharedPreferences sp = this.getSharedPreferences("com.three19.passwordmanager",MODE_PRIVATE);
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd.setMessage("Logging in...");

                String email_text = email.getText().toString().trim();
                String password_text = password.getText().toString().trim();



                Pattern pattern = Pattern.compile(EMAIL_PATTERN);
                Matcher matcher = pattern.matcher(email_text);

                if (TextUtils.isEmpty(email_text)) {
                    email_layout.setError("This Field should not be empty");
                }else if(!matcher.matches()){
                    Snackbar.make(login_layout,"Email not well formatted ", Snackbar.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(password_text)) {
                    pass_layout.setError("Password should not be empty");
                }else if(password_text.length() < 5) {
                    Snackbar.make(login_layout,"Password Should not be less than 5 characters", Snackbar.LENGTH_SHORT).show();
                }else{
                    pd.show();
                    sp.edit().putString("pass",password_text).apply();
                    signInUser(email_text, password_text);



                }

            }
        });
    }

    private void restoreDBAndInsertIntoExistingDB(){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        StorageReference db = storageRef.child(user.getUid()+"/"+"managerbackup");



        File localFile = null;
        try {

            //localFile = File.createTempFile("manager","db");
            localFile = new File("/data/user/0/com.three19.passwordmanager/","cloudManager");
            localFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File finalLocalFile = localFile;
        db.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local file has been created
                System.out.println(finalLocalFile.getAbsolutePath());
                Log.i("DatabasePath2",finalLocalFile.getAbsolutePath());

                SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(finalLocalFile.getAbsolutePath(),null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                SQLiteDatabase sql =  openOrCreateDatabase("manager",MODE_PRIVATE,null);
                sql.execSQL("CREATE TABLE IF NOT EXISTS passwordManager(id VARCHAR, password VARCHAR)");

                Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM passwordManager",null);
                ContentValues cv = new ContentValues();
                while (cursor.moveToNext()){
//                    Log.i("Restored-Data: ", Arrays.toString(cursor.getColumnNames()));
                    Log.i("Restored-datalist ",cursor.getString(cursor.getColumnIndex("password")) );
                    String _id_ = cursor.getString(cursor.getColumnIndex("id"));
                    String _password_ = cursor.getString(cursor.getColumnIndex("password"));

                    cv.put("id",_id_);
                    cv.put("password",_password_);
                    sql.insert("passwordManager",null,cv);

                }
                Cursor c = sqLiteDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

                if (c.moveToFirst()) {
                    while ( !c.isAfterLast() ) {

                        Log.i("Restored-table: ","Table Name=> "+c.getString(0));
                        c.moveToNext();
                    }
                }

                Cursor cursor3 = sql.rawQuery("SELECT * FROM passwordManager",null);
                while (cursor3.moveToNext()){
                    Log.i("New-datalist ",cursor3.getString(cursor3.getColumnIndex("password")) );
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.i("Storage-error2",exception.getMessage());
                Toast.makeText(LoginActivity.this, "Download Failed", Toast.LENGTH_SHORT).show();
            }
        });
//        return SQLiteDatabase.openDatabase(finalLocalFile.getAbsolutePath(),null,SQLiteDatabase.NO_LOCALIZED_COLLATORS);
//        return finalLocalFile;

        Log.i("restoreDB:  ", ""+finalLocalFile.getName());
        if(finalLocalFile.exists()){
            String isDeleted = String.valueOf(finalLocalFile.delete());
            Log.i("File Deleted: ",isDeleted);
        }
    }

    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(LoginActivity.this, "Logged In!!", Toast.LENGTH_SHORT).show();
                            File dbFile = getDatabasePath("manager");
                            Log.i("OLD DB Exists?: ",""+dbFile.exists());
                            if(dbFile.exists()){
                                // no need to restore any database
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            }else{
                                // restore backed up database and create a db(passwordManager) and insert the
                                // value from the backed up db to this db(passwordManager).
                                restoreDBAndInsertIntoExistingDB();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            }
                            pd.hide();


                        } else {
                            // If sign in fails, display a message to the user.
                            pd.hide();
                            Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    private void init() {
        mAuth = FirebaseAuth.getInstance();

        textView_signup = (TextView) findViewById(R.id.textView3_signup);
        login_layout = (ConstraintLayout)  findViewById(R.id.loginConstraintID);

        login_btn = (MaterialButton) findViewById(R.id.materialButton);

        email = findViewById(R.id.email_editText_signin_id);
        password = findViewById(R.id.password_editText_signin_id);

        email_layout = findViewById(R.id.email_inputlayout_signin_id);
        pass_layout = findViewById(R.id.pass_inputlayout_signin_id);



        pd = new ProgressDialog(LoginActivity.this);

    }


    //onClick method for the textview
    public void onClick(View view){
        startActivity(new Intent(LoginActivity.this,SignUpActivity.class));
    }
}
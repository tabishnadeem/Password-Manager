package com.three19.passwordmanager;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class ChangePassword extends AppCompatActivity {
    private TextInputLayout acc_name_layout, username_layout, password_layout;
    private TextInputEditText acc_name_edittext, username_edittext, password_edittext;

    private MaterialButton submit_btn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        init();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sp = this.getSharedPreferences("com.three19.passwordmanager",MODE_PRIVATE);
        SQLiteDatabase sql = this.openOrCreateDatabase("manager",MODE_PRIVATE,null);

        String acc_name = getIntent().getStringExtra("account");
        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");

        acc_name_edittext.setText(acc_name);
        username_edittext.setText(username);
        password_edittext.setText(password);

        Log.i("CURRENT_PASS_ID: ",getIDForPassword(password,sql));

        //FIXME: update this activity because os of now its updating differently and not saving pass id

        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(acc_name_edittext.getText())){
                    acc_name_layout.setError("Enter Account Name");
                    acc_name_layout.setErrorEnabled(true);
                }

                else if(TextUtils.isEmpty(username_edittext.getText())){
                    acc_name_layout.setError("Enter User Name");
                    acc_name_layout.setErrorEnabled(true);
                }

                else if(TextUtils.isEmpty(password_edittext.getText())){
                    acc_name_layout.setError("Enter Password");
                    acc_name_layout.setErrorEnabled(true);
                }else{
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    ContentValues cv = new ContentValues();
                    String uid = user.getUid();
                    String day_updated = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
                    String day_created = sp.getString("day_created","");
                    Map<String,Object> map = new HashMap<>();
                    String updated_acc_name = acc_name_edittext.getText().toString().trim();
                    String updated_user_name = username_edittext.getText().toString().trim();
                    String updated_password = password_edittext.getText().toString().trim();
                    map.put("user_account_name",updated_acc_name);
                    map.put("user_name",updated_user_name);
//                    map.put("user_password",updated_password);
                    cv.put("password",updated_password);
                    map.put("pass_id",getIDForPassword(password,sql));
                    map.put("day_updated",day_updated);
                    db.collection(uid).document(acc_name).delete();
                    db.collection(uid).document(updated_acc_name).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(ChangePassword.this, "Account Updated", Toast.LENGTH_SHORT).show();
                            sql.update("passwordManager",cv,"id=?",new String[]{getIDForPassword(password,sql)});
                            backupDatabase();
                            startActivity(new Intent(ChangePassword.this,MainActivity.class));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i("On Fail to Update",e.getMessage());
                        }
                    });
                }
            }
        });



    }

    private String getIDForPassword(String password, SQLiteDatabase sql){

        Cursor c = sql.rawQuery("SELECT id FROM passwordManager WHERE password = ?",new String[]{password});
        if(c.moveToFirst()){
            return c.getString(0);
        }
        return "";
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
//                Toast.makeText(ChangePassword.this, "Data BackedUp", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void init() {

        acc_name_layout = findViewById(R.id.edit_acc_name_input_layout_id);
        username_layout = findViewById(R.id.edit_user_name_input_layout_id);
        password_layout = findViewById(R.id.edit_apassword_input_layout_id);

        acc_name_edittext = findViewById(R.id.edit_acc_name_input_edittext_id);
        username_edittext = findViewById(R.id.edit_username_input_edittext_id);
        password_edittext = findViewById(R.id.edit_password_input_edittext_id);

        submit_btn = findViewById(R.id.edit_materialButton_submit);
    }
}
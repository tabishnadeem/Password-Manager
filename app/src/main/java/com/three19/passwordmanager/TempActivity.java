package com.three19.passwordmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.util.Assert;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;
import jp.wasabeef.recyclerview.animators.SlideInDownAnimator;

public class TempActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;
    protected ArrayList<Data> myList = new ArrayList<>();;
    protected FloatingActionButton fab;
    protected ImageView logout;
    protected TextView info_text;
    protected ProgressDialog pd;
    SearchView searchView;

    private long backPressedTime;
    private Toast backToast;

    private SwipeRefreshLayout swipeRefreshLayout;

    private SQLiteDatabase sql, sql2;


    // Permissions
    private static final int STORAGE_REQUEST_CODE_EXPORT = 1;
    private static final int STORAGE_REQUEST_CODE_IMPORT = 2;
    private String[] storagePermissions;




    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()){
            backToast.cancel();
            finishAffinity();
            super.onBackPressed();
        }else {
            backToast = Toast.makeText(getBaseContext(),"Press Back Again to Exit",Toast.LENGTH_SHORT);
            backToast.show();
            backPressedTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onStart() {
        Toast.makeText(this, "On start reached!", Toast.LENGTH_SHORT).show();
//        sql = TempActivity.this.openOrCreateDatabase("manager",MODE_PRIVATE,null);
        super.onStart();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                sql = TempActivity.this.openOrCreateDatabase("manager",MODE_PRIVATE,null);
//                File file = restoreDatabase();
//                sql2 = SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READWRITE);
                init_data(FirebaseFirestore.getInstance());
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        Log.i("DatabasePath",getDatabasePath("manager").getAbsolutePath());


        SharedPreferences sp = this.getSharedPreferences("com.three19.passwordmanager",MODE_PRIVATE);

        // 0 : fingerprint not available   1 : Fingerprint Available
        Log.i("BIOMETRIC",sp.getInt("SCANNER_UNAVAILABLE",1)+"");
        if (sp.getInt("SCANNER_UNAVAILABLE",1) == 0){ // Checking if fingerprint is there
            //if fingerprint is not available, show signout button
            logout.setVisibility(View.VISIBLE);
        }

        recyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        sql = this.openOrCreateDatabase("manager",MODE_PRIVATE,null);


        init_data(db);//adding data to the list and setting the adapter in it

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do what you want when search view expended
                Log.i("searchview","opened");
                findViewById(R.id.textView_recycle).setVisibility(View.INVISIBLE);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                //do what you want  searchview is not expanded
                Log.i("searchview","closed");
                onStart();
                findViewById(R.id.textView_recycle).setVisibility(View.VISIBLE);

                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Navigate to other activity
                startActivityForResult(new Intent(TempActivity.this,AddPassword.class),1);

            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd.setMessage("Logging out...");
                pd.show();
                FirebaseAuth.getInstance().signOut();
                pd.hide();
                startActivity(new Intent(TempActivity.this,LoginActivity.class));
                finishAffinity();
            }
        });

        logout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(TempActivity.this, "Logout", Toast.LENGTH_SHORT).show();
                return true;
            }
        });


        ///swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                Toast.makeText(TempActivity.this, "Moved", Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Vibrator vibe = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibe.vibrate(100);
                Log.i("SWIPED: ", String.valueOf(viewHolder.getAdapterPosition()));
                String uid = getUID();
                String doc_name = adapter.myList.get((int) (viewHolder.getAdapterPosition())).account_name;

                db.collection(uid).document(doc_name).delete();
                adapter.myList.remove((int)(viewHolder.getAdapterPosition()));
                adapter.notifyItemRemoved(viewHolder.getAdapterPosition());


            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

    }

    private File restoreDatabase() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        StorageReference db = storageRef.child(user.getUid()+"/"+"managerbackup");

        File localFile = null;
        try {

            //localFile = File.createTempFile("manager","db");
            localFile = new File("/data/user/0/com.three19.passwordmanager/","manager");
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

//                SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(finalLocalFile.getAbsolutePath(),null,SQLiteDatabase.NO_LOCALIZED_COLLATORS);

//                Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM passwordManager",null);
//                while (cursor.moveToNext()){
//                    Log.i("Datab-tab: ",Arrays.toString(cursor.getColumnNames()));
//                }
//                Cursor c = sqLiteDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
//
//                if (c.moveToFirst()) {
//                    while ( !c.isAfterLast() ) {
//
//                        Log.i("Datab-tab: ","Table Name=> "+c.getString(0));
//                        c.moveToNext();
//                    }
//                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.i("Storage-error2",exception.getMessage());
                Toast.makeText(TempActivity.this, "Download Failed", Toast.LENGTH_SHORT).show();
            }
        });
//        return SQLiteDatabase.openDatabase(finalLocalFile.getAbsolutePath(),null,SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        return finalLocalFile;
    }

    private String getUID(){
        return  FirebaseAuth.getInstance().getCurrentUser().getUid();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1){
            if (resultCode == 100){
                //password Saved Successfully in the AddPassword Activity

                Snackbar.make(findViewById(R.id.main_activity_relative_layout_id),"Data Saved Successfully!",Snackbar.LENGTH_SHORT);
            }else if (resultCode == 200){
                //password updated successfully in the ChangePassword Activity
                Snackbar.make(findViewById(R.id.main_activity_relative_layout_id),"Data Updated Successfully!",Snackbar.LENGTH_SHORT);

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void init() {

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView_id);
        fab =(FloatingActionButton) findViewById(R.id.fab_id);
        logout = (ImageView) findViewById(R.id.logout_imageView_);
        info_text = (TextView) findViewById(R.id.info_text_textView_id);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshId);

        searchView = findViewById(R.id.searchView);

        pd = new ProgressDialog(this);

    }

    private void init_data(FirebaseFirestore db) {
        myList.clear();
        String userID = getUID();
        File dbFile = this.getDatabasePath("manager");
        Log.i("OLD DB Exists?: ",""+dbFile.exists());

        db.collection(userID).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot document : task.getResult()){
                                if(document.exists()){
                                    sql.execSQL("CREATE TABLE IF NOT EXISTS passwordManager(id VARCHAR, password VARCHAR)");
//                                    File file = restoreDatabase();
                                    //onStart();
//                                    try {
//                                        sql2 = SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
//                                        Cursor cursor = sql2.rawQuery("SELECT * FROM passwordManager",null);
//                                        Log.i("cursor-count-cloud", String.valueOf(cursor.getCount()));
//                                        new Thread(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                while (cursor.moveToNext()){ // yaha aa rha password
//                                                    //Log.i("Datab-tab: ",cursor.getString(0));
//                                                    String _id_ = cursor.getString(0);
//                                                    String _password_ = cursor.getString(1);
//
//                                                    ContentValues cv = new ContentValues();
//                                                    cv.put("id",_id_);
//                                                    cv.put("password",_password_);
//                                                    sql.insert("passwordManager",null,cv);
//
//                                                }
//                                            }
//                                        }).start();
                                    // TODO: Data is fetching correctly and is loading correctly but
                                    //  duplicate data is being inserted to the cloud and more than 25k+ is stored for 10 distinct accounts
                                    //  This will impact the overall performance of the app and needs to be fixed asap
                                    //  Possible solution can be distinct data upload but first try to find why and how this
                                    //  data duplicates are created at the first place
                                    //sql2.close();
                                    //onStart();

//                                    }catch (Exception e){
//                                        e.printStackTrace();
//                                    }

                                }else{
                                    Toast.makeText(TempActivity.this, "New Data created", Toast.LENGTH_SHORT).show();
                                }
                                Log.d("ALLDOCUMENT", document.getId() + " => " + document.getData());
                                Data data = new Data();
                                String account = (String) document.getData().get("user_account_name");
                                String username = (String)document.getData().get("user_name");
                                String pass_id = (String)document.getData().get("pass_id");

                                try {
                                    Cursor cursor = sql.rawQuery("SELECT * FROM passwordManager WHERE id = '"+pass_id+"'",null);

                                    int pass_index = cursor.getColumnIndex("password");

                                    Log.i("cursor-condition", (cursor!=null) +" && "+ cursor.moveToFirst());
                                    Log.i("cursor-pass_index", String.valueOf(pass_index));

                                    if(cursor!=null && cursor.moveToFirst()){ // returns true and false for the first time and that is why the activity is refreshed
                                        String pass = cursor.getString(pass_index);
                                        Log.i("cursor", pass);
                                        data.setPassword(pass);
                                        cursor.close();
                                    }else{
                                        //  Here the activity is refreshed explicitly because the sql data was inserted after a swipe down and not automatically
                                        finish();
                                        overridePendingTransition(0, 0);
                                        startActivity(new Intent(TempActivity.this, TempActivity.class));
                                        overridePendingTransition(0, 0);

                                    }


                                    data.setAccount_name(account);
                                    data.setUser_name(username);

                                    Log.i("Account",data.getAccount_name());
                                    myList.add(data);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }



                            }

                            Log.i("list-size", String.valueOf(myList.size()));
                            adapter = new RecyclerViewAdapter(myList,TempActivity.this);
                            AlphaInAnimationAdapter alphaAdapter = new AlphaInAnimationAdapter(adapter);
                            recyclerView.setAdapter(new ScaleInAnimationAdapter(alphaAdapter));

                            //recyclerView.setAdapter(adapter);
                            if (adapter.getItemCount() > 0){

                                recyclerView.setVisibility(View.VISIBLE);
                                info_text.setVisibility(View.INVISIBLE);

                            }



                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(TempActivity.this, "Fetch unsuccessfull", Toast.LENGTH_SHORT).show();
            }
        });

        swipeRefreshLayout.setRefreshing(false);

        System.out.println("SIZE: "+myList.size());

    }







}

package com.example.abdull.scorebatao.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.abdull.scorebatao.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import pojo.user;
import utility.utilityConstant;


public class StartScreen extends AppCompatActivity implements View.OnClickListener {

    Button LoginButton, createAccount,forgetButton;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private TextView email;
    private TextView password;
    private com.facebook.login.widget.LoginButton loginButton;
    private CallbackManager callbackManager;
    private SharedPreferences sharedpreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);
        mAuth = FirebaseAuth.getInstance();

//        try {
//            PackageInfo info = getPackageManager().getPackageInfo(
//                    "com.example.abdull.scorebatao",
//                    PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                Log.d("Development key", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//
//        } catch (NoSuchAlgorithmException e) {
//
//        }

        // facebok login code

        FacebookSdk.sdkInitialize(getApplicationContext());
        loginButton = (LoginButton)findViewById(R.id.login_button);

        callbackManager = CallbackManager.Factory.create();

        // Callback registration

        //  LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
        loginButton.setReadPermissions(Arrays.asList("public_profile", "user_friends", "email", "user_birthday"));


        final GraphRequest.GraphJSONObjectCallback graphJSONObjectCallback=new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                final DatabaseReference myRef = database.getReference("users");
                final String userId = myRef.push().getKey();

                try {
                    // user insert
                    final String emailOfFB=object.getString("email").toString();
                    sharedpreferences = getSharedPreferences(utilityConstant.MyPREFERENCES,Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedpreferences.edit();

                    editor.putString("email", emailOfFB);
                    editor.commit();
                    myRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            Iterable<DataSnapshot> childrenData = dataSnapshot.getChildren();
                            boolean checkFb=false;

                            for (DataSnapshot child : childrenData) {
//                    DatabaseReference databaseReference=child.getRef();
//                    databaseReference.
                                HashMap hashMap = (HashMap) child.getValue();
                                String email = (String) hashMap.get("email");
                                if (email.equalsIgnoreCase(emailOfFB)) {
                                    checkFb=true;
                                    break;
                                    // break when email address find
                                }


                            }
                            if(checkFb)
                            {
                                // move to mathes screen
                                Intent ListOFScreen = new Intent(StartScreen.this, listOfMatch.class);
                                ListOFScreen.putExtra("Email",emailOfFB);
                                startActivity(ListOFScreen);
                                signInMethod(utilityConstant.facebook);
                                finish();
                                Toast.makeText(getApplicationContext(), "Sign In Successfully using Fb ", Toast.LENGTH_LONG).show();
                            }
                            else
                            {

                                user user = new user(emailOfFB,"ID-3333");
                                myRef.child(userId).setValue(user);

                                // move to mathes screen
                                Intent ListOFScreen = new Intent(StartScreen.this, listOfMatch.class);
                                ListOFScreen.putExtra("Email",emailOfFB);
                                startActivity(ListOFScreen);
                                signInMethod(utilityConstant.facebook);
                                finish();
                                Toast.makeText(getApplicationContext(), "Sign In Successfully using Fb ", Toast.LENGTH_LONG).show();

                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            Log.w("Read Failed", "Failed to read value.", error.toException());
                        }
                    });



                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        loginButton.registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        GraphRequest request = GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),graphJSONObjectCallback);
                        Log.d("request",request.toString());
                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "id,first_name,last_name,name,link,gender,birthday,email");
                        request.setParameters(parameters);
                        request.executeAsync();
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                    }
                });



        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d("signIN", "onAuthStateChanged:signed_in:" + user.getUid());

                    //check verification
                    if (!user.isEmailVerified()) {
                        Toast.makeText(StartScreen.this, "Please Verify email=" + user.getEmail(), Toast.LENGTH_SHORT).show();
                    } else {
                        // move to mathes screen
                        Intent ListOFScreen = new Intent(StartScreen.this, listOfMatch.class);
                        ListOFScreen.putExtra("Email",user.getEmail());
                        startActivity(ListOFScreen);
                        finish();
                        signInMethod(utilityConstant.custom);
                        Toast.makeText(getApplicationContext(), "Sign In Successfully ", Toast.LENGTH_LONG).show();

                    }

                } else {
                    // User is signed out
                    Log.d("singOUT", "onAuthStateChanged:signed_out");
                    Toast.makeText(getApplicationContext(), "Sign In out ", Toast.LENGTH_LONG).show();
                }
                // ...
            }
        };


        email = (TextView) findViewById(R.id.emailIDsignin);
        password = (TextView) findViewById(R.id.passwordsignin);

        LoginButton=(Button)findViewById(R.id.loginID);
        createAccount = (Button) findViewById(R.id.createaccount);
        forgetButton=(Button)findViewById(R.id.forget);
        LoginButton.setOnClickListener(this);
        createAccount.setOnClickListener(this);
        forgetButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.equals(LoginButton))
        {
            if (email.getText().toString().trim().equals("") || password.getText().toString().trim().equals("")) {
                Toast.makeText(this, "Empty Field", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.signInWithEmailAndPassword(email.getText().toString().trim(), password.getText().toString().trim())
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d("signin", "signInWithEmail:onComplete:" + task.isSuccessful());

                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                try {
                                    throw task.getException();

                                } catch (FirebaseAuthInvalidUserException e) {
                                    Toast.makeText(StartScreen.this, "" + e, Toast.LENGTH_SHORT).show();
                                } catch (FirebaseAuthInvalidCredentialsException e) {
                                    Toast.makeText(StartScreen.this, "" + e.getErrorCode(), Toast.LENGTH_SHORT).show();

                                } catch (Exception e) {

                                }

                                if (!task.isSuccessful()) {
                                    Log.w("signin", "signInWithEmail:failed", task.getException());
                                }

                                // ...
                            }
                        });

            }

        } else if (v.equals(createAccount)) {
            Intent ListOFScreen = new Intent(StartScreen.this, signupAccount.class);
            startActivity(ListOFScreen);
        }
        else if(v.equals(forgetButton))
        {
           postDailog();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
         //Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
       // Toast.makeText(this, "On Start"+currentUser.getEmail(), Toast.LENGTH_SHORT).show();

        AccessToken accessToken=AccessToken.getCurrentAccessToken();
        if(accessToken!=null)
        {
            sharedpreferences = getSharedPreferences(utilityConstant.MyPREFERENCES, Context.MODE_PRIVATE);
            String Email = sharedpreferences.getString("email","not found");
            Intent ListOFScreen = new Intent(StartScreen.this, listOfMatch.class);
            ListOFScreen.putExtra("Email",Email);
            signInMethod(utilityConstant.facebook);
            startActivity(ListOFScreen);
            Toast.makeText(getApplicationContext(), "Sign In Successfully Using Fb ", Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(this, "Fb Not Sign In ", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }

    }
    private void postDailog() {

        final AlertDialog.Builder builder=new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();



        final View dialogView = inflater.inflate(R.layout.dailog, null);

        final EditText email = (EditText) dialogView.findViewById(R.id.ForgetPassword_Email); //here
        final Button NextProcess = (Button) dialogView.findViewById(R.id.nextProcessforget); //here
        final Button discard = (Button) dialogView.findViewById(R.id.cancle); //here

        discard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        NextProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mAuth.sendPasswordResetEmail(email.getText().toString().trim())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(),"Reset Email send to user "+email.getText().toString().trim(),Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                    try {
                                        throw task.getException();

                                    } catch (FirebaseAuthInvalidUserException e) {
                                        Toast.makeText(StartScreen.this, "no email in database found " + e.getErrorCode(), Toast.LENGTH_SHORT).show();
                                    } catch (FirebaseAuthInvalidCredentialsException e) {
                                        Toast.makeText(StartScreen.this, "Incorrect Credential " + e.getErrorCode(), Toast.LENGTH_SHORT).show();

                                    } catch (Exception e) {
                                        Toast.makeText(StartScreen.this, "Invalid Email " + e, Toast.LENGTH_SHORT).show();

                                    }

                                }
                            }
                        });


            }
        });




        builder.setView(dialogView);

        AlertDialog dialogUpdate=builder.create();
        dialogUpdate.show();

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void signInMethod(String SigninMethod)
    {
        sharedpreferences = getSharedPreferences(utilityConstant.MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();

        editor.putString(utilityConstant.signInMethod, SigninMethod);
        editor.commit();
    }

}

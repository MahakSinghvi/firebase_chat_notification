package com.ps.agrostand.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.ps.agrostand.R;
import com.ps.agrostand.common.AgrostandBaseActivity;
import com.ps.agrostand.databinding.ActivityOTPBinding;
import com.ps.agrostand.dto.UserDTO;
import com.ps.agrostand.fcm_chat.FCM_UserDTO;
import com.ps.agrostand.fcm_chat.FcmMainActivity;
import com.ps.agrostand.session.SessionManager;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import in.aabhasjindal.otptextview.OTPListener;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OTPActivity extends AgrostandBaseActivity {
    Context mContext;
    ActivityOTPBinding binding;
    SessionManager sessionManager;
    String valOtp = "";
    private String LOG_TAG = "OTPActivity";
    Bundle bundle;
    int otpId = 0;
    private DatabaseReference dbFirebase;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseAuth auth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_o_t_p);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_o_t_p);
        initValues();
        initFirebaseValue();
        clickEvents();
    }

    private void initValues() {
        mContext = this;
        sessionManager = new SessionManager(mContext);
        bundle = getIntent().getExtras();
        if (bundle != null) {
            otpId = bundle.getInt("userOtpId");
            Log.d(LOG_TAG, "\tuserOtpId\t" + otpId);
        }

        binding.editOTP.setOtpListener(new OTPListener() {
            @Override
            public void onInteractionListener() {
                // fired when user types something in the Otpbox
                valOtp = "";
            }

            @Override
            public void onOTPComplete(String otp) {
                // fired when user has entered the OTP fully.
                valOtp = otp;
            }
        });
        //todo counter for resend OTP
        new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {
                binding.tvResend.setText(getString(R.string.otp_resend_text) + " " + millisUntilFinished / 1000 + " " + getString(R.string.seconds));
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                binding.tvResend.setText(getString(R.string.resent_otp));
            }

        }.start();
    }

    private void initFirebaseValue() {
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    private void clickEvents() {
        binding.btnOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkValidation();
            }
        });

        binding.tvResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.tvResend.getText().toString().equals(getString(R.string.resent_otp))) {
//                    binding.linearOtp.startAnimation(shakeAnimation);
//                    vibrate(200);
//                    customToast(getString(R.string.send_otp));
                    resendOtpAPI();
                }
            }
        });

    }

    private void checkValidation() {
        if (valOtp.equalsIgnoreCase("") || valOtp.length() < 6) {
            binding.linearOtp.startAnimation(shakeAnimation);
            vibrate(200);
            customToast(getString(R.string.enter_proper_otp));
        } else {
//            customToast("Success");
            verifyOTP();

        }

    }

    //8555858558
    private void verifyOTP() {
        sessionManager.LoadingSpinner(mContext);
        sessionManager.dialogProgress.show();
        HashMap<String, String> params = new HashMap<>();
        params.put("user_otp_id", "" + otpId);
        params.put("otp_code", valOtp);
        params.put("role_id", "1");
        params.put("device_id", sessionManager.getFcmDeviceId());
       /* String lng = "en";
        if (sessionManager.getLanguageId().equals("2")) {
            lng = "en";
        } else {
            lng = "hn";
        }*/

        Call<ResponseBody> call = apiService.verify_otp(getHeaderValues(), params);
        Log.d(LOG_TAG, "\t\tparams\t\t" + params);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        sessionManager.dialogProgress.dismiss();
                        String responseRecieved = response.body().string();

                        Log.d(LOG_TAG, "\t\tresponseRecieved\t\t" + responseRecieved);
                        JSONObject jsonObjectResult = new JSONObject(responseRecieved);
                        if (jsonObjectResult.getBoolean("status")) {
                            if (jsonObjectResult.getBoolean("is_new")) {
                                sessionManager.dialogProgress.dismiss();
                                startActivity(new Intent(mContext, NewUserActivity.class));
                                sessionManager.setLogin(false);
                            } else {
                                sessionManager.dialogProgress.show();
//                                Toast.makeText(mContext, "Old", Toast.LENGTH_SHORT).show();
                                JSONObject jsonObjectData = jsonObjectResult.getJSONObject("data");
                                UserDTO userDTO = new UserDTO(jsonObjectData.getString("id"),
                                        jsonObjectData.getString("name"),
                                        jsonObjectData.getString("email"),
                                        jsonObjectData.getString("email_verified_at"),
                                        jsonObjectData.getString("api_token"),
                                        jsonObjectData.getString("mobile"),
                                        jsonObjectData.getString("otp"),
                                        jsonObjectData.getString("user_image"),
                                        jsonObjectData.getString("category_id"),
                                        jsonObjectData.getString("commodity_id"),
                                        jsonObjectData.getString("role_id"),
                                        jsonObjectData.getString("assured_id"),
                                        jsonObjectData.getString("language_id"),
                                        jsonObjectData.getString("device_id"),
                                        jsonObjectData.getString("status"),
                                        jsonObjectData.getString("is_new"),
                                        jsonObjectData.getString("created_at"),
                                        jsonObjectData.getString("updated_at"),
                                        jsonObjectData.getString("deleted_at"),
                                        jsonObjectData.getString("user_code")
                                );
                                sessionManager.setUserDetail(userDTO);
//                                sessionManager.setLogin(true);
//                                startActivity(new Intent(mContext, AgrostandActivity.class));
                                //todo store data in fcm
                                FCM_UserDTO fcmUserDTO = new FCM_UserDTO(
                                        sessionManager.getUserDetail().getId(),
                                        sessionManager.getUserDetail().getEmail(),
                                        sessionManager.getUserDetail().getName(),
                                        sessionManager.getUserDetail().getId(),
                                        TimeZone.getDefault().getID(),
                                        sessionManager.getLanguageId(),
                                        sessionManager.getUserDetail().getUser_image(),
                                        "", "1", "",
                                        "1", sessionManager.getFcmDeviceId()
                                );
                                Log.d(LOG_TAG, "onResponse: getEmail" + sessionManager.getUserDetail().getEmail());
                                signInFirebase(sessionManager.getUserDetail().getEmail(), "123456"
                                        , "1", fcmUserDTO);

                            }
//                            enterActivityAnimation();
                        } else {
                            sessionManager.dialogProgress.dismiss();
                            customToast(jsonObjectResult.getString("message"));
//                            showAlert("Error",jsonObjectResult.getString("message"),R.color.colorRed);
                            vibrate(200);
                        }
                    } else {
                        //if reponse is not successful
                        sessionManager.dialogProgress.dismiss();
                        customToast(getString(R.string.otp_not_match));
                    }

                } catch (Exception e) {
                    sessionManager.dialogProgress.dismiss();
                    Log.e(LOG_TAG, "Exception \t" + e.getMessage());
                    e.printStackTrace();
                    customToast(getString(R.string.otp_not_match));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(LOG_TAG, "ONFailure \t" + t.getMessage());
                sessionManager.dialogProgress.dismiss();
                customToast(getString(R.string.otp_not_match));
            }
        });


    }

    private void resendOtpAPI() {
        sessionManager.LoadingSpinner(mContext);
        sessionManager.dialogProgress.show();
        HashMap<String, String> params = new HashMap<>();
        params.put("user_otp_id", "" + otpId);
    /*    String lng = "en";
        if (sessionManager.getLanguageId().equals("2")) {
            lng = "en";
        } else {
            lng = "hn";
        }*/

        Call<ResponseBody> call = apiService.resend_otp(getHeaderValues(), params);
        Log.d(LOG_TAG, "\t\tparams\t\t" + params);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        sessionManager.dialogProgress.dismiss();
                        String responseRecieved = response.body().string();
                        Log.d(LOG_TAG, "\t\tresponseRecieved\t\t" + responseRecieved);
                        JSONObject jsonObjectResult = new JSONObject(responseRecieved);
                        if (jsonObjectResult.getBoolean("status")) {
                            customToast(jsonObjectResult.getString("message"));
                            JSONObject jsonObject = jsonObjectResult.getJSONObject("data");
                            otpId = jsonObject.getInt("user_otp_id");

                        } else {
                            customToast(jsonObjectResult.getString("message"));
                            vibrate(200);
                        }
                    }

                } catch (Exception e) {
                    sessionManager.dialogProgress.dismiss();
                    Log.e(LOG_TAG, "Exception \t" + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(LOG_TAG, "ONFailure \t" + t.getMessage());
                sessionManager.dialogProgress.dismiss();
            }
        });


    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        finish();
        exitActivityAnimation();
    }

    private void signInFirebase(final String vEmail, final String vPassword, final String vUserType,
                                final FCM_UserDTO _fcmUserDTO) {
        Log.d(LOG_TAG, "signInFirebase");
        auth.signInWithEmailAndPassword(vEmail, vPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "===INSIDE===");
                            //    dialogProgress.dismiss();
                            // Sign in success, update UI with the signed-in user's information
                            final FirebaseUser user = auth.getCurrentUser();
                            dbFirebase = FirebaseDatabase.getInstance().getReference("user");
                            dbFirebase.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    try {
                                        Log.d(LOG_TAG, "===INSIDE CHILD===");
                                        FCM_UserDTO fcm_userDTO = dataSnapshot.getValue(FCM_UserDTO.class);

                                        fcm_userDTO.setDevice_type("1");
                                        fcm_userDTO.setDevice_token(sessionManager.getFcmDeviceId());
                                        fcm_userDTO.setImage(_fcmUserDTO.getImage());
                                        fcm_userDTO.setLanguageId(sessionManager.getLanguageId());
                                        fcm_userDTO.setAgroUserId(sessionManager.getUserDetail().getId());
                                        fcm_userDTO.setCreated_date(Calendar.getInstance().getTime() + "");
                                        fcm_userDTO.setUsername(sessionManager.getUserDetail().getName());
                                        fcm_userDTO.setUser_id(auth.getUid());
                                        sessionManager.setFcmUserDetail(fcm_userDTO);
                                        dbFirebase.child(fcm_userDTO.getUser_id()).setValue(fcm_userDTO);
                                        sessionManager.dialogProgress.dismiss();
                                        sessionManager.setLogin(true);
                                        startActivity(new Intent(mContext, AgrostandActivity.class));
//                                        myLoginCondition(vUserType, _fcmUserDTO);
////                                        dialogProgress.dismiss();
//                                        Toast.makeText(mContext, "User Logged In.", Toast.LENGTH_SHORT).show();

                                    } catch (Exception e) {
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
//                                    dialogProgress.dismiss();
                                    Log.d(LOG_TAG, "===INSIDE CHILD ERROR===");
                                    Log.d(LOG_TAG, "@@firebase db@@" + databaseError.getDetails() + "\n message" + databaseError.getMessage());
                                    Toast.makeText(mContext, "Password Not Matched.", Toast.LENGTH_SHORT).show();
                                }
                            });


                        } else {
                            Log.d(LOG_TAG, "===OUTSIDE===");
                            try {
                                sessionManager.getUserDetail().setId(_fcmUserDTO.getUser_id());
                                FcmMainActivity.fcmUserDTO = new FCM_UserDTO();
                                FcmMainActivity.fcmUserDTO.setEmail(vEmail);
                                FcmMainActivity.fcmUserDTO.setUsername(_fcmUserDTO.getUsername());
                                FcmMainActivity.fcmUserDTO.setDevice_token(sessionManager.getFcmDeviceId());
                                FcmMainActivity.fcmUserDTO.setDevice_type("1");
                                FcmMainActivity.fcmUserDTO.setUser_type(vUserType);
                                FcmMainActivity.fcmUserDTO.setUsername(sessionManager.getUserDetail().getName());
                                FcmMainActivity.fcmUserDTO.setAgroUserId(_fcmUserDTO.getAgroUserId());
                                FcmMainActivity.fcmUserDTO.setCreated_date(Calendar.getInstance().getTime() + "");
                                FcmMainActivity.fcmUserDTO.setImage(_fcmUserDTO.getImage());
                                FcmMainActivity.fcmUserDTO.setTimeZone(TimeZone.getDefault().getID());
                                signUpUsingFirebase(vEmail, vPassword, vUserType, FcmMainActivity.fcmUserDTO);

                            } catch (Exception e) {
                                Log.e(LOG_TAG, "signInFirebase While login\t" + e);
                            }
                        }

                    }

                });
    }

    private void signUpUsingFirebase(final String vEmail, final String vPassword, final String vUserType,
                                     final FCM_UserDTO _fcmUserDTO) {

        auth.createUserWithEmailAndPassword(vEmail, vPassword).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                try {
                    if (task.isComplete()) {
                        String vUid = auth.getUid();
                        if (vUid != null) {
                            FcmMainActivity.fcmUserDTO.setUser_id(vUid);
                            dbFirebase = FirebaseDatabase.getInstance().getReference("user");
                            dbFirebase.child(vUid).setValue(FcmMainActivity.fcmUserDTO);
                            sessionManager.login();
                            sessionManager.setFcmUserDetail(FcmMainActivity.fcmUserDTO);
                            //  dialogProgress.dismiss();
                            Log.d(LOG_TAG, "===FIRE-BASE===");
                            signInFirebase(vEmail, vPassword, vUserType, _fcmUserDTO);

                        } else {
//                        dialogProgress.dismiss();
                            Log.d("Firebase", "===444===" + vUid);
                        }
                    } else {
//                    dialogProgress.dismiss();
                        Log.d("Firebase", "===555===");
                    }

                } catch (Exception e) {
                    Log.d(LOG_TAG, "signUpUsingFirebase:\t" + e);
                    ;
                }


            }

        });
    }

}
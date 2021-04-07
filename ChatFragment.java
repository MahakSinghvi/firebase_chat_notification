package com.ps.agrostand.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ps.agrostand.R;
import com.ps.agrostand.adapter.ChatAdapter;
import com.ps.agrostand.common.AgrostandBaseActivity;
import com.ps.agrostand.databinding.FragmentChatBinding;
import com.ps.agrostand.fcm_chat.FCM_UserDTO;
import com.ps.agrostand.fcm_chat.FcmMainActivity;
import com.ps.agrostand.fcm_chat.FriendDTO;
import com.ps.agrostand.fcm_chat.MessageActivity;
import com.ps.agrostand.session.SessionManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChatFragment extends Fragment {
    private String LOG_TAG = "ChatFragment";
    View view;
    Context mContext;
    SessionManager sessionManager;
    AgrostandBaseActivity baseActivity;
    FragmentChatBinding binding;
    private ArrayList<FriendDTO> friendDTOs;
    ChatAdapter chatAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_chat, container, false);
        initValues();
        initializeAdapter();
//        getUsersByRoleFirebase();
        getUsersByRoleFirebase();
        return view;

    }

    private void initValues() {
        view = binding.getRoot();
        mContext = getContext();
        friendDTOs = new ArrayList<>();
        sessionManager = new SessionManager(mContext);
        baseActivity = (AgrostandBaseActivity) mContext;
    }

    private void initializeAdapter() {
//        binding.recycleChat.setLayoutManager(new LinearLayoutManager(baseActivity, LinearLayoutManager.VERTICAL, false));
//        chatAdapter = new ChatAdapter(mContext, friendDTOs);
//        binding.recycleChat.setAdapter(chatAdapter);
        binding.recycleChat.setLayoutManager(new LinearLayoutManager(baseActivity, LinearLayoutManager.VERTICAL, false));
        chatAdapter = new ChatAdapter(this, friendDTOs, baseActivity);
        binding.recycleChat.setAdapter(chatAdapter);
    }

//    private void getUsersByRoleFirebase() {
//        try {
//            FirebaseDatabase.getInstance().getReference("Communicated_User")
//                    .child(sessionManager.getUserDetail().getId())
//                    .addValueEventListener(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                            try {
//                                friendDTOs.clear();
//                                if (dataSnapshot.exists() && dataSnapshot != null) {
//                                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
//                                        FriendDTO friendDTO = postSnapshot.getValue(FriendDTO.class);
//                                        friendDTOs.add(friendDTO);
//                                    }
//                                }
//
//                                Collections.sort(friendDTOs, new Comparator<FriendDTO>() {
//                                    //todo passing Local.US to handle date format in arabic
//                                    DateFormat _dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
//
//                                    @Override
//                                    public int compare(FriendDTO o1, FriendDTO o2) {
//                                        try {
//
//                                            return _dateFormat.parse(o1.getDate() + " " + o1.getTime())
//                                                    .compareTo(_dateFormat.parse(o2.getDate() + " " + o2.getTime()));
//                                        } catch (Exception e) {
//                                            throw new IllegalArgumentException(e);
//                                        }
//                                    }
//                                });
//                                Collections.reverse(friendDTOs);
//                                chatAdapter.notifyDataSetChanged();
//
//                            } catch (DatabaseException e) {
//                                Log.e(LOG_TAG, "onDataChange: "+e );;
//                            }
////                            intializeAdapter();
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                        }
//                    });
//        } catch (Exception e) {
//            Log.d("EXCEPTION", "=@@@==" + e);
//        }
//    }


    private void getUsersByRoleFirebase() {
//        binding.progressBar.setVisibility(View.VISIBLE);
        sessionManager.LoadingSpinner(mContext);
        sessionManager.dialogProgress.show();
        FirebaseDatabase.getInstance().getReference("Communicated_User")
                .child(sessionManager.getFCMData().getUser_id())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        friendDTOs.clear();
                        try {
                            Log.d(LOG_TAG, "onDataChange:---> " + dataSnapshot.toString());
                            if (dataSnapshot.exists() && dataSnapshot != null) {
                                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                                    FriendDTO friendDTO = postSnapshot.getValue(FriendDTO.class);
                                    friendDTOs.add(friendDTO);
                                }
                            }

                        } catch (DatabaseException e) {
                            Log.d(LOG_TAG, "onDataChange: " + dataSnapshot.toString());
                            Log.d(LOG_TAG, "onDataChange: " + e);
                            Log.d(LOG_TAG, "onDataChange: " + dataSnapshot.getKey());
                        }
                        Collections.sort(friendDTOs, new Comparator<FriendDTO>() {
                            DateFormat f = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

                            @Override
                            public int compare(FriendDTO o1, FriendDTO o2) {
                                try {
                                    return f.parse(o1.getDate() + " " + o1.getTime())
                                            .compareTo(f.parse(o2.getDate() + " " + o2.getTime()));
                                } catch (ParseException e) {
                                    throw new IllegalArgumentException(e);
                                }
                            }
                        });
//                        binding.progressBar.setVisibility(View.GONE);
                        Collections.reverse(friendDTOs);
                        chatAdapter.notifyDataSetChanged();
                        Log.d(LOG_TAG, "\tfriendDTOs\t" + friendDTOs.size());
                        if (friendDTOs.size() > 0) {
                            sessionManager.dialogProgress.dismiss();
                            binding.linearNoData.setVisibility(View.GONE);
                        } else {
                            sessionManager.dialogProgress.dismiss();
                            binding.linearNoData.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
//                        binding.progressBar.setVisibility(View.GONE);
                        sessionManager.dialogProgress.dismiss();
                    }
                });

    }

    public void setClick(final FriendDTO friendDTO) {
        FirebaseDatabase.getInstance().getReference("user")
                .child(friendDTO.getUser_id())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        try {
                            if (dataSnapshot != null) {
                                FCM_UserDTO userDTO = dataSnapshot.getValue(FCM_UserDTO.class);
                                FcmMainActivity.fcmUserDTO = new FCM_UserDTO();
                                FcmMainActivity.fcmUserDTO.setUser_id(friendDTO.getUser_id());
                                FcmMainActivity.fcmUserDTO.setUsername(friendDTO.getUsername());
                                FcmMainActivity.fcmUserDTO.setImage(friendDTO.getImage());
//                            FcmMainActivity.fcmUserDTO.setDevice_token(userDTO.getDevice_token());
                                if (!userDTO.getDevice_token().isEmpty()) {
                                    FcmMainActivity.fcmUserDTO.setDevice_token(userDTO.getDevice_token());
                                } else {
                                    FcmMainActivity.fcmUserDTO.setDevice_token(sessionManager.getFcmDeviceId());
                                }
                                startActivity(new Intent(baseActivity, MessageActivity.class));
                                baseActivity.enterActivityAnimation();
                            }

                        } catch (DatabaseException e) {
                            Log.d(LOG_TAG, "onDataChange: " + e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}

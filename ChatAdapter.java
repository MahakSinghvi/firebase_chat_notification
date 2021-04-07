package com.ps.agrostand.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ps.agrostand.R;
import com.ps.agrostand.common.AgrostandBaseActivity;
import com.ps.agrostand.databinding.AdapterChatBinding;
import com.ps.agrostand.fcm_chat.FriendDTO;
import com.ps.agrostand.fragments.ChatFragment;
import com.ps.agrostand.session.SessionManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private String LOG_TAG = "ChatAdapter";
    public static Map<String, Query> mapQuery;
    public static Map<String, ChildEventListener> mapChildListener;
    public static Map<String, Boolean> mapMark;
    private AgrostandBaseActivity baseActivity;
    private Fragment fragment;
    private ArrayList<FriendDTO> userDTOs;
    private DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private SessionManager sessionManager;
    private String vAuthUid = "";

    public ChatAdapter(Fragment fragment, ArrayList<FriendDTO> userDTOs, AgrostandBaseActivity baseActivity) {
        this.fragment = fragment;


        this.baseActivity = baseActivity;
        sessionManager = baseActivity.getInstanceSession();
        vAuthUid = sessionManager.getUserDetail().getId();
        this.userDTOs = userDTOs;
        mapQuery = new HashMap<>();
        mapChildListener = new HashMap<>();
        mapMark = new HashMap<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        AdapterChatBinding binding = AdapterChatBinding.inflate(LayoutInflater.from(baseActivity), viewGroup, false);
        return new ViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int position) {
        final FriendDTO friendDTO = userDTOs.get(viewHolder.getAdapterPosition());
        viewHolder.binding.nameTV.setText(friendDTO.getUsername());
        boolean isOnline = false;

        if (isOnline) {
            viewHolder.binding.onlineStatusLL.setBackgroundResource(R.drawable.online_green);
        } else {
            viewHolder.binding.onlineStatusLL.setBackgroundResource(R.drawable.offline_gray);
        }

        if (friendDTO.getMessage().trim().length() == 0) {
            viewHolder.binding.lastMessageTV.setText("attachment");
        } else {
            viewHolder.binding.lastMessageTV.setText(friendDTO.getMessage());
        }
        viewHolder.binding.dateTV
                .setText(baseActivity.getHHTohhTime(friendDTO.getDate() + " " + friendDTO.getTime(), friendDTO.getTime()));

        if (isOnline) {
            viewHolder.binding.onlineStatusLL.setBackgroundResource(R.drawable.online_green);
        } else {
            viewHolder.binding.onlineStatusLL.setBackgroundResource(R.drawable.offline_gray);
        }

        String vProfileImage = "";
        vProfileImage = friendDTO.getImage();
        if (vProfileImage.trim().length() > 0) {
            Glide.with(baseActivity).load(friendDTO.getImage()).apply(new RequestOptions().circleCrop()).into(viewHolder.binding.roleIV);
        } else {
            Glide.with(baseActivity).load(R.mipmap.avatar).apply(new RequestOptions().circleCrop()).into(viewHolder.binding.roleIV);
        }
        viewHolder.binding.itemLL.setTag(position);
        viewHolder.binding.itemLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = (int) v.getTag();
                if (fragment instanceof ChatFragment) {
                    ((ChatFragment) fragment).setClick(userDTOs.get(pos));
                }
            }
        });
        FirebaseDatabase.getInstance().getReference()
                .child("user/" + friendDTO.getUser_id())
                .child("status")
                .child("isOnline")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        try {
                            Boolean isOnline = dataSnapshot.getValue(Boolean.class);
                            System.out.println("love...onChildAdded.." + isOnline);
                            if (isOnline) {
                                viewHolder.binding.onlineStatusLL.setBackgroundResource(R.drawable.online_green);
                            } else {
                                viewHolder.binding.onlineStatusLL.setBackgroundResource(R.drawable.offline_gray);
                            }
                        } catch (Exception e) {
                            Log.d(LOG_TAG, "onDataChange:Exception " + e);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return userDTOs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private AdapterChatBinding binding;

        public ViewHolder(AdapterChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

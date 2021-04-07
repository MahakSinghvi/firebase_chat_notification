package com.ps.agrostand.fcm_chat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ps.agrostand.R;
import com.ps.agrostand.app.MyApplication;
import com.ps.agrostand.common.AgrostandBaseActivity;
import com.ps.agrostand.databinding.ChatMessageRow1Binding;
import com.ps.agrostand.databinding.MessageViewBinding;
import com.ps.agrostand.session.SessionManager;
import com.ps.agrostand.utils.FileUtils;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.util.List;
import java.util.UUID;

import pub.devrel.easypermissions.EasyPermissions;

public class MessageActivity extends AgrostandBaseActivity implements View.OnClickListener, EasyPermissions.PermissionCallbacks {
    private String LOG_TAG = "MessageActivity";
    private DatabaseReference dbFirebase, dbUserFirebase;
    private FirebaseAuth auth;
    private MessageViewBinding binding;
    private MessageDTO messageDTO;
    private LinearLayoutManager linearLayoutManager;
    private FirebaseRecyclerAdapter<MessageDTO, MessageViewHolder> mFirebaseAdapter;
    private SessionManager sessionManager;
    private String ACTION_TYPE = "";
    private Uri profileUri;
    private String vImageUrl = "";
    private StorageReference storageReference;
    private FirebaseStorage storage;
    private String vSenderUID = "";
    private boolean is_last_message_update = false;
    private boolean is_from_notification = false;
    Bundle bundle;
    String vUniqueID = "";
    Context mContext;
    File attachedFile, mFile;
    int maxFileSize = 10240;
    private ProgressDialog progressDialog;
    private Uri fileUri;

    ChatMessageRow1Binding _row1Binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.message_view);
        initializeViews();
    }

    private void initializeViews() {
        try {
            mContext = this;
            sessionManager = new SessionManager(this);
            auth = FirebaseAuth.getInstance();
            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();
            binding.sendLL.setOnClickListener(this);
            binding.imgsendLL.setOnClickListener(this);
            binding.imgBack.setOnClickListener(this);
            binding.selectAttachments.setOnClickListener(this);
            dbFirebase = FirebaseDatabase.getInstance().getReference("messages");
            dbUserFirebase = FirebaseDatabase.getInstance().getReference("user");
            bundle = getIntent().getExtras();

            if (bundle != null) {

//                binding.jobNameTV.setText(getString(R.string.groupName) + " : " + bundle.getString("jobName"));
//                binding.jobNumberTV.setText(bundle.getString("jobNumberId"));

                if (getIntent().hasExtra("sender_uid")) {
                    is_from_notification = true;
                    vSenderUID = getIntent().getStringExtra("sender_uid");
                    dbUserFirebase.child(vSenderUID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            FcmMainActivity.fcmUserDTO = dataSnapshot.getValue(FCM_UserDTO.class);
//                            vUniqueID = sessionManager.getUniqueId(sessionManager.getUserDetail().getId() + "!*!",
                            vUniqueID = sessionManager.getUniqueId(sessionManager.getFCMData().getUser_id() + "!*!",
                                    FcmMainActivity.fcmUserDTO.getUser_id() + "!*!");
                            setAdapter();
                            onResume();
                            lastMessageListener();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                } else {
//                    vUniqueID = sessionManager.getUniqueId(sessionManager.getUserDetail().getId() + "!*!" ,
                    vUniqueID = sessionManager.getUniqueId(sessionManager.getFCMData().getUser_id() + "!*!",
                            FcmMainActivity.fcmUserDTO.getUser_id() + "!*!");
                    setAdapter();
                    lastMessageListener();
                }
            } else {
//                vUniqueID = sessionManager.getUniqueId(sessionManager.getUserDetail().getId() + "!*!",
                vUniqueID = sessionManager.getUniqueId(sessionManager.getFCMData().getUser_id() + "!*!",
                        FcmMainActivity.fcmUserDTO.getUser_id() + "!*!");
                setAdapter();
                lastMessageListener();
            }
//            Log.d(LOG_TAG, "===JOB NUMBER---ID---\t" + bundle.getString("jobNumberId"));
            Log.d(LOG_TAG, "===SESSION---ID---\t" + sessionManager.getUserDetail().getId());
            Log.d(LOG_TAG, "===MAIN---ID---\t" + FcmMainActivity.fcmUserDTO.getUser_id());
            Log.d(LOG_TAG, "===vUniqueID---\t" + vUniqueID);
//            Log.d(LOG_TAG, "===getFcmUserId---\t" + sessionManager.getUserDetail().getFcmUserId());
            Log.d(LOG_TAG, "===getFcmUserId---\t" + sessionManager.getFCMData().getUser_id());


        } catch (Exception e) {
            Log.d(LOG_TAG, "-Exception-initializeViews-\t" + e);
        }
    }

    private void setAdapter() {
//        messageDTOs = new ArrayList<>();
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(true);

        binding.messageRV.setLayoutManager(linearLayoutManager);

        SnapshotParser<MessageDTO> parser = new SnapshotParser<MessageDTO>() {
            @Override
            public MessageDTO parseSnapshot(DataSnapshot dataSnapshot) {
                MessageDTO friendlyMessage = dataSnapshot.getValue(MessageDTO.class);
                if (friendlyMessage != null) {
                    friendlyMessage.setId(dataSnapshot.getKey());
                }
                return friendlyMessage;
            }
        };

        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(vUniqueID + "");

        FirebaseRecyclerOptions<MessageDTO> options = new FirebaseRecyclerOptions.Builder<MessageDTO>()
                .setQuery(messagesRef, parser)
                .build();
        //todo show date

        mFirebaseAdapter = new FirebaseRecyclerAdapter<MessageDTO, MessageViewHolder>(options) {
            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                _row1Binding = DataBindingUtil.inflate(inflater, R.layout.chat_message_row1, viewGroup, false);
                //  setHasStableIds(true);
//                return new MessageViewHolder(inflater.inflate(R.layout.chat_message_row1, viewGroup, false));
                return new MessageViewHolder(_row1Binding);
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            final MessageDTO messageDTO) {

                //todo for set file size
                setDynamicHeightWithOfFiles(viewHolder);
                viewHolder.row1Binding.messageTV.setText(messageDTO.getMessage());
                viewHolder.row1Binding.messageTVTV.setText(messageDTO.getMessage());
                //todo for sender end
                if (messageDTO.getUser_id().equalsIgnoreCase(auth.getUid())) {

                    viewHolder.row1Binding.messageBoxSenderLL.setVisibility(View.VISIBLE);
                    viewHolder.row1Binding.messageBoxReceiverLL.setVisibility(View.GONE);
                    Log.d(LOG_TAG, "@@IMAGE@@" + messageDTO.getImage());

                    if (messageDTO.getImage().trim().length() > 0) {
                        viewHolder.row1Binding.messageTV.setVisibility(View.GONE);
                        viewHolder.row1Binding.messageBoxSenderLL.setBackgroundResource(R.drawable.message_chat_box_trans);
                        //todo for showing attachment with file name
                        showAttachmentsSender(messageDTO.getImage(), viewHolder, /*nameFile*/messageDTO.getOriginalName());

                        if (messageDTO.getImage().contains("agrostand_images")) { //todo for image files

                            viewHolder.row1Binding.mrLinearSender.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    startActivity(new Intent(MessageActivity.this, ImageZoomActivity.class).putExtra("IMAGE_URL", messageDTO.getImage()));
                                }
                            });
                        } else if (!messageDTO.getImage().contains("agrostand_video")) {
                            //todo for other files
//
                        } else {
                            //todo for video file
//
                        }
                        viewHolder.row1Binding.mrLinearAttachFiles.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    String url = messageDTO.getImage();
                                    Log.d(LOG_TAG, "=URLLLLL=> " + url);
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setData(Uri.parse(url));
                                    startActivity(i);

                                } catch (Exception e) {
                                }

                            }
                        });


                    } else {
                        viewHolder.row1Binding.messageBoxSenderLL.setBackgroundResource(R.drawable.message_chat_box_start);
                        viewHolder.row1Binding.messageTV.setVisibility(View.VISIBLE);
//                        if (!sessionManager.getUserDetail().getUser_image().isEmpty()) {
//                            Picasso.with(mContext).load(sessionManager.getUserDetail().getUser_image()).error(R.mipmap.avatar).into(viewHolder.row1Binding.imgUserProfile);
//                        }
                        viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
                        viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.GONE);
                    }
                }
                //todo for receiver end
                else {
                    viewHolder.row1Binding.messageBoxSenderLL.setVisibility(View.GONE);
                    viewHolder.row1Binding.messageBoxReceiverLL.setVisibility(View.VISIBLE);
                    if (messageDTO.getImage().trim().length() > 0) {
                        viewHolder.row1Binding.messageTVTV.setVisibility(View.GONE);
                        viewHolder.row1Binding.messageBoxReceiverLL.setBackgroundResource(R.drawable.message_chat_box_trans);
                        //todo for showing attachment with file name
                        showAttachmentsReceiver(messageDTO.getImage(), viewHolder, /*nameFile*/messageDTO.getOriginalName());

                        if (messageDTO.getImage().contains("agrostand_images")) { //todo for image files
                            viewHolder.row1Binding.mrLinearReceiver.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    startActivity(new Intent(MessageActivity.this, ImageZoomActivity.class).putExtra("IMAGE_URL", messageDTO.getImage()));
                                }
                            });
                        } else if (!messageDTO.getImage().contains("agrostand_video")) { //todo for other files
//

                        } else { //todo for video file
//
                        }
                        viewHolder.row1Binding.mrLinearReceiverAttachFiles.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    String url = messageDTO.getImage();
                                    Log.d("URLLLLL", "==> " + url);
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setData(Uri.parse(url));
                                    startActivity(i);

                                } catch (Exception e) {
                                }

                            }
                        });

                    } else {
                        viewHolder.row1Binding.messageBoxReceiverLL.setBackgroundResource(R.drawable.message_chat_box_end);
                        viewHolder.row1Binding.messageTVTV.setVisibility(View.VISIBLE);
                        Log.d(LOG_TAG, "--Receiver-\t\nuserType\t" + messageDTO.getLoginType() + "\nImage\t" + messageDTO.getUserImage());
                        viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.GONE);
                        viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.GONE);
                    }
                }

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        linearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    binding.messageRV.scrollToPosition(positionStart);
                }

            }
        });

        binding.messageRV.setAdapter(mFirebaseAdapter);

        Log.d(LOG_TAG, "--COUNT--\t" + mFirebaseAdapter.getItemCount());

    }

    private void setDynamicHeightWithOfFiles(MessageViewHolder viewHolder) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);
        int widthInDP = Math.round(dm.widthPixels);
        int width = (int) (widthInDP * 0.6f);
        int height = (int) (width * 0.5f);
        viewHolder.row1Binding.imgFileSender.getLayoutParams().width = width;
        viewHolder.row1Binding.imgFileSender.getLayoutParams().height = height;
        viewHolder.row1Binding.imgFileSender.setScaleType(ImageView.ScaleType.CENTER_CROP);
        viewHolder.row1Binding.imgFileSender.setPadding(10, 10, 10, 10);
        viewHolder.row1Binding.imgFileReceiver.getLayoutParams().width = width;
        viewHolder.row1Binding.imgFileReceiver.getLayoutParams().height = height;
        viewHolder.row1Binding.imgFileReceiver.setScaleType(ImageView.ScaleType.CENTER_CROP);
        viewHolder.row1Binding.imgFileReceiver.setPadding(10, 10, 10, 10);
        viewHolder.row1Binding.mrLinearAttachFiles.getLayoutParams().width = width;
        viewHolder.row1Binding.mrLinearReceiverAttachFiles.getLayoutParams().width = width;
    }

    private String separateNameFromFile(String[] nameToSplit) {
        nameToSplit[0] = nameToSplit[0].trim();
        String nameOfFile = nameToSplit[0].trim();
        nameToSplit[1] = nameToSplit[1].trim();
        String[] splitAgain = nameOfFile.split("!_");
        splitAgain[0] = splitAgain[0].trim();

        Log.d("STRING", "@@@000@@@===> " + nameToSplit[0].toString());
        Log.d("STRING", "@@@111@@@===> " + nameToSplit[1].toString());
        Log.d("STRING", "@@@222@@@===> " + splitAgain[0].toString());
        return splitAgain[0];
    }


    private void showAttachmentsSender(String fileContainer, MessageViewHolder _viewHolder, String fileName) {
        //   _viewHolder.img_green_arrow.setVisibility(View.GONE);
        if (fileContainer.contains("agrostand_images")) {
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.GONE);
            Glide.with(_viewHolder.row1Binding.imgFileSender.getContext())
                    .load(fileContainer)
                    .into(_viewHolder.row1Binding.imgFileSender);
        }
        if (fileContainer.contains("agrostand_pdf")) {
            _viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            Log.d(LOG_TAG, "@@@@PDF SENDER@@@");
            _viewHolder.row1Binding.imgAttachFile.setImageResource(R.drawable.icon_sender_pdf);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
//
        }
        if (fileContainer.contains("agrostand_doc")) {
            Log.d(LOG_TAG, "@@@@DOC SENDER@@@");
            _viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.imgAttachFile.setImageResource(R.drawable.icon_sender_doc);
        }
        if (fileContainer.contains("agrostand_apk")) {
            Log.d(LOG_TAG, "@@@@DOC SENDER@@@");
            _viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgAttachFile.setImageResource(R.drawable.icon_sender_apk);
//
        }
        if (fileContainer.contains("agrostand_excel")) {
            Log.d(LOG_TAG, "@@@@DOC SENDER@@@");
            _viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgAttachFile.setImageResource(R.drawable.icon_sender_xls);
//
        }
        if (fileContainer.contains("agrostand_zip")) {
            Log.d(LOG_TAG, "@@@@DOC SENDER@@@");
            _viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgAttachFile.setImageResource(R.drawable.icon_sender_zip);
//
        }
        if (fileContainer.contains("agrostand_ppt")) {
            Log.d(LOG_TAG, "@@@@DOC SENDER@@@");
            _viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgAttachFile.setImageResource(R.drawable.icon_sender_ppt);
//
        }
        if (fileContainer.contains("agrostand_audio")) {
            Log.d(LOG_TAG, "@@@@DOC SENDER@@@");
            _viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgAttachFile.setImageResource(R.drawable.icon_sender_audio);
        }
        if (fileContainer.contains("agrostand_video")) {
            Log.d(LOG_TAG, "@@@@DOC SENDER@@@");
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearAttachFiles.setVisibility(View.GONE);
        }
    }

    private void showAttachmentsReceiver(String fileContainer, MessageViewHolder _viewHolder, String fileName) {
        // _viewHolder.row1Binding.img_green_arrow.setVisibility(View.GONE);
        if (fileContainer.contains("agrostand_images")) {
            _viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.GONE);
            Log.d(LOG_TAG, "@@@@IMG RECEIVER@@@");
            _viewHolder.row1Binding.tvReceiverTypeOfFile.setText("IMAGE");
            Glide.with(_viewHolder.row1Binding.imgFileReceiver.getContext())
                    .load(fileContainer)
                    .into(_viewHolder.row1Binding.imgFileReceiver);
        }
        if (fileContainer.contains("agrostand_pdf")) {
            _viewHolder.row1Binding.tvReceiverFile.setText(fileName);
            _viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.GONE);
            _viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.VISIBLE);
            Log.d(LOG_TAG, "@@@@PDF RECEIVER@@@");
            _viewHolder.row1Binding.imgReceiverAttachFile.setImageResource(R.drawable.icon_receiver_pdf);
        }
        if (fileContainer.contains("agrostand_doc")) {
            _viewHolder.row1Binding.tvReceiverFile.setText(fileName);
            _viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.GONE);
            _viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.VISIBLE);
            Log.d(LOG_TAG, "@@@@DOC RECEIVER@@@");
            _viewHolder.row1Binding.imgReceiverAttachFile.setImageResource(R.drawable.icon_receiver_doc);
        }
        if (fileContainer.contains("agrostand_apk")) {
            _viewHolder.row1Binding.tvReceiverFile.setText(fileName);
            _viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.GONE);
            Log.d(LOG_TAG, "@@@@APK RECEIVER@@@");
            _viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgReceiverAttachFile.setImageResource(R.drawable.icon_receiver_apk);
//
        }
        if (fileContainer.contains("agrostand_excel")) {
            _viewHolder.row1Binding.tvReceiverFile.setText(fileName);
            _viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.GONE);
            Log.d(LOG_TAG, "@@@@APK RECEIVER@@@");
            _viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgReceiverAttachFile.setImageResource(R.drawable.icon_receiver_xls);
//
        }
        if (fileContainer.contains("agrostand_zip")) {
            _viewHolder.row1Binding.tvReceiverFile.setText(fileName);
            _viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.GONE);
            Log.d(LOG_TAG, "@@@@APK RECEIVER@@@");
            _viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgReceiverAttachFile.setImageResource(R.drawable.icon_receiver_zip);
//
        }
        if (fileContainer.contains("agrostand_ppt")) {
            _viewHolder.row1Binding.tvReceiverFile.setText(fileName);
            _viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.GONE);
            Log.d(LOG_TAG, "@@@@APK RECEIVER@@@");
            _viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgReceiverAttachFile.setImageResource(R.drawable.icon_receiver_ppt);
//
        }
        if (fileContainer.contains("agrostand_audio")) {
            _viewHolder.row1Binding.tvReceiverFile.setText(fileName);
            _viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.GONE);
            Log.d(LOG_TAG, "@@@@APK RECEIVER@@@");
            _viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearSender.setVisibility(View.GONE);
            _viewHolder.row1Binding.tvSenderFile.setText(fileName);
            _viewHolder.row1Binding.imgReceiverAttachFile.setImageResource(R.drawable.icon_receiver_audio);
        }
        if (fileContainer.contains("agrostand_video")) {
            _viewHolder.row1Binding.mrLinearReceiver.setVisibility(View.VISIBLE);
            _viewHolder.row1Binding.mrLinearReceiverAttachFiles.setVisibility(View.GONE);
            Log.d(LOG_TAG, "@@@@IMG RECEIVER@@@");
            _viewHolder.row1Binding.tvReceiverTypeOfFile.setText("IMAGE");

        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.img_back:
//                checkPermission();
                finish();
                exitActivityAnimation();
                break;
            case R.id.selectAttachments:
//                checkPermission();
                showFileDialog();
                break;

            case R.id.imgsendLL:
                if (binding.messageET.getText().toString().trim().length() == 0) {
                    Toast.makeText(this, "Enter Message.", Toast.LENGTH_SHORT).show();
                } else {

                    if (mFirebaseAdapter.getItemCount() == 0) {
                        addAsFriend(FcmMainActivity.fcmUserDTO.getUser_id());
                    }
                    is_last_message_update = true;
//                    updateTypingStatus(false);

//                    updateNewMessageStatus(true, FcmMainActivity.userDTO.getUser_id(), sessionManager.getUserDetail().getUser_id());
                    messageDTO = new MessageDTO();
                    messageDTO.setMessage(binding.messageET.getText().toString().trim());
                    messageDTO.setDate(sessionManager.getCurrentDateTime().split("&")[0]);
                    messageDTO.setTime(sessionManager.getCurrentDateTime().split("&")[1]);
                    messageDTO.setUser_id(auth.getUid());
                    dbFirebase.child(vUniqueID + "").push().setValue(messageDTO);
                    dbUserFirebase.child(auth.getUid() + "/message/message").setValue(binding.messageET.getText().toString().trim());
                    dbUserFirebase.child(auth.getUid() + "/message/timestamp").setValue(System.currentTimeMillis());
//                    dbUserFirebase.child(FcmMainActivity.fcmUserDTO.getUser_id() + "/message/message").setValue(binding.messageET.getText().toString().trim());
//                    dbUserFirebase.child(FcmMainActivity.fcmUserDTO.getUser_id() + "/message/timestamp").setValue(System.currentTimeMillis());
                    sessionManager.sendNotification(FcmMainActivity.fcmUserDTO.getDevice_token(),
                            sessionManager.getUserDetail().getName() + " send a message",
                            binding.messageET.getText().toString().trim(), sessionManager.getUserDetail().getId());
                    binding.messageET.setText("");
//                    messageAdapter.notifyDataSetChanged();
                }
                break;
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        ACTION_TYPE = "TAKE_IMAGE";
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        ChatMessageRow1Binding row1Binding;

        public MessageViewHolder(/*View v*/ChatMessageRow1Binding _row1Binding) {
            super(_row1Binding.getRoot());
            row1Binding = _row1Binding;
        }
    }


    private void showFileDialog() {
        final CharSequence[] options = {"Take Pic", "Select File", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        builder.setTitle("Attachment");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Pic")) {
                    checkPermission();
                } else if (options[item].equals("Select File")) {
//                    Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                    startActivityForResult(intent, 2);
                    ACTION_TYPE = "ATTACH_FILES";
                    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                    chooseFile.setType("*/*");
                    chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                    startActivityForResult(chooseFile, 1212);

                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void checkPermission() {
        String[] perms = {/*Manifest.permission.ACCESS_FINE_LOCATION,*/ Manifest.permission.READ_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            ACTION_TYPE = "TAKE_IMAGE";
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
//            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
        } else {
            EasyPermissions.requestPermissions(this, "We need permissions",
                    123, perms);
        }
    }

    private void addAsFriend(final String vFriendUid) {
        Log.d(LOG_TAG, "addAsFriend: " + vFriendUid + "\n" + sessionManager.getFCMData().getUser_id());
        if (!MyApplication.listFriendID.contains(vFriendUid)) {
            FirebaseDatabase.getInstance().getReference("friend/" + sessionManager.getFCMData().getUser_id())
                    .push().setValue(vFriendUid).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    MyApplication.listFriendID.add(vFriendUid);
                    FirebaseDatabase.getInstance().getReference("friend/" + vFriendUid)
                            .push().setValue(sessionManager.getFCMData().getUser_id()).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            MyApplication.listFriendID.add(vFriendUid);
                            //  Toast.makeText(MessageActivity.this, "Added as Friend.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }


    private void lastMessageListener() {
        Log.d("UniqueId", "@##@@" + vUniqueID);
        FirebaseDatabase.getInstance().getReference("messages")
                .child(vUniqueID + "")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        if (is_last_message_update) {
                            is_last_message_update = false;
                            MessageDTO messageDTO = dataSnapshot.getValue(MessageDTO.class);


                            // Toast.makeText(MessageActivity.this, "" + messageDTO.getMessage(), Toast.LENGTH_SHORT).show();
                            FriendDTO friendDTO = new FriendDTO();

                            friendDTO.setDate(sessionManager.getCurrentDateTime().split("&")[0]);
                            friendDTO.setTime(sessionManager.getCurrentDateTime().split("&")[1]);
                            friendDTO.setMessage(messageDTO.getMessage());

                            friendDTO.setUser_id(FcmMainActivity.fcmUserDTO.getUser_id());
                            friendDTO.setUsername(FcmMainActivity.fcmUserDTO.getUsername());
                            friendDTO.setImage(FcmMainActivity.fcmUserDTO.getImage());
                            friendDTO.setUserType(messageDTO.getLoginType());

                            FirebaseDatabase.getInstance().getReference("Communicated_User")
                                    .child(sessionManager.getFCMData().getUser_id())
                                    .child(FcmMainActivity.fcmUserDTO.getUser_id())
                                    .setValue(friendDTO);


                            FriendDTO friendDTO1 = new FriendDTO();

                            friendDTO1.setDate(sessionManager.getCurrentDateTime().split("&")[0]);
                            friendDTO1.setTime(sessionManager.getCurrentDateTime().split("&")[1]);
                            friendDTO1.setMessage(messageDTO.getMessage());

//                            friendDTO1.setUser_id(sessionManager.getUserDetail().getId());
                            friendDTO1.setUser_id(sessionManager.getFCMData().getUser_id());
                            friendDTO1.setUsername(sessionManager.getUserDetail().getName());
                            friendDTO1.setImage(sessionManager.getUserDetail().getUser_image());

                            FirebaseDatabase.getInstance().getReference("Communicated_User")
                                    .child(FcmMainActivity.fcmUserDTO.getUser_id())
                                    .child(sessionManager.getFCMData().getUser_id())
                                    .setValue(friendDTO1);
//                            binding.infoTV.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    @Override
    public void onPause() {
        MyApplication.activityPaused();
        if (mFirebaseAdapter != null)
            mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        MyApplication.activityResumed();
        if (mFirebaseAdapter != null)
            mFirebaseAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public File getFile() {
        return mFile;
    }

    public void setFile(File mFile) {
        this.mFile = mFile;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ACTION_TYPE.equals("TAKE_IMAGE")) {
            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    profileUri = result.getUri();
                    // try {
//                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileUri);
//                        Glide.with(this).load(bitmap).apply(new RequestOptions().circleCrop()).into(binding.profileIV);
                    uploadImage();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                    Toast.makeText(MessageActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
        if (ACTION_TYPE.equals("ATTACH_FILES")) {
            fileAttachmentCode(resultCode, data);

        }
    }

    private void fileAttachmentCode(int resultCode, Intent data) {
        try {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                fileUri = data.getData();
                String uriString = uri.toString();
                File myFile = new File(uriString);
                String displayName = null;
                Log.d("##PATH-FILE##", "=path==> " + data.getData().getPath() + "\n\nURI " + uri +
                        "\n\nURI String " + uriString + "\n\n##DATA##===> " + data + "\n\nDATA@@@===> " + data.getData());
                if (uriString.startsWith("content://")) {
                    Cursor cursor = null;
                    try {
                        cursor = mContext.getContentResolver().query(uri, null, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            Log.d("NAME_OF_FILE", "~~~displayName~~~\t" + displayName);
//                            getDriveFilePath(uri, mContext);
                            if (displayName.contains(".pdf")) {
                                String fileStr1 = FileUtils.getPath(mContext, uri);
                                Log.d("MYVALUE", "==PATH==FILE==" + fileStr1);
                                attachedFile = new File(fileStr1);
                                if (checkFileSize(attachedFile) > maxFileSize) {
                                    vibrate(200);
                                    customToast(getString(R.string.stringMessageFileSizeError));
                                } else {
                                    setFile(attachedFile);
                                    uploadAttachment(displayName, "pdf");
//                                    sendPDFMessage(displayName);
                                }
                            } else if (displayName.contains(".doc")
                                    || displayName.contains(".docx")/*
                                    || displayName.contains(".txt")*/) {
                                String fileStr2 = FileUtils.getPath(mContext, uri);
                                attachedFile = new File(fileStr2);

                                if (checkFileSize(attachedFile) > maxFileSize) {
                                    vibrate(200);
                                    customToast(getString(R.string.stringMessageFileSizeError));
                                } else {
                                    setFile(attachedFile);
//                                    sendDocMessage(displayName);
                                    uploadAttachment(displayName, "doc");
                                }
                            } else if (displayName.contains(".xls") || displayName.contains(".xlsx")) {
                                String fileSt3r = FileUtils.getPath(mContext, uri);
                                attachedFile = new File(fileSt3r);
                                if (checkFileSize(attachedFile) > maxFileSize) {
                                    vibrate(200);
                                    customToast(getString(R.string.stringMessageFileSizeError));
                                } else {
                                    setFile(attachedFile);
//                                    sendExcelMessage(displayName);
                                    uploadAttachment(displayName, "xls");
                                }
                            } else if (displayName.contains(".jpg") || displayName.contains(".jpeg") || displayName.contains(".png") || displayName.contains(".gif")) {
                                String fileStr4 = FileUtils.getPath(mContext, uri);
//                                attachedFile = new File(fileStr4);
//                                setFile(attachedFile);
                                ACTION_TYPE = "TAKE_IMAGE";
                                profileUri = uri;
                                uploadImage();
                            } else {
                                Toast.makeText(mContext, "Choose Other file", Toast.LENGTH_SHORT).show();
                            }
                            setAdapter();
                            Log.d("PATH", "=display name==> " + displayName);
                        }
                    } finally {
                        cursor.close();
                    }
                } else if (uriString.startsWith("file://")) {

                    displayName = myFile.getName();

                    Log.d("PATH", "@@@@@@@displayName==> " + displayName);
                }
            }

        } catch (Exception ew) {
            Log.d(LOG_TAG, "@@@@Select file@@@ERROR==>\t\t" + ew);
            try {
                //todo we are trying to get the file stored in drive
                Uri returnUri = fileUri;
                Cursor returnCursor = mContext.getContentResolver().query(returnUri, null, null, null, null);
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                returnCursor.moveToFirst();
                String name = (returnCursor.getString(nameIndex));
                String size = (Long.toString(returnCursor.getLong(sizeIndex)));
                File file = new File(mContext.getCacheDir(), name);
                Log.d(LOG_TAG, "~~~driveName~~~\t" + name + "\t" + size + "\t" + size);
                if (name.contains(".pdf")) {
                    attachedFile = file;
                    if (Integer.parseInt(size) > maxFileSize) {
                        vibrate(200);
                        customToast(getString(R.string.stringMessageFileSizeError));
                    } else {
                        setFile(attachedFile);
//                        sendPDFMessage(name);
                        uploadAttachment(name, "pdf");
                    }
                } else if (name.contains(".doc") || name.contains(".docx") || name.contains(".txt")) {
                    attachedFile = file;
                    if (Integer.parseInt(size) > maxFileSize) {
                        vibrate(200);
                        customToast(getString(R.string.stringMessageFileSizeError));
                    } else {
                        setFile(attachedFile);
//                        sendDocMessage(name);
                        uploadAttachment(name, "doc");
                    }
                } else if (name.contains(".xls") || name.contains(".xlsx")) {
                    attachedFile = file;
                    if (Integer.parseInt(size) > maxFileSize) {
                        vibrate(200);
                        customToast(getString(R.string.stringMessageFileSizeError));
                    } else {
                        setFile(attachedFile);
                        uploadAttachment(name, "xls");
//                        sendExcelMessage(name);
                    }
                } else if (name.contains(".jpg") || name.contains(".jpeg") || name.contains(".png") || name.contains(".gif")) {
                    ACTION_TYPE = "TAKE_IMAGE";
                    profileUri = fileUri;
                    uploadImage();
                }
            } catch (Exception z) {
                Log.e(LOG_TAG, "@@@@@@@ERROR=Drive=>\t" + z);
            }

        }
    }

    private long checkFileSize(File attachedFile) {
        long fileSizeInBytes = attachedFile.length();
        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
        long fileSizeInKB = fileSizeInBytes / 1024;
        return fileSizeInKB;
    }

    private void uploadImage() {
        if (profileUri != null) {
//            final ProgressDialog progressDialog = new ProgressDialog(this);
//            progressDialog.setTitle("Uploading...");
//            progressDialog.show();
            sessionManager.LoadingSpinner(mContext);
            sessionManager.dialogProgress.setCanceledOnTouchOutside(false);
            sessionManager.dialogProgress.setCancelable(false);
            sessionManager.dialogProgress.show();
//            StorageReference ref = storageReference.child("profile_images/" + UUID.randomUUID().toString());
            StorageReference ref = storageReference.child("agrostand_images/" + UUID.randomUUID().toString());
            ref.putFile(profileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            sessionManager.dialogProgress.dismiss();
                            Task<Uri> uriTask = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                            uriTask.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    vImageUrl = uri.toString();

                                    if (mFirebaseAdapter.getItemCount() == 0) {
                                        addAsFriend(FcmMainActivity.fcmUserDTO.getUser_id());
                                    }
                                    is_last_message_update = true;
                                    messageDTO = new MessageDTO();
                                    messageDTO.setMessage("");
                                    messageDTO.setImage(vImageUrl);
                                    messageDTO.setFileType("IMAGE");
                                    messageDTO.setDate(sessionManager.getCurrentDateTime().split("&")[0]);
                                    messageDTO.setTime(sessionManager.getCurrentDateTime().split("&")[1]);
                                    messageDTO.setUser_id(auth.getUid());
                                    messageDTO.setOriginalName("");
                                    dbFirebase.child(vUniqueID + "").push().setValue(messageDTO);
                                    dbUserFirebase.child(auth.getUid() + "/message/message").setValue("image");
                                    dbUserFirebase.child(auth.getUid() + "/message/timestamp").setValue(System.currentTimeMillis());
                                    dbUserFirebase.child(FcmMainActivity.fcmUserDTO.getUser_id() + "/message/message").setValue("image");
                                    dbUserFirebase.child(FcmMainActivity.fcmUserDTO.getUser_id() + "/message/timestamp").setValue(System.currentTimeMillis());
//                                    sendNotification(FcmMainActivity.userDTO.getDevice_token(), sessionManager.getUserDetail().getUsername() + " send an image", "image", sessionManager.getUserDetail().getUser_id());
                                    sessionManager.sendNotification(FcmMainActivity.fcmUserDTO.getDevice_token(),
                                            sessionManager.getUserDetail().getName() + "file attached",
                                            binding.messageET.getText().toString().trim(), sessionManager.getUserDetail().getId());
                                    binding.messageET.setText("");

                                    //Toast.makeText(MessageActivity.this, "" + vImageUrl, Toast.LENGTH_SHORT).show();
//                                    setButtonChecked();
                                    //  System.out.println("url..." + vImageUrl);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            sessionManager.dialogProgress.dismiss();
                            //  Toast.makeText(SignUpActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot
                                    .getTotalByteCount());
                            //   progressDialog.setMessage("Uploaded " + (int) progress + "%");
                        }
                    });
        } else
            Toast.makeText(this, getString(R.string.chooseImage), Toast.LENGTH_SHORT).show();
    }

    private void uploadAttachment(String _nameOfFile, final String _type) {
        try {
            final String finalNameOfFile = _nameOfFile;
            //todo if pdf come
            if (_type.equalsIgnoreCase("pdf")) {
                _nameOfFile = _nameOfFile.replace(".pdf", "");
            }
            //todo if doc come
            if (_nameOfFile.contains(".doc")) {
                _nameOfFile = _nameOfFile.replace(".doc", "");
            }
            if (_nameOfFile.contains(".docx")) {
                _nameOfFile = _nameOfFile.replace(".docx", "");
            }
            //todo if excel come
            if (_nameOfFile.contains(".xls")) {
                _nameOfFile = _nameOfFile.replace(".xls", "");
            }
            if (_nameOfFile.contains(".xlsx")) {
                _nameOfFile = _nameOfFile.replace(".xlsx", "");
            }

            if (fileUri != null) {
                progressDialog = new ProgressDialog(this);
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
//            StorageReference pdf_reference = storageReference.child("tender_pdf/" + UUID.randomUUID().toString());
                //todo we are doing these to make unique file in the db stored
                StorageReference file_reference = storageReference.child("agrostand_" + _type + "/" + _nameOfFile + "!_" + UUID.randomUUID());
                Log.d(LOG_TAG, "uploadAttachment:\t\t" + file_reference.toString());
                file_reference.putFile(fileUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                Task<Uri> uriTask = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                                uriTask.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        vImageUrl = uri.toString();
                                        messageDTO = new MessageDTO();
                                        messageDTO.setMessage("");
                                        messageDTO.setImage(vImageUrl);
                                        messageDTO.setFileType(_type);
                                        messageDTO.setDate(sessionManager.getCurrentDateTime().split("&")[0]);
                                        messageDTO.setTime(sessionManager.getCurrentDateTime().split("&")[1]);
                                        messageDTO.setUser_id(auth.getUid());
                                        messageDTO.setOriginalName(finalNameOfFile);
                                        dbFirebase.child(vUniqueID + "").push().setValue(messageDTO);
                                        dbUserFirebase.child(auth.getUid() + "/message/message").setValue("image");
                                        dbUserFirebase.child(auth.getUid() + "/message/timestamp").setValue(System.currentTimeMillis());
                                        dbUserFirebase.child(FcmMainActivity.fcmUserDTO.getUser_id() + "/message/message").setValue("image");
                                        dbUserFirebase.child(FcmMainActivity.fcmUserDTO.getUser_id() + "/message/timestamp").setValue(System.currentTimeMillis());
//                                    sendNotification(FcmMainActivity.userDTO.getDevice_token(), sessionManager.getUserDetail().getUsername() + " send an image", "image", sessionManager.getUserDetail().getUser_id());
                                        binding.messageET.setText("");

                                    }
                                });

                                progressDialog.dismiss();

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                                progressDialog.dismiss();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                progressDialog.setMessage((int) progress + "% Uploading...");
                            }
                        });


            } else {
                vibrate(200);
                if (_type.equalsIgnoreCase("pdf"))
                    customToast(getString(R.string.choosePdf));
                if (_type.equalsIgnoreCase("doc"))
                    customToast(getString(R.string.chooseDoc));
                if (_type.equalsIgnoreCase("xls"))
                    customToast(getString(R.string.chooseExcel));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "uploadAttachment: " + e);
        }


    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        finish();
        exitActivityAnimation();
    }
}

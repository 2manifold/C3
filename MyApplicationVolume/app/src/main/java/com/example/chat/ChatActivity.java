package com.example.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication__volume.Myapplication;
import com.example.myapplication__volume.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import cn.carbs.android.library.MDDialog;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;


/**
 * the main activity for chat MESSAGE & VIDEO
 */

public class ChatActivity extends AppCompatActivity {

    private RtmClientListener mClientListener;

    private RtmClient mRtmClient;
    private ChatManager mChatManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_main);

        mChatManager = Myapplication.the().getChatManager();
        mRtmClient = mChatManager.getRtmClient();

//        mClientListener = new MyRtmClientListener();
//        mChatManager.registerListener(mClientListener);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        Toolbar toolbar = findViewById(R.id.toolbar_chat);

//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

        ImageView add = toolbar.findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFriends();
            }
        });
    }


    /**
     * add friends
     */
    public void addFriends(){
        MDDialog mdDialog = new MDDialog.Builder(this)
                .setContentView(R.layout.peer_chat)
                .setNegativeButton("Cancel", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                })
                .setPositiveButton(R.string.btn_chat, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                })
                .setPositiveButtonMultiListener(new MDDialog.OnMultiClickListener() {
                    @Override
                    public void onClick(View clickedView, View contentView) {
                        Log.d("PeerToPeer", "Start To Chat");
                        EditText targetEdit = (EditText)contentView.findViewById(R.id.target_name_edit);
                        String mTargetName = targetEdit.getText().toString();
                        if (mTargetName.equals("")) {
                            Toast_in_Thread(getString(R.string.account_empty));
                        } else if (mTargetName.length() >= MessageUtil.MAX_INPUT_NAME_LENGTH) {
                            Toast_in_Thread(getString(R.string.account_too_long));
                        } else if (mTargetName.startsWith(" ")) {
                            Toast_in_Thread(getString(R.string.account_starts_with_space));
                        } else if (mTargetName.equals("null")) {
                            Toast_in_Thread(getString(R.string.account_literal_null));
                        } else if (mTargetName.equals(mChatManager.getUsername())) {
                            Toast_in_Thread(getString(R.string.account_cannot_be_yourself));
                        } else {
                            String result = mChatManager.addFriends(mTargetName);
                            if (result.equals("true")){
                                Toast_in_Thread("Add Friends " + mTargetName + "Successfully !");
                            }else {
                                Toast_in_Thread(result);
                            }
                        }
                    }
                })
                .setNegativeButtonMultiListener(new MDDialog.OnMultiClickListener() {
                    @Override
                    public void onClick(View clickedView, View contentView) {

                    }
                })
                .setTitle(R.string.title_add_friends)
                .create();

        mdDialog.show();
    }


    public void Toast_in_Thread(String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ChatActivity.this, message,Toast.LENGTH_SHORT).show();
            }
        });
    }


}
package com.nikolaychernov.vkmessages;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.perm.kate.api.Api;
import com.perm.kate.api.User;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class UserInfoDownloader<Token,Token1,Token2> extends HandlerThread {

	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;
	Map<UserInfo, Long> requestMap =
            Collections.synchronizedMap(new HashMap<UserInfo, Long>());

    Handler mHandler;
    Api api;
    Account account=DialogListFragment.account;
    
    private class UserInfo {
    	ImageView imageView;
    	TextView userView;
    	TextView onlineView;
    	
    	public UserInfo(){
    		imageView = null;
    		userView = null;
    		onlineView = null;
    	}
    }

    public UserInfoDownloader() {
        super(TAG);
    }
    
    Handler mResponseHandler;
    Listener<Token,Token1,Token2> mListener;

    public interface Listener<Token,Token1,Token2> {
        void onThumbnailDownloaded(Token token, Token1 token1,Token2 token2, Bitmap userPic, String username, String online);
    }

    public void setListener(Listener<Token,Token1,Token2> listener) {
        mListener = listener;
    }

    public UserInfoDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    public void queueUserInfo(Token token,Token1 token1,Token2 token2, long uid) {
        Log.i(TAG, "Got an URL: " + uid);
        UserInfo userInfo = new UserInfo();
        userInfo.imageView=(ImageView) token;
        userInfo.userView=(TextView) token1;
        userInfo.onlineView=(TextView) token2;
        requestMap.put(userInfo, uid);

        mHandler
            .obtainMessage(MESSAGE_DOWNLOAD, userInfo)
            .sendToTarget();
    }
    
    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
    	
    	if(account.access_token!=null)
            api=new Api(account.access_token, Constants.API_ID);
    	mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    UserInfo userInfo = (UserInfo)msg.obj;
                    Log.i(TAG, "Got a request for url: " + requestMap.get(userInfo));
                    handleRequest(userInfo);
                }
            }
        };
    }
    
    public void clearQueue() {
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }
    
    private void handleRequest(final UserInfo userInfo) {
        try {
            final Long uid = requestMap.get(userInfo);
            if (uid == null)
                return;
            
            ArrayList<Long> userids = new ArrayList<Long>();
        	ArrayList<User> users = new ArrayList<User>();
        	userids.add(uid);
        	//Log.i(TAG, "userids' size: " + userids.size());
        	Collection<Long> uids = new ArrayList<Long>(userids);
        	//Log.i(TAG, "uids' size: " + uids.size());
        	Collection<String> collection = null;
			try {
				users = api.getProfiles(uids,collection,"online,photo_50,photo_100,online_mobile",null,null,null);
			} catch (Exception e) {
                e.printStackTrace();
            }
        	User user = users.get(0);
        	URL newurl = new URL(user.photo_medium_rec);
        	//try {
        	final Bitmap bitmap = BitmapFactory.decodeStream(newurl.openConnection() .getInputStream());
        	Log.i(TAG, "Bitmap created");
        	//} catch (IOException e) {
        	//	Log.e(TAG, "Error downloading image", e);
        	//}
        	final String username = user.first_name + " " + user.last_name;
        	String onlineTemp = new String();
        	if (user.online) {
            	if (user.online_mobile)
            		onlineTemp = "mobile";
            	else 
            		onlineTemp = "online";
            }else 
            	onlineTemp = "";
        	final String online = onlineTemp;
        	
        	mResponseHandler.post(new Runnable() {
                public void run() {
                    if (requestMap.get(userInfo) != uid)
                        return;

                    requestMap.remove(userInfo);
                    Token token = (Token) userInfo.imageView;
                    Token1 token1 = (Token1) userInfo.userView;
                    Token2 token2 = (Token2) userInfo.onlineView;
                    mListener.onThumbnailDownloaded(token, token1, token2, bitmap, username, online);
                }
            });
        	
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);    
        }
    }
                
}

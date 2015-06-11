package com.nikolaychernov.vkmessages;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.nikolaychernov.vkmessages.dummy.DummyContent;
import com.perm.kate.api.Api;
import com.perm.kate.api.Message;
import com.perm.kate.api.User;

/**
 * A fragment representing a single Dialog detail screen. This fragment is
 * either contained in a {@link DialogListActivity} in two-pane mode (on
 * tablets) or a {@link DialogDetailActivity} on handsets.
 */
public class DialogDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";
	private static final String TAG = "Test";

	public static Account account=new Account(); // was not static
	private Api api;
	protected ArrayList<Message> messages;
	long offset = 0;
	long partnerUid = 1;
	String message;
	
	private DummyContent.DummyItem mItem;

	private DateFormat dateFormat;

	private MessageAdapter adapter;

	private ListView listView;
	public ArrayList<User> users;
	public Bitmap userPic;
	public Bitmap partnerPic;
	User partner;
	User user;
	

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public DialogDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM_ID)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
			
			account.restore(getActivity());
			if(account.access_token!=null)
	            api=new Api(account.access_token, Constants.API_ID);
			partnerUid = getArguments().getLong(ARG_ITEM_ID);
			
		}
	}
	
	private class GetMessages extends AsyncTask<Void,Void,ArrayList<Message>> {
		
		private ArrayList<User> users1;
		private Bitmap mIcon_val;

		@Override
		protected ArrayList<Message> doInBackground(Void... params) {
			try {
				//if (api!=null)
				messages  = api.getMessagesHistory(getArguments().getLong(ARG_ITEM_ID), 0, account.user_id, offset, 200);
				//messages = api.getMessagesDialogs(0, 200,"","");
			} catch (Exception e) {
                e.printStackTrace();
            }
			ArrayList<Message> tempElements = new ArrayList<Message>(messages);
			Collections.reverse(tempElements);
			messages = tempElements;
			
			ArrayList<Long> userids = new ArrayList<Long>();
			userids.add(account.user_id);
			userids.add(partnerUid);
			Collection<Long> uids = new ArrayList<Long>(userids);
			
			Collection<String> collection = null;
			try {
				users = api.getProfiles(uids,collection,"online,photo_50,photo_100,online_mobile,last_seen",null,null,null);
			} catch (Exception e) {
                e.printStackTrace();
            }
			
			user = users.get(0);
			partner = users.get(1);
			
			URL newurl=null;
			URL newurl1=null;
			try {
				newurl = new URL(user.photo_medium_rec);
				newurl1 = new URL(partner.photo_medium_rec);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				userPic = BitmapFactory.decodeStream(newurl.openConnection() .getInputStream());
				partnerPic = BitmapFactory.decodeStream(newurl1.openConnection() .getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			
			return messages;
			
		}
		
		@Override
		protected void onPostExecute(ArrayList<Message> mssgs) {
			//messages = mssgs;
			//pullToRefreshView.onRefreshComplete();
			adapter = new MessageAdapter(messages);
			listView.setAdapter(adapter);
			listView.setDividerHeight(0);
			listView.post(new Runnable(){
				  public void run() {
				    listView.setSelection(listView.getCount() - 1);
				  }});
			SimpleDateFormat df = new SimpleDateFormat("HH:mm, dd MMM yyyy");
            
            java.util.Date time=new java.util.Date((partner.last_seen*1000));
            
            String last_seen = getResources().getString(R.string.last_seen);
            	//dateTV.setText(/*mHour + ":" + minutes  + " " + */df1.format(time));
			ActionBar ab = getActivity().getActionBar();
			ab.setTitle(partner.first_name + " " + partner.last_name);
			if (partner.online) {
				ab.setSubtitle(R.string.online);
			} else
				ab.setSubtitle(last_seen + " "+ df.format(time));
			//messages = mssgs;
			//Toast.makeText(getActivity(), users.size()+"", 0).show();
		}
		
	}
	
	private class SendMessage extends AsyncTask<Void,Void,ArrayList<Message>> {
		
		private ArrayList<User> users1;
		private Bitmap mIcon_val;

		@Override
		protected ArrayList<Message> doInBackground(Void... params) {
			try {
				api.sendMessage(partnerUid, 0, message, null, null, null, null, null, null, null, null);
				//messages  = api.getMessagesHistory(getArguments().getLong(ARG_ITEM_ID), 0, account.user_id, offset, 200);
				//messages = api.getMessagesDialogs(0, 200,"","");
			} catch (Exception e) {
                e.printStackTrace();
            }
			ArrayList<Message> tempElements = new ArrayList<Message>(messages);
			Collections.reverse(tempElements);
			messages = tempElements;
			
			
			
			return null;
			
			
		}
		
		@Override
		protected void onPostExecute(ArrayList<Message> msgs) {
			//messages = mssgs;
			//pullToRefreshView.onRefreshComplete();
			adapter.notifyDataSetChanged();// = new MessageAdapter(messages);
			listView.setAdapter(adapter);
			listView.setDividerHeight(0);
			listView.post(new Runnable(){
				  public void run() {
				    listView.setSelection(listView.getCount() - 1);
				  }});
			new GetMessages().execute();
			//messages = mssgs;
			//Toast.makeText(getActivity(), users.size()+"", 0).show();
		}
		
	}
	
	private class MessageAdapter extends ArrayAdapter<Message> {

		private static final int TYPE_IN = 0;
        private static final int TYPE_OUT = 1;
        private static final int TYPE_MAX_COUNT = TYPE_OUT + 1;
		
		
        @Override
        public int getItemViewType(int position) {
        	Message m = getItem(position);
        	return m.is_out ? TYPE_OUT : TYPE_IN;
        }
 
        @Override
        public int getViewTypeCount() {
            return TYPE_MAX_COUNT;
        }
        
        public MessageAdapter(ArrayList<Message> messages) {
            super(getActivity(), 0, messages);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // If we weren't given a view, inflate one
        	Message m = getItem(position);
        	
        	int type = getItemViewType(position);
            System.out.println("getView " + position + " " + convertView + " type = " + type);
            if (convertView == null) {
                
                switch (type) {
                    case TYPE_IN:
                        convertView = getActivity().getLayoutInflater().inflate(R.layout.message_list_item, null);
                        break;
                    case TYPE_OUT:
                    	convertView = getActivity().getLayoutInflater().inflate(R.layout.message_list_item_out, null);                        
                        break;
                }
                
            } 
   
            
            LinearLayout messageLL = (LinearLayout) convertView.findViewById(R.id.message_linear_layout);
            TextView messageTV = (TextView) convertView.findViewById(R.id.message_body_textView);
            TextView dateTV = (TextView) convertView.findViewById(R.id.date_textView);
            //ImageView picView = (ImageView) convertView.findViewById(R.id.user_picture_imageView);
            ImageView userView = (ImageView) convertView.findViewById(R.id.sender_imageView);
            
            
            messageTV.setText(m.body);
            if (!m.read_state)
            	if (m.is_out) 
            		messageLL.setBackgroundColor(Color.parseColor("#D4E6EE"));
            	else{
            		messageLL.setBackgroundColor(Color.parseColor("#D4E6EE"));
            	}
            else {
            	messageLL.setBackgroundColor(Color.parseColor("#e5e5e5"));
            }
            SimpleDateFormat df = new SimpleDateFormat("HH:mm, dd MMM yyyy");
            SimpleDateFormat df1 = new SimpleDateFormat("HH:mm");
            java.util.Date time=new java.util.Date((m.date*1000));
            if (DateUtils.isToday(m.date*1000)) {
            	dateTV.setText(/*mHour + ":" + minutes  + " " + */df1.format(time));
            } else
            	dateTV.setText(/*mHour + ":" + minutes  + " " + */df.format(time));
            
            //dateTV.setText(dateFormat.format(time));
            
            
            if (m.is_out){
            	userView.setImageBitmap(userPic);
            } else {
            	userView.setImageBitmap(partnerPic);
            }
            /*picView.setBackgroundResource(R.drawable.pic_placeholder);
            
            //mUserInfoThread.queueUserInfo(picView, userTV, onlineTV, m.uid);
            userTV.setText(user.first_name + " " + user/.last_name);
            
            if (user.online) {
            	if (user.online_mobile)
            		onlineTV.setText("mobile");
            	else 
            		onlineTV.setText("online");
            }else 
            	onlineTV.setText("");*/
            
            
            //UrlImageViewHelper.setUrlDrawable(picView, user/*s.get(position)*/.photo_medium_rec,R.drawable.pic_placeholder);
            
            
            return convertView;
        }

 }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_dialog_detail,
				container, false);
		
		final EditText messageET = (EditText) rootView.findViewById(R.id.message_editText);
		ImageButton sendButton = (ImageButton) rootView.findViewById(R.id.send_button);
		sendButton.setOnClickListener(new OnClickListener(){
	        @Override
	        public void onClick(View v) {
	            message = messageET.getText().toString();
	        	new SendMessage().execute();
	        	messageET.setText("");
	        }
	    });
		listView = (ListView) rootView.findViewById(R.id.messages_listView);
		new GetMessages().execute();
		adapter = new MessageAdapter(messages);
		//if(listView!=null)
		//listView.setAdapter(adapter);
		//Log.i(TAG, messages.size()+"");	
		
		

		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    listView = (ListView) getView().findViewById(R.id.messages_listView);
	    //new GetMessages().execute();
	    //listView.setAdapter(new MessageAdapter(messages));
	    
	}
	
	public void onResume(){
		super.onResume();
	
		
	}
	
}

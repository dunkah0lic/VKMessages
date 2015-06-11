package com.nikolaychernov.vkmessages;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.extras.listfragment.PullToRefreshListFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.perm.kate.api.Api;
import com.perm.kate.api.Message;
import com.perm.kate.api.User;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A list fragment representing a list of Dialogs. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link DialogDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class DialogListFragment extends PullToRefreshListFragment {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";
	private static final String TAG = "PhotoGalleryFragment";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	
	public static Account account=new Account(); // was not static
	private Api api;
	
	private Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	protected ArrayList<Message> messages;
	
	ArrayList<User> users;
	
	Bitmap userPicture;

	private DateFormat dateFormat;

	public int size;

	public PullToRefreshListView pullToRefreshView;

	//public DialogAdapter adapter;
	public DialogItemAdapter adapter;

	public long startPosition=0;

	ArrayList<DialogItem> dialogItems = new ArrayList<DialogItem>();
	public ArrayList<Message> messagesTemp;
	ArrayList<ArrayList<User>> usersFinal = new ArrayList<ArrayList<User>>() ;

	
	private int j;
	
	UserInfoDownloader<ImageView,TextView,TextView> mUserInfoThread;
	

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(String id);

		public void onDialogSelected(long uid);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		
		public void onDialogSelected(long id) {
		}

		@Override
		public void onItemSelected(String id) {
			// TODO Auto-generated method stub
			
		}
	};
	
	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		mCallbacks.onDialogSelected(messages.get(position-1).uid);
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public DialogListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		account.restore(getActivity());
        

        if(account.access_token!=null)
            api=new Api(account.access_token, Constants.API_ID);
		
        dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
		
        //if (api!=null)
        	new GetDialogs().execute();
        setRetainInstance(true);
	}
	
	@Override
    public void onDestroy() {
        super.onDestroy();
        //mUserInfoThread.quit();
        //Log.i(TAG, "Background thread destroyed");
    }
	
	@Override
	public void onDestroyView() {
	    super.onDestroyView();
	    //mUserInfoThread.clearQueue();
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		pullToRefreshView = (PullToRefreshListView) getPullToRefreshListView();
		pullToRefreshView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
    	    @Override
    	    public void onRefresh(PullToRefreshBase<ListView> refreshView) {
    	        // Do work to refresh the list here.
    	    	startPosition = 0;
    	    	new RefreshDialogs().execute();
    	    }
    	});
		pullToRefreshView.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener(){

			@Override
			public void onLastItemVisible() {
				// TODO Auto-generated method stub
				startPosition = messages.size();
				new GetMoreDialogs().execute();
			}
			
		});
		//getListView().setDividerHeight(0);
	}
	
	private class DialogItem {
		Message message;
		ArrayList<User> users;
		
		public DialogItem(Message msg, ArrayList<User> usrs){
			message = msg;
			users = usrs;
		}
		
		public ArrayList<User> getUsers() {
			return users;
		}
		public void setUsers(ArrayList<User> users) {
			this.users = users;
		}
		public Message getMessage() {
			return message;
		}
		public void setMessage(Message message) {
			this.message = message;
		}
		
	}
	
	private class GetDialogs extends AsyncTask<Void,Void,ArrayList<Message>> {
		
		private ArrayList<User> users1;
		private Bitmap mIcon_val;

		@Override
		protected ArrayList<Message> doInBackground(Void... params) {
			try {
				messages = api.getMessagesDialogs(0, 200,"","");
			} catch (Exception e) {
                e.printStackTrace();
            }
			ArrayList<Long> userids = new ArrayList<Long>();
			for (int i=0;i<messages.size();i++) {
				userids.add(messages.get(i).uid);
				if (messages.get(i).chat_id!=null)
					userids.addAll(messages.get(i).chat_members);
			}
			Collection<Long> uids = new ArrayList<Long>(userids);
			
			Collection<String> collection = null;
			try {
				users = api.getProfiles(uids,collection,"online,photo_50,photo_100,online_mobile",null,null,null);
			} catch (Exception e) {
                e.printStackTrace();
            }
			
			for (int i=0;i<messages.size();i++) {
				ArrayList<User> usersTemp = new ArrayList<User>();
				Long uid = messages.get(i).uid;
				for (int j=0;j<users.size();j++) {
					if (users.get(j).uid==uid) {
						usersTemp.add(users.get(j));
					}
				}
				if (messages.get(i).chat_id!=null) {
					ArrayList<Long> temp = messages.get(i).chat_members;
					for (int k=0;k<temp.size();k++){
						for (int j=0;j<users.size();j++) {
							if (users.get(j).uid==temp.get(k)) {
								usersTemp.add(users.get(j));
							}
						}
					}
				}
				usersFinal.add(usersTemp);
			}
			
			userids.clear();
			userids.add(account.user_id);
			uids = new ArrayList<Long>(userids);
			
			try {
				users1 = api.getProfiles(uids,collection,"online,photo_50,photo_100,online_mobile",null,null,null);
			} catch (Exception e) {
                e.printStackTrace();
            }
			User user = users1.get(0);
			URL newurl=null;
			try {
				newurl = new URL(user.photo_medium_rec);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				mIcon_val = BitmapFactory.decodeStream(newurl.openConnection() .getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			userPicture = mIcon_val;
			
			for (int i=0;i<messages.size();i++) {
				DialogItem tempDialogItem = new DialogItem(messages.get(i),usersFinal.get(i));
				dialogItems.add(i,tempDialogItem);
				//dialogItems.get(i).setMessage(messages.get(i));
				//dialogItems.get(i).setUsers(usersFinal.get(i));
			}
			
			return messages;
			
		}
		
		@Override
		protected void onPostExecute(ArrayList<Message> mssgs) {
			//messages = mssgs;
			
			pullToRefreshView.onRefreshComplete();
			//adapter = new DialogAdapter(messages);
			adapter = new DialogItemAdapter(dialogItems);
			setListAdapter(adapter);
			Toast.makeText(getActivity(), usersFinal.size()+"", Toast.LENGTH_LONG).show();
		}
		
	}
	

	
private class GetMoreDialogs extends AsyncTask<Void,Void,ArrayList<Message>> {
		
		@Override
		protected ArrayList<Message> doInBackground(Void... params) {
			try {
                messagesTemp = api.getMessagesDialogs(startPosition, 5,"","");
                messages.addAll(messagesTemp);
                
			} catch (Exception e) {
                e.printStackTrace();
            }
			
			for (int i = (int) startPosition;i<messages.size();i++) {
				ArrayList<User> user = new ArrayList<User>();
				ArrayList<Long> userids = new ArrayList<Long>();
				userids.add(messages.get(i).uid);
				Collection<Long> uids = new ArrayList<Long>(userids);
				
				Collection<String> collection = null;
				try {
					user = api.getProfiles(uids,collection,"online,photo_50, photo_100, online_mobile",null,null,null);
				} catch (Exception e) {
	                e.printStackTrace();
	            }
				users.add(user.get(0));
			}
			/*ArrayList<Long> userids = new ArrayList<Long>();
			for (int i=0;i<messages.size();i++) {
				userids.add(messages.get(i).uid);
			}
			Collection<Long> uids = new ArrayList<Long>(userids);
			
			Collection<String> collection = null;
			try {
				users = api.getProfiles(uids,collection,"online,photo_50, photo_100, online_mobile",null,null,null);
			} catch (Exception e) {
                e.printStackTrace();
            }*/
			for (int i=0;i<messages.size();i++) {
				DialogItem tempDialogItem = new DialogItem(messages.get(i),usersFinal.get(i));
				dialogItems.add(i,tempDialogItem);
				//dialogItems.get(i).setMessage(messages.get(i));
				//dialogItems.get(i).setUsers(usersFinal.get(i));
			}
			
			return messages;
			
		}
		
		@Override
		protected void onPostExecute(ArrayList<Message> mssgs) {
			//messages = mssgs;
			//pullToRefreshView.onRefreshComplete();
			//((BaseAdapter) ((PullToRefreshListView) pullToRefreshView).getAdapter()).notifyDataSetChanged(); 
			adapter.notifyDataSetChanged();
			Toast.makeText(getActivity(), users.size()+"", Toast.LENGTH_SHORT).show();
		}
		
	}	

private Bitmap getUserPic(long id) throws MalformedURLException {
	
	ArrayList<Long> userids = new ArrayList<Long>();
	ArrayList<User> users1 = new ArrayList<User>();
	userids.add(id);
	Bitmap mIcon_val = null;
	Collection<Long> uids = new ArrayList<Long>(userids);
	Collection<String> collection = null;
	try {
		users1 = api.getProfiles(uids,collection,"online,photo_50, photo_100, online_mobile",null,null,null);
	} catch (Exception e) {
        e.printStackTrace();
    }
	User user = users.get(0);
	URL newurl = new URL(user.photo_medium_rec);
	try {
		mIcon_val = BitmapFactory.decodeStream(newurl.openConnection() .getInputStream());
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
	return mIcon_val;
	
}
	
private class RefreshDialogs extends AsyncTask<Void,Void,ArrayList<Message>> {
		
		@Override
		protected ArrayList<Message> doInBackground(Void... params) {
			/*try {
				messages = api.getMessagesDialogs(startPosition, 200,"","");
			} catch (Exception e) {
                e.printStackTrace();
            }
			ArrayList<Long> userids = new ArrayList<Long>();
			for (int i=0;i<messages.size();i++) {
				userids.add(messages.get(i).uid);
			}
			Collection<Long> uids = new ArrayList<Long>(userids);
			
			Collection<String> collection = null;
			try {
				users = api.getProfiles(uids,collection,"online,photo_50,photo_100, online_mobile",null,null,null);
			} catch (Exception e) {
                e.printStackTrace();
            }*/
			usersFinal.clear();
			try {
				messages = api.getMessagesDialogs(0, 200,"","");
			} catch (Exception e) {
                e.printStackTrace();
            }
			ArrayList<Long> userids = new ArrayList<Long>();
			for (int i=0;i<messages.size();i++) {
				userids.add(messages.get(i).uid);
				if (messages.get(i).chat_id!=null)
					userids.addAll(messages.get(i).chat_members);
			}
			Collection<Long> uids = new ArrayList<Long>(userids);
			
			Collection<String> collection = null;
			try {
				users = api.getProfiles(uids,collection,"online,photo_50,photo_100,online_mobile",null,null,null);
			} catch (Exception e) {
                e.printStackTrace();
            }
			
			for (int i=0;i<messages.size();i++) {
				ArrayList<User> usersTemp = new ArrayList<User>();
				Long uid = messages.get(i).uid;
				for (int j=0;j<users.size();j++) {
					if (users.get(j).uid==uid) {
						usersTemp.add(users.get(j));
					}
				}
				if (messages.get(i).chat_id!=null) {
					ArrayList<Long> temp = messages.get(i).chat_members;
					for (int k=0;k<temp.size();k++){
						for (int j=0;j<users.size();j++) {
							if (users.get(j).uid==temp.get(k)) {
								usersTemp.add(users.get(j));
							}
						}
					}
				}
				usersFinal.add(usersTemp);
			}
			
			for (int i=0;i<messages.size();i++) {
				DialogItem tempDialogItem = new DialogItem(messages.get(i),usersFinal.get(i));
				dialogItems.add(i,tempDialogItem);
				//dialogItems.get(i).setMessage(messages.get(i));
				//dialogItems.get(i).setUsers(usersFinal.get(i));
			}
			
			return messages;
			
		}
		
		@Override
		protected void onPostExecute(ArrayList<Message> mssgs) {
			//messages = mssgs;
			pullToRefreshView.onRefreshComplete();
			adapter.notifyDataSetChanged();
			
			//adapter = new DialogAdapter(messages);
			//setListAdapter(adapter);
			
			//Toast.makeText(getActivity(), usersFinal.size()+"", 0).show();
		}
		
	}


	
	 private class DialogAdapter extends ArrayAdapter<Message> {

	        public DialogAdapter(ArrayList<Message> messages) {
	            super(getActivity(), 0, messages);
	            //ArrayList<Long> users = new ArrayList<Long>() ;
	            //for (int i=0; i<messages.size();i++){
	            //	users.add(messages.get(i).uid);
	            //}
	            //Collection<Long> collection = new ArrayList<Long>(users);
	        }
	        
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            // If we weren't given a view, inflate one
	            if (convertView == null) {
	                convertView = getActivity().getLayoutInflater()
	                    .inflate(R.layout.dialog_list_item, null);
	            }
	            
	            Message m = getItem(position);
	            User user = usersFinal.get(position).get(0);
	            
	            TextView userTV = (TextView) convertView.findViewById(R.id.username_textView);
	            TextView messageTV = (TextView) convertView.findViewById(R.id.message_body_textView);
	            TextView dateTV = (TextView) convertView.findViewById(R.id.date_textView);
	            TextView onlineTV = (TextView) convertView.findViewById(R.id.online_textView);
	            ImageView picView = (ImageView) convertView.findViewById(R.id.user_picture_imageView);
	            ImageView userView = (ImageView) convertView.findViewById(R.id.user_imageView);
	            LinearLayout dialogLayout = (LinearLayout) convertView.findViewById(R.id.dialog_linearLayout);
	            
	            messageTV.setText(m.body);
	            if (!m.read_state)
	            	if (m.is_out) 
	            		messageTV.setBackgroundColor(Color.parseColor("#D4E6EE"));
	            	else{
	            		dialogLayout.setBackgroundColor(Color.parseColor("#D4E6EE"));
	            		messageTV.setBackgroundColor(Color.parseColor("#D4E6EE"));
	            	}
	            else {
	            	messageTV.setBackgroundColor(0xffffffff);
	            	dialogLayout.setBackgroundColor(0xffffffff);
	            }
	            
	            SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy");
	            SimpleDateFormat df1 = new SimpleDateFormat("HH:mm");
	            java.util.Date time=new java.util.Date((m.date*1000));
	            if (DateUtils.isToday(m.date*1000)) {
	            	dateTV.setText(/*mHour + ":" + minutes  + " " + */df1.format(time));
	            } else
	            	dateTV.setText(/*mHour + ":" + minutes  + " " + */df.format(time));
	            
	            //if (m.chat_id!=null)
	            	//Toast.makeText(getActivity(), m.chat_members.size()+"", 0).show();
	            
	            if (m.is_out){
	            	userView.setImageBitmap(userPicture);
	            	userView.setVisibility(View.VISIBLE);
	            } else {
	            	userView.setVisibility(View.GONE);
	            }
	            picView.setBackgroundResource(R.drawable.ic_launcher);
	            
	            //mUserInfoThread.queueUserInfo(picView, userTV, onlineTV, m.uid);
	            userTV.setText(user/*s.get(position)*/.first_name + " " + user/*s.get(position)*/.last_name);
	            
	            if (user/*s.get(position)*/.online) {
	            	if (user/*s.get(position)*/.online_mobile)
	            		onlineTV.setText("mobile");
	            	else 
	            		onlineTV.setText("online");
	            }else 
	            	onlineTV.setText("");
	            
	            
	            UrlImageViewHelper.setUrlDrawable(picView, user/*s.get(position)*/.photo_medium_rec,R.drawable.ic_launcher);
	            
	            
	            return convertView;
	        }

	 }
	 
	 private class DialogItemAdapter extends ArrayAdapter<DialogItem> {

	        public DialogItemAdapter(ArrayList<DialogItem> dialogItems) {
	            super(getActivity(), 0, dialogItems);
	            //ArrayList<Long> users = new ArrayList<Long>() ;
	            //for (int i=0; i<messages.size();i++){
	            //	users.add(messages.get(i).uid);
	            //}
	            //Collection<Long> collection = new ArrayList<Long>(users);
	        }
	        
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            // If we weren't given a view, inflate one
	            if (convertView == null) {
	                convertView = getActivity().getLayoutInflater()
	                    .inflate(R.layout.dialog_list_item, null);
	            }
	            
	            DialogItem m = getItem(position);
	            User user = m.getUsers().get(0);
	            
	            TextView userTV = (TextView) convertView.findViewById(R.id.username_textView);
	            TextView messageTV = (TextView) convertView.findViewById(R.id.message_body_textView);
	            TextView dateTV = (TextView) convertView.findViewById(R.id.date_textView);
	            TextView onlineTV = (TextView) convertView.findViewById(R.id.online_textView);
	            ImageView picView = (ImageView) convertView.findViewById(R.id.user_picture_imageView);
	            ImageView userView = (ImageView) convertView.findViewById(R.id.user_imageView);
	            LinearLayout dialogLayout = (LinearLayout) convertView.findViewById(R.id.dialog_linearLayout);
	            
	            messageTV.setText(m.getMessage().body);
	            if (!m.getMessage().read_state)
	            	if (m.getMessage().is_out) 
	            		messageTV.setBackgroundColor(Color.parseColor("#D4E6EE"));
	            	else{
	            		dialogLayout.setBackgroundColor(Color.parseColor("#D4E6EE"));
	            		messageTV.setBackgroundColor(Color.parseColor("#D4E6EE"));
	            	}
	            else {
	            	messageTV.setBackgroundColor(0xffffffff);
	            	dialogLayout.setBackgroundColor(0xffffffff);
	            }
	            
	            SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy");
	            SimpleDateFormat df1 = new SimpleDateFormat("HH:mm");
	            java.util.Date time=new java.util.Date((m.getMessage().date*1000));
	            if (DateUtils.isToday(m.getMessage().date*1000)) {
	            	dateTV.setText(/*mHour + ":" + minutes  + " " + */df1.format(time));
	            } else
	            	dateTV.setText(/*mHour + ":" + minutes  + " " + */df.format(time));
	            
	            //if (m.chat_id!=null)
	            	//Toast.makeText(getActivity(), m.chat_members.size()+"", 0).show();
	            
	            if (m.getMessage().is_out){
	            	userView.setImageBitmap(userPicture);
	            	userView.setVisibility(View.VISIBLE);
	            } else {
	            	userView.setVisibility(View.GONE);
	            }
	            picView.setBackgroundResource(R.drawable.ic_launcher);
	            
	            
	            userTV.setText(user/*s.get(position)*/.first_name + " " + user/*s.get(position)*/.last_name);
	            
	            if (user/*s.get(position)*/.online) {
	            	if (user/*s.get(position)*/.online_mobile)
	            		onlineTV.setText("mobile");
	            	else 
	            		onlineTV.setText("online");
	            }else 
	            	onlineTV.setText("");
	            
	            
	            UrlImageViewHelper.setUrlDrawable(picView, user/*s.get(position)*/.photo_medium_rec,R.drawable.ic_launcher);
	            
	            
	            return convertView;
	        }

	 }
	 


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	
	
	

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}
}

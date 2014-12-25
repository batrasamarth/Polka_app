/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.scrumptious;


import java.io.IOException;
import java.util.HashMap;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.facebook.AppEventsLogger;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

public class MainActivity extends ActionBarActivity {

    private static final String USER_SKIPPED_LOGIN_KEY = "user_skipped_login";
    
    private static final int SPLASH = 0;
    private static final int SELECTION = 1;
    private static final int SETTINGS = 2;
    private static final int FRIENDS = 3;
    private static final int FRAGMENT_COUNT = FRIENDS +1;

    
    ConnectionConfiguration config;
    XMPPConnection connection;
    AccountManager am;
    
    private ActionBar actionBar;
    private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
    private MenuItem settings;
    private boolean isResumed = false;
    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        FragmentManager fm = getSupportFragmentManager();
        SplashFragment splashFragment = (SplashFragment) fm.findFragmentById(R.id.splashFragment);
        fragments[SPLASH] = splashFragment;
        fragments[SELECTION] = fm.findFragmentById(R.id.selectionFragment);
        fragments[SETTINGS] = fm.findFragmentById(R.id.userSettingsFragment);
        fragments[FRIENDS] = fm.findFragmentById(R.id.FriendsFragment);
        
        FragmentTransaction transaction = fm.beginTransaction();
        for(int i = 0; i < fragments.length; i++) {
            transaction.hide(fragments[i]);
        }
        transaction.commit();
        
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
        isResumed = true;

        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        AppEventsLogger.activateApp(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
        isResumed = false;

        // Call the 'deactivateApp' method to log an app event for use in analytics and advertising
        // reporting.  Do so in the onPause methods of the primary Activities that an app may be launched into.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);

        
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        Session session = Session.getActiveSession();

        if (session != null && session.isOpened()) {
        	
        	if(getSupportActionBar()==null||getSupportActionBar().getTabCount()==0){
        		enableActionBar();
        	}
        	getSupportActionBar().show();
            Log.d("XMPP-send","in onResume()");
        	sendMessage();
            // if the session is already open, try to show the selection fragment
            showFragment(SELECTION, false);
        } 
        else {
            // otherwise present the splash screen and ask the user to login, unless the user explicitly skipped.
        	getSupportActionBar().hide();
        	showFragment(SPLASH, false);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // only add the menu when the selection fragment is showing
        if (fragments[SELECTION].isVisible()) {
            if (menu.size() == 0) {
                settings = menu.add(R.string.settings);
            }
            return true;
        } else {
            menu.clear();
            settings = null;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Log.d("YAY!","Settings selected!");
    	Log.d("YAY!",Integer.toString(item.getItemId()));
        
    	if (item.getItemId()==0) {
    		Log.d("YAY!","yahan bhi aaya!");
    		getSupportActionBar().hide();
        	showSettingsFragment();
            return true;
        }
        return false;
    }

    public void showSettingsFragment() {
    	Log.d("YAY!","showSettingsFragment");
        showFragment(SETTINGS, true);
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (isResumed) {
            FragmentManager manager = getSupportFragmentManager();
            int backStackSize = manager.getBackStackEntryCount();
            for (int i = 0; i < backStackSize; i++) {
                manager.popBackStack();
            }
            // check for the OPENED state instead of session.isOpened() since for the
            // OPENED_TOKEN_UPDATED state, the selection fragment should already be showing.
            if (state.equals(SessionState.OPENED)) {
                showFragment(SELECTION, false);
                Context context = getApplicationContext();
                SmackAndroid.init(context);
                connectToServer();
                
            } else if (state.isClosed()) {
                showFragment(SPLASH, false);
            }
        }
    }

    public void showFragment(int fragmentIndex, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        for (int i = 0; i < fragments.length; i++) {
            if (i == fragmentIndex) {
            	Log.d("YAY!","show "+fragments[fragmentIndex]);
                transaction.show(fragments[i]);
            } else {
                transaction.hide(fragments[i]);
            }
        }
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
    
    public void sendMessage(){
    	Thread t  = new Thread(new Runnable(){
    		@Override
    		public void run(){
    			try{
    				if(!(connection.isConnected())){
    					Log.d("XMPP-connect","!connection.isConnected");
    					connection.connect();
    					connection.login("sample", "my_password");
    					
    				}
    				else{
    					Log.d("XMPP-connect","connection.isConnected");
    				}
    				Chat chat = ChatManager.getInstanceFor(connection).createChat("pioneersamarth@gmail.com", new MessageListener() {
					     public void processMessage(Chat chat, Message message) {
					         // Print out any messages we get back to standard out.
					         System.out.println("Received message: " + message);
					     }
					 });
					chat.sendMessage("Kiddan!");
					Log.d("XMPP-message!","Bhej dia!");
    			}
    			catch(Exception e){
    				
    			}
    		}
    	});
    	t.start();
    }
    
    public void connectToServer(){
    	Thread t = new Thread(new Runnable(){
    		@Override
    		public void run(){
    			config = new ConnectionConfiguration("54.69.228.44",5222,
    					"ec2-54-69-228-44.us-west-2.compute.amazonaws.com",
    					ProxyInfo.forDefaultProxy());
                config.setDebuggerEnabled(true);
                config.setSecurityMode(SecurityMode.disabled);
                connection = new XMPPTCPConnection(config);
                System.setProperty("smack.debugEnabled", "true");
                am = AccountManager.getInstance(connection);
                try {
                	
					connection.connect();
					Log.d("XMPP","Connection success!");
					HashMap<String,String> attributes = new HashMap<String,String>();
					attributes.put("username", "sample");
					attributes.put("password", "my_password");
					attributes.put("email", "foo@foo.com");
					attributes.put("name", "Sample Batra");
					am.createAccount("sample", "my_password", attributes);
					
				} catch (SmackException e) {
					Log.d("XMPP","SmackException@onSessionStateChange");
					e.printStackTrace();
				} catch (IOException e) {
					Log.d("XMPP","IOException@onSessionStateChange");
					e.printStackTrace();
				} catch (XMPPException e) {
					Log.d("XMPP","XMPPException@onSessionStateChange");
					e.printStackTrace();
				}
                

    		}
    	});
    	t.start();
    }
    
    public void enableActionBar(){
    	actionBar = getSupportActionBar();
    	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    	ActionBar.Tab tab1 =  actionBar.newTab();
    	tab1.setText(R.string.home_tab);
    	tab1.setTabListener(new MyTabListener<SelectionFragment>(this,R.id.selectionFragment,
    			SelectionFragment.class,fragments) );
    	ActionBar.Tab tab2 = actionBar.newTab();
     	tab2.setText(R.string.activity_tab);
     	tab2.setTabListener(new MyTabListener<FriendsFragment>(this,R.id.FriendsFragment,
     			FriendsFragment.class,fragments));
     	actionBar.addTab(tab1); actionBar.addTab(tab2);
    }
    
    public static class MyTabListener<T extends Fragment> implements android.support.v7.app.ActionBar.TabListener {
	    private Fragment mFragment;
	    private final FragmentActivity mActivity;
	    private final int mId;
	    private final Class<T> mClass;
	    private FragmentTransaction fft;
	    private Fragment mFragments[];
	    FragmentManager fm;

	    /** Constructor used each time a new tab is created.
	      * @param activity  The host Activity, used to instantiate the fragment
	      * @param tag  The identifier tag for the fragment
	      * @param clz  The fragment's Class, used to instantiate the fragment
	      */
	    public MyTabListener(FragmentActivity activity, int id, Class<T> clz, Fragment[] fragArg) {
	        mActivity = activity;
	        mId = id;
	        mClass = clz;
	        mFragments = fragArg;
	        fm  = mActivity.getSupportFragmentManager();
	        mFragment = fm.findFragmentById(mId);
            
	    }

	    /* The following are each of the ActionBar.TabListener callbacks */

	    public void onTabSelected(Tab tab, FragmentTransaction ft) {
	        // Check if the fragment is already initialized
	    	FragmentTransaction transaction = fm.beginTransaction();
	    	for(int i=0; i<mFragments.length; i++){
	    		transaction.hide(mFragments[i]);
	    	}
	        transaction.show(mFragment);
	        transaction.commit();
	    }

	    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	    	FragmentTransaction transaction = fm.beginTransaction();
	    	transaction.hide(mFragment);
	    	transaction.commit();
	    }

	    public void onTabReselected(Tab tab, FragmentTransaction ft) {
	        // User selected the already selected tab. Usually do nothing.
	    }
	}
    
    
}

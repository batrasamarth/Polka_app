package com.facebook.scrumptious;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FriendsFragment extends Fragment{
	 
	 public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        super.onCreateView(inflater, container, savedInstanceState);
		 	View view = inflater.inflate(R.layout.dummy, container, false);
	        return view;
	 }
}

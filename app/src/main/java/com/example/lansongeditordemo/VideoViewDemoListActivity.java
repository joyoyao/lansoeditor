package com.example.lansongeditordemo;


import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;


import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.lansoeditor.demo.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Video;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 *视频和View叠加来实现各种效果的列表.
 *
 */
public class VideoViewDemoListActivity extends Activity implements OnClickListener{

	 private static final String TAG="VideoViewOverlayDemoActivity";
	 private String mVideoPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		 Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
		
		 setContentView(R.layout.vview_demo_list_layout);
		  mVideoPath = getIntent().getStringExtra("videopath");
		  
		 findViewById(R.id. id_vview_demo_commonwidget).setOnClickListener(this);
		 findViewById(R.id.id_vview_demo_viewpage).setOnClickListener(this);
		 findViewById(R.id.id_vview_demo_drawimage).setOnClickListener(this);
		 findViewById(R.id.id_vview_demo_dragimage).setOnClickListener(this);
		 findViewById(R.id.id_vview_demo_image3d).setOnClickListener(this);
		 findViewById(R.id.id_vview_demo_all).setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
    	// TODO Auto-generated method stub
    		switch (v.getId()) {
	    		case R.id.id_vview_demo_commonwidget:
	    			startExecuteDemo(VViewCommonWidgetActivity.class);
					break;
	    		case R.id.id_vview_demo_viewpage:
	    			startExecuteDemo(VViewViewPageDemoActivity.class);
					break;
	    		case R.id.id_vview_demo_drawimage:
	    			startExecuteDemo(VViewDrawImageDemoActivity.class);
					break;
	    		case R.id.id_vview_demo_dragimage:
	    			startExecuteDemo(VViewImageDragDemoActivity.class);
					break;
	    		case R.id.id_vview_demo_image3d:
	    			startExecuteDemo(VViewImage3DDemoActivity.class);
					break;
	    		case R.id.id_vview_demo_all:
	    			startExecuteDemo(VViewAllWidgetDemoActivity.class);
					break;
				default:
					break;
    	}
    }
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	
    }
    private void startExecuteDemo(Class<?> cls)
   	{
    	Intent intent=new Intent(VideoViewDemoListActivity.this,cls);
    	intent.putExtra("videopath", mVideoPath);
    	startActivity(intent);
   	}

}

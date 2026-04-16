package com.abdulsalam.motobike;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.MemoryManager;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.secrethq.ads.PTStartAppBridge;
import com.secrethq.ads.PTAdAppLovinBridge;
import com.secrethq.ads.PTAdChartboostBridge;
import com.secrethq.ads.PTAdFacebookBridge;
import com.secrethq.ads.PTAdHeyzapBridge;
import com.secrethq.ads.PTAdInMobiBridge;
import com.secrethq.ads.PTAdLeadBoltBridge;
import com.secrethq.ads.PTAdRevMobBridge;
import com.secrethq.store.PTStoreBridge;
import com.secrethq.utils.PTJniHelper;
import com.secrethq.utils.PTServicesBridge;

import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxGLSurfaceView;

import com.abdulsalam.R;

public class PTPlayer extends Cocos2dxActivity {
    private FirebaseAnalytics firebaseAnalytics;

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try {
			Log.v("----------","onActivityResult: request: " + requestCode + " result: "+ resultCode);
			if(PTStoreBridge.iabHelper().handleActivityResult(requestCode, resultCode, data)){
				Log.v("-----------", "handled by IABHelper");
			}
			else if(requestCode == PTServicesBridge.RC_SIGN_IN){
				if(resultCode == RESULT_OK){
					PTServicesBridge.instance().onActivityResult(requestCode, resultCode, data);
				}
				else if(resultCode == GamesActivityResultCodes.RESULT_SIGN_IN_FAILED){
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(this, "Google Play Services: Sign in error", duration);
					toast.show();
				}
				else if(resultCode == GamesActivityResultCodes.RESULT_APP_MISCONFIGURED){
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(this, "Google Play Services: App misconfigured", duration);
					toast.show();
				}
			}
		} catch (Exception e) {
			Log.v("-----------", "onActivityResult FAIL on iabHelper : " + e.toString());
		}
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PTServicesBridge.initBridge(this, getString( R.string.app_id ));
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// Firebase Analytics init
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // ONLY TWO EVENTS (Total User + Active User)
        trackUsers();
        UpdateManager.check(this);
	}
	
	private void trackUsers() {

        // Total User → জীবনে প্রথমবার open হলেও count হবে
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.APP_OPEN,
            null
        );

        // Active User → প্রতিবার app open হলে count হবে
        firebaseAnalytics.logEvent(
            "active_user",
            null
        );
    }

	@Override
	public void onNativeInit(){
		initBridges();
	}

	private void initBridges(){
		PTStoreBridge.initBridge( this );


		if (PTJniHelper.isAdNetworkActive("kChartboost")) {
			PTAdChartboostBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kRevMob")) {
			PTAdRevMobBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kInMobi")) {
			PTAdInMobiBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kAdMob") || PTJniHelper.isAdNetworkActive("kFacebook")) {
			PTStartAppBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kAppLovin")) {
			PTAdAppLovinBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kLeadBolt")) {
			PTAdLeadBoltBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kFacebook")) {
			PTAdFacebookBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kHeyzap")) {
			PTAdHeyzapBridge.initBridge(this);
		}
	}

	@Override
	public Cocos2dxGLSurfaceView onCreateView() {
		Cocos2dxGLSurfaceView glSurfaceView = new Cocos2dxGLSurfaceView(this);
		glSurfaceView.setEGLConfigChooser(8, 8, 8, 0, 0, 0);

		return glSurfaceView;
	}

	static {
		System.loadLibrary("player");
	}

	@Override
	protected void onResume() {
		super.onResume();
		MemoryManager.onResume();
		if (PTJniHelper.isAdNetworkActive("kChartboost")) {
			PTAdChartboostBridge.onResume( this );
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		MemoryManager.onStart();
		if (PTJniHelper.isAdNetworkActive("kChartboost")) {
			PTAdChartboostBridge.onStart( this );
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		MemoryManager.onStop();
		if (PTJniHelper.isAdNetworkActive("kChartboost")) {
			PTAdChartboostBridge.onStop( this );
		}
	}

	@Override
	protected void onDestroy() {
		MemoryManager.onDestroy();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		MemoryManager.onBackPressed();
		exitConfirmation(this);
	}

	private void exitConfirmation(final Activity activity) {

		AlertDialog.Builder alertDlg = new AlertDialog.Builder(activity);
		alertDlg.setMessage("Do you Want Exit ?");
		alertDlg.setCancelable(false);

		alertDlg.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.finish();
			}
		});

		alertDlg.setNeutralButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		alertDlg.create().show();
	}
}

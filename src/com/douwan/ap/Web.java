package com.douwan.ap;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ResponseServer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class Web extends Activity {

	WebView web = null;
	ProgressDialog dialog = null;
	ProgressBar pb = null;
	Handler handler = null;
	long new_time = 0;
	Intent Data = null;
	String no_redirect = "1";
	int num = 0;
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout);
		
		Data = new Intent();
		
		pb = (ProgressBar)findViewById(R.id.progressbar);
		pb.setMax(100);
		
		web = (WebView)findViewById(R.id.webview);
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setBlockNetworkImage(true); 
		
		Intent it = getIntent();
		Bundle bd = it.getExtras();
		String uri = bd.getString("uri");
		if(web != null)
        {
			web.setWebChromeClient(new WebChromeClient()
			{
				public void onProgressChanged(WebView view, int progress)
				{           
					Web.this.setTitle(view.getUrl());
					Web.this.setProgress(progress * 100);
					pb.setProgress(progress);
					if(progress == 100)
					{
						pb.setProgress(View.GONE-100);
//						Log.i("debug.info","url:"+view.getUrl());
						Web.this.setTitle(view.getUrl());
	                }
	             }
			}); 
			
			web.setWebViewClient(new WebViewClient()
			{
				@Override
				public void onPageFinished(WebView view,String url)
				{
					view.getSettings().setBlockNetworkImage(false);
					new_time = SystemClock.uptimeMillis();
					num = num +1;
					Log.i("debug.info","url:"+view.getUrl());
					Log.i("debug.info","num = "+num);
					if(num == 3)
					{
						long time1 = SystemClock.uptimeMillis();
						while(true)
						{
							long time2 = SystemClock.uptimeMillis();
							if(time2-time1> 5000)
									break;
						}
						Intent it = new Intent("com.douwan.ap.mainActivity");
						it.putExtra("mainActivity","web_back");
						it.putExtra("time",new_time);
						it.putExtra("redirect", no_redirect);
						Log.i("debug.info","new_time = "+new_time);
						Log.i("debug.info","no_redirect = "+no_redirect);
						sendBroadcast(it);
						finish();
					}
					dialog.dismiss();
					
				}
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) 
				{
					HitTestResult hit = view.getHitTestResult();
					int hitType = hit.getType();
					Log.i("debug.info","type:"+hitType);
					if(hitType == 0)
					{
						no_redirect = hitType+"";
					}
					if(url!=null) 
					{				
						view.loadUrl(url);					
					}
			
					return true;
				}
				@Override
				public void onReceivedError(WebView view, int errorCode,
						String description, String failingUrl) {
					// TODO Auto-generated method stub
					if(errorCode == 404)
					{
						Data.putExtra("errorCode", 404);
						Log.i("debug.info","1:"+errorCode);
					}
					Log.i("debug.info",""+description);
					Log.i("debug.info",""+errorCode);
					Log.i("debug.info",""+failingUrl);
					super.onReceivedError(view, errorCode, description, failingUrl);
				}	
				
			});
			loadUrl(uri);
        }
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		
		if (keyCode == KeyEvent.KEYCODE_BACK || web.canGoBack()) 
		{  
			//web.goBack();
			
			Data.putExtra("time",new_time);
			Data.putExtra("redirect", no_redirect);
			setResult(10,Data);
			finish();
            return false;  
        } 
		return super.onKeyDown(keyCode, event);
	}

	public void loadUrl(final String url)
    {	 
		
		if(web != null)
		{ 
			web.loadUrl(url);
//			web.reload();
			dialog = ProgressDialog.show(this,null,"页面加载中，请稍后.."); 
      	
		}
    }
	
}

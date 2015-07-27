package com.douwan.ap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.Timer;
import java.util.TimerTask;

import com.douwan.ap.MainActivity.MsgReceiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class WifiService extends Service {
	
	private final int OPEN_WIFI = 1;
	private final int CLOSE_WIFI = 2;
	private final int UPDATE_LISTVIEW = 3;
	private final int SCAN_WIFI = 4;
	private final int DHCP_FAIL = 5;
	private final int CAN_NETWORK = 6;
	private final int PORTAL_FAIL = 7;
	private final int START_BROW = 8;
	private final int WEB_BACK = 9;
	
	private Handler myHandler = null;
	private MsgReceiver msgReceiver = null;
	public static Wifi wifi = null;
	
	private String tag = null;
	long new_time = 0;
	long old_time = 0;
	String no_redirect = null;
	String string_mac_input = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		Log.i("debug.info", "onBind");
		return new MyBinder();
	}	

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		recBroadcast();
		wifi = new Wifi(WifiService.this);
		
		myHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				Intent it = new Intent("com.douwan.ap.WifiService");
				switch(msg.what)
				{
				case OPEN_WIFI:
					processOpenWifi(it);
					break;
				case CLOSE_WIFI:
					processCloseWifi(it);
					break;
				case UPDATE_LISTVIEW:
					processUpdateListView(it);
					break;
				case SCAN_WIFI:
					processScanWifi(it);
					break;
				case DHCP_FAIL:
					processDhcpFail(it);
					break;
				case CAN_NETWORK:
					processCanNetwork(it);
					break;
				case START_BROW:
					processStartBrow(it,msg.obj.toString());
					break;
				case PORTAL_FAIL:
					processPortFail(it);
					break;
				case WEB_BACK:
					processWebBack(it);
				default:
					break;
				}
				super.handleMessage(msg);
			}	
		};
		Log.i("debug.info", "onCreate");	
	}
	
	public void processWebBack(Intent it)
	{
		it.putExtra("wifiservice","processWebBack");
		it.putExtra("new_time",new_time);
		it.putExtra("old_time", old_time);
		it.putExtra("redirect", no_redirect);
		Log.i("debug.info", "sendBroadcast:web_back");
		sendBroadcast(it);
	}
	
	public void processOpenWifi(Intent it)
	{
		it.putExtra("wifiservice","openwifi");
		it.putExtra("tag", tag);
		Log.i("debug.info", "sendBroadcast:openwifi...");
		sendBroadcast(it);
	}
	
	public void processCloseWifi(Intent it)
	{
		it.putExtra("wifiservice","closewifi");
		it.putExtra("tag", tag);
		Log.i("debug.info", "sendBroadcast:closewifi...");
		sendBroadcast(it);
	}
	
	public void processUpdateListView(Intent it)
	{
		it.putExtra("wifiservice","updatelistview");
		Log.i("debug.info", "sendBroadcast:updatelistview...");
		sendBroadcast(it);
	}
	
	public void processScanWifi(Intent it)
	{
		it.putExtra("wifiservice","scanwifi");
		it.putExtra("tag", tag);
		Log.i("debug.info", "sendBroadcast:scanwifi...");
		sendBroadcast(it);
	}
	
	public void processDhcpFail(Intent it)
	{
		it.putExtra("wifiservice","dhcpfail");
		Log.i("debug.info", "sendBroadcast:dhcpfail...");
		sendBroadcast(it);
	}
	
	public void processCanNetwork(Intent it)
	{
		it.putExtra("wifiservice","cannetwork");
		it.putExtra("tag", tag);
		Log.i("debug.info", "sendBroadcast:cannetwork...");
		sendBroadcast(it);
	}
	
	public void processStartBrow(Intent it,String uri)
	{
		/*it.putExtra("wifiservice","processStartBrow");
		it.putExtra("uri", uri);
		Log.i("debug.info", "sendBroadcast:processStartBrow...");
		sendBroadcast(it);*/
		if(startBrows(Uri.parse(uri)) == true)
		{
			Log.i("debug.info","start brows...");
		}
	}
	
	public void processPortFail(Intent it)
	{
		it.putExtra("wifiservice","processPortFail");
		Log.i("debug.info", "sendBroadcast:processPortFail...");
		sendBroadcast(it);
	}
	
	public void execute()
	{
		executeListView();
	}
	
	public void executeListView()
	{
		Timer timer = new Timer(true);
		TimerTask timerTask = new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Message message = new Message();
				message.what = UPDATE_LISTVIEW;
				myHandler.sendMessage(message);
			}
		};
		timer.schedule(timerTask, 1000, 10000);
	}  
	
	public void recBroadcast()
	{
		msgReceiver = new MsgReceiver();  
        IntentFilter intentFilter = new IntentFilter();  
        intentFilter.addAction("com.douwan.ap.web");
        intentFilter.addAction("com.douwan.ap.mainActivity");
        registerReceiver(msgReceiver, intentFilter);
	}
	public class MsgReceiver extends BroadcastReceiver{  
		  
        @Override  
        public void onReceive(Context context, Intent intent) { 
        	Message message = new Message();
        	receivemainActivityWifi(intent,message);
        }  
          
    }
	
	public void receivemainActivityWifi(Intent intent,Message message)
	{
		switch(intent.getStringExtra("mainActivity"))
    	{
    	case "openwifi":
    		openWifi(message);
    		break;
    	case "closewifi":
    		closeWifi(message);
    		break;
    	case "scanwifi":
    		string_mac_input = intent.getStringExtra("string_mac_input");
    		Log.i("debug.info","mac:"+string_mac_input);
    		scanWifi(message);
    		break;
    	case "web_back":
    		webBack(intent,message);
			break;
    	default :
			break;
    	}
	}
	
	public void openWifi(Message message)
	{
		if(wifi.wifiManager.isWifiEnabled())
		{
			tag = "WIFI已经打开，不要点我！";
			message.what = OPEN_WIFI;
    		myHandler.sendMessage(message);
			return;
		}
		wifi.openWifi();
		while(!wifi.wifiManager.isWifiEnabled());
		tag = "WIFI正在打开...";
		message.what = OPEN_WIFI;
		myHandler.sendMessage(message);
	}
	
	public void closeWifi(Message message)
	{
		if(!wifi.wifiManager.isWifiEnabled())
		{
			tag = "WIFI已经关闭，不要点我！";
			message.what = CLOSE_WIFI;
    		myHandler.sendMessage(message);
			return;
		}
		tag = "WIFI正在关闭...";
		wifi.closeWifi();
		message.what = CLOSE_WIFI;
		myHandler.sendMessage(message);
	}
	
	public void scanWifi(Message message)
	{
		if(!wifi.wifiManager.isWifiEnabled())
		{
			tag = "WIFI已经关闭，请打开！";
			/*wifi.openWifi();
			while(!wifi.wifiManager.isWifiEnabled());*/
			message.what = SCAN_WIFI;
    		myHandler.sendMessage(message);
			return;
		}
		if(canConnectWifi() == false)
		{
			tag = "MAC地址无效";
			message.what = SCAN_WIFI;
    		myHandler.sendMessage(message);
		}
		
	}
	
	public boolean canConnectWifi()
	{
		if(wifi.isMacValid(string_mac_input))
		{
			MyThread mythread = new MyThread();
			Thread t = new Thread(mythread);
			t.start();
		}
		else
		{
			Log.i("debug.info","MAC地址无效");
			return false;
		}
		return true;
	}
	
	public void webBack(Intent intent,Message message)
	{
		Log.i("debug.info", "web_back");
		new_time = intent.getExtras().getLong("time");
		no_redirect = intent.getExtras().getString("redirect");
		message.what = WEB_BACK;
		myHandler.sendMessage(message);
	}
	
	public class MyThread implements Runnable
	{
		@Override
		public void run() {
			wifi.addNetwork(wifi.CreateWifiInfo(string_mac_input,""));
			while(true)
			{
				if(wifi.isWifiConnected(WifiService.this) == 0)
					break;
				else if(wifi.isWifiConnected(WifiService.this) == -1)
				{
					break;
				}
				else if (wifi.isWifiConnected(WifiService.this) == 1)
					continue;
			}
			
			if(wifi.getDhcp())
			{
				old_time = SystemClock.uptimeMillis();	
				sendUrl();
			}
			else
			{
				Message message = new Message();
				message.what = DHCP_FAIL;
				tag = "ip 获取失败";
				myHandler.sendMessage(message);
				Log.i("debug.info","ip 获取失败");
			}
    
		}
		
	}
	
	public void sendUrl()
	{
		String[] res = {
//				"http://www.thinkdifferent.us",
				"http://captive.apple.com"
//				"http://www.baidu.com"
				};
		long old_portal = 0;
		long new_portal = 0;
		String line = null;
		URL url = null;
		Uri uri = null;
		HttpURLConnection connection = null;
		try {
			url = new URL(res[0]);
			uri = Uri.parse(res[0]);
			try {
				connection = (HttpURLConnection) url.openConnection();
				connection.setDoInput(true);
		        connection.setConnectTimeout((int)(Long.parseLong(MainActivity.string_timeout_input)*1000));
		        connection.setReadTimeout((int)(Long.parseLong(MainActivity.string_timeout_input)*1000));
		        connection.setRequestMethod("GET");
		        connection.setUseCaches(false);
		        old_portal = SystemClock.uptimeMillis();
		        connection.connect();
		        if(connection.getResponseCode() != 200)
		        {
		        //	throw new IOException("return 200:false");
		        }
		        new_portal = SystemClock.uptimeMillis();
		        BufferedReader reader = new BufferedReader(new InputStreamReader(  
		                connection.getInputStream(), "utf-8"));
		        Log.i("debug.info","connect time="+(new_portal - old_portal));
		        StringBuffer m = new StringBuffer();
		        while ((line = reader.readLine()) != null) 
		        {
		            m.append(line);
		        }
		        reader.close();
			    Log.i("debug.info",""+m);
		        if(m.toString().contains("<TITLE>Success</TITLE>"))
				{
					Log.i("debug.info","访问IOS网站返回Succeed！");
					tag = "设备可以上网了,请重置!";
					Message message = new Message();
					message.what = CAN_NETWORK;
					myHandler.sendMessage(message);
					/*if(startBrows(uri) == true)
		    		{
		    			Log.i("debug.info","start brows...");
		    		}*/
					
					return;
				}
		        
		       else if(m.toString().contains("<title>404</title>"))
		        {
		        	throw new IOException("return 404");
		        }
		        
		        else
		        {
		        	Message message = new Message();
					message.what = START_BROW;
					message.obj = res[0];
					myHandler.sendMessage(message);
		        }
		        
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.i("debug.info","IOException:"+e.getMessage());
				Log.i("debug.info","connect time="+(new_portal - old_portal));
//				Thread.sleep(Long.parseLong(string_timeout_input)*1000);
				Message message = new Message();
				message.what = PORTAL_FAIL;
				myHandler.sendMessage(message);
				e.printStackTrace();
			  }
			finally{
				  connection.disconnect();
			  }
		}
         catch (MalformedURLException e) {
		// TODO Auto-generated catch block
        	 Log.i("debug.info","MalformedURLException");
        	 Log.i("debug.info","2:"+(new_portal - old_portal));
        	 e.printStackTrace();
		}
		
	}
	
	
	public boolean startBrows(Uri uri)
	{
		boolean result = false;
		
		Intent it = new Intent(WifiService.this,Web.class);
		it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		it.putExtra("uri", uri.toString());
		startActivity(it);
		result = true;
		return result;
	}

	
	public class MyBinder extends Binder{  
        
        public WifiService getService(){  
            return WifiService.this;  
        }  
    }

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		return super.onUnbind(intent);
	}

		
	
}

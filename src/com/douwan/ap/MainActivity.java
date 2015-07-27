package com.douwan.ap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.douwan.ap.Wifi;

public class MainActivity extends Activity {
	
	private List<ScanResult> wifi_list = null;
	
	private Button open_wifi = null;
	private Button close_wifi = null;
	private Button scan_wifi = null;
	private EditText mac_input = null;
	public static TextView remain_count = null;
	private EditText timeout_input = null;
	private TextView portal_time = null;
	private EditText count_input = null;
	private TextView succeed_count = null;
	private TextView min_time = null;
	private TextView max_time = null;
	private TextView average_time= null;
	private TextView fail_count = null;
	private TextView dhcp_fail_count = null;
	private TextView response_fail_count = null;
	private TextView portal_fail_count = null;
	
	private String string_mac_input = null;
	private String string_count_input = null;
	public static String string_timeout_input = null;
	public static String string_remain_count = null;
	
	private int string_succeed_count = 0;
	private int string_fail_count = 0;
	private long old_time = 0;
	private long new_time = 0;
	private long time_all[]= new long [100];
	private long long_min_time = 0;
	private long long_max_time = 0;
	private float long_average_time = 0;
	private long sum_time = 0;
	
	private int int_dhcp_fail_count = 0;
	private int int_response_fail_count = 0;
	private int int_portal_fail_count = 0;
	
	private final int DHCP_FAIL = 1;
	private final int CAN_NETWORK = 2;
	private final int UPDATE_LISTVIEW = 3;
	private final int PORTAL_FAIL = 4;

	private Handler myHandler = null;
	
	private ListView listView = null;
	private AlertDialog.Builder alertDialog = null;
	private ClipboardManager copy= null;
	private String string_copy = null;
	private ArrayAdapter<String> arrayAdapter = null;
	private List<String> list = null;
	
	private WifiService wifiService = null;
	private MsgReceiver msgReceiver = null; 
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		CrashHandler crashHandler = new CrashHandler();    
		crashHandler.init(this);  

		
		findById();
		startBindService();
		receiveBroadcast();
			
		mac_input.addTextChangedListener(new Watcher());
		count_input.addTextChangedListener(new Watcher());
		timeout_input.addTextChangedListener(new Watcher());
		
		open_wifi.setOnClickListener(new MyOnClickListener());
		close_wifi.setOnClickListener(new MyOnClickListener());
		scan_wifi.setOnClickListener(new MyOnClickListener());
		
		listView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				if(scrollState == SCROLL_STATE_TOUCH_SCROLL)
				{		
					arrayAdapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
	}
	
	public boolean stop()
	{
		boolean flag = false;
		if(MainActivity.string_remain_count != null && Long.parseLong(MainActivity.string_remain_count) == 0)
		{
			Log.i("debug.info", "...........................");
//			unbindService(connect);
//			unregisterReceiver(msgReceiver);
			flag = true;
		}
		return flag;
	}
	
	public void findById()
	{
		open_wifi = (Button) findViewById(R.id.open_wifi);
		close_wifi = (Button) findViewById(R.id.close_wifi);
		scan_wifi = (Button) findViewById(R.id.scan_wifi);
		
		mac_input = (EditText) findViewById(R.id.mac_input);
		portal_time = (TextView) findViewById(R.id.portal_time);
		count_input = (EditText) findViewById(R.id.count_input);
		remain_count = (TextView) findViewById(R.id.remain_count);
		timeout_input = (EditText) findViewById(R.id.timeout_input);
		succeed_count = (TextView) findViewById(R.id.succeed_count);
		min_time = (TextView) findViewById(R.id.min_time);
		max_time = (TextView) findViewById(R.id.max_time);
		average_time = (TextView) findViewById(R.id.average_time);
		fail_count = (TextView) findViewById(R.id.fail_count);
		dhcp_fail_count = (TextView) findViewById(R.id.dhcp_fail_count);
		response_fail_count = (TextView) findViewById(R.id.response_fail_count);
		portal_fail_count = (TextView) findViewById(R.id.portal_fail_count);
		
		listView = (ListView) findViewById(R.id.listView);
	}
	
	public  void startBindService()
	{
		Intent it = new Intent(MainActivity.this,WifiService.class);
		bindService(it, connect,BIND_AUTO_CREATE);
		Log.i("debug.info", "startService");
	}
	
	ServiceConnection connect = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
			Log.i("debug.info", "onServiceDisconnected");
			wifiService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			// TODO Auto-generated method stub
			Log.i("debug.info", "onServiceConnected");
			wifiService = ((WifiService.MyBinder)arg1).getService();
			wifiService.execute();
		}
	};
	
	public void receiveBroadcast()
	{
		msgReceiver = new MsgReceiver();  
        IntentFilter intentFilter = new IntentFilter();  
        intentFilter.addAction("com.douwan.ap.WifiService");
        registerReceiver(msgReceiver, intentFilter);
	}
	
	public class MsgReceiver extends BroadcastReceiver{  
		  
        @Override  
        public void onReceive(Context context, Intent intent) { 
        	switch(intent.getStringExtra("wifiservice"))
        	{
        	case "openwifi":
        		openWifi(intent);
        		break;
        	case "closewifi":
        		closeWifi(intent);
        		break;
        	case "updatelistview":
        		if(stop())
        		{
        			updateListView(false);
        			wifiService.wifi.closeWifi();
        			break;
        		}
        		updateListView(true);
        		break;
        	case "scanwifi":
        		scanWifi(intent);
        		break;
        	case "dhcpfail":
        		if(stop())
        		{
        			updateListView(false);
        			wifiService.wifi.closeWifi();
        			break;
        		}
        		dhcpFail();
        		sendScanWifiBroadcast();
        		break;
        	case "cannetwork":
        		canNetwork(intent);
        		break;
        	case "processPortFail":
        		if(stop())
        		{
        			updateListView(false);
        			wifiService.wifi.closeWifi();
        			break;
        		}
        		portalFail();
        		sendScanWifiBroadcast();
        		break;
        	case "processWebBack":
        		if(stop())
        		{
        			updateListView(false);
        			wifiService.wifi.closeWifi();
        			break;
        		}
        		webBack(intent);
        		sendScanWifiBroadcast();
				break;
			default:
				break;
        	} 
          
        }
	}
	
	public void openWifi(Intent intent)
	{
		String tag = intent.getExtras().getString("tag");
		Toast.makeText(MainActivity.this,tag, Toast.LENGTH_SHORT).show();
	}
	
	public void closeWifi(Intent intent)
	{
		String tag = intent.getExtras().getString("tag");
		if(tag.equals("WIFI正在关闭..."))
		{
			setList(MainActivity.this,false);
		}
		Toast.makeText(MainActivity.this,tag, Toast.LENGTH_SHORT).show();
	}
	
	public void updateListView(boolean flag)
	{
		setList(MainActivity.this,flag);
	}
	
	public void scanWifi(Intent intent)
	{
		String tag = intent.getExtras().getString("tag");
		Toast.makeText(MainActivity.this,tag, Toast.LENGTH_SHORT).show();
	}
	
	public void dhcpFail()//失败：无法获取ip
	{
//		WifiService.wifi.closeWifi();
		Toast.makeText(MainActivity.this,"IP获取失败，关闭WFIF....", Toast.LENGTH_SHORT).show();
		string_remain_count = (Long.parseLong(string_remain_count)-1)+"";
		remain_count.setText("剩余测试次数: "+string_remain_count);
		string_fail_count = string_fail_count+1;
		fail_count.setText("失败次数:"+string_fail_count);
		int_dhcp_fail_count = int_dhcp_fail_count+1;
		dhcp_fail_count.setText("dhcp失败:"+int_dhcp_fail_count);
	}
	
	public void canNetwork(Intent intent)
	{
		String tag = intent.getExtras().getString("tag");
		Toast.makeText(MainActivity.this,tag, Toast.LENGTH_SHORT).show();
	}
	
	public boolean startBrows(String string)
	{
		Uri uri = Uri.parse(string);
		boolean result = false;	
		Intent it = new Intent(MainActivity.this,Web.class);
		it.putExtra("uri", uri.toString());
		startActivity(it);
		result = true;
		return result;
	}
	
	public void portalFail()
	{
//		WifiService.wifi.closeWifi();
		Toast.makeText(MainActivity.this,"弹窗失败....", Toast.LENGTH_SHORT).show();
		string_remain_count = (Long.parseLong(string_remain_count)-1)+"";
		remain_count.setText("剩余测试次数: "+string_remain_count);
		string_fail_count = string_fail_count+1;
		fail_count.setText("失败次数:"+string_fail_count);
		int_portal_fail_count = int_portal_fail_count +1;
		portal_fail_count.setText("弹窗失败:"+int_portal_fail_count);
//		WifiService.wifi.closeWifi();
	}
	
	public void webBack(Intent it)
	{
		new_time = it.getExtras().getLong("new_time");
		old_time = it.getExtras().getLong("old_time");
		Log.i("debug.info","弹窗时间: "+(new_time-old_time)+"ms");
		portal_time.setText("弹窗时间: "+(long)((new_time-old_time)/1000)+"s");
		
		string_remain_count = (Long.parseLong(string_remain_count)-1)+"";
		remain_count.setText("剩余测试次数: "+string_remain_count);
		
		Log.i("debug.info","redirect :"+it.getExtras().getString("redirect").equals("0"));
		if(it.getExtras().getString("redirect").equals("0") == false)
		{
			responseFail();
			Log.i("debug.info","redirect error.....");
			return;
		}
		countTime((long)((new_time-old_time)/1000));
	}
	
	public void responseFail()
	{
//		WifiService.wifi.closeWifi();
		Toast.makeText(MainActivity.this,"响应失败....", Toast.LENGTH_SHORT).show();
		/*string_remain_count = (Long.parseLong(string_remain_count)-1)+"";*/
		remain_count.setText("剩余测试次数: "+string_remain_count);
		string_fail_count = string_fail_count+1;
		fail_count.setText("失败次数:"+string_fail_count);
		int_response_fail_count = int_response_fail_count+1;
		response_fail_count.setText("响应失败:"+int_response_fail_count);
//		WifiService.wifi.closeWifi();
	}
	
	public void countTime(long time)
	{
		string_succeed_count = string_succeed_count+1;
		succeed_count.setText("成功次数:"+string_succeed_count);
		time_all[string_succeed_count-1] = time;
		
		sum_time = 0;
		query_time(time_all);
		
		min_time.setText("最小时间(s): "+long_min_time);
		max_time.setText("最大时间(s): "+long_max_time);
		average_time.setText("平均时间(s): "+long_average_time);
	}
	
	public void query_time(long query_time[])
	{
		long_min_time = query_time[0];
		long_max_time = query_time[0];
		for(int i=0;i<query_time.length;i++)
		{
			if(query_time[i] != 0)
			{
				if(query_time[i]>=long_max_time)
				{
					long_max_time = query_time[i];
				}
				else if(query_time[i]<=long_min_time)
				{
					long_min_time = query_time[i];
				}
				sum_time = sum_time + query_time[i];
			}
			
		}
		long_average_time = ((float)sum_time/string_succeed_count);
	}

	
	public class MyOnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.open_wifi:
				sendOpenWifiBroadcast();
				break;
			case R.id.close_wifi:
				sendCloseWifiBroadcast();
				break;
			case R.id.scan_wifi:
				if(isInputComplete() == false)
				{
					break;
				}
				sendScanWifiBroadcast();
			default:
				break;
			}
		}
		
	}
	
	
	public void sendOpenWifiBroadcast()
	{
		Intent it = new Intent("com.douwan.ap.mainActivity");
		it.putExtra("mainActivity","openwifi");
		sendBroadcast(it);
	}
	
	public void sendCloseWifiBroadcast()
	{
		Intent it = new Intent("com.douwan.ap.mainActivity");
		it.putExtra("mainActivity","closewifi");
		sendBroadcast(it);
	}
	
	public void sendScanWifiBroadcast()
	{
		Intent it = new Intent("com.douwan.ap.mainActivity");
		it.putExtra("mainActivity","scanwifi");
		it.putExtra("string_mac_input", string_mac_input);
		sendBroadcast(it);
	}
	
	
	public boolean isInputComplete()
	{
		if(string_mac_input == null || string_count_input == null || string_timeout_input == null)
		{
			Log.i("debug.info","输入信息为空");
			Toast.makeText(MainActivity.this,"输入信息为空", Toast.LENGTH_SHORT).show();
			return false;
		}
		else if(string_mac_input.isEmpty() || string_count_input.isEmpty() || string_timeout_input.isEmpty())
		{
			Log.i("debug.info","输入信息不完整");
			Toast.makeText(MainActivity.this,"输入信息不完整", Toast.LENGTH_SHORT).show();
			return false;
		}
		else if(string_remain_count.equals("0"))
		{
			Log.i("debug.info","请重新输入测试次数");
			Toast.makeText(MainActivity.this,"请重新输入测试次数", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	
	public void init_param()
	{
		string_succeed_count = 0;
		long_min_time = 0;
		long_max_time = 0;
		long_average_time = 0;
		sum_time = 0;
		int_dhcp_fail_count = 0;
		int_response_fail_count = 0;
		string_fail_count = 0;
		int_portal_fail_count = 0;
		
		succeed_count.setText("成功次数:0");
		max_time.setText("最大时间(s):0");
		min_time.setText("最小时间(s):0");
		average_time.setText("平均时间(s):0");
		
		fail_count.setText("失败次数:0");
		dhcp_fail_count.setText("dhcp失败:0");
		response_fail_count.setText("响应失败:0");
		portal_fail_count.setText("弹窗失败:0");
	}
	
	public class Watcher implements TextWatcher
	{

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub
			
		}

		@SuppressLint("DefaultLocale")
		@Override
		public void afterTextChanged(Editable s)
		{
			// TODO Auto-generated method stub
			string_mac_input = mac_input.getText().toString();
			string_mac_input = string_mac_input.toString();
			string_mac_input = string_mac_input.toLowerCase();
			
			init_param();
			time_all = new long [100];
			
			string_count_input = count_input.getText().toString();
			string_remain_count = string_count_input;
			remain_count.setText("剩余测试次数:"+string_remain_count);
	
			string_timeout_input = timeout_input.getText().toString();
		}		
		
	}
	
	public void setList(Context context,boolean yes)
	{
		list = new ArrayList<String>();
		if(yes && WifiService.wifi.wifiManager.isWifiEnabled())
		{		
			alertDialog = new AlertDialog.Builder(context);
			copy = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			WifiService.wifi.wifiManager.startScan();
			wifi_list = WifiService.wifi.wifiManager.getScanResults();
			
			while(wifi_list.size() == 0 || wifi_list.size() == 1)
			{
				
				wifi_list = WifiService.wifi.wifiManager.getScanResults();
				if(wifi_list.size() != 0 && wifi_list.size() != 1 )
				{
					break;
				}
			}
			for(int i=0;i<wifi_list.size();i++)
			{
//				Log.i("debug.info","SSID size = " + wifi_list.size());
//				Log.i("debug.info","SSID " + wifi_list.get(i).BSSID);
				list.add(wifi_list.get(i).SSID);
			}
			arrayAdapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1, list);
			listView.setAdapter(arrayAdapter);		
			listView.setOnItemLongClickListener(new OnItemLongClickListener() {
	
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
					
					for(int i=0;i<wifi_list.size();i++)
	        		{
						if(position == i)
	            		{
//	            			setTitle(wifi_list.get(i).BSSID);
	            			string_copy = wifi_list.get(i).BSSID;
	            			alertDialog.setTitle("MAC地址").setMessage(
	            					wifi_list.get(i).BSSID).setPositiveButton("复制", new DialogInterface.OnClickListener() 
	            					{ 
	            	                     
	            	                    @SuppressWarnings("deprecation")
										@Override 
	            	                    public void onClick(DialogInterface dialog, int which)
	            	                    { 
	            	                        // TODO Auto-generated method stub  
	            	                    	copy.setText(string_copy);
	            	                    } 
	            	                }).show();
	            			break;
	            		}
	        		} 
					return false;
				}
				
			}
			);
		}
		else {
			listView = (ListView) findViewById(R.id.listView);
			listView.setAdapter(new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,list));
		}
		
	}
	
	public class CrashHandler implements UncaughtExceptionHandler {
		private Thread.UncaughtExceptionHandler mDefaultHandler;

		public void init(Context ctx) {
			mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
			Thread.setDefaultUncaughtExceptionHandler(this);
		}

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			// TODO Auto-generated method stub
			if (!handleException(ex) && mDefaultHandler != null) {

				mDefaultHandler.uncaughtException(thread, ex);
			}
			
		}
		
		private boolean handleException(Throwable ex) {
			if (ex == null) {
				return false;
			}
			StringBuffer sb = new StringBuffer();

			Throwable e = ex.getCause() == null ? ex : ex.getCause();
			StackTraceElement[] stacks = e.getStackTrace();
			for (int i = 0; i < stacks.length; i++) {
				sb.append("class: ").append(stacks[i].getClassName())
						.append("; method: ").append(stacks[i].getMethodName())
						.append("; line: ").append(stacks[i].getLineNumber())
						.append(";  Exception: ").append(e.toString() + "\n");
			}
			Log.i("debug.info","handleException.........:" +sb.toString());

			return true;
		}
		
		   
	}

}

package com.example.uart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import cn.pda.serialport.SerialPort;
import cn.pda.serialport.Tools;

public class MainActivity extends Activity implements OnClickListener, AdapterView.OnItemClickListener {

	private final String TAG = MainActivity.class.getSimpleName();

	/** UI **/
	private EditText editRecv;
	private AutoCompleteTextView editSend;
	private AutoCompleteTextView editId;
	private Spinner spinnerBuadrate;
	private Spinner spinnerSerialport;
	private CheckBox checkRecv;
	private CheckBox checkSend;
	private Button buttonOpen;
	private Button buttonSend;
	private Button buttonClear;
	private Button buttonStart,btn_nolight;
	private Button buttonLight;
	private ListView listView;
	private TextView label;
	private TextView title;

	private Button btnPower;

	private String[] buadrateStrs;
	private String[] serialportStrs;
	private String[] powerStrs;

	private List<String> listBuadRate = new ArrayList<String>();
	private List<String> listSerialPort = new ArrayList<String>();
	private List<String> listPower = new ArrayList<String>();
	private ArrayAdapter adapter;
	private ListAdapter listAdapter;

	/** Serial port **/
	private SerialPort mSerialPort;
	private InputStream is;
	private OutputStream os;

	private int port = 13;
	private int buad;

	/** Thread to receive data from serial port**/
	private RecvThread recvThread;

	private boolean isLight = false;
	private boolean isHexRecv = false;
	private boolean isHexSend = false;
	private boolean isOpen = false;
	private List<Label> list = new ArrayList<Label>();

	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isTaskRoot()) {
			final Intent intent = getIntent();
			final String intentAction = intent.getAction();
			if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null
					&& intentAction.equals(Intent.ACTION_MAIN)) {
				finish();
				return;
			}
		}

		setContentView(R.layout.activity_main);

		context = this;
		initView();
		listener();

		Util.initSoundPool(this);

		//打开模块供电
		if (!isOpen) {
			open();
		}

		new Thread(runnable).start();

	}


	@Override
	protected void onDestroy() {
		if (isOpen) {
			this.close();
		}
		super.onDestroy();
	}

	/**
	 * Open serial port, and turn on the selected power
	 */
	private void open() {
		//硬件初始化打开串口
		try {
			Log.e(TAG, "open, port >>>>>> " + port);
			Log.e(TAG, "open, buad >>>>>> " + buad);
			mSerialPort = new SerialPort(port, 115200, 0);
//			mSerialPort.power_5Von();
//			mSerialPort.power_3v3on();
			mSerialPort.rfidPoweron();
			is = mSerialPort.getInputStream();
			os = mSerialPort.getOutputStream();
			recvThread = new RecvThread();
			recvThread.start();
			isOpen = true;
			buttonOpen.setText(context.getResources().getString(R.string.close));
			title.setText("com 13, power_rfid");
			showToast("SerialPort open success");
		} catch (IOException e) {
			showToast("SerialPort init fail\n" + Log.getStackTraceString(e));
		}
	}

	/**
	 * Close serial port, and turn off the selected power
	 */
	private void close() {
		//硬件模块关闭下电
		try {
			if (startFlag) {
				onClick(buttonStart);
			}
			if (recvThread != null) {
				recvThread.interrupt();
				recvThread = null;
			}
			if (mSerialPort != null) {
				is.close();
				os.close();
//				mSerialPort.power_5Voff();
				mSerialPort.rfidPoweron();
				mSerialPort.close(13);
				isOpen = false;
				buttonOpen.setText(context.getResources().getString(R.string.open));
				mSerialPort = null;
			}
		} catch (IOException e) {
			Log.e(TAG, "close(), IOException >>>>>> " + Log.getStackTraceString(e));
		} catch (Exception e) {
			Log.e(TAG, "close(), Exception >>>>>> " + Log.getStackTraceString(e));
		}
	}

	boolean startFlag = false;
	/**
	 * Send command to serial port
	 * 发送串口指令
	 */
	private void sendCmd(String cmddata) {
		try {
			byte[] cmd = null;
			if (cmddata == null) {
				showToast("cmd is null");
			}
			if (isHexSend) {
				cmd = Tools.HexString2Bytes(cmddata);
			} else {
				cmd = (cmddata).getBytes();
			}
			os.write(cmd);
			Log.e(TAG, "sendCmd: " + cmddata );
		} catch (IOException e) {
			Log.e(TAG, "send(), IOException >>>>>> " + Log.getStackTraceString(e));
		} catch (Exception e) {
			Log.e(TAG, "send(), Exception >>>>>> " + Log.getStackTraceString(e));
		}

	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.buttonClear:
			list.clear();
			listdata.clear();
			listLight.clear();
			map.clear();
			list1.clear();
			label.setText("");
			editRecv.setText("");
			listView.setAdapter(listAdapter);
			break;
		case R.id.buttonStart:
			//开始读卡
			if (!isOpen) {
				showToast("先open打开串口");
				return;
			}
			if (!startFlag) {
				startFlag = true;
				//53570003FF4113为寻卡指令
				buttonStart.setText(getString(R.string.Stop));
				sendCmd("53570003FF4113");
				showToast("开始寻卡");
			} else {
				startFlag = false;
				//53570003FF4014为停止指令
				buttonStart.setText(getString(R.string.Start));
				sendCmd("53570003FF4014");
				showToast("关闭寻卡");
			}
			break;
			case R.id.buttonlight:
//				if (startFlag) {
//					//群体点亮指令
//					sendCmd("53570007FF65FFFFFFFFEF");
//					showToast("点亮标签");
//				} else {
//					showToast("需要先start开始寻卡");
//				}
				if (isstart) {
					isstart = false;
				} else {
					isstart = true;
				}
				break;
		default:
			break;
		}

	}


	private boolean isstart = false;
	private Handler handler = new Handler();
	private List<Label> list1 = new ArrayList<>();
	private List<String> listLight = new ArrayList<>();
	int a = 0;
	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if (isstart) {
				if (list1.size()!=0) {
					final String data = list1.get(a).getData();
					if (a < list1.size() - 1) {
						a = a + 1;
					} else {
						a = 0;
					}
					if (!listLight.contains(data)) {
						light(data);
						Log.e("send", "a: " + a);
					}
				}
			}
			handler.postDelayed(runnable,400);
		}
	};

	private void light(String ID) {
		//点亮标签
		if (startFlag) {
			//53570007FF65为固定的指令头,00000001为标签id,EA为前面所有数据校验所得校验位
			if (ID.length() != 10 || ID == null) {
				showToast("请先输入要点亮的标签的10位id号");
				return;
			}
			//10位10进制数转8位16进制
			String id1 = Long.toHexString(Long.valueOf(ID)) + "";
			while (id1.length() < 8) {
				id1 = "0" + id1;
			}
			label.setText(ID);
			String light = (80+type) + "0" + time;
			//计算校验和拼接点亮指令
//			String cmd = "53570007FF65" + id1 + Tools.GetCheckSum("53570007FF65" + id1);
			String cmd = "53570009FF65" + id1  + light + Tools.GetCheckSum("53570009FF65" + id1 +  light);
			sendCmd(cmd);
			showToast("点亮标签" + ID);
			isLight = true;

		} else {
			showToast("需要先start开始寻卡");
		}
	}


	private HashMap<String,Integer> map = new HashMap<String, Integer>();
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//点击list点亮对应标签
		boolean checked = list.get(position).getCheck();
		String data = list.get(position).getData();
		Log.e("TAG", "onItemClick: " + position);
		listAdapter.notifyDataSetChanged();
		light(list.get(position).getData());
	}

	/**
	 * recv thread receive serialport data
	 * //串口线程保持模块通信
	 * @author Administrator
	 * 
	 */
	private class RecvThread extends Thread {
		@Override
		public void run() {
			super.run();
			try {
				while (!isInterrupted()) {
					int size = 0;
					int available = 0;
					byte[] buffer = new byte[1024];
					if (is == null) {
						return;
					}
					Thread.sleep(200);
					available = is.available();
					if (available > 0) {
						size = is.read(buffer);
						if (size > 0) {
							onDataReceived(buffer, size);
						}
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "RecvThread, Exception >>>>>> " + Log.getStackTraceString(e));
			}
		}
	}

	/**
	 * add recv data on UI
	 * 
	 * @param buffer
	 * @param size
	 */
	private void onDataReceived(final byte[] buffer, final int size) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				//接收并处理模块返回数据
				if (editRecv.getText().toString().length() > 1024) {
					editRecv.setText("");
				}
				if (size > 9) {
					// Util.play(1, 0);
				}
				if (!startFlag) {
					return;
				}
				//过滤出标签的id号
				String recv = new String(buffer, 0, size);
				Log.e("Huang", "recv >>>>>> " + recv);
				String[] data = recv.split("\"ID\":\"");
				for (int i=0;i<data.length;i++) {
					String label = data[i];
					if (label.length()<10 || label.contains("CT")) {
						return;
					}
					Log.e("Huang", "recv >>>>>> " + label);
					if (!label.contains("DevSN")) {
						//得到10位数的标签id
						label = label.substring(0,10).replace("\",","");
						Log.e("HEX:", "HEX:"+ label);
						if (label.length()==8) {
							label = Long.parseLong(label,16) + "";
						}
						for (int a=label.length();a<10;a++) {
							label = "0" +label;
						}
						Log.e(TAG, "data: " + label );
						Util.play(1, 0);
						if (listdata.contains(label)) {
							return;
						} else {
							//添加标签号到列表
							Label label1 = new Label();
							label1.setCheck(false);
							label1.setData(label);
							list.add(label1);
							listdata.add(label);
							listView.setAdapter(listAdapter);
						}
					}
				}
				Log.e(TAG, "run: " + listdata );
			}
		});
	}

	private Set<String> listdata = new HashSet<>();
	private Toast mToast;

	private void showToast(String content) {
		if (mToast == null) {
			mToast = Toast.makeText(this, content, Toast.LENGTH_SHORT);
			mToast.show();
		} else {
			mToast.setText(content);
			mToast.show();
		}
	}



	private void initView() {
		editRecv = (EditText) findViewById(R.id.editTextInfo);
		editSend = (AutoCompleteTextView) findViewById(R.id.editTextSend);
		// 1.1 �ı�������ϵĻس�����ACTION
		editSend.setImeOptions(EditorInfo.IME_ACTION_SEND);
		// 1.2��������̵Ļس���
		editSend.setOnKeyListener(onKeyListener);

		spinnerBuadrate = (Spinner) findViewById(R.id.spinnerBuadrate);
		spinnerSerialport = (Spinner) findViewById(R.id.spinnerSerialport);
		checkRecv = (CheckBox) findViewById(R.id.checkBoxHexRecv);
		checkSend = (CheckBox) findViewById(R.id.checkBoxHexSend);
		buttonOpen = (Button) findViewById(R.id.buttonOpen);
		buttonSend = (Button) findViewById(R.id.buttonSend);
		buttonClear = (Button) findViewById(R.id.buttonClear);
		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonLight = (Button) findViewById(R.id.buttonlight);
		listView = (ListView) findViewById(R.id.listview);
		adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1 , list);
		listAdapter = new ListAdapter(MainActivity.this, list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		label = (TextView) findViewById(R.id.text_label);
		title = (TextView) findViewById(R.id.textTitle);


		buadrateStrs = context.getResources().getStringArray(R.array.buadrateArray);
		serialportStrs = context.getResources().getStringArray(R.array.serialportArray);
		powerStrs = context.getResources().getStringArray(R.array.powerArray);

		btnPower = (Button) findViewById(R.id.button_Power);
		for (String buad : buadrateStrs) {
			listBuadRate.add(buad);
		}
		for (String serial : serialportStrs) {
			listSerialPort.add(serial);
		}
		for (String power : powerStrs) {
			listPower.add(power);
		}
		spinnerBuadrate.setAdapter(
				new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listBuadRate));
		spinnerSerialport.setAdapter(
				new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listSerialPort));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				setDialog();
				break ;
		}
		return super.onOptionsItemSelected(item);
	}

	int type = 2;
	int time = 0;
	private void setDialog(){
		View view1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.setting,null,false);
		final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setView(view1).create();
		Button ok = view1.findViewById(R.id.button_ok);
		Button cancel = view1.findViewById(R.id.button_cancel);
		final Spinner spinner = view1.findViewById(R.id.filter_type);
		final Spinner spinner1 = view1.findViewById(R.id.filter_time);
		spinner1.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item,MainActivity.this.getResources().getStringArray(R.array.filter_time)));
		spinner.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item,MainActivity.this.getResources().getStringArray(R.array.filter_type)));
		spinner.setSelection(type);
		spinner1.setSelection(time);
		ok.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				type = spinner.getSelectedItemPosition();
				time = spinner1.getSelectedItemPosition();
				showToast("设置成功");
				alertDialog.cancel();
			}
		});
		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				alertDialog.cancel();
			}
		});
		alertDialog.show();
		alertDialog.setCanceledOnTouchOutside(false);               //点击框外不起作用，返回键也不能用
		//此处设置位置窗体大小，我这里设置为了手机屏幕宽度的3/4  注意一定要在show方法调用后再写设置窗口大小的代码，否则不起效果会
		alertDialog.getWindow().setLayout((ScreenUtils.getScreenWidth(MainActivity.this)/8*7), LinearLayout.LayoutParams.WRAP_CONTENT);
	}


	/**
	 * Listener of keyboard
	 */
	private OnKeyListener onKeyListener = new OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
				/* ��������� */
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(
						Context.INPUT_METHOD_SERVICE);
				if (inputMethodManager.isActive()) {
					inputMethodManager.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
				}
				return true;
			}
			return false;
		}
	};

	/**
	 * listen componet
	 */
	private void listener() {
		btnPower.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

			}
		});
		spinnerSerialport.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
				port = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});
		spinnerBuadrate.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
				String buadrate = buadrateStrs[position];
				buad = Integer.valueOf(buadrate);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});

		checkRecv.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				isHexRecv = isChecked;

			}
		});
		checkSend.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override``
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				isHexSend = isChecked;

			}	`
		});

		buttonOpen.setOnClickListener(this);
		buttonSend.setOnClickListener(this);
		buttonClear.setOnClickListener(this);
		buttonStart.setOnClickListener(this);
		buttonLight.setOnClickListener(this);

		spinnerBuadrate.setSelection(10);
		spinnerSerialport.setSelection(13);
		checkRecv.setChecked(false);
		checkSend.setChecked(true);
	}

}

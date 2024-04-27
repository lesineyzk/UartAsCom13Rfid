package com.example.uart;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.SoundPool;

public class Util {

	
	public static SoundPool sp ;
	public static Map<Integer, Integer> suondMap;
	public static Context context;
	
	//��ʼ��������
	public static void initSoundPool(Context context){
		Util.context = context;
		sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 1);
		suondMap = new HashMap<Integer, Integer>();
		suondMap.put(1, sp.load(context, R.raw.msg, 1));
	}
	
	//��������������
	public static  void play(int sound, int number){
		AudioManager am = (AudioManager)Util.context.getSystemService(Util.context.AUDIO_SERVICE);
	   //���ص�ǰAlarmManager�������
	    float audioMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	        
	        //���ص�ǰAudioManager���������ֵ
	        float audioCurrentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
	        float volumnRatio = audioCurrentVolume/audioMaxVolume;
	        sp.play(
	        		suondMap.get(sound), //���ŵ�����Id 
	        		audioCurrentVolume, //����������
	        		audioCurrentVolume, //����������
	                1, //���ȼ���0Ϊ���
	                number, //ѭ��������0�޲�ѭ����-1����Զѭ��
	                1);//�ط��ٶȣ�ֵ��0.5-2.0֮�䣬1Ϊ�����ٶ�
	    }
	
	//��ȡ��һ�α����,��Դ��","����
	public static String getPowers(Context context){
		SharedPreferences shared = context.getSharedPreferences("config", Context.MODE_PRIVATE) ;
		return shared.getString("power", "5v")  ;
	}
	
	//������һ�α����
	public static void savePower(Context context, String powers){
		SharedPreferences shared = context.getSharedPreferences("config", Context.MODE_PRIVATE) ;
		Editor editor = shared.edit() ;
		editor.putString("power", powers) ;
		editor.commit() ;
	}
	
}

package cn.pda.serialport;


import android.util.Log;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Tools {

	//XOR check 异或校验
	public static byte getXor(byte[] datas) {
		byte temp = datas[0];
		for (int i = 1; i < datas.length; i++) {
			temp ^= datas[i];
		}
		return temp;
	}

	public static String GetCheckSum(String cmd){
		return getCheckSum(cmd,2);
	}

	/**
	 * 和校验，取最后round位
	 * @param round 取后面多少位
	 */
	public static String getCheckSum(String cmd, int round) {
		int lenth = cmd.length() / 2;
		int cmmSum = 0;
		for (int i = 0; i < lenth; i++) {
			//每两位转为16进制进行计算
			int c = Integer.valueOf(cmd.substring(0, 2), 0x10);
			cmd = cmd.substring(2);
			cmmSum = cmmSum + c;
		}
		//结果大于255，对结果取补码
		if (cmmSum>255) {
			cmmSum = (byte)(~cmmSum + 1);
		}
		String newString = Integer.toHexString(cmmSum);
		newString = "00000000000000000000000" + newString;//这里黑科技
		//这里获得我们需要返回的位数
		newString = newString.substring(newString.length() - round);
		return newString;
	}




	//byte תʮ������
		public static String Bytes2HexString(byte[] b, int size) {
		    String ret = "";
		    for (int i = 0; i < size; i++) {
		      String hex = Integer.toHexString(b[i] & 0xFF);
		      if (hex.length() == 1) {
		        hex = "0" + hex;
		      }
		      ret += hex.toUpperCase();
		    }
		    return ret;
		  }
		
		public static byte uniteBytes(byte src0, byte src1) {
		    byte _b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();
		    _b0 = (byte)(_b0 << 4);
		    byte _b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
		    byte ret = (byte)(_b0 ^ _b1);
		    return ret;
		  }
		
		//ʮ������תbyte
		public static byte[] HexString2Bytes(String src) {
			int len = src.length() / 2;
			byte[] ret = new byte[len];
			byte[] tmp = src.getBytes();

			for (int i = 0; i < len; i++) {
				ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
			}
			return ret;
		}
		
		/* byte[]תInt */
		public static int bytesToInt(byte[] bytes)
		{
			int addr = bytes[0] & 0xFF;
			addr |= ((bytes[1] << 8) & 0xFF00);
			addr |= ((bytes[2] << 16) & 0xFF0000);
			addr |= ((bytes[3] << 25) & 0xFF000000);
			return addr;

		}

		/* Intתbyte[] */
		public static byte[] intToByte(int i)
		{
			byte[] abyte0 = new byte[4];
			abyte0[0] = (byte) (0xff & i);
			abyte0[1] = (byte) ((0xff00 & i) >> 8);
			abyte0[2] = (byte) ((0xff0000 & i) >> 16);
			abyte0[3] = (byte) ((0xff000000 & i) >> 24);
			return abyte0;
		}
		
		
		

}

package com.non.packer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;

public class MergeDexs {
	public static final String SHELL_DEX = "lib/shell.dex";
	public static final String ORIGIN_DEX = "tmp/origin.dex";
	public static final String MERGED_CLASSES_DEX = "tmp/classes.dex";

	/**
	 * �ϲ�shell.dex��origin.dex,����classes.dex
	 */
	public static void merge() throws Exception {
		Utils.savelog("---------------------------------------------\n"
				+ "merging dexs...\n");
		byte[] crypteddata = Utils.encrypt(Utils.readbytes(ORIGIN_DEX));// ����origin.dex
		byte[] shelldexdata = Utils.readbytes(SHELL_DEX);// ��ȡshell.dex
		// Ҫ��origin.dex��ӵ�shell.dex����,�����origin.dex�ĳ��ȵ�ĩβ,
		int cryptlen = crypteddata.length;
		int shelllen = shelldexdata.length;
		// ����ϲ���classes.dex���ܳ���(����dex�ĳ���+4�ֽ�)
		int totallen = cryptlen + shelllen + 4;
		print("cryptlen:" + cryptlen + "\t0x" + Integer.toHexString(cryptlen));
		print("shelllen:" + shelllen + "\t0x" + Integer.toHexString(shelllen));
		print("totallen:" + totallen + "\t0x" + Integer.toHexString(totallen));
		byte[] dstdex = new byte[totallen];
		// ���ο���shell.dex, origin.dex, len(origin.dex)��dstdex��
		System.arraycopy(shelldexdata, 0, dstdex, 0, shelllen);
		System.arraycopy(crypteddata, 0, dstdex, shelllen, cryptlen);
		System.arraycopy(int2byte(cryptlen), 0, dstdex, totallen - 4, 4);// ���orgin.dex���ȵ�dstdex��ĩβ

		fixdexfilesize(dstdex);// �޸�dstdex�е��ļ���С
		fixdexsha1(dstdex);// ���¼���dstdex��ǩ��(SHA-1)
		fixdexchecksum(dstdex);// ���¼���dstdex��У���

		Utils.write2file(dstdex, MERGED_CLASSES_DEX);// ��dstdexд��classes.dex�ļ�
	}

	/**
	 * �޸�У���
	 */
	private static void fixdexchecksum(byte[] dstdex) {
		Adler32 adler = new Adler32();
		adler.update(dstdex, 12, dstdex.length - 12);// ��ȡdstdex�д�12�ֽڿ�ʼ,���ݴ�СΪ�ܳ��ȼ�ȥ12,�����ݵ�У��ֵ
		int value = (int) adler.getValue();
		byte[] bytes = int2byte(value);
		System.arraycopy(tobigend(bytes), 0, dstdex, 8, 4);// У���Ҳ�Ǵ�˴洢
		System.out.println("checksum:" + Integer.toHexString(value));
	}

	/**
	 * �޸�ǩ��
	 */
	private static void fixdexsha1(byte[] dstdex)
			throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");// ��ȡSHA-1�㷨ʵ��
		md.update(dstdex, 32, dstdex.length - 32);// ��ȡdstdex�д�32�ֽڿ�ʼ,���ݴ�СΪ�ܳ��ȼ�ȥ32,�����ݵ�sha1ֵ
		byte[] sha1 = md.digest();// ����ժҪ
		System.arraycopy(sha1, 0, dstdex, 12, 20);
		System.out.println("sha1:" + byte2hexstring(sha1).toString());
	}

	/**
	 * �޸�dstdex�е��ļ���С
	 */
	private static void fixdexfilesize(byte[] dstdex) {
		// ��dex����ת��Ϊ��˴洢
		System.arraycopy(tobigend(int2byte(dstdex.length)), 0, dstdex, 32, 4);
	}

	private static String byte2hexstring(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < b.length; i++) {
			sb.append(Integer.toHexString(b[i] & 0xFF));
		}
		return sb.toString();
	}

	/**
	 * ת��Ϊ���
	 */
	private static byte[] tobigend(byte[] b) {
		byte[] bytes = new byte[4];
		for (int i = 4; i > 0; i--) {
			bytes[(i - 1)] = b[(4 - i)];
		}
		return bytes;
	}

	/**
	 * ��int��תΪbyte
	 */
	private static byte[] int2byte(int num) {
		byte[] bytes = new byte[4];
		for (int i = 4; i > 0; i--) {
			bytes[(i - 1)] = ((byte) (num & 0xFF));
			num >>= 8;
		}
		return bytes;
	}

	private static void print(String str) {
		System.out.println(str);
	}
}

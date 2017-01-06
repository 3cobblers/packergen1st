package com.non.packer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;

public class MergeDexs {
	public static final String SHELL_DEX = "lib/shell.dex";
	public static final String ORIGIN_DEX = "tmp/origin.dex";
	public static final String MERGED_CLASSES_DEX = "tmp/classes.dex";

	/**
	 * 合并shell.dex和origin.dex,生成classes.dex
	 */
	public static void merge() throws Exception {
		Utils.savelog("---------------------------------------------\n"
				+ "merging dexs...\n");
		byte[] crypteddata = Utils.encrypt(Utils.readbytes(ORIGIN_DEX));// 加密origin.dex
		byte[] shelldexdata = Utils.readbytes(SHELL_DEX);// 读取shell.dex
		// 要将origin.dex添加到shell.dex后面,并添加origin.dex的长度到末尾,
		int cryptlen = crypteddata.length;
		int shelllen = shelldexdata.length;
		// 计算合并后classes.dex的总长度(两个dex的长度+4字节)
		int totallen = cryptlen + shelllen + 4;
		print("cryptlen:" + cryptlen + "\t0x" + Integer.toHexString(cryptlen));
		print("shelllen:" + shelllen + "\t0x" + Integer.toHexString(shelllen));
		print("totallen:" + totallen + "\t0x" + Integer.toHexString(totallen));
		byte[] dstdex = new byte[totallen];
		// 依次拷贝shell.dex, origin.dex, len(origin.dex)到dstdex中
		System.arraycopy(shelldexdata, 0, dstdex, 0, shelllen);
		System.arraycopy(crypteddata, 0, dstdex, shelllen, cryptlen);
		System.arraycopy(int2byte(cryptlen), 0, dstdex, totallen - 4, 4);// 添加orgin.dex长度到dstdex的末尾

		fixdexfilesize(dstdex);// 修复dstdex中的文件大小
		fixdexsha1(dstdex);// 重新计算dstdex的签名(SHA-1)
		fixdexchecksum(dstdex);// 重新计算dstdex的校验和

		Utils.write2file(dstdex, MERGED_CLASSES_DEX);// 将dstdex写入classes.dex文件
	}

	/**
	 * 修复校验和
	 */
	private static void fixdexchecksum(byte[] dstdex) {
		Adler32 adler = new Adler32();
		adler.update(dstdex, 12, dstdex.length - 12);// 获取dstdex中从12字节开始,数据大小为总长度减去12,的数据的校验值
		int value = (int) adler.getValue();
		byte[] bytes = int2byte(value);
		System.arraycopy(tobigend(bytes), 0, dstdex, 8, 4);// 校验和也是大端存储
		System.out.println("checksum:" + Integer.toHexString(value));
	}

	/**
	 * 修复签名
	 */
	private static void fixdexsha1(byte[] dstdex)
			throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");// 获取SHA-1算法实例
		md.update(dstdex, 32, dstdex.length - 32);// 获取dstdex中从32字节开始,数据大小为总长度减去32,的数据的sha1值
		byte[] sha1 = md.digest();// 生成摘要
		System.arraycopy(sha1, 0, dstdex, 12, 20);
		System.out.println("sha1:" + byte2hexstring(sha1).toString());
	}

	/**
	 * 修复dstdex中的文件大小
	 */
	private static void fixdexfilesize(byte[] dstdex) {
		// 将dex长度转换为大端存储
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
	 * 转换为大端
	 */
	private static byte[] tobigend(byte[] b) {
		byte[] bytes = new byte[4];
		for (int i = 4; i > 0; i--) {
			bytes[(i - 1)] = b[(4 - i)];
		}
		return bytes;
	}

	/**
	 * 将int型转为byte
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

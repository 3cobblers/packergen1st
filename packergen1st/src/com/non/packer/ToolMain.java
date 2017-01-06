package com.non.packer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ToolMain {
	private static final String sline = "-----------------------------------------\n";
	static {

		try {
			File f = new File("log.txt");
			if (f.exists()) {
				f.delete();
			}
			f.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			// 查找当前目录下的第一个apk文件
			String apkpath = get1stapk();
			if ("".equals(apkpath))
				return;
			// 从apk文件中读取classes.dex,命名为origin.dex
			Utils.readDex(apkpath, "classes.dex");
			// 将origin.dex和shell.dex合并生成classes.dex
			MergeDexs.merge();
			// 反编译apk文件，(为了得到可修改的AndroiManifest.xml)
			Utils.disassmApk(apkpath);
			// 修改AndroidManifest.xml中application标签的属性
			Utils.fixManifest();
			// 重打包为apk
			Utils.compileApk();

			// 将classes.dex添加到重打包后的apk中
			String srcfile = MergeDexs.MERGED_CLASSES_DEX;
			Utils.addfile2zip(srcfile, Utils.OUTPUT_APK_DIR,
					srcfile.substring(srcfile.lastIndexOf('/') + 1));
			// 签名apk
			Utils.signapk(Utils.TMP_APK_DIR);
			Utils.savelog("---------------------------------------------\n"
					+ "deleting tmp...\n");
			// 删除临时文件/夹
			File f = new File(Utils.TMP_DIR);
			Utils.deleteAll(f);
			Utils.savelog("done\n---------------------------------------------\n");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			try {
				FileOutputStream ff = new FileOutputStream(new File("log.txt"));
				ff.write(e.toString().getBytes("utf-8"));
				ff.flush();
				ff.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

	}

	private static String get1stapk() {
		// 获取用户的当前目录
		File file = new File(System.getProperty("user.dir"));
		String[] files = file.list();
		// 查看目录下第一个apk文件，找到则返回
		for (String name : files) {
			if (name.endsWith(".apk")) {
				return name;
			}
		}
		return "";
	}
}

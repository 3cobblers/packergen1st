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
			// ���ҵ�ǰĿ¼�µĵ�һ��apk�ļ�
			String apkpath = get1stapk();
			if ("".equals(apkpath))
				return;
			// ��apk�ļ��ж�ȡclasses.dex,����Ϊorigin.dex
			Utils.readDex(apkpath, "classes.dex");
			// ��origin.dex��shell.dex�ϲ�����classes.dex
			MergeDexs.merge();
			// ������apk�ļ���(Ϊ�˵õ����޸ĵ�AndroiManifest.xml)
			Utils.disassmApk(apkpath);
			// �޸�AndroidManifest.xml��application��ǩ������
			Utils.fixManifest();
			// �ش��Ϊapk
			Utils.compileApk();

			// ��classes.dex��ӵ��ش�����apk��
			String srcfile = MergeDexs.MERGED_CLASSES_DEX;
			Utils.addfile2zip(srcfile, Utils.OUTPUT_APK_DIR,
					srcfile.substring(srcfile.lastIndexOf('/') + 1));
			// ǩ��apk
			Utils.signapk(Utils.TMP_APK_DIR);
			Utils.savelog("---------------------------------------------\n"
					+ "deleting tmp...\n");
			// ɾ����ʱ�ļ�/��
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
		// ��ȡ�û��ĵ�ǰĿ¼
		File file = new File(System.getProperty("user.dir"));
		String[] files = file.list();
		// �鿴Ŀ¼�µ�һ��apk�ļ����ҵ��򷵻�
		for (String name : files) {
			if (name.endsWith(".apk")) {
				return name;
			}
		}
		return "";
	}
}

package com.non.packer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Utils {
	public static final String AM_XML = "tmp/out/AndroidManifest.xml";
	public static final String OUT_DIR = "tmp\\out";
	public static final String TMP_DIR = "tmp";
	public static final String OUTPUT_APK_DIR = "tmp\\t.apk";
	private static String SIGNED_APK;
	public static final String TMP_APK_DIR = "tmp\\out.apk";
	private static final String APP_NAME = "APPLICATION_CLASS_NAME";
	private static final String APP_VALUE = "com.reinforce.app.MShellApplication";

	/**
	 * ����byte����,��ʱû��ʵ��
	 */
	public static byte[] encrypt(byte[] bytes) {
		return bytes;
	}

	/**
	 * ���ļ��ж�ȡbytes
	 */
	public static byte[] readbytes(String filedir) throws IOException {
		FileInputStream fis = new FileInputStream(new File(filedir));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] cache = new byte[1024];
		int len = 0;
		while ((len = fis.read(cache)) != -1) {
			baos.write(cache, 0, len);// ��Ϊһ�����һ��read����װ��cache,�����ַ�ʽȷ�����Ὣcacheĩβ������д����������
		}
		fis.close();
		baos.close();
		return baos.toByteArray();
	}

	/**
	 * ��byte����д���ļ�
	 * 
	 * @param dstdex
	 *            ��Ҫд���bytes����
	 * @param clsdex
	 *            �洢���ļ���
	 */
	public static void write2file(byte[] dstdex, String clsdex)
			throws Exception {
		File file = new File(clsdex);
		if (file.exists()) {
			file.delete();
		}
		file.createNewFile();
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(dstdex);
		fos.flush();
		fos.close();
	}

	/**
	 * ������apk�ļ�
	 */
	public static void disassmApk(String apkpath) throws Exception {
		File f = new File(OUT_DIR);
		if (f.exists())
			deleteAll(f);// ���Ŀ¼����,��ɾ��
		String cmd = "java -jar -Duser.language=en lib\\apktool.jar d "
				+ apkpath + " -o " + OUT_DIR;// ����javaִ��apktool.jar������apk�ļ�
		Runtime run = Runtime.getRuntime();
		Process process = run.exec(cmd);
		process.waitFor();
		savelog("---------------------------------------------\n"
				+ "disassembling apk...\n");
		savelog(process);
		process.destroy();
	}

	/**
	 * �ش��Ŀ¼Ϊapk�ļ�
	 */
	public static void compileApk() throws Exception {
		String cmd = "java -jar -Duser.language=en lib\\apktool.jar b "
				+ OUT_DIR + " -o " + OUTPUT_APK_DIR;// ����javaִ��apktool.jar�ش��
		Runtime run = Runtime.getRuntime();
		Process process = run.exec(cmd);
		process.waitFor();
		savelog("---------------------------------------------\n"
				+ "compiling apk...\n");
		savelog(process);
		process.destroy();
	}

	public static void savelog(Process process) throws Exception {
		BufferedInputStream bis = new BufferedInputStream(
				process.getInputStream());
		BufferedInputStream error = new BufferedInputStream(
				process.getErrorStream());

		// �����ش���ĵ�����Ϣ���ļ���,�Ա�鿴��־
		File f = new File("log.txt");
		FileOutputStream fos = new FileOutputStream(f, true);
		copy(bis, fos);
		copy(error, fos);
		bis.close();
		error.close();
		fos.close();
	}

	public static void savelog(String str) throws Exception {

		// ������Ϣ���ļ���,�Ա�鿴��־
		File f = new File("log.txt");
		FileOutputStream fos = new FileOutputStream(f, true);
		fos.write(str.getBytes("utf-8"));
		fos.flush();
		fos.close();
	}

	/**
	 * ����������
	 */
	public static void copy(InputStream is, OutputStream os) throws IOException {
		int len = 0;
		byte[] cache = new byte[8192];
		while ((len = is.read(cache)) > 0) {
			os.write(cache, 0, len);
		}
		os.flush();
	}

	/**
	 * �����ļ�
	 */
	public static void copy(String src, String dst) throws IOException {
		System.out.println("copy file from " + src + " to " + dst);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
				new File(src)));// ��BufferedInputStream��Ŀ����Ϊ����߶�д���ٶ�(�Ƚ��ļ�����BufferedInputStream�Ļ�����,Ĭ����8192,�������ٽ���д����)
		FileOutputStream fos = new FileOutputStream(new File(dst));

		int len = 0;
		byte[] cache = new byte[8192];
		while ((len = bis.read(cache)) > 0) {
			fos.write(cache, 0, len);
		}
		bis.close();
		fos.flush();
		fos.close();
		System.out.println("copy file finished.");
	}

	/**
	 * �Ӹ�����apk�ж�ȡclasses.dex,���Ϊorigin.dex
	 */
	public static void readDex(String apkpath, String filename)
			throws Exception {
		savelog("---------------------------------------------\n"
				+ "reading dex...\n");
		SIGNED_APK = "packed-" + apkpath;// �������յ�apk�ļ���
		File file = new File(apkpath);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
				new FileInputStream(file)));
		String name;// zipѹ�����е��ļ���
		// ��ѹ�����в���classes.dex,�ҵ��˾ͼ�����һ������
		do {
			ZipEntry entry = zis.getNextEntry();
			name = entry.getName();
		} while (!name.equals(filename));
		// �½�origin.dex�ļ�,��������classes.dex�ļ�
		File originfile = new File(MergeDexs.ORIGIN_DEX);
		if (!originfile.getParentFile().exists()) {
			System.out.println(originfile.getParentFile() + " not exist");
			originfile.getParentFile().mkdirs();
		}
		if (originfile.exists()) {
			originfile.delete();
		}
		System.out.println(originfile.getAbsolutePath());
		originfile.createNewFile();
		// ��ѹ�����ж�ȡclasses.dex�ļ�,���浽origin.dex��
		FileOutputStream fos = new FileOutputStream(originfile);
		int len = 0;
		byte[] cache = new byte[4096];
		while ((len = zis.read(cache)) > 0) {
			fos.write(cache, 0, len);
		}
		fos.flush();
		fos.close();
		System.out.println(name + " extract success");
		zis.closeEntry();
		zis.close();
	}

	/**
	 * ���ļ�ǩ��
	 */
	public static void signapk(String file) throws Exception {
		savelog("---------------------------------------------\n"
				+ "signing apk...\n");
		System.out.println("sign apk " + file);
		String cmd = "java -jar lib\\signapk.jar lib\\testkey.x509.pem lib\\testkey.pk8 "
				+ file + " " + SIGNED_APK;// ����javaִ��signapk.jar��apkǩ��
		Runtime run = Runtime.getRuntime();

		Process process = run.exec(cmd);
		process.waitFor();
		process.destroy();

	}

	/**
	 * ����ļ���zip����,��Ҫʵ����addbyte2zip��
	 */
	public static void addfile2zip(String srcfile, String srczip,
			String showname) throws Exception {
		savelog("---------------------------------------------\n"
				+ "adding file...\n");
		addbytes2zip(readbytes(srcfile), srczip, showname);
		System.out.println("add file done");
	}

	/**
	 * ���bytes��zip����,��Ҫʵ������:��zip�е��ļ���������һ��zip�ļ���,���в������ļ���Ϊ��Ҫ��ӵ��ļ�,
	 * ������zip�е��ļ��������bytes��Ŀ��zip��
	 * 
	 * @param srczip
	 *            ��Ҫ��������zip��
	 * @param bytes
	 *            ��Ҫ������bytes
	 * @param showname
	 *            bytes��zip����ʾ��·�����ļ���
	 */
	private static void addbytes2zip(byte[] bytes, String srczip,
			String showname) throws Exception {
		System.out.println("src zip:" + srczip);
		System.out.println("dst zip:" + TMP_APK_DIR);
		System.out.println("display name in zip:" + showname);

		String outdir = TMP_APK_DIR.substring(0, TMP_APK_DIR.lastIndexOf('\\'));
		File szip = new File(srczip);
		if (!szip.exists()) {
			return;
		}
		File outpath = new File(outdir);
		if (!outpath.exists()) {
			outpath.mkdirs();
		}
		File dzip = new File(TMP_APK_DIR);
		if (dzip.exists()) {
			dzip.delete();
		}
		ZipOutputStream zipos = new ZipOutputStream(new FileOutputStream(dzip));
		ZipFile szipfile = new ZipFile(szip);

		@SuppressWarnings("unchecked")
		Enumeration<ZipEntry> emu = (Enumeration<ZipEntry>) szipfile.entries();// ���zip�ļ��е�������Ŀ
		while (emu.hasMoreElements()) {
			// ��������������Ŀ,����ļ���Ϊ��Ҫ�������ļ�,����,���򿽱���Ŀ��zip�����
			ZipEntry entry = (ZipEntry) emu.nextElement();
			if (showname.equals(entry.getName())) {
				System.out.println("not copy " + entry.getName());
			} else {
				zipos.putNextEntry(entry);
				// ������ļ�,�����ļ�����
				if (!entry.isDirectory()) {
					System.out.println("copy file " + entry.getName());
					copy(szipfile.getInputStream(entry), zipos);
				}
				zipos.closeEntry();
			}
		}
		System.out.println("add file to zip");
		// ��zip������ļ�
		ZipEntry zEntry = new ZipEntry(showname);
		zipos.putNextEntry(zEntry);
		zipos.write(bytes);
		zipos.closeEntry();
		szipfile.close();
		zipos.close();
	}

	/**
	 * �޸�manifest�ļ�
	 */
	public static void fixManifest() throws Exception {
		savelog("---------------------------------------------\n"
				+ "fixing manifest...\n");
		String filename = AM_XML;
		File amxml = new File(filename);
		DocumentBuilder builder = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		Document document = builder.parse(amxml);// ��xml�ļ��н���

		Node app = document.getElementsByTagName("application").item(0);// �õ�application��ǩ(manifest��ֻ��1��application��ǩ,item(0)������һ��)

		// �޸�application��ǩ�е�android:name��valueֵ,���ؾɵ�valueֵ(��Ҫ���ɵ�ֵ������meta-data��)
		String old = setNodeAttr(app,
				new String[] { "android:name", APP_VALUE });
		// ����ɵ�valueֵ��.MyApplication������ʽ, ��Ҫ��ǰ����ϰ���
		if (old.startsWith(".")) {
			Node manifest = document.getElementsByTagName("manifest").item(0);
			String packagename = setNodeAttr(manifest,
					new String[] { "package" });// ��ȡmanifest��ǩ�е�packageֵ
			old = packagename + old;
		}
		// ��ȡapplication��ǩ�������ӱ�ǩ,�������meta-data��ǩ,�޸�meta-data��name��valueֵ,��������������meta-data��ǩ
		NodeList nodelist = app.getChildNodes();
		int pos = -1;
		if ((pos = isExist(nodelist, "meta-data")) != -1) {
			setNodeAttr(nodelist.item(pos), new String[] { "android:name",
					APP_NAME });
			setNodeAttr(nodelist.item(pos),
					new String[] { "android:value", old });
		} else {
			// mete-data������,���meta-data��ǩ
			Element node = document.createElement("meta-data");
			node.setAttribute("android:name", APP_NAME);
			node.setAttribute("android:value", old);
			app.appendChild(node);
		}
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.transform(new DOMSource(document), new StreamResult(filename));// ���޸ĺ��document���浽�ļ���
	}

	/**
	 * ���ñ�ǩ������,������Բ�����,�������.(ÿһ����ǩ��Ϊһ��Node,��ǩ�е�ÿ������<name,value>Ҳ����Ϊһ��Node)
	 * 
	 * @param n
	 *            ��Ҫ�޸ĵı�ǩ
	 * @param attr
	 *            ��Ҫ�޸ĵ����Լ���ֵ, ����Ϊ�ɱ����,����Ϊ1��ʾ��ȡ�����Ե�ֵ,����Ϊ2��ʾ�޸ĸ����Ե�ֵ
	 * @return ���ؾɵ�����ֵ
	 */
	private static String setNodeAttr(Node n, String... attr) {
		NamedNodeMap nnmap = n.getAttributes();// ȡ�ñ�ǩ���ȫ�����Խڵ�,ÿһ���ڵ�Ϊ<name,value>��ֵ��
		int len = nnmap.getLength();
		int i = 0;
		while (i < len) {
			Node node = nnmap.item(i);
			// �жϵ�ǰ�������Ƿ�����Ҫ�޸ĵ�������
			if (attr[0].equals(node.getNodeName())) {
				String old = node.getNodeValue();// ����ɵ�����ֵ
				if (attr.length == 2) {
					node.setNodeValue(attr[1]);// ���attrΪ2������,���޸�Ϊ�µ�ֵ
				}
				return old;
			}
			i++;
		}
		// ���Բ�����,���attr��������Ϊ2,�������
		if (attr.length == 2) {
			((Element) n).setAttribute(attr[0], attr[1]);
		}
		return "";
	}

	/**
	 * �жϱ�ǩ�б����Ƿ�����ӱ�ǩ,�����򷵻��ӱ�ǩ��λ��
	 */
	private static int isExist(NodeList list, String nodename) {
		int len = list.getLength();
		for (int i = 0; i < len; i++) {
			if (nodename.equals(list.item(i).getNodeName())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * ɾ���ļ��м��ļ�������������
	 */
	public static void deleteAll(File dir) {
		if (dir.isFile()) {
			System.out.println("delete " + dir);
			dir.delete();
			return;
		}
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File f : files) {
				deleteAll(f);
			}
			System.out.println("delete " + dir);
			dir.delete();
		}
	}
}

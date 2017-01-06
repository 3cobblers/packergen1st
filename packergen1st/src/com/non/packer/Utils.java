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
	 * 加密byte数组,暂时没有实现
	 */
	public static byte[] encrypt(byte[] bytes) {
		return bytes;
	}

	/**
	 * 从文件中读取bytes
	 */
	public static byte[] readbytes(String filedir) throws IOException {
		FileInputStream fis = new FileInputStream(new File(filedir));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] cache = new byte[1024];
		int len = 0;
		while ((len = fis.read(cache)) != -1) {
			baos.write(cache, 0, len);// 因为一般最后一次read不能装满cache,用这种方式确保不会将cache末尾空数据写入数据流中
		}
		fis.close();
		baos.close();
		return baos.toByteArray();
	}

	/**
	 * 将byte数组写入文件
	 * 
	 * @param dstdex
	 *            需要写入的bytes数据
	 * @param clsdex
	 *            存储的文件名
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
	 * 反编译apk文件
	 */
	public static void disassmApk(String apkpath) throws Exception {
		File f = new File(OUT_DIR);
		if (f.exists())
			deleteAll(f);// 如果目录存在,则删除
		String cmd = "java -jar -Duser.language=en lib\\apktool.jar d "
				+ apkpath + " -o " + OUT_DIR;// 调用java执行apktool.jar反编译apk文件
		Runtime run = Runtime.getRuntime();
		Process process = run.exec(cmd);
		process.waitFor();
		savelog("---------------------------------------------\n"
				+ "disassembling apk...\n");
		savelog(process);
		process.destroy();
	}

	/**
	 * 重打包目录为apk文件
	 */
	public static void compileApk() throws Exception {
		String cmd = "java -jar -Duser.language=en lib\\apktool.jar b "
				+ OUT_DIR + " -o " + OUTPUT_APK_DIR;// 调用java执行apktool.jar重打包
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

		// 保存重打包的调试信息到文件中,以便查看日志
		File f = new File("log.txt");
		FileOutputStream fos = new FileOutputStream(f, true);
		copy(bis, fos);
		copy(error, fos);
		bis.close();
		error.close();
		fos.close();
	}

	public static void savelog(String str) throws Exception {

		// 保存信息到文件中,以便查看日志
		File f = new File("log.txt");
		FileOutputStream fos = new FileOutputStream(f, true);
		fos.write(str.getBytes("utf-8"));
		fos.flush();
		fos.close();
	}

	/**
	 * 拷贝数据流
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
	 * 拷贝文件
	 */
	public static void copy(String src, String dst) throws IOException {
		System.out.println("copy file from " + src + " to " + dst);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
				new File(src)));// 用BufferedInputStream的目的是为了提高读写的速度(先将文件读到BufferedInputStream的缓冲中,默认是8192,读满了再进行写操作)
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
	 * 从给定的apk中读取classes.dex,另存为origin.dex
	 */
	public static void readDex(String apkpath, String filename)
			throws Exception {
		savelog("---------------------------------------------\n"
				+ "reading dex...\n");
		SIGNED_APK = "packed-" + apkpath;// 设置最终的apk文件名
		File file = new File(apkpath);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
				new FileInputStream(file)));
		String name;// zip压缩包中的文件名
		// 从压缩包中查找classes.dex,找到了就继续下一步操作
		do {
			ZipEntry entry = zis.getNextEntry();
			name = entry.getName();
		} while (!name.equals(filename));
		// 新建origin.dex文件,用来保存classes.dex文件
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
		// 从压缩包中读取classes.dex文件,保存到origin.dex中
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
	 * 对文件签名
	 */
	public static void signapk(String file) throws Exception {
		savelog("---------------------------------------------\n"
				+ "signing apk...\n");
		System.out.println("sign apk " + file);
		String cmd = "java -jar lib\\signapk.jar lib\\testkey.x509.pem lib\\testkey.pk8 "
				+ file + " " + SIGNED_APK;// 调用java执行signapk.jar对apk签名
		Runtime run = Runtime.getRuntime();

		Process process = run.exec(cmd);
		process.waitFor();
		process.destroy();

	}

	/**
	 * 添加文件到zip包中,主要实现在addbyte2zip中
	 */
	public static void addfile2zip(String srcfile, String srczip,
			String showname) throws Exception {
		savelog("---------------------------------------------\n"
				+ "adding file...\n");
		addbytes2zip(readbytes(srcfile), srczip, showname);
		System.out.println("add file done");
	}

	/**
	 * 添加bytes到zip包中,主要实现流程:将zip中的文件拷贝到另一个zip文件中,其中不拷贝文件名为需要添加的文件,
	 * 拷贝完zip中的文件后再添加bytes到目标zip中
	 * 
	 * @param srczip
	 *            需要拷贝到的zip包
	 * @param bytes
	 *            需要拷贝的bytes
	 * @param showname
	 *            bytes在zip中显示的路径和文件名
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
		Enumeration<ZipEntry> emu = (Enumeration<ZipEntry>) szipfile.entries();// 获得zip文件中的所有条目
		while (emu.hasMoreElements()) {
			// 遍历拷贝所有条目,如果文件名为需要拷贝的文件,跳过,否则拷贝条目到zip输出流
			ZipEntry entry = (ZipEntry) emu.nextElement();
			if (showname.equals(entry.getName())) {
				System.out.println("not copy " + entry.getName());
			} else {
				zipos.putNextEntry(entry);
				// 如果是文件,拷贝文件内容
				if (!entry.isDirectory()) {
					System.out.println("copy file " + entry.getName());
					copy(szipfile.getInputStream(entry), zipos);
				}
				zipos.closeEntry();
			}
		}
		System.out.println("add file to zip");
		// 向zip中添加文件
		ZipEntry zEntry = new ZipEntry(showname);
		zipos.putNextEntry(zEntry);
		zipos.write(bytes);
		zipos.closeEntry();
		szipfile.close();
		zipos.close();
	}

	/**
	 * 修改manifest文件
	 */
	public static void fixManifest() throws Exception {
		savelog("---------------------------------------------\n"
				+ "fixing manifest...\n");
		String filename = AM_XML;
		File amxml = new File(filename);
		DocumentBuilder builder = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		Document document = builder.parse(amxml);// 从xml文件中解析

		Node app = document.getElementsByTagName("application").item(0);// 得到application标签(manifest中只有1个application标签,item(0)就是那一个)

		// 修改application标签中的android:name的value值,返回旧的value值(需要将旧的值保存在meta-data中)
		String old = setNodeAttr(app,
				new String[] { "android:name", APP_VALUE });
		// 如果旧的value值是.MyApplication这种形式, 需要在前面加上包名
		if (old.startsWith(".")) {
			Node manifest = document.getElementsByTagName("manifest").item(0);
			String packagename = setNodeAttr(manifest,
					new String[] { "package" });// 获取manifest标签中的package值
			old = packagename + old;
		}
		// 获取application标签的所有子标签,如果存在meta-data标签,修改meta-data的name和value值,如果不存在则添加meta-data标签
		NodeList nodelist = app.getChildNodes();
		int pos = -1;
		if ((pos = isExist(nodelist, "meta-data")) != -1) {
			setNodeAttr(nodelist.item(pos), new String[] { "android:name",
					APP_NAME });
			setNodeAttr(nodelist.item(pos),
					new String[] { "android:value", old });
		} else {
			// mete-data不存在,添加meta-data标签
			Element node = document.createElement("meta-data");
			node.setAttribute("android:name", APP_NAME);
			node.setAttribute("android:value", old);
			app.appendChild(node);
		}
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.transform(new DOMSource(document), new StreamResult(filename));// 将修改后的document保存到文件中
	}

	/**
	 * 设置标签的属性,如果属性不存在,添加属性.(每一个标签作为一个Node,标签中的每项属性<name,value>也都作为一个Node)
	 * 
	 * @param n
	 *            需要修改的标签
	 * @param attr
	 *            需要修改的属性及其值, 这里为可变参数,长度为1表示读取该属性的值,长度为2表示修改该属性的值
	 * @return 返回旧的属性值
	 */
	private static String setNodeAttr(Node n, String... attr) {
		NamedNodeMap nnmap = n.getAttributes();// 取得标签里的全部属性节点,每一个节点为<name,value>键值对
		int len = nnmap.getLength();
		int i = 0;
		while (i < len) {
			Node node = nnmap.item(i);
			// 判断当前属性名是否是需要修改的属性名
			if (attr[0].equals(node.getNodeName())) {
				String old = node.getNodeValue();// 保存旧的属性值
				if (attr.length == 2) {
					node.setNodeValue(attr[1]);// 如果attr为2个参数,则修改为新的值
				}
				return old;
			}
			i++;
		}
		// 属性不存在,如果attr参数个数为2,添加属性
		if (attr.length == 2) {
			((Element) n).setAttribute(attr[0], attr[1]);
		}
		return "";
	}

	/**
	 * 判断标签列表中是否存在子标签,存在则返回子标签的位置
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
	 * 删除文件夹及文件夹中所有内容
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

/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.Logger;

/**
 * InputStream流转字符串的工具
 * @author Kingstar
 * @since  1.9.8
 */
public class StreamUtil {

	private static final String UTF_8 = "UTF-8";

	private StreamUtil() {}

	/**
	 * InputStream转字符串. InputStream to String.
	 * @param in  InputStream对象.instance of InputStream.
	 * @return 字符串.string
	 */
	public static String stream2String(InputStream in) {
		return stream2String(in, UTF_8);
	}

	/**
	 * InputStream转字符串. InputStream to String.
	 * @param in  InputStream对象.instance of InputStream.
	 * @param charsetName 字符集名称.charset name
	 * @return 字符串.string
	 */
	public static String stream2String(InputStream in, String charsetName) {
		return stream2String(in, null, UTF_8);
	}

	public static String stream2String(InputStream in, Map<String, String> map) {
		return stream2String(in, map, UTF_8);
	}

	/**
	 * InputStream转字符串,并可替换字符值. InputStream to String,and replace some String.
	 * @param in  InputStream对象.instance of InputStream.
	 * @param map 需要替换的字符map,(key:old-String,value:new-String).
	 * @param charsetName 字符集名称.charset name
	 * @return 字符串.string
	 */
	public static String stream2String(InputStream in, Map<String, String> map, String charsetName) {

		if (in == null) return null;
		StringBuffer sb = new StringBuffer();
		try (BufferedReader bfReader = new BufferedReader(new InputStreamReader(in, charsetName));) {
			String line = bfReader.readLine();
			while (line != null) {
				if (map != null) line = replace(line, map);
				sb.append(line);
				line = bfReader.readLine();
				if (line != null) {
					sb.append("\n");
				}
			}
		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		} finally {
			try {
				if (in != null) in.close(); // V1.17
			} catch (Exception e2) {
				Logger.debug(e2.getMessage(), e2);
			}

		}

		return sb.toString();
	}

	private static String replace(String line, Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			line = line.replace(entry.getKey(), entry.getValue());
		}
		return line;
	}

	/**
	 * byte array to InputStream
	 * @param data
	 * @return
	 * @since 2.1.8 
	 */
	public static InputStream byteArray2Stream(byte[] data) {
		return new ByteArrayInputStream(data);
	}

	public static String byteArray2String(byte[] data) {
		return new String(data);
	}

	/**
	 * InputStream to byte array
	 * @param inputStream
	 * @return
	 * @throws IOException
	 * @since 2.1.8 
	 */
	public static byte[] stream2ByteArray(InputStream inputStream) {
		ByteArrayOutputStream byteArrayOs = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int bytesRead;
		try {
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				byteArrayOs.write(buffer, 0, bytesRead);
			}
		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		}
//		return byteArrayOs.toByteArray();
		byte[] a = byteArrayOs.toByteArray();

		try {
			if (byteArrayOs != null) byteArrayOs.close();
		} catch (Exception e) {
			// ignore
		}
		return a;
	}

	public static void inputStream2File(InputStream inputStream, String fileName) {
		File file = new File(fileName);

		try (OutputStream outputStream = new FileOutputStream(file)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			throw ExceptionHelper.convert(e);
		}
	}
}

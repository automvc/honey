/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.teasoft.honey.osql.core.ExceptionHelper;

/**
 * InputStream流转字符串的工具
 * @author Kingstar
 * @since  1.9.8
 */
public class StreamUtil {

	/**
	 * InputStream转字符串. InputStream to String.
	 * @param in  InputStream对象.instance of InputStream.
	 * @param charsetName 字符集名称.charset name
	 * @return 字符串.string
	 */
	public static String stream2String(InputStream in) {
		return stream2String(in, "UTF-8");
	}

	/**
	 * InputStream转字符串. InputStream to String.
	 * @param in  InputStream对象.instance of InputStream.
	 * @param charsetName 字符集名称.charset name
	 * @return 字符串.string
	 */
	public static String stream2String(InputStream in, String charsetName) {
		return stream2String(in, null, "UTF-8");
	}
	
	public static String stream2String(InputStream in, Map<String, String> map) {
		return stream2String(in, map, "UTF-8");
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
		BufferedReader bfReader = null;
		StringBuffer sb = new StringBuffer();
		try {
			bfReader = new BufferedReader(new InputStreamReader(in, charsetName));

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
				if (bfReader != null) bfReader.close();
			} catch (Exception e2) {

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

}

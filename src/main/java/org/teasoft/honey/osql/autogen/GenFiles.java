/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(aiteasoft@163.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.autogen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.util.FileUtil;
import org.teasoft.honey.osql.util.StringUtil;

/**
 * @author Kingstar
 * @since  1.7.2
 */
public class GenFiles {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator"); // 换行符
	
	private GenFiles() {}
	
	public static void genFile(String templatePath, Map<String, String> map, String targetFilePath) {
		genFile(templatePath, map, targetFilePath, "#{", "}");
	}

	public static void genFile(String templatePath, Map<String, String> map, String targetFilePath,String startToken,String endToken) {

		BufferedReader br = null;
		StringBuffer sb = new StringBuffer();
		boolean firstLine=true;
		try {
			br = FileUtil.readFile(templatePath);
			String temp = null;
			while ((temp = br.readLine()) != null) {
				temp = StringUtil.replaceWithMap(temp, map,startToken,endToken);
				if(firstLine) firstLine=false;
				else sb.append(LINE_SEPARATOR);
				sb.append(temp);
			}
			br.close();
		} catch (IOException e) {
			Logger.error(e.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ioe) {
					Logger.error(ioe.getMessage());
				}
			}
		}
		FileUtil.genFile(targetFilePath, sb.toString());
	}
	
	public static void genFileViaStream(String templatePath, Map<String, String> map, String targetFilePath) {
		String txt = readTemplateFile(templatePath);
		txt = StringUtil.replaceWithMap(txt, map, "#{", "}");
		FileUtil.genFile(targetFilePath, txt);
	}

	private static String readTemplateFile(String filePath) {
		StringBuilder stringBuilder = new StringBuilder();

//		org\abc\gencode\template\Rest.java.template  //用这种格式，打包成jar后，访问会有问题.

		filePath = filePath.replace("\\", "/"); // 要转换斜杠

//		if (!filePath.trim().startsWith("/"))
//			filePath = "/" + filePath.trim();
//		try (InputStream inputStream = GenCode.class.getResourceAsStream(filePath); //不行
		try (InputStream inputStream = GenFiles.class.getClassLoader()
				.getResourceAsStream(filePath);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line).append("\n");
			}
		} catch (IOException e) {
			Logger.error(e.getMessage(),e);
		}

		return stringBuilder.toString();
	}
	
	
}

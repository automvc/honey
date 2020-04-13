/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(aiteasoft@163.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.autogen;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.util.FileUtil;
import org.teasoft.honey.osql.util.StringUtil;

/**
 * @author Kingstar
 * @since  1.7.2
 */
public class GenFiles {

	private static String LINE_SEPARATOR = System.getProperty("line.separator"); // 换行符

	public static void genFile(String templatePath, Map<String, String> map, String targetFilePath) {

		BufferedReader br = null;
		StringBuffer sb = new StringBuffer();
		boolean firstLine=true;
		try {
			br = FileUtil.readFile(templatePath);
			String temp = null;
			while ((temp = br.readLine()) != null) {
				temp = StringUtil.replaceWithMap(temp, map);
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
		FileUtil.genAppendFile(targetFilePath, sb.toString());
	}
}

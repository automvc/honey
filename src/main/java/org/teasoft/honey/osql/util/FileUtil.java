/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(automvc@163.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.teasoft.honey.osql.core.Logger;

/**
 * @author Kingstar
 * @since  1.5
 */
public class FileUtil {

	public static void genFile(String fullPath, String fileName, String content) {
		
		// 生成文件
		if (!fullPath.endsWith(File.separator)) fullPath += File.separator;
		File folder = new File(fullPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		File entityFile = new File(fullPath + fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(entityFile)));
			bw.write(content);
			bw.flush();
			bw.close();
			Logger.info("Create file: "+fullPath + fileName);
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
	}
	
	public static void genFile(String basePath,String packagePath, String fileName, String content) {
		if (!basePath.endsWith(File.separator)) basePath += File.separator;
		String fullPath = basePath + packagePath.replace(".", File.separator) + File.separator;
		genFile( fullPath,  fileName,  content);
	}

}

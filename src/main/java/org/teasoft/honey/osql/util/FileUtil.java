/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(automvc@163.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.io.BufferedReader;

import org.teasoft.bee.file.FileCreator;
import org.teasoft.honey.file.FileHandle;

/**
 * @author Kingstar
 * @since  1.5
 */
public class FileUtil {
	private static FileCreator fileCreator=null;
	static{
		 fileCreator=new FileHandle();
		 fileCreator.setCharsetName("UTF-8");
	}

	public static void genFile(String fullPathAndName, String content) {
		fileCreator.genFile(fullPathAndName, content);
	}
	
	public static void genFile(String fullPath, String fileName, String content) {
		// 生成文件
		fileCreator.genFile(fullPath, fileName, content);
	}
	
	public static void genFile(String basePath,String packagePath, String fileName, String content) {
		fileCreator.genFile(basePath, packagePath, fileName, content);
	}
	
	public static void genAppendFile(String fullPathAndName, String content) {
		fileCreator.genAppendFile(fullPathAndName, content);
	}
	
	public static BufferedReader readFile(String fullPathAndName) {
		return fileCreator.readFile(fullPathAndName);
	}
	
}

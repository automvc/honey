/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.teasoft.bee.file.FileCreator;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author Kingstar
 * @since  1.7.1
 */
public class FileHandle implements FileCreator{

	@Override
	public void genFile(String fullPathAndName, String content) {
		File f = new File(fullPathAndName);
		
		if (!f.exists()) {
			String substr=fullPathAndName.substring(0,fullPathAndName.lastIndexOf(File.separator));
			new File(substr).mkdirs();
			Logger.info("Create file: "+fullPathAndName);
		}
		
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			bw.write(content);
			bw.flush();
			bw.close();
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
	}

	@Override
	public void genFile(String fullPath, String fileName, String content) {
		// 生成文件
		if (!fullPath.endsWith(File.separator)) fullPath += File.separator;
		
		File folder = new File(fullPath);
		if (!folder.exists()) {
			folder.mkdirs();
			Logger.info("Create file: "+fullPath + fileName);
		}

		File entityFile = new File(fullPath + fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(entityFile)));
			bw.write(content);
			bw.flush();
			bw.close();
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
	}
	
	@Override
	public void genFile(String basePath, String packagePath, String fileName, String content) {
		
		if (!basePath.endsWith(File.separator)) basePath += File.separator;
		String fullPath = basePath + packagePath.replace(".", File.separator) + File.separator;
		genFile( fullPath,  fileName,  content);
	}
	
	
	private String LINE_SEPARATOR = System.getProperty("line.separator"); // 换行符
	
	@Override
	public void genAppendFile(String fullPathAndName, String content) {
		File f = new File(fullPathAndName);
		
		if (!f.exists()) {
			String substr=fullPathAndName.substring(0,fullPathAndName.lastIndexOf(File.separator));
			new File(substr).mkdirs();
		}
		
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f,true)));//true,追加的方式
			bw.write(content);
			bw.append(LINE_SEPARATOR);
			bw.flush();
			bw.close();
			
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
	}
}

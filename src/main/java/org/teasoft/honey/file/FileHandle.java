/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.teasoft.bee.file.FileCreator;
import org.teasoft.honey.logging.Logger;

/**
 * @author Kingstar
 * @since  1.7.1
 */
public class FileHandle implements FileCreator {

	private String charsetName;

	@Override
	public void setCharsetName(String charsetName) {
		this.charsetName = charsetName;
	}

	public String getCharsetName() {
		if (this.charsetName == null || "".equals(charsetName.trim())) return "UTF-8";
		else return this.charsetName;
	}

	@Override
	public void genFile(String fullPathAndName, String content) {
		File f = new File(fullPathAndName);

		if (f.exists()) {
			Logger.info("The file already exist.");
			backFile(f);
		} else if (!f.exists()) {
			String substr = fullPathAndName.substring(0, fullPathAndName.lastIndexOf(File.separator));
			new File(substr).mkdirs();
			Logger.info("Create file: " + fullPathAndName);
		}

		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), getCharsetName()));) {
			bw.write(content);
			bw.flush();
			logGenFile(fullPathAndName);
		} catch (Exception e) {
			Logger.warn(e.getMessage());
		}

	}

	@Override
	public void genFile(String fullPath, String fileName, String content) {
		// 生成文件
		if (!fullPath.endsWith(File.separator)) fullPath += File.separator;

		File folder = new File(fullPath);
		boolean needCheckExist = false;
		if (!folder.exists()) {
			folder.mkdirs();
			Logger.debug("Create folder: " + fullPath);
		} else {
			needCheckExist = true;
		}

		File entityFile = new File(fullPath + fileName);
		if (needCheckExist) {
			if (entityFile.exists()) {
				Logger.info("The file already exist.");
				backFile(entityFile);
			}
		}
		try (BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(entityFile), getCharsetName()));) {
			bw.write(content);
			bw.flush();
			logGenFile(fullPath + fileName);
		} catch (Exception e) {
			Logger.warn(e.getMessage());
		}
	}

	@Override
	public void genFile(String basePath, String packagePath, String fileName, String content) {

		if (!basePath.endsWith(File.separator)) basePath += File.separator;
		String fullPath = basePath + packagePath.replace(".", File.separator) + File.separator;
		genFile(fullPath, fileName, content);
	}

	private String LINE_SEPARATOR = System.getProperty("line.separator"); // 换行符

	@Override
	public void genAppendFile(String fullPathAndName, String content) {
		File f = new File(fullPathAndName);

		if (!f.exists()) {
			String substr = fullPathAndName.substring(0, fullPathAndName.lastIndexOf(File.separator));
			new File(substr).mkdirs();
		}

		try (BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(f, true), getCharsetName()));// true,追加的方式
		) {
			bw.write(content);
			bw.append(LINE_SEPARATOR);
			bw.flush();
//			Logger.info("genAppendFile, file name: " + fullPathAndName); //no need, so many log
		} catch (Exception e) {
			Logger.warn(e.getMessage());
		}

	}

	@Override
	public BufferedReader readFile(String fullPathAndName) {
		File file = new File(fullPathAndName);
		return readFile(file);
	}

	@Override
	public BufferedReader readFile(File file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (IOException e) {
			Logger.warn(e.getMessage());
		}
		return reader;
	}

	private void logGenFile(String pathAndName) {
		Logger.info("Generate file successful. path: " + pathAndName);
	}

	/**
	 * back the file
	 * @param f
	 * @since 2.4.0
	 */
	public void backFile(File f) {
		String originalFileName = f.getName();
		// 备份文件名
		String backupFileName = originalFileName + "_" + System.currentTimeMillis() + ".bak";
		File backupFile = new File(f.getParent(), backupFileName);

		// copy
		try {
			Path sourcePath = f.toPath();
			Path backupPath = backupFile.toPath();
			Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			Logger.info("Backup file successful. path: " + backupFile.getAbsolutePath());
		} catch (IOException e) {
			Logger.debug("Backup file failed: " + backupFile.getAbsolutePath());
		}
	}

}

package org.teasoft.honey.osql.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.teasoft.bee.osql.exception.ConfigWrongException;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author Kingstar
 * @since  1.0
 */
public class PropertiesReader {
	private Properties prop;
	
	public PropertiesReader() {}

	public PropertiesReader(String fileName) {
		try {
			if (!fileName.trim().startsWith("/")) fileName = "/" + fileName.trim();
//			if (fileName!=null && !fileName.trim().startsWith(File.separator)) fileName = File.separator + fileName.trim();
			prop = new Properties();
			InputStream in = PropertiesReader.class.getResourceAsStream(fileName);
			prop.load(in);
		} catch (IOException | NullPointerException e) {
			Logger.warn("  In PropertiesReader not found the file :"+fileName+"  .  " + e.getMessage());
		    //不需要抛出异常,适合有则执行,没有则忽略的情况.
		}
	}
	
	/**
	 * 使用指定路径的文件进行配置.
	 * @param filePathAndName
	 * @param custom
	 * @since 1.9.8
	 */
	public PropertiesReader(String filePathAndName,boolean custom) {  //custom TODO
		try {
			prop = new Properties();
			InputStream in = new FileInputStream(new File(filePathAndName));
			prop.load(in);
		} catch (IOException | NullPointerException e) {
			Logger.warn("  In PropertiesReader not found the file :"+filePathAndName+"  .  " + e.getMessage());
			throw new ConfigWrongException("filePathAndName: "+filePathAndName+" config wrong!  "+ e.getMessage());
		}
	}
	

	/**
	 * @param key
	 * @return value,如果不存在,则返回null对象
	 */
	public String getValue(String key) {
		return prop.getProperty(key);
	}

	/**
	 * @param key
	 * @return value,如果不存在,则返回空值""
	 */
	public String getValueText(String key) {
		return prop.getProperty(key, "");
	}

	public String getValue(String key, String defaultValue) {
		return prop.getProperty(key, defaultValue);
	}

	public Set<String> getKeys() {
		return prop.stringPropertyNames();
	}
}

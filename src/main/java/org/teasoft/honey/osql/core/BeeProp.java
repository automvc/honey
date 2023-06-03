package org.teasoft.honey.osql.core;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import org.teasoft.bee.osql.Properties;
import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * Bee Properties
 * @author Kingstar
 * @since  1.0
 */
public class BeeProp implements Properties{

	private static PropertiesReader beePropReader;
	private static BeeProp beeProp = null;

	static {
		beePropReader = new PropertiesReader("bee.properties");
		beeProp=new BeeProp();
	}
	
	private BeeProp() {}
	
	public static BeeProp getBeeProp() {
		return beeProp;
	}

	public String getProp(String key) {
		return beePropReader.getValue(key);
	}

	public  String getPropText(String key) {
		return beePropReader.getValueText(key);
	}
	
	public Set<String> getKeys() {
		return beePropReader.getKeys();
	}
	
	/**
	 * 使用指定路径的bee.properties进行配置.set the folder path of bee.properties
	 * @param folderPath bee.properties所在的路径. the folder path of bee.properties
	 * @since 1.9.8
	 */
	static void resetBeeProperties(String folderPath) {
		if (!folderPath.trim().endsWith(File.separator)) folderPath = folderPath.trim()+File.separator;
		folderPath+="bee.properties";
		beePropReader = new PropertiesReader(folderPath, true);
	}
	
	/**
	 * 以InputStream的形式指定配置文件
	 * @since 1.17
	 */
	static void resetBeeProperties(InputStream inputStream) {
		beePropReader = new PropertiesReader(inputStream);
	}
}

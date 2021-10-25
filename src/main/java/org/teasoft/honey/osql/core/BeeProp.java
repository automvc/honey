package org.teasoft.honey.osql.core;

import java.io.File;

import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * @author Kingstar
 * @since  1.0
 */
class BeeProp {

	private static PropertiesReader beeProp;

	static {
		beeProp = new PropertiesReader("/bee.properties");
	}

	public static String getBeeProp(String key) {
		return beeProp.getValue(key);
	}

	public static String getBeePropText(String key) {
		return beeProp.getValueText(key);
	}
	
	/**
	 * 使用指定路径的bee.properties进行配置.
	 * @param filePath bee.properties所在的路径
	 */
	static void resetBeeProperties(String filePath) {
		if (!filePath.trim().endsWith(File.separator)) filePath = filePath.trim()+File.separator;
		filePath+="bee.properties";
		beeProp = new PropertiesReader(filePath, true);
	}
}

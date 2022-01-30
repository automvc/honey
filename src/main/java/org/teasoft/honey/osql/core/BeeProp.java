package org.teasoft.honey.osql.core;

import java.io.File;

import org.teasoft.bee.osql.Properties;
import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * @author Kingstar
 * @since  1.0
 */
public class BeeProp implements Properties{

	private static PropertiesReader beePropReader;
	private static BeeProp beeProp = null;

	static {
		beePropReader = new PropertiesReader("/bee.properties");
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
	
	/**
	 * 使用指定路径的bee.properties进行配置.
	 * @param filePath bee.properties所在的路径
	 * @since 1.9.8
	 */
	static void resetBeeProperties(String filePath) {
		if (!filePath.trim().endsWith(File.separator)) filePath = filePath.trim()+File.separator;
		filePath+="bee.properties";
		beePropReader = new PropertiesReader(filePath, true);
	}
}

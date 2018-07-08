package org.honey.osql.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * @author KingStar
 * @since  1.0
 */
public  class PropertiesReader {
	private  Properties prop;
	
	public PropertiesReader(String fileName){
		try {
			if(!fileName.trim().startsWith("/")) fileName="/"+fileName.trim();
			prop = new Properties();
			InputStream in = PropertiesReader.class.getResourceAsStream(fileName);
			prop.load(in);
		} catch (IOException e) {
			System.err.println("=========PropertiesReadUtil====  "+ e.getMessage());
		}
	}
	/**
	 * @param key
	 * @return value,如果不存在,则返回null对象
	 */
	public  String getValue(String key) {
		return prop.getProperty(key);
	}
	
	/**
	 * @param key
	 * @return value,如果不存在,则返回空值""
	 */
	public  String getValueText(String key) {
		return prop.getProperty(key,"");
	}
	
	public  String getValue(String key,String defaultValue) {
		return prop.getProperty(key,defaultValue);
	}
	
	public Set<String> getKeys(){
		return prop.stringPropertyNames();
	}
}

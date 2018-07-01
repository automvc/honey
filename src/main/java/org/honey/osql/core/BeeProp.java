package org.honey.osql.core;

import org.honey.osql.util.PropertiesReader;

/**
 * @author KingStar
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
}

/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.Set;

import org.teasoft.bee.osql.Properties;
import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * @author Kingstar
 * @since  2.1.8
 */
public class BootApplicationProp implements Properties{
	
	private static final String SPRING_DOT = "spring.";
	public static final String DATASOURCE_DRIVER_CLASS_NAME2 = SPRING_DOT+ "datasource.driver-class-name";
	public static final String DATASOURCE_DRIVER_CLASS_NAME = SPRING_DOT+ "datasource.driverClassName";
	public static final String DATASOURCE_PW = SPRING_DOT + "datasource.pass"+"word";
	public static final String DATASOURCE_USERNAME = SPRING_DOT + "datasource.username";
	public static final String DATASOURCE_URL = SPRING_DOT + "datasource.url";

	private PropertiesReader bootPropReader;
	
	public BootApplicationProp() {
		bootPropReader = new PropertiesReader("application.properties");
	}
	
	public String getProp(String key) {
		return bootPropReader.getValue(key);
	}

	public  String getPropText(String key) {
		return bootPropReader.getValueText(key);
	}
	
	public Set<String> getKeys() {
		return bootPropReader.getKeys();
	}
}

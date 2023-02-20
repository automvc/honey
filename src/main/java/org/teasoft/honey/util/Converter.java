/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.Map;
import java.util.Properties;

/**
 * @author Kingstar
 * @since  2.1
 */
public class Converter {

	public static Properties map2Prop(Map<String, String> map) {
		if (map == null) return null;

		Properties p = new Properties();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			p.setProperty(entry.getKey(), entry.getValue());
		}
		return p;
	}

}

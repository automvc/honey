/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.Properties;
import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * @author Kingstar
 * @since 1.11
 */
class BeeActiveProp implements Properties {
	PropertiesReader beePropReader = null;

	BeeActiveProp(String fileName) {
		beePropReader = new PropertiesReader(fileName);
	}

	public String getProp(String key) {
		return beePropReader.getValue(key);
	}

	public String getPropText(String key) {
		return beePropReader.getValueText(key);
	}

}

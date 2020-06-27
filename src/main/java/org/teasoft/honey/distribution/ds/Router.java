/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution.ds;

import org.teasoft.bee.distribution.ds.Route;
import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * @author Kingstar
 * @since  1.7.3
 */
public class Router {

	private static Route route = null;

	private static int multiDsType;
	private static String defaultDs;

	static {
		multiDsType = HoneyConfig.getHoneyConfig().multiDsType;
		defaultDs = HoneyConfig.getHoneyConfig().multiDsDefalutDS;

		if (multiDsType == 1) {
			route = new RwDs();

		} else if (multiDsType == 2) {
			route = new OnlyMulitiDB();
		}
	}

	public static String getDsName() {

		if (route == null) return defaultDs;

		return route.getDsName();
	}

	public static void refresh() {
		if (multiDsType == 1) {
			route = new RwDs();
		}
	}

}

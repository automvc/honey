/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution.ds;

import org.teasoft.bee.distribution.ds.Route;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;

/**
 * @author Kingstar
 * @since  1.8
 */
public class Router {

	private static Route route = null;

	private static int multiDsType;
	private static String defaultDs;

	static {
		init();
	}
	
	private static void init(){
		multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
		defaultDs = HoneyConfig.getHoneyConfig().multiDS_defalutDS;

		if (multiDsType == 1) {
			route = new RwDs();
		} else if (multiDsType == 2) {
			route = new OnlyMulitiDB();
		}
	}

	public static String getDsName() {
		if (HoneyContext.isConfigRefresh()) {
			refresh();
			HoneyContext.setConfigRefresh(false);
		}
		if (route == null) return defaultDs;

		return route.getDsName();
	}

	public static void refresh() {
		init();  //refresh all model
	}

}

/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding;

import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.StringConst;

/**
 * 强制修改路由,只在当次操作有效.
 * @author AiTeaSoft
 * @since  2.0
 */
public class HintManager {
	
	public static void setDataSourceName(String dsName) {
		HoneyContext.setAppointDS(dsName);
		regHint();
	}
	
	public static void setTableName(String tableName) {
		HoneyContext.setAppointTab(tableName);
		regHint();
	}
	
	private static void regHint() {
		HoneyContext.setSysCommStrLocal(StringConst.HintDsTab, StringConst.tRue);
	}

}

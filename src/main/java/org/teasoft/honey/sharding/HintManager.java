/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding;

import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.StringConst;

/**
 * 强制指定当次操作的路由.
 * @author AiTeaSoft
 * @since  2.0
 */
public class HintManager {

	public static void setDataSourceName(String dsName) {
		HoneyContext.setAppointDS(dsName);
		HoneyContext.setSysCommStrInheritableLocal(StringConst.HintDs, StringConst.tRue);
	}

	/**
	 * 强制当次操作使用的表名;只设置tableName,框架会先通过反查确定ds
	 * @param tableName
	 */
	public static void setTableName(String tableName) {
		HoneyContext.setAppointTab(tableName);
		HoneyContext.setSysCommStrInheritableLocal(StringConst.HintTab, StringConst.tRue);
	}

}

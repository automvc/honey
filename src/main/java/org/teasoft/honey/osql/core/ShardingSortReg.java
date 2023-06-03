/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.sql.ResultSetMetaData;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingSortReg {

	public static void regSort(ResultSetMetaData rmeta) {
		TransformResultSet.regSort(rmeta);
	}

}

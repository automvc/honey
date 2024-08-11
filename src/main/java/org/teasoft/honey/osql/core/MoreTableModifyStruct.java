/*
 * Copyright 2016-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;

/**
 * @author Kingstar
 * for update
 * @since  2.1.8
 * for update/insert/delete
 * @since  2.4.0
 */
class MoreTableModifyStruct {

	Field subField[];
	boolean subIsList[];  //子属性是否是List类型

	String ref[][];
	String foreignKey[][];

	boolean oneHasOne;
}

/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;

/**
 * @author Kingstar
 * @since  2.1.8
 */
class MoreTableInsertStruct {

	Field subField[];
	boolean subIsList[];  //子属性是否是List类型

	String ref[][];
	String foreignKey[][];

	boolean oneHasOne;
}

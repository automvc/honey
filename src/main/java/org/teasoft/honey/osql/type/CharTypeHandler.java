/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.type;

import org.teasoft.bee.osql.TypeHandler;

/**
 * raw char type handler.
 * Javabean field type is char.
 * @author Kingstar
 * @since  1.11
 */
public class CharTypeHandler<T> implements TypeHandler<Character> {

	@Override
	public Character process(Class<Character> fieldType, Object obj) {
		if (obj == null) return ' ';
		return obj.toString().charAt(0);
	}

}

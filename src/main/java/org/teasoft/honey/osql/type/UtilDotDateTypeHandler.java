/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.type;

import java.util.Date;

import org.teasoft.bee.osql.type.TypeHandler;

/**
*java.sql.Date transform to java.util.Date 
*
 * @author Kingstar
 * @since  1.11
 */
public class UtilDotDateTypeHandler<T> implements TypeHandler<Date> {

	@Override
	public Date process(Class<Date> fieldType, Object obj) {
		

		return null;
	}

}

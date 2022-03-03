/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.type;

import java.sql.Timestamp;
import java.util.Date;

import org.teasoft.bee.osql.type.SetParaTypeConvert;

/**
 * convert java.util.Date to Timestamp.
 * Oracle will use preferentially.
 * @author Kingstar
 * @since  1.11
 */
public class UtilDotDateTypeToTimestampConvert<T> implements SetParaTypeConvert<Date> {

	@Override
	public Object convert(Date value) {
		return new Timestamp(value.getTime());
	}

}

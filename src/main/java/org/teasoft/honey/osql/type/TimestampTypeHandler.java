/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.type;

import java.sql.Timestamp;

import org.teasoft.bee.osql.type.TypeHandler;
import org.teasoft.honey.osql.util.DateUtil;

/**
 * Long of timestamp or date String tansform Timestamp
 * @author Kingstar
 * @since  1.11
 */
public class TimestampTypeHandler<T> implements TypeHandler<Timestamp> {

	/**
	 * the dateFormat use :bee.osql.dateFormat.
	 * can define custom TypeHandler for other dateFormat.
	 */
	@Override
	public Timestamp process(Class<Timestamp> fieldType, Object result) {

		if (result == null) return null;
		if (result.getClass().equals(String.class)) {
			try {
				Long timeNum = Long.parseLong((String) result); //存的是数字
				return new Timestamp(timeNum);
			} catch (NumberFormatException e) {
			}
			return DateUtil.toTimestamp((String) result);
		}
		return (Timestamp) result;
	}

}

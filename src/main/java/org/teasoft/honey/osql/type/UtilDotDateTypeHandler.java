/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.type;

import java.sql.Timestamp;
import java.util.Date;

import org.teasoft.bee.osql.type.TypeHandler;
import org.teasoft.honey.osql.util.DateUtil;

/**
* transform to java.util.Date 
*
 * @author Kingstar
 * @since  1.11
 */
public class UtilDotDateTypeHandler<T> implements TypeHandler<Date> {

	@Override
	public Date process(Class<Date> fieldType, Object result) {

		if (result == null) return null;

		if (result.getClass().equals(java.sql.Date.class)) {
			try {
				java.util.Date d = new java.util.Date(((java.sql.Date) result).getTime());
				return d;
			} catch (Exception e) {
			}
		}

		Timestamp t = null;
		if (result.getClass().equals(Timestamp.class)) {
			try {
				t = (Timestamp) result;
				return getDate(t);
			} catch (Exception e) {
			}
		}

		if (result.getClass().equals(Long.class)) {
			try {
				t = new Timestamp((Long) result);
				return getDate(t);
			} catch (Exception e) {
			}
		}

		if (result.getClass().equals(String.class)) {
			try {
				Long timeNum = Long.parseLong((String) result); // 存的是数字
				t = new Timestamp(timeNum);
				return getDate(t);
			} catch (Exception e) {
			}

			try {
				t = DateUtil.toTimestamp((String) result);
				return getDate(t);
			} catch (Exception e) {

			}
		}
		return (java.util.Date) result;
	}

	private java.util.Date getDate(Timestamp t) {
		java.util.Date d = new java.util.Date(t.getTime());
		return d;
	}

}

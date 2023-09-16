/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept.annotation;

import java.lang.reflect.Field;
import java.sql.Timestamp;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.Createtime;
import org.teasoft.bee.osql.annotation.Datetime;
import org.teasoft.bee.osql.annotation.Updatetime;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.util.DateUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.11-E
 */
public class DatetimeHandler {
	
	private DatetimeHandler() {}

	public static void process(Field field, Object entity, SuidType suidType) {
		Datetime datetime = field.getAnnotation(Datetime.class);

		String formatter = datetime.formatter();
		boolean override = datetime.override();
		SuidType setSuidType = datetime.suidType();
		//		String value=datetime.value();

		process(field, entity, suidType, formatter, override, setSuidType);
	}

	public static void processCreatetime(Field field, Object entity, SuidType suidType) {
		Createtime datetime = field.getAnnotation(Createtime.class);

		String formatter = datetime.formatter();
		boolean override = datetime.override();
		SuidType setSuidType = SuidType.INSERT;

		process(field, entity, suidType, formatter, override, setSuidType);
	}

	public static void processUpdatetime(Field field, Object entity, SuidType suidType) {
		Updatetime datetime = field.getAnnotation(Updatetime.class);

		String formatter = datetime.formatter();
		boolean override = datetime.override();
		SuidType setSuidType = SuidType.UPDATE;

		process(field, entity, suidType, formatter, override, setSuidType);
	}

	private static void process(Field field, Object entity, SuidType sqlSuidType, String formatter,
			boolean override, SuidType setSuidType) {

		try {
			if (!(setSuidType == sqlSuidType || setSuidType == SuidType.SUID
					|| (setSuidType == SuidType.MODIFY && (sqlSuidType == SuidType.UPDATE
							|| sqlSuidType == SuidType.INSERT || sqlSuidType == SuidType.DELETE))))
				return; //操作类型不对,则返回
			
//			field.setAccessible(true);
			HoneyUtil.setAccessibleTrue(field);
			if (!override) { //不允许覆盖,原来有值则返回
				if (field.get(entity) != null) return;
			}

//			if (field.getType() == Timestamp.class) {
			if(field.getType().equals(Timestamp.class)) {
				HoneyUtil.setFieldValue(field, entity, DateUtil.currentTimestamp());
			}else if(field.getType().equals(java.sql.Date.class)) {
				HoneyUtil.setFieldValue(field, entity, DateUtil.currentSqlDate());
			}else if(field.getType().equals(java.util.Date.class)) {
				HoneyUtil.setFieldValue(field, entity, new java.util.Date());
			}else {
				if (StringUtils.isNotBlank(formatter))
					HoneyUtil.setFieldValue(field, entity, DateUtil.currentDate(formatter));
				else
					HoneyUtil.setFieldValue(field, entity, DateUtil.currentDate());
			}
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}

	}

}

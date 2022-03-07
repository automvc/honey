/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.lang.reflect.Field;

import org.teasoft.bee.osql.annotation.Column;
import org.teasoft.bee.osql.annotation.Createtime;
import org.teasoft.bee.osql.annotation.Datetime;
import org.teasoft.bee.osql.annotation.Dict;
import org.teasoft.bee.osql.annotation.ReplaceInto;
import org.teasoft.bee.osql.annotation.Updatetime;
import org.teasoft.bee.osql.annotation.customizable.AutoSetString;
import org.teasoft.bee.osql.annotation.customizable.Desensitize;
import org.teasoft.bee.osql.annotation.customizable.DictI18n;
import org.teasoft.bee.osql.annotation.customizable.Json;
import org.teasoft.bee.osql.annotation.customizable.MultiTenancy;

/**
 * @author Kingstar
 * @since  1.11
 */
public class AnnoUtil {
	
	private AnnoUtil() {}

	public static boolean isDatetime(Field field) {

		return field.isAnnotationPresent(Datetime.class);
	}

	public static boolean isCreatetime(Field field) {
		return field.isAnnotationPresent(Createtime.class);
	}

	public static boolean isUpdatetime(Field field) {
		return field.isAnnotationPresent(Updatetime.class);
	}

	public static boolean isAutoSetString(Field field) {
		return field.isAnnotationPresent(AutoSetString.class);
	}

	public static boolean isDesensitize(Field field) {
		return field.isAnnotationPresent(Desensitize.class);
	}

	public static boolean isMultiTenancy(Field field) {
		return field.isAnnotationPresent(MultiTenancy.class);
	}

	public static boolean isDict(Field field) {
		return field.isAnnotationPresent(Dict.class);
	}

	public static boolean isDictI18n(Field field) {
		return field.isAnnotationPresent(DictI18n.class);
	}

	public static boolean isReplaceInto(Object entity) {
		return entity.getClass().isAnnotationPresent(ReplaceInto.class);
	}
	
	public static boolean isColumn(Field field) {
		return field.isAnnotationPresent(Column.class);
	}
	
	public static boolean isJson(Field field) {
		return field.isAnnotationPresent(Json.class);
	}

}

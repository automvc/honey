/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.lang.reflect.Field;

import org.teasoft.bee.osql.annotation.Createtime;
import org.teasoft.bee.osql.annotation.Datetime;
import org.teasoft.bee.osql.annotation.Updatetime;

/**
 * @author Kingstar
 * @since  1.11
 */
public class AnnoUtil {
	
	public static boolean isDatetime(Field field) {
		if (field.isAnnotationPresent(Datetime.class)) return true;
		else return false;
	}
	
	public static boolean isCreatetime(Field field) {
		if (field.isAnnotationPresent(Createtime.class)) return true;
		else return false;
	}
	
	public static boolean isUpdatetime(Field field) {
		if (field.isAnnotationPresent(Updatetime.class)) return true;
		else return false;
	}
	
}

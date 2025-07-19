/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;

/**
 * @author Kingstar
 * @since  2.5.2
 */
public class CoreUtil {
	
	public static void setFieldValue(Field field, Object targetObj, Object value) throws IllegalAccessException {
		field.set(targetObj, value); // NOSONAR
	}

	public static void setAccessibleTrue(Field field) {
		field.setAccessible(true); // NOSONAR
	}

}

/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.teasoft.bee.osql.annotation.Createtime;
import org.teasoft.bee.osql.annotation.Datetime;
import org.teasoft.bee.osql.annotation.Dict;
import org.teasoft.bee.osql.annotation.GenId;
import org.teasoft.bee.osql.annotation.GenUUID;
import org.teasoft.bee.osql.annotation.ReplaceInto;
import org.teasoft.bee.osql.annotation.Sharding;
import org.teasoft.bee.osql.annotation.Updatetime;
import org.teasoft.bee.osql.annotation.customizable.AutoSetString;
import org.teasoft.bee.osql.annotation.customizable.Desensitize;
import org.teasoft.bee.osql.annotation.customizable.DictI18n;
import org.teasoft.bee.osql.annotation.customizable.Json;
import org.teasoft.bee.osql.annotation.customizable.MultiTenancy;
import org.teasoft.bee.spi.AnnoAdapter;
import org.teasoft.bee.spi.defaultimpl.AnnoAdapterBeeDefault;
import org.teasoft.honey.osql.core.DefaultColumnHandler;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.name.NameRegistry;

/**
 * @author Kingstar
 * @since  1.11
 */
public class AnnoUtil {

	private static AnnoAdapter annoAdapter;

	static {
		initAnnoAdapterInstance();
		NameRegistry.registerColumnHandler(new DefaultColumnHandler()); //字段自定义命名转换
	}

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

	public static boolean isMultiTenancyAnno(Field field) {
		return field.isAnnotationPresent(MultiTenancy.class);
	}
	
	public static boolean isShardingAnno(Field field) {
		return field.isAnnotationPresent(Sharding.class);
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

	public static boolean isJson(Field field) {
		return field.isAnnotationPresent(Json.class);
	}

	public static boolean isGenPkAnno(Field field) {
		return field.isAnnotationPresent(GenId.class)
				|| field.isAnnotationPresent(GenUUID.class);
	}
	
	
	//----------support SPI-------start--<<<<<<<-
	public static boolean isColumn(Field field) {
//		return field.isAnnotationPresent(Column.class);
		return annoAdapter.isColumn(field);
	}

	public static boolean isTable(Class<?> clazz) {
		return annoAdapter.isTable(clazz);
	}

	public static boolean isPrimaryKey(Field field) {
		return annoAdapter.isPrimaryKey(field);
	}
	
	public static boolean isIgnore(Field field) {
		return annoAdapter.isIgnore(field);
	}
	

	public static String getValue(Field field) {
		return annoAdapter.getValue(field);
	}

	public static String getValue(Class<?> clazz) {
		return annoAdapter.getValue(clazz);
	}

	private static void initAnnoAdapterInstance() {
		ServiceLoader<AnnoAdapter> annoAdapterLoader = ServiceLoader.load(AnnoAdapter.class);
		Iterator<AnnoAdapter> annoIterator = annoAdapterLoader.iterator();

		if (annoIterator.hasNext()) {
			try {
				annoAdapter = annoIterator.next();
			} catch (ServiceConfigurationError e) {
				Logger.error(e.getMessage(), e);
				initAnnoAdapterInstance2();
			}
		} else {
			initAnnoAdapterInstance2();
		}
	}

	private static void initAnnoAdapterInstance2() {
		try {
			Class.forName("javax.persistence.Table"); //check
		} catch (Exception e) {
//			Logger.debug(e.getMessage(), e);
			// maybe donot add the bee-ext.
			annoAdapter = new AnnoAdapterBeeDefault();
			return ;
		}
		
	try {
			annoAdapter = (AnnoAdapter) Class.forName("org.teasoft.beex.spi.AnnoAdapterDefault").newInstance();
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
			// maybe donot add the bee-ext.
			annoAdapter = new AnnoAdapterBeeDefault();
		}
	}
	
	//----------support SPI-------end-->>>>>>>>-

}

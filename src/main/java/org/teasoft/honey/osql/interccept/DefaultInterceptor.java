/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept;

import java.lang.reflect.Field;
import java.util.List;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.interccept.Interceptor;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.interccept.annotation.DatetimeHandler;
import org.teasoft.honey.osql.util.AnnoUtil;

/**
 * @author Kingstar
 * @since  1.11
 */
public class DefaultInterceptor implements Interceptor {

	private String ds;
	private String tabName;
	private String tabSuffix;

	@Override
	public Object beforePasreEntity(Object entity, SuidType suidType) {

//		if(entity==null) return entity;  //自定义sql,MapSuid会用到.  放在chain

		if (entity.getClass().equals(Class.class)) {
//			.println("是Class类型,默认不处理."); //deleteById
			return entity;
		}

		Boolean f = HoneyContext.getEntityInterceptorFlag(entity.getClass().getName());
		if (Boolean.FALSE.equals(f)) return entity;

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		boolean isHas = false;
		for (int i = 0; i < len; i++) {
			if (AnnoUtil.isDatetime(fields[i])) {
				if (f == null && !isHas) isHas = true;
				DatetimeHandler.process(fields[i], entity, suidType);
			} else if (AnnoUtil.isCreatetime(fields[i])) {
				if (f == null && !isHas) isHas = true;
				DatetimeHandler.processCreatetime(fields[i], entity, suidType);
			} else if (AnnoUtil.isUpdatetime(fields[i])) {
				if (f == null && !isHas) isHas = true;
				DatetimeHandler.processUpdatetime(fields[i], entity, suidType);
			}
		}

		if (f == null) // 原来为null,还没设置的,会进行初次设置
			HoneyContext.addEntityInterceptorFlag(entity.getClass().getName(), isHas);

		return entity;
	}

	@Override
	public Object[] beforePasreEntity(Object[] entityArray, SuidType suidType) {

		for (int i = 0; i < entityArray.length; i++) {
			beforePasreEntity(entityArray[i], suidType);
		}

		return entityArray;
	}

	@Override
	public void setDataSourceOneTime(String ds) {
		this.ds = ds;
	}

	@Override
	public String getOneTimeDataSource() {
		return ds;
	}

	@Override
	public void setTabNameOneTime(String tabName) {
		this.tabName = tabName;
	}

	@Override
	public void setTabSuffixOneTime(String tabSuffix) {
		this.tabSuffix = tabSuffix;
	}

	@Override
	public String getOneTimeTabName() {
		return tabName;
	}

	@Override
	public String getOneTimeTabSuffix() {
		return tabSuffix;
	}

	@Override
	public String afterCompleteSql(String sql) {
		// NOTICE:if change the sql,need update the context.
		return sql;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void beforeReturn(List list) {

	}

	@Override
	public void beforeReturn() {}

}

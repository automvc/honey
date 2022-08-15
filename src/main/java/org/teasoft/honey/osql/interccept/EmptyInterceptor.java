/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept;

import java.util.List;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.interccept.Interceptor;

/**
 * @author Kingstar
 * @since  1.11
 */
public class EmptyInterceptor implements Interceptor {

	protected String ds;
	protected String tabName;
	protected String tabSuffix;
	
	private static final long serialVersionUID = 1595293159216L;

	protected boolean isSkip(Object entity) {
		if (entity == null) return true; //自定义sql会用到

		if (entity.getClass().equals(Class.class)) { //是Class类型,默认不处理. //deleteById
			return true;
		}

		//		Boolean f = HoneyContext.getEntityInterceptorFlag(entity.getClass().getName());
		//		if (f == Boolean.FALSE) return true;   与默认检测的注解不一样

		return false;
	}

	@Override
	public Object beforePasreEntity(Object entity, SuidType suidType) {
		return entity;
	}

	@Override
	public Object[] beforePasreEntity(Object[] entityArray, SuidType suidType) {
		return entityArray;
	}

	@Override
	public String afterCompleteSql(String sql) {
		return sql;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void beforeReturn(List list) {
	   //empty
	}

	@Override
	public void beforeReturn() {
       //empty
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

}

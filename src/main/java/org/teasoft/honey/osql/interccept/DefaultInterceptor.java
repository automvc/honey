/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept;

import java.util.List;

import org.teasoft.bee.osql.interccept.Interceptor;

/**
 * @author Kingstar
 * @since  1.11
 */
public class DefaultInterceptor implements Interceptor{

	private String ds;
	
	@Override
	public Object beforePasreEntity(Object entity) {
		return entity;
	}
	
	@Override
	public void setDataSourceOneTime(String ds) {
		this.ds=ds;
	}
	
	@Override
	public String getOneTimeDataSource() {
		return ds;
	}

	@Override
	public String afterCompleteSql(String sql) {
		return sql;
	}

	@Override
	public void afterAccessDB(List list) {
	}

	@Override
	public void afterAccessDB(Object entity) {
	}
	
	@Override
	public void afterAccessDB() {
	}

}

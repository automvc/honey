/*
 * Copyright 2016-2022 the original author.All rights reserved.
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
		System.out.println("beforePasreEntity---------------------------");
		return entity;
	}
	
	@Override
	public void setDataSourceOneTime(String ds) {
		this.ds=ds;
		System.out.println("--------------------------------ds:"+ds);
	}
	
	@Override
	public String getOneTimeDataSource() {
		return ds;
	}

	@Override
	public String afterCompleteSql(String sql) {
		System.out.println("afterCompleteSql---------------------------");
		return sql;
	}

	@Override
	public void afterAccessDB(List list) {
		System.out.println("afterQueryResult(List list)---------------------------");
		
	}
	
	@Override
	public void afterAccessDB() {
		System.out.println("afterAccessDB()---------------------------");
	}

}

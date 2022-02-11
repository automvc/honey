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
public class DefaultInterceptor implements Interceptor{

	private String ds;
	
	@Override
	public Object beforePasreEntity(Object entity,SuidType suidType) {
		Boolean f=HoneyContext.getEntityInterceptorFlag(entity.getClass().getName());
		if(f==Boolean.FALSE) return entity;
		
		Field fields[] = entity.getClass().getDeclaredFields(); 
		int len = fields.length;
		boolean isHas=false;
		for (int i = 0; i < len; i++) {
			fields[i].setAccessible(true);
			if (AnnoUtil.isDatetime(fields[i])) {
				if(f==null && !isHas) isHas=true;
				DatetimeHandler.process(fields[i], entity,suidType);
			}else if(AnnoUtil.isCreatetime(fields[i])) {
				if(f==null && !isHas) isHas=true;
				DatetimeHandler.processCreatetime(fields[i], entity, suidType);
			}else if(AnnoUtil.isUpdatetime(fields[i])) {
				if(f==null && !isHas) isHas=true;
				DatetimeHandler.processUpdatetime(fields[i], entity, suidType);
			}
		}
		
		if(f==null)
		  HoneyContext.addEntityInterceptorFlag(entity.getClass().getName(), isHas);
		
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
	@SuppressWarnings("rawtypes")
	public void beforeReturn(List list) {
		
	}
	
	@Override
	public void beforeReturn() {
	}

}

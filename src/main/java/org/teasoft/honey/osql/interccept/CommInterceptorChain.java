/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept;

import java.util.ArrayList;
import java.util.List;

import org.teasoft.bee.osql.interccept.Interceptor;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.11
 */
public class CommInterceptorChain implements InterceptorChain {

	private final List<Interceptor> chain = new ArrayList<>();

	public void addInterceptor(Interceptor interceptor) {  //如何以配置的形式出现?? 参考:DefaultInterceptorChain
		chain.add(interceptor);
	}

	@Override
	public Object beforePasreEntity(Object entity) {

		for (int i = 0; i < chain.size(); i++) {
			chain.get(i).beforePasreEntity(entity);
		}
		
		doResetDataSourceOneTime();

		return entity;
	}
	
	@Override
	public void setDataSourceOneTime(String ds) {
		//do nothing
	}
	
	@Override
	public String getOneTimeDataSource() {
		//do nothing
		return null;
	}
	
	private void doResetDataSourceOneTime() {
		
		int count=0;
		String ds;
		for (int i = 0; i < chain.size(); i++) {
			
			ds=chain.get(i).getOneTimeDataSource();
			
			if(StringUtils.isNotBlank(ds)) {
				count++;
				HoneyContext.setAppointDS(ds);
				Logger.info("[Bee] Reset the DataSource OneTime, ds name:"+ds);
			}
		}
		if(count>1) Logger.warn("[Bee] Just the last DataSource is effective,if set the OneTime DataSource more than one!");
	}

	@Override
	public String afterCompleteSql(String sql) {
		for (int i = 0; i < chain.size(); i++) {
			sql=chain.get(i).afterCompleteSql(sql);
		}
//		HoneyContext.removeAppointDS();  //放在这影响缓存
		return sql;
	}

	@Override
	public void afterAccessDB(List list) {
		HoneyContext.removeAppointDS();  //放在这可能影响异步.  更新操作,不执行这方法
		for (int i = 0; i < chain.size(); i++) {
			chain.get(i).afterAccessDB(list);
		}
	}
	
	//用于update,insert,delete
	@Override
	public void afterAccessDB() {
		HoneyContext.removeAppointDS(); 
	}
	
}

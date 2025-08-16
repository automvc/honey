/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.teasoft.bee.osql.SuidType;
//import org.teasoft.bee.osql.annotation.customizable.MultiTenancy;
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

	private static final long serialVersionUID = 1595293159213L;

	private final List<Interceptor> chain = new ArrayList<>();
	private final Set<Class<?>> set = new HashSet<>();

	public void addInterceptor(Interceptor interceptor) {
		if (!set.add(interceptor.getClass())) Logger
				.warn("The InterceptorChain already contain the Interceptor, type: " + interceptor.getClass().getName());
		chain.add(interceptor);
	}

	@Override
	public Object beforePasreEntity(Object entity, SuidType suidType) {
		for (int i = 0; entity != null && i < chain.size(); i++) { // 自定义sql,MapSuid会传入null
			chain.get(i).beforePasreEntity(entity, suidType);
		}

		doResetDataSourceOneTime();

		return entity;
	}

	@Override
	public Object[] beforePasreEntity(Object[] entityArray, SuidType suidType) {
		for (int i = 0; entityArray != null && i < chain.size(); i++) {
			chain.get(i).beforePasreEntity(entityArray, suidType);
		}
		doResetDataSourceOneTime();

		return entityArray;
	}

	@Override
	public void setDataSourceOneTime(String ds) {
		// do nothing
	}

	@Override
	public String getOneTimeDataSource() {
		// do nothing
		return null;
	}

	private void doResetDataSourceOneTime() {

		int count = 0;
		int countTab = 0;
		int countTabSuffix = 0;
		String ds, tabName, tabSuffix;
		for (int i = 0; i < chain.size(); i++) {
			ds = chain.get(i).getOneTimeDataSource();
			tabName = chain.get(i).getOneTimeTabName();
			tabSuffix = chain.get(i).getOneTimeTabSuffix();

			if (StringUtils.isNotBlank(ds)) {
				count++;
				HoneyContext.setAppointDS(ds); // 拦截器里获取的, 而拦截器则是从@MultiTenancy等获取到.
				Logger.info("[Bee] Reset the DataSource OneTime, ds name:" + ds);
			}

			if (StringUtils.isNotBlank(tabName)) {
				countTab++;
				HoneyContext.setAppointTab(tabName);
				Logger.info("[Bee] Reset the tabName OneTime, tabName:" + tabName);
			}

			if (StringUtils.isNotBlank(tabSuffix)) {
				countTabSuffix++;
				HoneyContext.setTabSuffix(tabSuffix);
				Logger.info("[Bee] Reset the tabName OneTime, tabSuffix:" + tabSuffix);
			}

		}
		if (count > 1)
			Logger.warn("[Bee] Just the last DataSource is effective,if set the OneTime DataSource more than one!");

		if (countTab > 1)
			Logger.warn("[Bee] Just the last tabName is effective,if set the OneTime tabName more than one!");

		if (countTabSuffix > 1)
			Logger.warn("[Bee] Just the last TabSuffix is effective,if set the OneTime TabSuffix more than one!");

	}

	@Override
	public String afterCompleteSql(String sql) {
		for (int i = 0; i < chain.size(); i++) {
			sql = chain.get(i).afterCompleteSql(sql);
		}
		// HoneyContext.removeAppointDS(); //放在这影响缓存
		return sql;
	}

	// 用于有返回Javabean结构的查询
	@Override
	@SuppressWarnings("rawtypes")
	public void beforeReturn(List list) {
		for (int i = 0; i < chain.size(); i++) {
			chain.get(i).beforeReturn(list);
		}
		_remove();
	}

	// 用于update,insert,delete及没有返回Javabean结构的查询方法
	@Override
	public void beforeReturn() {

		for (int i = 0; i < chain.size(); i++) {
			chain.get(i).beforeReturn();
		}
		_remove();
	}

	private void _remove() {
		HoneyContext.removeAppointDS(); // 放在这可能影响异步.
		HoneyContext.removeCurrentRoute();
		HoneyContext.removeAppointTab(); // V1.17
		HoneyContext.removeTabSuffix();// V1.17
	}

	@Override
	public void setTabNameOneTime(String tabName) {
		// do nothing
	}

	@Override
	public void setTabSuffixOneTime(String tabSuffix) {
		// do nothing
	}

	@Override
	public String getOneTimeTabName() {
		// do nothing
		return null;
	}

	@Override
	public String getOneTimeTabSuffix() {
		// do nothing
		return null;
	}

}

/*
 * Copyright 2020-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */
package org.teasoft.honey.osql.core;

import java.util.List;
import java.util.Map;

import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.bee.osql.MongodbPreparedSql;
import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.interccept.InterceptorChain;

/**
 * @author Kingstar
 * @since  2.1
 */
public class MongodbPreparedSqlLib implements MongodbPreparedSql {

	private MongodbBeeSql mongodbBeeSql;
	private InterceptorChain interceptorChain;
	private String dsName;// 用于设置当前对象使用的数据源名称
	private NameTranslate nameTranslate; // 用于设置当前对象使用的命名转换器.使用默认的不需要设置

	@Override
	public <T> List<T> select(String commandStr, Class<T> returnTypeClass) {
		doBeforePasreEntity(returnTypeClass, SuidType.SELECT);// returnType的值,虽然不用作占位参数的值,但可以用作拦截器的业务逻辑判断
		commandStr = doAfterCompleteSql(commandStr);
		Logger.logSQL("MongodbPreparedSql select SQL: ", commandStr);
		List<T> list = getMongodbBeeSql().select(commandStr, returnTypeClass);
		doBeforeReturn(list);
		return list;
	}

	@Override
	public String selectJson(String sql) {
		doBeforePasreEntity();
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("MongodbPreparedSql selectJson SQL: ", sql);
		String r = getMongodbBeeSql().selectJson(sql);
		doBeforeReturn();
		return r;
	}

	@Override
	public int modify(String sql) {

		doBeforePasreEntity();
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("MongodbPreparedSql modify SQL: ", sql);
		int r = getMongodbBeeSql().modify(sql);
		doBeforeReturn();
		return r;
	}

	@Override
	public List<Map<String, Object>> selectMapList(String sql) {

		doBeforePasreEntity();
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("MongodbPreparedSql selectMapList SQL: ", sql);
		List<Map<String, Object>> list = getMongodbBeeSql().selectMapList(sql);
		doBeforeReturn();
		return list;
	}

	public MongodbBeeSql getMongodbBeeSql() {
		if (mongodbBeeSql == null) return BeeFactory.getHoneyFactory().getMongodbBeeSql();
		return mongodbBeeSql;
	}

	public void setMongodbBeeSql(MongodbBeeSql mongodbBeeSql) {
		this.mongodbBeeSql = mongodbBeeSql;
	}

	@Override
	public InterceptorChain getInterceptorChain() {
		if (interceptorChain == null) return BeeFactory.getHoneyFactory().getInterceptorChain();
		return HoneyUtil.copy(interceptorChain);
	}

	public void setInterceptorChain(InterceptorChain interceptorChain) {
		this.interceptorChain = interceptorChain;
	}

	@Override
	public void setDataSourceName(String dsName) {
		this.dsName = dsName;
	}

	@Override
	public String getDataSourceName() {
		return dsName;
	}

	@Override
	public void setNameTranslate(NameTranslate nameTranslate) {
		this.nameTranslate = nameTranslate;
	}

	private void doBeforePasreEntity() {
		if (this.dsName != null) HoneyContext.setTempDS(dsName);
		if (this.nameTranslate != null) HoneyContext.setCurrentNameTranslate(nameTranslate);
		getInterceptorChain().beforePasreEntity(null, SuidType.SELECT);
	}

	private void doBeforePasreEntity(Object entity, SuidType suidType) {// 都是select在用
		if (this.dsName != null) HoneyContext.setTempDS(dsName);
		if (this.nameTranslate != null) HoneyContext.setCurrentNameTranslate(nameTranslate);
		getInterceptorChain().beforePasreEntity(entity, suidType);
	}

	private String doAfterCompleteSql(String sql) {
		// if change the sql,need update the context.
		sql = getInterceptorChain().afterCompleteSql(sql);
		return sql;
	}

	@SuppressWarnings("rawtypes")
	private void doBeforeReturn(List list) {
		if (this.dsName != null) HoneyContext.removeTempDS();
		if (this.nameTranslate != null) HoneyContext.removeCurrentNameTranslate();
		getInterceptorChain().beforeReturn(list);
	}

	private void doBeforeReturn() {
		if (this.dsName != null) HoneyContext.removeTempDS();
		if (this.nameTranslate != null) HoneyContext.removeCurrentNameTranslate();
		getInterceptorChain().beforeReturn();
	}

}

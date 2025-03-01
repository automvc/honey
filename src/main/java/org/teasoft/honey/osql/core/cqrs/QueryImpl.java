/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core.cqrs;

import java.util.List;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.bee.osql.api.cqrs.Query;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.honey.osql.core.BeeFactory;

/**
 * @author Kingstar
 * @since  2.5.2
 */
public class QueryImpl implements Query {

	private SuidRich suidRich;

	public void setSuidRich(SuidRich suidRich) {
		this.suidRich = suidRich;
	}

	public SuidRich getSuidRich() {
		if (suidRich == null) return BeeFactory.getHoneyFactory().getSuidRich();
		return suidRich;
	}

	@Override
	public <T> List<T> select(T entity) {
		return getSuidRich().select(entity);
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		return getSuidRich().select(entity, condition);
	}

	@Override
	public <T> List<T> select(T entity, int size) {
		return getSuidRich().select(entity, size);
	}

	@Override
	public <T> List<T> select(T entity, int start, int size) {
		return getSuidRich().select(entity, start, size);
	}

	@Override
	public <T> List<T> select(T entity, String... selectFields) {
		return getSuidRich().select(entity, selectFields);
	}

	@Override
	public <T> List<T> select(T entity, int start, int size, String... selectFields) {
		return getSuidRich().select(entity, start, size, selectFields);
	}

	@Override
	public <T> List<String[]> selectString(T entity) {
		return getSuidRich().selectString(entity);
	}

	@Override
	public <T> List<String[]> selectString(T entity, String... selectFields) {
		return getSuidRich().selectString(entity, selectFields);
	}

	@Override
	public <T> List<String[]> selectString(T entity, Condition condition) {
		return getSuidRich().selectString(entity, condition);
	}

	@Override
	public <T> String selectJson(T entity) {
		return getSuidRich().selectJson(entity);
	}

	@Override
	public <T> T selectOne(T entity) {
		return getSuidRich().selectOne(entity);
	}

	@Override
	public <T> T selectFirst(T entity, Condition condition) {
		return getSuidRich().selectFirst(entity, condition);
	}

	@Override
	public <T> String selectWithFun(T entity, FunctionType functionType, String fieldForFun) {
		return getSuidRich().selectWithFun(entity, functionType, fieldForFun);
	}

	@Override
	public <T> String selectWithFun(T entity, FunctionType functionType, String fieldForFun, Condition condition) {
		return getSuidRich().selectWithFun(entity, functionType, fieldForFun, condition);
	}

	@Override
	public <T> int count(T entity) {
		return getSuidRich().count(entity);
	}

	@Override
	public <T> int count(T entity, Condition condition) {
		return getSuidRich().count(entity, condition);
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFields) {
		return getSuidRich().selectOrderBy(entity, orderFields);
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFields, OrderType[] orderTypes) {
		return getSuidRich().selectOrderBy(entity, orderFields, orderTypes);
	}
	
	@Override
	public <T> String selectJson(T entity, Condition condition) {
		return getSuidRich().selectJson(entity, condition);
	}

	@Override
	public <T> String selectJson(T entity, String... selectFields) {
		return getSuidRich().selectJson(entity, selectFields);
	}

	@Override
	public <T> String selectJson(T entity, int start, int size, String... selectFields) {
		return getSuidRich().selectJson(entity, start, size, selectFields);
	}

	@Override
	public <T> T selectById(Class<T> entityClass, Integer id) {
		return getSuidRich().selectById(entityClass, id);
	}

	@Override
	public <T> T selectById(Class<T> entityClazz, Long id) {
		return getSuidRich().selectById(entityClazz, id);
	}

	@Override
	public <T> T selectById(Class<T> entityClass, String id) {
		return getSuidRich().selectById(entityClass, id);
	}

	@Override
	public <T> List<T> selectByIds(Class<T> entityClass, String ids) {
		return getSuidRich().selectByIds(entityClass, ids);
	}

	@Override
	public <T> boolean exist(T entity) {
		return getSuidRich().exist(entity);
	}

	// ---------------common methods--------------
//	@Override
//	public Query setDynamicParameter(String para, String value) {
//		// Add the business logic if need.
//		getSuidRich().setDynamicParameter(para, value);
//		return this;
//	}

	@Override
	public void beginSameConnection() {
		// Add the business logic if need.
		getSuidRich().beginSameConnection();
	}

	@Override
	public void endSameConnection() {
		// Add the business logic if need.
		getSuidRich().endSameConnection();
	}

	@Override
	public void setDataSourceName(String dsName) {
		// Add the business logic if need.
		getSuidRich().setDataSourceName(dsName);
	}

	@Override
	public String getDataSourceName() {
		// Add the business logic if need.
		return getSuidRich().getDataSourceName();
	}

	@Override
	public InterceptorChain getInterceptorChain() {
		// Add the business logic if need.
		return getSuidRich().getInterceptorChain();
	}

	@Override
	public void setNameTranslateOneTime(NameTranslate nameTranslate) {
		// Add the business logic if need.
		getSuidRich().setNameTranslateOneTime(nameTranslate);
	}

}

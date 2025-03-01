/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core.cqrs;

import java.util.List;

import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.bee.osql.api.cqrs.Command;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.honey.osql.core.BeeFactory;

/**
 * @author Kingstar
 * @since  2.5.2
 */
public class CommandImpl implements Command {

	private SuidRich suidRich;

	public void setSuidRich(SuidRich suidRich) {
		this.suidRich = suidRich;
	}

	public SuidRich getSuidRich() {
		if (suidRich == null) return BeeFactory.getHoneyFactory().getSuidRich();
		return suidRich;
	}

	@Override
	public <T> int update(T entity) {
		return getSuidRich().update(entity);
	}

	@Override
	public <T> int insert(T entity) {
		return getSuidRich().insert(entity);
	}

	@Override
	public <T> long insertAndReturnId(T entity) {
		return getSuidRich().insertAndReturnId(entity);
	}

	@Override
	public <T> int delete(T entity) {
		return getSuidRich().delete(entity);
	}

	@Override
	public <T> int delete(T entity, Condition condition) {
		return getSuidRich().delete(entity, condition);
	}

	@Override
	public <T> int update(T entity, String... updateFields) {
		return getSuidRich().update(entity, updateFields);
	}

	@Override
	public <T> int update(T entity, IncludeType includeType, String... updateFields) {
		return getSuidRich().update(entity, includeType, updateFields);
	}

	@Override
	public <T> int insert(T[] entity) {
		return getSuidRich().insert(entity);
	}

	@Override
	public <T> int insert(T[] entity, int batchSize) {
		return getSuidRich().insert(entity, batchSize);
	}

	@Override
	public <T> int insert(T[] entity, String excludeFields) {
		return getSuidRich().insert(entity, excludeFields);
	}

	@Override
	public <T> int insert(T[] entity, int batchSize, String excludeFields) {
		return getSuidRich().insert(entity, batchSize, excludeFields);
	}

	@Override
	public <T> int insert(List<T> entityList) {
		return getSuidRich().insert(entityList);
	}

	@Override
	public <T> int insert(List<T> entityList, int batchSize) {
		return getSuidRich().insert(entityList, batchSize);
	}

	@Override
	public <T> int insert(List<T> entityList, String excludeFields) {
		return getSuidRich().insert(entityList, excludeFields);
	}

	@Override
	public <T> int insert(List<T> entityList, int batchSize, String excludeFields) {
		return getSuidRich().insert(entityList, batchSize, excludeFields);
	}

	@Override
	public <T> int update(T entity, IncludeType includeType) {
		return getSuidRich().update(entity, includeType);
	}

	@Override
	public <T> int insert(T entity, IncludeType includeType) {
		return getSuidRich().insert(entity, includeType);
	}

	@Override
	public <T> long insertAndReturnId(T entity, IncludeType includeType) {
		return getSuidRich().insertAndReturnId(entity, includeType);
	}

	@Override
	public int deleteById(Class<?> c, Integer id) {
		return getSuidRich().deleteById(c, id);
	}

	@Override
	public int deleteById(Class<?> c, Long id) {
		return getSuidRich().deleteById(c, id);
	}

	@Override
	public int deleteById(Class<?> c, String ids) {
		return getSuidRich().deleteById(c, ids);
	}

	@Override
	public <T> int updateBy(T entity, String... whereFields) {
		return getSuidRich().updateBy(entity, whereFields);
	}

	@Override
	public <T> int updateBy(T entity, IncludeType includeType, String... whereFields) {
		return getSuidRich().updateBy(entity, includeType, whereFields);
	}

	@Override
	public <T> int updateBy(T entity, Condition condition, String... whereFields) {
		return getSuidRich().updateBy(entity, condition, whereFields);
	}

	@Override
	public <T> int updateById(T entity, Condition condition) {
		return getSuidRich().updateById(entity, condition);
	}

	@Override
	public <T> int update(T entity, Condition condition, String... updateFields) {
		return getSuidRich().update(entity, condition, updateFields);
	}

	@Override
	public <T> int update(T entity, Condition condition) {
		return getSuidRich().update(entity, condition);
	}

	@Override
	public <T> int update(T oldEntity, T newEntity) {
		return getSuidRich().update(oldEntity, newEntity);
	}

	@Override
	public <T> int save(T entity) {
		return getSuidRich().save(entity);
	}

	@Override
	public <T> boolean createTable(Class<T> entityClass, boolean isDropExistTable) {
		return getSuidRich().createTable(entityClass, isDropExistTable);
	}

	@Override
	public <T> void indexNormal(Class<T> entityClass, String fields, String indexName) {
		getSuidRich().indexNormal(entityClass, fields, indexName);
	}

	@Override
	public <T> void unique(Class<T> entityClass, String fields, String indexName) {
		getSuidRich().unique(entityClass, fields, indexName);
	}

	@Override
	public <T> void primaryKey(Class<T> entityClass, String fields, String keyName) {
		getSuidRich().primaryKey(entityClass, fields, keyName);
	}

	@Override
	public <T> void dropIndex(Class<T> entityClass, String indexName) {
		getSuidRich().dropIndex(entityClass, indexName);
	}

	// ---------------common methods--------------
	private SuidRich localSuidRichForDynamicParameter = null;

//	@Override
//	public Command setDynamicParameter(String para, String value) {
//		if (localSuidRichForDynamicParameter == null) {
//			localSuidRichForDynamicParameter = getSuidRich();
//		}
////		getSuidRich().setDynamicParameter(para, value); // TODO getSuidRich() 在重复设置多个时,是否会有问题?
//		localSuidRichForDynamicParameter.setDynamicParameter(para, value); //可能和操作对象设置的不是同一个getSuidRich()对象
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

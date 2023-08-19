/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.List;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.MoreObjToSQL;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.MoreTable;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.honey.osql.util.AnnoUtil;

/**
 * 多表查询,MoreTable实现类.Multi table query, moretable implementation class.
 * @author Kingstar
 * @since  1.7
 * More table insert
 * @since  2.1.8
 */
public class MoreObjSQL extends AbstractCommOperate implements MoreTable {

	private BeeSql beeSql;
	private MoreObjToSQL moreObjToSQL;

	private static final String SELECT_SQL = "select SQL: ";

	@Override
	public <T> List<T> select(T entity) {
		if (entity == null) return null;
		doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
		String sql = getMoreObjToSQL().toSelectSQL(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().moreTableSelect(sql, entity);
		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> select(T entity, int start, int size) {
		if (entity == null) return null;
		if (size <= 0) throw new BeeIllegalParameterException(StringConst.SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(StringConst.START_GREAT_EQ_0);
		doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
		String sql = getMoreObjToSQL().toSelectSQL(entity, start, size);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().moreTableSelect(sql, entity);
		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;
		regCondition(condition);
		doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
		String sql = getMoreObjToSQL().toSelectSQL(entity, condition);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().moreTableSelect(sql, entity);
		doBeforeReturn(list);
		return list;
	}

	@Override
	public MoreObjSQL setDynamicParameter(String para, String value) {
		OneTimeParameter.setAttribute(para, value);
		return this;
	}

	private void doBeforePasreEntity(Object entity) {
		super.doBeforePasreEntity(entity, SuidType.SELECT);
		OneTimeParameter.setAttribute(StringConst.InterceptorChainForMoreTable, getInterceptorChain());// 用于子表
	}

	public BeeSql getBeeSql() {
		if (this.beeSql == null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

	public MoreObjToSQL getMoreObjToSQL() {
		if (moreObjToSQL == null) return BeeFactory.getHoneyFactory().getMoreObjToSQL();
		return moreObjToSQL;
	}

	public void setMoreObjToSQL(MoreObjToSQL moreObjToSQL) {
		this.moreObjToSQL = moreObjToSQL;
	}

	
	
	// -------------------more---insert------V2.1.8------------------
	//OneToOne Table1->Table2
	//最多支持三个表(子表中又有子表)关联插入: Table1->Table2->Table3 ;此种,表2不支持List的形式
	//OneToMany: Table1-> List<Table2>
	
	//** 先拆解实体,再调用SuidRich;   而不是像多表查询,要解析成sql,再调用SqlLib.

	private SuidRich suidRich;
	
	public SuidRich getSuidRich() {
		if (this.suidRich == null) suidRich = BeeFactory.getHoneyFactory().getSuidRich();
		return suidRich;
	}

	public void setSuidRich(SuidRich suidRich) {
		this.suidRich = suidRich;
	}
	

	@Override
	public <T> int insert(T entity) {
		// 是否需要事务？？ 由上一层负责
		long returnId = getSuidRich().insertAndReturnId(entity);
		if (returnId <= 0) return (int) returnId;
		
		MoreTableInsertStruct struct = MoreInsertUtils._getMoreTableInsertStruct(entity);

		if (struct != null) {
			int len = struct.subField.length;
			try {
				for (int k = 0; k < len; k++) {
					if (k == 0 && struct.oneHasOne) {
						OneHasOne t = moreInsert(struct, k, returnId, entity);
						if (t != null) {
							k++;
							moreInsert(struct, 1, t.returnId1, t.subEntity);
						}
					} else { //不是OneHasOne(子表又有子表) 的情形
						moreInsert(struct, k, returnId, entity);
					}
				}
			} catch (IllegalAccessException e) {
				throw ExceptionHelper.convert(e);
			} catch (NoSuchFieldException e) {
				throw ExceptionHelper.convert(e);
			}
//			T t; //传入的实体参数；
//			List list2= (List) field[i].get(t); //获取到实体t的 List 属性
//		         问题是,实体t的 List 属性有一个属性是bookId,
//		         如何给它设置值
		}
		
		return 1;
	}

	private OneHasOne moreInsert(MoreTableInsertStruct struct, int i, long returnId,
			Object currentEntity) throws IllegalAccessException, NoSuchFieldException {
		if (struct.subIsList[i]) {
			struct.subField[i].setAccessible(true);
			List listSubI = (List) struct.subField[i].get(currentEntity);
			// 设置外键的值
			for (Object item : listSubI) {
				for (int propIndex = 0; propIndex < struct.foreignKey[i].length; propIndex++) {
					Field fkField = item.getClass().getDeclaredField(struct.foreignKey[i][propIndex]);
					setPkField(struct, i, returnId, currentEntity, item, fkField, propIndex);
				}
				
//				fkField.setAccessible(true); 
//				fkField.set(item, returnId); 
			}
			getSuidRich().insert(listSubI);
		} else { // 单个实体
			struct.subField[i].setAccessible(true);
			Object subEntity = struct.subField[i].get(currentEntity);
			if (subEntity == null) return null;
			for (int propIndex = 0; propIndex < struct.foreignKey[i].length; propIndex++) {
				Field f = subEntity.getClass().getDeclaredField(struct.foreignKey[i][propIndex]);
				// eg: 同步 id,name
				setPkField(struct, i, returnId, currentEntity, subEntity, f, propIndex);
			}

			// 如果是id,或使用了主键注解，才使用返回值。
			// 如何不是，要从主表中获取相应字段的值； 或该字段为blank，则报错
			
//			struct.ref[i]; //这个不是主键值的话,  要使用实体的
			
			

			
//			boolean useReturnId=false;
//			if ("id".equalsIgnoreCase(struct.ref[i])) {
//				useReturnId=true;
//			}else {
//				Field k = subEntity.getClass().getDeclaredField(struct.ref[i]);
//				if (AnnoUtil.isPrimaryKey(k)) {
//					useReturnId = true;
//				} else {
//					f.setAccessible(true);
//					f.set(subEntity, f.get(currentEntity));
//				}
//			}
//
//			if (useReturnId) {
//				f.setAccessible(true);
//				f.set(subEntity, returnId);
//			}

			long returnId1 = getSuidRich().insertAndReturnId(subEntity); // OneHasOne 这里要将返回值存起

			if (i == 0 && struct.oneHasOne) {
				OneHasOne t = new OneHasOne();
				t.returnId1 = returnId1;
				t.subEntity = subEntity;
				return t;
			}
		}
		return null;
	}
	
	private void setPkField(MoreTableInsertStruct struct, int i, long returnId,
			Object currentEntity, Object subEntity, Field fkField,int propIndex)
			throws IllegalAccessException, NoSuchFieldException {
		boolean useReturnId = false;
		if ("id".equalsIgnoreCase(struct.ref[i][propIndex])) {
			useReturnId = true;
		} else {
			Field refField = currentEntity.getClass().getDeclaredField(struct.ref[i][propIndex]); //获取 被引用的字段
			if (AnnoUtil.isPrimaryKey(refField)) {
				useReturnId = true;
			} else {
				fkField.setAccessible(true);
				refField.setAccessible(true);
				fkField.set(subEntity, refField.get(currentEntity));
			}
		}

		if (useReturnId) {
			fkField.setAccessible(true);
			fkField.set(subEntity, returnId);
		}
	}

	private class OneHasOne {
		long returnId1;
		Object subEntity;
	}

}

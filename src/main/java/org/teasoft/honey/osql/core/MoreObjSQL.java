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
		return modify(entity,SuidType.INSERT);
	}
	
	
	@Override
	public <T> int update(T entity) {
		return modify(entity,SuidType.UPDATE);
	}

	@Override
	public <T> int delete(T entity) {
		return modify(entity,SuidType.DELETE);
	}

	private <T> int modify(T entity,SuidType suidType) {	
		// 是否需要事务？？ 由上一层负责
		long returnId=modifyOneEntity(entity, suidType);
		
		if (returnId <= 0) return (int) returnId;   //等于0时, 子表是否还要处理??   不需要,既然是关联操作,父表都没有操作到,则无关联可言
		
		MoreTableInsertStruct struct = MoreInsertUtils._getMoreTableInsertStruct(entity);

		if (struct != null) {
			int len = struct.subField.length;
			try {
				for (int k = 0; k < len; k++) {
					if (k == 0 && struct.oneHasOne) {
						OneHasOne t = moreInsert(struct, k, returnId, entity, suidType);
						if (t != null) {
							k++;
							moreInsert(struct, 1, t.returnId1, t.subEntity, suidType);
						}else {
							if(SuidType.DELETE==suidType || SuidType.UPDATE==suidType) k++; //若是这两种类型,当第一个子表没有设置外键值时,子表的子表将不再处理
						} 
					} else { // 不是OneHasOne(子表又有子表) 的情形
						moreInsert(struct, k, returnId, entity, suidType);
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
		if (SuidType.INSERT == suidType)
			return 1;   //主表只插入一行
		else
			return (int)returnId; //主表受影响的行数
	}
	
	private OneHasOne moreInsert(MoreTableInsertStruct struct, int i, long returnId,
			Object currentEntity,SuidType suidType) throws IllegalAccessException, NoSuchFieldException {
		if (struct.subIsList[i]) {
			struct.subField[i].setAccessible(true);
			List listSubI = (List) struct.subField[i].get(currentEntity);
			boolean setFlag=false;
			// 设置外键的值
			for (Object item : listSubI) {
				for (int propIndex = 0; propIndex < struct.foreignKey[i].length; propIndex++) {
					Field fkField = item.getClass().getDeclaredField(struct.foreignKey[i][propIndex]);
					
					if (SuidType.INSERT == suidType)
						setFlag=setPkField(struct, i, returnId, currentEntity, item, fkField, propIndex);
					else
						setFlag=setPkField2(struct,i, returnId, currentEntity, item, fkField, propIndex);
				}
//				fkField.setAccessible(true); 
//				fkField.set(item, returnId); 
				if(setFlag) {
					if(SuidType.DELETE==suidType)  getSuidRich().delete(listSubI.get(i));
					else if(SuidType.UPDATE==suidType) getSuidRich().update(listSubI.get(i));
				}
			}
			
			if (SuidType.INSERT == suidType) getSuidRich().insert(listSubI);
//			modifyListSubEntity(listSubI, suidType);
		} else { // 单个实体
			if (struct.subField[i] == null) return null;
			struct.subField[i].setAccessible(true);
			Object subEntity = struct.subField[i].get(currentEntity);
			if (subEntity == null) return null;
			for (int propIndex = 0; propIndex < struct.foreignKey[i].length; propIndex++) {
				Field f = subEntity.getClass().getDeclaredField(struct.foreignKey[i][propIndex]);
				boolean setFlag;
				// eg: 同步 id,name
				if (SuidType.INSERT == suidType)
					setFlag = setPkField(struct, i, returnId, currentEntity, subEntity, f, propIndex);
				else
					setFlag = setPkField2(struct,i, returnId, currentEntity, subEntity, f, propIndex);
				
				//update,delete,如果子实体没有用上FK声明的字段则不执行,防止更新到多余记录
				if(!setFlag && SuidType.INSERT != suidType) return null;  
			}

			// 如果是id,或使用了主键注解，才使用返回值。
			// 如何不是，要从主表中获取相应字段的值； 或该字段为blank，则报错
//			struct.ref[i]; //这个不是主键值的话,  要使用实体的
			
			
			long returnId1=modifyOneEntity(subEntity, suidType);
//			long returnId1 = getSuidRich().insertAndReturnId(subEntity); // OneHasOne 这里要将返回值存起
			if (i == 0 && struct.oneHasOne) {
				OneHasOne t = new OneHasOne();
				t.returnId1 = returnId1;
				t.subEntity = subEntity;
				return t;
			}
		}
		return null;
	}
	
	private boolean setPkField(MoreTableInsertStruct struct, int i, long returnId,
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
		
		return true;
	}
	
	// Update,Delete no return id
	//setPkFieldForUpdateOrDelete
	//如果子实体没有用上FK声明的字段则不执行,防止更新到多余记录
	private boolean setPkField2(MoreTableInsertStruct struct, int i, long returnId,
			Object currentEntity, Object subEntity, Field fkField, int propIndex)
			throws IllegalAccessException, NoSuchFieldException {

		Field refField = currentEntity.getClass().getDeclaredField(struct.ref[i][propIndex]); // 获取 被引用的字段

		fkField.setAccessible(true);
		refField.setAccessible(true);
		Object v = refField.get(currentEntity);
		if (v == null) {
//			Field refField2 = subEntity.getClass().getDeclaredField(struct.foreignKey[i][propIndex]); //子表的外键字段
			Object v2 = fkField.get(subEntity);// 子表的外键字段
			if (v2 == null) return false; // 父表没有设置, 子表也没有设置才返回null
			// 若设置了id,name; 其实name没有值,也是可以的.如何处理??? todo
		} else {
			fkField.set(subEntity, v);
		}

		return true;
	}
	
	private <T> long modifyOneEntity(T entity,SuidType suidType) {
		long returnId=0;
		if (SuidType.INSERT == suidType)
			returnId = getSuidRich().insertAndReturnId(entity);
		else if (SuidType.UPDATE == suidType)
			returnId = getSuidRich().update(entity);  //todo 是否需要加codition?
		else if (SuidType.DELETE == suidType)
			returnId = getSuidRich().delete(entity);
		
		return  returnId;
	}
	
	
//	private void modifyListSubEntity(List listSubI, SuidType suidType) {
//
//		if (SuidType.INSERT == suidType) {
//			getSuidRich().insert(listSubI);
//		} else if (SuidType.UPDATE == suidType) {
//			for (int i = 0; listSubI != null && i < listSubI.size(); i++) {
//				getSuidRich().update(listSubI.get(i));
//			}
//		} else if (SuidType.DELETE == suidType) {
//			for (int i = 0; listSubI != null && i < listSubI.size(); i++) {
//				getSuidRich().delete(listSubI.get(i));
//			}
//		}
//	}
		

	private class OneHasOne {
		long returnId1;
		Object subEntity;
	}

}

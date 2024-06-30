/*
 * Copyright 2016-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.MoreObjToSQL;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.MoreTable;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.honey.osql.core.ConditionImpl.FunExpress;
import org.teasoft.honey.osql.shortcut.BF;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.util.ObjectUtils;

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
	private static final String SELECT_JSON_SQL = "selectJson SQL: ";

	@Override
	public <T> List<T> select(T entity) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
			String sql = getMoreObjToSQL().toSelectSQL(entity);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL(SELECT_SQL, sql);
			list = getBeeSql().moreTableSelect(sql, entity);
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> List<T> select(T entity, int start, int size) {
		if (entity == null) return null;
		if (size <= 0) throw new BeeIllegalParameterException(StringConst.SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(StringConst.START_GREAT_EQ_0);
		List<T> list = null;
		try {
			doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
			String sql = getMoreObjToSQL().toSelectSQL(entity, start, size);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL(SELECT_SQL, sql);
			list = getBeeSql().moreTableSelect(sql, entity);
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			regCondition(condition);
			doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
			String sql = getMoreObjToSQL().toSelectSQL(entity, condition);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL(SELECT_SQL, sql);
			list = getBeeSql().moreTableSelect(sql, entity);
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}
	
	@Override
	public <T> String selectWithFun(T entity, Condition condition) {

		if (entity == null) return null;
		String fun = null;
		try {
			ConditionImpl conditionImpl = (ConditionImpl) condition;
			List<FunExpress> funExpList = conditionImpl.getFunExpList();
			if (ObjectUtils.isEmpty(funExpList)) {
				throw new BeeIllegalSQLException("In selectWithFun, the aggregation function can not be empty!");
			}
			if (funExpList.size()>1) {
				throw new BeeIllegalSQLException("In selectWithFun, just support one aggregation function!");
			}

			regCondition(condition);
			doBeforePasreEntity(entity);// 因要解析子表,子表下放再执行
			_regFunType(getFunctionType(funExpList.get(0).getFunctionType())); // test?
			String sql = getMoreObjToSQL().toSelectSQL(entity, condition);
			_regEntityClass1(entity);
			sql = doAfterCompleteSql(sql);
			fun = getBeeSql().selectFun(sql);  //因是select max(id) from ...的形式,只会返回一个字段还可以多表的也共用
			Logger.logSQL(SELECT_SQL, sql);
		} finally {
			doBeforeReturn();
		}
		return fun;
	}
	
	private static FunctionType getFunctionType(String functionName) {
		for (FunctionType type : FunctionType.values()) {
			if (type.getName().equalsIgnoreCase(functionName)) {
				return type;
			}
		}
		return null;
	}

	// 没能将entity传到SqlLib,需要注册
	private <T> void _regEntityClass1(T entity) {
		if (entity == null) return;
		HoneyContext.regEntityClass(entity.getClass());
	}

	private <T> void _regFunType(FunctionType functionType) {
		HoneyContext.regFunType(functionType);
	}

	@Override
	public <T> int count(T entity) {
		return count(entity, null);
	}

	@Override
	public <T> int count(T entity, Condition condition) {
		Condition con;
		con = condition == null ? BF.getCondition() : condition;

		String total = selectWithFun(entity, con.selectFun(FunctionType.COUNT, "*"));
		return total == null ? 0 : Integer.parseInt(total);
	}

	@Override
	public <T> List<String[]> selectString(T entity, Condition condition) {
		if (entity == null) return null;
		List<String[]> list = null;
		try {
			regCondition(condition);
			doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
			OneTimeParameter.setTrueForKey(StringConst.Check_Group_ForSharding);
			String sql = getMoreObjToSQL().toSelectSQL(entity, condition);
			_regEntityClass1(entity);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL("select SQL(return List<String[]>): ", sql);
			list = getBeeSql().select(sql); // TODO 要测试分片时,是否合适
		} finally {
			doBeforeReturn();
		}
		return list;
	}
	
	
	@Override
	public <T> String selectJson(T entity, Condition condition) {
		if (entity == null) return null;
		String json = null;
		try {
			regCondition(condition);
			doBeforePasreEntity(entity, SuidType.SELECT);
			_regEntityClass1(entity);
			OneTimeParameter.setTrueForKey(StringConst.Check_Group_ForSharding); //TODO
			String sql = getMoreObjToSQL().toSelectSQL(entity, condition);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL(SELECT_JSON_SQL, sql);

			json = getBeeSql().selectJson(sql);
		} finally {
			doBeforeReturn();
		}
		return json;
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
	
	private <T> boolean checkTempKeyNullValue(T entity, String tempKey[]) {
		Field field = null;
		boolean nullKeyValue=true;
		try {
			for (int i = 0; i < tempKey.length; i++) {
				field = HoneyUtil.getField(entity.getClass(), tempKey[i]);

				Object obj = null;
				try {
					if (field != null) {
						HoneyUtil.setAccessibleTrue(field);
						obj = field.get(entity);
						if (obj == null) {
							Logger.warn("The " + field.getName() + " value is null!");
							nullKeyValue=nullKeyValue && true;
						}else {
							nullKeyValue=false;
						}
					}
				} catch (IllegalAccessException e) {
					throw ExceptionHelper.convert(e);
				}
			}
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}
		return nullKeyValue;
	}

	private <T> int modify(T entity,SuidType suidType) {	
		// 是否需要事务？？ 由上一层负责
		MoreTableModifyStruct struct =null;
		boolean hasParseEntity=false;
		boolean hasProcess=false;
		long returnId=0;
		if(suidType==SuidType.UPDATE) {
			Object idValeu=HoneyUtil.getIdValue(entity);
			
			if (idValeu == null) {
				struct = MoreInsertUtils._getMoreTableModifyStruct(entity); // UPDATE时,先执行,要用结构信息判断
				hasParseEntity = true;
				boolean nullKeyValue=false;
				if (struct.ref.length == 1) {
					nullKeyValue =checkTempKeyNullValue(entity, struct.ref[0]);
					returnId=getSuidRich().updateBy(entity, struct.ref[0]); // update by
					hasProcess = true;
				} else if (struct.ref.length == 2) {
					String tempKey[]=mergeArrays(struct.ref[0], struct.ref[1]);
					nullKeyValue=checkTempKeyNullValue(entity, tempKey);
					returnId=getSuidRich().updateBy(entity, tempKey); // update by
					hasProcess = true;
				}
				if(nullKeyValue) return (int)returnId; //无法关联子表操作,提前返回
			}
		}
		
		if(! hasProcess) returnId=modifyOneEntity(entity, suidType);
		
		if (returnId < 0 || (suidType!=SuidType.UPDATE && returnId==0)) return (int) returnId;   //等于0时, 子表是否还要处理??   不需要,既然是关联操作,父表都没有操作到,则无关联可言
		//update时,returnId==0,还需要试着更新子表  V2.4.0
		
		if(! hasParseEntity) struct = MoreInsertUtils._getMoreTableModifyStruct(entity);

		if (struct != null) {
			int len = struct.subField.length;
			try {
				for (int k = 0; k < len; k++) { //只能处理两重子表
					if (k == 0 && struct.oneHasOne) {
						OneHasOne t = moreSubModify(struct, k, returnId, entity, suidType);
						if (t != null) {
							k++; //oneHasOne 要加1
							moreSubModify(struct, 1, t.returnId1, t.subEntity, suidType);
						}else {
							if(SuidType.DELETE==suidType || SuidType.UPDATE==suidType) k++; //若是这两种类型,当第一个子表没有设置外键值时,子表的子表将不再处理
						} 
					} else { // not OneHasOne (不是子表又有子表) 的情形
						moreSubModify(struct, k, returnId, entity, suidType);
					}
				}
			} catch (IllegalAccessException e) {
				throw ExceptionHelper.convert(e);
			} catch (NoSuchFieldException e) {
				throw ExceptionHelper.convert(e);
			}
		}
		
		if (SuidType.INSERT == suidType)
			return 1;   //主表只插入一行
		else
			return (int)returnId; //主表受影响的行数
	}
	
	private OneHasOne moreSubModify(MoreTableModifyStruct struct, int i, long returnId,
			Object currentEntity,SuidType suidType) throws IllegalAccessException, NoSuchFieldException {
		if (struct.subIsList[i]) {
			HoneyUtil.setAccessibleTrue(struct.subField[i]);
			List listSubI = (List) struct.subField[i].get(currentEntity);
			boolean setFlag=false;
			// 设置外键的值
			for (Object item : listSubI) {
				for (int propIndex = 0; propIndex < struct.foreignKey[i].length; propIndex++) {
					Field fkField = HoneyUtil.getField(item.getClass(), struct.foreignKey[i][propIndex]);
					// 设置外键的值
					if (SuidType.INSERT == suidType)
						setFlag=setPkField(struct, i, returnId, currentEntity, item, fkField, propIndex);
					else
						setFlag=setPkField2(struct,i, returnId, currentEntity, item, fkField, propIndex);
				}
				if(setFlag) {
					if(SuidType.DELETE==suidType)  {
						getSuidRich().delete(listSubI.get(i));
					}else if(SuidType.UPDATE==suidType) {
						Object idValeu=HoneyUtil.getIdValue(listSubI.get(i));
						if(idValeu!=null) getSuidRich().update(listSubI.get(i)); //by id or primary key
						else getSuidRich().updateBy(listSubI.get(i), struct.foreignKey[i]); //update by V2.4.0
					}
				}
			}
			if (SuidType.INSERT == suidType) getSuidRich().insert(listSubI); //insert的,一次调用插入list即可
			
		} else { // 单个实体
			if (struct.subField[i] == null) return null;
			HoneyUtil.setAccessibleTrue(struct.subField[i]);
			Object subEntity = struct.subField[i].get(currentEntity);
			if (subEntity == null) return null;
			for (int propIndex = 0; propIndex < struct.foreignKey[i].length; propIndex++) {
				Field f = HoneyUtil.getField(subEntity.getClass(),struct.foreignKey[i][propIndex]);
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
			
			boolean needReturn=false;
			long returnId1;
			if (SuidType.UPDATE == suidType) {
				
				Object idValeu = HoneyUtil.getIdValue(subEntity); //by id or primary key
				if (idValeu != null) returnId1=getSuidRich().update(subEntity);
				else returnId1=getSuidRich().updateBy(subEntity, struct.foreignKey[i]); //update by V2.4.0
				if(returnId1>=0) needReturn=true;
			} else {
				// 从表要设置了关联信息才执行  上面已设置(设置外键的值)
				returnId1 = modifyOneEntity(subEntity, suidType); // OneHasOne 这里要将返回值存起
				if(returnId1>0) needReturn=true;
			}
			
			if (i == 0 && needReturn && struct.oneHasOne) {
				OneHasOne t = new OneHasOne();
				t.returnId1 = returnId1;
				t.subEntity = subEntity;
				return t;
			}
		}
		return null;
	}
	
	private boolean setPkField(MoreTableModifyStruct struct, int i, long returnId,
			Object currentEntity, Object subEntity, Field fkField,int propIndex)
			throws IllegalAccessException, NoSuchFieldException {
		boolean useReturnId = false;
		if ("id".equalsIgnoreCase(struct.ref[i][propIndex])) {
			useReturnId = true;
		} else {
			Field refField = HoneyUtil.getField(currentEntity.getClass(),struct.ref[i][propIndex]); //获取 被引用的字段
			if (AnnoUtil.isPrimaryKey(refField)) {
				useReturnId = true;
			} else {
				HoneyUtil.setAccessibleTrue(fkField);
				HoneyUtil.setAccessibleTrue(refField);
				HoneyUtil.setFieldValue(fkField, subEntity, refField.get(currentEntity));
			}
		}

		if (useReturnId) {
			HoneyUtil.setAccessibleTrue(fkField);
			HoneyUtil.setFieldValue(fkField, subEntity, returnId);
		}
		
		return true;
	}
	
	// Update,Delete no return id
	//setPkFieldForUpdateOrDelete
	//如果子实体没有用上FK声明的字段则不执行,防止更新到多余记录
	private boolean setPkField2(MoreTableModifyStruct struct, int i, long returnId,
			Object currentEntity, Object subEntity, Field fkField, int propIndex)
			throws IllegalAccessException, NoSuchFieldException {

		Field refField = HoneyUtil.getField(currentEntity.getClass(),struct.ref[i][propIndex]); // 获取 被引用的字段

		HoneyUtil.setAccessibleTrue(fkField);
		HoneyUtil.setAccessibleTrue(refField);
		Object v = refField.get(currentEntity);
		if (v == null) {
			Object v2 = fkField.get(subEntity);// 子表的外键字段
			if (v2 == null) return false; // 父表没有设置, 子表也没有设置才返回null
			// 若设置了id,name; 其实name没有值,也是可以的.如何处理??? todo
		} else {
			HoneyUtil.setFieldValue(fkField, subEntity, v);
		}

		return true;
	}
	
	private <T> long modifyOneEntity(T entity,SuidType suidType) {//子表是update时,不走这个分支
		long returnId=0;
		if (SuidType.INSERT == suidType) {
			returnId = getSuidRich().insertAndReturnId(entity);
		}else if (SuidType.UPDATE == suidType) {
			returnId = getSuidRich().update(entity); 
		}else if (SuidType.DELETE == suidType) {
			returnId = getSuidRich().delete(entity);
		}
		
		return  returnId;
	}
	
	private String[] mergeArrays(String[] array1, String[] array2) {
		if (array2 == null || array2.length == 0) return array1;

		Set<String> set = new LinkedHashSet<>();
		for (String s : array1) {
			set.add(s);
		}

		for (String s : array2) {
			set.add(s);
		}

		String r[] = new String[set.size()];
		int i = 0;
		for (String str : set) {
			r[i++] = str;
		}

		return r;
	}

	private class OneHasOne {
		long returnId1;
		Object subEntity;
	}

}

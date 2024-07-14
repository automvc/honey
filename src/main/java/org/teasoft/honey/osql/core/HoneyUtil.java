package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.Serializer;
import org.teasoft.bee.osql.annotation.GenId;
import org.teasoft.bee.osql.annotation.GenUUID;
import org.teasoft.bee.osql.annotation.JoinTable;
import org.teasoft.bee.osql.annotation.JoinType;
import org.teasoft.bee.osql.annotation.JustFetch;
import org.teasoft.bee.osql.annotation.customizable.Json;
import org.teasoft.bee.osql.exception.BeeErrorFieldException;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalEntityException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.bee.osql.exception.JoinTableException;
import org.teasoft.bee.osql.exception.JoinTableParameterException;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.bee.osql.type.SetParaTypeConvert;
import org.teasoft.bee.spi.SqlFormat;
import org.teasoft.honey.distribution.GenIdFactory;
import org.teasoft.honey.distribution.UUID;
import org.teasoft.honey.osql.constant.NullEmpty;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.type.*;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.spi.SpiInstanceRegister;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * Bee的实现Honey的工具类.Honey Util.
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyUtil {

	private static Map<String, Integer> javaTypeMap = new HashMap<>();

	static {
		initJavaTypeMap();
		initSetParaAndResultTypeHandlerRegistry();
	}

	static void refreshSetParaAndResultTypeHandlerRegistry() {
		initSetParaAndResultTypeHandlerRegistry();
	}
	
	private HoneyUtil() {}
	
	//初始化  SQL设置参数转换注册器 和 查询结果类型转换注册器
	private static void initSetParaAndResultTypeHandlerRegistry() {
		
		//V2.1.6 若设置了关闭,则不再进行默认设置
		boolean closeDefaultParaResultRegistry = HoneyConfig.getHoneyConfig().closeDefaultParaResultRegistry;
		if (closeDefaultParaResultRegistry) return;
		
		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeToTimestampConvert<java.util.Date>(), DatabaseConst.PostgreSQL);
		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeToTimestampConvert<java.util.Date>(), DatabaseConst.H2);
		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeToTimestampConvert<java.util.Date>(), DatabaseConst.SQLSERVER); // v2.1.8  
		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeToTimestampConvert<java.util.Date>(), DatabaseConst.ORACLE); // v2.1.8  
//		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeConvert<java.util.Date>()); //bug 会少了时分秒  v2.1.8  
		
		TypeHandlerRegistry.register(char.class, new CharTypeHandler<Character>(),true);
		
		//单DS  或者  DsMap中包含有   才执行.   触发时间,应该是在被更改配置时,调用一次
		if ((!HoneyConfig.getHoneyConfig().multiDS_enable)
		  || (HoneyContext.getDsName2DbName() != null && HoneyContext.getDsName2DbName().containsValue(DatabaseConst.SQLite))) {
			
		    TypeHandlerRegistry.register(Timestamp.class, new TimestampTypeHandler<Timestamp>(),DatabaseConst.SQLite,true);
			TypeHandlerRegistry.register(java.util.Date.class, new UtilDotDateTypeHandler<java.util.Date>(), DatabaseConst.SQLite,true);
			TypeHandlerRegistry.register(java.sql.Date.class, new SqlDotDateTypeHandler<java.util.Date>(), DatabaseConst.SQLite,true);
		}
	}

	@SuppressWarnings("rawtypes")
	static String getBeanField(Field field[],Class entityClass) {
		if (field == null) return "";
		StringBuffer s = new StringBuffer();
		int len = field.length;
		boolean isFirst = true;

		for (int i = 0; i < len; i++) {
			if(isSkipField(field[i])) continue;

			if(field[i].isAnnotationPresent(JustFetch.class)) {
				if(StringUtils.isBlank(getJustFetchDefineName(field[i]))) continue; //V2.1.8  只用@JustFetch()时,该字段不转换到select列表中,用于像count(*) as para变量的接收
				if (isFirst) {
					isFirst = false;
				} else {
					s.append(",");
				}
				s.append(getJustFetchColumn(field[i]));
			}else {
				if (isFirst) {
					isFirst = false;
				} else {
					s.append(",");
				}
			   s.append(NameTranslateHandle.toColumnName(field[i].getName(),entityClass));
			}
			
		}
		return s.toString();
	}
	
	private static String getJustFetchColumn(Field field) {
		
		String expression = getJustFetchDefineName(field);
		String c = "";
		String fName = NameTranslateHandle.toColumnName(field.getName());
		if (isSQLite()) {
			c = expression + " "+ K.as + " "+fName; //fixed bug V2.1.8
		} else {
			c = expression + " " + fName;
		}

		return c;
	}
	
	private static String getJustFetchDefineName(Field field) {
		JustFetch justFetch= field.getAnnotation(JustFetch.class);
		String expression=justFetch.value();
		
		checkExpression(expression);
		
		return expression;
	}
	
	private static void checkExpression(String expression){
		if(Check.isNotValidExpressionForJustFetch(expression)) {
			throw new BeeIllegalSQLException("The expression: '"+expression+ "' is invalid in JustFetch Annotation!");
		}
	}

	static <T> MoreTableStruct[] getMoreTableStructAndCheckBefore(T entity) {
		
		if (entity == null) return null;
		
		String packageAndClassName = entity.getClass().getName();
		String key = "ForMoreTable:" + packageAndClassName; //ForMoreTable  
		
//		String key = "ForMoreTable:" +tableName+":"+ packageAndClassName; //ForMoreTable  
		//是否会受多表有Table标签影响??? 不会. 是包名+类名.不是表名,也没有解析标签. 不管是否有标签,对应的Javabean结构都一样的.
		//但解析的查询字段(带表名时,因为注解的原因,可能不一样)不一样,所以不能混在一起
		//在sqlLib用后删除. 因从表的字段也有可能带表名(而该表名有动态参数解析.)
		
		MoreTableStruct moreTableStruct[] =null;
		
		if(OneTimeParameter.isTrue(StringConst.MoreStruct_to_SqlLib)) {
			moreTableStruct = _getMoreTableStructAndCheckBefore(entity);
			OneTimeParameter.setAttribute(key, moreTableStruct);
			
			if (moreTableStruct[1] == null) { //v1.9
				throw new BeeErrorGrammarException(
						"MoreTable select on " + entity.getClass().getName() + " must own at least one JoinTable annotation!");
			}
		}else {//供SqlLib,多表查询使用
			moreTableStruct=(MoreTableStruct[])OneTimeParameter.getAttribute(key);
		}

		return moreTableStruct;
	}
	
	@SuppressWarnings("rawtypes")
	private static <T> MoreTableStruct[] _getMoreTableStructAndCheckBefore(T entity) {

		if (entity == null) return null;

		String entityFullName = entity.getClass().getName();
		
//		Field field[] = entity.getClass().getDeclaredFields();
		Field field[] = HoneyUtil.getFields(entity.getClass());

		MoreTableStruct moreTableStruct[] = new MoreTableStruct[3];
		moreTableStruct[0] = new MoreTableStruct();
		Field subField[] = new Field[2];
		int subEntityFieldNum = 0;
		
		Set<String> mainFieldSet =new HashSet<>();
		Map<String,String> dulMap=new HashMap<>();
		
		//V1.9
		String tableName = (String) OneTimeParameter.getAttribute(StringConst.TABLE_NAME);
		if (tableName == null) {
			tableName = _toTableName(entity);
		}
		
		StringBuffer columns = new StringBuffer();
		
//		StringBuffer listcolumns = new StringBuffer();
//		String listEntityFullName="";
//		Class subClass=null;
		List listOne=null;
		List listTwo=null;
		Class list_T_classOne=null;
		Class list_T_classTwo=null;
		boolean subOneIsList=false; 
		boolean subTwoIsList=false; 
		
		int len = field.length;
		boolean isFirst = true;
//		String listStr="";
		String mailField="";//v1.8
		for (int i = 0; i < len; i++) {
//			if ("serialVersionUID".equals(field[i].getName()) || field[i].isSynthetic()) continue;
			if(isSkipFieldForMoreTable(field[i])) continue; //有Ignore注释,将不再处理JoinTable
			if (field[i] != null && field[i].isAnnotationPresent(JoinTable.class)) {
				subEntityFieldNum++;
				if (subEntityFieldNum == 1) subField[0] = field[i];
				if (subEntityFieldNum == 2) subField[1] = field[i];
				
//				if("java.util.List".equals(field[i].getType().getName())) {
//				if(field[i].getType().isAssignableFrom(List.class)) {
				if(List.class.isAssignableFrom(field[i].getType())) {
					try {
//					field[i].setAccessible(true);
					HoneyUtil.setAccessibleTrue(field[i]);
						if (subEntityFieldNum == 1) {
							subOneIsList = true;
							moreTableStruct[0].subOneIsList = true; //moreTableStruct[0]
							List list = (List) field[i].get(entity);
							listOne = list;
							if (ObjectUtils.isNotEmpty(list)) {
								list_T_classOne = list.get(0).getClass(); 
							} 
						}else if (subEntityFieldNum == 2) {
							subTwoIsList = true;
							moreTableStruct[0].subTwoIsList = true;
							List list = (List) field[i].get(entity);
							listTwo = list;
							if (ObjectUtils.isNotEmpty(list)) {
								list_T_classTwo = list.get(0).getClass(); 
							}
						}
					} catch (IllegalAccessException e) {
//						e.printStackTrace();
						Logger.warn(e.getMessage());
					}
				}
				
				continue;
			}
			if (isFirst) {
				isFirst = false;
			} else {
				columns.append(",");
			}
			columns.append(tableName);
			columns.append(".");
			
			mailField=NameTranslateHandle.toColumnName(field[i].getName(),entity.getClass());
			columns.append(mailField);  
//			moreTableStruct[0].mainColumnsForListType=columns.toString(); //v1.9.8
			mainFieldSet.add(mailField);  //v1.8
		}// (main table) for end

		if (subEntityFieldNum > 2) { //只支持一个实体里最多关联两个实体
			throw new JoinTableException("One entity only supports two JoinTable at most! " + entityFullName + " has " + subEntityFieldNum + " JoinTable now !");
		}

		JoinTable joinTable[] = new JoinTable[2];
		String subTableName[] = new String[2];
		
		if (subField[0] != null) {
			joinTable[0] = subField[0].getAnnotation(JoinTable.class);

			String errorMsg = checkJoinTable(joinTable[0]);
			if (!"".equals(errorMsg)) {
				throw new JoinTableParameterException("Error: mainField and subField can not just use only one." + errorMsg);
			}
			
			if(subOneIsList && joinTable[0].joinType()==JoinType.RIGHT_JOIN){
				throw new JoinTableException("The List type subTable donot support JoinType.RIGHT_JOIN, you can adjust with JoinType.LEFT_JOIN.");
			}
			
			if (subOneIsList && list_T_classOne == null) {
				Class c=joinTable[0].subClazz();
//				if(! "java.lang.Object".equals(c.getName())) {//V1.11
				if(! c.equals(Object.class)) {//V1.11
					list_T_classOne=c;
				}else {
					String subClassStr = joinTable[0].subClass();
					list_T_classOne=createClass(subClassStr, entityFullName);
				}
			}
		}

		//支持两个left join/right join要修改
		//closed on v1.9.8 
//		if(subEntityFieldNum==2)
//		       throw new JoinTableException("Just support JoinType.JOIN in this version when a entity has two JoinTable annotation fields!");
		
		//v1.9.8 主表只有一个从表时,检测从表1是否还有从表.
		boolean oneHasOne=false;
		StringBuffer subColumnStringBuffer[]=new StringBuffer[2];
		if(subEntityFieldNum==1 && !subOneIsList) { //支持在主表有一个从表时, 从表1还能有一个从表
			String t_subAlias = joinTable[0].subAlias();
			String useSubTableName;
			if (StringUtils.isNotBlank(t_subAlias)) {
				useSubTableName = t_subAlias;
			} else {
				subTableName[0]=_toTableNameByEntityName(subField[0].getType().getName()); 
				useSubTableName = subTableName[0];
			}
			
			//V1.11 fixed bug
			boolean checkOneHasOne;
			if(entity.getClass().equals(subField[0].getType())) { //同一个表自我关联
				checkOneHasOne=false;
			}else {
				checkOneHasOne=true;
			}
			subColumnStringBuffer[0] = _getBeanFullField_0(subField[0].getType(), useSubTableName,entityFullName,mainFieldSet,dulMap,checkOneHasOne);
			
		}else if(subEntityFieldNum==1 && subOneIsList) { //从表1是List类型
			
			String t_subAlias = joinTable[0].subAlias();
			String useSubTableName;
			if (StringUtils.isNotBlank(t_subAlias)) {
				useSubTableName = t_subAlias;
			} else {
				subTableName[0]=_toTableNameByEntityName(list_T_classOne.getName()); 
				useSubTableName = subTableName[0];
			}
//			Field ff[]=list_T_classOne.getDeclaredFields();
			subColumnStringBuffer[0] = _getBeanFullField_0(list_T_classOne, useSubTableName,entityFullName,mainFieldSet,dulMap,true);
		}
		
		//处理从表1返回的从表字段
		if (subEntityFieldNum == 1) {
			//子表首个JoinTable注解字段
			subField[1] = (Field) OneTimeParameter.getAttribute(StringConst.SUBENTITY_FIRSTANNOTATION_FIELD); 
			if (subField[1] != null) {
				subEntityFieldNum = 2; //v1.9.8  在主表只有从表1, 从表1也只有1个从表.   调整为2
				oneHasOne = true;
				moreTableStruct[0].oneHasOne = true;
			}
		}
		
		if (subField[1] != null) {
			joinTable[1] = subField[1].getAnnotation(JoinTable.class);
			
			//之前的subTwoIsList为主表下的从表的  oneHasOne时,在此处再作判断
//			if (oneHasOne && "java.util.List".equals(subField[1].getType().getName())) {
//			if (oneHasOne && subField[1].getType().isAssignableFrom(List.class)) {
			if (oneHasOne && List.class.isAssignableFrom(subField[1].getType())) {
				subTwoIsList = true; 
			}

			String errorMsg = checkJoinTable(joinTable[1]);
			if (!"".equals(errorMsg)) {
				throw new JoinTableParameterException("Annotation JoinTable, error: mainField and subField can not just use only one." + errorMsg);
			}
			
			if(subTwoIsList && joinTable[1].joinType()==JoinType.RIGHT_JOIN){  
				throw new JoinTableException("The List type subTable donot support JoinType.RIGHT_JOIN, you can adjust with JoinType.LEFT_JOIN.");
			}
		}
		
		//if no exception , set for main table
		moreTableStruct[0].tableName = tableName;
//		moreTableStruct[0].entityFullName = entityFullName;
//		moreTableStruct[0].entityName = entity.getClass().getSimpleName();
		moreTableStruct[0].joinTableNum=subEntityFieldNum;  //一个实体包含关联的从表数
		//moreTableStruct[0].columnsFull = columns.toString();  //还要从表列

		//set for subTable1 and subTable2
		//开始全面检测,处理两个从表
		for (int j = 0; j < 2; j++) { // 2 subTables
			if (subField[j] != null) {
				
//				j==0时此处不执行
				//返回的subField[1]是List, 要特别处理
				if(j==1) {  //要等从表1的subObject对象处理完, 再处理从表2的
					//处理是List oneHasOne字段  
					if (oneHasOne && subTwoIsList) {
						try {
//							subField[1].setAccessible(true);
							HoneyUtil.setAccessibleTrue(subField[1]);
//							subTwoIsList = true;
							moreTableStruct[0].subTwoIsList = true;
//							List list = (List) subField[1].get(entity);
							List list =null;
							if(moreTableStruct[1].subObject!=null) {  //bug fixed V1.11
								list=(List) subField[1].get(moreTableStruct[1].subObject);//要等从表1的subObject对象处理完
							}
							if (ObjectUtils.isNotEmpty(list)) {
								listTwo = list;
								list_T_classTwo = list.get(0).getClass();
							}
						} catch (IllegalAccessException e) {
							Logger.warn(e.getMessage());
						}
					}
					
					if (subTwoIsList && list_T_classTwo == null) {
						Class c=joinTable[1].subClazz();
//						if(! "java.lang.Object".equals(c.getName())) {//V1.11
						if(! c.equals(Object.class)) {//V1.11
							list_T_classTwo=c;
						}else {
							String subClassStr = joinTable[1].subClass();
							list_T_classTwo=createClass(subClassStr, entityFullName);
						}
					}
				}

				String mainColumn = _toColumnName(joinTable[j].mainField());
				String subColumn = _toColumnName(joinTable[j].subField());
				
				if (j == 0 && subOneIsList) {
					subTableName[j] = _toTableNameByEntityName(list_T_classOne.getName());
				} else if (j == 1 && subTwoIsList) {
					subTableName[j] = _toTableNameByEntityName(list_T_classTwo.getName());
				} else {
//				    subTableName[j] = _toTableNameByEntityName(subField[j].getType().getSimpleName());
					subTableName[j] = _toTableNameByEntityName(subField[j].getType().getName()); //从表可能有注解,要用包名去检查
				}
				
				moreTableStruct[1 + j] = new MoreTableStruct();
				//从表的  
				moreTableStruct[1 + j].subEntityField = subField[j];  //用于返回拼装数据时,获取字段名
				
				moreTableStruct[1 + j].tableName = subTableName[j]; //各实体对应的表名
//				moreTableStruct[1 + j].entityFullName = subField[j].getType().getName();
//				moreTableStruct[1 + j].entityName = subField[j].getType().getSimpleName();

				moreTableStruct[1 + j].mainField = joinTable[j].mainField();
				moreTableStruct[1 + j].subField = joinTable[j].subField();
				moreTableStruct[1 + j].joinType = joinTable[j].joinType();
				String t_subAlias = joinTable[j].subAlias();
				String useSubTableName;
				if (t_subAlias != null && !"".equals(t_subAlias)) {
					moreTableStruct[1 + j].subAlias = t_subAlias;
					useSubTableName = t_subAlias;
					moreTableStruct[1 + j].hasSubAlias = true;
				} else {
					useSubTableName = subTableName[j];
				}
				if(!"".equals(mainColumn) && !"".equals(subColumn)){
//				   moreTableStruct[1 + j].joinExpression = tableName + "." + mainColumn + "=" + useSubTableName + "." + subColumn;
				   //v1.9
					String mainColumnArray[]=mainColumn.split(",");
					String subColumnArray[]=subColumn.split(",");
					if(mainColumnArray.length!=subColumnArray.length) {
						throw new JoinTableException("The number of field in mainField & subField is different , mainField is: "+mainColumnArray.length+" ,subField is : "+subColumnArray.length);
					}
					moreTableStruct[1 + j].joinExpression="";
					String firstTableName="";
					for (int i = 0; i < mainColumnArray.length; i++) {
						if(i!=0) moreTableStruct[1 + j].joinExpression += K.space+K.and+K.space;
						if(oneHasOne && j==1) {
							firstTableName=moreTableStruct[1].useSubTableName;
						}else {
							firstTableName=tableName;
						}
						moreTableStruct[1 + j].joinExpression +=firstTableName + "." + mainColumnArray[i] + "=" + useSubTableName + "." + subColumnArray[i];
					}
					
				}
				moreTableStruct[1 + j].useSubTableName = useSubTableName;
				try {
//					subField[j].setAccessible(true);
					HoneyUtil.setAccessibleTrue(subField[j]);
					if (j == 0 && subOneIsList) {
						if (ObjectUtils.isNotEmpty(listOne))
							moreTableStruct[1 + j].subObject = listOne.get(0);
					} else if (j == 1 && subTwoIsList) {
						if (ObjectUtils.isNotEmpty(listTwo))
							moreTableStruct[1 + j].subObject = listTwo.get(0);
					} else if (j == 1 && oneHasOne) {
						if (moreTableStruct[1].subObject == null)
							moreTableStruct[1 + j].subObject = null;
						else
							moreTableStruct[1 + j].subObject = subField[j].get(moreTableStruct[1].subObject);
					} else {
						moreTableStruct[1 + j].subObject = subField[j].get(entity); //need entity , not class
					}
				} catch (IllegalAccessException e) {
					throw ExceptionHelper.convert(e);
				}

				if(subEntityFieldNum==1){ //subEntityFieldNum==1 只有一个从表,从表1上面都有扫描, 表示上面都有扫描过
					                       //subEntityFieldNum==1  j也不可以等于1(不可能进行两次循环)
			    }else if(j==0 && subOneIsList && !oneHasOne) { //从主表来的 从表1 List (主表有两个从表时)         
//					Field ff[]=list_T_classOne.getDeclaredFields();
					subColumnStringBuffer[0] = _getBeanFullField_0(list_T_classOne, useSubTableName,entityFullName,mainFieldSet,dulMap,true);
				}else if(j==1 && subTwoIsList) { //j==1,表示有第二个存在
//				   Field ff[]=list_T_classTwo.getDeclaredFields();
				   subColumnStringBuffer[1] = _getBeanFullField_0(list_T_classTwo, useSubTableName,entityFullName,mainFieldSet,dulMap,true);
			    }else if(!oneHasOne || j==1) { //上面没查， 这里要实现
//				}else {//有些不需要处理, 如oneHasOne,i==0
			    	subColumnStringBuffer[j] = _getBeanFullField_0(subField[j].getType(), useSubTableName,entityFullName,mainFieldSet,dulMap);
			    }
//			    subColumnStringBuffer[j]=listcolumns;
				moreTableStruct[1 + j].columnsFull = subColumnStringBuffer[j].toString(); 

				columns.append(",");
				columns.append(subColumnStringBuffer[j]);
				
			}
		}//end subFieldEntity for
		
		if(subOneIsList)
		   moreTableStruct[1].subClass=list_T_classOne;
		if(subTwoIsList)
			   moreTableStruct[2].subClass=list_T_classTwo;
		
		moreTableStruct[0].columnsFull = columns.toString(); //包含从表的列
		moreTableStruct[0].subDulFieldMap=dulMap;

		//		return columns.toString();
		return moreTableStruct;
	}
	
	@SuppressWarnings("rawtypes")
	private static Class createClass(String subClassStr, String packageAndClassName) {
		//		String subClassStr = joinTable[0].subClass();
		Class newClazz = null;
		boolean isOk = false;
		if (StringUtils.isNotBlank(subClassStr)) {
			try {
				newClazz = Class.forName(subClassStr);
				isOk = true;
			} catch (ClassNotFoundException e) {
				try {
					int index1 = subClassStr.indexOf('.');
					int index2 = packageAndClassName.lastIndexOf('.');

					if (index1 == -1 && index2 > 0) {
						String newStr = packageAndClassName.substring(0, index2+1) + subClassStr;
						newClazz = Class.forName(newStr);
						isOk = true;
					}
				} catch (ClassNotFoundException e2) {
					// ignore
				}
			}
		}

		if (isOk) {
			return newClazz;
		} else {
			throw new BeeException("MoreTable select, if use List type subEntity field , "
					+ "the object must have element or config the subClass with JoinTable Annotation!");
		}
	}
	
	//for moreTable
	@SuppressWarnings("rawtypes")
	static StringBuffer _getBeanFullField_0(Class entityClass, String tableName,String entityFullName,
			Set<String> mainFieldSet,Map<String,String> dulMap) {
		return _getBeanFullField_0(entityClass, tableName, entityFullName, mainFieldSet, dulMap,false);
	}

	@SuppressWarnings("rawtypes")
	static StringBuffer _getBeanFullField_0(Class entityClass, String tableName,String entityFullName,
			Set<String> mainFieldSet,Map<String,String> dulMap,boolean checkOneHasOne) {
		
//		Field field[] = entityField.getType().getDeclaredFields();
		Field field[] = entityClass.getDeclaredFields();
		
//	 return	_getBeanFullField_0(field, tableName, entityFullName, mainFieldSet, dulMap, checkOneHasOne, entityField.getName());
//	}
//	//for moreTable
//	static StringBuffer _getBeanFullField_0(Field field[], String tableName,String entityFullName,
//			Set<String> mainFieldSet,Map<String,String> dulMap,boolean checkOneHasOne,String entityFieldFullName) {
		
		String entityFieldFullName=entityClass.getName();

//		tableName传入的也是:useSubTableName
//		entityFieldFullName just for tip
//		Field field[] = entityField.getType().getDeclaredFields();

//		String tableName = _toTableNameByEntityName(entityField.getType().getSimpleName());//有可能用别名
		StringBuffer columns = new StringBuffer();
		int len = field.length;
		boolean isFirst = true;
		String subColumnName="";
		int currentSubNum=0;
		Field subEntityFirstAnnotationField=null;
		List<String> WarnMsglist=new ArrayList<>();
		for (int i = 0; i < len; i++) {
//			if ("serialVersionUID".equals(field[i].getName()) || field[i].isSynthetic()) continue;
			if(HoneyUtil.isSkipFieldForMoreTable(field[i])) continue; //有Ignore注解,将不再处理JoinTable
			if (field[i] != null && field[i].isAnnotationPresent(JoinTable.class)) {
				currentSubNum++;
				if(checkOneHasOne && currentSubNum==1) subEntityFirstAnnotationField=field[i]; //第一个从表里的第一个连接字段
				
////				Logger.error("注解字段的实体: " + entityField.getType().getName() + "里面又包含了注解:" + field[i].getType());
//				String entityFieldName=entityField.getType().getName();
//				
//				if(!entityFieldName.equals(field[i].getType().getName())){ //??
				   if(checkOneHasOne) {
					   WarnMsglist.add("Annotation JoinTable field: " +entityFieldFullName+"(in "+ entityFullName + ") still include JoinTable field:" + field[i].getName() + "(will be ignored)!");
				   }else if(!entityClass.equals(field[i].getType())) {//不是同一个实体自我关联   V1.11 fixed bug
					   Logger.warn("Annotation JoinTable field: " +entityFieldFullName+"(in "+ entityFullName + ") still include JoinTable field:" + field[i].getName() + "(will be ignored)!");
				   }
//				}
				continue;
			}

			if (isFirst) {
				isFirst = false;
			} else {
				columns.append(",");
			}
			subColumnName=NameTranslateHandle.toColumnName(field[i].getName(),entityClass); //todo
			
			if (field[i].isAnnotationPresent(JustFetch.class)) {
				columns.append(getJustFetchDefineName(field[i]));
			} else {
				columns.append(tableName);
				columns.append(".");
				columns.append(subColumnName);
			}
			
			if(!mainFieldSet.add(subColumnName) && isConfuseDuplicateFieldDB()){
				if (isSQLite()) {
					dulMap.put(tableName + "." + subColumnName, tableName + "." + subColumnName); 
				} else {
					dulMap.put(tableName + "." + subColumnName, tableName + "_" + subColumnName + "_$"); 
				}
				if (isSQLite()) {
					columns.append(" "+K.as+" '" + tableName + "." + subColumnName+"'");
				} else {
					columns.append(" " + tableName + "_" + subColumnName + "_$");
				}
			}
		}
		
		if(checkOneHasOne && currentSubNum>1) {  //从表,只能有1个关联字段,  超过1个,将会被忽略.
			subEntityFirstAnnotationField=null;
			for (int i = 0; i < currentSubNum; i++) {
				Logger.warn(WarnMsglist.get(i));
			}
		}
		
		if (checkOneHasOne && currentSubNum > 2) { //只支持一个实体里最多关联两个实体
			throw new JoinTableException("One entity only supports two JoinTable at most! " + entityFieldFullName + " has " + currentSubNum + " JoinTable now !");
		}
		
		OneTimeParameter.setAttribute(StringConst.SUBENTITY_FIRSTANNOTATION_FIELD, subEntityFirstAnnotationField);
		
		return columns;
	}
	
	/**
	 * jdbc type->java type
	 * 将jdbc的数据类型转换为java的类型 
	 * @param jdbcType
	 * @return the string of java type
	 */
	public static String getFieldType(String jdbcType) {
		
		String dbName = HoneyConfig.getHoneyConfig().getDbName();
		Map<String, String> jdbc2JavaTypeMap=JdbcToJavaType.getJdbcToJavaType(dbName);

		String javaType = jdbc2JavaTypeMap.get(jdbcType);

		if (javaType != null) return javaType;

		if (jdbc2JavaTypeMap.get(jdbcType) == null) {

			//fix UNSIGNED,  like :TINYINT UNSIGNED 
			String tempType = jdbcType.trim();
			if (tempType.endsWith(" UNSIGNED")) {
				int i = tempType.indexOf(" ");
				javaType = jdbc2JavaTypeMap.get(tempType.substring(0, i));
				if (javaType != null) return javaType;
			}
			
			if (javaType == null){
				javaType =jdbc2JavaTypeMap.get(jdbcType.toLowerCase());
				if (javaType != null) return javaType;
				
				if (javaType == null){
					javaType =jdbc2JavaTypeMap.get(jdbcType.toUpperCase());
					if (javaType != null) return javaType;
				}
			}
			
			if (HoneyUtil.isMongoDB()) {
				javaType = NameTranslateHandle.toEntityName(jdbcType);
			} else {
				javaType = "[UNKNOWN TYPE]" + jdbcType;
			}
			Logger.debug(javaType); //V1.17
		}

		return javaType;
	}



	private static void initJavaTypeMap() {

		javaTypeMap.put("java.lang.String", 1);
		javaTypeMap.put("java.lang.Integer", 2);
		javaTypeMap.put("java.lang.Long", 3);
		javaTypeMap.put("java.lang.Double", 4);
		javaTypeMap.put("java.lang.Float", 5);
		javaTypeMap.put("java.lang.Short", 6);
		javaTypeMap.put("java.lang.Byte", 7);
//		javaTypeMap.put("[Ljava.lang.Byte;", 8); //  Byte[]
		javaTypeMap.put("[B", 8); //byte[]  
		javaTypeMap.put("java.lang.Boolean", 9);
		
		//支持原生类型
		javaTypeMap.put("int", 2);
		javaTypeMap.put("long", 3);
		javaTypeMap.put("double", 4);
		javaTypeMap.put("float", 5);
		javaTypeMap.put("short", 6);
		javaTypeMap.put("byte", 7);
		javaTypeMap.put("boolean", 9);

		javaTypeMap.put("java.math.BigDecimal", 10);

		javaTypeMap.put("java.sql.Date", 11);
		javaTypeMap.put("java.sql.Time", 12);
		javaTypeMap.put("java.sql.Timestamp", 13);
		
		javaTypeMap.put("java.sql.Blob", 14);
		javaTypeMap.put("java.sql.Clob", 15);

		javaTypeMap.put("java.sql.NClob", 16);
		javaTypeMap.put("java.sql.RowId", 17);
		javaTypeMap.put("java.sql.SQLXML", 18);

		javaTypeMap.put("java.math.BigInteger", 19);
		
		javaTypeMap.put("char", 20);
		javaTypeMap.put("java.util.Date", 21);  
		
		javaTypeMap.put("java.sql.Array", 22);
		javaTypeMap.put("java.io.InputStream", 23);
		javaTypeMap.put("java.io.Reader", 24);
		javaTypeMap.put("java.sql.Ref", 25);
		
//	    javaTypeMap.put("org.teasoft.bee.osql.annotation.customizable.Json", 26);
		
		javaTypeMap.put("java.net.URL", 27);
		
//		javaTypeMap.put("java.util.UUID", 28);  //1 

		javaTypeMap.put("java.time.LocalDate", 31);
		javaTypeMap.put("java.time.LocalTime", 32);
		javaTypeMap.put("java.time.LocalDateTime", 33);
	}

	public static int getJavaTypeIndex(String javaType) {
		return javaTypeMap.get(javaType) == null ? -1 : javaTypeMap.get(javaType);
	}

	/**
	 * 首字母转换成大写
	 */
	public static String firstLetterToUpperCase(String str) {
		return NameUtil.firstLetterToUpperCase(str);
	}

	public static boolean isContinue(int includeType, Object object, Field field) {
		//		object字段上对应的值
		if (field != null) {
			if(isSkipField(field)) return true;
			if(isSkipFieldJustFetch(field)) return true;  //V1.11  JustFetch不用于where
		}

		//exclude:  NULL and "" and "  "
		if(-3==includeType && StringUtils.isBlank((String)object)) { 
			 return true;
		}
		
//		includeType == NullEmpty.EMPTY_STRING && object == null  要包括空字符,但对象不是空字符,而是null,则跳过.
		return (((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.EMPTY_STRING) && object == null)
				|| ((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.NULL) && "".equals(object)) );
	}
	
	public static boolean isSkipField(Field field) {
		if (field != null) {
			if ("serialVersionUID".equals(field.getName())) return true;
			if(AnnoUtil.isIgnore(field)) return true; //1.17
			if (field.isAnnotationPresent(JoinTable.class)) return true;
			if (field.isSynthetic()) return true;
			if(AnnoUtil.isFK(field)) return true; //2.1.8
			if(AnnoUtil.isGridFs(field)) return true; //2.1.8
			if(AnnoUtil.isGridFsMetadata(field)) return true; //2.1.8
		}
		return false;
	}
	
	static boolean isSkipFieldForMoreTable(Field field) {
		if (field != null) {
			if ("serialVersionUID".equals(field.getName())) return true;
			if(AnnoUtil.isIgnore(field)) return true; //1.17
//			if (field.isAnnotationPresent(JoinTable.class)) return true;
			if (field.isSynthetic()) return true;
		}
		
		return false;
	}
	
	public static boolean isSkipFieldJustFetch(Field field) {
		if (field != null) {
			if (field.isAnnotationPresent(JustFetch.class)) return true;
		}
		return false;
	}

	/**
	 * 
	 * @param pst PreparedStatement
	 * @param objTypeIndex
	 * @param i  prarmeter index
	 * @param value
	 * @throws SQLException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static void setPreparedValues(PreparedStatement pst, int objTypeIndex, int i, Object value) throws SQLException {

		if (value == null) {
			setPreparedNull(pst, objTypeIndex, i);
			return;
		}

		switch (objTypeIndex) {
			case 1:
				pst.setString(i + 1, (String) value);
				break;
			case 2:
				pst.setInt(i + 1, (Integer) value);
				break;
			case 3:
				pst.setLong(i + 1, (Long) value);
				break;
			case 4:
				pst.setDouble(i + 1, (Double) value);
				break;
			case 5:
				pst.setFloat(i + 1, (Float) value);
				break;
			case 6:
				pst.setShort(i + 1, (Short) value);
				break;
			case 7:
				pst.setByte(i + 1, (Byte) value);
				break;
			case 8:
				pst.setBytes(i + 1, (byte[]) value);
				break;
			case 9:
				pst.setBoolean(i + 1, (Boolean) value);
				break;
			case 10:
				pst.setBigDecimal(i + 1, (BigDecimal) value);
				break;
//			case 11: //you can define your SetParaTypeConvert for one of both
//				pst.setDate(i + 1, (Date) value);//process in default
//				void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
//				break;
			case 12:
				pst.setTime(i + 1, (Time) value);
				break;
			case 13:
				pst.setTimestamp(i + 1, (Timestamp) value);
				break;
//			case 14://you can define your SetParaTypeConvert for one of both
//				pst.setBlob(i + 1, (Blob) value);//process in default
//				pst.setBlob(parameterIndex, inputStream);
//				break;
//			case 15:  //you can define your SetParaTypeConvert for one of both
//				pst.setClob(i + 1, (Clob) value); //process in default
////			pst.setClob(i + 1, reader);
//				break;
//			case 16: // like 15
//				pst.setNClob(i + 1, (NClob) value);
//				break;
			case 17:
				pst.setRowId(i + 1, (RowId) value);
				break;
			case 18:
				pst.setSQLXML(i + 1, (SQLXML) value);
				break;
			case 20:
				pst.setString(i + 1, value.toString());
				break;
				
//	             process in define SetParaTypeConvert
//			case 21:
//				Date d= new Date(((java.util.Date)value).getTime());
////				pst.setDate(i + 1, d); //ok
//				pst.setObject(i + 1, d); //ok
//				//测试数据库是date,datetime,timestamp是否都可以
//				break;
				
			case 22:	
				pst.setArray(i + 1, (java.sql.Array)value);
				break;
				
//			case 23,24
				//InputStream,Reader can define the SetParaTypeConvert for them.
//				pst.setAsciiStream(parameterIndex, java.io.InputStream x);
//				pst.setBinaryStream(parameterIndex, java.io.InputStream x);
//				pst.setCharacterStream(parameterIndex, reader);
//				pst.setNCharacterStream(parameterIndex, reader);
//				pst.setUnicodeStream(parameterIndex, x, length);
				
			case 25:
				pst.setRef(i + 1, (Ref)value);
				break;
				
			case 26:  //Json Annotation  //NOSONAR
			{
				SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(Json.class);
				if (converter != null) {
					pst.setString(i + 1, (String) converter.convert(value));
					break;
				}
			}
			
			case 27:
				pst.setURL(i + 1, (java.net.URL)value);
				break;
				
//				pst.setUnicodeStream(parameterIndex, x, length);
			
//			case 28:   //2 
//				UUID u=(UUID)value;
//				pst.setObject(i + 1, u.toString());
//				pst.setString(i + 1, u.toString());
//				pst.setObject(i + 1, u);  
//				pst.setObject(i + 1, u, targetSqlType);
//				break;
			
			case 19:
//	        	pst.setBigInteger(i+1, (BigInteger)value);break;
			default:
			{
//				value=tryConvert(value);
//				pst.setObject(i + 1, value);
				
//				先查找是否有对应类型的转换器;  到这里value不会是null;前面已处理
				SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(value.getClass()); //fixed bug
				if (converter != null) {
					value = converter.convert(value);
					  pst.setObject(i + 1, value);
				//if did not define SetParaTypeConvert,will process by default	
				}else if(objTypeIndex==11) {
					pst.setDate(i + 1, (Date) value);
				}else if(objTypeIndex==14) {	
					pst.setBlob(i + 1, (Blob) value);
				}else if(objTypeIndex==15) {
					pst.setClob(i + 1, (Clob) value);
			    }else if(objTypeIndex==16) {
			    	pst.setNClob(i + 1, (NClob) value);
			    }else if(objTypeIndex==23) {
			    	pst.setBinaryStream(i + 1, (java.io.InputStream)value);
			    }else if(objTypeIndex==24) {
			    	pst.setCharacterStream(i + 1, (java.io.Reader)value);
			    }else {
			    	pst.setObject(i + 1, value);
			    }
				
			}
		} //end switch
	}
	
//	private static Object tryConvert(Object value) {
//		if (value == null) return value;
//		return SetParaTypeConverterRegistry.converterProcess(value.getClass(), value);
//	}

	static Object getResultObject(ResultSet rs, String typeName, String columnName) throws SQLException {

		int k = HoneyUtil.getJavaTypeIndex(typeName);
		if (isSQLite() && "java.sql.Timestamp".equals(typeName)) {
			k = 1;
		}

		switch (k) {
			case 1:
				return rs.getString(columnName);
			case 2:
				return rs.getInt(columnName);
			case 3:
				return rs.getLong(columnName);
			case 4:
				return rs.getDouble(columnName);
			case 5:
				return rs.getFloat(columnName);
			case 6:
				return rs.getShort(columnName);
			case 7:
				return rs.getByte(columnName);
			case 8:
				return rs.getBytes(columnName);
			case 9:
				return rs.getBoolean(columnName);
			case 10:
				return rs.getBigDecimal(columnName);
			case 11:
				return rs.getDate(columnName);
			case 12:
				return rs.getTime(columnName);
			case 13:
				return rs.getTimestamp(columnName);
			case 14:
				return rs.getBlob(columnName);
			case 15:
				return rs.getClob(columnName);
			case 16:
				return rs.getNClob(columnName);
			case 17:
				return rs.getRowId(columnName);
			case 18:
				return rs.getSQLXML(columnName);
				
//				19: BigInteger
//				20:char
				
//				 21:java.util.Date 
			case 21:	
				return rs.getTimestamp(columnName);//改动态???
			case 22:
				return rs.getArray(columnName);  //java.sql.Array
			case 23:
				return rs.getBinaryStream(columnName); //java.io.InputStream
			case 24:
				return rs.getCharacterStream(columnName); //java.io.Reader
			case 25:
				return rs.getRef(columnName);  //java.sql.Ref	
				
//			26:	annotation.customizable.Json
				
			case 27:
				return rs.getURL(columnName);
				
			case 19:
//	        	no  getBigInteger
			default:
				return rs.getObject(columnName);
		} //end switch

	}

	static Object getResultObjectByIndex(ResultSet rs, String typeName, int index) throws SQLException {

		int k = HoneyUtil.getJavaTypeIndex(typeName);
//		if (isSQLite() && "java.sql.Timestamp".equals(typeName)) {
		if (isSQLite() && ( "java.sql.Timestamp".equals(typeName) ||"java.sql.Date".equals(typeName) )  ) {
			k = 1;
		}

		switch (k) {
			case 1:
				return rs.getString(index);
			case 2:
				return rs.getInt(index);
			case 3:
				return rs.getLong(index);
			case 4:
				return rs.getDouble(index);
			case 5:
				return rs.getFloat(index);
			case 6:
				return rs.getShort(index);
			case 7:
				return rs.getByte(index);
			case 8:
				return rs.getBytes(index);
			case 9:
				return rs.getBoolean(index);
			case 10:
				return rs.getBigDecimal(index);
			case 11:
			case 31:
				return rs.getDate(index);
			case 12:
			case 32:
				return rs.getTime(index);
			case 13:
			case 33:
				return rs.getTimestamp(index);
			case 14:
				return rs.getBlob(index);
			case 15:
				return rs.getClob(index);
			case 16:
				return rs.getNClob(index);
			case 17:
				return rs.getRowId(index);
			case 18:
				return rs.getSQLXML(index);
				
//				19:BigInteger
//				20:char
				
//				 21:java.util.Date 
			case 21:	
				return rs.getTimestamp(index);//改动态???
//				return rs.getDate(index);  //Oracle 使用该方法获取会丢失:时分秒
				
			case 22:
				return rs.getArray(index);  //java.sql.Array
			case 23:
				return rs.getBinaryStream(index); //java.io.InputStream
			case 24:
				return rs.getCharacterStream(index); //java.io.Reader
//				return rs.getAsciiStream(index); //java.io.InputStream	
//				return rs.getNCharacterStream(index); //java.io.Reader	
//				return rs.getNString(index);  //java.lang.String
			case 25:
				return rs.getRef(index);  //java.sql.Ref
				
//				26:	annotation.customizable.Json

			case 27:
				return rs.getURL(index);
				
			case 19:
				//no  getBigInteger	
			default:
				return rs.getObject(index);
		} //end switch

	}

	public static void setPreparedNull(PreparedStatement pst, int objTypeIndex, int i) throws SQLException {

		pst.setNull(i + 1, Types.NULL);
	}

	public static String genSerializableNum() {
		String s = Math.random() + "";
		int end = s.length() > 12 ? 12 : s.length();
		return "159" + s.substring(2, end) + "L";
	}

	public static String deleteLastSemicolon(String sql) {
		if(sql==null) return sql;
		String new_sql = sql.trim();
		if (new_sql.endsWith(";")) return new_sql.substring(0, new_sql.length() - 1); //fix oracle ORA-00911 bug.oracle用jdbc不能有分号
		
		sql=_deletePrefix(new_sql); //2.2
		
		return sql;
	}
	
	public static String deletePrefix(String sql) { //2.2
		if(sql==null) return sql;
		String new_sql = sql.trim();
		
		return _deletePrefix(new_sql);
	}
	private static String _deletePrefix(String new_sql) {
		if(new_sql.startsWith(START_MARK)) return new_sql.substring(20,new_sql.length()); //2.2
		return new_sql;
	}


	public static <T> void checkPackage(T entity) {
		if (entity == null) return;
		
		String classFullName=entity.getClass().getName();
		//		传入的实体可以过滤掉常用的包开头的,如:java., javax. ; 但像spring开头不能过滤,否则spring想用bee就不行了.
		if (classFullName.startsWith("java.") || classFullName.startsWith("javax.")) {
			throw new BeeIllegalEntityException("BeeIllegalEntityException: Illegal Entity, " + entity.getClass().getName());
		}
	}
	
	public static boolean isJavaPackage(Class<?> entityClass) {
		if (entityClass == null) return false;
		String classFullName = entityClass.getName();
		return (classFullName.startsWith("java.") || classFullName.startsWith("javax."));
	}

	/**
	 * 将非null的字段值以Map形式返回(被认为忽略的字段也不会返回,即isSkipField(fields[i]))
	 * @param entity
	 * @return
	 */
	public static <T> Map<String, Object> getColumnMapByEntity(T entity) {
		Map<String, Object> map = new HashMap<>();
		Field fields[] = HoneyUtil.getFields(entity.getClass());
		int len = fields.length;
		try {
			for (int i = 0; i < len; i++) {
				HoneyUtil.setAccessibleTrue(fields[i]);
				if (fields[i].get(entity) == null || isSkipField(fields[i])) {
					continue;
				} else {
					map.put(NameTranslateHandle.toColumnName(fields[i].getName(),entity.getClass()), fields[i].get(entity));
				}
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return map;
	}
	
	//List<PreparedValue>  to valueBuffer
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String list2Value(List<PreparedValue> list,boolean needType){
		StringBuffer b=new StringBuffer();
		if(list==null ) return null;
		if(list.size()==0) return "";
		
		String type="";
		
		int size=list.size();
		Object value=null;
		for (int j = 0; j < size; j++) {
			
			value=list.get(j).getValue();
			
			//V1.11
			Field f = list.get(j).getField();
			if (f != null) {
				SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(Json.class);
				if (converter != null) {
					value = converter.convert(value);
				}
			}
			
			b.append(value);
			type=list.get(j).getType();
			if(needType && type !=null) {
				b.append("(");
				
				if(type.startsWith("java.lang.")){
					b.append(type.substring(10));
				}else{
					b.append(type);
				}
				b.append(")");
			}
			if(j!=size-1) b.append(",");
		}
		
		return b.toString();
	}
	

	/**
	 *  ! just use in debug env.  please set off in prod env.
	 * @param sql
	 * @param list
	 * @return executable sql string
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getExecutableSql(String sql, List<PreparedValue> list){
		if(list==null || list.size()==0) return sql;
		
		int size=list.size();
		Object value=null;
		for (int j = 0; j < size; j++) {
			value=list.get(j).getValue();
			//V1.11
			Field f = list.get(j).getField();
			if (f != null) {
				SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(Json.class);
				if (converter != null) {
					value = converter.convert(value);
				}
			}
			
			if(value!=null && value instanceof CharSequence) { //V1.11
//				V1.17 添加单引号转义;双引号不需要转;
				sql=sql.replaceFirst("\\?", "'"+String.valueOf(value).replace("\\%", "\\\\%").replace("\\_", "\\\\_").replace("'","\\\\'").replace("$", "\\$")+"'"); //bug 2021-05-25
			}else {
				sql=sql.replaceFirst("\\?", String.valueOf(value));
			}
		}
		sql+=" ;"; //V1.17 添加分号
		return sql;
	}
	
	// V2.1.7
	public static String sqlFormat(String sql) {
		boolean sqlFormatFlag = HoneyConfig.getHoneyConfig().showSql_sqlFormat;
		if (sqlFormatFlag) {
			SqlFormat sqlFormat = SpiInstanceRegister.getInstance(SqlFormat.class);
			if (sqlFormat != null) {
				try {
					sql = sqlFormat.format(sql);
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return sql;
	}
	
	static <T> String checkAndProcessSelectField(T entity, String ...fieldList) {

		if (fieldList == null) return null;
		
		String packageAndClassName=entity.getClass().getName();
		String columnsdNames=HoneyContext.getBeanField(packageAndClassName);
		if (columnsdNames == null) {
			Field fields[]=HoneyUtil.getFields(entity.getClass());
			columnsdNames=HoneyUtil.getBeanField(fields,entity.getClass());//获取属性名对应的DB字段名
			HoneyContext.addBeanField(packageAndClassName, columnsdNames);
		}

		return checkAndProcessSelectFieldViaString(columnsdNames, null, fieldList);
	}
	
	public static String checkAndProcessSelectFieldViaString(String columnsdNames,Map<String,String> subDulFieldMap,String ...fields){
		return checkAndProcessSelectFieldViaString(columnsdNames, subDulFieldMap, true, fields);
	}
	 
	public static String checkAndProcessSelectFieldViaString(String columnsdNames,Map<String,String> subDulFieldMap,boolean check,String ...fields){
		if (fields == null) return null;
		 
//		Field fields[] = entity.getClass().getDeclaredFields();
//		String packageAndClassName = entity.getClass().getName();
//		String columnsdNames = HoneyContext.getBeanField(packageAndClassName);
//		if (columnsdNames == null) {
//			columnsdNames = HoneyUtil.getBeanField(fields);//获取属性名对应的DB字段名
//			HoneyContext.addBeanField(packageAndClassName, columnsdNames);
//		}
		
		columnsdNames=columnsdNames.toLowerCase();//不区分大小写检测

		String errorField = "";
		boolean isFirstError = true;
		String selectFields[];
		
		if (fields.length == 1) { //变长参数,只有一个时,才允许用逗号隔开
			selectFields = fields[0].split(",");
		} else {
			selectFields = fields;
		}
		StringUtils.trim(selectFields);
		
		String newSelectFields = "";
		boolean isFisrt = true;
		String colName;
        String checkColName;

		for (String s : selectFields) {
			colName=_toColumnName(s);
			checkColName=colName.toLowerCase();
//			if(isMoreTable){  //带有点一样转换
//			}
			
//			if (!columnsdNames.contains(colName)) {
			if( !(  
			     columnsdNames.contains(","+checkColName+",") || columnsdNames.startsWith(checkColName+",") 
			  || columnsdNames.endsWith(","+checkColName) ||  columnsdNames.equals(checkColName) 
			  || columnsdNames.contains("."+checkColName+",")  || columnsdNames.endsWith("."+checkColName)
			  || columnsdNames.contains(","+checkColName+" ") || columnsdNames.startsWith(checkColName+" ")  //取别名
			  || columnsdNames.contains("."+checkColName+" ") //取别名
			  )  ){
				if (isFirstError) {
					errorField += s;
					isFirstError = false;
				} else {
					errorField += "," + s;
				}
			}
			
			String newField;
			if (subDulFieldMap == null) {
				newField=null;
			} else {
				newField=subDulFieldMap.get(colName);
			}
			if (newField != null) {
				if (isSQLite()) {
					colName=colName +"  "+ K.as + " '" + newField + "'";
				} else {//oracle
					colName=colName + " " + newField;
				}
			}
			if (isFisrt) {
				newSelectFields += colName;
				isFisrt = false;
			} else {
				newSelectFields += ", " + colName;
			}

		}//end for

		if (!"".equals(errorField)) {
			if (check)
				throw new BeeErrorFieldException("ErrorField: " + errorField);
			else { //2.4.0 fun 不检测字段是否是实体的
				Logger.debug("Please confirm the field(s) is right or not :" + errorField);
			}
		}
		
		if("".equals(newSelectFields.trim())) return null;
		
		return newSelectFields;
	} 

	private static String _toColumnName(String fieldName) {
		return NameTranslateHandle.toColumnName(fieldName);
	}

	private static String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

	private static String _toTableNameByEntityName(String entityName) {
		return NameTranslateHandle.toTableName(entityName);
	}

	private static String SET_WRONG_VALUE_IN="Annotation JoinTable set wrong value in ";
	private static String checkJoinTable(JoinTable joinTable) {
		String mainField= joinTable.mainField();
		String subField=joinTable.subField();
		
		String subAlias=joinTable.subAlias();
		String subClass=joinTable.subClass();
		
		if (NameCheckUtil.isIllegal(mainField)) {
			throw new JoinTableParameterException(SET_WRONG_VALUE_IN+"mainField:" + mainField);
		}
		if (NameCheckUtil.isIllegal(subField)) {
			throw new JoinTableParameterException(SET_WRONG_VALUE_IN+"subField:" + subField);
		}
		if (NameCheckUtil.isIllegal(subAlias)) {
			throw new JoinTableParameterException(SET_WRONG_VALUE_IN+"subAlias:" + subAlias);
		}
		if (NameCheckUtil.isIllegal(subClass)) {
			throw new JoinTableParameterException(SET_WRONG_VALUE_IN+"subClass:" + subClass);
		}
		
		String errorMsg = "";
		int errorCount=0;
		
		if (mainField == null) {
			errorMsg = "mainField is null! ";
			errorCount++;
		} else if ("".equals(mainField.trim())) {
			errorMsg += "mainField is empty! ";
			errorCount++;
		}

		if (subField == null) {
			errorMsg += "subField is null! ";
			errorCount++;
		} else if ("".equals(subField.trim())) {
			errorMsg += "subField is empty! ";
			errorCount++;
		}
        if(errorCount==1)
		    return errorMsg;
        else return "";
	}
	
	public static boolean isMysql() {
//		return false;    //test  用来测HoneyContext.justGetPreparedValue("abc"); 检查是否还有元素,  不准确
		return    DatabaseConst.MYSQL.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName()) 
			   || DatabaseConst.MariaDB.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
	//Oracle,SQLite,Sql Server
	public static boolean isConfuseDuplicateFieldDB(){
		return DatabaseConst.ORACLE.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())
			|| DatabaseConst.SQLite.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())
			|| (DatabaseConst.SQLSERVER.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName()) 
//				&& HoneyConfig.getHoneyConfig().getDatabaseMajorVersion()>=11
			)	
				;
	}
	
	public static boolean isSQLite() {
		return DatabaseConst.SQLite.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean isSqlServer() {
		return DatabaseConst.SQLSERVER.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
	public static boolean isOracle(){
		return DatabaseConst.ORACLE.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
	public static boolean isH2() {
		return DatabaseConst.H2.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
	public static boolean isCassandra(){
		return DatabaseConst.Cassandra.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
	public static boolean isHbase(){
		return DatabaseConst.Hbase.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
	public static boolean isMongoDB(){
		return DatabaseConst.MongoDB.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
	
	public static boolean setPageNum(List<PreparedValue> list) {
		int array[] = (int[]) OneTimeParameter.getAttribute("_SYS_Bee_Paing_NumArray");
		for (int i = 0; array != null && i < array.length; i++) {
			PreparedValue p = new PreparedValue();
//			p.setType("Integer"); //bug
			p.setType("java.lang.Integer"); //fixed bug
			p.setValue(array[i]);
			if (HoneyUtil.isSqlServer()) { //top n
				list.add(0, p);
			} else { //default the page num in the last.
				list.add(p);
			}
		}
		
		return array != null ? true : false;
	}

	public static boolean isRegPagePlaceholder() {
	    return OneTimeParameter.isTrue("_SYS_Bee_Paing_Placeholder");
	}

	public static void regPagePlaceholder() {
	    if(isSqlServer()) return ;
	    if(ShardingUtil.hadSharding()) return ; //2.0 有分片则不用
		OneTimeParameter.setTrueForKey("_SYS_Bee_Paing_Placeholder");
	}

	public static void regPageNumArray(int array[]) {
		OneTimeParameter.setAttribute("_SYS_Bee_Paing_NumArray", array);
	}
	
	public static boolean isSqlKeyWordUpper() {
		String kwCase = HoneyConfig.getHoneyConfig().sqlKeyWordCase;
		return "upper".equalsIgnoreCase(kwCase) ? true : false;
	}
	
	public static <T> Field getPkField(T entity) {
		return getPkField(entity.getClass());
	}
	
	public static <T> Field getPkField(Class<T> entityClass) {
		Field field = null;
		try {
//			field = entityClass.getDeclaredField("id");
			field = HoneyUtil.getField(entityClass, "id");
		} catch (NoSuchFieldException e) {
			String pkName = getPkFieldNameByClass(entityClass);

			boolean hasException = false;
			if ("".equals(pkName)) {
				hasException = true;
			} else if (pkName != null && !pkName.contains(",")) {
				try {
//					field = entityClass.getDeclaredField(pkName);
					field = HoneyUtil.getField(entityClass, pkName);
				} catch (NoSuchFieldException e2) {
					hasException = true;
				}
			} else {
				// DB是否支持联合主键返回?? 不支持
				Logger.warn(
						"Don't support return id value when the primary key more than one field!");
			}
			if (hasException)
				throw new ObjSQLException("Miss id field: the entity no id field!");
		}

		return field;
	}
	
	public static <T> Object getIdValue(T entity) {
		Field field = getPkField(entity);
		Object obj = null;
		try {
			if (field != null) {
				HoneyUtil.setAccessibleTrue(field);
				obj = field.get(entity);
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return obj;
	}
	
	public static <T> boolean hasGenPkAnno(T entity) {
		Field field = getPkField(entity);
		if (field != null) {
			return AnnoUtil.isGenPkAnno(field);
		}
		return false;
	}
	
	public static <T> void revertId(T entity) {
		Field field = null;
		if (OneTimeParameter.isTrue(StringConst.OLD_ID_EXIST)) {
			try {
				Object obj = OneTimeParameter.getAttribute(StringConst.OLD_ID);
				String pkName=(String)OneTimeParameter.getAttribute(StringConst.Primary_Key_Name);
//				field = entity.getClass().getDeclaredField(pkName);
				field = HoneyUtil.getField(entity.getClass(), pkName);
				
				HoneyUtil.setAccessibleTrue(field);
				HoneyUtil.setFieldValue(field, entity, obj);
			} catch (NoSuchFieldException e) {
				throw new ObjSQLException("Miss id field: the entity no id field!");
			} catch (IllegalAccessException e) {
				throw ExceptionHelper.convert(e);
			}
		}
	}
	
	public static <T> void revertId(T entity[]) {
		Field field = null;
		String pkName=(String)OneTimeParameter.getAttribute(StringConst.Primary_Key_Name);  //可能为null 
		if (pkName == null) {
			for (int i = 0; i < entity.length; i++) {
				//用掉
				OneTimeParameter.isTrue(StringConst.OLD_ID_EXIST + i);
				OneTimeParameter.getAttribute(StringConst.OLD_ID + i);
			}
			return; // pkName == null时提前返回
		}
		
		for (int i = 0; i < entity.length; i++) {

			if (OneTimeParameter.isTrue(StringConst.OLD_ID_EXIST+i)) {
				try {
					Object obj = OneTimeParameter.getAttribute(StringConst.OLD_ID+i);
					field = HoneyUtil.getField(entity[i].getClass(), pkName);
					HoneyUtil.setAccessibleTrue(field);
					HoneyUtil.setFieldValue(field, entity[i], obj);
				} catch (NoSuchFieldException e) {
					throw new ObjSQLException("entity[] miss id field: the element in entity[] no id field!");
				} catch (IllegalAccessException e) {
					throw ExceptionHelper.convert(e);
				} catch (Exception e) {
					Logger.error(e.getMessage(),e);
				}
			}
		}
	}
	
	public static <T> void setInitArrayIdByAuto(T entity[]) {
			
			if(entity==null || entity.length<1) return ;
			
//			boolean needGenId = HoneyContext.isNeedGenId(entity[0].getClass());
//			if (!needGenId) return;
			
			boolean hasValue = false;
//			Long v = null;

			Field field0 = null;
			String pkName ="";	
			String pkAlias="";
			boolean isStringField=false;
			boolean hasGenUUIDAnno=false;
			boolean useSeparatorInUUID=false;
			try {
				//V1.11
				boolean noId = false;
				try {
					field0 = HoneyUtil.getField(entity[0].getClass(),"id");
					pkName="id";
				} catch (NoSuchFieldException e) {
					noId = true;
				}
				if (noId) {
					pkName = HoneyUtil.getPkFieldName(entity[0]);
					if("".equals(pkName) || pkName.contains(",")) return ; //just support single primary key.
					field0 = HoneyUtil.getField(entity[0].getClass(),pkName);//fixed 1.17
					pkAlias="("+pkName+")";
				}	
				
				if (field0==null) return; //没有主键,则提前返回
				
				boolean replaceOldValue = HoneyConfig.getHoneyConfig().genid_replaceOldId;
				
				if(field0.isAnnotationPresent(GenId.class)) {
					GenId genId=field0.getAnnotation(GenId.class);
					replaceOldValue=replaceOldValue || genId.override();
				}else if(field0.isAnnotationPresent(GenUUID.class)) {
					GenUUID gen=field0.getAnnotation(GenUUID.class);
					replaceOldValue=replaceOldValue || gen.override();
					hasGenUUIDAnno=true;
					useSeparatorInUUID=gen.useSeparator();
				}else {
					boolean needGenId = HoneyContext.isNeedGenId(entity[0].getClass());
					if (!needGenId) return ;
				}
				
				
				isStringField=field0.getType().equals(String.class);
				if(hasGenUUIDAnno && !isStringField) {
					Logger.warn("Gen UUID as id just support String type field!");
					return ;
				}
				
				
//				if (!field0.getType().equals(Long.class)) {//just set the null Long id field
				if (_ObjectToSQLHelper.errorType(field0)) {//set Long or Integer type id
					Logger.warn("The id"+pkAlias+" field's "+field0.getType()+" is not Long/Integer, can not generate the Long/Integer id automatically!");
					return; 
				}
				
//				if (field.get(entity[0]) != null) return; //即使没值,运行一次后也会有值,下次再用就会重复.而用户又不知道.    //要提醒是被覆盖了。
			
//				boolean replaceOldValue = HoneyConfig.getHoneyConfig().genid_replaceOldId;
				
				int len = entity.length;
				String tableKey = _toTableName(entity[0]);
				long ids[];
				long id =0;
				if (_ObjectToSQLHelper.isInt(field0)) {
					ids = GenIdFactory.getRangeId(tableKey, GenIdFactory.GenType_IntSerialIdReturnLong, len);
					id = ids[0];
				} else if(! hasGenUUIDAnno) {
					ids = GenIdFactory.getRangeId(tableKey, len);
					id = ids[0];
				}
				
				Field field = null;
				for (int i = 0; i < len; id++, i++) {
					if(entity[i]==null) continue;
					hasValue = false;
//					v = null;
					
					field = HoneyUtil.getField(entity[i].getClass(),pkName);
					HoneyUtil.setAccessibleTrue(field);
					Object obj = field.get(entity[i]);
					
					if (obj != null) {
						if (!replaceOldValue) return ;
						hasValue = true;
//						v = (Long) obj;
					}
					
//					OneTimeParameter.setTrueForKey(StringConst.OLD_ID_EXIST+i);
//					OneTimeParameter.setAttribute(StringConst.OLD_ID+i, obj);
					
					HoneyUtil.setAccessibleTrue(field);
					try {
						if (_ObjectToSQLHelper.isInt(field0))
							HoneyUtil.setFieldValue(field, entity[i], (int)id);
						else if(!hasGenUUIDAnno && isStringField) //没有用GenUUID又是String
							HoneyUtil.setFieldValue(field, entity[i], id+"");
						else if(hasGenUUIDAnno && isStringField) //用GenUUID
							HoneyUtil.setFieldValue(field, entity[i], UUID.getId(useSeparatorInUUID));
						else
							HoneyUtil.setFieldValue(field, entity[i], id);
						if (hasValue) {
							Logger.warn(" [ID WOULD BE REPLACED] entity["+i+"] : " + entity[0].getClass() + " 's id field"+pkAlias+" value is " + obj.toString()
									+ " would be replace by " + id);
						}
						
						//fixed bug
						OneTimeParameter.setAttribute(StringConst.Primary_Key_Name, pkName);
						OneTimeParameter.setTrueForKey(StringConst.OLD_ID_EXIST+i);
						OneTimeParameter.setAttribute(StringConst.OLD_ID+i, obj);
					} catch (IllegalAccessException e) {
						throw ExceptionHelper.convert(e);
					}
				}
//				OneTimeParameter.setAttribute(StringConst.Primary_Key_Name, pkName); //bug,在分片批量插入时,多线程下,有可能设置不成功
			
			} catch (NoSuchFieldException e) {
				//is no id field , ignore.
				return;
			} catch (Exception e) {
				Logger.error(e.getMessage(),e);
				return;
			}
	}
	
	public static <T> String getPkFieldName(T entity) {
		if (entity == null) return null;
		return getPkFieldNameByClass(entity.getClass());
	}
	
	/**
	 * 查找有PrimaryKey的字段
	 * @param c
	 * @return
	 */
	public static String getPkFieldNameByClass(Class<?> c) {

		if (c == null) return null;
		String classFullName = c.getName();
		String pkey = HoneyContext.getBeanCustomPKey(classFullName);
		if (pkey != null) return pkey;

		Field field[] = HoneyUtil.getFields(c);
		int len = field.length;
		boolean isFirst = true;

		pkey = "";
		for (int i = 0; i < len; i++) {
//			if (isSkipFieldForMoreTable(field[i])) continue; //JoinTable可以与PrimaryKey合用? 实际不会同时用
			if(isSkipField(field[i]) && !AnnoUtil.isFK(field[i])) continue;  //V2.1.8 是FK,也有可能是Primary
			if (AnnoUtil.isPrimaryKey(field[i])) {
				if (isFirst)
					isFirst = false;
				else
					pkey += ",";
				pkey += field[i].getName();
			}
		} //end for
		
		HoneyContext.addBeanCustomPKey(classFullName,pkey);

		return pkey;
	}
	
	public static String getPlaceholderValue(int size) {
		StringBuffer placeholderValue = new StringBuffer(" (");
		for (int i = 0; i < size; i++) {
			if(i!=0) placeholderValue.append(",");
			placeholderValue.append("?");
		}
		if(size<=0) placeholderValue.append("''");
		placeholderValue.append(")");
		
		return placeholderValue.toString();
	}
	
	private final static String NumberArrayTypes[] = { "[Ljava.lang.Long;",
			"[Ljava.lang.Integer;", "[Ljava.lang.Short;", "[Ljava.lang.Byte;",
			"[Ljava.lang.Number;",
			"[Ljava.lang.Double;", "[Ljava.lang.Float;", 
			"[Ljava.math.BigInteger;","[Ljava.math.BigDecimal;" };
	
	public static boolean isNumberArray(Class<?> c) {
		return isNumberArray0(c);
	}
	
	private static boolean isNumberArray0(Class<?> c) {
		if (c == null) return false;
		for (String type : NumberArrayTypes) {
			if (type.equals(c.getName())) return true;
		}
		return false;
	}

	public static boolean isNumberArray(Object v) {
		if(v==null) return false;
//		if (v ==Class.class) return isNumberArray0((Class<?>)v);
		if (v instanceof Class) isNumberArray0((Class<?>)v);
		
		if (v instanceof Integer[] || v instanceof Long[] 
		 || v instanceof Double[] || v instanceof Float[]
		 || v instanceof Number[] 
		 || v instanceof Short[] || v instanceof Byte[] 
		 || v instanceof BigInteger[] || v instanceof BigDecimal[]
		) {
			return true;
		} else {
			return false;
		}
	}

	
	  /**
     * 判断参数是否为数字
     * @param obj
     * @return 是数字返回true
     */
    public static boolean isNumber(Object obj){
        if(obj instanceof Integer ||
                obj instanceof Long ||
                obj instanceof Short ||
                obj instanceof Byte ||
                obj instanceof Double ||
                obj instanceof Float ||
                obj instanceof BigInteger ||
                obj instanceof BigDecimal
        		){
            return true;
        }
        return false;
    }
	
	/**
	 * 只判断MySQL,MariaDB,Oracle,H2,SQLite,PostgreSQL,SQL Server,Cassandra
	 * @return
	 */
	public static boolean isUpperCaseDB() {
		if (isOracle())
			return true;
		else if (DatabaseConst.H2.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName()))
			return true;

		return false;
	}
	
	public static InterceptorChain copy(InterceptorChain obj) {
		try {
			Serializer jdks = new JdkSerializer();
			return (InterceptorChain) jdks.unserialize(jdks.serialize(obj));
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		return null;
	}
	
	public static <T extends Serializable> Object copyObject(T obj) {
		try {
			Serializer jdks = new JdkSerializer();
			return jdks.unserialize(jdks.serialize(obj));
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		return null;
	}
	
	//exclude union,union all
	public static boolean isNotSupportUnionQuery() {
		return HoneyConfig.getHoneyConfig().sharding_notSupportUnionQuery || HoneyUtil.isSQLite();
	}
	
	public static <T> String getColumnNames(T entity) {
		Field fields[] = HoneyUtil.getFields(entity.getClass());
		String columnNames;
		String packageAndClassName = entity.getClass().getName();
		columnNames = HoneyContext.getBeanField(packageAndClassName);
		if (columnNames == null) {
			columnNames = HoneyUtil.getBeanField(fields,entity.getClass());
			HoneyContext.addBeanField(packageAndClassName, columnNames);
		}
		
		return columnNames;
	}
	
	public static String toTableName(Object entity) {
		if (entity instanceof Class) return _toTableNameByClass((Class<?>) entity); // fixed bug 2.1
		if (entity instanceof String) return _toTableName2((String) entity);// fixed bug 2.1
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
	
	private static String _toTableName2(String entityName) {
		return NameTranslateHandle.toTableName(entityName);
	}
	
	private static String _toTableNameByClass(Class<?> c) {
		return NameTranslateHandle.toTableName(c.getName());
	}
	
	public static void setFieldValue(Field field, Object targetObj, Object value)
			throws IllegalAccessException {
		field.set(targetObj, value); // NOSONAR
	}

	public static void setAccessibleTrue(Field field) {
		field.setAccessible(true); // NOSONAR
	}
	
	public static <T> Field[] getFields(Class<T> entityClass) {
		Field fields[] = entityClass.getDeclaredFields();
		
		if (isOpenEntityCanExtend()) {//entity支持父类时,再添加
			Class<?> superClass = null;
			Class<?> currentClass = entityClass;
			do {
				superClass = currentClass.getSuperclass(); // 拿的是父类，而不是字段的值
				if (HoneyUtil.isSuperEntity(superClass)) {
					currentClass=superClass;
					fields=mergeSuperFields(fields, currentClass);
				} else {
					break;
				}
			} while (fields != null);
		}
		
		if(fields==null) fields=new Field[0];
		
		return fields;
	}
	
	private static <T> Field[] mergeSuperFields(Field oldFields[], Class<T> superClass) {
		Field superFields[] = superClass.getDeclaredFields();
		if (superFields == null || superFields.length == 0) return oldFields;

		int oldFieldsLength = oldFields.length;
		int superFieldsLength = superFields.length;
		Field[] mergedFields = Arrays.copyOf(oldFields, oldFieldsLength + superFieldsLength);
		System.arraycopy(superFields, 0, mergedFields, oldFieldsLength, superFieldsLength);

		return mergedFields;
	}
	
	public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			if (isOpenEntityCanExtend()) {
				Class<?> superClass = clazz.getSuperclass(); // 拿的是父类，而不是字段的值
				if (isSuperEntity(superClass)) {
					return getField(superClass, fieldName);
				}
			}
			throw e;
		}
	}

	public static boolean isSuperEntity(Class<?> superClass) {
		return superClass != null && superClass != Object.class
				&& (!superClass.getName().startsWith("java.") && !superClass.getName().startsWith("javax."));
	}
	
	private static boolean isOpenEntityCanExtend() {
		return HoneyConfig.getHoneyConfig().openEntityCanExtend;
	}
	
	private static char ch[]="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789XY".toCharArray();//64
	private static final String START_MARK = "[$#(";
	private static final String END_MARK= ")#$]";
	private static int index=0;
	private static int MAX=1<<30;
	
	public static String getRandomPrefix() {
		String s = Math.random() + "";
		if (index > MAX)
			index = 0;
		index++;
		try {
			return START_MARK+s.substring(2, 13) + ch[index % 64]+END_MARK; //len 20
		} catch (StringIndexOutOfBoundsException e) { //Exception in thread "main" java.lang.StringIndexOutOfBoundsException: String index out of range: 13
			return getRandomPrefix();
		}
	}
	
}

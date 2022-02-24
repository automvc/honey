package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.annotation.Ignore;
import org.teasoft.bee.osql.annotation.JoinTable;
import org.teasoft.bee.osql.annotation.JoinType;
import org.teasoft.bee.osql.annotation.JustFetch;
import org.teasoft.bee.osql.annotation.PrimaryKey;
import org.teasoft.bee.osql.exception.BeeErrorFieldException;
import org.teasoft.bee.osql.exception.BeeIllegalEntityException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.bee.osql.exception.JoinTableException;
import org.teasoft.bee.osql.exception.JoinTableParameterException;
import org.teasoft.honey.osql.constant.NullEmpty;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.osql.util.PropertiesReader;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyUtil {

	private static final String STRING = "String";
	private static Map<String, String> jdbcTypeMap = new HashMap<>(); 
	private static Map<String, Integer> javaTypeMap = new HashMap<>();

	private static PropertiesReader jdbcTypeCustomProp = new PropertiesReader("/jdbcTypeToFieldType.properties");
	private static PropertiesReader jdbcTypeCustomProp_specificalDB = null;

	static {
		initTypeMapConfig();
	}

	static void refreshTypeMapConfig() {
		initTypeMapConfig();
	}
	
	private HoneyUtil() {}
	
	private static void initTypeMapConfig() {
		String proFileName = "/jdbcTypeToFieldType-{DbName}.properties";
		
		initJdbcTypeMap();
		appendJdbcTypeCustomProp();
		
		String dbName = HoneyConfig.getHoneyConfig().getDbName();
		if (dbName != null) {
			jdbcTypeCustomProp_specificalDB = new PropertiesReader(proFileName.replace("{DbName}", dbName));
			appendJdbcTypeCustomProp_specificalDB();
		}

		initJavaTypeMap();
	}

//	public static int[] mergeArray(int total[], int part[], int start, int end) {
//
//		try {
//			for (int i = 0; i < part.length; i++) {
//				total[start + i] = part[i];
//			}
//		} catch (Exception e) {
//			Logger.error(" HoneyUtil.mergeArray() " + e.getMessage());
//		}
//
//		return total;
//	}

	static String getBeanField(Field field[]) {
		if (field == null) return "";
		StringBuffer s = new StringBuffer();
		int len = field.length;
		boolean isFirst = true;

		for (int i = 0; i < len; i++) {
			if(isSkipField(field[i])) continue;
			if (isFirst) {
				isFirst = false;
			} else {
				s.append(",");
			}

			if(field[i].isAnnotationPresent(JustFetch.class)) {
				s.append(getJustFetchColumn(field[i]));
			}else {
			   s.append(NameTranslateHandle.toColumnName(field[i].getName()));
			}
			
		}
		return s.toString();
	}
	
	private static String getJustFetchColumn(Field field) {
		
		String expression = getJustFetchDefineName(field);
		String c = "";
		String fName = NameTranslateHandle.toColumnName(field.getName());
		if (isSQLite()) {
			c = expression + K.as + fName;
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
		}else {//供SqlLib,多表查询使用
			moreTableStruct=(MoreTableStruct[])OneTimeParameter.getAttribute(key);
		}

		return moreTableStruct;
	}
	
	private static <T> MoreTableStruct[] _getMoreTableStructAndCheckBefore(T entity) {

		if (entity == null) return null;

		String entityFullName = entity.getClass().getName();
		
		Field field[] = entity.getClass().getDeclaredFields();

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
				if(field[i].getType().isAssignableFrom(List.class)) {
					try {
					field[i].setAccessible(true);
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
			
			mailField=NameTranslateHandle.toColumnName(field[i].getName());
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
			
			subColumnStringBuffer[0] = _getBeanFullField_0(subField[0], useSubTableName,entityFullName,mainFieldSet,dulMap,true);
			
		}else if(subEntityFieldNum==1 && subOneIsList) { //从表1是List类型
			
			String t_subAlias = joinTable[0].subAlias();
			String useSubTableName;
			if (StringUtils.isNotBlank(t_subAlias)) {
				useSubTableName = t_subAlias;
			} else {
				subTableName[0]=_toTableNameByEntityName(list_T_classOne.getName()); 
				useSubTableName = subTableName[0];
			}
			Field ff[]=list_T_classOne.getDeclaredFields();
			subColumnStringBuffer[0] = _getBeanFullField_0(ff, useSubTableName,entityFullName,mainFieldSet,dulMap,true,list_T_classOne.getName());
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
			if (oneHasOne && subField[1].getType().isAssignableFrom(List.class)) {
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
							subField[1].setAccessible(true);
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
					subField[j].setAccessible(true);
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
						moreTableStruct[1 + j].subObject = subField[j].get(entity);
					}
				} catch (IllegalAccessException e) {
					throw ExceptionHelper.convert(e);
				}

				if(subEntityFieldNum==1){ //subEntityFieldNum==1 只有一个从表,从表1上面都有扫描, 表示上面都有扫描过
					                       //subEntityFieldNum==1  j也不可以等于1(不可能进行两次循环)
			    }else if(j==0 && subOneIsList && !oneHasOne) { //从主表来的 从表1 List (主表有两个从表时)         
					Field ff[]=list_T_classOne.getDeclaredFields();
					subColumnStringBuffer[0] = _getBeanFullField_0(ff, useSubTableName,entityFullName,mainFieldSet,dulMap,true,list_T_classOne.getName());
				}else if(j==1 && subTwoIsList) { //j==1,表示有第二个存在
				   Field ff[]=list_T_classTwo.getDeclaredFields();
				   subColumnStringBuffer[1] = _getBeanFullField_0(ff, useSubTableName,entityFullName,mainFieldSet,dulMap,true,list_T_classTwo.getName());
			    }else if(!oneHasOne || j==1) { //上面没查， 这里要实现
//				}else {//有些不需要处理, 如oneHasOne,i==0
			    	subColumnStringBuffer[j] = _getBeanFullField_0(subField[j], useSubTableName,entityFullName,mainFieldSet,dulMap);
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
	static StringBuffer _getBeanFullField_0(Field entityField, String tableName,String entityFullName,
			Set<String> mainFieldSet,Map<String,String> dulMap) {
		return _getBeanFullField_0(entityField, tableName, entityFullName, mainFieldSet, dulMap,false);
	}

	
	static StringBuffer _getBeanFullField_0(Field entityField, String tableName,String entityFullName,
			Set<String> mainFieldSet,Map<String,String> dulMap,boolean checkOneHasOne) {
		
		Field field[] = entityField.getType().getDeclaredFields();
		
	 return	_getBeanFullField_0(field, tableName, entityFullName, mainFieldSet, dulMap, checkOneHasOne, entityField.getName());
	}
	//for moreTable
	static StringBuffer _getBeanFullField_0(Field field[], String tableName,String entityFullName,
			Set<String> mainFieldSet,Map<String,String> dulMap,boolean checkOneHasOne,String entityFieldFullName) {

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
				   if(checkOneHasOne) WarnMsglist.add("Annotation JoinTable field: " +entityFieldFullName+"(in "+ entityFullName + ") still include JoinTable field:" + field[i].getName() + "(will be ignored)!");
				   else Logger.warn("Annotation JoinTable field: " +entityFieldFullName+"(in "+ entityFullName + ") still include JoinTable field:" + field[i].getName() + "(will be ignored)!");
//				}
				continue;
			}

			if (isFirst) {
				isFirst = false;
			} else {
				columns.append(",");
			}
			subColumnName=NameTranslateHandle.toColumnName(field[i].getName());
			
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
	
	/*	static boolean isNumberType(Field field){
			if (
				(field.getType() == Integer.class)|| (field.getType() == Long.class)
			  ||(field.getType() == Short.class) || (field.getType() == Byte.class)
			  ||(field.getType() == Double.class)|| (field.getType() == Float.class)
			  ||(field.getType() == BigInteger.class)||(field.getType() == BigDecimal.class)
			  )  return true;
			else return false;
		}*/

	/**
	 * jdbc type->java type
	 * 将jdbc的数据类型转换为java的类型 
	 * @param jdbcType
	 * @return the string of java type
	 */
	public static String getFieldType(String jdbcType) {

		String javaType = jdbcTypeMap.get(jdbcType);

		if (javaType != null) return javaType;

		if (null == jdbcTypeMap.get(jdbcType)) {

			//fix UNSIGNED,  like :TINYINT UNSIGNED 
			String tempType = jdbcType.trim();
			if (tempType.endsWith(" UNSIGNED")) {
				int i = tempType.indexOf(" ");
				javaType = jdbcTypeMap.get(tempType.substring(0, i));
				if (javaType != null) return javaType;
			}
			
			if (javaType == null){
				javaType =jdbcTypeMap.get(jdbcType.toLowerCase());
				if (javaType != null) return javaType;
				
				if (javaType == null){
					javaType =jdbcTypeMap.get(jdbcType.toUpperCase());
					if (javaType != null) return javaType;
				}
			}
			
			javaType = "[UNKNOWN TYPE]" + jdbcType;
		}

		return javaType;
	}

	private static void initJdbcTypeMap() {

		//url: https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html
		//		https://docs.oracle.com/javadb/10.8.3.0/ref/rrefjdbc20377.html
		//		https://docs.oracle.com/javase/8/docs/api/java/sql/package-summary.html
		jdbcTypeMap.put("CHAR", STRING);
		jdbcTypeMap.put("VARCHAR", STRING);
		jdbcTypeMap.put("LONGVARCHAR", STRING);

		jdbcTypeMap.put("NVARCHAR", STRING);
		jdbcTypeMap.put("NCHAR", STRING);

		jdbcTypeMap.put("NUMERIC", "BigDecimal");
		jdbcTypeMap.put("DECIMAL", "BigDecimal");

		jdbcTypeMap.put("BIT", "Boolean");

		//rs.getObject(int index)  bug   
		//pst.setByte(i+1,(Byte)value); break;设置查询没问题,结果也能返回,用rs.getObject拿结果时才报错
		jdbcTypeMap.put("TINYINT", "Byte");
		jdbcTypeMap.put("SMALLINT", "Short");

		jdbcTypeMap.put("INT", "Integer");
		jdbcTypeMap.put("INTEGER", "Integer");

		jdbcTypeMap.put("BIGINT", "Long");
		jdbcTypeMap.put("REAL", "Float");
		jdbcTypeMap.put("FLOAT", "Float"); //notice: mysql在创表时,要指定float的小数位数,否则查询时不能用=精确查询
		jdbcTypeMap.put("DOUBLE", "Double");

		jdbcTypeMap.put("BINARY", "byte[]");
		jdbcTypeMap.put("VARBINARY", "byte[]");
		jdbcTypeMap.put("LONGVARBINARY", "byte[]");
		
		jdbcTypeMap.put("image","byte[]");

		jdbcTypeMap.put("DATE", "Date");
		jdbcTypeMap.put("TIME", "Time");
		jdbcTypeMap.put("TIMESTAMP", "Timestamp");

		jdbcTypeMap.put("CLOB", "Clob");
		jdbcTypeMap.put("BLOB", "Blob");
		jdbcTypeMap.put("ARRAY", "Array");

		jdbcTypeMap.put("NCLOB", "java.sql.NClob");//JDK6
		jdbcTypeMap.put("ROWID", "java.sql.RowId"); //JDK6
		jdbcTypeMap.put("SQLXML", "java.sql.SQLXML"); //JDK6

		// JDBC 4.2 JDK8
		jdbcTypeMap.put("TIMESTAMP_WITH_TIMEZONE", "Timestamp");
		jdbcTypeMap.put("TIMESTAMP WITH TIME ZONE", "Timestamp"); //test in oralce 11g
		jdbcTypeMap.put("TIMESTAMP WITH LOCAL TIME ZONE", "Timestamp");//test in oralce 11g

		String dbName = HoneyConfig.getHoneyConfig().getDbName();

		if (DatabaseConst.MYSQL.equalsIgnoreCase(dbName) || DatabaseConst.MariaDB.equalsIgnoreCase(dbName)) {
			jdbcTypeMap.put("MEDIUMINT", "Integer");
//			jdbcTypeMap.put("DATETIME", "Date");
			jdbcTypeMap.put("DATETIME", "Timestamp");//fix on 2019-01-19
			jdbcTypeMap.put("TINYBLOB", "Blob");
			jdbcTypeMap.put("MEDIUMBLOB", "Blob");
			jdbcTypeMap.put("LONGBLOB", "Blob");
			jdbcTypeMap.put("YEAR", "Integer"); //todo 
			
			jdbcTypeMap.put("TINYINT", "Byte");
			jdbcTypeMap.put("SMALLINT", "Short");
			jdbcTypeMap.put("TINYINT UNSIGNED", "Short");
			jdbcTypeMap.put("SMALLINT UNSIGNED", "Integer");

			jdbcTypeMap.put("INT UNSIGNED", "Long");
			jdbcTypeMap.put("BIGINT UNSIGNED", "BigInteger");
		} else if (DatabaseConst.ORACLE.equalsIgnoreCase(dbName)) {
//			https://docs.oracle.com/cd/B12037_01/java.101/b10983/datamap.htm
//			https://docs.oracle.com/cd/B19306_01/java.102/b14188/datamap.htm
			jdbcTypeMap.put("LONG", STRING);
			jdbcTypeMap.put("VARCHAR2", STRING);
			jdbcTypeMap.put("NVARCHAR2", STRING);
			jdbcTypeMap.put("NUMBER", "BigDecimal"); //oracle todo
			jdbcTypeMap.put("RAW", "byte[]");

			jdbcTypeMap.put("INTERVALYM", STRING); //11g 
			jdbcTypeMap.put("INTERVALDS", STRING); //11g
			jdbcTypeMap.put("INTERVAL YEAR TO MONTH", STRING); //just Prevention
			jdbcTypeMap.put("INTERVAL DAY TO SECOND", STRING);//just Prevention
//			jdbcTypeMap.put("TIMESTAMP", "Timestamp");   exist in comm

		} else if (DatabaseConst.SQLSERVER.equalsIgnoreCase(dbName)) {
//			jdbcTypeMap.put("SMALLINT", "Short");  //comm
			jdbcTypeMap.put("TINYINT", "Short");
//			jdbcTypeMap.put("TIME","java.sql.Time");  exist in comm
//			 DATETIMEOFFSET // SQL Server 2008  microsoft.sql.DateTimeOffset
			jdbcTypeMap.put("DATETIMEOFFSET", "microsoft.sql.DateTimeOffset");
			jdbcTypeMap.put("microsoft.sql.Types.DATETIMEOFFSET", "microsoft.sql.DateTimeOffset");
			
			jdbcTypeMap.put("datetime","Timestamp");
			jdbcTypeMap.put("money","BigDecimal");
			jdbcTypeMap.put("smallmoney","BigDecimal");
			
			jdbcTypeMap.put("ntext",STRING);
			jdbcTypeMap.put("text",STRING);
			jdbcTypeMap.put("xml",STRING);
			
			jdbcTypeMap.put("smalldatetime","Timestamp");
			jdbcTypeMap.put("uniqueidentifier",STRING);
			
			jdbcTypeMap.put("hierarchyid","byte[]");
			jdbcTypeMap.put("image","byte[]");
			
		} else if (DatabaseConst.PostgreSQL.equalsIgnoreCase(dbName)) {	

			jdbcTypeMap.put("bigint","Long");
			jdbcTypeMap.put("int8","Long");
			jdbcTypeMap.put("bigserial","Long");
			jdbcTypeMap.put("serial8","Long");

			jdbcTypeMap.put("integer","Integer");
			jdbcTypeMap.put("int","Integer");
			jdbcTypeMap.put("int4","Integer");
			
			jdbcTypeMap.put("serial","Integer");
			jdbcTypeMap.put("serial4","Integer");
			
			jdbcTypeMap.put("smallint","Short");
			jdbcTypeMap.put("int2","Short");
			jdbcTypeMap.put("smallserial","Short");
			jdbcTypeMap.put("serial2","Short");

			jdbcTypeMap.put("money", "BigDecimal");
			jdbcTypeMap.put("numeric", "BigDecimal");
			jdbcTypeMap.put("decimal", "BigDecimal");
			
			jdbcTypeMap.put("bit",STRING);
			jdbcTypeMap.put("bit varying",STRING);
			jdbcTypeMap.put("varbit",STRING);
			jdbcTypeMap.put("character",STRING);
			jdbcTypeMap.put("char",STRING);
			jdbcTypeMap.put("character varying",STRING);
			jdbcTypeMap.put("varchar",STRING);
			jdbcTypeMap.put("text",STRING);
			jdbcTypeMap.put("bpchar",STRING);//get from JDBC

			jdbcTypeMap.put("boolean","Boolean");
			jdbcTypeMap.put("bool","Boolean");
			
			jdbcTypeMap.put("double precision","Double"); //prevention
			jdbcTypeMap.put("float8","Double");

			jdbcTypeMap.put("real","Float");
			jdbcTypeMap.put("float4","Float");

//			jdbcTypeMap.put("cidr","
//			jdbcTypeMap.put("inet ","
//			jdbcTypeMap.put("macaddr","
//			jdbcTypeMap.put("macaddr8","

			jdbcTypeMap.put("json",STRING);  //
//			jdbcTypeMap.put("jsonb","

			jdbcTypeMap.put("bytea","byte[]");  //

			jdbcTypeMap.put("date","Date");
//			jdbcTypeMap.put("interval","
			jdbcTypeMap.put("time","Time");
			jdbcTypeMap.put("timestamp","Timestamp");

			jdbcTypeMap.put("time without time zone","Time");
			jdbcTypeMap.put("timetz","Time");
			jdbcTypeMap.put("timestamp without time zone","Timestamp");
			jdbcTypeMap.put("timestamptz","Timestamp");

		} else if (DatabaseConst.H2.equalsIgnoreCase(dbName) 
			    || DatabaseConst.SQLite.equalsIgnoreCase(dbName)) {
			jdbcTypeMap.put("MEDIUMINT", "Integer");
			jdbcTypeMap.put("INT4", "Integer");
			jdbcTypeMap.put("INT2", "Short");
			jdbcTypeMap.put("INT8", "Long");
			
			jdbcTypeMap.put("NUMBER", "BigDecimal");
			jdbcTypeMap.put("NUMERIC", "BigDecimal");

			jdbcTypeMap.put("BOOLEAN", "Boolean");
			jdbcTypeMap.put("BOOL", "Boolean");
			jdbcTypeMap.put("BIT", "Boolean");

			jdbcTypeMap.put("FLOAT8", "Double");
			jdbcTypeMap.put("FLOAT4 ", "Float");

			jdbcTypeMap.put("CHARACTER", STRING);
			jdbcTypeMap.put("VARCHAR2", STRING);
			jdbcTypeMap.put("NVARCHAR2", STRING);
			jdbcTypeMap.put("VARCHAR_IGNORECASE", STRING);
		} 
		
//		else if (DatabaseConst.H2.equalsIgnoreCase(dbName)) {  // can not use elseif again.
		if (DatabaseConst.H2.equalsIgnoreCase(dbName)) {
			
			//	/h2/docs/html/datatypes.html#real_type
			jdbcTypeMap.put("SIGNED", "Integer");
			jdbcTypeMap.put("DEC", "BigDecimal");
			jdbcTypeMap.put("YEAR", "Byte");
			jdbcTypeMap.put("BINARY VARYING", "byte[]");
			jdbcTypeMap.put("WITHOUT TIME ZONE", "Time");
			
			jdbcTypeMap.put("BINARY LARGE OBJECT","Blob");     //java.sql.Blob
			jdbcTypeMap.put("CHARACTER LARGE OBJECT","Clob");  //java.sql.Clob
			
			jdbcTypeMap.put("CHARACTER VARYING",STRING); 
			jdbcTypeMap.put("VARCHAR_CASESENSITIVE",STRING); 
			jdbcTypeMap.put("VARCHAR_IGNORECASE",STRING); 
			
		}else if (DatabaseConst.SQLite.equalsIgnoreCase(dbName)) {
			
			jdbcTypeMap.put("VARYING CHARACTER", STRING);
			jdbcTypeMap.put("NATIVE CHARACTER", STRING);
			jdbcTypeMap.put("TEXT", STRING);
			jdbcTypeMap.put("DOUBLE PRECISION", "Double");
			
			jdbcTypeMap.put("DATETIME", STRING);
			jdbcTypeMap.put("INTEGER", "Long");  // INTEGER  PRIMARY key
			
			jdbcTypeMap.put("UNSIGNED BIG INT", "Long");
			
			jdbcTypeMap.put("VARYING", STRING);
		}

	}

	private static void appendJdbcTypeCustomProp() {
		for (String s : jdbcTypeCustomProp.getKeys()) {
			jdbcTypeMap.put(s, jdbcTypeCustomProp.getValue(s));
		}
	}

	private static void appendJdbcTypeCustomProp_specificalDB() {
		for (String s : jdbcTypeCustomProp_specificalDB.getKeys()) {
			jdbcTypeMap.put(s, jdbcTypeCustomProp_specificalDB.getValue(s));
		}
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
//		char  TODO

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

	}

	public static int getJavaTypeIndex(String javaType) {
		//    	return javaTypeMap.get(javaTypeMap)==null?-1:javaTypeMap.get(javaTypeMap);
		return javaTypeMap.get(javaType) == null ? -1 : javaTypeMap.get(javaType);
	}

	/**
	 * 首字母转换成大写
	 */
	public static String firstLetterToUpperCase(String str) {
		return NameUtil.firstLetterToUpperCase(str);
	}

	static boolean isContinue(int includeType, Object object, Field field) {
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
			if (field.isAnnotationPresent(Ignore.class)) return true; //v1.9
			if (field.isAnnotationPresent(JoinTable.class)) return true;
			if (field.isSynthetic()) return true;
		}
		return false;
	}
	
	static boolean isSkipFieldForMoreTable(Field field) {
		if (field != null) {
			if ("serialVersionUID".equals(field.getName())) return true;
			if (field.isAnnotationPresent(Ignore.class)) return true; //v1.9
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
	static void setPreparedValues(PreparedStatement pst, int objTypeIndex, int i, Object value) throws SQLException {

		if (null == value) {
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
			case 11:
				pst.setDate(i + 1, (Date) value);
				break;
			case 12:
				pst.setTime(i + 1, (Time) value);
				break;
			case 13:
				pst.setTimestamp(i + 1, (Timestamp) value);
				break;
			case 14:
				pst.setBlob(i + 1, (Blob) value);
				break;
			case 15:
				pst.setClob(i + 1, (Clob) value);
				break;
			case 16:
				pst.setNClob(i + 1, (NClob) value);
				break;
			case 17:
				pst.setRowId(i + 1, (RowId) value);
				break;
			case 18:
				pst.setSQLXML(i + 1, (SQLXML) value);
				break;
			case 19:
				//	        	pst.setBigInteger(i+1, (BigInteger)value);break;
			default:
				pst.setObject(i + 1, value);
		} //end switch
	}

	static Object getResultObject(ResultSet rs, String typeName, String columnName) throws SQLException {

		int k = HoneyUtil.getJavaTypeIndex(typeName);

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
			case 19:
				//	        	no  getBigInteger
			default:
				return rs.getObject(columnName);
		} //end switch

	}

	static Object getResultObjectByIndex(ResultSet rs, String typeName, int index) throws SQLException {

		int k = HoneyUtil.getJavaTypeIndex(typeName);

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
				return rs.getDate(index);
			case 12:
				return rs.getTime(index);
			case 13:
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
			case 19:
				//	        	no  getBigInteger
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
		String new_sql = sql.trim();
		if (new_sql.endsWith(";")) return new_sql.substring(0, new_sql.length() - 1); //fix oracle ORA-00911 bug.oracle用jdbc不能有分号
		return sql;
	}

	public static <T> void checkPackage(T entity) {
		if (entity == null) return;
		
//		if(entity.getClass().getPackage()==null) return ; //2020-04-19 if it is default package or empty package, do not check. Suggest by:pcode
//		String packageName = entity.getClass().getPackage().getName();
		
		String classFullName=entity.getClass().getName();
		//		传入的实体可以过滤掉常用的包开头的,如:java., javax. ; 但spring开头不能过滤,否则spring想用bee就不行了.
		if (classFullName.startsWith("java.") || classFullName.startsWith("javax.")) {
			throw new BeeIllegalEntityException("BeeIllegalEntityException: Illegal Entity, " + entity.getClass().getName());
		}
	}

	//将非null的字段值以Map形式返回
	public static <T> Map<String, Object> getColumnMapByEntity(T entity) {
		Map<String, Object> map = new HashMap<>();
		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		try {
			for (int i = 0; i < len; i++) {
				fields[i].setAccessible(true);
//				if (fields[i].get(entity) == null || "serialVersionUID".equals(fields[i].getName()) || fields[i].isSynthetic() || fields[i].isAnnotationPresent(JoinTable.class)) {
				if (fields[i].get(entity) == null || isSkipField(fields[i])) {
					continue;
				} else {
					map.put(_toColumnName(fields[i].getName()), fields[i].get(entity));
				}
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return map;
	}
	
	//List<PreparedValue>  to valueBuffer
	public static String list2Value(List<PreparedValue> list,boolean needType){
		StringBuffer b=new StringBuffer();
		if(list==null ) return null;
		if(list.size()==0) return "";
		
		String type="";
		
		int size=list.size();
		for (int j = 0; j < size; j++) {
			b.append(list.get(j).getValue());
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
	 * @return
	 */
	public static String getExecutableSql(String sql, List<PreparedValue> list){
		if(list==null || list.size()==0) return sql;
		
		int size=list.size();
		Object value=null;
		for (int j = 0; j < size; j++) {
			value=list.get(j).getValue();
			if(value==null || value instanceof Number){  //v1.8.15    Null no need ' and '
				sql=sql.replaceFirst("\\?", String.valueOf(value));
			}else{
//				sql=sql.replaceFirst("\\?", "'"+String.valueOf(value)+"'");
				sql=sql.replaceFirst("\\?", "'"+String.valueOf(value).replace("$", "\\$")+"'"); //bug 2021-05-25
			}
		}
		
		return sql;
	}
	
	static <T> String checkAndProcessSelectField(T entity, String fieldList) {

		if (fieldList == null) return null;
		
		String packageAndClassName=entity.getClass().getName();
		String columnsdNames=HoneyContext.getBeanField(packageAndClassName);
		if (columnsdNames == null) {
			Field fields[]=entity.getClass().getDeclaredFields();
			columnsdNames=HoneyUtil.getBeanField(fields);//获取属性名对应的DB字段名
			HoneyContext.addBeanField(packageAndClassName, columnsdNames);
		}

		return checkAndProcessSelectFieldViaString(columnsdNames, fieldList, null);
	}
	 
	 static String checkAndProcessSelectFieldViaString(String columnsdNames,String fieldList,Map<String,String> subDulFieldMap){
			
		if(fieldList==null) return null;
		 
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
		String selectFields[] = fieldList.split(",");
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
			if(!(  
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

		if (!"".equals(errorField)) throw new BeeErrorFieldException("ErrorField: " + errorField);
		
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

	private static String checkJoinTable(JoinTable joinTable) {
		String mainField= joinTable.mainField();
		String subField=joinTable.subField();
		
		String subAlias=joinTable.subAlias();
		String subClass=joinTable.subClass();
		
		if (NameCheckUtil.isIllegal(mainField)) {
			throw new JoinTableParameterException("Annotation JoinTable set wrong value in mainField:" + mainField);
		}
		if (NameCheckUtil.isIllegal(subField)) {
			throw new JoinTableParameterException("Annotation JoinTable set wrong value in subField:" + subField);
		}
		if (NameCheckUtil.isIllegal(subAlias)) {
			throw new JoinTableParameterException("Annotation JoinTable set wrong value in subAlias:" + subAlias);
		}
		if (NameCheckUtil.isIllegal(subClass)) {
			throw new JoinTableParameterException("Annotation JoinTable set wrong value in subClass:" + subClass);
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
	
	//oracle,SQLite
	public static boolean isConfuseDuplicateFieldDB(){
		return DatabaseConst.ORACLE.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())
			|| DatabaseConst.SQLite.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())
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
	
	public static void setPageNum(List<PreparedValue> list) {
		int array[] = (int[]) OneTimeParameter.getAttribute("_SYS_Bee_Paing_NumArray");
		for (int i = 0; array != null && i < array.length; i++) {
			PreparedValue p = new PreparedValue();
			p.setType("Integer");
			p.setValue(array[i]);
			if (HoneyUtil.isSqlServer()) { //top n
				list.add(0, p);
			} else { //default the page num in the last.
				list.add(p);
			}
		}
	}

	public static boolean isRegPagePlaceholder() {
	    return OneTimeParameter.isTrue("_SYS_Bee_Paing_Placeholder");
	}

	public static void regPagePlaceholder() {
	    if(isSqlServer()) return ;
		OneTimeParameter.setTrueForKey("_SYS_Bee_Paing_Placeholder");
	}

	public static void regPageNumArray(int array[]) {
		OneTimeParameter.setAttribute("_SYS_Bee_Paing_NumArray", array);
	}
	
	public static boolean isSqlKeyWordUpper() {
		String kwCase = HoneyConfig.getHoneyConfig().sqlKeyWordCase;
		return "upper".equalsIgnoreCase(kwCase) ? true : false;
	}
	
	public static <T> Object getIdValue(T entity) {
		Field field = null;
		Object obj = null;
		try {
			field = entity.getClass().getDeclaredField("id");
		} catch (NoSuchFieldException e) {
			String pkName = getPkFieldName(entity);
			
			boolean hasException = false;
			if ("".equals(pkName)) {
				hasException = true;
			} else if (pkName!=null && !pkName.contains(",")){
				try {
					field = entity.getClass().getDeclaredField(pkName);
				} catch (NoSuchFieldException e2) {
					hasException = true;
				}
			}else {
				//DB是否支持联合主键返回??    不支持
				Logger.warn("Don't support return id value when the primary key more than one field!");
			}
			if (hasException) throw new ObjSQLException("Miss id field: the entity no id field!");
		}

		try {
			if (field != null) {
				field.setAccessible(true);
				obj = field.get(entity);
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return obj;
	}
	
	public static <T> void revertId(T entity) {
		Field field = null;
		if (OneTimeParameter.isTrue(StringConst.OLD_ID_EXIST)) {
			try {
				Object obj = OneTimeParameter.getAttribute(StringConst.OLD_ID);
				String pkName=(String)OneTimeParameter.getAttribute(StringConst.Primary_Key_Name);
				field = entity.getClass().getDeclaredField(pkName);
				field.setAccessible(true);
				field.set(entity, obj);
			} catch (NoSuchFieldException e) {
				throw new ObjSQLException("Miss id field: the entity no id field!");
			} catch (IllegalAccessException e) {
				throw ExceptionHelper.convert(e);
			}
		}
	}
	
	public static <T> void revertId(T entity[]) {
		Field field = null;
		String pkName=(String)OneTimeParameter.getAttribute(StringConst.Primary_Key_Name);
		for (int i = 0; i < entity.length; i++) {

			if (OneTimeParameter.isTrue(StringConst.OLD_ID_EXIST+i)) {
				try {
					Object obj = OneTimeParameter.getAttribute(StringConst.OLD_ID+i);
					field = entity[i].getClass().getDeclaredField(pkName);
					field.setAccessible(true);
					field.set(entity[i], obj);
				} catch (NoSuchFieldException e) {
					throw new ObjSQLException("entity[] miss id field: the element in entity[] no id field!");
				} catch (IllegalAccessException e) {
					throw ExceptionHelper.convert(e);
				}
			}
		}
	}
	
	static <T> String getPkFieldName(T entity) {
		if (entity == null) return null;
		return getPkFieldNameByClass(entity.getClass());
	}
	
	@SuppressWarnings("rawtypes")
	static String getPkFieldNameByClass(Class c) {

		if (c == null) return null;
		String classFullName = c.getName();
		String pkey = HoneyContext.getBeanCustomPKey(classFullName);
		if (pkey != null) return pkey;

		Field field[] = c.getDeclaredFields();
		int len = field.length;
		boolean isFirst = true;

		pkey = "";
		for (int i = 0; i < len; i++) {
//			if (isSkipFieldForMoreTable(field[i])) continue; //JoinTable可以与PrimaryKey合用? 实际不会同时用
			if(isSkipField(field[i])) continue;
			if (field[i].isAnnotationPresent(PrimaryKey.class)) {
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

}

package org.teasoft.honey.osql.autogen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.Check;
import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.osql.core.SessionFactory;
import org.teasoft.honey.osql.mongodb.MongodbComm;
import org.teasoft.honey.osql.mongodb.MongodbCommRegister;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.util.DateUtil;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * 生成Javabean.Generate Javabean.
 * @author Kingstar
 * @since 1.0
 */
@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
public class GenBean {

	private GenConfig config;
	private String LINE_SEPARATOR = System.getProperty("line.separator"); // 换行符
	// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private boolean isNeedKeyColumn = false;

	private static boolean printOverrideSetTip = true;

	public GenBean() {
		this.config = new GenConfig();
	}

	public GenBean(GenConfig config) {
		this.config = config;
	}

	// 生成指定表对象对应的类文件
	private boolean genBeanFile(TableBean tableBean) {
		String tableName = tableBean.getTableName();
		List<String> columnNames = tableBean.getColumnNames();
		List<String> columnTypes = tableBean.getColumnTypes(); // jdbcType
		Map<String, String> commentMap = tableBean.getCommentMap();
		// 表名对应的实体类名
		String entityName = "";
		entityName = NameTranslateHandle.toEntityName(tableName);

		entityName = NameUtil.firstLetterToUpperCase(entityName);// 确保类名首字母大写.

		if (config.getEntityNamePre() != null) entityName = config.getEntityNamePre() + entityName;

		String tableComment = "";
		if ((config.isGenComment() || config.isGenSwagger()) && commentMap != null)
			tableComment = commentMap.get(tableBean.getTableName());

		String authorComment = "/**" + LINE_SEPARATOR;
		if (config.isGenComment() && StringUtils.isNotBlank(tableComment))
			authorComment += " * " + tableComment + LINE_SEPARATOR;
		// authorComment+="*@author Bee"+ LINE_SEPARATOR;
		authorComment += " * @author Honey" + LINE_SEPARATOR;
		// authorComment+="*Create on "+format.format(new Date())+ LINE_SEPARATOR;
		authorComment += " * Create on " + DateUtil.currentDate() + LINE_SEPARATOR; // v1.7.2
		authorComment += " */";

		String packageStr = "package " + config.getPackagePath() + ";" + LINE_SEPARATOR;

		String importStr = "";

		// 生成无参构造方法
		// String constructorStr = "\t" + "public " + entityName + "() {}"+ LINE_SEPARATOR;
		String propertyName = ""; // 属性名
		// 生成私有属性和get,set方法
		String propertiesStr = ""; // 私有属性字符串
		String getsetStr = ""; // get、set方法字符串
		String getsetProNameStr = "";
		String javaType = ""; // 数据库对应的java类型

		boolean bigIntegerFlag = true;
		boolean bigDecimalFlag = true;
		boolean dateFlag = true;
		boolean timeFlag = true;
		boolean timestampFlag = true;
		boolean localDateTimeFlag = true; // 2.4.0
		boolean localDateFlag = true; // 2.4.0
		boolean localTimeFlag = true; // 2.4.0
		boolean blobFlag = true;
		boolean clobFlag = true;
		boolean arrayFlag = true;

		boolean listFlag = true;
		boolean setFlag = true;
		boolean mapFlag = true;

		boolean nClobFlag = true;
		boolean rowIdFlag = true;
		boolean sqlxmlFlag = true;

		TreeSet<String> importSet = new TreeSet<>();

		StringBuilder tostr = new StringBuilder();

		if (config.isGenSerializable()) {
			importSet.add("import java.io.Serializable;");
		}
		List<Boolean> ynNulls = tableBean.getYnNulls();

		for (int i = 0; i < columnNames.size(); i++) {
			String columnName = columnNames.get(i);
			String columnType = columnTypes.get(i);
			String comment = "";
			String getOrIs = "get";
			String unknownTypeTip = "";

			propertyName = NameTranslateHandle.toFieldName(columnName);

			getsetProNameStr = HoneyUtil.firstLetterToUpperCase(propertyName);
			javaType = HoneyUtil.getFieldType(columnType);

			// V1.17
			if ("id".equalsIgnoreCase(propertyName) && "BigDecimal".equalsIgnoreCase(javaType)) {
				javaType = "Long";
			}

			// import
			if ("BigDecimal".equals(javaType) && bigDecimalFlag) {
				importSet.add("import java.math.BigDecimal;");
				bigDecimalFlag = false;
			} else if ("BigInteger".equals(javaType) && bigIntegerFlag) {
				importSet.add("import java.math.BigInteger;");
				bigIntegerFlag = false;
			} else if ("Date".equals(javaType) && dateFlag) {
				importSet.add("import java.sql.Date;");
				dateFlag = false;
			} else if ("Time".equals(javaType) && timeFlag) {
				importSet.add("import java.sql.Time;");
				timeFlag = false;
			} else if ("Timestamp".equals(javaType) && timestampFlag) {
				importSet.add("import java.sql.Timestamp;");
				timestampFlag = false;
			} else if ("LocalDateTime".equals(javaType) && localDateTimeFlag) {
				importSet.add("import java.time.LocalDateTime;");
				localDateTimeFlag = false;
			} else if ("LocalDate".equals(javaType) && localDateFlag) {
				importSet.add("import java.time.LocalDate;");
				localDateFlag = false;
			} else if ("LocalTime".equals(javaType) && localTimeFlag) {
				importSet.add("import java.time.LocalTime;");
				localTimeFlag = false;
			} else if ("Blob".equals(javaType) && blobFlag) {
				importSet.add("import java.sql.Blob;");
				blobFlag = false;
			} else if ("Clob".equals(javaType) && clobFlag) {
				importSet.add("import java.sql.Clob;");
				clobFlag = false;
			} else if ("Array".equals(javaType) && arrayFlag) {
				importSet.add("import java.sql.Array;");
				arrayFlag = false;
			} else if ("List".equals(javaType) && listFlag) {
				importSet.add("import java.util.List;");
				listFlag = false;
			} else if ("Set".equals(javaType) && setFlag) {
				importSet.add("import java.util.Set;");
				setFlag = false;
			} else if ("Map".equals(javaType) && mapFlag) {
				importSet.add("import java.util.Map;");
				mapFlag = false;
			} else if ("NCLOB".equals(javaType) && nClobFlag) {
				importSet.add("import java.sql.NClob;");
				nClobFlag = false;
			} else if ("ROWID".equals(javaType) && rowIdFlag) {
				importSet.add("import java.sql.RowId;");
				rowIdFlag = false;
			} else if ("SQLXML".equals(javaType) && sqlxmlFlag) {
				importSet.add("import java.sql.SQLXML;");
				sqlxmlFlag = false;

			} else if (javaType.startsWith("[UNKNOWN TYPE]")) {
				unknownTypeTip = " //set the type mapping in the jdbcTypeToFieldType.properties";
			} else if ("boolean".equals(javaType)) {
				getOrIs = "is";
			}

			if (commentMap != null && (config.isGenComment() || config.isGenSwagger())) {
				comment = commentMap.get(columnName);
				boolean ynNull = true;
				if (ObjectUtils.isNotEmpty(ynNulls)) ynNull = ynNulls.get(i) == null ? true : ynNulls.get(i);

//				String requiredStr=", required = true";
				String requiredStr = "";
				String descValue = comment;
				if (StringUtils.isBlank(comment)) descValue = propertyName;
				if (!ynNull && !"id".equalsIgnoreCase(propertyName)) requiredStr = ", required = true";
				if (config.getCommentPlace() == 2) {
					if (StringUtils.isNotBlank(comment)) propertiesStr += "\t" + "// " + comment + LINE_SEPARATOR;
					if (config.isGenSwagger())
						propertiesStr += "\t" + "@ApiModelProperty(value = \"#VALUE#\"##required##)"
								.replace("#VALUE#", descValue).replace("##required##", requiredStr) + LINE_SEPARATOR;
					propertiesStr += "\t" + "private " + javaType + " " + propertyName + ";" + unknownTypeTip
							+ LINE_SEPARATOR;
				} else if (config.isGenComment() && (config.getCommentPlace() == 0 || config.getCommentPlace() == 1)) { // 不设置,默认是0; //0, 1:after field name at the same line;
					if (config.isGenSwagger())
						propertiesStr += "\t" + "@ApiModelProperty(value = \"#VALUE#\"##required##)"
								.replace("#VALUE#", descValue).replace("##required##", requiredStr) + LINE_SEPARATOR;
					propertiesStr += "\t" + "private " + javaType + " " + propertyName + ";" + unknownTypeTip;
					if (StringUtils.isNotBlank(comment)) propertiesStr += "//" + comment;
					propertiesStr += LINE_SEPARATOR;
				} else if (config.isGenSwagger()) { // just have swagger, no comment
					propertiesStr += "\t" + "@ApiModelProperty(value = \"#VALUE#\"##required##)".replace("#VALUE#", descValue).replace("##required##", requiredStr) + LINE_SEPARATOR;
					propertiesStr += "\t" + "private " + javaType + " " + propertyName + ";" + unknownTypeTip;
					propertiesStr += LINE_SEPARATOR;
				}
			} else {
				propertiesStr += "\t" + "private " + javaType + " " + propertyName + ";" + unknownTypeTip
						+ LINE_SEPARATOR;
			}

			getsetStr += "\t" + "public " + javaType + " " + getOrIs + getsetProNameStr + "() {" + LINE_SEPARATOR + "\t\t"
					+ "return " + propertyName + ";" + LINE_SEPARATOR + "\t}" + LINE_SEPARATOR + LINE_SEPARATOR +

					"\t" + "public void set" + getsetProNameStr + "(" + javaType + " " + propertyName + ") {"
					+ LINE_SEPARATOR + "\t\t" + "this." + propertyName + " = " + propertyName + ";" + LINE_SEPARATOR
					+ "\t}" + LINE_SEPARATOR + LINE_SEPARATOR;

			if (config.isGenToString()) { // toString()
				tostr.append("\t\tstr.append(\",").append(propertyName).append("=\").append(").append(propertyName)
						.append(");");
				tostr.append(LINE_SEPARATOR);
			}
		} // end for

		if (config.isLombokSetter()) importSet.add("import lombok.Setter;");
		if (config.isLombokGetter()) importSet.add("import lombok.Getter;");
		if (config.isLombokData()) importSet.add("import lombok.Data;");
		if (config.isLombokSetter() || config.isLombokGetter() || config.isLombokData()) config.setGenGetSet(false);

		if (config.isGenSwagger()) {
			importSet.add("import io.swagger.annotations.ApiModel;");
			importSet.add("import io.swagger.annotations.ApiModelProperty;");
		}

		for (String s : importSet) {
			importStr += s + LINE_SEPARATOR;
		}

		if (HoneyUtil.isCassandra())
			importStr += LINE_SEPARATOR + "//import org.teasoft.bee.osql.annotation.Table;" + LINE_SEPARATOR;

		// 生成实体类文件
		String basePath = config.getBaseDir();
		if (!basePath.endsWith(File.separator)) basePath += File.separator;
		String entitySaveDir = basePath + config.getPackagePath().replace(".", File.separator) + File.separator;
		File folder = new File(entitySaveDir);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		File entityFile = new File(entitySaveDir + entityName + ".java");

		if (entityFile.isFile()) {
			if (config.isOverride()) {
				Logger.debug("Override file:  " + entityFile.getAbsolutePath());
			} else {
				printOvrrideTip(entityFile.getAbsolutePath());
				return false;
			}
		}

		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(entityFile)));) {
			bw.write(packageStr + LINE_SEPARATOR);
			if (!"".equals(importStr)) bw.write(importStr + LINE_SEPARATOR);
			bw.write(authorComment + LINE_SEPARATOR);
			if (HoneyUtil.isCassandra())
				bw.write("//@Table(\"" + tableBean.getSchema() + "." + tableName + "\")" + LINE_SEPARATOR);

			// support lombok
			if (config.isLombokSetter()) bw.write("@Setter" + LINE_SEPARATOR);
			if (config.isLombokGetter()) bw.write("@Getter" + LINE_SEPARATOR);
			if (config.isLombokData()) bw.write("@Data" + LINE_SEPARATOR);

			if (config.isGenSwagger()) bw.write("@ApiModel(description = \"#VALUE#\") ".replace("#VALUE#", tableComment) + LINE_SEPARATOR);

			bw.write("public class " + entityName);
			if (config.isGenSerializable()) {
				bw.write(" implements Serializable");
			}
			bw.write(" {" + LINE_SEPARATOR + LINE_SEPARATOR);
			if (config.isGenSerializable()) {
				bw.write("\tprivate static final long serialVersionUID = " + HoneyUtil.genSerializableNum() + ";" + LINE_SEPARATOR);
				bw.write(LINE_SEPARATOR);
			}
			bw.write(propertiesStr);
			bw.write(LINE_SEPARATOR);
			if (config.isGenGetSet()) bw.write(getsetStr);

			if (config.isGenToString()) { // toString()
				tostr.deleteCharAt(tostr.indexOf(","));
				tostr.insert(0, LINE_SEPARATOR);
//				tostring.insert(0,"\t");

				tostr.append("\t\tstr.append(\"]\");");
				tostr.append(LINE_SEPARATOR);
				tostr.append("\t\treturn str.toString();");
				tostr.append(LINE_SEPARATOR);
				tostr.append("\t}");
				tostr.append(LINE_SEPARATOR);

//				tostring.insert(0,"\t"+LINE_SEPARATOR ); 
				tostr.insert(0, "\t\tstr.append(\"" + entityName + "[\");");
				tostr.insert(0, LINE_SEPARATOR);
				tostr.insert(0, "\t\tStringBuilder str = new StringBuilder();");
				tostr.insert(0, LINE_SEPARATOR);
				tostr.insert(0, "\tpublic String toString() {");

				bw.write(tostr.toString());
			}
			bw.write("}");
			bw.flush();
//			bw.close();

			Logger.info("The Honey gen the JavaBean: " + config.getPackagePath() + "." + entityName);

		} catch (Exception e) {
//			Logger.error(e.getMessage());
			throw ExceptionHelper.convert(e);
		}

		return true;
	}

	private void printOvrrideTip(String path) {
		Logger.warn("The file is exist!   " + path);
		if (printOverrideSetTip) {
			Logger.warn("You can config the override type as : config.setOverride(true);");
			printOverrideSetTip = false;
		}
	}

	private void genFieldFile(TableBean tableBean) {

		String tableName = tableBean.getTableName();
		List<String> columnNames = tableBean.getColumnNames();
		Map<String, String> commentMap = tableBean.getCommentMap();

		// 表名对应的实体类名
		String entityName = "";
		entityName = NameTranslateHandle.toEntityName(tableName);
//		String selfName=entityName;

		entityName = NameUtil.firstLetterToUpperCase(entityName);// 确保类名首字母大写.

		if (config.getEntityNamePre() != null) entityName = config.getEntityNamePre() + entityName;

		String fieldFileName = config.getFieldFilePrefix() + entityName + config.getFieldFileSuffix();
		String fieldFilePackagePath = config.getPackagePath();
		if (StringUtils.isNotBlank(config.getFieldFileRelativeFolder())) {
			fieldFilePackagePath += "." + config.getFieldFileRelativeFolder();
		}

		String tableComment = "";
		if (config.isGenComment() && commentMap != null) tableComment = commentMap.get(tableBean.getTableName());

		String authorComment = "/**" + LINE_SEPARATOR;
		if (config.isGenComment() && !"".equals(tableComment))
			authorComment += " * " + tableComment + " (relative field name for Javabean " + entityName + ")" + LINE_SEPARATOR;
		else
			authorComment += " * Relative field name for Javabean " + entityName + LINE_SEPARATOR;
		authorComment += " * @author Honey" + LINE_SEPARATOR;
		authorComment += " * Create on " + DateUtil.currentDate() + LINE_SEPARATOR; // v1.7.2
		authorComment += " */";

		String packageStr = "package " + fieldFilePackagePath + ";" + LINE_SEPARATOR;

		// 生成实体类文件
		String basePath = config.getBaseDir();
		if (!basePath.endsWith(File.separator)) basePath += File.separator;
		String fieldFileSaveDir = basePath + config.getPackagePath().replace(".", File.separator) + File.separator;
		if (StringUtils.isNotBlank(config.getFieldFileRelativeFolder())) {
			fieldFileSaveDir += config.getFieldFileRelativeFolder() + File.separator;
		}
		File folder = new File(fieldFileSaveDir);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		File fieldFile = new File(fieldFileSaveDir + fieldFileName + ".java");
		if (fieldFile.isFile()) {
			if (config.isOverride()) {
				Logger.debug("Override file:  " + fieldFile.getAbsolutePath());
			} else {
				printOvrrideTip(fieldFile.getAbsolutePath());
				return;
			}
		}

		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fieldFile)));) {
			bw.write(packageStr + LINE_SEPARATOR);
			bw.write(authorComment + LINE_SEPARATOR);
			bw.write("public interface " + fieldFileName);
			bw.write(" {" + LINE_SEPARATOR + LINE_SEPARATOR);

//			bw.write("	private FieldFileName() {}".replace("FieldFileName", fieldFileName));
//			bw.write(LINE_SEPARATOR + LINE_SEPARATOR);

			String st = "";
			String comment = "";
			String fieldName = "";
			String columnName = "";
			String allFieldName = "";
			boolean genFieldAll = config.isGenFieldAll();
			boolean genSelfName = config.isGenSelfName();

			for (int i = 0; i < columnNames.size(); i++) {
				columnName = columnNames.get(i);
				fieldName = NameTranslateHandle.toFieldName(columnName);
				st = "	String {fieldName1} = \"{fieldName2}\";".replace("{fieldName2}", fieldName);
				if (config.isUpperFieldNameInFieldFile()) // 2.4.2
					st = st.replace("{fieldName1}", columnName.toUpperCase());
				else
					st = st.replace("{fieldName1}", fieldName);

				if (config.isGenComment() && commentMap != null) {
					comment = commentMap.get(columnName); // 1.17 fixed bug
					if (config.getCommentPlace() == 2) {
						if (!"".equals(comment)) st = "\t" + "// " + comment + LINE_SEPARATOR + st;
					} else {
						if (!"".equals(comment)) st += "//" + comment;
						st += LINE_SEPARATOR;
					}
				}
				bw.write(st);
				bw.write(LINE_SEPARATOR);

				if (genFieldAll) {
					if (i != 0) allFieldName += ",";
					allFieldName += fieldName;
				}
			}

			if (genSelfName) {
				bw.write("	String ENTITY_NAME = \"" + entityName + "\";");
				bw.write(LINE_SEPARATOR);

				bw.write("	String TABLE_NAME = \"" + tableName + "\";");
				bw.write(LINE_SEPARATOR);
			}

			if (genFieldAll) {
				bw.write("	String ALL_NAMES = \"" + allFieldName + "\";");
				bw.write(LINE_SEPARATOR);
			}

			bw.write("}");
			bw.flush();

			Logger.info("The Honey gen the FieldFile for JavaBean: " + fieldFilePackagePath + "." + fieldFileName);

		} catch (Exception e) {
//			Logger.error(e.getMessage());
			throw ExceptionHelper.convert(e);
		}
	}

	private Queue<Set> setQueue = new LinkedBlockingQueue<>();
	private Queue<Integer> layerQueue = new LinkedBlockingQueue<>();
	private Queue<String> nameQueue = new LinkedBlockingQueue<>();

	private boolean isMongodb() {
		return DatabaseConst.MongoDB.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	private void _genBeanFileForMongodb(String[] tableNames) {
		for (String tab : tableNames) {
			_genBeanForMongodb(tab);
		}
	}

	private boolean some_mongodb = false;
	private boolean all_mongodb = true;
	private boolean f1_mongodb;

	private void _genBeanForMongodb(String tableName) {
		MongodbComm mongodbComm = MongodbCommRegister.getInstance();
		Set<Map.Entry<String, Object>> set = mongodbComm.getCollectStrcut(tableName);
		if (set == null || set.size() < 1) {
			Logger.warn(
					"Generate Javabean via Mongodb,the collection(table) must have one document(row) at least!!!  collection(table):"
							+ tableName);
			return;
		}

		_genBeanForMongodb(set, 1, tableName);
	}

	private void _genBeanForMongodb(Set<Map.Entry<String, Object>> set, int layer, String tableNameOrPropertyName) {
		TableBean tableBean = new TableBean();
		tableBean.setTableName(tableNameOrPropertyName);
		Map<String, String> commentMap = new HashMap<>();
		String key = "";
		commentMap.put(tableNameOrPropertyName, tableNameOrPropertyName); // 为mongodb字段添加默认注释
		Logger.debug("The layer is: " + layer);
		for (Entry<String, Object> entry : set) {
			key = entry.getKey();
			if ("_id".equals(key)) key = "id";
			tableBean.getColumnNames().add(key);
			commentMap.put(key, key);
//			table.getColumnTypes().add(entry.getValue().getClass().getName());

//			多层Json结构当String处理,不会生成多个Javabean
			String className = entry.getValue().getClass().getName();
			if ("org.bson.Document".equals(className) && !"String".equals(HoneyUtil.getFieldType("org.bson.Document"))) {
				Map d2 = (Map) entry.getValue();
				setQueue.add(d2.entrySet());
				layerQueue.add(layer + 1);
//				nameQueue.add(entry.getValue().getClass().getSimpleName());
				nameQueue.add(key);
				tableBean.getColumnTypes().add(key);
//				commentMap.put(key, key);
			} else {
				tableBean.getColumnTypes().add(entry.getValue().getClass().getName());
//				String entry.getValue().getClass().getSimpleName();
//				commentMap.put(, entry.getValue().getClass().getName());
			}
		}
		tableBean.setCommentMap(commentMap);

		f1_mongodb = genBeanFile(tableBean);
		if (config.isGenFieldFile()) genFieldFile(tableBean);
		some_mongodb = some_mongodb || f1_mongodb;
		all_mongodb = all_mongodb && f1_mongodb;

		if (!setQueue.isEmpty()) {
			_genBeanForMongodb(setQueue.poll(), layerQueue.poll(), nameQueue.poll());
		} else {
			if (all_mongodb) Logger.info("Generate Success!");
			else if (some_mongodb) Logger.info("Generate some file Success!");

			printCheck(all_mongodb || some_mongodb);
		}
	}

	public void genAllBeanFile() {

		if (isMongodb()) {
			MongodbComm mongodbComm = MongodbCommRegister.getInstance();
			_genBeanFileForMongodb(mongodbComm.getAllCollectionNames());
			return;
		}

		List<TableBean> tableList = getAllTables();
		_genBeanFiles(tableList);
	}

	private void _genBeanFiles(List<TableBean> tableBeans) {
		Logger.info("Generating...");
//		List<Table> tables = getAllTables();
		TableBean tableBean = null;
		boolean some = false;
		boolean all = true;
		boolean f1;

		for (int i = 0; i < tableBeans.size(); i++) {
			tableBean = tableBeans.get(i);
			// 生成实体类
			f1 = genBeanFile(tableBean);
			if (config.isGenFieldFile()) genFieldFile(tableBean);

			some = some || f1;
			all = all && f1;
		}
		if (all) Logger.info("Generate Success!");
		else if (some) Logger.info("Generate some file Success!");

		printCheck(all || some);

	}

	private void printCheck(boolean isNeed) {
		if (isNeed) Logger.info(
				"Please check folder: " + config.getBaseDir() + config.getPackagePath().replace(".", File.separator));
	}

	/**
	 * generate Javabean file via some table name.
	 * @param tableNameList table name list
	 */
	public void genSomeBeanFile(String tableNameList) {
		String[] tableNames = tableNameList.split(",");
		if (isMongodb()) {
			_genBeanFileForMongodb(tableNames);
			return;
		}

		Connection con = null;
		List<TableBean> tablesList = new ArrayList<>();
		try {
			con = SessionFactory.getConnection();
			TableBean tableBean = null;

			for (int i = 0; i < tableNames.length; i++) {
				tableBean = getTable(tableNames[i], con);
				tablesList.add(tableBean);
			}
		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
			if (e.getMessage().contains("You have an error in your SQL syntax;") && e.getMessage().contains("where 1<>1")) {
				Logger.info("Maybe the table name is the database key work. Please rename the tableName and test again." + e.getMessage());
			}
			throw ExceptionHelper.convert(e);
		} finally {
			try {
				if (con != null) con.close();
			} catch (Exception e2) {
				// ignore
			}
		}
		_genBeanFiles(tablesList);
	}

	// 获取所有表信息
	private List<TableBean> getAllTables() {
//		List<Table> tables = new ArrayList<>();

		boolean isCommDb = true;

		// 获取所有表名
		String showTablesSql = "";
		if (!"".equals(config.getQueryTableSql().trim())) {
			showTablesSql = config.getQueryTableSql();
		} else if (config.getDbName().equalsIgnoreCase(DatabaseConst.MYSQL)) {
			showTablesSql = "show tables";
		} else if (config.getDbName().equalsIgnoreCase(DatabaseConst.ORACLE)) {
			showTablesSql = "select table_name from user_tables";
		} else if (config.getDbName().equalsIgnoreCase(DatabaseConst.SQLSERVER)) {
			showTablesSql = "select table_name from edp.information_schema.tables where table_type='base table'"; // SQLServer查询所有表格名称命令
		} else {
//			throw new BeeException(
//					"There are not default sql, please check the bee.db.dbName in bee.properties is right or not, or define queryTableSql in GenConfig!");
			isCommDb = false;
		}

		if (isCommDb)
			return _mainDbTables(showTablesSql);
		else
			return _otherDbTables();
	}

	private List<TableBean> _mainDbTables(String showTablesSql) {
		List<TableBean> tableBeans = new ArrayList<>();
		try (Connection conn = SessionFactory.getConnection();
				PreparedStatement ps = conn.prepareStatement(showTablesSql);
				ResultSet rs = ps.executeQuery();) {

			List<String> tabList = new ArrayList<>();
			while (rs.next()) {
				if (rs.getString(1) == null) continue;
				tabList.add(rs.getString(1).trim());
//				tables.add(getTable(rs.getString(1).trim(), con));
			}

			for (String tab : tabList) {
				tableBeans.add(getTable(tab, conn));
			}
		} catch (SQLException e) {
//			Logger.error(e.getMessage());
			throw ExceptionHelper.convert(e);
		}

		return tableBeans;
	}

	private List<TableBean> _otherDbTables() {
		List<TableBean> tableBeans = new ArrayList<>();
		Connection conn = SessionFactory.getConnection();
		boolean has = false;
		try {
			DatabaseMetaData dbmd = conn.getMetaData();
			String schemaPattern = null;
			String types[] = null;
			if (config.getDbName().equalsIgnoreCase(DatabaseConst.H2)) schemaPattern = "PUBLIC";
			if (config.getDbName().equalsIgnoreCase(DatabaseConst.PostgreSQL)) {
				schemaPattern = "public";
				types = new String[] { "TABLE" };
			}

			ResultSet rs = dbmd.getTables(null, schemaPattern, "%", types);
			List<String> tabList = new ArrayList<>();
			while (rs.next()) {
				if (rs.getString(3) == null) continue;
				tabList.add(rs.getString(3).trim());
				has = true;
			}
			for (String tab : tabList) {
				tableBeans.add(getTable(tab, conn));
			}

		} catch (Exception e) {
			Logger.warn(e.getMessage());
			has = false;
		}

		if (!has) {
			throw new BeeException(
					"There are not default sql, please check the bee.db.dbName in bee.properties is right or not, or define queryTableSql in GenConfig!");
		}

		return tableBeans;
	}

	private TableBean getTable(String tableName, Connection con) throws SQLException {

		NameCheckUtil.checkName(tableName);

		PreparedStatement ps = null;
		ResultSet rs = null;
		TableBean tableBean = new TableBean();
		try {
			StringBuilder sql = new StringBuilder();
			if (HoneyUtil.isCassandra()) sql.append("select * from ").append(tableName).append(" limit 1");
			else sql.append("select * from ").append(tableName).append(" where 1<>1");
//			Logger.info(sql.toString()); 
			ps = con.prepareStatement(sql.toString());
			rs = ps.executeQuery();
			ResultSetMetaData rmeta = rs.getMetaData();
			// int index=tableName.indexOf(".");
			// if(index>-1) tableName=tableName.substring(index+1); //处理用数据库名.表名查的情况
			// 该方法不可取.因为要是用关键字作表名,别的suid操作都是要带数据库名.
			if (HoneyUtil.isCassandra()) {
				int index = tableName.indexOf(".");
				if (index > -1) tableBean.setTableName(tableName.substring(index + 1));
				else tableBean.setTableName(tableName);
			} else {
				tableBean.setTableName(tableName);
			}
			tableBean.setSchema(rmeta.getCatalogName(1));
//			.println("------------------getCatalogName:");
//			.println(rmeta.getCatalogName(1));
//			.println(rmeta.getSchemaName(1));
			int columCount = rmeta.getColumnCount();
			for (int i = 1; i <= columCount; i++) {
//				table.getColumnNames().add(rmeta.getColumnName(i).trim());
				tableBean.getColumnNames().add(rmeta.getColumnLabel(i).trim()); // V2.1.8
				tableBean.getColumnTypes().add(rmeta.getColumnTypeName(i).trim());
//				.println("--------------------------------");
//				.println(rmeta.getColumnName(i).trim()+ "     :    " +rmeta.getColumnTypeName(i).trim());
//				.println(rmeta.getColumnName(i).trim()+ "     :    " +rmeta.getColumnClassName(i).trim());
				if (rmeta.isNullable(i) == 1) tableBean.getYnNulls().add(true);
				else tableBean.getYnNulls().add(false);
			}
		} finally {
			HoneyContext.checkClose(rs, ps, null);
		}
		if (config == null || config.isGenComment() || config.isGenSwagger()) { // v1.8.15, V2.1.8
			// set comment
			initComment(tableBean, con);
		}

		if (isNeedKeyColumn) initKeyColumn(tableBean, con);

		return tableBean;
	}

	private void initKeyColumn(TableBean tableBean, Connection con) throws SQLException {
		DatabaseMetaData dbmd = con.getMetaData();

		ResultSet rs = null;
		Map<String, String> primaryKeyNames = new HashMap<>();

		try {
			rs = dbmd.getPrimaryKeys(null, null, tableBean.getTableName());
			while (rs.next()) {
				String keyName = rs.getString(4);
				primaryKeyNames.put(keyName, keyName);
			}

		} finally {
			HoneyContext.checkClose(rs, null, null);
		}

		tableBean.setPrimaryKeyNames(primaryKeyNames);
	}

	private void initComment(TableBean tableBean, Connection con) throws SQLException {
		String sql = "";
		String t_sql = null;
		String dbName = "";
		if (config != null) {
			t_sql = config.getQueryColumnCommnetSql();
			dbName = config.getDbName();
		} else {
			dbName = HoneyConfig.getHoneyConfig().getDbName();
		}

		if (t_sql != null) {
			sql = t_sql;
			if (Check.isNotValidExpression(sql)) {
				throw new BeeIllegalSQLException("The sql: '" + sql + "' is invalid!");
			}
		} else if (DatabaseConst.MYSQL.equalsIgnoreCase(dbName) || DatabaseConst.MariaDB.equalsIgnoreCase(dbName)) {
			sql = "select column_name,column_comment from information_schema.COLUMNS where TABLE_SCHEMA='"
					+ tableBean.getSchema() + "' and TABLE_NAME=?"; // the first select column is column name, second is comment
		} else if (DatabaseConst.ORACLE.equalsIgnoreCase(dbName)) {
			sql = "select column_name,comments from user_col_comments where table_name=?";
		} else {
			// V1.17
			Logger.warn("There are not default sql, please check the bee.db.dbName in bee.properties is right or not, or define queryColumnCommnetSql in GenConfig!");
			return;
		}
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		try {
			ps = con.prepareStatement(sql);
			ps.setString(1, tableBean.getTableName());
			Map<String, String> map = getCommentMap(ps);
			// get table comment
			String sql2 = "";
			String t_sql2 = null;

			if (config != null) {
				t_sql2 = config.getQueryTableCommnetSql();
			}

			if (t_sql2 != null) {
				sql2 = t_sql2;
				if (Check.isNotValidExpression(sql2)) {
					throw new BeeIllegalSQLException("The sql: '" + sql2 + "' is invalid!");
				}
			} else if (DatabaseConst.MYSQL.equalsIgnoreCase(dbName) || DatabaseConst.MariaDB.equalsIgnoreCase(dbName)) {
				sql2 = "select TABLE_NAME,TABLE_COMMENT from information_schema.TABLES where TABLE_SCHEMA='"
						+ tableBean.getSchema() + "' and TABLE_NAME=?";
			} else if (DatabaseConst.ORACLE.equalsIgnoreCase(dbName)) {
				sql2 = "select table_name,comments from user_tab_comments where table_name=?";
			} else {
				throw new BeeException(
						"There are not default sql, please check the bee.db.dbName in bee.properties is right or not, or define queryTableCommnetSql in GenConfig!");
			}
			ps2 = con.prepareStatement(sql2);
			ps2.setString(1, tableBean.getTableName());
			Map<String, String> map2 = getCommentMap(ps2);

			map2.putAll(map);
			tableBean.setCommentMap(map2);
		} finally {
			HoneyContext.checkClose(ps, null);
			HoneyContext.checkClose(ps2, null);
		}
	}

	private Map<String, String> getCommentMap(PreparedStatement ps) throws SQLException {
		Map<String, String> map = new HashMap<>();
		ResultSet rs = null;
		try {
			rs = ps.executeQuery();
			String comment;
			while (rs.next()) {
				comment = rs.getString(2);
				if (comment == null) comment = "";
				map.put(rs.getString(1), comment);
			}
		} finally {
			HoneyContext.checkClose(rs, ps, null);
		}
		return map;
	}

	private TableBean getTalbe(String tableName) {
		Connection con = null;
		TableBean tableBean = null;
		try {
			con = SessionFactory.getConnection();
			tableBean = getTable(tableName, con);
		} catch (Exception e) {
			Logger.info(e.getMessage());
			if (e.getMessage().contains("You have an error in your SQL syntax;") && e.getMessage().contains("where 1<>1")) {
				Logger.info("Maybe the table name is the database key work. Please rename the tableName and test again."
						+ e.getMessage());
			}
			throw ExceptionHelper.convert(e);
		} finally {
			try {
				if (con != null) con.close();
			} catch (Exception e2) {
				// ignore
			}
		}
		return tableBean;
	}

	public TableBean getTableInfo(String tableName) {
		isNeedKeyColumn = true;
		TableBean tableBean = getTalbe(tableName);
		isNeedKeyColumn = false;
		return tableBean;
	}

	public List<String> getColumnNames(String tableName) {
		TableBean tableBean = getTalbe(tableName);
		if (tableBean != null) {
			return tableBean.getColumnNames();
		}
		return Collections.emptyList();
	}

	public List<String> getFieldNames(String tableName) {
		List<String> columnNames = getColumnNames(tableName);
		if (columnNames == null) return Collections.emptyList();
		List<String> fieldNames = new ArrayList<>();
		for (int i = 0; i < columnNames.size(); i++) {
			fieldNames.add(NameTranslateHandle.toFieldName(columnNames.get(i)));
		}
		return fieldNames;
	}
}

class TableBean {
	private String tableName; // 表名
	private List<String> columnNames = new ArrayList<>(); // 列名集合
	private List<String> columnTypes = new ArrayList<>(); // 列类型集合，列类型严格对应java类型
	private Map<String, String> commentMap;

	private List<Boolean> ynNulls = new ArrayList<>(); // 生成javabean不需要用到.
	private Map<String, String> primaryKeyNames;// 生成javabean不需要用到.

	private String schema; // DB库名

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}

	public List<String> getColumnTypes() {
		return columnTypes;
	}

	public Map<String, String> getCommentMap() {
		return commentMap;
	}

	public void setCommentMap(Map<String, String> commentMap) {
		this.commentMap = commentMap;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public List<Boolean> getYnNulls() {
		return ynNulls;
	}

	public Map<String, String> getPrimaryKeyNames() {
		return primaryKeyNames;
	}

	public void setPrimaryKeyNames(Map<String, String> primaryKeyNames) {
		this.primaryKeyNames = primaryKeyNames;
	}

}

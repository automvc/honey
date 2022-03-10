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

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.honey.osql.core.Check;
import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.osql.core.SessionFactory;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.util.DateUtil;
import org.teasoft.honey.osql.util.NameCheckUtil;

//是否覆盖文件,       支持写生成其中一个文件或几个文件(已实现)
/**
 * 生成Javabean.Generate Javabean.
 * @author Kingstar
 * @since 1.0
 */
public class GenBean {

	private GenConfig config;
	private String LINE_SEPARATOR = System.getProperty("line.separator"); // 换行符
	//	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private boolean isNeedKeyColumn=false;

	public GenBean(GenConfig config) {
		this.config = config;
	}

	//	生成指定表对象对应的类文件
	private void genBeanFile(Table table) {
		String tableName = table.getTableName();
		List<String> columnNames = table.getColumnNames();
		List<String> columnTypes = table.getColumnTypes(); //jdbcType
		Map<String, String> commentMap = table.getCommentMap();
		// 表名对应的实体类名
		String entityName = "";
		entityName = NameTranslateHandle.toEntityName(tableName);

		entityName = NameUtil.firstLetterToUpperCase(entityName);// 确保类名首字母大写.

		if (config.getEntityNamePre() != null) entityName = config.getEntityNamePre() + entityName;

		Logger.info("The Honey gen the JavaBean: " + config.getPackagePath() + "." + entityName);

		String tableComment = "";
		if (config.isGenComment()) tableComment = commentMap.get(table.getTableName());

		String authorComment = "/**" + LINE_SEPARATOR;
		if (config.isGenComment() && !"".equals(tableComment)) authorComment += "*" + tableComment + LINE_SEPARATOR;
		//		 authorComment+="*@author Bee"+ LINE_SEPARATOR;
		authorComment += "*@author Honey" + LINE_SEPARATOR;
		//		 authorComment+="*Create on "+format.format(new Date())+ LINE_SEPARATOR;
		authorComment += "*Create on " + DateUtil.currentDate() + LINE_SEPARATOR; //v1.7.2
		authorComment += "*/";

		String packageStr = "package " + config.getPackagePath() + ";" + LINE_SEPARATOR;

		String importStr = "";

		// 生成无参构造方法
		//		String constructorStr = "\t" + "public " + entityName + "() {}"+ LINE_SEPARATOR; 
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
		boolean blobFlag = true;
		boolean clobFlag = true;
		boolean arrayFlag = true;

		StringBuilder tostr = new StringBuilder();

		if (config.isGenSerializable()) {
			importStr += "import java.io.Serializable;" + LINE_SEPARATOR;
		}

		for (int i = 0; i < columnNames.size(); i++) {
			String columnName = columnNames.get(i);
			String columnType = columnTypes.get(i);
			String comment = "";
			String getOrIs = "get";

			propertyName = NameTranslateHandle.toFieldName(columnName);

			getsetProNameStr = HoneyUtil.firstLetterToUpperCase(propertyName);
			javaType = HoneyUtil.getFieldType(columnType);

			//import
			if ("BigDecimal".equals(javaType) && bigDecimalFlag) {
				importStr += "import java.math.BigDecimal;" + LINE_SEPARATOR;
				bigDecimalFlag = false;
			} else if ("BigInteger".equals(javaType) && bigIntegerFlag) {
				importStr += "import java.math.BigInteger;" + LINE_SEPARATOR;
				bigIntegerFlag = false;
			} else if ("Date".equals(javaType) && dateFlag) {
				importStr += "import java.sql.Date;" + LINE_SEPARATOR;
				dateFlag = false;
			} else if ("Time".equals(javaType) && timeFlag) {
				importStr += "import java.sql.Time;" + LINE_SEPARATOR;
				timeFlag = false;
			} else if ("Timestamp".equals(javaType) && timestampFlag) {
				importStr += "import java.sql.Timestamp;" + LINE_SEPARATOR;
				timestampFlag = false;
			} else if ("Blob".equals(javaType) && blobFlag) {
				importStr += "import java.sql.Blob;" + LINE_SEPARATOR;
				blobFlag = false;
			} else if ("Clob".equals(javaType) && clobFlag) {
				importStr += "import java.sql.Clob;" + LINE_SEPARATOR;
				clobFlag = false;
			} else if ("Array".equals(javaType) && arrayFlag) {
				importStr += "import java.sql.Array;" + LINE_SEPARATOR;
				arrayFlag = false;
			}
			//			else if(javaType.contains(".")){
			//				importStr += "import "+javaType+";" + LINE_SEPARATOR;
			//				arrayFlag = false;
			//			}//防止类名与上面的重复,还是直接用

			if ("boolean".equals(javaType)) getOrIs = "is";

			if (config.isGenComment() && commentMap != null) {
				comment = commentMap.get(columnName);
				if (config.getCommentPlace() == 2) {
					if (!"".equals(comment)) propertiesStr += "\t" + "// " + comment + LINE_SEPARATOR;
					propertiesStr += "\t" + "private " + javaType + " " + propertyName + ";" + LINE_SEPARATOR;
				} else {
					propertiesStr += "\t" + "private " + javaType + " " + propertyName + ";";
					if (!"".equals(comment)) propertiesStr += "//" + comment;
					propertiesStr += LINE_SEPARATOR;
				}
			} else {
				propertiesStr += "\t" + "private " + javaType + " " + propertyName + ";" + LINE_SEPARATOR;
			}

			getsetStr += "\t" + "public " + javaType + " " + getOrIs + getsetProNameStr + "() {" + LINE_SEPARATOR + "\t\t"
					+ "return " + propertyName + ";" + LINE_SEPARATOR + "\t}" + LINE_SEPARATOR + LINE_SEPARATOR +

					"\t" + "public void set" + getsetProNameStr + "(" + javaType + " " + propertyName + ") {" + LINE_SEPARATOR
					+ "\t\t" + "this." + propertyName + " = " + propertyName + ";" + LINE_SEPARATOR + "\t}" + LINE_SEPARATOR
					+ LINE_SEPARATOR;

			if (config.isGenToString()) { //toString()
				tostr.append("\t\t str.append(\",").append(propertyName).append("=\").append(").append(propertyName).append(");");
				tostr.append("\t\t " + LINE_SEPARATOR);
			}
		}

		// 生成实体类文件
		String basePath = config.getBaseDir();
		if (!basePath.endsWith(File.separator)) basePath += File.separator;
		String entitySaveDir = basePath + config.getPackagePath().replace(".", File.separator) + File.separator;
		File folder = new File(entitySaveDir);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		File entityFile = new File(entitySaveDir + entityName + ".java");

		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(entityFile)));) {
			bw.write(packageStr + LINE_SEPARATOR);
			if (!"".equals(importStr)) bw.write(importStr + LINE_SEPARATOR);
			bw.write(authorComment + LINE_SEPARATOR);
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
			//			bw.write(LINE_SEPARATOR);
			//			bw.write(constructorStr);
			bw.write(LINE_SEPARATOR);
			// bw.write(toStringStr);
			// bw.write(LINE_SEPARATOR);
			bw.write(getsetStr);
			// bw.write(LINE_SEPARATOR);

			if (config.isGenToString()) { //toString()
				tostr.deleteCharAt(tostr.indexOf(","));
				tostr.insert(0, "\t\t" + LINE_SEPARATOR);
				//				tostring.insert(0,"\t");

				tostr.append("\t\t str.append(\"]\");\t");
				tostr.append("\t\t " + LINE_SEPARATOR);
				tostr.append("\t\t return str.toString();\t");
				tostr.append("\t\t " + LINE_SEPARATOR);
				tostr.append("\t }");
				tostr.append("\t\t " + LINE_SEPARATOR);

				//				tostring.insert(0,"\t"+LINE_SEPARATOR ); 
				tostr.insert(0, "\t\t str.append(\"" + entityName + "[\");\t");
				tostr.insert(0, "\t" + LINE_SEPARATOR);
				tostr.insert(0, "\t\t StringBuffer str=new StringBuffer();");
				tostr.insert(0, "\t" + LINE_SEPARATOR);
				tostr.insert(0, "\t public String toString(){");

				bw.write(tostr.toString());
			}
			bw.write("}");
			bw.flush();
			//			bw.close();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			throw ExceptionHelper.convert(e);
		}
	}

	public void genAllBeanFile() {
		Logger.info("Generating...");
		List<Table> tables = getAllTables();
		Table table = null;
		for (int i = 0; i < tables.size(); i++) {
			table = tables.get(i);
			// 生成实体类
			genBeanFile(table);
		}
		Logger.info("Generate Success!");
		Logger.info("Please check: " + config.getBaseDir() + config.getPackagePath().replace(".", "\\"));
	}

	public void genSomeBeanFile(String tableList) {// throws IOException {
		String[] tables = tableList.split(",");
		Connection con = null;
		try {
			con = SessionFactory.getConnection();
			Table table = null;
			for (int i = 0; i < tables.length; i++) {
				table = getTable(tables[i], con);
				// 生成实体类
				genBeanFile(table);
			}
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
				//ignore
			}
		}
		Logger.info("Generate Success!");
		Logger.info("Please check folder: " + config.getBaseDir() + config.getPackagePath().replace(".", "\\"));
	}

	// 获取所有表信息
	private List<Table> getAllTables() {
		List<Table> tables = new ArrayList<>();
		//		Connection con = null;
		//		PreparedStatement ps = null;
		//		ResultSet rs = null;
		//		try {
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
			throw new BeeException(
					"There are not default sql, please check the bee.db.dbName in bee.properties is right or not, or define queryTableSql in GenConfig!");
		}
		//			con = getConnection();
		try (Connection con = SessionFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(showTablesSql);
				ResultSet rs = ps.executeQuery();) {

			while (rs.next()) {
				if (rs.getString(1) == null) continue;
				tables.add(getTable(rs.getString(1).trim(), con));
			}
			//			rs.close();
			//			ps.close();
			//			con.close();
		} catch (SQLException e) {
			Logger.error(e.getMessage());
			throw ExceptionHelper.convert(e);
		}
		return tables;
	}

	private Table getTable(String tableName, Connection con) throws SQLException {
		
//		if(CheckField.isIllegal(tableName)) {
//			throw new BeeIllegalParameterException("The tableName: '"+tableName+ "' is illegal!");
//		}
		
		NameCheckUtil.checkName(tableName);
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		Table table = new Table();
		try {
			StringBuilder sql=new StringBuilder();
			sql.append("select * from ").append(tableName).append(" where 1<>1");
			ps = con.prepareStatement(sql.toString()); 
			rs = ps.executeQuery();
			ResultSetMetaData rmeta = rs.getMetaData();
			//		int index=tableName.indexOf(".");
			//		if(index>-1) tableName=tableName.substring(index+1); //处理用数据库名.表名查的情况
			//该方法不可取.因为要是用关键字作表名,别的suid操作都是要带数据库名.
			table.setTableName(tableName);
			table.setSchema(rmeta.getCatalogName(1));
			int columCount = rmeta.getColumnCount();
			for (int i = 1; i <= columCount; i++) {
				table.getColumnNames().add(rmeta.getColumnName(i).trim());
				table.getColumnTypes().add(rmeta.getColumnTypeName(i).trim());
//				.println("--------------------------------");
////				.println(rmeta.getColumnName(i).trim()+ "     :    " +rmeta.getColumnTypeName(i).trim());
//				.println(rmeta.getColumnName(i).trim()+ "     :    " +rmeta.getColumnClassName(i).trim());
				if (rmeta.isNullable(i) == 1) table.getYnNulls().add(true);
				else table.getYnNulls().add(false);
			}
		} finally {
			HoneyContext.checkClose(rs, ps, null);
		}
		if (config==null || config.isGenComment()) { //v1.8.15
			//set comment
			initComment(table, con);
		}
		
		if(isNeedKeyColumn) initKeyColumn(table, con);
		
		return table;
	}
	
	private void initKeyColumn(Table table, Connection con) throws SQLException {
		DatabaseMetaData dbmd = con.getMetaData();

		ResultSet rs = null;
		Map<String, String> primaryKeyNames = new HashMap<>();

		try {
			rs = dbmd.getPrimaryKeys(null, null, table.getTableName());
			while (rs.next()) {
				String keyName = rs.getString(4);
				primaryKeyNames.put(keyName, keyName);
			}

		} finally {
			HoneyContext.checkClose(rs, null, null);
		}
		
		table.setPrimaryKeyNames(primaryKeyNames);
	}

	private void initComment(Table table, Connection con) throws SQLException {
		String sql = "";
		String t_sql = null;
		String dbName="";
		if(config!=null) {
			t_sql=config.getQueryColumnCommnetSql();
			dbName=config.getDbName();
		}else {
			dbName=HoneyConfig.getHoneyConfig().getDbName();
		}
		
		if (t_sql != null) {
			sql = t_sql;
			if(Check.isNotValidExpression(sql)) {
				throw new BeeIllegalSQLException("The sql: '"+sql+ "' is invalid!");
			}
		}else if (DatabaseConst.MYSQL.equalsIgnoreCase(dbName)
				|| DatabaseConst.MariaDB.equalsIgnoreCase(dbName)) {
			sql = "select column_name,column_comment from information_schema.COLUMNS where TABLE_SCHEMA='" + table.getSchema()
					+ "' and TABLE_NAME=?"; //the first select column is column name, second is comment
		} else if (DatabaseConst.ORACLE.equalsIgnoreCase(dbName)) {
			sql = "select column_name,comments from user_col_comments where table_name=?";
		} else {
			throw new BeeException(
					"There are not default sql, please check the bee.db.dbName in bee.properties is right or not, or define queryColumnCommnetSql in GenConfig!");
		}
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		try {
			ps = con.prepareStatement(sql);
			ps.setString(1, table.getTableName());
			Map<String, String> map = getCommentMap(ps);
			//get table comment
			String sql2 = "";
			String t_sql2=null;
			
			if(config!=null) {
				t_sql2 = config.getQueryTableCommnetSql();
			}
			
			if (t_sql2 != null) {
				sql2 = t_sql2;
				if(Check.isNotValidExpression(sql2)) {
					throw new BeeIllegalSQLException("The sql: '"+sql2+ "' is invalid!");
				}
			}else if (DatabaseConst.MYSQL.equalsIgnoreCase(dbName)
					|| DatabaseConst.MariaDB.equalsIgnoreCase(dbName)) {
				sql2 = "select TABLE_NAME,TABLE_COMMENT from information_schema.TABLES where TABLE_SCHEMA='" + table.getSchema()
						+ "' and TABLE_NAME=?";
			} else if (DatabaseConst.ORACLE.equalsIgnoreCase(dbName)) {
				sql2 = "select table_name,comments from user_tab_comments where table_name=?";
			} else {
				throw new BeeException(
						"There are not default sql, please check the bee.db.dbName in bee.properties is right or not, or define queryTableCommnetSql in GenConfig!");
			}
			ps2 = con.prepareStatement(sql2);
			ps2.setString(1, table.getTableName());
			Map<String, String> map2 = getCommentMap(ps2);

			map2.putAll(map);
			table.setCommentMap(map2);
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

	private Table getTalbe(String tableName) {
		Connection con = null;
		Table table = null;
		try {
			con = SessionFactory.getConnection();
			table = getTable(tableName, con);
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
				//ignore
			}
		}
		return table;
	}
	
	public Table getTableInfo(String tableName) {
		isNeedKeyColumn=true;
		Table table= getTalbe(tableName);
		isNeedKeyColumn=false;
		return table;
	}

	public List<String> getColumnNames(String tableName) {
		Table table = getTalbe(tableName);
		if (table != null) {
			return table.getColumnNames();
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

class Table {
	private String tableName; // 表名
	private List<String> columnNames = new ArrayList<>(); // 列名集合
	private List<String> columnTypes = new ArrayList<>(); // 列类型集合，列类型严格对应java类型
	private Map<String, String> commentMap; 
	
	private List<Boolean> ynNulls= new ArrayList<>();   //生成javabean不需要用到.
	private Map<String, String> primaryKeyNames;//生成javabean不需要用到.
	
	private String schema; //DB库名

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
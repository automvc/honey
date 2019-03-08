package org.teasoft.honey.osql.atuogen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.teasoft.honey.osql.constant.DatabaseConst;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.SessionFactory;

//TODO 是否覆盖文件,       支持写生成其中一个文件或几个文件(已实现)
public class GenBean {

	private GenConfig config;
	private String LINE_SEPARATOR = System.getProperty("line.separator"); // 换行符
	
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public GenBean(GenConfig config) {
		this.config = config;
	}

//	生成指定表对象对应的类文件
	private void genBeanFile(Table table) {
		String tableName = table.getTableName();
		List<String> columnNames = table.getColumNames();
		List<String> columTypes = table.getColumTypes();  //jdbcType

		// 表名对应的实体类名
		String entityName ="";
		
		if(HoneyConfig.getHoneyConfig().isUnderScoreAndCamelTransform()){
			entityName=HoneyUtil.toCamelNaming(tableName.toLowerCase());
		}else{
			entityName=tableName;
		}
		entityName=HoneyUtil.firstLetterToUpperCase(entityName);
		
		Logger.print("The Honey gen the JavaBean: " + config.getPackagePath() +"."+entityName);
		
		String authorComment="/**"+ LINE_SEPARATOR;
//		       authorComment+="*@author Bee"+ LINE_SEPARATOR;
		       authorComment+="*@author Honey"+ LINE_SEPARATOR;
		       authorComment+="*Create on "+format.format(new Date())+ LINE_SEPARATOR;
		       authorComment+="*/";

		
		String packageStr = "package " + config.getPackagePath() + ";"+ LINE_SEPARATOR;
		
		String importStr ="";

		// 生成无参构造方法
//		String constructorStr = "\t" + "public " + entityName + "() {}"+ LINE_SEPARATOR; 

		String propertyName = ""; // 属性名

		// 生成私有属性和get,set方法
		String propertiesStr = ""; // 私有属性字符串
		String getsetStr = ""; // get、set方法字符串
		String getsetProNameStr = "";
		String javaType = ""; // 数据库对应的java类型
		
		boolean bigDecimalFlag=true;
		boolean dateFlag=true;
		boolean timeFlag=true;
		boolean timestampFlag=true;
		boolean blobFlag=true;
		boolean clobFlag=true;
		boolean arrayFlag=true;
		
		StringBuffer tostring=new StringBuffer();
		
		if(config.isGenSerializable()){
			importStr += "import java.io.Serializable;" + LINE_SEPARATOR;
		}
		
		for (int i = 0; i < columnNames.size(); i++) {
			String columnName = columnNames.get(i);
			String columnType = columTypes.get(i);
			
			if(HoneyConfig.getHoneyConfig().isDbNamingToLowerCaseBefore()){
				columnName=columnName.toLowerCase();
			}
			
			if(HoneyConfig.getHoneyConfig().isUnderScoreAndCamelTransform()){
				propertyName=HoneyUtil.toCamelNaming(columnName);
			}else{
				propertyName=columnName;
			}
			
			getsetProNameStr = HoneyUtil.firstLetterToUpperCase(propertyName);
			javaType = HoneyUtil.getFieldType(columnType);
			
			//import
			if ("BigDecimal".equals(javaType) && bigDecimalFlag) {
				importStr += "import java.math.BigDecimal;" + LINE_SEPARATOR;
				bigDecimalFlag = false;
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
			

			propertiesStr += "\t" + "private " + javaType + " " + propertyName
					+ ";" + LINE_SEPARATOR;

			getsetStr += "\t" + "public " + javaType
					+ " get" + getsetProNameStr + "() {"
					+ LINE_SEPARATOR + "\t\t" + "return " + propertyName
					+ ";" + LINE_SEPARATOR + "\t}" + LINE_SEPARATOR
					+ LINE_SEPARATOR +

					"\t" + "public void set" + getsetProNameStr
					+ "(" + javaType + " " + propertyName + ") {"
					+ LINE_SEPARATOR + "\t\t" + "this." + propertyName + " = "
					+ propertyName + ";" + LINE_SEPARATOR + "\t}"
					+ LINE_SEPARATOR + LINE_SEPARATOR;
			
			if(config.isGenToString()){  //toString()
				tostring.append("\t\t str.append(\",").append(propertyName).append("=\").append(").append(propertyName).append(");");
				tostring.append("\t\t "+LINE_SEPARATOR );
			}
		}

		// 生成实体类文件
		String basePath=config.getBaseDir();
		if(!basePath.endsWith(File.separator)) basePath+=File.separator;
		String entitySaveDir = basePath+ config.getPackagePath().replace(".", File.separator)+ File.separator;
		File folder = new File(entitySaveDir);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		File entityFile = new File(entitySaveDir + entityName + ".java");
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(entityFile)));
			bw.write(packageStr + LINE_SEPARATOR);
			if(!"".equals(importStr))
			    bw.write(importStr + LINE_SEPARATOR);
			bw.write(authorComment+ LINE_SEPARATOR);
			bw.write("public class " + entityName );
			if(config.isGenSerializable()){
			  bw.write(" implements Serializable");
			}
			bw.write(" {" + LINE_SEPARATOR+ LINE_SEPARATOR);
			if(config.isGenSerializable()){
			  bw.write("\tprivate static final long serialVersionUID = "+HoneyUtil.genSerializableNum()+";"+ LINE_SEPARATOR);
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
			
			if(config.isGenToString()){ //toString()
				tostring.deleteCharAt(tostring.indexOf(","));
				tostring.insert(0,"\t\t"+LINE_SEPARATOR );
//				tostring.insert(0,"\t");
				
				tostring.append("\t\t str.append(\"]\");\t");  tostring.append("\t\t "+LINE_SEPARATOR );
				tostring.append("\t\t return str.toString();\t");  tostring.append("\t\t "+LINE_SEPARATOR );
				tostring.append("\t }");  tostring.append("\t\t "+LINE_SEPARATOR );
				
//				tostring.insert(0,"\t"+LINE_SEPARATOR ); 
				tostring.insert(0, "\t\t str.append(\""+entityName+"[\");\t");   
				tostring.insert(0,"\t"+LINE_SEPARATOR ); tostring.insert(0, "\t\t StringBuffer str=new StringBuffer();"); 
				tostring.insert(0,"\t"+LINE_SEPARATOR ); tostring.insert(0, "\t public String toString(){");
				
				bw.write(tostring.toString());
			}
		
			
			bw.write("}");
			bw.flush();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void genAllBeanFile() throws IOException {
		Logger.print("Generating...");

		List<Table> tables = getAllTables();
		Table table = null;
		for (int i = 0; i < tables.size(); i++) {
			table = tables.get(i);
			// 生成实体类
			genBeanFile(table);
		}

		Logger.print("Generate Success!");
		Logger.print("Please check: " + config.getBaseDir()+config.getPackagePath().replace(".", "\\"));
	}
	
	
	public void genSomeBeanFile(String tableList) throws IOException {
		
		String [] tables=tableList.split(",");

		Connection con = null;
		try {
			con =SessionFactory.getConnection();
			Table table = null;
		for (int i = 0; i < tables.length; i++) {
			table = getTable(tables[i], con);
			// 生成实体类
			genBeanFile(table);
		}
		con.close();

		}catch(Exception e){
			Logger.print(e.getMessage());
			if(e.getMessage().contains("You have an error in your SQL syntax;")&& e.getMessage().contains("where 1<>1")){
				Logger.print("Maybe the table name is the database key work. Please rename the tableName and test again.",e.getMessage());
			}
		}
		Logger.print("Generate Success!");
		Logger.print("Please check: " + config.getBaseDir()+config.getPackagePath().replace(".", "\\"));
	}

    // 获取所有表信息
	private List<Table> getAllTables() {
		List<Table> tables = new ArrayList<Table>();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
//			con = getConnection();
			con =SessionFactory.getConnection();
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
			}

			ps = con.prepareStatement(showTablesSql);
			rs = ps.executeQuery();

			while (rs.next()) {
				if (rs.getString(1) == null)
					continue;
				tables.add(getTable(rs.getString(1).trim(), con));
			}

			rs.close();
			ps.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tables;
	}

	private Table getTable(String tableName, Connection con)
			throws SQLException {
		PreparedStatement ps = con.prepareStatement("select * from "+ tableName + " where 1<>1;");
		ResultSet rs = ps.executeQuery();
		ResultSetMetaData rmeta = rs.getMetaData();
		
		Table table = new Table();
//		int index=tableName.indexOf(".");
//		if(index>-1) tableName=tableName.substring(index+1); //处理用数据库名.表名查的情况
		//该方法不可取.因为要是用关键字作表名,别的suid操作都是要带数据库名.
		
		table.setTableName(tableName);

		int columCount = rmeta.getColumnCount();
		for (int i = 1; i <=columCount; i++) {
			table.getColumNames().add(rmeta.getColumnName(i).trim());
			table.getColumTypes().add(rmeta.getColumnTypeName(i).trim());
		}

		rs.close();
		ps.close();

		return table;
	}

}

class Table {
	private String tableName; // 表名
	private List<String> columNames = new ArrayList<String>(); // 列名集合
	private List<String> columTypes = new ArrayList<String>(); // 列类型集合，列类型严格对应java类型

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public List<String> getColumNames() {
		return columNames;
	}

	public List<String> getColumTypes() {
		return columTypes;
	}

}

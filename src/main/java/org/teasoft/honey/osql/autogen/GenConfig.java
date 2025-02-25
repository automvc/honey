package org.teasoft.honey.osql.autogen;

import java.io.File;

import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * 生成Javabean的相关配置信息.Gen Javabean config.
 * @author Kingstar
 *
 */
public class GenConfig {
	private String encode = "UTF-8"; // 字符编码

	private String baseDir = "";
	private String packagePath = "";
//	private String dbName = ""; // 
	private String dbName; // 数据库类型 MySQL, Oracle等     fixed bug. V2.1
	private String queryTableSql = ""; // 查询所有表名的SQL语句，MySQL,Oracle和SQL Server不用设置
	private boolean genToString;
	private boolean genSerializable=true; //V1.17默认值改为true
	
	private String queryColumnCommnetSql;
	private String queryTableCommnetSql;
	private boolean genComment;
	private int commentPlace; //1:after field name at the same line; 2:last line
	private String entityNamePre;
	
	//V1.11
	private boolean override;
	
	//V1.11
	private boolean genFieldFile;
	private String fieldFileRelativeFolder="field";
	private String fieldFilePrefix="";
	private String fieldFileSuffix="_F";
	private boolean genFieldAll=true; //V1.17
	private boolean genSelfName=true; //V2.0  first letter is lowerCase
	
	private boolean genGetSet=true;  //2.1 默认生成
	
	//2.1 support lombok
	private boolean lombokSetter;
	private boolean lombokGetter;
	private boolean lombokData;
	
	//2.1.8
	private boolean genSwagger;
	
	public String getEncode() {
		return encode;
	}

	public void setEncode(String encode) {
		this.encode = encode;
	}

	public String getBaseDir() {
		if("".equals(baseDir)) {
			baseDir=System.getProperty("user.dir")+"#src#main#java#".replace("#", File.separator); //syn 2.4.0
		}
		return baseDir;
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	public String getQueryTableSql() {
		return queryTableSql;
	}

	public void setQueryTableSql(String queryTableSql) {
		this.queryTableSql = queryTableSql;
	}

	public String getDbName() {
		if(dbName==null) return HoneyConfig.getHoneyConfig().getDbName();//V1.17
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getPackagePath() {
		if("".equals(packagePath))  return "entity";
		return packagePath;
	}

	public void setPackagePath(String packagePath) {
		this.packagePath = packagePath;
	}

	public boolean isGenToString() {
		return genToString;
	}

	public void setGenToString(boolean genToString) {
		this.genToString = genToString;
	}

	public boolean isGenSerializable() {
		return genSerializable;
	}

	public void setGenSerializable(boolean genSerializable) {
		this.genSerializable = genSerializable;
	}

	
	public boolean isGenComment() {
		return genComment;
	}

	public void setGenComment(boolean genComment) {
		this.genComment = genComment;
	}

	public int getCommentPlace() {
		return commentPlace;
	}

	/**
	 * set comment place,If genComment is true;
	 * @param commentPlace 1:after field name at the same line; 2:last line.  default is 1.
	 */
	public void setCommentPlace(int commentPlace) {
		this.commentPlace = commentPlace;
	}

	/**
	 * @deprecated !!!Automatic translation is highly recommended
	 */
	@Deprecated
	public String getEntityNamePre() {
		return entityNamePre;
	}

	/**
	 * @deprecated !!!Automatic translation is highly recommended
	 * @param entityNamePre  prefix of entityName
	 */
	@Deprecated
	public void setEntityNamePre(String entityNamePre) {
		this.entityNamePre = entityNamePre;
	}

	public String getQueryColumnCommnetSql() {
		return queryColumnCommnetSql;
	}

	/**
	 * @param queryColumnCommnetSql the first select column is column name, second is comment;parameter table_name=?(use placeholder)
	 */
	public void setQueryColumnCommnetSql(String queryColumnCommnetSql) {
		this.queryColumnCommnetSql = queryColumnCommnetSql;
	}

	public String getQueryTableCommnetSql() {
		return queryTableCommnetSql;
	}

	/**
	 * @param queryTableCommnetSql the first select column is column name, second is comment;parameter table_name=?(use placeholder)
	 */
	public void setQueryTableCommnetSql(String queryTableCommnetSql) {
		this.queryTableCommnetSql = queryTableCommnetSql;
	}
	
	public boolean isOverride() {
		return override;
	}

	public void setOverride(boolean override) {
		this.override = override;
	}

	public boolean isGenFieldFile() {
		return genFieldFile;
	}

	public void setGenFieldFile(boolean genFieldFile) {
		this.genFieldFile = genFieldFile;
	}

	public String getFieldFileRelativeFolder() {
		return fieldFileRelativeFolder;
	}

	public void setFieldFileRelativeFolder(String fieldFileRelativeFolder) {
		this.fieldFileRelativeFolder = fieldFileRelativeFolder;
	}

	public String getFieldFileSuffix() {
		return fieldFileSuffix;
	}

	public void setFieldFileSuffix(String fieldFileSuffix) {
		this.fieldFileSuffix = fieldFileSuffix;
	}

	public String getFieldFilePrefix() {
		return fieldFilePrefix;
	}

	public void setFieldFilePrefix(String fieldFilePrefix) {
		this.fieldFilePrefix = fieldFilePrefix;
	}

	public boolean isGenFieldAll() {
		return genFieldAll;
	}

	public void setGenFieldAll(boolean genFieldAll) {
		this.genFieldAll = genFieldAll;
	}

	public boolean isGenSelfName() {
		return genSelfName;
	}

	public void setGenSelfName(boolean genSelfName) {
		this.genSelfName = genSelfName;
	}

	public boolean isLombokSetter() {
		return lombokSetter;
	}

	public void setLombokSetter(boolean lombokSetter) {
		this.lombokSetter = lombokSetter;
	}

	public boolean isLombokGetter() {
		return lombokGetter;
	}

	public void setLombokGetter(boolean lombokGetter) {
		this.lombokGetter = lombokGetter;
	}

	public boolean isLombokData() {
		return lombokData;
	}

	public void setLombokData(boolean lombokData) {
		this.lombokData = lombokData;
	}

	public boolean isGenGetSet() {
		return genGetSet;
	}

	public void setGenGetSet(boolean genGetSet) {
		this.genGetSet = genGetSet;
	}

	public boolean isGenSwagger() {
		return genSwagger;
	}

	public void setGenSwagger(boolean genSwagger) {
		this.genSwagger = genSwagger;
	}

}

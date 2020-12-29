package org.teasoft.honey.osql.autogen;

public class GenConfig {
	private String encode = "UTF-8"; // 字符编码

	private String baseDir = "";
	private String packagePath = "";
	private String dbName = ""; // 数据库类型 MySQL, Oracle等
	private String queryTableSql = ""; // 查询所有表名的SQL语句，MySQL,Oracle和SQL Server不用设置
	private boolean genToString;
	private boolean genSerializable;
	
	private String queryColumnCommnetSql;
	private String queryTableCommnetSql;
	private boolean genComment;
	private int commentPlace; //1:after field name at the same line; 2:last line
	private String entityNamePre;

	public String getEncode() {
		return encode;
	}

	public void setEncode(String encode) {
		this.encode = encode;
	}

	public String getBaseDir() {
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
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getPackagePath() {
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
	
}

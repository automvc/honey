package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.Cache;
import org.teasoft.bee.osql.CallableSql;
import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.MapSql;
import org.teasoft.bee.osql.MapSuid;
import org.teasoft.bee.osql.MoreObjToSQL;
import org.teasoft.bee.osql.MoreTable;
import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.ObjToSQL;
import org.teasoft.bee.osql.ObjToSQLRich;
import org.teasoft.bee.osql.PreparedSql;
import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.SuidRich;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.NoConfigException;
import org.teasoft.honey.osql.dialect.LimitOffsetPaging;
import org.teasoft.honey.osql.dialect.NoPagingSupported;
import org.teasoft.honey.osql.dialect.mysql.MySqlFeature;
import org.teasoft.honey.osql.dialect.oracle.OracleFeature;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerFeature;
import org.teasoft.honey.osql.name.OriginalName;
import org.teasoft.honey.osql.name.UnderScoreAndCamelName;
import org.teasoft.honey.osql.name.UpperCaseUnderScoreAndCamelName;

/**
 * @author Kingstar
 * @since  1.0
 */
public class HoneyFactory {
	
	private Suid suid;
	private SuidRich suidRich;
	private BeeSql beeSql;
	private ObjToSQL objToSQL;
	private ObjToSQLRich objToSQLRich;
	private PreparedSql preparedSql;
	private CallableSql callableSql;
	private Condition condition;
	
	//@since  1.7
	private MoreTable moreTable;
	private MoreObjToSQL moreObjToSQL;
	
	//@since  1.9
	private MapSql mapSql;
	private MapSuid mapSuid;
	
	private DbFeature dbFeature;
	private NameTranslate nameTranslate;
	private static Cache cache;
	
	static {
		boolean nocache = HoneyConfig.getHoneyConfig().cache_nocache;
		if (nocache) cache= new NoCache(); //v1.7.2
		else cache= new DefaultCache(); 
	}
	
	public HoneyFactory(){
	}

	public Suid getSuid() {
		if(suid==null) return new ObjSQL();
		return suid; //可以通过配置spring bean的方式注入
	}

	public void setSuid(Suid suid) {
		this.suid = suid;
	}
	
	public SuidRich getSuidRich() {
		if(suidRich==null) return new ObjSQLRich();
		return suidRich;
	}

	public void setSuidRich(SuidRich suidRich) {
		this.suidRich = suidRich;
	}
	
	public MoreTable getMoreTable() {
		if(moreTable==null) return new MoreObjSQL();
		return moreTable;
	}

	public void setMoreTable(MoreTable moreTable) {
		this.moreTable = moreTable;
	}

	public BeeSql getBeeSql() {
		if(this.beeSql==null) return new SqlLib();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

	public ObjToSQL getObjToSQL() {
		if(objToSQL==null) return new ObjectToSQL();
		return objToSQL;
	}

	public void setObjToSQL(ObjToSQL objToSQL) {
		this.objToSQL = objToSQL;
	}

	public ObjToSQLRich getObjToSQLRich() {
		if(objToSQLRich==null) return new ObjectToSQLRich();
		return objToSQLRich;
	}

	public void setObjToSQLRich(ObjToSQLRich objToSQLRich) {
		this.objToSQLRich = objToSQLRich;
	}

	public MoreObjToSQL getMoreObjToSQL() {
		if(moreObjToSQL==null) return new MoreObjectToSQL();
		return moreObjToSQL;
	}

	public void setMoreObjToSQL(MoreObjToSQL moreObjToSQL) {
		this.moreObjToSQL = moreObjToSQL;
	}

	public PreparedSql getPreparedSql() {
		if(preparedSql==null) return new PreparedSqlLib();
		return preparedSql;
	}

	public void setPreparedSql(PreparedSql preparedSql) {
		this.preparedSql = preparedSql;
	}

	public CallableSql getCallableSql() {
		if(callableSql==null) return new CallableSqlLib();
		return callableSql;
	}

	public void setCallableSql(CallableSql callableSql) {
		this.callableSql = callableSql;
	}
	
	public Condition getCondition() {
		if(condition==null) return new ConditionImpl();
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public MapSql getMapSql() {
		if (mapSql == null) return new MapSqlImpl();
		return mapSql;
	}

	public void setMapSql(MapSql mapSql) {
		this.mapSql = mapSql;
	}

	public MapSuid getMapSuid() {
		if (mapSuid == null) return new MapSuidImpl();
		return mapSuid;
	}

	public void setMapSuid(MapSuid mapSuid) {
		this.mapSuid = mapSuid;
	}
	
	public Cache getCache() {
		if (cache == null) {
			boolean nocache = HoneyConfig.getHoneyConfig().cache_nocache;
			if (nocache) return new NoCache(); //v1.7.2
			return new DefaultCache();  
		} else {
			return cache;
		}
	}

//	public void setCache(Cache cache) {
//		this.cache = cache;
//	}
	
	public DbFeature getDbFeature() {

		String dbName = HoneyContext.getRealTimeDbName();
		if (dbName != null) return _getDbDialectFeature(dbName);
//		dbName == null则表示不同时使用多种数据库
		if (dbFeature != null)
			return dbFeature;
		else
			return _getDbDialectFeature();
	}
	
	public void setDbFeature(DbFeature dbFeature) {
		this.dbFeature = dbFeature;
	}
	
	NameTranslate getInitNameTranslate() {
		if(nameTranslate==null) {
			//since 1.7.2
			int translateType=HoneyConfig.getHoneyConfig().naming_translateType;
			if(translateType==1) nameTranslate=new UnderScoreAndCamelName();
			else if(translateType==2) nameTranslate=new UpperCaseUnderScoreAndCamelName();
			else if(translateType==3) nameTranslate=new OriginalName();
			else nameTranslate=new UnderScoreAndCamelName();  //if the value is not 1,2,3
				
			return nameTranslate;
		}else {
			return nameTranslate;
		}
	}

//	public void setNameTranslate(NameTranslate nameTranslate) {
//		this.nameTranslate = nameTranslate;
//		HoneyContext.clearFieldNameCache();
//	}
	
	DbFeature _getDbDialectFeature() {
		return _getDbDialectFeature(HoneyContext.getDbDialect());
	}

	private DbFeature _getDbDialectFeature(String dbName) {
		if (DatabaseConst.MYSQL.equalsIgnoreCase((dbName)) || DatabaseConst.MariaDB.equalsIgnoreCase((dbName)))
			return new MySqlFeature();
		else if (DatabaseConst.ORACLE.equalsIgnoreCase((dbName)))
			return new OracleFeature();
		else if (DatabaseConst.SQLSERVER.equalsIgnoreCase((dbName)))
			return new SqlServerFeature();
		else if (_isLimitOffsetDB())
			return new LimitOffsetPaging(); //v1.8.15 
		else if (dbName != null)
			return new NoPagingSupported(); //v1.8.15 当没有用到分页功能时,不至于报错.
		else { //要用setDbFeature(DbFeature dbFeature)设置自定义的实现类
			throw new NoConfigException("Error: Do not set the DbFeature implements class or do not set the database name. "); //v1.8.15
			//todo 也有可能是没开DB服务.
		}
	}
	
	private boolean _isLimitOffsetDB() {
		return  DatabaseConst.H2.equalsIgnoreCase((HoneyContext.getDbDialect())) 
				|| DatabaseConst.SQLite.equalsIgnoreCase((HoneyContext.getDbDialect()))
				|| DatabaseConst.PostgreSQL.equalsIgnoreCase((HoneyContext.getDbDialect()));
	}
	
}

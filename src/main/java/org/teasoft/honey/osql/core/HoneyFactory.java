package org.teasoft.honey.osql.core;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

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
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.honey.osql.dialect.LimitOffsetPaging;
import org.teasoft.honey.osql.dialect.NoPagingSupported;
import org.teasoft.honey.osql.dialect.mysql.MySqlFeature;
import org.teasoft.honey.osql.dialect.oracle.OracleFeature;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerFeature;
import org.teasoft.honey.osql.interccept.DefaultInterceptorChain;
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
	
	private InterceptorChain interceptorChain;
	
	static {
       cache=initCache();
	}
	
	public HoneyFactory() {

	}
	
	//NoCache>Custom Cache>BeeExtRedisCache>DefaultBeeExtCache>DefaultCache
	private static Cache initCache() {
//		System.err.println(">>>>>>>>>>>>>>>>>>>initCache...");
		Cache cache;
		boolean nocache = HoneyConfig.getHoneyConfig().cache_nocache;
		boolean useLevelTwo=HoneyConfig.getHoneyConfig().cache_useLevelTwo;
		if (nocache) {
			cache= new NoCache(); //v1.7.2
		}else if(useLevelTwo) {//V1.11
			ServiceLoader<Cache> caches = ServiceLoader.load(Cache.class);
			Cache cache1=null;
			Cache cache2=null;
			String className;
			int num=0;
	        Iterator<Cache> cacheIterator = caches.iterator();
			Cache ca;
			while (cacheIterator.hasNext()) {
				try {
					ca = cacheIterator.next();
					num++;
					className = ca.getClass().getName();
					if ("org.teasoft.beex.cache.redis.BeeExtRedisCache".equals(className))
						cache1 = ca;
					else
						cache2 = ca;
					Logger.warn("[Bee] ==========load Cache's Service by ServiceLoader:" + className);
				} catch (ServiceConfigurationError e) {
					Logger.error(e.getMessage(), e);
				}
			}
		
			if(num!=0) Logger.warn("[Bee] ==========load Cache's Service number: " +num);
			if(cache2!=null) {//此种,超过1个则没有指定,使用的是后一个
				cache=cache2;
				Logger.warn("[Bee] ==========use Cache's Service is:" + cache2.getClass().getName());
			}else if(cache1!=null) {
				cache=cache1;
				Logger.warn("[Bee] ==========use Cache's Service is:" + cache1.getClass().getName());
		    }else {
				cache=new DefaultBeeExtCache();
			}
		}else {
			cache= new DefaultCache(); 
		}
		
		return cache;
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
		if (cache == null) cache = initCache();
		return cache;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}
	
	public DbFeature getDbFeature() {

		String dbName = HoneyContext.getRealTimeDbName();
		if (dbName != null) {
			String logMsg="[Bee] ========= get the dbName in real time is :" + dbName;
			Logger.info(logMsg);
			return _getDbDialectFeature(dbName);
		}
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
//	使用:
//	NameTranslateHandle.setNameTranslat(nameTranslat) { // for set customer naming.
	
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

	public InterceptorChain getInterceptorChain() {
		if (interceptorChain == null) return new DefaultInterceptorChain();
		return interceptorChain;
	}

	public void setInterceptorChain(InterceptorChain interceptorChain) {
		this.interceptorChain = interceptorChain;
	}
	
}

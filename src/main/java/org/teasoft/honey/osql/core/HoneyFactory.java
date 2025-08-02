package org.teasoft.honey.osql.core;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.bee.mongodb.MongodbRawSql;
import org.teasoft.bee.mvc.service.ObjSQLRichService;
import org.teasoft.bee.mvc.service.ObjSQLService;
import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.Cache;
import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.MoreObjToSQL;
import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.ObjToSQL;
import org.teasoft.bee.osql.ObjToSQLRich;
import org.teasoft.bee.osql.api.CallableSql;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.MapSql;
import org.teasoft.bee.osql.api.MapSuid;
import org.teasoft.bee.osql.api.MoreTable;
import org.teasoft.bee.osql.api.PreparedSql;
import org.teasoft.bee.osql.api.Suid;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.bee.osql.chain.UnionSelect;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.dialect.DbFeatureRegistry;
import org.teasoft.bee.osql.exception.NoConfigException;
import org.teasoft.bee.osql.exception.NotSupportedException;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.chain.UnionSelectImpl;
import org.teasoft.honey.osql.dialect.*;
import org.teasoft.honey.osql.dialect.mysql.MySqlFeature;
import org.teasoft.honey.osql.dialect.oracle.OracleFeature;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerFeature;
import org.teasoft.honey.osql.interccept.InterceptorChainRegistry;
import org.teasoft.honey.osql.mongodb.MongodbBeeSqlRegister;
import org.teasoft.honey.osql.name.DbUpperAndJavaLower;
import org.teasoft.honey.osql.name.OriginalName;
import org.teasoft.honey.osql.name.UnderScoreAndCamelName;
import org.teasoft.honey.osql.name.UpperCaseUnderScoreAndCamelName;
import org.teasoft.honey.osql.serviceimpl.ObjSQLRichServiceImpl;
import org.teasoft.honey.osql.serviceimpl.ObjSQLServiceImpl;

/**
 * Honey工厂类.Honey Factory class.
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

	// @since 1.7
	private MoreTable moreTable;
	private MoreObjToSQL moreObjToSQL;

	// @since 1.9
	private MapSql mapSql;
	private MapSuid mapSuid;

//	private DbFeature dbFeature;
	private NameTranslate nameTranslate;
	private static Cache cache;

	// @since 2.0
	private UnionSelect unionSelect;

	private InterceptorChain interceptorChain;

	// @since 2.0
	private MongodbBeeSql mongodbBeeSql;

	// @since 2.1
	private MongodbRawSql mongodbRawSql;
	private ObjSQLService objSQLService;
	private ObjSQLRichService objSQLRichService;

	private boolean isOceanBasePrintFirst = true;

	private static final byte[] lock = new byte[0];

	static {
		cache = initCache();
	}

	/**
	 * get the instance by BeeFactory.getHoneyFactory()
	 */
	HoneyFactory() {}

	private static boolean getUseLevelTwo() {
		return HoneyConfig.getHoneyConfig().cache_useLevelTwo;
	}

	// NoCache>Custom Cache>BeeExtRedisCache>DefaultBeeExtCache>DefaultCache
	private static Cache initCache() {
		Cache cache;
		boolean nocache = HoneyConfig.getHoneyConfig().cache_nocache;
		boolean useLevelTwo = getUseLevelTwo();
		if (nocache) {
			Logger.warn("[Bee] ==========Now the Cache type is: nocache.");
			cache = new NoCache(); // v1.7.2
		} else if (useLevelTwo) {// V1.11
			ServiceLoader<Cache> caches = ServiceLoader.load(Cache.class);
			Cache cache1 = null;
			Cache cache2 = null;
			String className;
			int num = 0;
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
					Logger.warn(e.getMessage(), e);
				}
			}

			if (num != 0) Logger.warn("[Bee] ==========load Cache's Service number: " + num);
			if (cache2 != null) {// 此种,超过1个则没有指定,使用的是后一个
				cache = cache2;
				Logger.warn("[Bee] ==========use Cache's Service is:" + cache2.getClass().getName());
			} else if (cache1 != null) {
				cache = cache1;
				Logger.warn("[Bee] ==========use Cache's Service is:" + cache1.getClass().getName());
			} else {
				cache = new DefaultBeeExtCache();
			}
		} else {
			cache = new DefaultCache();
		}

		return cache;
	}

	public Suid getSuid() {
		if (suid == null) {
			if (isMongodb())
				return new MongodbObjSQL(); // 2.0
			else
				return new ObjSQL();
		}
		return suid; // 可以通过配置spring bean的方式注入
	}

	public void setSuid(Suid suid) {
		this.suid = suid;
	}

	public SuidRich getSuidRich() {
		if (suidRich == null) {
			if (isMongodb())
				return new MongodbObjSQLRich(); // 2.0
			else
				return new ObjSQLRich();
		}
		return suidRich;
	}

	// 是多数据源,有同时使用多种不同类型DB
	private boolean isMongodb() {
		boolean justMongodb = HoneyConfig.getHoneyConfig().multiDS_justMongodb; // 2.1
		boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
		boolean isDifferentDbType = HoneyConfig.getHoneyConfig().multiDS_differentDbType;
//		return (justMongodb || (!(enableMultiDs && isDifferentDbType) && HoneyUtil.isMongoDB()) );
		return (justMongodb || (((enableMultiDs && isDifferentDbType) || !enableMultiDs) && HoneyUtil.isMongoDB())); // V2.1.8
	}

	public void setSuidRich(SuidRich suidRich) {
		this.suidRich = suidRich;
	}

	// 同时使用多种类型的DB,在为Mongodb获取时,需要用声明是获取给Mongodb用的方法
	public Suid getSuidForMongodb() {// 2.0
		return new MongodbObjSQL();
	}

	// 同时使用多种类型的DB,在为Mongodb获取时,需要用声明是获取给Mongodb用的方法
	public SuidRich getSuidRichForMongodb() { // 2.0
		return new MongodbObjSQLRich();
	}

	public MoreTable getMoreTable() {
		if (moreTable == null) {
			if (isMongodb()) {// 2.4.2
				throw new NotSupportedException("MoreTable do not support MongoDB!"); // 2.4.2
			}
			return new MoreObjSQL();
		}
		return moreTable;
	}

	public void setMoreTable(MoreTable moreTable) {
		this.moreTable = moreTable;
	}

	public BeeSql getBeeSql() {
		if (this.beeSql == null) {
			boolean isAndroid = HoneyConfig.getHoneyConfig().isAndroid;
			boolean isHarmony = HoneyConfig.getHoneyConfig().isHarmony;
			if (isAndroid || isHarmony) {
				beeSql = new SqlLibForApp(); // app环境,可以只用一个实例
				return beeSql;
			} else {
				return new SqlLib();
			}
		}
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

	public MongodbBeeSql getMongodbBeeSql() {
		if (mongodbBeeSql == null) return MongodbBeeSqlRegister.getInstance();
		return mongodbBeeSql;
	}

	public void setMongodbBeeSql(MongodbBeeSql mongodbBeeSql) {
		this.mongodbBeeSql = mongodbBeeSql;
	}

	public MongodbRawSql getMongodbRawSql() {
		if (mongodbBeeSql == null) return new MongodbRawSqlLib();
		return mongodbRawSql;
	}

	public void setMongodbRawSql(MongodbRawSql mongodbRawSql) {
		this.mongodbRawSql = mongodbRawSql;
	}

	public ObjToSQL getObjToSQL() {
		if (objToSQL == null) return new ObjectToSQL();
		return objToSQL;
	}

	public void setObjToSQL(ObjToSQL objToSQL) {
		this.objToSQL = objToSQL;
	}

	public ObjToSQLRich getObjToSQLRich() {
		if (objToSQLRich == null) return new ObjectToSQLRich();
		return objToSQLRich;
	}

	public void setObjToSQLRich(ObjToSQLRich objToSQLRich) {
		this.objToSQLRich = objToSQLRich;
	}

	public MoreObjToSQL getMoreObjToSQL() {
		if (moreObjToSQL == null) return new MoreObjectToSQL();
		return moreObjToSQL;
	}

	public void setMoreObjToSQL(MoreObjToSQL moreObjToSQL) {
		this.moreObjToSQL = moreObjToSQL;
	}

	public PreparedSql getPreparedSql() {
		if (preparedSql == null) return new PreparedSqlLib();
		return preparedSql;
	}

	public void setPreparedSql(PreparedSql preparedSql) {
		this.preparedSql = preparedSql;
	}

	public CallableSql getCallableSql() {
		if (callableSql == null) return new CallableSqlLib();
		return callableSql;
	}

	public void setCallableSql(CallableSql callableSql) {
		this.callableSql = callableSql;
	}

	public Condition getCondition() {
		if (condition == null) return new ConditionImpl();
		return condition;
	}

	public void setCondition(Condition condition) {
		if (condition != null)
			this.condition = condition.clone();
		else
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

	public UnionSelect getUnionSelect() {
		if (unionSelect == null) return new UnionSelectImpl();
		return unionSelect;
	}

	public void setUnionSelect(UnionSelect unionSelect) {
		this.unionSelect = unionSelect;
	}

	public ObjSQLService getObjSQLService() {
		if (objSQLService == null) return new ObjSQLServiceImpl();
		return objSQLService;
	}

	public void setObjSQLService(ObjSQLService objSQLService) {
		this.objSQLService = objSQLService;
	}

	public ObjSQLRichService getObjSQLRichService() {
		if (objSQLRichService == null) return new ObjSQLRichServiceImpl();
		return objSQLRichService;
	}

	public void setObjSQLRichService(ObjSQLRichService objSQLRichService) {
		this.objSQLRichService = objSQLRichService;
	}

	public Cache getCache() {
		if (cache == null) {
			synchronized (lock) {
				if (cache == null) _setCache(initCache());
			}
		}
		return cache;
	}

	public void setCache(Cache cache) {
		_setCache(cache);
	}

	private static void _setCache(Cache cache) {
		HoneyFactory.cache = cache;
	}

	public DbFeature getDbFeature() {

		String dbName = HoneyContext.getRealTimeDbName();
		if (dbName != null) {
			String logMsg = "========= get the dbName in real time is :" + dbName;
			Logger.logSQL(logMsg);
			return _getDbDialectFeature(dbName);
		}
//		else dbName == null则表示不同时使用多种数据库

//		if (dbFeature != null)
//			return dbFeature;  //closed 2.5.2
//		else
		return _getDbDialectFeature();
	}

	// closed V2.5.2 ; can use DbFeatureRegistry.register(databaseName, dbFeature)
//	public void setDbFeature(DbFeature dbFeature) {
//		this.dbFeature = dbFeature;
//	}

	NameTranslate getInitNameTranslate() {
		if (nameTranslate == null) {
			// since 1.7.2
			int translateType = HoneyConfig.getHoneyConfig().naming_translateType;
			if (translateType == 1) nameTranslate = new UnderScoreAndCamelName();
			else if (translateType == 2) nameTranslate = new UpperCaseUnderScoreAndCamelName();
			else if (translateType == 3) nameTranslate = new OriginalName();
			else if (translateType == 4) nameTranslate = new DbUpperAndJavaLower(); // V1.17
			else nameTranslate = new UnderScoreAndCamelName(); // if the value is not 1,2,3,4

			return nameTranslate;
		} else {
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

		// V1.11
		// 自定义的DbFeature,添加到DbFeature注册器.
		DbFeature dbFeature = DbFeatureRegistry.getDbFeature(dbName);
		if (dbFeature != null) return dbFeature;

		if (DatabaseConst.MYSQL.equalsIgnoreCase((dbName)) || DatabaseConst.MariaDB.equalsIgnoreCase((dbName)))
			return new MySqlFeature();
		else if (DatabaseConst.ORACLE.equalsIgnoreCase((dbName))) return new OracleFeature();
		else if (DatabaseConst.SQLSERVER.equalsIgnoreCase((dbName))) return new SqlServerFeature();
		else if (_isLimitOffsetDB()) return new LimitOffsetPaging(); // v1.8.15
		else if (_isFrontLimitDB()) return new HSqlDbFrontLimitPaging(); // 2.1
		else if (_isOffsetFetchDB()) return new OffsetFetchPaging(); // 2.1
		else if (_isLimitMN()) return new MySqlFeature(); // 2.1
		else if (DatabaseConst.OceanBase.equalsIgnoreCase((dbName))) { // 2.1.10
			String oceanbaseMode = HoneyConfig.getHoneyConfig().oceanbaseMode;
			boolean isDifferentDbType = HoneyConfig.getHoneyConfig().multiDS_differentDbType;
			if (isDifferentDbType || isOceanBasePrintFirst) {
				isOceanBasePrintFirst = false;
				Logger.logSQL("Using OceanBase,the Mode is : " + oceanbaseMode);
			}
			if (DatabaseConst.ORACLE.equalsIgnoreCase(oceanbaseMode))
				return new OracleFeature();
			else
				return new MySqlFeature();
		} else if (dbName != null) {
			return new NoPagingSupported(); // v1.8.15 当没有用到分页功能时,不至于报错.
		} else { // 要用setDbFeature(DbFeature dbFeature)设置自定义的实现类
			throw new NoConfigException(
					"Error: Do not set the DbFeature implements class or do not set the database name. "); // v1.8.15
			// 也有可能是没开DB服务.
		}
	}

	private boolean _isLimitOffsetDB() {
		String dbName = HoneyContext.getDbDialect();
		boolean comm = DatabaseConst.H2.equalsIgnoreCase(dbName) || DatabaseConst.SQLite.equalsIgnoreCase(dbName)
				|| DatabaseConst.PostgreSQL.equalsIgnoreCase(dbName) || DatabaseConst.MsAccess.equalsIgnoreCase(dbName);

		if (comm) return comm;

		boolean other = HoneyConfig.getHoneyConfig().pagingWithLimitOffset;
		return comm || other;
	}

	private boolean _isLimitMN() {
		String dbName = HoneyContext.getDbDialect();
		boolean f = DatabaseConst.Cubrid.equalsIgnoreCase(dbName);
		return f;
	}

	private boolean _isFrontLimitDB() {
		String dbName = HoneyContext.getDbDialect();
		boolean comm = DatabaseConst.HSQLDB.equalsIgnoreCase(dbName) || DatabaseConst.HSQL.equalsIgnoreCase(dbName);
		return comm;
	}

	private boolean _isOffsetFetchDB() {
		String dbName = HoneyContext.getDbDialect();
		boolean comm = DatabaseConst.Derby.equalsIgnoreCase(dbName) || DatabaseConst.Firebird.equalsIgnoreCase(dbName);
		return comm;
	}

	public InterceptorChain getInterceptorChain() {
		// 当前对象没有设置拦截器链,则使用全局的
//		if (interceptorChain == null) return InterceptorChainRegistry.getInterceptorChain();
//		return interceptorChain;

		if (interceptorChain == null) return InterceptorChainRegistry.getInterceptorChain();
		return HoneyUtil.copy(interceptorChain);
	}

	public void setInterceptorChain(InterceptorChain interceptorChain) {
		this.interceptorChain = interceptorChain;
	}

}

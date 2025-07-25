/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.ShardingErrorException;
import org.teasoft.bee.sharding.DsTabStruct;
import org.teasoft.bee.sharding.ShardingBean;
import org.teasoft.bee.sharding.ShardingSimpleStruct;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.ConditionImpl;
import org.teasoft.honey.osql.core.Expression;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.osql.core.OpType;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.osql.interccept.EmptyInterceptor;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.SetList;
import org.teasoft.honey.util.StringUtils;

/**
 * 分片拦截器.Sharding Interceptor.
 * 系统没有设置为sharding模式,则调用该类方法会直接返回.
 * @author AiTeaSoft
 * @since 2.0
 */
public class ShardingInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = 1595293159217L;

	private final String partKey = StringConst.PREFIX + "ShardingInterceptor";
	private boolean showShardingSQL = getShowShardingSQL();

	private boolean getShowShardingSQL() {
		return HoneyConfig.getHoneyConfig().showSQL && HoneyConfig.getHoneyConfig().showShardingSQL;
	}

	@Override
	public Object beforePasreEntity(Object entity, SuidType suidType) {

		boolean isSharding = ShardingUtil.isSharding();
		if (!isSharding) return entity;

		// 2.0从表对应的从实体,不用来计算分片. 从表分片的下标与主表的一致
		if (HoneyContext.isInterceptorSubEntity()) return entity;

//		if (isSkip(entity,suidType)) return entity; //close in 2.4.2
		if (entity == null) return entity; // 2.4.2

		boolean moreTableSelectShardingFlag = HoneyContext
				.isTrueInSysCommStrInheritableLocal(StringConst.MoreTableSelectShardingFlag);

		String tableName = _toTableName(entity);

		if (isSharding && ShardingRegistry.isBroadcastTab(tableName)) { // 广播表

			if (suidType == SuidType.SELECT) {
				// 第一个
//				Map<String, Set<String>> map = ShardingRegistry.getFullNodes(tableName);
				this.ds = ShardingRegistry.getRandDs(tableName); // 随机从一个ds获取
			} else { // 更改要对所在ds
				regFullInModifyAllNodes(suidType);
			}
			return entity;
		}

		if (isSharding && suidType == SuidType.DDL) { // DDL
			regFullInModifyAllNodes(suidType);
			return entity;
		}

		// Hint指定,提前返回.
		if (isHintBoth()) { // ds,tab都指定了,就提前返回; 批量插入也可提前返回
			return entity;
		} else if (isHintTab()) { // 只指定了tab,通过反查,查ds
			String t_ds = ShardingRegistry.getDsByTab(HoneyContext.getAppointTab());
			// 可以反查时,直接返回;不能才计算
			if (StringUtils.isNotBlank(t_ds)) {
				HoneyContext.setAppointDS(t_ds);
				return entity;
			}
		} else if (isHintDs()) { // 只指定了ds
			this.ds = HoneyContext.getAppointDS();
//			return entity;  //不能返回, 还要寻找tab分片
		}

		boolean isByIdWithClass = false;
		if (entity.getClass().equals(Class.class)) { // 2.4.2
			isByIdWithClass = true;
		}

		String key;
		if (isByIdWithClass)
			key = partKey + "_beforePasreEntity" + ((Class) entity).getName();
		else
			key = partKey + "_beforePasreEntity" + entity.getClass().getName();// 1

		Boolean flag = HoneyContext.getCustomFlagMap(key);
		if (Boolean.FALSE.equals(flag)) return entity;

		Field fields[] = null;
		Object oldEntity = null; // the type is class

		if (isByIdWithClass) {// 仅将class转成实体对象;id放在condtion,不用在实体设置
			fields = HoneyUtil.getFields((Class) entity);
			oldEntity = entity;
			try {
				entity = ((Class) entity).newInstance();
			} catch (Exception e) {
				Logger.warn(e.getMessage());
			}
//			OneTimeParameter.getAttribute(StringConst.ByIdWithClassForSharding);
		} else {
			fields = HoneyUtil.getFields(entity.getClass()); // 2
		}

		int len = fields.length;
		boolean isHas = false;
		int annoCounter = 0;
		DsTabStruct dsTabStruct = null;
//		boolean hasShardingAnno = false;
		ShardingBean shardingBean = null;

		Condition condition = HoneyContext.getConditionLocal();

		// for 查找是否使用了注解
		for (int i = 0; i < len; i++) {
			// 处理sharding的5 case
			// 1. Sharding Anno condtion null
			// 2. Sharding Anno condtion is not null(whether is sharding value)
			// 3. no config sharding
			// 4. config sharding condtion null
			// 5. config sharding condtion is not null(whether is sharding value)
			// 4,5 the javabean also can set value

			if (ShardingUtil.isSharding() && AnnoUtil.isShardingAnno(fields[i])) {
				if (flag == null && !isHas) isHas = true;
				annoCounter++;
				ShardingSimpleStruct shardingSimpleStruct = null;
				if (condition != null) {
					shardingSimpleStruct = new ShardingSimpleStruct();
					shardingSimpleStruct.setTabName(tableName); // 要是注解没有设置就会用
				}

				dsTabStruct = ShardingAnnoHandlerController.process(fields[i], entity, suidType, shardingSimpleStruct);

//				hasShardingAnno = true;

				// 用了sharding注解,也是可以用Condtion的. 要添加逻辑.
				// 构造shardingBean
				if (condition != null) {
					shardingBean = new ShardingBean(shardingSimpleStruct);
					shardingBean.setTabField(fields[i].getName());
					shardingBean.setDsField(fields[i].getName());

				}
			} else if (AnnoUtil.isMultiTenancyAnno(fields[i])) {
				if (flag == null && !isHas) isHas = true;
				annoCounter++;
				dsTabStruct = MultiTenancyHandlerController.process(fields[i], entity, suidType);
			}
		} // end for 遍历完字段

		if (annoCounter > 1) {
			throw new BeeErrorGrammarException("One Class just allow one field has MultiTenancy or Sharding Annotaion!");
		}

//		有sharding注解
//		if (annoCounter == 1 && dsTabStruct != null) {
		if (annoCounter == 1) {
			if (condition == null) { // case 1
				if (dsTabStruct != null) {
					adjustValue(dsTabStruct, entity);
					setValeueForOneDsOneTab(dsTabStruct, suidType);
				} else {
					regFullorHintDsFull(suidType);
				}
				// 不用处理condition提前返回
				if (flag == null) // 原来为null,还没设置的,会进行初次设置
					HoneyContext.addCustomFlagMap(key, isHas);

				if (isByIdWithClass) return oldEntity;
				return entity;
			} else { // case 2 有sharding注解且condition不为null
				// 要将Sharding注解的转成shardingBean 用了注解,就不再用配置的sharding信息.所以Sharding注解,还有全表的属性
				boolean sharded = processCondition(entity, shardingBean, condition, dsTabStruct, suidType,
						moreTableSelectShardingFlag);
				if (!sharded && dsTabStruct == null) {
					regFullorHintDsFull(suidType);
				}
			}

		} else {// 检测是否使用全局配置 (没有用注解时才检测)
			if (ShardingUtil.isSharding()) {
				// 设置sharding Value
				shardingBean = ShardingRegistry.getShardingBean(entity.getClass());
				if (shardingBean == null) {
					shardingBean = ShardingRegistry.getShardingBean(_toTableName((entity)));
				}

				// 以下要检测,是会路由到一库一表,再处理; 否则,只解析出ds,tab记录到缓存,就返回.
				int type = 0;
				// case 3 shardingBean==null 没有设置,不使用分片规则.
				if (shardingBean != null) { // 说明要检测分片

					if (ObjectUtils.isNotEmpty(shardingBean.getTabField())) { // 表分片
						type += 1;
						shardingBean.setTabShardingValue(getFieldValue(entity, shardingBean.getTabField())); // 是拿实体的分片值
					}

					if (ObjectUtils.isNotEmpty(shardingBean.getDsField())) { // Ds分片
						type += 2;
						shardingBean.setDsShardingValue(getFieldValue(entity, shardingBean.getDsField()));// 是拿实体的分片值
					}

					// case 3
					if (type == 0) { // type=0,说明没有注册分片键,不需要分片. 提前返回
						if (flag == null) // 原来为null,还没设置的,会进行初次设置
							HoneyContext.addCustomFlagMap(key, isHas); // 不需要分片
//						regFull(suidType);  没有设置,不使用分片规则.
						if (isByIdWithClass) return oldEntity;
						return entity;
					}

					if (StringUtils.isBlank(shardingBean.getTabName())) {
						shardingBean.setTabName(tableName);
					}

					if (flag == null && !isHas) isHas = true;
//					以下肯定是注册有分片的   即以下都是type!=0;
					dsTabStruct = new ShardingDsTabHandler().process(shardingBean); // 分片键的值是null,也放到算法处理类处理,因在处理类,可能会给默认ds

//					if(type!=0 && condition==null) { 
//					if (condition == null) {//case 4 只有来自javabean的; 肯定是一库一表
					if (condition == null && !moreTableSelectShardingFlag) {
						if (dsTabStruct != null) {
							adjustValue(dsTabStruct, entity);
							setValeueForOneDsOneTab(dsTabStruct, suidType); // 一库一表时,设置到当前线程.
						} else {
							regFullorHintDsFull(suidType);
						}
					}

//					else if (condition != null) {//case 5
					if (condition != null || moreTableSelectShardingFlag) {// case 5 多表查询的一库一表 不能简化
						boolean sharded = processCondition(entity, shardingBean, condition, dsTabStruct, suidType,
								moreTableSelectShardingFlag);
						if (dsTabStruct == null && !sharded) {// 原来dsTabStruct是null,后来也没发现有
							regFullorHintDsFull(suidType);
						}
					} // end condition!=null
				} else {
					Logger.debug("Confirm whether had set sharding config for entity: " + entity.getClass().getName());
				}
			}
		}

		if (flag == null) // 原来为null,还没设置的,会进行初次设置
			HoneyContext.addCustomFlagMap(key, isHas);

		if (isByIdWithClass) return oldEntity;
		return entity;
	}

	private boolean isHintBoth() {
		return ShardingUtil.isTrue(StringConst.HintDs) && ShardingUtil.isTrue(StringConst.HintTab);
	}

	private boolean isHintDs() {
		return ShardingUtil.isTrue(StringConst.HintDs);
	}

	private boolean isHintTab() {
		return ShardingUtil.isTrue(StringConst.HintTab);
	}

	private void regFullorHintDsFull(SuidType suidType) {
		if (isHintDs()) {
			ShardingReg.regSomeDsFull(suidType);

			List<String> dsList = new ArrayList<String>();
			dsList.add(HoneyContext.getAppointDS());
			ShardingReg.regShardingJustDs(dsList);
		} else {
			ShardingReg.regFull(suidType);
		}
	}

	private void regFullInModifyAllNodes(SuidType suidType) { // Broadcast,DDL
		ShardingReg.regFullInModifyAllNodes(suidType);
	}

	private boolean processCondition(Object entity, ShardingBean shardingBean, Condition condition,
			DsTabStruct dsTabStruct0, SuidType suidType, boolean moreTableSelectShardingFlag) {

		DsTabStruct dsTabStruct = null;
		List<Expression> expList = null;

//		解析condition,看是否会导致多库多表.
		if (condition != null) { // moreTable select 的一库一表也要放到这运算. 2.4.0
			ConditionImpl conditionImpl = (ConditionImpl) condition;
			expList = conditionImpl.getExpList();
		}
		Expression expression = null;

//		String dsField = shardingBean.getDsField();
//		String tabField = shardingBean.getTabField();

		List<String> dsNameList = new SetList<>(); // dsNameList与tabNameList下标没有对应关系
		List<String> tabNameList = new SetList<>();// SetList无重复元素的List
		List<String> tabSuffixList = new SetList<>();

		Map<String, String> tab2DsMap = new HashMap<>();// 只在使用注解时, 分库与分表同属于一个分片键,才有用.
//		tab2DsMap: {0=null, orders1=null, 1=null, orders2=null, 2=null, orders0=null}

//		adjustTab(dsTabStruct, entity);
		// 处理condition前,设置通过javabean计算得来的
		setValeueForSharding(dsTabStruct0, dsNameList, tabNameList, tabSuffixList, tab2DsMap);

		// 将分片值置空,供下次计算conditon的
		shardingBean.setDsShardingValue(null);
		shardingBean.setTabShardingValue(null);
		boolean sharded = false;
		String tabName1 = shardingBean.getTabName();

		for (int j = 0; expList != null && j < expList.size(); j++) {
			expression = expList.get(j);
			String fieldName = expression.getFieldName();
//			if("myorders.userid".equals(fieldName)) fieldName="userid";
			if (fieldName == null) continue;
			if (fieldName.contains(".")) {
				if (fieldName.startsWith(tabName1 + ".")) fieldName = fieldName.substring(tabName1.length() + 1);
			}
			if (!isShardingField(shardingBean, fieldName)) continue;

			OpType opType = expression.getOpType();
			Op op=expression.getOp();
			boolean foundSharding = false;
			if (Op.eq == op) {
				// id>1 像这种没办法精确路由 会引起全路由
/*				if (dsField != null) {
					if (dsField.equals(expression.getFieldName())) {
						//  将找到的值放缓存 要计算过才能.
						// 设置ds分片值
						shardingBean.setDsShardingValue(expression.getValue());
						foundSharding = true;
					}
				}
				if (tabField != null) {
					if (tabField.equals(expression.getFieldName())) {
						// 设置表分片值
						shardingBean.setTabShardingValue(expression.getValue());
						foundSharding = true;
					}
				}*/

				foundSharding = checkAndProcessShardingField(shardingBean, fieldName, expression.getValue());
				// 找到一个,就计算一次
				if (foundSharding) {
					dsTabStruct = new ShardingDsTabHandler().process(shardingBean);
					if (dsTabStruct != null) sharded = true;
					setValeueForSharding(dsTabStruct, dsNameList, tabNameList, tabSuffixList, tab2DsMap);
					// 将分片值置空,供下次使用
					shardingBean.setDsShardingValue(null);
					shardingBean.setTabShardingValue(null);
				}

			} else if (OpType.IN == opType || OpType.BETWEEN == opType || OpType.NOT_BETWEEN == opType) {

				Object v = expression.getValue();

				List<?> inList;
				if (OpType.IN == opType) {
					inList = processIn(v);
				} else { // between v and v2 -> [v,v2]
					String tableName = _toTableName(entity);
					int tabSize = ShardingRegistry.getTabSize(tableName);
					Object v2 = expression.getValue2();
					inList = processBetween(v, v2, tabSize);
				}

				for (Object value : inList) {
					foundSharding = checkAndProcessShardingField(shardingBean, fieldName, value);
					// 找到一个,就计算一次
					if (foundSharding) {
						dsTabStruct = new ShardingDsTabHandler().process(shardingBean);
						if (dsTabStruct != null) sharded = true;
						setValeueForSharding(dsTabStruct, dsNameList, tabNameList, tabSuffixList, tab2DsMap);
						// 将分片值置空,供下次使用
						shardingBean.setDsShardingValue(null);
						shardingBean.setTabShardingValue(null);
					}
				}
			}
//			else if
			// or,and可能不需要另外处理 不拆分sql,则不需要

		} // end for

		// 要区分自定义的分片类, 还是系统默认的.
		// 自定义的,可以不写分片规则, 由给出的类提供逻辑,运算后,返回结果.
//		if(tabSuffixList.size()>1) Collections.sort(tabSuffixList);

		if (showShardingSQL) {
			Logger.debug("dsNameList: " + dsNameList.toString());
			Logger.debug("tabNameList: " + tabNameList.toString());
			Logger.debug("tabSuffixList: " + tabSuffixList.toString());
			Logger.debug("tab2DsMap: " + tab2DsMap.toString());
		}

//		[ds0]
//		[]
//		[]

		if (tabSuffixList.size() == 0 && dsNameList.size() > 0) { // 分片值,只计算到库,应查库下的所有表. 2022-09-23

			ShardingReg.regSomeDsFull(suidType);
			ShardingReg.regShardingJustDs(dsNameList);

			// 超过一个放缓存
		} else if ((dsNameList.size() > 1 || tabNameList.size() > 1 || tabSuffixList.size() > 1) // 多表时,不能用这种
				|| (moreTableSelectShardingFlag
						&& (dsNameList.size() == 1 || tabNameList.size() == 1 || tabSuffixList.size() == 1)) // 多表时,即使为1也不能转一库一表
		) {

			ShardingReg.regHadSharding();

			// 若是下标有值,都转成具体的表名. sharding只返回Ds和Tab 下标也要返回,更方便生成新sql
			// 不要这个是否可以?? 有时只转出了下标,就需要处理.
			if (tabSuffixList.size() >= 1 && tabNameList.size() < 1) {
				String tableName = _toTableName(entity);
				for (int i = 0; i < tabSuffixList.size(); i++) {
					String tab = tableName.replace(StringConst.ShardingTableIndexStr, tabSuffixList.get(i));
					tabNameList.add(tab);
					tab2DsMap.put(tab, tab2DsMap.get(tabSuffixList.get(i)));
				}
			}

			// 多个时,不能在拦截器设置.拦截器只能设置一个.
			this.tabName = null;
			this.tabName = null;
			this.tabSuffix = null;

			// 寻找tabName对应的dsName.即使dsNameList不为空,但它在分库键与分表键是不同的字段时,只计算了部分值,所以还是要计算一遍.
			// 当ds0:tab0,tab1; ds1:tab0,tab1; 像这样时, ShardingRegistry.getDsByTab 不能获取到值.
//			该如何处理????    不同库同表名,且分库键与分表键是不同的字段时,触发全域查询.
			String dsName = "";
			for (int i = 0; i < tabNameList.size(); i++) {
//				dsName=ShardingRegistry.getDsByTab(tabNameList.get(i));

				if (isHintDs()) { // 即使设置了,在重写sql时,能用反查查到,也不会用这个 ; 改成ShardingUtil.findDs()会好些
					dsName = HoneyContext.getAppointDS();
				} else {
					dsName = tab2DsMap.get(tabSuffixList.get(i)); // 只在使用注解 或 分库与分表同属于一个分片键,才有用.
					if (StringUtils.isBlank(dsName)) {
						dsName = ShardingRegistry.getDsByTab(tabNameList.get(i));
					}
				}
				if (StringUtils.isNotBlank(dsName))
					dsNameList.add(dsName);
				else
					Logger.warn("Table name :" + tabNameList.get(i) + " , can not find its dataSoure name!");
			}

//			HoneyContext.setListLocal(StringConst.TabNameListLocal, tabNameList);
//			HoneyContext.setListLocal(StringConst.TabSuffixListLocal, tabSuffixList);  
//			HoneyContext.setListLocal(StringConst.DsNameListLocal, dsNameList);
//			HoneyContext.setCustomMapLocal(StringConst.ShardingTab2DsMap, tab2DsMap);

			ShardingReg.regShardingManyTables(dsNameList, tabNameList, tabSuffixList, tab2DsMap);

//			处理后:
			if (showShardingSQL) {
				Logger.debug("after process: ");
				Logger.debug("dsNameList: " + dsNameList.toString());
				Logger.debug("tabNameList: " + tabNameList.toString());
				Logger.debug("tabSuffixList: " + tabSuffixList.toString());
				Logger.debug("tab2DsMap: " + tab2DsMap.toString());
			}

		} else {
//			只找到一个,也是按一库一表
			dsTabStruct = new DsTabStruct();
			if (dsNameList.size() == 1) dsTabStruct.setDsName(dsNameList.get(0));
			if (tabNameList.size() == 1) dsTabStruct.setTabName(tabNameList.get(0));
			if (tabSuffixList.size() == 1) dsTabStruct.setTabSuffix(tabSuffixList.get(0));
			// 要调整空的ds,tab
			adjustValue(dsTabStruct, entity);
			setValeueForOneDsOneTab(dsTabStruct, suidType); // 一库一表时,设置到当前线程.
		}

		return sharded;
	}

	private boolean isShardingField(ShardingBean shardingBean, String fieldName) {
		boolean foundSharding = false;
		String dsField = shardingBean.getDsField();
		String tabField = shardingBean.getTabField();

		if (dsField != null) {
			if (dsField.equals(fieldName)) {
				foundSharding = true;
			}
		}
		if (tabField != null) {
			if (tabField.equals(fieldName)) {
				foundSharding = true;
			}
		}
		return foundSharding;
	}

//	private boolean checkAndProcessShardingField(ShardingBean shardingBean,Expression expression) {
	private boolean checkAndProcessShardingField(ShardingBean shardingBean, String fieldName, Object value) {
		boolean foundSharding = false;
		String dsField = shardingBean.getDsField();
		String tabField = shardingBean.getTabField();

		if (dsField != null) {
			if (dsField.equals(fieldName)) {
				// 将找到的值放缓存? 要计算过才有用.
				// 设置ds分片值
				shardingBean.setDsShardingValue(value);
				foundSharding = true;
			}
		}
		if (tabField != null) {
			if (tabField.equals(fieldName)) {
				// 设置表分片值
				shardingBean.setTabShardingValue(value);
				foundSharding = true;
			}
		}

		return foundSharding;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List processIn(Object v) {
		List inList = new ArrayList();
		if (List.class.isAssignableFrom(v.getClass()) || Set.class.isAssignableFrom(v.getClass())) { // List,Set
			Collection<?> c = (Collection<?>) v;
//			len = c.size();
			for (Object e : c) {
//				setPreValue(inList, e);
				inList.add(e);
			}
		} else if (HoneyUtil.isNumberArray(v.getClass())) { // Number Array
			Number n[] = (Number[]) v;
//			len = n.length;
			for (Number number : n) {
//				setPreValue(inList, number);
				inList.add(number);
			}
//		} else if (String.class.equals(v.getClass())) { // String 逗号(,)为分隔符
		} else if (v instanceof String) { // String 逗号(,)为分隔符
			Object values[] = v.toString().trim().split(",");
//			len = values.length;
			for (Object e : values) {
//				setPreValue(inList, e);
				inList.add(e);
			}
		} else { // other one elements
//			setPreValue(inList, v);
			inList.add(v);
		}

		return inList;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List processBetween(Object v, Object v2, int tabSize) {
		List inList = new ArrayList();

		try {
			int r;
			int r2 = 0;
			String value = v.toString();
			if (value.contains(".")) {
				Double d = Double.parseDouble(value);
				r = d.intValue();

				Double d2 = Double.parseDouble(v2.toString());
				r2 = d2.intValue();
			} else {
				r = Integer.parseInt(value);
				r2 = Integer.parseInt(v2.toString());
			}
			for (int i = r; i <= r2 && r2 <= i + tabSize; i++) {
				inList.add(i);
			}

		} catch (Exception e) {
			// ignore
		}

		return inList;
	}

//	private static final String DoNotSetTabShadngValue="Do not set the sharding value for table!";
	private static final String DoNotSetTabShadngValue = "Do not set the sharding value when insert record to table!";

//	tabName,tabSuffix不会同时设置;当只设置tabSuffix下标时,表基本名称同实体名转化而来.
	private void setValeueForOneDsOneTab(DsTabStruct dsTabStruct, SuidType suidType) {
		if (dsTabStruct == null) return; // 不是一库一表

		if (!batchInsert) { // 不是批量才要设置

			// 分片值,只计算到库,应查库下的所有表. 2022-09-23
			if (StringUtils.isBlank(dsTabStruct.getTabSuffix()) // Blank
					&& StringUtils.isNotBlank(dsTabStruct.getDsName())) { // DsName NotBlank
				ShardingReg.regSomeDsFull(suidType);
				List<String> dsList = new ArrayList<String>();
				dsList.add(dsTabStruct.getDsName());
				ShardingReg.regShardingJustDs(dsList);

				return;
			}

			if (StringUtils.isNotBlank(dsTabStruct.getDsName())) this.ds = dsTabStruct.getDsName();

//		tabName,tabSuffix不会同时设置;当只设置tabSuffix下标时,表基本名称由实体名转化而来.
			if (StringUtils.isNotBlank(dsTabStruct.getTabName()))
				this.tabName = dsTabStruct.getTabName();
			else if (StringUtils.isNotBlank(dsTabStruct.getTabSuffix()))
				this.tabSuffix = dsTabStruct.getTabSuffix();
			else {
				if (SuidType.INSERT == suidType) {
//				clearContext();
//				//记录插入时,分表的分片值,要设置
//                throw new ShardingErrorConfigException(DoNotSetTabShadngValue);
					triggerDoNotSetTabShadngValueException();
				} else {
					Logger.warn(DoNotSetTabShadngValue);
				}
			}

//		不报异常,是批量插入的都设置
		} else { // batchInsert
			if (StringUtils.isBlank(dsTabStruct.getTabName()) && StringUtils.isBlank(dsTabStruct.getTabSuffix())) {
				triggerDoNotSetTabShadngValueException();
			}
			dsNameListForBatch.add(dsTabStruct.getDsName());
			tabNameListForBatch.add(dsTabStruct.getTabName());
			dsNameSetForBatch.add(dsTabStruct.getDsName());
			tabNameSetForBatch.add(dsTabStruct.getTabName());
		}
	}

	private void triggerDoNotSetTabShadngValueException() {
		clearContext();
		// 记录插入时,分表的分片值,要设置
		throw new ShardingErrorException(DoNotSetTabShadngValue);
	}

	private void adjustValue(DsTabStruct dsTabStruct, Object entity) {
		if (dsTabStruct == null) return;

		String tabName = dsTabStruct.getTabName();
		// 一库一表时,若用户没有设置分库键的值,可以通过表名反查ds;所以若只有表下标,则转化好表名后设置,用于通过表名,反查ds.
		// 只是临计算表名. 对多表查询更好. 2022-09-06 从表,可以通过下标拼出实际的表名.
		if (StringUtils.isNotBlank(dsTabStruct.getTabSuffix()) && StringUtils.isBlank(dsTabStruct.getTabName())) {
//			dsTabStruct.setTabName(_toTableName(entity) + dsTabStruct.getTabSuffix());
			tabName = _toTableName(entity) + dsTabStruct.getTabSuffix();
//			tabName=_toTableName(entity) +"_"+ dsTabStruct.getTabSuffix();  // 分隔符在DsTabHandler实现类加
			dsTabStruct.setTabName(tabName); // 2022-09-23
		}

		if (StringUtils.isBlank(dsTabStruct.getDsName())) { // 一库一表时, 若ds为空,通过表名查到ds后并设置. 用于在获取连接时使用.
			dsTabStruct.setDsName(ShardingRegistry.getDsByTab(tabName)); // Sharding注解,肯定是ds,tab都能确认的.
																			// 统一配置的,只有表分表,可以通过表名,反查.
																			// 要是ds0:tab0,tab1; ds1:tab0,tab1, 这种就没办法反查.
			// 一库一表的, 关了tab2ds就可以测.
//			String dsName = tab2DsMap.get(tabSuffixList.get(i)); // 只在使用注解  或  分库与分表同属于一个分片键,才有用. 
//			if (StringUtils.isBlank(dsName)) {
//				dsName = ShardingRegistry.getDsByTab(tabNameList.get(i));
//			}

		}
	}

	private void setValeueForSharding(DsTabStruct dsTabStruct, List<String> dsNameList, List<String> tabNameList,
			List<String> tabSuffixList, Map<String, String> tab2DsMap) {

		if (dsTabStruct == null) return; // 不是一库一表

		if (StringUtils.isNotBlank(dsTabStruct.getDsName())) dsNameList.add(dsTabStruct.getDsName());

		if (StringUtils.isNotBlank(dsTabStruct.getTabName())) {
			tabNameList.add(dsTabStruct.getTabName());
			tab2DsMap.put(dsTabStruct.getTabName(), dsTabStruct.getDsName());
		}

		if (StringUtils.isNotBlank(dsTabStruct.getTabSuffix())) {
			tabSuffixList.add(dsTabStruct.getTabSuffix());
			tab2DsMap.put(dsTabStruct.getTabSuffix(), dsTabStruct.getDsName());
		}

	}

	private Object getFieldValue(Object entity, String fieldName) {
		if (fieldName == null) return null; // 没设置有分片键时
		try {
			return getFieldValue0(entity, fieldName);
		} catch (Exception e) {
			try {
				// fieldName as column name check again for javabean field name
				return getFieldValue0(entity, NameTranslateHandle.toFieldName(fieldName, entity.getClass()));
			} catch (Exception e2) {
				Logger.debug(e2.getMessage(), e2);
				return null;
			}
		}
	}

	private Object getFieldValue0(Object entity, String fieldName) throws Exception { // 检测有多少个？ todo
		Field field = HoneyUtil.getField(entity.getClass(), fieldName);
		HoneyUtil.setAccessibleTrue(field);
		return field.get(entity);
	}

//	在类层面定义存ds,tab的List
	private List<String> dsNameListForBatch = null;
	private List<String> tabNameListForBatch = null;
	private Set<String> dsNameSetForBatch = null;
	private Set<String> tabNameSetForBatch = null;
	private boolean batchInsert = false;

	@Override
	public Object[] beforePasreEntity(Object[] entityArray, SuidType suidType) {
		boolean isSharding = ShardingUtil.isSharding();
		if (!isSharding) return entityArray;
		if (entityArray == null || entityArray.length < 1 || entityArray[0] == null) return entityArray;

		boolean isBroadcastTab = false;
//		是insert,循环前将ds,tab的List清空
		if (isSharding && SuidType.INSERT == suidType && entityArray.length > 1) {
			batchInsert = true;
			dsNameListForBatch = new ArrayList<>();
			tabNameListForBatch = new ArrayList<>();
			dsNameSetForBatch = new HashSet<>();
			tabNameSetForBatch = new HashSet<>();
		}

		String tableName = "";
		if (isSharding) {
			tableName = _toTableName(entityArray[0]);
			if (ShardingRegistry.isBroadcastTab(tableName)) {
				isBroadcastTab = true;
			}
		}

		for (int i = 0; i < entityArray.length; i++) {
			beforePasreEntity(entityArray[i], suidType);
			// 在每一次循环,记录ds,tab到List
			// 每一次循环,只能是一库一表,不会有多,因为插入语句不会有condition.

			if (isBroadcastTab && suidType != SuidType.SELECT) break;
		}

		// 循环后,再进行分类统计.
		if (batchInsert) {
			// 非空,涉及多个库或多个表,要分片拆分, 放缓存.
			if (tabNameListForBatch.size() > 1 && (tabNameSetForBatch.size() > 1 || dsNameSetForBatch.size() > 1)) {
//				HoneyContext.setListLocal(StringConst.TabNameListForBatchLocal, tabNameListForBatch);
//				HoneyContext.setListLocal(StringConst.DsNameListForBatchLocal, dsNameListForBatch);
				ShardingReg.regBatchInsert(tabNameListForBatch, dsNameListForBatch);
			}
		}

		// 广播表,要更新所有节点
		if (isBroadcastTab && suidType != SuidType.SELECT) {
			List<String> allDsList = ShardingRegistry.getAllDs(tableName);
			List<String> tabList = new ArrayList<>();
			for (int k = 0; k < allDsList.size(); k++) {
				tabList.add(tableName);
			}
			ShardingReg.regBatchInsert(tabList, allDsList);
		}

		return entityArray;
	}

	@Override
	public String afterCompleteSql(String sql) {
		return sql;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void beforeReturn(List list) {
		clearContext();
	}

	@Override
	public void beforeReturn() {
		clearContext();
	}

	// 2.0
	private void clearContext() {
		ShardingReg.clearContext();
	}

	private String _toTableName(Object entity) {
		return HoneyUtil.toTableName(entity);
	}

}

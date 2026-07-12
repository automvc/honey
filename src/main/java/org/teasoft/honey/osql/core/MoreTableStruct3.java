package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.List;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.FK;
import org.teasoft.bee.osql.annotation.JoinTable;
import org.teasoft.bee.osql.annotation.JoinType;
import org.teasoft.bee.osql.exception.ConfigWrongException;
import org.teasoft.bee.osql.exception.JoinTableParameterException;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.util.EntityUtil;
import org.teasoft.honey.util.StringUtils;

class MoreTableStruct3 {
	JoinType joinType; // 关联类型
	Class<?> subClass; // 从表实体的类型
	String[] mainFields; // 主表关联的属性
	String[] subFields; // 从表关联的属性
	String subAlias; // 从表别名
	String mainAlias; // 主表别名
	boolean currentIsList; // 当前MoreTableStruct所对应的从表属性类型是否是List

	String fieldName; // 用作关联的属性名
	Object subObject; // 子对象
	boolean hasNextLayer;

	List<?> subListObject; // List类型的子对象；不用于select

	int layer; // 关联树的层
	List<String> parentTree; // 父节点树(不包括根)
	List<Class<?>> typeTree; // 树对应的类型(包括根);主要用于判断循环依赖

	MoreTableStructOverall overall = null; // just the first element have it.

	MoreTableStruct3() {}

	MoreTableStruct3(Field joinField, Object entity, int layer, List<String> parentTree, SuidType suidType) {
		JoinTable joinTable = joinField.getAnnotation(JoinTable.class);

		if (List.class == joinField.getType()) {
			this.currentIsList = true;
		}
		
		if (joinTable != null) {
			this.fieldName = joinField.getName();
			
			this.mainFields = joinTable.mainField();
			this.subFields = joinTable.subField();
			this.subClass = joinTable.subClass();
			this.joinType = joinTable.joinType();
			
			
			this.subAlias = joinTable.subAlias();
			this.mainAlias = joinTable.mainAlias();
			checkField(subAlias);
			checkField(mainAlias);
			
		} else { //just have FK
			this.fieldName = joinField.getName();
			
			FK fk = joinField.getAnnotation(FK.class);
			this.mainFields = fk.mainField();
			this.subFields = fk.subField();
			
			this.subClass = fk.subClass();
			
//			this.joinType = fk.joinType();
//			this.fieldName = fk.getName();
		}

		if (suidType == SuidType.MODIFY) {
			FK fk = joinField.getAnnotation(FK.class);
			String[] fkMainField = fk.mainField();
			String[] fkSubField = fk.subField();

			if (fkMainField != null && fkMainField.length > 0) {
				this.mainFields = fkMainField;
			}
			if (fkSubField != null && fkSubField.length > 0) {
				this.subFields = fkSubField;
			}
		}

//		@JoinTable(mainField="userid, orderid", subField="userid, orderid", joinType=JoinType.JOIN)
//		support: mainField="userid, orderid"
		if (mainFields.length == 1 && mainFields[0] != null && mainFields[0].contains(",")) {
			mainFields = mainFields[0].split(",");
			StringUtils.trim(mainFields);
		}
		if (subFields.length == 1 && subFields[0] != null && subFields[0].contains(",")) {
			subFields = subFields[0].split(",");
			StringUtils.trim(subFields);
		}

		if (mainFields.length != subFields.length) {
			throw new ConfigWrongException("the length of mainField and subField are not equal.");
		}
		checkFields(mainFields);
		checkFields(subFields);

		try {
			if (entity != null) {
				HoneyUtil.setAccessibleTrue(joinField);
				this.subObject = HoneyUtil.getValue(joinField, entity);

				// subObject 是list时，只解析第一个元素 (select的场景这样用) //对于Insert时，不满足
				if (this.currentIsList && this.subObject != null) {
					List<?> list = (List<?>) this.subObject;
					if (list.isEmpty()) {
						this.subObject = null; // 子对象是空的List
						this.subListObject = null;
					} else {
						this.subObject = list.get(0);
						this.subListObject = list;
					}
				}
			}
		} catch (Exception e) {
//			e.printStackTrace(); // todo
			throw new BeeException(e);
		}

		if (this.subClass == Object.class) {
			if (this.currentIsList) { // list, 若不使用泛型，则一定要设置subClass
				Class<?> clazz = EntityUtil.getGenericType(joinField);
				if (clazz != null)
					this.subClass = clazz;
				else
					throw new JoinTableParameterException("The Join field type is List, must set subClass!");
			} else { // not list
				this.subClass = joinField.getType();
			}
		}

		this.layer = layer;
		if (this.subAlias == null || "".equals(this.subAlias.trim())) {
			if (!this.currentIsList)
				this.subAlias = NameTranslateHandle.toTableNameSimple(this.fieldName);
			else
				this.subAlias = HoneyUtil.toTableName(this.subClass);
		}
		this.parentTree = parentTree;
		parentTree.add(this.subAlias);
	}

	MoreTableStruct3(Field joinField, Object entity, int layer, List<String> parentTree, SuidType suidType,
			boolean hasNextLayer, String mainAlias) {
		this(joinField, entity, layer, parentTree, suidType);

		checkField(mainAlias);

		if (hasNextLayer) {
			this.hasNextLayer = hasNextLayer;
			if (this.mainAlias == null || "".equals(this.mainAlias.trim())) {
				this.mainAlias = mainAlias;
//			else 有在JoinTable声明的，就用
			}
		}
	}

	private void checkField(String fields) {
		NameCheckUtil.checkName(fields);
	}

	private void checkFields(String str[]) {
		for (String t : str) {
			NameCheckUtil.checkName(t);
		}
	}

}

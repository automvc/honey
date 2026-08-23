package org.teasoft.honey.osql.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.teasoft.honey.util.StringUtils;

/**
 * 通用线程级上下文容器。
 * <p>
 * 支持 ThreadLocal（不继承）和 InheritableThreadLocal（子线程继承）两种模式， 每种模式下可按任意字符串分类存储键值对。
 * </p>
 * 
 * <pre>
 *   
 * 使用示例：  
 *   // 存储  
 *   HoneyContext1.set(LocalType.REQUEST, "userId", "u123");  
 *   HoneyContext1.setInheritable(REQUEST.SHARDING, "key", "value-xxx");  
 *  
 *   // 读取  
 *   String uid = HoneyContext1.get(LocalType.REQUEST, "userId", String.class);  
 *  
 *   // 清理  
 *  // HoneyContext1.remove(..);
 *  // HoneyContext1.removexxx();
 * </pre>
 */
public class HoneyContext1 {

	private HoneyContext1() {}

	private static boolean isBlank(String str) {
		return StringUtils.isBlank(str);
	}

	private static boolean isBlank(LocalType str) {
		return str == null;
	}

	// ==================== ThreadLocal ====================START

	private static final ThreadLocal<Map<LocalType, Map<String, Object>>> LOCAL = new ThreadLocal<Map<LocalType, Map<String, Object>>>() {
		@Override
		protected Map<LocalType, Map<String, Object>> initialValue() {
			return new ConcurrentHashMap<LocalType, Map<String, Object>>();
		}
	};

	public static void set(String key, Object value) {
		set(LocalType.NORMAL, key, value);
	}

	public static Object get(String key) {
		return get(LocalType.NORMAL, key);
	}

	public static void setForSharding(String key, Object value) {
		set(LocalType.SHARDING, key, value);
	}

	/**
	 * 存储数据到 ThreadLocal（子线程不继承）
	 */
	public static void set(LocalType localType, String key, Object value) {
		if (isBlank(localType) || isBlank(key) || value == null) {
			return;
		}
		Map<LocalType, Map<String, Object>> store = LOCAL.get();
		Map<String, Object> catMap = store.get(localType);
		if (catMap == null) {
			catMap = new ConcurrentHashMap<String, Object>();
			store.put(localType, catMap);
		}
		catMap.put(key, value);
	}

	/**
	 * 从 ThreadLocal 读取数据
	 */
	public static Object get(LocalType localType, String key) {
		if (isBlank(localType) || isBlank(key)) {
			return null;
		}
		Map<String, Object> catMap = LOCAL.get().get(localType);
		return catMap != null ? catMap.get(key) : null;
	}

	/**
	 * 移除 ThreadLocal 中指定 key
	 */
	public static void remove(LocalType category, String key) {
		if (category == null || key == null) {
			return;
		}
		Map<String, Object> catMap = LOCAL.get().get(category);
		if (catMap != null) {
			catMap.remove(key);
		}
	}

	public static void remove(String key) {
		remove(LocalType.NORMAL, key);
	}

	/**
	 * 移除 ThreadLocal 中整个分类
	 */
	public static void removeCategory(LocalType category) {
		if (category == null) {
			return;
		}
		LOCAL.get().remove(category);
	}

	/**
	 * 获取 ThreadLocal 中某个分类的所有键
	 */
	public static Set<String> getKeys(LocalType category) {
		return _getKeys(category, false);
	}

	private static Set<String> _getKeys(LocalType category, boolean isInheritable) {
		if (category == null) {
			return new HashSet<String>();
		}
		Map<String, Object> catMap;
		if (isInheritable)
			catMap = INHERITABLE_LOCAL.get().get(category);
		else
			catMap = LOCAL.get().get(category);

		return catMap != null ? catMap.keySet() : new HashSet<String>();
	}

	// for Sharding just Local
	public static List<String> getListLocal(String key) {
		return (List<String>) get(LocalType.SHARDING, key);
	}

	// ==================== ThreadLocal ====================END

	// ========== InheritableThreadLocal — 子线程自动继承 ==========
	// extend == Inheritable

	private static final InheritableThreadLocal<Map<LocalType, Map<String, Object>>> INHERITABLE_LOCAL = new InheritableThreadLocal<Map<LocalType, Map<String, Object>>>() {
		@Override
		protected Map<LocalType, Map<String, Object>> initialValue() {
			return new ConcurrentHashMap<LocalType, Map<String, Object>>();
		}

		@Override
		protected Map<LocalType, Map<String, Object>> childValue(Map<LocalType, Map<String, Object>> parentValue) {
			// 深拷贝，避免父子线程互相影响
			Map<LocalType, Map<String, Object>> child = new ConcurrentHashMap<LocalType, Map<String, Object>>();
			for (Map.Entry<LocalType, Map<String, Object>> entry : parentValue.entrySet()) {
				child.put(entry.getKey(), new ConcurrentHashMap<String, Object>(entry.getValue()));
			}
			return child;
		}
	};

	public static void setInheritable(String key, Object value) {
		setInheritable(LocalType.NORMAL, key, value);
	}

	public static Object getInheritable(String key) {
		return getInheritable(LocalType.NORMAL, key);
	}

	public static void setExtForSharding(String key, Object value) {
		setInheritable(LocalType.SHARDING, key, value);
	}

	/**
	 * 存储数据到 InheritableThreadLocal（子线程不继承）
	 */
	public static void setInheritable(LocalType category, String key, Object value) {
		if (isBlank(category) || isBlank(key) || value == null) {
			return;
		}

		Map<LocalType, Map<String, Object>> store = INHERITABLE_LOCAL.get();
		Map<String, Object> catMap = store.get(category);
		if (catMap == null) {
			catMap = new ConcurrentHashMap<String, Object>();
			store.put(category, catMap);
		}
		catMap.put(key, value);
	}

	/**
	 * 从 InheritableThreadLocal 读取数据
	 */
	public static Object getInheritable(LocalType category, String key) {
		if (isBlank(category) || isBlank(key)) {
			return null;
		}
		Map<String, Object> catMap = INHERITABLE_LOCAL.get().get(category);
		return catMap != null ? catMap.get(key) : null;
	}

	public static Object getExtForSharding(String key) {
		return getInheritable(LocalType.SHARDING, key);
	}

	public static String getExtForShardingStr(String key) {
		return (String) getInheritable(LocalType.SHARDING, key);
	}

	/**
	 * 移除 InheritableThreadLocal 中指定 key
	 */
	public static void removeInheritable(LocalType category, String key) {
		if (category == null || key == null) {
			return;
		}
		Map<String, Object> catMap = INHERITABLE_LOCAL.get().get(category);
		if (catMap != null) {
			catMap.remove(key);
		}
	}

	/**
	 * 移除 InheritableThreadLocal 中整个分类
	 */
	public static void removeInheritableCategory(LocalType category) {
		if (category == null) {
			return;
		}
		INHERITABLE_LOCAL.get().remove(category);
	}

	/**
	 * 获取 InheritableThreadLocal 中某个分类的所有键
	 */
	public static Set<String> getInheritableKeys(LocalType category) {
		return _getKeys(category, true);
	}

	public static void setTrueExt(String key) {
		setInheritable(key, StringConst.tRue);
	}

	public static boolean isTrueExt(String key) {
		return StringConst.tRue.equals(getInheritable(key));
	}

	public static void setTrueExtForSharding(String key) {
		setInheritable(LocalType.SHARDING, key, StringConst.tRue);
	}

	public static boolean isTrueExtForSharding(String key) {
		return StringConst.tRue.equals(getInheritable(LocalType.SHARDING, key));
	}

}

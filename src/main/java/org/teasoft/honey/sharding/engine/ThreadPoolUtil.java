/*
 * Copyright 2016-2026 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * ThreadPool Util, support JDK 21 VirtualThread.
 * 分片查询线程池工具,支持JDK21虚拟线程;配置 sharding_useVirtualThread=true 开启;JDK<21自动降级平台线程池
 * 
 * @author Kingstar
 * @since  2.1.7
 * support JDK 21 VirtualThread.
 * @since  3.0.0
 */
public final class ThreadPoolUtil {

	private ThreadPoolUtil() {
	}

	/**
	 * @param hopeSize 期望线程数，一般传入分片数量 sqls.length
	 * @return ExecutorService,开启虚拟线程时内部自带限流(sharding_executorSize作为最大并发)
	 */
	public static ExecutorService getThreadPool(int hopeSize) {
		HoneyConfig config = HoneyConfig.getHoneyConfig();
		int executorSize = config.sharding_executorSize;
		boolean useVirtualThread = config.sharding_useVirtualThread;

		// 用户没有开启虚拟线程，直接走原有逻辑
	    if (!useVirtualThread) {
	        return getOriginThreadPool(hopeSize, executorSize);
	    }

	    ExecutorService vtExecutor = tryGetVirtualThreadPerTaskExecutor();
	    if (vtExecutor == null) {
	        // 运行环境不支持虚拟线程API，回退原有线程池
	        return getOriginThreadPool(hopeSize, executorSize);
	    }

	    // 使用sharding_executorSize作为最大并发保护;如果配置<=0,给一个默认保护值,避免无限制并发
	    int maxConcurrency = executorSize > 0 ? executorSize : 16;
	    return new SemaphoreWrappedExecutor(vtExecutor, maxConcurrency);
	}

	/**
	 * 原有平台线程池逻辑,保留不变
	 */
	private static ExecutorService getOriginThreadPool(int hopeSize, int executorSize) {
		if (hopeSize > 0 && executorSize > 0 && hopeSize < executorSize) {
			return Executors.newFixedThreadPool(hopeSize);
		} else if (executorSize <= 0) {
			return Executors.newCachedThreadPool();
		} else {
			return Executors.newFixedThreadPool(executorSize);
		}
	}

	private static ExecutorService tryGetVirtualThreadPerTaskExecutor() {
	    try {
	        Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
	        return (ExecutorService) method.invoke(null);
	    } catch (NoSuchMethodException e) {
	        //方法不存在，JDK<21
	        return null;
	    } catch (Exception e) {
	        //存在方法，但调用失败（安全管理器等）
	        return null;
	    }
	}

	/**
	 * 包装ExecutorService: 委托给底层executor执行,通过Semaphore限制最大并发任务数
	 * 对上层完全透明,适配ExecutorCompletionService JDK7版本：无lambda，全部匿名内部类
	 */
	private static class SemaphoreWrappedExecutor implements ExecutorService {
		private final ExecutorService delegate;
		private final Semaphore semaphore;
		private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);

		public SemaphoreWrappedExecutor(ExecutorService delegate, int maxConcurrency) {
			this.delegate = delegate;
			this.semaphore = new Semaphore(maxConcurrency);
		}

		@Override
		public void execute(Runnable command) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						semaphore.acquire();
						try {
							command.run();
						} finally {
							semaphore.release();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			};
			delegate.execute(runnable);
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			Callable<T> wrappedTask = wrapCallable(task);
			return delegate.submit(wrappedTask);
		}

		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			Callable<T> wrappedTask = wrapRunnable(task, result);
			return delegate.submit(wrappedTask);
		}

		@Override
		public Future<?> submit(Runnable task) {
			Callable<Object> wrappedTask = wrapRunnable(task, null);
			return delegate.submit(wrappedTask);
		}

		@Override
		public void shutdown() {
			if (shutdownFlag.compareAndSet(false, true)) {
				delegate.shutdown();
			}
		}

		@Override
		public List<Runnable> shutdownNow() {
			shutdownFlag.set(true);
			return delegate.shutdownNow();
		}

		@Override
		public boolean isShutdown() {
			return delegate.isShutdown();
		}

		@Override
		public boolean isTerminated() {
			return delegate.isTerminated();
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			return delegate.awaitTermination(timeout, unit);
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
			List<Callable<T>> wrapped = wrapCallableList(tasks);
			return delegate.invokeAll(wrapped);
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
				throws InterruptedException {
			List<Callable<T>> wrapped = wrapCallableList(tasks);
			return delegate.invokeAll(wrapped, timeout, unit);
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
				throws InterruptedException, ExecutionException {
			List<Callable<T>> wrapped = wrapCallableList(tasks);
			return delegate.invokeAny(wrapped);
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			List<Callable<T>> wrapped = wrapCallableList(tasks);
			return delegate.invokeAny(wrapped, timeout, unit);
		}

		/**
		 * 包装Callable，增加semaphore信号量，JDK7无lambda
		 */
		private <T> Callable<T> wrapCallable(final Callable<T> task) {
			return new Callable<T>() {
				@Override
				public T call() throws Exception {
					semaphore.acquire();
					try {
						return task.call();
					} finally {
						semaphore.release();
					}
				}
			};
		}

		/**
		 * 包装Runnable转Callable
		 */
		private <T> Callable<T> wrapRunnable(final Runnable runnable, final T resultVal) {
			return new Callable<T>() {
				@Override
				public T call() throws Exception {
					semaphore.acquire();
					try {
						runnable.run();
						return resultVal;
					} finally {
						semaphore.release();
					}
				}
			};
		}

		/**
		 * 批量包装任务集合
		 */
		private <T> List<Callable<T>> wrapCallableList(Collection<? extends Callable<T>> tasks) {
			List<Callable<T>> list = new ArrayList<Callable<T>>();
			for (Callable<T> t : tasks) {
				list.add(wrapCallable(t));
			}
			return list;
		}
	}
}

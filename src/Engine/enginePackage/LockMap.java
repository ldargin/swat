package Engine.enginePackage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** 
 * Synchronization primitive to emulate a set of locks (binary semaphores).
 * The generic type K is the type of the keys used to identify the
 * locks in the set. The advantage of using this primitive instead
 * of a map of locks is that it does not require adding or
 * deleting locks from the map.
 * <p>
 * It is used like this:
 * <pre>
 * LockMap<String> lm = new LockMap<String>();
 * lm.acquire("someKey");
 * try {
 *  ... critical section that must 
 *  be executed by only one thread 
 *  at a time ...
 * } finally {
 *   lm.release("someKey");
 * }
 * 
 * lm.acquire("someOtherKey");
 * try {
 *  ... other critical section that 
 *  must be executed by only one 
 *  thread at a time ...
 * } finally {
 *   lm.release("someOtherKey");
 * }
 * </pre>
 * */
public class LockMap<K> {

	/** 
	 * For each key in use, this map stores the lock used to block
	 * threads that want to acquire the semaphore. 
	 * */
	private Map<K,Condition> m = new HashMap<K,Condition>(); 
	/** 
	 * A lock to prevent execution of the body of
	 * {@link #acquire(Object)} and {@link #release(Object)}
	 * in multiple threads at the same time. 
	 * */
	private ReentrantLock lock = new ReentrantLock();

	/** 
	 * Turns off semaphore for key. Blocks the thread if the semaphore
	 * is already turned off.
	 * @throws InterruptedException if the thread is interrupted 
	 *                              while blocked. 
	 * */
	void acquire(K key) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			Condition c = m.get(key);
			if (c!=null) {
				do {
					c.await();
				} while (m.containsKey(key));
			} else
				c = lock.newCondition();

			m.put(key,c);
		} finally {
			lock.unlock();
		}
	}
	
	/** 
	 * Turns on semaphore for key. Wakes up one of the threads blocked
	 * by the semaphore if any.
	 * @throws InterruptedException if the thread is interrupted 
	 *                              while waiting to make the release. 
	 * */
	void release(K key) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			Condition c = m.remove(key);
			if (c!=null)
				c.signal();
		} finally {
			lock.unlock();
		}
	}
	
	/** 
	 * A test program that should print in the console:
	 * thread1 blocking
	 * thread1 unblocked
	 * thread3 not blocked
	 * */
	public static void main(String[] args) throws Exception {
		final LockMap<String> bsm = new LockMap<String>();

		bsm.acquire("main");
		bsm.acquire("a");
		bsm.acquire("b");

		new Thread(){
			@Override
			public void run() {
				try {
					System.out.println("thread1 blocking");
					bsm.acquire("a");
					bsm.release("main");
					System.out.println("thread1 unblocked");
					bsm.release("b");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();

		bsm.release("a");
		bsm.acquire("main");

		new Thread(){
			@Override
			public void run() {
				try {
					bsm.acquire("a");
					System.out.println("Error: thread2 not blocked");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();

		new Thread(){
			@Override
			public void run() {
				try {
					bsm.acquire("b");
					System.out.println("thread3 not blocked");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();

	}
}

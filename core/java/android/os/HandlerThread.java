/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

/**
 * Handy class for starting a new thread that has a looper. The looper can then be 
 * used to create handler classes. Note that start() must still be called.
 * //---
 * HandlerThread意即：处理线程，对所有处理任务结果做统一处理的线程。通常处理handler发送Message来的任务。
 * 已添加"变量副本"Looper的线程：HandlerThread是特殊的线程，此线程已经为线程添加"变量副本"-Looper。
 * HandlerThread
 *
 */
public class HandlerThread extends Thread {
    int mPriority;//优先级--这里使用默认级别
    int mTid = -1;//线程ID
    Looper mLooper;

	/**
	* HandlerThread构造函数。
	* @param name 线程名称
	*/
    public HandlerThread(String name) {
        super(name);
        mPriority = Process.THREAD_PRIORITY_DEFAULT;
    }
    
    /**
     * Constructs a HandlerThread.
     * @param name 
     * @param priority The priority to run the thread at. The value supplied must be from 
     * {@link android.os.Process} and not from java.lang.Thread. 运行线程的优先级
     */
    public HandlerThread(String name, int priority) {
        super(name);
        mPriority = priority;
    }
    
    /**
     * Call back method that can be explicitly overridden if needed to execute some
     * setup before Looper loops.
	 * //-------------------------------------------------------------------------------
	 * 如果在正式的轮训之前需要执行一些设置，那么请覆盖这个方法。
	 *
     */
    protected void onLooperPrepared() {
    }

    @Override
    public void run() {
        mTid = Process.myTid();//初始化线程标识符
        Looper.prepare(); //设置：本线程的“变量副本”为Looper
        synchronized (this) { //获取：本线程的“变量副本”Looper
            mLooper = Looper.myLooper();
            notifyAll();
        }
        Process.setThreadPriority(mPriority);
        onLooperPrepared(); //在正式轮训之前回调onLooperPrepared给用户一次设置机会
        Looper.loop();
        mTid = -1;
    }
    
    /**
     * This method returns the Looper associated with this thread. If this thread not been started
     * or for any reason is isAlive() returns false, this method will return null. If this thread 
     * has been started, this method will block until the looper has been initialized.  
     * @return The looper.
	 * //-------------------------------------------------------------------------------------------
	 * 返回关联到本线程的“变量副本”----Looper对象。
	 * 此方法有可能返回null，返回null情形如下：
	 * 1>此线程尚未start
	 * 2>此线程已经不存活，也就是 isAlive()返回false。
	 * 如果线程已经被start，这个方法会被阻塞知道初始化Looper对象完成。
     */
    public Looper getLooper() {
        if (!isAlive()) {//如果线程已经不存活，则返回null
            return null;
        }
        
        // If the thread has been started, wait until the looper has been created.如果线程已经启动，则等待直到Looper被创建。
        synchronized (this) {
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mLooper;
    }

    /**
     * Quits the handler thread's looper.
     * <p>
     * Causes the handler thread's looper to terminate without processing any
     * more messages in the message queue.
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * </p><p class="note">
     * Using this method may be unsafe because some messages may not be delivered
     * before the looper terminates.  Consider using {@link #quitSafely} instead to ensure
     * that all pending work is completed in an orderly manner.
     * </p>
     *
     * @return True if the looper looper has been asked to quit or false if the
     * thread had not yet started running.
     *
     * @see #quitSafely
	 * //---------------------------------------------------------------------------------------------
	 * 退出处理线程的轮训器，即：停止轮训。
	 * 在消息队列中没有更多消息消息处理时，使处理线程终止轮训。
	 * 执行退出操作之后，任何发送的消息都将不能收到。sendMessage会返回false。
	 * 这种方式的退出循环是不安全的。因为可能存在尚未送达的消息，Looper就已经被终止循环。
	 * 可以使用{@link #quitSafely}来保证所有待处理的工作都有序完成后，再终止轮训。
     */
    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }

    /**
     * Quits the handler thread's looper safely.
     * <p>
     * Causes the handler thread's looper to terminate as soon as all remaining messages
     * in the message queue that are already due to be delivered have been handled.
     * Pending delayed messages with due times in the future will not be delivered.
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * </p><p>
     * If the thread has not been started or has finished (that is if
     * {@link #getLooper} returns null), then false is returned.
     * Otherwise the looper is asked to quit and true is returned.
     * </p>
     *
     * @return True if the looper looper has been asked to quit or false if the
     * thread had not yet started running.
     */
    public boolean quitSafely() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
            return true;
        }
        return false;
    }

    /**
     * Returns the identifier of this thread. See Process.myTid().
	 * //--------------------------------------------------------
	 * 返回当前线程的ID
     */
    public int getThreadId() {
        return mTid;
    }
}

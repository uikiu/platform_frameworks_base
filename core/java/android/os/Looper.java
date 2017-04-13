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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.Log;
import android.util.Printer;

/**
  * Class used to run a message loop for a thread.  Threads by default do
  * not have a message loop associated with them; to create one, call
  * {@link #prepare} in the thread that is to run the loop, and then
  * {@link #loop} to have it process messages until the loop is stopped.
  *
  * <p>Most interaction with a message loop is through the
  * {@link Handler} class.
  *
  * <p>This is a typical example of the implementation of a Looper thread,
  * using the separation of {@link #prepare} and {@link #loop} to create an
  * initial Handler to communicate with the Looper.
  *
  * <pre>
  *  class LooperThread extends Thread {
  *      public Handler mHandler;
  *
  *      public void run() {
  *          Looper.prepare();
  *
  *          mHandler = new Handler() {
  *              public void handleMessage(Message msg) {
  *                  // process incoming messages here
  *              }
  *          };
  *
  *          Looper.loop();
  *      }
  *  }</pre>
  * //-----------------------------------------------------------------------------
  * Looper的本质就是局部线程的“变量副本”ThreadLocal的升级版：添加了轮训功能。
  * 每个线程只能由一个Looper类的实例对象。Looper类的实例对象必须通过prepare()创建。
  * prepare()方法会创建一个Looper对象，并把它保存在变量副本mThreadLocal中。一个线程中多次调用prepare()方法将会抛出异常：每个线程只能创建一个looper
  * 线程在默认情况下是没有消息轮训器关联到它们的。其实就是局部线程默认情况下是没有“变量副本”的。
  * 创建Looper{@link Looper.prepare()}其实就是：1、new 一个Looper对象;2、将这个Looper对象设置局部线程的“变量副本”。
  * 开启变量副本自身的特性--消息轮训：Looper.loop()
  * //-----------------------------------------------------------------------------
  * 模仿秀：
  * 我们可以模仿Looper这个变量为线程池中的工作线程添加一个“变量副本”以便监听线程的对任务执行状态的监听。
  */
public final class Looper {
    /*
     * API Implementation Note:
     *
     * This class contains the code required to set up and manage an event loop
     * based on MessageQueue.  APIs that affect the state of the queue should be
     * defined on MessageQueue or Handler rather than on Looper itself.  For example,
     * idle handlers and sync barriers are defined on the queue whereas preparing the
     * thread, looping, and quitting are defined on the looper.
	 * //------------------------------------------------------------------------------
	 * 该类包含设置和管理基于消息队列的事件轮训所需的代码。
	 * 这个类针对：事件轮训
	 * 对事件轮训的操作：1、设置；2、管理
	 * 影响消息队列状态的API应该定义在MessageQueue或者Handler，而不是looper自身。例如：
	 * 
	 * 
     */

    private static final String TAG = "Looper";

    // sThreadLocal.get() will return null unless you've called prepare().
	/**
	* sThreadLocal会返回null，除非你调用prepare()方法。
	* ThreadLocal这个类比较特殊，需要说明一下。
	* 通常某一对象，当一个线程修改其数据后，另一个线程访问时会获取到变更后的数据。
	* ThreadLocal可以在任意线程中声明，但是它会在单独的局部线程中保存一份对象的副本。所以线程之间对对象操作不会相互干扰。
	* 这个特性可以利用：1、某个对象数据只能由特性的线程读写（极少）；2、对线程状态监听（多）；3、对线程单一类型任务的处理（一般），比如Looper
	* ThreadLocal的作用域：thread的生命周期。
	* 本类属性sThreadLocal将自身Looper存进去。那么就可以通过threadLocal的set 和 get方法来分别设置和获取特定线程的Looper对象。
	*
	*/
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
    private static Looper sMainLooper;  // guarded by Looper.class

    final MessageQueue mQueue;
    final Thread mThread;

    private Printer mLogging;
    private long mTraceTag;

     /** Initialize the current thread as a looper.
      * This gives you a chance to create handlers that then reference
      * this looper, before actually starting the loop. Be sure to call
      * {@link #loop()} after calling this method, and end it by calling
      * {@link #quit()}.
	  * //----------------------------------------------------------------
	  * 将当前线程初始化为轮训器。
	  * 在真正的开启轮训之前：你有机会创建Handler随后引用此loopr。
	  * 在调用此方法之后，确保调用了{@link #loop()},最后调用{@link #quit()}
	  * 整个loop的调用顺序：创建{@link #prepare()}--->轮训{@link #loop()}---->结束{@link #quit()}
      */
    public static void prepare() {
        prepare(true);
    }

	/**
	* 创建Looper其实就是：1、new 一个Looper对象;2、将这个Looper对象设置局部线程的“变量副本”。
	*/
    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {//如果线程中已经存在Looper对象则不能再次设置：这个检测也就间接的证明了线程只能存在一个“变量副本”
            throw new RuntimeException("Only one Looper may be created per thread---每个线程只能创建一个looper");
        }
        sThreadLocal.set(new Looper(quitAllowed));//将本类Looper设置为局部线程的“变量副本”
    }

    /**
     * Initialize the current thread as a looper, marking it as an
     * application's main looper. The main looper for your application
     * is created by the Android environment, so you should never need
     * to call this function yourself.  See also: {@link #prepare()}
     */
    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }

    /**
     * Returns the application's main looper, which lives in the main thread of the application.
     */
    public static Looper getMainLooper() {
        synchronized (Looper.class) {
            return sMainLooper;
        }
    }

    /**
     * Run the message queue in this thread. Be sure to call
     * {@link #quit()} to end the loop.
     */
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;

        // Make sure the identity of this thread is that of the local process,
        // and keep track of what that identity token actually is.
        Binder.clearCallingIdentity();
        final long ident = Binder.clearCallingIdentity();

        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }

            // This must be in a local variable, in case a UI event sets the logger
            final Printer logging = me.mLogging;
            if (logging != null) {
                logging.println(">>>>> Dispatching to " + msg.target + " " +
                        msg.callback + ": " + msg.what);
            }

            final long traceTag = me.mTraceTag;
            if (traceTag != 0 && Trace.isTagEnabled(traceTag)) {
                Trace.traceBegin(traceTag, msg.target.getTraceName(msg));
            }
            try {
                msg.target.dispatchMessage(msg);
            } finally {
                if (traceTag != 0) {
                    Trace.traceEnd(traceTag);
                }
            }

            if (logging != null) {
                logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
            }

            // Make sure that during the course of dispatching the
            // identity of the thread wasn't corrupted.
            final long newIdent = Binder.clearCallingIdentity();
            if (ident != newIdent) {
                Log.wtf(TAG, "Thread identity changed from 0x"
                        + Long.toHexString(ident) + " to 0x"
                        + Long.toHexString(newIdent) + " while dispatching to "
                        + msg.target.getClass().getName() + " "
                        + msg.callback + " what=" + msg.what);
            }

            msg.recycleUnchecked();
        }
    }

    /**
     * Return the Looper object associated with the current thread.  Returns
     * null if the calling thread is not associated with a Looper.
	 * //-------------------------------------------------------------------
	 * 返回关联到当前线程的looper对象。如果调用的线程没有关联looper则返回null
	 * 
     */
    public static @Nullable Looper myLooper() {
        return sThreadLocal.get();
    }

    /**
     * Return the {@link MessageQueue} object associated with the current
     * thread.  This must be called from a thread running a Looper, or a
     * NullPointerException will be thrown.
	 * //---------------------------------------------------------------------
	 * 返回当前线程关联的消息队列。这个方法的调用，必须是在运行looper的线程中调用。
	 * 否则会抛出空指针异常。
     */
    public static @NonNull MessageQueue myQueue() {
        return myLooper().mQueue;
    }

	/**
	 * Looper的构造方法：私有的
	 * 通过构造方法的源码，我们可以知道：Looper.perpare()方法在newLooper()的时候其实已经创建了MessageQueue对象mQueue
	 *
	 */
    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }

    /**
     * Returns true if the current thread is this looper's thread.
     */
    public boolean isCurrentThread() {
        return Thread.currentThread() == mThread;
    }

    /**
     * Control logging of messages as they are processed by this Looper.  If
     * enabled, a log message will be written to <var>printer</var>
     * at the beginning and ending of each message dispatch, identifying the
     * target Handler and message contents.
     *
     * @param printer A Printer object that will receive log messages, or
     * null to disable message logging.
     */
    public void setMessageLogging(@Nullable Printer printer) {
        mLogging = printer;
    }

    /** {@hide} */
    public void setTraceTag(long traceTag) {
        mTraceTag = traceTag;
    }

    /**
     * Quits the looper.
     * <p>
     * Causes the {@link #loop} method to terminate without processing any
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
     * @see #quitSafely
     */
    public void quit() {
        mQueue.quit(false);
    }

    /**
     * Quits the looper safely.
     * <p>
     * Causes the {@link #loop} method to terminate as soon as all remaining messages
     * in the message queue that are already due to be delivered have been handled.
     * However pending delayed messages with due times in the future will not be
     * delivered before the loop terminates.
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * </p>
     */
    public void quitSafely() {
        mQueue.quit(true);
    }

    /**
     * Gets the Thread associated with this Looper.
     *
     * @return The looper's thread.
     */
    public @NonNull Thread getThread() {
        return mThread;
    }

    /**
     * Gets this looper's message queue.
     *
     * @return The looper's message queue.
     */
    public @NonNull MessageQueue getQueue() {
        return mQueue;
    }

    /**
     * Dumps the state of the looper for debugging purposes.
     *
     * @param pw A printer to receive the contents of the dump.
     * @param prefix A prefix to prepend to each line which is printed.
     */
    public void dump(@NonNull Printer pw, @NonNull String prefix) {
        pw.println(prefix + toString());
        mQueue.dump(pw, prefix + "  ");
    }

    @Override
    public String toString() {
        return "Looper (" + mThread.getName() + ", tid " + mThread.getId()
                + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }
}

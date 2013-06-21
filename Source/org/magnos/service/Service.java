/* 
 * NOTICE OF LICENSE
 * 
 * This source file is subject to the Open Software License (OSL 3.0) that is 
 * bundled with this package in the file LICENSE.txt. It is also available 
 * through the world-wide-web at http://opensource.org/licenses/osl-3.0.php
 * If you did not receive a copy of the license and are unable to obtain it 
 * through the world-wide-web, please send an email to pdiffenderfer@gmail.com 
 * so we can send you a copy immediately. If you use any of this software please
 * notify me via my website or email, your feedback is much appreciated. 
 * 
 * @copyright   Copyright (c) 2011 Magnos Software (http://www.magnos.org)
 * @license     http://opensource.org/licenses/osl-3.0.php
 * 				Open Software License (OSL 3.0)
 */

package org.magnos.service;

import java.util.Queue;

import org.magnos.util.BlockableQueue;
import org.magnos.util.Notifier;
import org.magnos.util.Release;
import org.magnos.util.Sleepable;
import org.magnos.util.State;



/**
 * A service is a more friendly version of java.lang.Thread. A service can
 * have several states and can be restarted (opposed to a once and done Thread).
 * A service can automatically handle events passed to the service. A service
 * also has a set of listeners which will be notified when a change in state
 * occurs (started, stopped, paused, resumed, etc). All listeners will also
 * be notified of the events received in the context of the service's thread.
 * A service is built for optimum performance when state changes are infrequent.
 * Frequent state changes will cause a lot of contention among all threads
 * modifying or viewing the state of the service.
 * 
 * @author Philip Diffenderfer
 *
 * @param <E>
 * 		The event added to the service. Events added to the service
 * 		are handled in the service's thread by invoking the onServiceEvent
 * 		method in all attached listeners.
 */
public abstract class Service<E> implements EventHandler<E>, Runnable, Sleepable
{

	/**
	 * The state of the service when its completely paused.
	 */
	public static final int Paused = State.create(0);

	/**
	 * The state of the service when its in the process of pausing.
	 */
	public static final int Pausing = State.create(1);

	/**
	 * The state of the service when its operating normally.
	 */
	public static final int Running = State.create(2);

	/**
	 * The state of the service when its completely stopped.
	 */
	public static final int Stopped = State.create(3);

	/**
	 * The state of the service when its in the process of stopping.
	 */
	public static final int Stopping = State.create(4);


	// The state of this Service.
	private final State state = new State();

	
	// Whether this service is paused or trying to be paused.
	private volatile boolean paused = false;
	
	// Whether this service is stopped or trying to be stopped.
	private volatile boolean stopped = false;
	

	// An interrupt object which dictates what things (events, executes) can be 
	// done during an interrupting state (pausing or stopping).
	private volatile ServiceInterrupt interrupt = ServiceInterrupt.None;

	// The set of listeners to this services events.
	protected final Notifier<ServiceListener<E>> notifier;
	
	// The set of blockers that need to be awakened when this service is in the
	// process of being paused or stopped.
	protected final Release release;
	
	// The thread executing this service.  
	private Thread thread;

	// The queue events are added to. If this queue is being shared then
	// events can be added externally. If this queue is shared and events are
	// added to this service, this service won't be guarunteed to get the event,
	// another service may take it if this Service is busy.
	private final BlockableQueue<E> eventQueue;

	
	
	// The remaining number of events this service can handle before it
	// automatically stops.
	private volatile int remainingEvents = -1;

	// The remaining number of iterations this service can process before it
	// automatically stops.
	private volatile int remainingIterations = -1;

	// The remaining number of executes invokable before this service 
	// automatically stops.
	private volatile int remainingExecutes = -1;

	// Whether this service will accept adding events through the addEvent
	// method. Events can still be given to the event queue if one was passed
	// to this service in the constructor.
	private volatile boolean acceptsEvents = true;

	// Whether processing events will occur in this service. If true, added 
	// events will be processed as soon as the last execution finishes. If false
	// events will continue to be queued and not processed until this is set to
	// true.
	private volatile boolean activeEvents = true;

	// Whether invoking execute will occur in this service. If true, every 
	// iteration all listeners will be notified. If false, no listeners during
	// any iterations will be notified.
	private volatile boolean activeExecute = true;


	/**
	 * Instantiates a new Service.
	 */
	public Service() 
	{
		this(new BlockableQueue<E>());
	}
	
	/**
	 * Instantiates a new Service.
	 * 
	 * @param sourceQueue
	 * 		The queue implementation to use internally as an event queue.
	 */
	public Service(Queue<E> sourceQueue) 
	{
		this(new BlockableQueue<E>(sourceQueue));
	}
	
	/**
	 * Instantiates a new Service given a queue to poll and offer events to.
	 * 
	 * @param eventQueue
	 * 		The queue of events.
	 */
	public Service(BlockableQueue<E> eventQueue) 
	{
		this.eventQueue = eventQueue;
		this.notifier = Notifier.create(ServiceListener.class);
		this.release = new Release();
		this.release.getBlockers().add(this);
		this.state.set(Stopped);
	}
	
	
	/**
	 * The queue of events which have been added to the service. Events should
	 * not be directly added to this queue unless you mean to purposely bypass
	 * all logic imposed by several properties of this Service.
	 * 
	 * @return
	 * 		The reference to the event queue.
	 */
	public BlockableQueue<E> getEventQueue() 
	{
		return eventQueue;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void awake() 
	{
		// Unblock the event queue.
		eventQueue.wakeup();
		
		// Unblock all threads waiting for thread states.
		state.wakeup();
	}
	
	
	/**
	 * Starts this service and waits for it to finish starting. If this service
	 * is in the Stopping state, this will wait indefinitely until the service
	 * has stopped. If the service is in the Stopped state then this service
	 * will be started, else this will wait indefinitely until the service is
	 * in a started state (Paused, Pausing, Running).
	 *  
	 * @return
	 * 		True if the service was started, otherwise false.
	 */
	public boolean start() 
	{
		return start(true, Long.MAX_VALUE);
	}
	
	/**
	 * Starts this service and optionally waits for it to finish starting. If 
	 * this service is in the Stopping state, this will wait indefinitely until 
	 * the service has stopped. If the service is in the Stopped state then this
	 * service will be started, else this will wait indefinitely until the 
	 * service is in a started state (Paused, Pausing, Running).
	 * 
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		started state, false if this method should return immediately.
	 * @return
	 * 		True if the service was started, otherwise false.
	 */
	public boolean start(boolean wait) 
	{
		return start(wait, Long.MAX_VALUE);
	}
	
	/**
	 * Starts this service and optionally waits for a maximum amount of time for
	 * it to finish starting. If this service is in the Stopping state, this 
	 * will wait the maximum time until the service has stopped. If the service
	 * is in the Stopped state then this service will be started, else this will
	 * wait the maximum time until the service is in a started state (Paused, 
	 * Pausing, Running). 
	 * 
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		started state, false if this method should return immediately.
	 * @param timeout
	 * 		The maximum amount of time in milliseconds to wait for the service 
	 * 		to start or wait for the service to stop if it is Stopping. 
	 * @return
	 * 		True if the service was started, otherwise false.
	 */
	public boolean start(boolean wait, long timeout) 
	{
		synchronized (state) 
		{
			// Acceptable states to be considered "started"
			final int STARTED_STATE = Running | Paused | Pausing;
			
			// If its stopping, wait for it to stop.
			if (state.has(Stopping)) {
				state.waitFor(Stopped, timeout);
			}
			// Only start if it's in the stopped state.
			if (state.has(Stopped)) {
				thread = new Thread(this);
				
				try {
					thread.start();	
				}
				// Don't have enough memory to allocate another thread.
				catch (java.lang.OutOfMemoryError e) {
					// TODO
					System.err.println("Cannot allocate a Thread; out of memory.");
					return false;
				}
				
				// Mark as running, even though the thread may have not started.
				state.set(Running);		
			}
			// Wait for started state if specified.
			if (wait) {
				state.waitFor(STARTED_STATE, timeout);
			}
			
			// Are we in a started state?
			return state.has(STARTED_STATE);
		}
	}

	/**
	 * Pauses this service and waits indefinitely for it to be in a resting 
	 * state (Paused, Stopped). If the service is not started, already paused, 
	 * or is stopped this will return immediately. If this service is in the 
	 * Pausing or Stopping state this method will simply wait indefinitely for 
	 * it to reach its resting state. This pause will not wait for all events
	 * to be processed, it will interrupt all processing immediately. 
	 *
	 * @return
	 * 		True if this service is now in a resting state (Paused, Stopped).
	 */
	public boolean pause() 
	{
		return pause(ServiceInterrupt.Immediate, true, Long.MAX_VALUE);
	}
	
	/**
	 * Pauses this service and optionally waits indefinitely for it to be in a 
	 * resting state (Paused, Stopped). If the service is not started, already 
	 * paused, or is stopped this will return immediately. If this service is in
	 * the Pausing or Stopping state this method will simply wait indefinitely 
	 * for it to reach its resting state. This pause will not wait for all 
	 * events to be processed, it will interrupt all processing immediately.
	 * 
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		resting state, false if this method should return immediately.
	 * @return
	 * 		True if this service is now in a resting state (Paused, Stopped).
	 */
	public boolean pause(boolean wait) 
	{
		return pause(ServiceInterrupt.Immediate, wait, Long.MAX_VALUE);
	}
	
	/**
	 * Pauses this service and optionally waits a maximum time for it to be in a 
	 * resting state (Paused, Stopped). If the service is not started, already 
	 * paused, or is stopped this will return immediately. If this service is in
	 * the Pausing or Stopping state this method will simply wait a maximum time 
	 * for it to reach its resting state. This pause will not wait for all 
	 * events to be processed, it will interrupt all processing immediately.
	 * 
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		resting state, false if this method should return immediately.
	 * @param timeout
	 * 		The maximum amount of time in milliseconds to wait for the service 
	 * 		to be in a resting state. 
	 * @return
	 * 		True if this service is now in a resting state (Paused, Stopped).
	 */
	public boolean pause(boolean wait, long timeout) 
	{
		return pause(ServiceInterrupt.Immediate, wait, timeout);
	}
	
	/**
	 * Pauses this service and optionally waits a maximum time for it to be in a 
	 * resting state (Paused, Stopped). If the service is not started, already 
	 * paused, or is stopped this will return immediately. If this service is in
	 * the Pausing or Stopping state this method will simply wait a maximum time 
	 * for it to reach its resting state. This pause can choose to wait for all 
	 * events to finish processing, and choose to invoke execute before the
	 * service comes to a resting state.
	 * 
	 * @param serviceInterrupt
	 * 		The type of interruption. This can specify whether to finish 
	 * 		processing events, or whether to skip execution. This is typically
	 *		used to ensure that all events that have been added to the service
	 *		are processed before resting the service.
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		resting state, false if this method should return immediately.
	 * @param timeout
	 * 		The maximum amount of time in milliseconds to wait for the service 
	 * 		to be in a resting state. 
	 * @return
	 * 		True if this service is now in a resting state (Paused, Stopped).
	 */
	public boolean pause(ServiceInterrupt serviceInterrupt, boolean wait, long timeout) 
	{
		synchronized (state) 
		{
			// Acceptable states to be considered "resting"
			final int RESTING_STATE = Paused | Stopped;
			
			// If the service is currently running...
			if (state.equals(Running)) 
			{
				// Acquire the lock on the releaser
				release.lock(); 
				try {
					// Mark this as true, this will set off the double checked
					// locking of the service, which is more efficient than
					// having the service constantly checking its state for each
					// event end execution.
					paused = true;
					
					// Set the requested interruption type.
					interrupt = serviceInterrupt;
					
					// Now in the pausing state.
					state.set(Pausing);
					
					// Awake any blockers.
					release.awake();
					
					// Wait for the resting state?
					if (wait) {
						state.waitFor(RESTING_STATE, timeout);
					}
				}
				finally {
					// Always unlock, even if an error occurs above.
					release.unlock();
				}
			}
			// Not running, wait for the resting state?
			else if (wait) {
				state.waitFor(RESTING_STATE, timeout);
			}
			
			// Are we in a resting state?
			return state.has(RESTING_STATE);
		}
	}
	

	/**
	 * Resumes this service. If the service is in the process of pausing this
	 * will block until the service reaches the paused state. The service
	 * will only resume if it's in the Paused state.
	 * 
	 * @return
	 * 		True if this service is now in a resumed state (Running, Stopping, 
	 * 		Stopped).
	 */
	public boolean resume() 
	{
		return resume(true, Long.MAX_VALUE);
	}

	/**
	 * Resumes this service. If the service is in the process of pausing this
	 * will optionally block until the service reaches the paused state. The 
	 * service will only resume if it's in the Paused state.
	 * 
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		Paused state if its currently in the Pausing state.
	 * @return
	 * 		True if this service is now in a resumed state (Running, Stopping, 
	 * 		Stopped).
	 */
	public boolean resume(boolean wait) 
	{
		return resume(wait, Long.MAX_VALUE);
	}

	/**
	 * Resumes this service. If the service is in the process of pausing this
	 * will optionally block until the service reaches the paused state. The 
	 * service will only resume if it's in the Paused state.
	 * 
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		Paused state if its currently in the Pausing state.
	 * @param timeout
	 * 		The maximum amount of time in milliseconds to wait for the service 
	 * 		to be in a Paused state. 
	 * @return
	 * 		True if this service is now in a resumed state (Running, Stopping, 
	 * 		Stopped).
	 */
	public boolean resume(boolean wait, long timeout) 
	{
		synchronized (state) 
		{
			// Wait for complete pause?
			if (state.has(Pausing)) 
			{
				state.waitFor(Paused, timeout);
			}
			
			// Paused, resume service execution.
			if (state.has(Paused)) 
			{
				// Mark this as false, this will avoid any synchronizing calls
				// by utilizing the double check algorithm.
				paused = false;
				
				// Reset the interrupte to resume everything.
				interrupt = ServiceInterrupt.None;
				
				// Mark as running
				state.set(Running);
			}
			
			// Has a resumed state?
			return state.has(Running | Stopped | Stopping);
		}
	}


	/**
	 * Stops this service and waits indefinitely for it to be in the Stopped
	 * state. If the service is not started, already paused, or is stopped this 
	 * will return immediately. If this service is in the Pausing or Stopping 
	 * state this method will simply wait indefinitely for it to reach its 
	 * Stopped state. This stop will not wait for all events to be processed, 
	 * it will interrupt all processing immediately. 
	 * 
	 * @return
	 * 		True if this service is now in a Stopped state.
	 */
	public boolean stop() 
	{
		return stop(ServiceInterrupt.Immediate, true, Long.MAX_VALUE);
	}
	

	/**
	 * Stops this service and optionally waits indefinitely for it to be in a 
	 * Stopped state. If the service is not started, already paused, or is 
	 * stopped this will return immediately. If this service is in the Pausing 
	 * or Stopping state this method will simply wait indefinitely for it to 
	 * reach its Stopped state. This stop will not wait for all events to be 
	 * processed, it will interrupt all processing immediately. 
	 * 
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		Stopped state, false if this method should return immediately.
	 * @return
	 * 		True if this service is now in a Stopped state.
	 */
	public boolean stop(boolean wait) 
	{
		return stop(ServiceInterrupt.Immediate, wait, Long.MAX_VALUE);
	}

	/**
	 * Stops this service and optionally waits a maximum time for it to be in a 
	 * Stopped state. If the service is not started, already paused, or is 
	 * stopped this will return immediately. If this service is in the Pausing 
	 * or Stopping state this method will simply wait indefinitely for it to 
	 * reach its Stopped state. This stop will not wait for all events to be 
	 * processed, it will interrupt all processing immediately.
	 * 
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		Stopped state, false if this method should return immediately.
	 * @param timeout
	 * 		The maximum amount of time in milliseconds to wait for the service 
	 * 		to be in a Stopped state. 
	 * @return
	 * 		True if this service is now in a Stopped state.
	 */
	public boolean stop(boolean wait, long timeout) 
	{
		return stop(ServiceInterrupt.Immediate, wait, timeout);
	}

	/**
	 * Stops this service and optionally waits a maximum time for it to be in a 
	 * Stopped state.If the service is not started, already paused, or is 
	 * stopped this will return immediately. If this service is in the Pausing 
	 * or Stopping state this method will simply wait indefinitely for it to 
	 * reach its Stopped state. This pause can choose to wait for all events to 
	 * finish processing, and choose to invoke execute before the service comes 
	 * to a resting state.
	 * 
	 * @param serviceInterrupt
	 * 		The type of interruption. This can specify whether to finish 
	 * 		processing events, or whether to skip execution. This is typically
	 *		used to ensure that all events that have been added to the service
	 *		are processed before Stopping the service.
	 * @param wait
	 * 		True if the invoking thread should wait until this service is in a
	 * 		Stopped state, false if this method should return immediately.
	 * @param timeout
	 * 		The maximum amount of time in milliseconds to wait for the service 
	 * 		to be in a Stopped state. 
	 * @return
	 * 		True if this service is now in a Stopped state.
	 */
	public boolean stop(ServiceInterrupt serviceInterrupt, boolean wait, long timeout) 
	{
		synchronized (state) 
		{
			// Wait for a complete Pause if it's in the Pausing state.
			if (state.has(Pausing)) 
			{
				state.waitFor(Paused, timeout);
			}
			// Requires stopping?
			if (state.has(Running | Paused)) 
			{
				release.lock();
				try {
					// Mark this as true, this will set off the double checked
					// locking of the service, which is more efficient than
					// having the service constantly checking its state for each
					// event end execution.
					stopped = true;
					
					// Set the requested interruption type.
					interrupt = serviceInterrupt;
					
					// Now in the stopping state.
					state.set(Stopping);

					// Awake any blockers.
					release.awake();

					// Wait for the resting state?
					if (wait) {
						state.waitFor(Stopped, timeout);
					}
				}
				finally {
					// Always unlock, even if an error occurs above.
					release.unlock();
				}
			}
			// Must be Stopping or Stopped
			else if (wait) 
			{
				state.waitFor(Stopped, timeout);
			}
			
			// Are we in the Stopped state?
			return state.equals(Stopped);
		}
	}

	/**
	 * Removes all events from the service's queue.
	 */
	public void clear() 
	{
		eventQueue.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean addEvent(E event) 
	{
		if (!acceptsEvents/* || !state.has(Running | Paused | Pausing)*/) {
			return false;
		}
		return eventQueue.offer(event);
	}

	/**
	 * Sets whether this service is accepting events.
	 * 
	 * @param accept
	 * 		True if this service should accept events, otherwise false.
	 */
	public void setEventAccept(boolean accept) 
	{
		this.acceptsEvents = accept;
	}
	
	/**
	 * Sets whether the events should be invoked each iteration.
	 * 
	 * @param active
	 * 		True if the events chould be invoked, otherwise false.
	 */
	public void setEventActive(boolean active) 
	{
		this.activeEvents = active;
	}

	/**
	 * Sets whether the execute event is invoked each iteration.
	 * 
	 * @param active
	 * 		True if the execute event should be invoked, otherwise false.
	 */
	public void setExecuteActive(boolean active) 
	{
		this.activeExecute = active;
	}

	/**
	 * Returns the thread of the service. If the service is currently not 
	 * running then null is returned.
	 *  	
	 * @return
	 * 		The reference to the thread of the service.
	 */
	public Thread getThread() 
	{
		synchronized (state) 
		{
			return thread;
		}
	}

	/**
	 * The event processing and execution loop.
	 */
	public void run() 
	{
		// Applies the running state, and notifies all listeners that the
		// service has started.
		notifier.proxy().onServiceStart(this);

		// While the service is in a runnable state...
		while (!stopped) 
		{
			boolean valid = true;
			
			// If event processing is active...
			if (activeEvents) 
			{
				valid &= invokeEvents(valid);
			}
			
			// If execution is active...
			if (activeExecute) 
			{
				valid &= invokeExecute(valid);
			}

			// Adjust validation based on remaining iterations
			valid &= (remainingIterations == -1 || --remainingIterations > 0);

			// If the service state is not valid, stop service.
			if (!valid) {
				stopped = true;
			}
		}

		// Applies the stopped state, and notifies all listeners that the
		// service has stopped.
		state.set(Stopped);
		
		notifier.proxy().onServiceStop(this, interrupt);
	}
	
	/**
	 * Invokes all events in the queue.
	 * 
	 * @param valid
	 * 		The current validity of the service.
	 * @return
	 * 		The new validity of the service.
	 */
	private boolean invokeEvents(boolean valid)
	{
		E event = null;
		// Take all events from the event queue and process them.
		for (;;) 
		{
			// Clear event, very important!
			event = null;
			
			// Enter blocking section with caution!
			if (release.enter()) {
				try {
					event = eventQueue.poll();	
				}
				finally {
					release.exit();	
				}
			}
			
			// If event was null (queue is empty and it returned
			// immediately or it was awoken by an attempt to pause or
			// stop the server) then stop processing events.
			if (event == null) {
				canEvent();
				break;	
			}
			
			// If events can be processed right now...
			if (canEvent()) 
			{
				// Adjust validation based on remaining events.
				valid &= (remainingEvents == -1 || --remainingEvents >= 0);

				// If the service state is valid, notify listeners.
				if (valid) {
					// Quicker then calling the proxy, and this could
					// be called VERY frequently.
					// notifier.proxy().onServiceEvent(this, event);
					for (ServiceListener<E> listener : notifier) {
						listener.onServiceEvent(this, event);
					}
				}
			}
		}	
		return valid;
	}

	/**
	 * Invokes the execution method.
	 * 
	 * @param valid
	 * 		The current validity of the service.
	 * @return
	 * 		The new validity of the service.
	 */
	public boolean invokeExecute(boolean valid)
	{
		// If execution can occur right now...
		if (canExecute()) 
		{
			// Adjust validation based on remaining executions
			valid &= (remainingExecutes == -1 || --remainingExecutes >= 0);

			// If the service state is valid, notify listeners.
			if (valid) {
				// Quicker then calling the proxy, and this could
				// be called VERY frequently.
				// notifier.proxy().onServiceExecute(this);
				for (ServiceListener<E> listener : notifier) {
					listener.onServiceExecute(this);
				}
			}
		}
		return valid;
	}
	
	/**
	 * Blocks the invoking thread indefinitely until this Service reaches any
	 * of the given states. 
	 * 
	 * @param desiredState
	 * 		A set of states to wait for.
	 * @return
	 * 		True if any of the states was reached.
	 */
	public boolean waitFor(int desiredState) 
	{
		return waitFor(desiredState, Long.MAX_VALUE);
	}

	/**
	 * Blocks the invoking thread a maximum amount of time until this Service 
	 * reaches any of the given states. 
	 * 
	 * @param desiredState
	 * 		A set of states to wait for.
	 * @param timeout
	 * 		The maximum amount of time to wait in milliseconds for the state to
	 * 		be reached.
	 * @return
	 * 		True if any of the states was reached.
	 */
	public boolean waitFor(int desiredState, long timeout) 
	{
		return state.waitFor(desiredState, timeout);
	}

	/**
	 * Returns whether this service has any of the given states.
	 * 
	 * @param desiredState
	 * 		A set of states to check for.
	 * @return
	 * 		True if this service has any of the given states.
	 */
	public boolean hasState(int desiredState) 
	{
		return state.has(desiredState);
	}

	/**
	 * Returns whether this service is running.
	 * 
	 * @return
	 * 		True if the service is running, paused, or is pausing.
	 */
	public boolean isRunning() 
	{
		return state.has(Running | Paused | Pausing);
	}
	
	/**
	 * Returns whether this service is in a resting state.
	 * 
	 * @return
	 * 		True if the service is stopped or paused.
	 */
	public boolean isResting() 
	{
		return state.has(Stopped | Paused);
	}
	
	/**
	 * Returns the notifier which manages the listeners to events on the 
	 * service. ServiceListeners can be directly added and removed to the
	 * notifier. Avoid invoking the methods of the proxy object in the notifier
	 * since it may notify the listeners falsely when an event has not actually
	 * occurred with the service. 
	 * 
	 * @return
	 * 		The reference to the ServiceListener notifier.
	 */
	public Notifier<ServiceListener<E>> getListeners() 
	{
		return notifier;
	}

	/**
	 * Returns the release object. This is used to unblock any blocking calls
	 * in any implementing services. The only time unblocking calls need to
	 * be interrupted is when the service is trying to paused or stopped.
	 * 
	 * @return
	 * 		The reference to this Service's release.
	 */
	public Release getRelease() 
	{
		return release;
	}

	/**
	 * Returns the remaining number of events this service can process before
	 * it automatically stops. If this returns -1 then this service will process
	 * events until the service is stopped.
	 * 
	 * @return
	 * 		The maximum number of events this service will handle.
	 */
	public int getRemainingEvents() 
	{
		return remainingEvents;
	}

	/**
	 * Sets the remaining number of events this service can process before it
	 * automatically stops. If this returns -1 then this service will process
	 * events until the service is stopped.
	 * 
	 * @param remainingEvents
	 * 		The maximum number of events this service will handle.
	 */
	public void setRemainingEvents(int remainingEvents) 
	{
		this.remainingEvents = remainingEvents;
	}

	/**
	 * Returns the remaining number of iterations this service can process
	 * before it automatically stops. An iteration is considered one round
	 * of processing events and an execute. If this returns -1 then this
	 * service will run indefinitely.
	 * 
	 * @return
	 * 		The maximum number of iterations this service can perform.
	 */
	public int getRemainingIterations() 
	{
		return remainingIterations;
	}

	/**
	 * Sets the remaining number of iterations this service can process
	 * before it automatically stops. An iteration is considered one round
	 * of processing events and an execute. If this returns -1 then this
	 * service will run indefinitely.
	 * 
	 * @param remainingIterations
	 * 		The maximum number of iterations this service can perform.
	 */
	public void setRemainingIterations(int remainingIterations) 
	{
		this.remainingIterations = remainingIterations;
	}

	/**
	 * Returns the remaining number of executions this service can perform
	 * before it automatically stops. If this returns -1 then this service will 
	 * execute until the service is stopped.
	 * 
	 * @return
	 * 		The maximum number of executions this service can perform.
	 */
	public int getRemainingExecutes() 
	{
		return remainingExecutes;
	}

	/**
	 * Sets the remaining number of executions this service can perform
	 * before it automatically stops. If this returns -1 then this service will 
	 * execute until the service is stopped.
	 * 
	 * @param remainingExecutes
	 * 		The maximum number of executions this service can perform.
	 */
	public void setRemainingExecutes(int remainingExecutes) 
	{
		this.remainingExecutes = remainingExecutes;
	}

	/**
	 * Checks for pausing and returns whether an event can be executed.
	 * 
	 * @return
	 * 		True if an event can be processed.
	 */
	private boolean canEvent() 
	{
		checkPause();
		return interrupt.runEvents;
	}

	/**
	 * Checks for pausing and returns whether to invoke execution.
	 * 
	 * @return
	 * 		True if an execution can be invoked.
	 */
	private boolean canExecute() 
	{
		checkPause();
		return interrupt.runExecute;
	}
	
	/**
	 * Checks whether the service should pause by waiting for the Running state
	 * to be set.
	 */
	private void checkPause() 
	{
		// double check
		if (paused) 
		{
			synchronized (state) 
			{
				// Actually pausing?
				if (state.has(Pausing)) 
				{
					// Pause!
					state.set(Paused);
					
					// Notify all listeners of pause.
					notifier.proxy().onServicePause(this, interrupt);
					
					// Wait for the running or stopping state.
					state.waitFor(Running | Stopping);
					
					// Notify all listeners of resume.
					notifier.proxy().onServiceResume(this, interrupt);
				}
			}
		}
	}

}
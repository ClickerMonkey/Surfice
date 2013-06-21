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



/**
 * An abstract implementation of a Service which also listens to its own events.
 * 
 * @author Philip Diffenderfer
 *
 * @param <E>
 * 		The event added to the service. Events added to the service
 * 		are handled in the service's thread by invoking the onServiceEvent
 * 		method in all attached listeners.
 */
public abstract class AbstractService<E> extends Service<E> implements ServiceListener<E> 
{

	/**
	 * Instantiates a new AbstractService.
	 */
	public AbstractService() 
	{
		getListeners().add(this);
	}
	
	/**
	 * Instantiates a new AbstractService.
	 * 
	 * @param blocking
	 * 		Whether the event queue for this service blocks when it polls for
	 * 		events or whether it returns immediately when empty.
	 */
	public AbstractService(boolean blocking) 
	{ 
		getListeners().add(this);
		getEventQueue().setBlocking(blocking);
	}
	
	/**
	 * Instantiates a new AbstractService.
	 * 
	 * @param sourceQueue
	 * 		The queue implementation to use internally as an event queue.
	 */
	public AbstractService(Queue<E> sourceQueue) 
	{
		super(new BlockableQueue<E>(sourceQueue));
		getListeners().add(this);
	}
	
	/**
	 * Instantiates a new AbstractService.
	 * 
	 * @param eventQueue
	 * 		The queue of events to poll from.
	 */
	public AbstractService(BlockableQueue<E> eventQueue) 
	{
		super(eventQueue);
		getListeners().add(this);
	}
	
	/**
	 * Invoked when the service handles an event.
	 * 
	 * @param event
	 * 		The event handled by the service.
	 */
	protected abstract void onEvent(E event);
	
	/**
	 * Invoked when the service performs an execution.
	 */
	protected abstract void onExecute();
	
	/**
	 * Invoked when the service has paused.
	 */
	protected abstract void onPause();
	
	/**
	 * Invoked when the service is resumed.
	 */
	protected abstract void onResume();
	
	/**
	 * Invoked when the service has started.
	 */
	protected abstract void onStart();
	
	/**
	 * Invoked when the service has stopped.
	 */
	protected abstract void onStop();
	
	
	/**
	 * {@inheritDoc}
	 */
	public final void onServiceEvent(Service<E> service, E event) 
	{
		this.onEvent(event);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final void onServiceExecute(Service<E> service) 
	{
		this.onExecute();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onServicePause(Service<E> service, ServiceInterrupt interrupt) 
	{
		this.onPause();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onServiceResume(Service<E> service, ServiceInterrupt interrupt) 
	{
		this.onResume();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onServiceStart(Service<E> service) 
	{
		this.onStart();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onServiceStop(Service<E> service, ServiceInterrupt interrupt) 
	{
		this.onStop();
	}

}

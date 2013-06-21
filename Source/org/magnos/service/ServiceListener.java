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

/**
 * A listener to events that occur within a service. All methods in a listener
 * are invoked in the service's thread.
 * 
 * @author Philip Diffenderfer
 *
 * @param <E> 
 * 		The event added to the service. Events added to the service
 * 		are handled in the service's thread by invoking the onServiceEvent
 * 		method in all attached listeners.
 */
public interface ServiceListener<E> 
{
	
	/**
	 * The method invoked when the service is handling an event. This method
	 * executes concurrently in the service's thread. This method is invoked in
	 * the beginning of a servicing iteration, before execution.
	 * 
	 * @param service
	 * 		The service invoking this listener.
	 * @param event
	 * 		The event being handled.
	 */
	public void onServiceEvent(Service<E> service, E event);
	
	/**
	 * The method invoked after the service has handled all events in the 
	 * current servicing iteration. This may not be called if the service is
	 * being shut down immediately.  
	 * 
	 * @param service
	 * 		The service invoking this listener.
	 */
	public void onServiceExecute(Service<E> service);
	
	/**
	 * The method invoked when the service thread has started and is about to 
	 * begin its servicing iterations. This method is only called once per
	 * time the service is started (a service can only be started if its not
	 * running at all).
	 * 
	 * @param service
	 * 		The service invoking this listener.
	 */
	public void onServiceStart(Service<E> service);
	
	/**
	 * The method invoked when the service has successfully paused. The next 
	 * method invoked in the listener will either be stop or resume.
	 * 
	 * @param service
	 * 		The service invoking this listener.
	 * @param interrupt
	 * 		The interrupt type given to pause the service.
	 */
	public void onServicePause(Service<E> service, ServiceInterrupt interrupt);
	
	/**
	 * The method invoked when the service has successfully resumed. The method
	 * previously invoked can only be pause.
	 * 
	 * @param service
	 * 		The service invoking this listener.
	 * @param interrupt
	 * 		The interrupt type given to pause the service.
	 */
	public void onServiceResume(Service<E> service, ServiceInterrupt interrupt);
	
	/**
	 * The method invoked when the service has successfully stopped.
	 * 
	 * @param service
	 * 		The service invoking this listener.
	 * @param interrupt
	 * 		The interrupt type given to stop the service.
	 */
	public void onServiceStop(Service<E> service, ServiceInterrupt interrupt);
	
}
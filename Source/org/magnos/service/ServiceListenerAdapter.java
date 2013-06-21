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
 * An adapter which implements all ServiceListener methods. This can be used
 * to only listen to specific service events. i.e.
 * 
 * <h1>Example Usage</h1>
 * <pre>
 * service.addListener(new ServiceListenerAdapter() {
 * 	public void onServiceEvent(Service service, String event) {
 * 		
 * 	}
 * });
 * </pre>
 * 
 * @author Philip Diffenderfer
 *
 * @param <E> The event added to the service. Events added to the service
 * 		are handled in the service's thread by invoking the onServiceEvent 
 * 		method in all attached listeners.
 */
public class ServiceListenerAdapter<E> implements ServiceListener<E>
{
	
	/**
	 * {@inheritDoc}
	 */
	public void onServiceEvent(Service<E> service, E event)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public void onServiceExecute(Service<E> service)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public void onServiceStart(Service<E> service)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public void onServicePause(Service<E> service, ServiceInterrupt interrupt)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public void onServiceResume(Service<E> service, ServiceInterrupt interrupt)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public void onServiceStop(Service<E> service, ServiceInterrupt interrupt)
	{
	}
	
}
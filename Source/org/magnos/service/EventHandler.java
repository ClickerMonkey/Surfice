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
 * A generic handler of events.
 * 
 * @author Philip Diffenderfer
 *
 * @param <E>
 * 		The event type.
 */
public interface EventHandler<E> 
{

	/**
	 * Adds the event to the handler. The handler can choose to deny the event
	 * from being processed. Even if the handler accepts the event it may not 
	 * run if the application is being stopped.
	 * 
	 * @param event
	 * 		The event to add and process.
	 * @return
	 * 		True if the event was accepted, otherwise false.
	 */
	public boolean addEvent(E event);
	
}

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

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;


import org.junit.Test;
import org.magnos.service.AbstractService;
import org.magnos.service.Service;
import org.magnos.test.BaseTest;

public class TestService extends BaseTest
{
	class MessageService extends AbstractService<String> {
		public MessageService() {
			super(true);
		}
		protected void onEvent(String event) {
			System.out.format("Message receieved: '%s'\n", event);
		}
		protected void onExecute() {
		}
		protected void onPause() {
			System.out.println("Paused");
		}
		protected void onResume() {
			System.out.println("Resumed");
		}
		protected void onStart() {
			System.out.println("Started");
		}
		protected void onStop() {
			System.out.println("Stopped");
		}
	}
	

	@Test
	public void test() throws InterruptedException {
		MessageService service = new MessageService();

		assertTrue( service.hasState(Service.Stopped) );
		service.start(false);
		
		service.waitFor(Service.Running);
		assertTrue( service.hasState(Service.Running) );
		
		service.addEvent("Hello World");
		Thread.sleep(500);
		service.addEvent("Greetings Earthling");
		
		service.setEventAccept(false);
		service.addEvent("This will not be recieved");
		Thread.sleep(500);
		
		service.setEventAccept(true);
		service.addEvent("But this will be recieved.");
		Thread.sleep(500);
		
		service.stop();
		assertTrue( service.hasState(Service.Stopped) );
	}
	
	@Test
	public void testWait() throws InterruptedException {
		final MessageService service = new MessageService();
		
		// This thread waits for the service to stop.
		startDaemon(1, new Runnable() {
			public void run() {
				service.waitFor(Service.Stopping);
				System.out.println("The service is stopping");
				service.waitFor(Service.Stopped);
				System.out.println("The service has stopped");
			}
		});

		Thread.sleep(500);		// 1. Wait a second for the daemon to start
		service.start();		// 2. Start the service
		Thread.sleep(1000);		// 3. Wait 3 seconds
		service.stop();			// 4. Stop the service
	}

	
	
	public void startDaemon(final Runnable runnable) {
		startDaemon(Integer.MAX_VALUE, runnable);
	}
	public void startDaemon(int repeats, final Runnable runnable) {
		final AtomicInteger counter = new AtomicInteger(repeats);
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				while (counter.decrementAndGet() >= 0) {
					runnable.run();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}
	
}

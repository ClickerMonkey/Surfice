surfice
=======

A Java library centered around the concept of a Service, which is a glorified thread that continually loops performing some job and handling events sent to the Service.

**Example**

```java
// A service which receives string events and echoes them.
public class EchoService extends AbstractService<String> 
{
    protected void onStart() {
        System.out.println("Service started");
    }
    protected void onEvent(String event) {
        // This occurs in the context of the service, not the thread that added the event.
        System.out.println(event);
    }
    protected void onExecute() {
        System.out.println("Service execute");
    }
    protected void onPause() {
        System.out.println("Service paused");
    }
    protected void onResume() {
        System.out.println("Service resumed");
    }
    protected void onStop() {
        System.out.println("Service stopped");
    }
}

// Create the service
EchoService service = new EchoService();
// Restrict the number of events it can process before it automatically stops.
service.setRemainingEvents(5);

// Start the service and add the event
service.start();
service.addEvent("Hello World");
// Request the service to pause and wait until it has paused
service.pause();
// Finally resume the service once it has paused.
service.resume();
// Stop the service now.
service.stop();

// Restart the service and stop it again.
service.start();
service.stop();
```

**Builds**

https://github.com/ClickerMonkey/surfice/tree/master/build

**Testing Examples**

https://github.com/ClickerMonkey/surfice/tree/master/Testing/org/magnos/service

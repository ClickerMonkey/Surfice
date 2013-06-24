surfice
=======

A Java library centered around the concept of a Service, which is a glorified thread that continually loops performing some job and handling events sent to the Service.

**Features**
- Services can be started, paused, resumed, and stopped whenever. This is entirely thread-safe.
- A service can be interrupted in several fashions, it can be paused/stopped immediately, after it's done executing the current task, after it's done handling events, or after the service is done entirely.
- A service can have multiple listeners that are notified when it receives an event, it has just executed it's main piece of code, it has started, it has been paused, it was resumed, and it has stopped.

**Documentation**
- [JavaDoc](http://gh.magnos.org/?r=http://clickermonkey.github.com/surfice/)

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
- [surfice-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/surfice/blob/master/build/surfice-1.0.0.jar?raw=true)
- [surfice-src-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/surfice/blob/master/build/surfice-src-1.0.0.jar?raw=true) *- includes source code*
- [surfice-all-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/surfice/blob/master/build/surfice-1.0.0.jar?raw=true) *- includes all dependencies*
- [surfice-all-src-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/surfice/blob/master/build/surfice-src-1.0.0.jar?raw=true) *- includes all dependencies and source code*

**Projects using surfice:**
- [taskaroo](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/taskaroo)
- [statastic](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic)
- [falcon](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/falcon)

**Dependencies**
- [curity](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/curity)
- [testility](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/testility) *for unit tests*

**Testing Examples**
- [Testing/org/magnos/service](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/surfice/tree/master/Testing/org/magnos/service)

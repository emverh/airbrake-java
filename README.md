Airbrake Java
=============

This is the notifier jar for integrating apps with [Airbrake](http://airbrake.io) with Java Applications. Sign up for a [Free](https://airbrake.io/account/new/Free) or [Paid](https://airbrake.io/account/new?source=github) account.

When an uncaught exception occurs, Airbrake will POST the relevant data
to the Airbrake server specified in your environment.

The easy way to use airbrake is configuring a logback appender. Otherwise if you don't
use logback you can use airbrake notifier directly with a very simple API.

Setting up with Maven
---------------------

	<project>
  		<dependencies>
    		<dependency>
      		<groupId>io.airbrake</groupId>
      		<artifactId>airbrake-java-logback</artifactId>
      		<version>2.2.9</version>
    		</dependency>
  		</dependencies>
	</project>

Without Maven
-------------

you need to add these libraries to your classpath
 * [airbrake-java-2.2.9](https://github.com/davidkeen/airbrake-java/blob/master/maven2/io/airbrake/airbrake-java/2.2.8/airbrake-java-2.2.8.jar?raw=true)
 * [log4j-1.2.14](https://github.com/airbrake/airbrake-java/blob/master/maven2/log4j/1.2.14/log4j-1.2.14.jar?raw=true)

Logback
-------

    in XML format:

    <appender name="AIRBRAKE" class="airbrake.AirbrakeAppender">
        <apiKey>81bff829226bfcb7f72ae8a0de2be9ff</apiKey>
        <env>production</env>
        <enabled>true</enabled>
        <url>http://api.airbrake.io/notifier_api/v2/notices</url>

        <!-- deny all events with a level below ERROR, that is TRACE, DEBUG, INFO -->
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
    </appender>

	<root>
		<appender-ref ref="AIRBRAKE"/>
	</root>

Directly
------------------------------

	try {
  		doSomethingThatThrowAnException();
	}
	catch(Throwable t) {
  		AirbrakeNotice notice = new AirbrakeNoticeBuilder(YOUR_AIRBRAKE_API_KEY, t, "env").newNotice();
  		AirbrakeNotifier notifier = new AirbrakeNotifier();
  		notifier.notify(notice);
	}

if you need to specifiy a different url to send notice you can create new notifier with this url:

	try {
  		doSomethingThatThrowAnException();
	}
	catch(Throwable t) {
  		AirbrakeNotice notice = new AirbrakeNoticeBuilder(YOUR_AIRBRAKE_API_KEY, t, "env").newNotice();
  		AirbrakeNotifier notifier = new AirbrakeNotifier("http://api.airbrake.io/notifier_api/v2/notices");
  		notifier.notify(notice);
	}


	

Support
-------

For help with using Airbrake and this notifier visit [our support site](http://help.airbrake.io).

For SSL verification see the [Resources](https://github.com/airbrake/airbrake/blob/master/resources/README.md).

For any issues, please post then in our [Issues](https://github.com/airbrake/airbrake-java/issues).



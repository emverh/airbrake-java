// Modified or written by Luca Marrocco for inclusion with airbrake.
// Copyright (c) 2009 Luca Marrocco.
// Licensed under the Apache License, Version 2.0 (the "License")

package airbrake;

import static airbrake.ApiKeys.API_KEY;
import static airbrake.Exceptions.ERROR_MESSAGE;
import static airbrake.Exceptions.newException;
import static airbrake.Slurp.read;
import static airbrake.Slurp.slurp;
import static airbrake.Slurp.strings;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ThrowableProxy;

public class AirbrakeNotifierTest {

    public static final String END_POINT = "http://api.airbrake.io/notifier_api/v2/notices";
    protected static final Backtrace BACKTRACE = new Backtrace(asList("backtrace is empty"));
    protected static final Map<String, Object> REQUEST = new HashMap<String, Object>();
    protected static final Map<String, Object> SESSION = new HashMap<String, Object>();
    protected static final Map<String, Object> ENVIRONMENT = new HashMap<String, Object>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, Object> EC2 = new HashMap<String, Object>();

    private AirbrakeNotifier notifier;

    private <T> Matcher<T> internalServerError() {
        return new BaseMatcher<T>() {
            public void describeTo(final Description description) {
                description.appendText("internal server error");
            }

            public boolean matches(final Object item) {
                return item.equals(500);
            }
        };
    }

    private int notifing(final String string) throws InterruptedException {

        return new AirbrakeNotifier().notify(new AirbrakeNoticeBuilder(API_KEY, ERROR_MESSAGE) {
            {
                backtrace(new Backtrace(asList(string)));
            }
        }.newNotice());
    }

    private int notifyStatus(AirbrakeNotice notice) throws InterruptedException {
        int code;
        int iteration = 1;
        do {
            code = notifier.notify(notice);
            if (code == 429) {
                Thread.sleep(500 * iteration);
            }
            iteration++;
        } while (code == 429);

        return code;
    }

    @Before
    public void setUp() {
        ENVIRONMENT.put("A_KEY", "test");
        EC2.put("AWS_SECRET", "AWS_SECRET");
        EC2.put("EC2_PRIVATE_KEY", "EC2_PRIVATE_KEY");
        EC2.put("AWS_ACCESS", "AWS_ACCESS");
        EC2.put("EC2_CERT", "EC2_CERT");
        notifier = new AirbrakeNotifier();
    }

    @Test
    public void testHowBacktraceairbrakeNotInternalServerError() throws InterruptedException {
        assertThat(notifing(ERROR_MESSAGE), not(internalServerError()));
        assertThat(notifing("java.lang.RuntimeException: an expression is not valid"), not(internalServerError()));
        assertThat(notifing("Caused by: java.lang.NullPointerException"), not(internalServerError()));
        assertThat(
                notifing("at org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1307)"),
                not(internalServerError()));
        assertThat(notifing("... 23 more"), not(internalServerError()));
    }

    @Test
    public void testLogErrorWithException() {
        logger.error("error", newException(ERROR_MESSAGE));
    }

    @Test
    public void testLogErrorWithoutException() {
        logger.error("error");
    }

    @Test
    public void testLogThresholdLesserThatErrorWithExceptionDoNotNotifyToairbrake() {
        logger.info("info", newException(ERROR_MESSAGE));
        logger.warn("warn", newException(ERROR_MESSAGE));
    }

    @Test
    public void testLogThresholdLesserThatErrorWithoutExceptionDoNotNotifyToairbrake() {
        logger.info("info");
        logger.warn("warn");
    }

    @Test
    public void testNotifyToairbrakeUsingBuilderNoticeFromExceptionInEnv() throws InterruptedException {
        final Exception EXCEPTION = newException(ERROR_MESSAGE);
        final AirbrakeNotice notice = new AirbrakeNoticeBuilder(API_KEY, new ThrowableProxy(EXCEPTION), "test")
                .newNotice();

        assertThat(notifyStatus(notice), is(200));
    }

    @Test
    public void testNotifyToairbrakeUsingBuilderNoticeFromExceptionInEnvAndSystemProperties()
            throws InterruptedException {
        final Exception EXCEPTION = newException(ERROR_MESSAGE);
        final AirbrakeNotice notice = new AirbrakeNoticeBuilder(API_KEY, new ThrowableProxy(EXCEPTION), "test") {
            {
                filteredSystemProperties();
            }
        }.newNotice();

        assertThat(notifyStatus(notice), is(200));
    }

    @Test
    public void testNotifyToairbrakeUsingBuilderNoticeInEnv() throws InterruptedException {
        final AirbrakeNotice notice = new AirbrakeNoticeBuilder(API_KEY, ERROR_MESSAGE, "test").newNotice();

        assertThat(notifyStatus(notice), is(200));
    }

    @Test
    public void testSendExceptionNoticeWithFilteredBacktrace() throws InterruptedException {
        final Exception EXCEPTION = newException(ERROR_MESSAGE);
        final AirbrakeNotice notice = new AirbrakeNoticeBuilder(API_KEY, new QuietRubyBacktrace(), new ThrowableProxy(
                EXCEPTION), "test").newNotice();

        assertThat(notifyStatus(notice), is(200));
    }

    @Test
    public void testSendExceptionToairbrake() throws InterruptedException {
        final Exception EXCEPTION = newException(ERROR_MESSAGE);
        final AirbrakeNotice notice = new AirbrakeNoticeBuilder(API_KEY, new ThrowableProxy(EXCEPTION)).newNotice();

        assertThat(notifyStatus(notice), is(200));
    }

    @Test
    public void testSendExceptionToairbrakeUsingRubyBacktrace() throws InterruptedException {
        final Exception EXCEPTION = newException(ERROR_MESSAGE);
        final AirbrakeNotice notice = new AirbrakeNoticeBuilder(API_KEY, new RubyBacktrace(), new ThrowableProxy(
                EXCEPTION), "test").newNotice();

        assertThat(notifyStatus(notice), is(200));
    }

    @Test
    public void testSendExceptionToairbrakeUsingRubyBacktraceAndFilteredSystemProperties() throws InterruptedException {
        final Exception EXCEPTION = newException(ERROR_MESSAGE);
        final AirbrakeNotice notice = new AirbrakeNoticeBuilderUsingFilteredSystemProperties(API_KEY,
                new RubyBacktrace(), new ThrowableProxy(EXCEPTION), "test").newNotice();

        assertThat(notifyStatus(notice), is(200));
    }

    @Test
    public void testSendNoticeToairbrake() throws InterruptedException {
        final AirbrakeNotice notice = new AirbrakeNoticeBuilder(API_KEY, ERROR_MESSAGE).newNotice();

        assertThat(notifyStatus(notice), is(200));
    }

    @Test
    public void testSendNoticeWithFilteredBacktrace() throws InterruptedException {
        final AirbrakeNotice notice = new AirbrakeNoticeBuilder(API_KEY, ERROR_MESSAGE) {
            {
                backtrace(new QuietRubyBacktrace(strings(slurp(read("backtrace.txt")))));
            }
        }.newNotice();

        assertThat(notifyStatus(notice), is(200));
    }

    @Test
    public void testSendNoticeWithLargeBacktrace() throws InterruptedException {
        final AirbrakeNotice notice = new AirbrakeNoticeBuilder(API_KEY, ERROR_MESSAGE) {
            {
                backtrace(new Backtrace(strings(slurp(read("backtrace.txt")))));
            }
        }.newNotice();

        assertThat(notifyStatus(notice), is(200));
    }
}

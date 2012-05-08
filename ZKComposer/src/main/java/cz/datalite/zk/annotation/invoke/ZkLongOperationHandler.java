/*
 * Copyright (c) 2012, DataLite. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.datalite.zk.annotation.invoke;

import cz.datalite.zk.annotation.ZkLongOperation;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.lang.Library;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zul.Window;

/**
 * <p>Handles binding request before and after method invocation. For all
 * registered component executes load or safe based on annotation's
 * properties.</p>
 *
 * @author Karel Čemus <cemus@datalite.cz>
 */
public class ZkLongOperationHandler extends Handler {

    private static final Logger LOGGER = Logger.getLogger(ZkLongOperationHandler.class.getCanonicalName());

    /** name of echo event */
    private static final String EVENT = "onEchoEvent";

    /** used queue name */
    private static final String QUEUE = "qLongOperations";

    /** message to be shown to a user */
    private final String message;

    /** if the operation is interruptable */
    private final boolean cancellable;

    /** state of general property */
    private static boolean localizeAll;

    /** opened busy window */
    private Window busybox;

    static {
        /** Reads default configuration for library */
        localizeAll = Boolean.parseBoolean(Library.getProperty("zk-dl.annotation.i18n", "false"));
    }

    public static Invoke process(Invoke inner, ZkLongOperation annotation) {
        String message = annotation.message();
        // check for default localized message
        boolean i18n = localizeAll || message.startsWith("zkcomposer.") || annotation.i18n();
        return new ZkLongOperationHandler(inner, message, i18n, annotation.cancellable());
    }

    public ZkLongOperationHandler(Invoke inner, final String message, final boolean i18n, final boolean cancellable) {
        super(inner);
        this.message = i18n ? Labels.getLabel(message) : message;
        this.cancellable = cancellable;
    }

    @Override
    protected boolean doBeforeInvoke(final Event event, final Component master, final Object controller) {

        if (cancellable) { // request for async processing
            // receiving async request failed
            if (!fireAsynchronnousEvent(event, master, controller)) {
                return false;
            }
        } else { // receive sync request
            fireEchoEvent(event, master, controller);
        }

        // invokes status window informing user about operation
        invokeBusyBox();

        // prevent invoke propagation
        return false;
    }

    @Override
    protected void doAfterInvoke(Event event, Component master, Object controller) {
        if (busybox != null) {
            busybox.detach();
        }
        busybox = null;
    }

    /**
     * Invokes busy box window informing user about long running operation
     */
    private void invokeBusyBox() {
        // show busy message
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("cancellable", cancellable);
        parameters.put("message", message);
        busybox = (Window) Executions.createComponents("~./busybox.zul", null, parameters);

        if (cancellable) {
            busybox.addEventListener(Events.ON_CLOSE, new EventListener() {

                public void onEvent(Event event) throws Exception {
                    // ToDo cancelable
                    LOGGER.log(Level.WARNING, "STOP!!....");
                    event.stopPropagation();
                }
            });
        }
    }

    /**
     * Fires assynchronnous event - that allows cancelable operations
     */
    private boolean fireAsynchronnousEvent(final Event event, final Component master, final Object controller) {
        // check for already running
        if (EventQueues.exists(QUEUE, EventQueues.SESSION)) {
            LOGGER.log(Level.FINE, "One operation is already running. Request was rejected.");
            return false; //busy
        }

        // create / get a queue
        EventQueue queue = EventQueues.lookup(QUEUE, EventQueues.SESSION, true);

        // subscribe async listener to handle long operation
        queue.subscribe(new EventListener() {

            public void onEvent(Event evt) { //asynchronous
                try {
                    Thread.currentThread().sleep(5000);
                    LOGGER.log(Level.FINE, "Starting async operation.");
                    goOn(event, master, controller);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Execution of long running operation failed. Exception has been thrown.", ex);
                }
            }
        }, new EventListener() {  //callback

            public void onEvent(Event evt) throws Exception {
                LOGGER.log(Level.FINE, "Async operation finished.");
                if (busybox != null) {
                    busybox.detach();
                }
                EventQueues.remove(QUEUE, EventQueues.SESSION);
            }
        });

        // fire event to start the long operation
        queue.publish(new Event("FireEvent"));
        return true; // fired
    }

    /**
     * fires synchronnous echo event
     *
     * @param sourceEvent source of event origin
     * @param master      master component (usually window)
     * @param controller  controller of the master component
     */
    private void fireEchoEvent(final Event sourceEvent, final Component master, final Object controller) {

        master.addEventListener(EVENT, new EventListener() {

            public void onEvent(Event event) throws Exception {
                master.removeEventListener(EVENT, this);


//                final Timer timer = new Timer(1000);
//                timer.setParent(master);
//                timer.setRepeats(true);
//                timer.addEventListener(Events.ON_TIMER, new EventListener() {
//
//                    public void onEvent(Event event) throws Exception {
//                        Logger.getLogger(ZkLongOperationHandler.class.getCanonicalName()).log(Level.FINE, "timer");
//                    }
//                });
//                timer.start();

                // user was informed, go on in execution
                goOn(sourceEvent, master, controller);
            }
        });
        // fire event
        Events.echoEvent(EVENT, master, null);
    }
}
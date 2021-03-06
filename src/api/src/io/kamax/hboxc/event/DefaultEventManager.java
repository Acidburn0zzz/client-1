/*
 * Hyperbox - Virtual Infrastructure Manager
 * Copyright (C) 2013 Maxime Dor
 * 
 * http://kamax.io/hbox/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.hboxc.event;

import io.kamax.hbox.exception.HyperboxException;
import io.kamax.tool.logging.Logger;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import net.engio.mbassy.IPublicationErrorHandler;
import net.engio.mbassy.PublicationError;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.config.BusConfiguration;

public class DefaultEventManager implements _EventManager, Runnable, UncaughtExceptionHandler {

    private String label;

    private Set<_EventProcessor> postProcessors = new HashSet<_EventProcessor>();
    protected MBassador<Object> eventBus;
    private BlockingQueue<Object> eventsQueue;
    private boolean running;
    private Thread worker;

    public DefaultEventManager() {
        this("EMW");
    }

    public DefaultEventManager(String label) {
        this.label = label;
    }

    private void stopWorker() {
        running = false;
        worker.interrupt();
        try {
            worker.join(1000);
        } catch (InterruptedException e) {
            Logger.exception(e);
        }
    }

    private void startWorker() {
        worker = new Thread(this);
        worker.setUncaughtExceptionHandler(this);
        worker.setName(label);
        worker.start();
    }

    @Override
    public void uncaughtException(Thread arg0, Throwable arg1) {
        Logger.error("Event Manager " + label + " Worker Thread has crashed: " + arg1.getMessage());
        stopWorker();
        startWorker();
    }

    @Override
    public void start() throws HyperboxException {
        Logger.verbose("Event Manager - " + label + " - is starting");
        eventBus = new MBassador<Object>(BusConfiguration.Default());
        eventBus.addErrorHandler(new IPublicationErrorHandler() {

            @Override
            public void handleError(PublicationError error) {
                Logger.error("Failed to dispatch event " + error.getPublishedObject(), error.getCause());
            }

        });
        eventsQueue = new LinkedBlockingQueue<Object>();
        startWorker();
        Logger.verbose("Event Manager - " + label + " - has started");
    }

    @Override
    public void start(_EventProcessor postProcessor) throws HyperboxException {
        postProcessors.add(postProcessor);
        start();
    }

    @Override
    public void stop() {
        if (running) {
            Logger.verbose("Event Manager - " + label + " - is stopping");
            stopWorker();
            eventsQueue = null;
            Logger.verbose("Event Manager - " + label + " - has stopped");
        }
    }

    @Override
    public void register(Object o) {
        eventBus.subscribe(o);
    }

    @Override
    public void unregister(Object o) {
        eventBus.unsubscribe(o);
    }

    @Override
    public void post(Object o) {
        if (eventsQueue != null) {
            if (!eventsQueue.offer(o)) {
                Logger.error("Event Manager - " + label + " queue is full, cannot add " + o.getClass().getSimpleName());
            }
        } else {
            Logger.error("Event Manager - " + label + " was not started, event ignored");
        }
    }

    protected void publish(Object event) throws Throwable {
        send(event);
    }

    protected final void send(Object event) {
        eventBus.publish(event);
    }

    @Override
    public void run() {
        Logger.debug("Event Manager - " + label + " Worker Started");
        running = true;
        while (running) {
            try {
                Object event = eventsQueue.take();
                Logger.debug("Processing Event " + event.getClass().getSimpleName() + ": " + event.toString());
                publish(event);
                for (_EventProcessor postProcessor : postProcessors) {
                    postProcessor.post(event);
                }
            } catch (InterruptedException e) {
                Logger.debug("Interupted, halting...");
            } catch (Throwable e) {
                Logger.error("Error while trying to dispatch event");
                Logger.exception(e);
            }
        }
        Logger.debug("Event Manager - " + label + " Worker halted.");
    }

    @Override
    public void add(_EventProcessor postProcessor) {
        postProcessors.add(postProcessor);
    }

}

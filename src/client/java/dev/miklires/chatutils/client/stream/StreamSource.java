package dev.miklires.chatutils.client.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class StreamSource {

    private final ConcurrentLinkedQueue<StreamMessage> inbox = new ConcurrentLinkedQueue<>();
    private int inboxSize;

    private final String target;

    private volatile boolean running;
    private volatile Thread worker;

    protected StreamSource(String target) {
        this.target = target;
    }

    public final String target() {
        return target;
    }

    public final boolean isRunning() {
        return running;
    }

    public final synchronized void start() {
        if (running) {
            return;
        }
        running = true;

        worker = new Thread(this::runSafely, "chatutils-" + name());
        worker.setDaemon(true);
        worker.start();
    }

    public final synchronized void stop() {
        running = false;
        close();
        Thread active = worker;
        if (active != null) {
            active.interrupt();
        }
    }

    public final synchronized List<StreamMessage> drain(int limit) {
        List<StreamMessage> messages = new ArrayList<>();
        while (messages.size() < limit) {
            StreamMessage message = inbox.poll();
            if (message == null) {
                break;
            }
            messages.add(message);
            inboxSize--;
        }
        return messages;
    }

    protected final synchronized void deliver(StreamMessage message) {
        while (inboxSize >= 512) {
            if (inbox.poll() != null) {
                inboxSize--;
            } else {
                inboxSize = 0;
                break;
            }
        }
        inbox.add(message);
        inboxSize++;
    }

    protected final boolean shouldRun() {
        return running;
    }

    private void runSafely() {
        try {
            run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            StreamChat.reportFailure(this, e);
        } finally {
            running = false;
            worker = null;
        }
    }

    protected abstract void run() throws Exception;

    protected abstract void close();

    public abstract String name();
}

package dev.miklires.chatutils.client.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A live chat the mod is reading from somewhere outside the game.
 *
 * <p>Every source runs its own networking off the game thread and drops what it receives into a
 * queue; the game thread drains that queue on its own schedule. Nothing here ever blocks the client,
 * and a service being slow, rude or offline can only ever mean "no new messages".
 *
 * <p>Read-only by design. The mod connects anonymously where it can and never sends anything back:
 * a Minecraft chat mod has no business posting to someone's stream chat, and not asking for write
 * access means never asking for the credentials that would allow it.
 */
public abstract class StreamSource {

    private final ConcurrentLinkedQueue<StreamMessage> inbox = new ConcurrentLinkedQueue<>();
    private int inboxSize;

    /** What this source is configured to read, used to notice when the config changed under it. */
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

    /** Starts the background worker. Safe to call once; a stopped source is not restarted. */
    public final synchronized void start() {
        if (running) {
            return;
        }
        running = true;

        worker = new Thread(this::runSafely, "chatutils-" + name());
        // A daemon thread so a stuck socket can never keep the game from closing.
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

    /** Everything received since the last call, oldest first. */
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
        // A source that outruns the drain must not grow without bound; the oldest chatter loses.
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
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception failure) {
            StreamChat.reportFailure(this, failure);
        } finally {
            running = false;
            worker = null;
        }
    }

    /** Runs on the worker thread until {@link #shouldRun()} goes false or the connection dies. */
    protected abstract void run() throws Exception;

    /** Releases the connection. Called from the game thread, so it must not block. */
    protected abstract void close();

    /** Short name for logs and thread names. */
    public abstract String name();
}

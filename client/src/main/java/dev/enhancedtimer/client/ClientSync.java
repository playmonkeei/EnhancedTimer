package dev.enhancedtimer.client;

import dev.enhancedtimer.core.*;
import dev.enhancedtimer.network.TimerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

/** Server messages only update the remote clock. They never invoke local commands or settings. */
public final class ClientSync {
    public final TimerSession session = new TimerSession();
    public boolean connected, canControl;
    public String scope = "global";
    private long sequence = -1, lastReceive;
    private int helloAttempts;
    private boolean registered;
    public void reset() { connected = false; canControl = false; registered = false; sequence = -1; lastReceive = 0; helloAttempts = 0; session.configure(TimerEngine.Mode.STOPWATCH, 7_200_000); session.forgetHistory(); }
    public void tick(int tick) {
        if (Minecraft.getInstance().getConnection() == null) return;
        if (!connected && helloAttempts < 3 && tick % 100 == 0) {
            send(new SyncProtocol.Message(SyncProtocol.Kind.HELLO, "", "", 0, null, false, false, 0)); helloAttempts++;
        }
        boolean localPause = Minecraft.getInstance().getSingleplayerServer() != null && Minecraft.getInstance().isPaused();
        if (connected && localPause) { session.timer.suspend(true); lastReceive = System.nanoTime(); }
        if (connected && !localPause && System.nanoTime() - lastReceive > 5_000_000_000L) {
            connected = false; canControl = false; session.timer.pause();
        }
    }
    public void receive(SyncProtocol.Message message) {
        if (message.kind() == SyncProtocol.Kind.STATE && message.sequence() > sequence) {
            boolean first = !connected;
            if (message.timer().mode() != session.timer.mode() || message.timer().elapsedMillis() < session.timer.elapsedMillis() - 1000
                    || message.timer().state() == TimerEngine.State.READY) session.forgetHistory();
            sequence = message.sequence(); lastReceive = System.nanoTime(); connected = true;
            canControl = message.canControl(); scope = message.scope();
            session.timer.synchronize(message.timer(), message.suspended());
            session.remoteHistory(0, java.util.List.of());
            if (first && EnhancedTimerClient.get().settings.serverHud) select(scope);
        } else if (message.kind() == SyncProtocol.Kind.HISTORY && message.sequence() == sequence && message.scope().equals(scope)
                && message.duration() >= 0 && message.duration() <= 256) {
            session.remoteHistory((int)message.duration(), message.splits());
        } else if (message.kind() == SyncProtocol.Kind.ERROR) EnhancedTimerClient.feedback(message.action());
    }
    public void select(String scope) {
        if (!connected) return;
        EnhancedTimerClient.get().settings.serverHud = true; EnhancedTimerClient.get().save();
        send(new SyncProtocol.Message(SyncProtocol.Kind.SELECT, scope, "", 0, null, false, false, 0));
    }
    public void useLocal() {
        EnhancedTimerClient.get().settings.serverHud = false; EnhancedTimerClient.get().save();
        send(new SyncProtocol.Message(SyncProtocol.Kind.HELLO, "", "local", 0, null, false, false, 0));
    }
    public void action(String action) {
        if (!connected || !canControl) return;
        send(new SyncProtocol.Message(SyncProtocol.Kind.ACTION, scope, action, 0, null, false, false, 0));
    }
    public void configure(TimerEngine.Mode mode, long duration) {
        if (!connected || !canControl) return;
        send(new SyncProtocol.Message(SyncProtocol.Kind.CONFIGURE, scope, mode.name(), duration, null, false, false, 0));
    }
    private void send(SyncProtocol.Message message) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            if (!registered) {
                connection.send(new ServerboundCustomPayloadPacket(new dev.enhancedtimer.network.ChannelRegistration())); registered = true;
            }
            try { connection.send(new ServerboundCustomPayloadPacket(new TimerPayload(SyncProtocol.encode(message)))); }
            catch (UnsupportedOperationException unavailableChannel) {
                // NeoForge rejects unknown channels before the server's registration arrives.
                // A vanilla server may never advertise EnhancedTimer; local timers remain available.
            }
        }
    }
}

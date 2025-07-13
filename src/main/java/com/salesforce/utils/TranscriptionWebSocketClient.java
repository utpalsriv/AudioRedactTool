package com.salesforce.utils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TranscriptionWebSocketClient extends WebSocketClient {
    
    private final CompletableFuture<Void> openFuture;
    private final CompletableFuture<Void> closeFuture;
    private Consumer<String> messageHandler;

    public TranscriptionWebSocketClient(URI serverUri, Map<String, String> headers, 
                                      CompletableFuture<Void> openFuture, 
                                      CompletableFuture<Void> closeFuture) {
        super(serverUri, headers);
        this.openFuture = openFuture;
        this.closeFuture = closeFuture;
    }

    public void addMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("WebSocket connection opened");
        openFuture.complete(null);
    }

    @Override
    public void onMessage(String message) {
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket connection closed: code=" + code + ", reason='" + reason + "', remote=" + remote);
        if (code != 1000) { // 1000 is normal closure
            System.err.println("WebSocket closed unexpectedly with code: " + code);
        }
        closeFuture.complete(null);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
        openFuture.completeExceptionally(ex);
        closeFuture.completeExceptionally(ex);
    }

    public void send(ByteBuffer data) {
        if (!isOpen()) {
            throw new RuntimeException("WebSocket is not connected. Cannot send data.");
        }
        super.send(data);
    }

    public boolean isConnected() {
        return isOpen();
    }
} 
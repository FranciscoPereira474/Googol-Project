// NotificationController.java
package googol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.rmi.RemoteException;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private GatewayService gatewayService;

    @MessageMapping("/sendMessage") // Endpoint matching the JavaScript destination
    @SendTo("/topic/notifications") // Broadcast to subscribers of this topic
    public String sendMessage(String message) {
        System.out.println("Received message: " + message); // Debugging log
        return message; // Broadcast the message
    }

    @MessageMapping("/clientConnected") // Handle client connection
    public void clientConnected() {
        // System.out.println("Client connected: " + message); // Debugging log
        // notificationService.sendNotification("Welcome to Googol!");
        try {
            // gatewayService.notifyClients("Client connected: " + message); // Notify other clients

            gatewayService.sendStatsToClients();
        } catch (RemoteException e) {
            System.err.println("Failed to notify clients: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
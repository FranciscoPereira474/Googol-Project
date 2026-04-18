package googol;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class NotificationService{
    private static NotificationService instance;

@Autowired
    private SimpMessagingTemplate template;
    public void sendNotification(String message) {
        template.convertAndSend("/topic/notifications", message);
    }

    public void sendNotification(List<String> messages) {
        template.convertAndSend("/topic/notifications", messages);
    }



}

package googol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.AbstractMap.SimpleEntry; // Use SimpleEntry as an alternative to Pair
import java.util.ArrayList;
// Removed unused import for javafx.util.Pair


@Service
public class GatewayService {

    private static final int MAX_RETRIES = 30;

    private GatewayInterface gateway;

    @Autowired
    private NotificationService notificationService;

    private String gatewayIP;
    private int gatewayPort;

    @PostConstruct
    public void init() {
        try {
            setGatewaySettings();
            gateway = retryRemoteCall(
                () -> (GatewayInterface) Naming.lookup("rmi://" + gatewayIP + ":" + gatewayPort + "/gateway"),
                "Failed to connect to the gateway. Retrying..."
            );
            System.out.println("Gateway found");

            


        } catch (Exception e) {
            System.err.println("Error initializing GatewayService: Gateway not found");
        }
    }

    public List<List<String>> searchWords(String results, int page) throws RemoteException {
        List<String> searchResults;
        System.out.println("Search results: " + results);
        int searchWordCount = results.split(" ").length;
        System.out.println("Search word count: " + searchWordCount);

        if (searchWordCount > 1) {
            searchResults = gateway.searchCommonWords(List.of(results.split(" ")), page);
        } else {
            searchResults = gateway.searchResults(results, page);
        }
        

        List<List<String>> resultsList = new ArrayList<>();

        for (String result : searchResults) {
            String[] parts = result.split("\\|", 3); // Split into two parts: URL and title
            if (parts.length >= 1) {
                String title = parts[0];
                String url = parts[1];
                String text = parts[2];
                
                List<String> entry = new ArrayList<>();
                entry.add(title);
                entry.add(url);
                entry.add(text);
                resultsList.add(entry);
            }
        }

        
        sendStatsToClients();
        
        return resultsList;
    }

    public List<SimpleEntry<String,String>> searchCommonWords(List<String> results, int page) throws RemoteException {
        List<String> searchResults = gateway.searchCommonWords(results, page);

        //List<SimpleEntry<String, String>> resultsList = new ArrayList<>();

        List<SimpleEntry<String, String>> resultsList = new ArrayList<>();
        for (String result : searchResults) {
            String[] parts = result.split("\\|", 2); // Split into two parts: URL and title
            if (parts.length == 2) {
                String title = parts[0];
                String url = parts[1];
                resultsList.add(new SimpleEntry<>(title, url)); // Add as a Pair
            }
        }
        sendStatsToClients();

        
        return resultsList;
    }

    public List<String> searchLinks(String results, int page) throws RemoteException {
        List<String> searchResults = gateway.searchLinks(results, page);

        //List<SimpleEntry<String, String>> resultsList = new ArrayList<>();

        
        return searchResults;
    }

    public void addUrl(String url) throws RemoteException {
        gateway.putNewPriorityUrl(url);
        // notifyClients("url added");
    }

    public void clearQueue() throws RemoteException {
        gateway.clearQueue();
        // notifyClients("queue cleared");
    }


    public void notifyClients(String message) throws RemoteException{
        
        notificationService.sendNotification(message);
    }

    public void sendStatsToClients() throws RemoteException{
        // notificationService.sendNotification(gateway.getTop10WordCount().toString());
        // notificationService.sendNotification(gateway.getAverageResponseTime().toString());
        List<String> top10WordCount = gateway.getTop10WordCount();

        

        if (!top10WordCount.isEmpty()) notificationService.sendNotification(top10WordCount);
        
    
        
        List<String> allStats = gateway.getAllStats();
        if (allStats != null) {
            allStats.add(0, "ALL_STATS");
            notificationService.sendNotification(allStats);
        }


    }


    private boolean setGatewaySettings() throws Exception {
        String filePath = ".property_file"; // Path to the property file
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Read the first line
            if (line != null && line.startsWith("gateway:")) {
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    this.gatewayIP = parts[1]; // Extract the IP
                    this.gatewayPort = Integer.parseInt(parts[2]); // Extract the port
                    System.out.println("Gateway settings loaded: IP = " + gatewayIP + ", Port = " + gatewayPort);
                    return true;
                }
            }
            System.err.println("Invalid format in .property_file. Expected format: gateway:<IP>:<PORT>");
        } catch (IOException e) {
            System.err.println("Error reading .property_file: " + e.getMessage());
        }
        return false; // Return false if the settings could not be loaded
    }


    private <T> T retryRemoteCall(Callable<T> remoteCall, String errorMessage) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return remoteCall.call();
            } catch (Exception e) {
                System.out.println(errorMessage);
                
                if (i < MAX_RETRIES - 1) {
                    System.out.println("Retrying...");
                    Thread.sleep(3000); // Wait before retrying
                } else {
                    throw e; // Rethrow the exception if max retries are reached
                }
            }
        }
        return null; // This line will never be reached
    }
}

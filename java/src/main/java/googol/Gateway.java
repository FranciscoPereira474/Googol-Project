package googol;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.concurrent.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Gateway extends UnicastRemoteObject implements GatewayInterface {
    private int gatewayPort;
    private String gatewayIP;

    private BlockingDeque<String> urlQueue;
    private String searchResults;

    private static final int MAX_RETRIES = 3;

    private Set<ClientStructure> clients;
    // private List<BarrelInterface> barrels;



    private Set<BarrelStructure> barrels;

    private int barrelCount;
    
    private int DownloaderCount = 0;

    private int lastBarrelIndex;
    
    private Iterator<BarrelStructure> barrelIterator;

    private Set<wordCount> coutingWords;

    private List<wordCount> top10WordCount;

    // @Autowired
    // private NotificationService notificationService;

    // private ClientInterface client;


    public Gateway() throws RemoteException {

        super();

        urlQueue = new LinkedBlockingDeque<>();
        searchResults = new String();

        barrels = new HashSet<BarrelStructure>();
        barrelCount = 0;

        clients = new HashSet<ClientStructure>();
        
        lastBarrelIndex = -1;

        coutingWords = new HashSet<wordCount>();
        top10WordCount = new ArrayList<wordCount>();

        
        
        
    }
    
    // @PostConstruct
    // public void init() {
    public static void main(String[] args) {
        
    
        try{
            Gateway gateway = new Gateway();
            // System.setProperty("java.rmi.server.hostname", "172.20.10.9");

            // Read the IP address and the port from .property_file
            if (!gateway.setGatewaySettings()) {
                System.err.println("Failed to load gateway settings. Exiting...");
                System.exit(1);
            }
            
            
            Registry registry = LocateRegistry.createRegistry(gateway.gatewayPort);
            registry.rebind("gateway", gateway);

        
            System.out.println("Gateway is running...");

            
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
         

    public void putNewUrl(String url) throws java.rmi.RemoteException {
        urlQueue.addLast(url);
        //System.out.println("URL added to queue: " + url);

        // client.printResults("Received string: " + url);
    }

    public void putNewPriorityUrl(String url) throws java.rmi.RemoteException {
        
        urlQueue.addFirst(url);
    }

    public void clearQueue() throws java.rmi.RemoteException {
        
        urlQueue.clear();
        if (urlQueue.isEmpty()) {
            System.out.println("Queue cleared");
        } else {
            System.out.println("Queue not cleared");
        }
    }
    
    public String takeNext() throws java.rmi.RemoteException {
        if (urlQueue.isEmpty()) {
            return null;
        }
        return urlQueue.poll();
    }

    public void subscribeCliente(String ip, int port) throws java.rmi.RemoteException {
        System.out.println("Client subscribed");
        // this.client = client;
        ClientInterface client = null;
        for (int i=0;i<MAX_RETRIES && client == null; i++){
            try{
                client = retryRemoteCall(
                    () -> (ClientInterface) LocateRegistry.getRegistry(ip, port).lookup("client"),
                    "Error connecting to client");
                ClientStructure clientStructure = new ClientStructure(ip, port, client);
                clients.add(clientStructure);
                client.setTop10(top10WordCount.stream().map(w -> w.getWord()).collect(Collectors.toList()));
            }
            catch(Exception e){
                System.out.println("Error connecting to client");
            }
        }

        
    
    }

    public boolean isAlive() throws java.rmi.RemoteException {
        return true;
    }

    public void subscribeBarrel(int port, String IP_adress) throws java.rmi.RemoteException {
        System.out.println("Barrel subscribed with port: " + port + " and IP: " + IP_adress);

        Iterator<BarrelStructure> iterator = barrels.iterator();
        while (iterator.hasNext()) {
            BarrelStructure barrel = iterator.next();
            if (barrel.getPort() == port && barrel.getIP_adress().equals(IP_adress)) {
                System.out.println("Barrel already exists in set");
                //iterator.remove(); // Safely remove the element using the iterator
                barrels.remove(barrel);
                barrelCount--;
                break;
            }
        }

        try {
            BarrelInterface barrel = retryRemoteCall(
                () -> (BarrelInterface) LocateRegistry.getRegistry(IP_adress, port).lookup("barrel"),
                "Error connecting to barrel");
            
            if (barrel == null) {
                System.out.println("Error conecting to barrel");
                return;
            }

            BarrelStructure barrelsStructure = new BarrelStructure(IP_adress, port, barrel);

            

            boolean success = true;

            if (!barrels.isEmpty()) {
                BarrelInterface lastBarrel = barrels.iterator().next().getBarrel();
                
                success = retryRemoteCall(
                    () -> {barrelsStructure.getBarrel().setIndexWords(lastBarrel.getIndexWordsA_M(), lastBarrel.getIndexWordsN_Z()); return Boolean.TRUE;},
                    "Error setting index words");
                for (int i = 0; i < MAX_RETRIES && success == false; i++) {
                    try {
                        success = retryRemoteCall(
                            () -> {barrelsStructure.getBarrel().setIndexWords(lastBarrel.getIndexWordsA_M(), lastBarrel.getIndexWordsN_Z()); return Boolean.TRUE;},
                            "Error setting index words");
                        break;
                    } catch (Exception e) {
                        System.out.println("Error setting index words");
                    }
                }
                
            }
            if (success == true) {
                barrels.add(barrelsStructure);
                System.out.println("Barrel added to set");
                barrelCount++;
                updateBarrelsPartition();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBarrelsPartition(){
        System.out.println("Updating barrels partition");
        System.out.println("Barrel count: " + barrelCount);
        int counter = 0;
        if (barrelCount == 0) {
            System.out.println("No barrels available");
            return;
        }
        if (barrelCount == 1) {
            try {
                barrels.iterator().next().getBarrel().updateBarrelPartition(true, true);
            } catch (RemoteException e) {
                System.out.println("Error updating barrel partition: " + e.getMessage());
            }
            return;
        }
        for (BarrelStructure barrel : barrels) {
            try {
                if (counter % 2 == 0) {
                    barrel.getBarrel().updateBarrelPartition(true, false);
                } else {
                    barrel.getBarrel().updateBarrelPartition(false, true);
                }
                counter++;
            } catch (Exception e) {
                System.out.println("Error updating barrel partition: " + barrel.getIP_adress() + ":" + barrel.getPort());
            }
        }
    }

    private BarrelStructure getBarrelByIndex(int index) {
        Iterator<BarrelStructure> iterator = barrels.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            BarrelStructure barrel = iterator.next();
            if (i == index) {
                return barrel;
            }
            i++;
        }
        return null;
    }

    public List<String> searchResults(String results, int page) throws java.rmi.RemoteException {
        if (barrelCount == 0) {

            return "No barrels available".lines().collect(Collectors.toList());
        }

        if (!verifyAllBarrelsConection()) {
            System.out.println("Barrel not connected");
            updateBarrelsPartition();
            System.out.println("Sending to another barrel...");
            
        }
        

        int barrelIndex = (lastBarrelIndex + 1) % barrelCount;
        lastBarrelIndex = barrelIndex;
        System.out.println("Searching in barrel: " + barrelIndex + " for word: " + results);

        BarrelStructure currentBarrel = getBarrelByIndex(barrelIndex);
        List<String> urls = new ArrayList<String>();

        System.out.println("Barrel IP: " + currentBarrel.getIP_adress() + " Barrel port: " + currentBarrel.getPort());
        
        String indexToUse;
        if (results.charAt(0) >= 'a' && results.charAt(0) <= 'm') {
            indexToUse = "A-M";
        } else {
            indexToUse = "N-Z";
        }
        
        for (int i=0; i<MAX_RETRIES; i++){
            try{
                if (barrelCount > 1){
                    if (!currentBarrel.getBarrel().returnPartitionIndex().equals(indexToUse)){
                        urls = searchResults(results, page);
                        break;

                    }

                }
                urls = new ArrayList<>(currentBarrel.getBarrel().searchUrls(results, page));
                break;
            }
            catch(Exception e){
                
                
                if (!verifyBarrelConection(getBarrelByIndex(barrelIndex))){
                    
                    barrels.remove(currentBarrel); 
                    barrelCount--;
                    searchResults(results, page);
                }
            }
        }   
        

        wordCount word = coutingWords.stream().filter(w -> w.getWord().equals(results)).findFirst().orElse(null);
        if (word == null){
            word = new wordCount(results, 1);
            coutingWords.add(word);
            System.out.println("Word added: " + word.getWord() + " " + word.getCount());
        }
        else{
            word.setCount(word.getCount() + 1);
        }


        updateTop10WordCount();

        

        return urls;
    }

    public boolean updateTop10WordCount() throws RemoteException {

        for (wordCount word : coutingWords) {
            System.out.println("Word: " + word.getWord() + " Count: " + word.getCount());
        }

        // Se o top 10 for alterado, da print
        List<wordCount> top10 = coutingWords.stream().sorted((w1, w2) -> w2.getCount() - w1.getCount()).limit(10).collect(Collectors.toList());
        if (!top10.equals(top10WordCount)) {
            /*
            for (ClientStructure client : clients) {
                try {
                    client.getClient().setTop10(top10.stream().map(w -> w.getWord()).collect(Collectors.toList()));
                } catch (Exception e) {
                    //e.printStackTrace();
                    ClientInterface currentClient = null;
                    for (int i=0;i<MAX_RETRIES; i++){
                        try{
                            currentClient = retryRemoteCall(
                                () -> (ClientInterface) LocateRegistry.getRegistry(client.getIP_adress(), client.getPort()).lookup("client"),
                                "Error connecting to client");
                            ClientStructure clientStructure = new ClientStructure(client.getIP_adress(), client.getPort(), currentClient);
                            clients.add(clientStructure);
                            currentClient.setTop10(top10WordCount.stream().map(w -> w.getWord()).collect(Collectors.toList()));
                        }
                        catch(Exception e1){
                            System.out.println("Error connecting to client");
                        }
                    }


                }
            }
                 */
            // Atualiza o top 10
            top10WordCount = top10;
            return true;
        }

        return false;
    }

    private boolean verifyAllBarrelsConection() {
        boolean allConnected = true;
        for (BarrelStructure barrel : barrels) {
            if (!verifyBarrelConection(barrel)) {
                removeBarrel(barrel);
                return false;
            }
        }
        return allConnected;
    }
    
    private boolean verifyBarrelConection(BarrelStructure barrel) {

        for (int i = 0; i < 3; i++) {
            try {
                BarrelInterface barrelInterface = (BarrelInterface) LocateRegistry.getRegistry(barrel.getIP_adress(), barrel.getPort()).lookup("barrel");
                barrel.setBarrel(barrelInterface);
                System.out.println("Reconnected to barrel: " + barrel.getIP_adress() + ":" + barrel.getPort());
                return true;
            } catch (Exception e) {
                System.out.println("Error connecting to barrel: " + barrel.getIP_adress() + ":" + barrel.getPort());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }
    
    public void connectBarrel(BarrelStructure barrel) throws java.rmi.RemoteException {
        // find the given barrel in the linked list
        BarrelStructure currentBarrel = null;

        

        for (BarrelStructure current : barrels){
            if (current.getIP_adress().equals(barrel.getIP_adress()) && current.getPort() == barrel.getPort()){
                currentBarrel = current;
            }
        }

        for (int i=0; i<3; i++){
            try{
                BarrelInterface barrelInterface = (BarrelInterface) LocateRegistry.getRegistry(currentBarrel.getIP_adress(), currentBarrel.getPort()).lookup("barrel");
                currentBarrel.setBarrel(barrelInterface);
                System.out.println("Reconnected to barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
                return;
            }
            catch(Exception e){
                System.out.println("Error connecting to barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
                
                
            }
        }
    }

    public void disconnectBarrel(BarrelStructure barrel) throws java.rmi.RemoteException {
        // find the given barrel in the linked list
        BarrelStructure currentBarrel = null;

        

        for (BarrelStructure current : barrels){
            if (current.getIP_adress().equals(barrel.getIP_adress()) && current.getPort() == barrel.getPort()){
                currentBarrel = current;
            }
        }

        barrels.remove(currentBarrel);
        barrelCount--;
        System.out.println("Disconnected from barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
                
    }

    private void removeBarrel(BarrelStructure barrel) {
        barrels.remove(barrel);
        barrelCount--;
        System.out.println("Removed from barrel: " + barrel.getIP_adress() + ":" + barrel.getPort());
    }

    public void disconnectBarrel(String ip, int port) throws java.rmi.RemoteException{
        for (BarrelStructure currentBarrel : barrels){
            if (currentBarrel.getIP_adress().equals(ip) && currentBarrel.getPort() == port){
                barrels.remove(currentBarrel);
                barrelCount--;
                System.out.println("Disconnected from barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
                return;
            }
        }
    }

    public Set<BarrelStructure> getBarrels() throws java.rmi.RemoteException {
        return barrels;
    }

    public int getBarrelCount() throws java.rmi.RemoteException {
        return barrelCount;
    }

    public List<String> searchLinks(String link, int page) throws java.rmi.RemoteException {

        if (barrelCount == 0) {
            return "No barrels available".lines().collect(Collectors.toList());
        }
        int barrelIndex = (lastBarrelIndex + 1) % barrelCount;
        lastBarrelIndex = barrelIndex;
        System.out.println("Searching in barrel: " + barrelIndex);

        BarrelStructure currentBarrel = getBarrelByIndex(barrelIndex);
        List<String> urlsList = currentBarrel.getBarrel().searchLinks(link, page);

        System.out.println(urlsList);

        return urlsList;
    }

    public List<Page> getAllPages() throws java.rmi.RemoteException {
        
        
        int barrelIndex = (lastBarrelIndex + 1) % barrelCount;
        lastBarrelIndex = barrelIndex;
        System.out.println("Searching in barrel: " + barrelIndex);

        BarrelStructure currentBarrel = getBarrelByIndex(barrelIndex);
        

        return currentBarrel.getBarrel().getPages();
    }

    public Map<String, List<Page>> getIndexWordsN_Z() throws java.rmi.RemoteException {
        int barrelIndex = (lastBarrelIndex + 1) % barrelCount;
        lastBarrelIndex = barrelIndex;
        System.out.println("Searching in barrel: " + barrelIndex);

        BarrelStructure currentBarrel = getBarrelByIndex(barrelIndex);
        return currentBarrel.getBarrel().getIndexWordsN_Z();
    }

    public Map<String, List<Page>> getIndexWordsA_M() throws java.rmi.RemoteException {
        int barrelIndex = (lastBarrelIndex + 1) % barrelCount;
        lastBarrelIndex = barrelIndex;
        System.out.println("Searching in barrel: " + barrelIndex);

        BarrelStructure currentBarrel = getBarrelByIndex(barrelIndex);
        return currentBarrel.getBarrel().getIndexWordsA_M();
    }

    public List<String> searchCommonWords(List<String> words, int page) throws java.rmi.RemoteException {

        if (barrelCount == 0) {
            return "No barrels available".lines().collect(Collectors.toList());
        }

        if (barrelCount > 1) {
            BarrelStructure currentBarrel = getBarrelByIndex(0);
            BarrelStructure currentBarrel2 = getBarrelByIndex(1);

            List<String> urlsListA_M = new ArrayList<String>();
            List<String> urlsListN_Z = new ArrayList<String>();


            List<String> wordsA_M = words.stream().filter(w -> w.charAt(0) >= 'a' && w.charAt(0) <= 'm').collect(Collectors.toList());
            System.out.println("Words A-M: " + wordsA_M);

            List<String> wordsN_Z = words.stream().filter(w -> w.charAt(0) >= 'n' && w.charAt(0) <= 'z').collect(Collectors.toList());
            System.out.println("Words N-Z: " + wordsN_Z);

            if (!wordsA_M.isEmpty()) {
                urlsListA_M = currentBarrel.getBarrel().searchCommonWords(wordsA_M, page);

                System.out.println("Searching in barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort() + " for words A-M");
            }
            

            if (!wordsN_Z.isEmpty()){
                urlsListN_Z = currentBarrel2.getBarrel().searchCommonWords(wordsN_Z, page);
                System.out.println("Searching in barrel: " + currentBarrel2.getIP_adress() + ":" + currentBarrel2.getPort() + " for words N-Z");
            } 

            List<String> urlsList = new ArrayList<String>();
            if (!urlsListA_M.isEmpty() && !urlsListN_Z.isEmpty()) {

                urlsList.addAll(urlsListA_M);
                urlsList.retainAll(urlsListN_Z);

                System.out.println(urlsList);

                List<String> urlsPageTitlesA_M = currentBarrel.getBarrel().searchPageByLink(urlsListA_M);
                List<String> urlsPageTitlesN_Z = currentBarrel2.getBarrel().searchPageByLink(urlsListN_Z);
                
                Set<String> urlsTitlesSet = new HashSet<>(urlsPageTitlesA_M);
                urlsTitlesSet.addAll(urlsPageTitlesN_Z);

                List<String> urlsTitlesList = new ArrayList<>(urlsTitlesSet);
                Collections.sort(urlsTitlesList);





                
                return urlsTitlesList;
            }

            if (!urlsListA_M.isEmpty()) {
                urlsList.addAll(urlsListA_M);
                System.out.println("UrlsListA_M: " + urlsListA_M);

                List<String> urlsPageTitlesA_M = currentBarrel.getBarrel().searchPageByLink(urlsListA_M);

                return urlsPageTitlesA_M;
            }
            if (!urlsListN_Z.isEmpty()) {
                urlsList.addAll(urlsListN_Z);
                System.out.println("UrlsListN_Z: " + urlsListN_Z);
                
                List<String> urlsPageTitlesN_Z = currentBarrel2.getBarrel().searchPageByLink(urlsListN_Z);
                
                return urlsPageTitlesN_Z;
            }

            return urlsList;
            
        }


        int barrelIndex = (lastBarrelIndex + 1) % barrelCount;
        lastBarrelIndex = barrelIndex;
        System.out.println("Searching in barrel: " + barrelIndex);

        BarrelStructure currentBarrel = getBarrelByIndex(barrelIndex);
        List<String> urlsList = currentBarrel.getBarrel().searchCommonWords(words, page);
        System.out.println(urlsList);
        List<String> urlsPageTitles = currentBarrel.getBarrel().searchPageByLink(urlsList);
        System.out.println(urlsPageTitles);

        return urlsPageTitles;
    }
    
    public List<String> getBarrelsList() throws java.rmi.RemoteException {
        List<String> barrelsList = new ArrayList<String>();
        if (barrels.isEmpty()) {
            return null;
        }
        for (BarrelStructure barrel : barrels) {
            barrelsList.add(barrel.getIP_adress() + ":" + barrel.getPort());
        }
        return barrelsList;
    }

    public List<String> getAverageResponseTime() throws java.rmi.RemoteException {
        List<String> responseTimes = new ArrayList<String>();

        if (barrels.isEmpty()) {
            return null;
        }

        for (BarrelStructure barrel : barrels) {
            try {
                Float tempFloat = retryRemoteCall(
                    () -> barrel.getBarrel().getAverageResponseTime(),
                    "Error getting average response time from barrel: " + barrel.getIP_adress() + ":" + barrel.getPort());
                String tempString = String.format("%s:%d", barrel.getIP_adress(), barrel.getPort());
                responseTimes.add(tempString + " -> " + tempFloat);
            } catch (Exception e) {
                System.out.println("Error getting average response time from barrel: " + barrel.getIP_adress() + ":" + barrel.getPort());
            }
        }
        
        return responseTimes;
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

    public boolean setBarrelListToUse(String ip, int port) throws java.rmi.RemoteException {
        int i=0;
        for (BarrelStructure barrel : barrels) {
            if (barrel.getIP_adress().equals(ip) && barrel.getPort() == port) {
                barrelIterator = barrels.iterator();
                break;
            }
            i++;

        }
        return i%2 == 0;
    }

    public List<String> getTop10WordCount() throws java.rmi.RemoteException {
        // System.out.println(top10WordCount.stream().map(w -> w.getWord()).collect(Collectors.toList()));

        // top10WordCount = coutingWords.stream().sorted((w1, w2) -> w2.getCount() - w1.getCount()).limit(10).collect(Collectors.toList());
        List<String> finalString = new ArrayList<String>();
        for (wordCount word:top10WordCount){
            System.out.println(word.getWord() + "," + word.getCount());
            String tempString = word.getWord() + "," + word.getCount();
            finalString.add(tempString);
        }
        // return top10WordCount.stream().map(w -> w.getWord()).collect(Collectors.toList());
        return finalString;
    }

    public void increaseDownloaderCount() throws java.rmi.RemoteException {
        DownloaderCount++;
    }

    public void decreaseDownloaderCount() throws java.rmi.RemoteException {
        DownloaderCount--;
    }

    public List<String> getAllStats() throws java.rmi.RemoteException {
        List<String> stats = new ArrayList<String>();
        
        for (BarrelStructure barrel : barrels) {
            String barelID = "[" + barrel.getIP_adress() + ":" + barrel.getPort() + "]?";
            String averageResponseTime = String.valueOf(barrel.getBarrel().getAverageResponseTime());
            String indexBarrelSize = String.valueOf(barrel.getBarrel().getIndexSize());
            String indexPartitionBarrel = String.valueOf(barrel.getBarrel().returnPartitionIndex());
            String finalString = barelID + indexBarrelSize + "?" + averageResponseTime + "?" + indexPartitionBarrel;
            stats.add(finalString);
        }

        stats.add(String.valueOf(DownloaderCount));
        return stats;
    }

    /*
    public void notifyClients(String message) throws java.rmi.RemoteException {
        if (notificationService == null) {
            System.out.println("Notification service is not initialized.");
            return;
        }
        notificationService.sendNotification(message);
    }
    */

}

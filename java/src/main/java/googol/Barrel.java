package googol;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.util.*;

public class Barrel extends UnicastRemoteObject implements BarrelInterface {

    private Map<String, List<Page>> indexWordsA_M;
    private Map<String, List<Page>> indexWordsN_Z;

    private List<Page> allpages;

    private Page nextUrl;

    private final int MAX_RETRIES = 3;

    private GatewayInterface gateway;
    
    private int port;    
    private String ip;

    private int gatewayPort;
    private String gatewayIP;

    private float averageResposnseTime;
    private int responseCount;

    private boolean saveFiles = true;

    // File path for storing the serialized indexes
    private final String INDEX_FILE_A_M = "indexesA_M.ser";
    private final String PAGES_FILE_A_M = "pagesA_M.ser";

    private final String INDEX_FILE_N_Z = "indexesN_Z.ser";
    private final String PAGES_FILE_N_Z = "pagesN_Z.ser";

    private boolean useA_M = true;
    private boolean useN_Z = true;

    public Barrel() throws RemoteException {
        super();
        averageResposnseTime = 0;
    }

    public static void main(String[] args) throws Exception {
        Barrel barrel = new Barrel();

        if (!barrel.setGatewaySettings()) {
            System.err.println("Gateway settings could not be loaded. Exiting...");
            System.exit(1);
        }

        try {
            // Lógica atual do método main
            barrel.port = Integer.parseInt(args[0]);
            barrel.ip = args[1];

            barrel.gateway = barrel.retryRemoteCall(
                () -> (GatewayInterface) Naming.lookup("rmi://" + barrel.gatewayIP + ":" + barrel.gatewayPort + "/gateway"),
                3,
                "Error connecting to gateway"
            );
            
            /* 
            boolean succeededA_M = false;
            boolean succeededN_Z = false;

            File fileA_Z = new File(barrel.INDEX_FILE_A_M);
            File fileN_Z = new File(barrel.INDEX_FILE_N_Z);
            if (fileA_Z.exists() && fileN_Z.exists()) {
                succeededA_M = barrel.retryRemoteCall(
                    () -> { barrel.loadIndexes(barrel.INDEX_FILE_A_M); return Boolean.TRUE; },
                    3,
                    "Error loading indexes"
                );
                succeededN_Z = barrel.retryRemoteCall(
                    () -> { barrel.loadIndexes(barrel.INDEX_FILE_N_Z); return Boolean.TRUE; },
                    3,
                    "Error loading indexes"
                );

            } else {
                System.out.println("No indexes file found, starting with empty indexes.");
            }

            if (succeededA_M && succeededN_Z) {
                File allPagesFileA_M = new File(barrel.PAGES_FILE_A_M);
                File allPagesFileN_Z = new File(barrel.PAGES_FILE_N_Z);
                if (allPagesFileA_M.exists() && allPagesFileN_Z.exists()) {
                    succeededA_M = barrel.retryRemoteCall(
                        () -> { barrel.loadAllPages(barrel.PAGES_FILE_A_M); return Boolean.TRUE; },
                        3,
                        "Error loading all pages"
                    );
                    succeededN_Z = barrel.retryRemoteCall(
                        () -> { barrel.loadAllPages(barrel.PAGES_FILE_N_Z); return Boolean.TRUE; },
                        3,
                        "Error loading all pages"
                    );
                } else {
                    System.out.println("No all pages file found, starting with empty all pages list.");
                }
            }

            if (!succeededA_M && barrel.retryRemoteCall(() -> barrel.gateway.getBarrelCount(), 3, "Error connecting to gateway") > 0) {
                barrel.allpages = barrel.gateway.getAllPages();
                barrel.indexWordsA_M = barrel.gateway.getIndexWordsA_M();
            }

            if (!succeededN_Z && barrel.retryRemoteCall(() -> barrel.gateway.getBarrelCount(), 3, "Error connecting to gateway") > 0) {
                barrel.allpages = barrel.gateway.getAllPages();
                barrel.indexWordsN_Z = barrel.gateway.getIndexWordsN_Z();
            }

            if (!succeededA_M && !succeededN_Z) {
                barrel.allpages = new ArrayList<>();
                barrel.indexWordsA_M = new HashMap<>();
                barrel.indexWordsN_Z = new HashMap<>();
            }
            */
            barrel.allpages = new ArrayList<>();
            barrel.indexWordsA_M = new HashMap<>();
            barrel.indexWordsN_Z = new HashMap<>();

            

            
            
            Registry registry = LocateRegistry.createRegistry(barrel.port);
            registry.rebind("barrel", barrel);

            barrel.retryRemoteCall(
                () -> {
                    barrel.gateway.subscribeBarrel(barrel.port, barrel.ip);
                    return null;
                },
                3,
                "Error connecting to gateway"
            );
            if (barrel.gateway.getBarrelCount() > 1) {
                    
                if (barrel.gateway.setBarrelListToUse(barrel.ip, barrel.port)) {
                    barrel.useA_M = true;
                    barrel.useN_Z = false;
                } else {
                    barrel.useA_M = false;
                    barrel.useN_Z = true;
                }
            }

            /*
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (barrel.useA_M) {
                            barrel.saveAllPages(barrel.PAGES_FILE_A_M);
                            barrel.saveIndexes(barrel.INDEX_FILE_A_M);
                        }
                        if (barrel.useN_Z) {
                            barrel.saveAllPages(barrel.PAGES_FILE_N_Z);
                            barrel.saveIndexes(barrel.INDEX_FILE_N_Z);
                        }
                        System.out.println("Indexes and all pages saved successfully.");
                    
                    } catch (Exception e) {
                        System.err.println("Error saving indexes: " + e.getMessage());
                    }
            }));
             */

            System.out.println("Barrel is running...");
            while(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    // Method to save the allpages list to a file.
    public void saveAllPages(String filePath) throws Exception {
        
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(allpages);
            System.out.println("All pages saved successfully to " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving all pages: " + e.getMessage());
            
        }
        
    }

    
    public void loadAllPages(String filePath) throws Exception {
        
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            allpages = retryRemoteCall(
                () -> (List<Page>) in.readObject(),
                3,
                "Error loading all pages"
            );
            System.out.println("All pages loaded successfully from " + filePath);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error on the ObjectInputStream: " + e.getMessage() + "\nStrarting with empty all pages list.");
            allpages = new ArrayList<>();

        }
        
    }

    // Method to save both indexes to a file.
    public void saveIndexes(String filePath) throws Exception{
        
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            retryRemoteCall(() -> { out.writeObject(indexWordsA_M); return Boolean.TRUE; }, MAX_RETRIES, "Error saving indexes");
            retryRemoteCall(() ->{ out.writeObject(indexWordsN_Z); return Boolean.TRUE; }, MAX_RETRIES, "Error saving indexes");
            System.out.println("Indexes saved successfully to " + filePath);
            return;
        } catch (IOException e) {
            System.err.println("Error saving indexes: " + e.getMessage());
            
        }
        
    }


    public void loadIndexes(String filePath) throws Exception {
        
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            
            indexWordsA_M = retryRemoteCall(
                () -> (Map<String, List<Page>>) in.readObject(),
                3,
                "Error loading indexes"
            );
            indexWordsN_Z = retryRemoteCall(
                () -> (Map<String, List<Page>>) in.readObject(),
                3,
                "Error loading indexes"
            );
            System.out.println("Indexes loaded successfully from " + filePath);
            return;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error on the ObjectInputStream: " + e.getMessage() + "Starting with empty indexes.");
            indexWordsA_M = new HashMap<>();
            indexWordsN_Z = new HashMap<>();
            
        }
        
    }

    public List<String> searchUrls(String word, int page) throws java.rmi.RemoteException {
        word = word.toLowerCase();
        Map<String, List<Page>> indexWords = indexWordsA_M;
        if (word.charAt(0) >= 'N') {
            indexWords = indexWordsN_Z;
        }
        List<String> urls = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        if (indexWords.containsKey(word)) {
            List<Page> urlsSet = indexWords.get(word);
            int i = 0;


            for (Page url : urlsSet) {
                if (page == -1) {
                    // System.out.println("Word: " + word + " Url: " + url.getUrl() + " Refered count: " + url.getReferedLinksCount());
                    String tempStr = url.getUrl();
                    urls.add(tempStr);
                    continue;
                }
                if (i >= page * 10 && i < (page + 1) * 10) {
                    
                    String someText = url.getText().subSequence(0, Math.min(url.getText().length(), 200)).toString();
                    String tempStr = url.getPageTitle() + "|" + url.getUrl() + "|" + someText;
                    urls.add(tempStr);

                    
                    // System.out.println("Word: " + word + " Url: " + url.getUrl() + " Refered count: " + url.getReferedLinksCount() + "Content words: " + someText);
                }
                i++;
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        averageResposnseTime = (averageResposnseTime * responseCount + duration) / (responseCount + 1);
        responseCount++;
        System.out.println("Average response time: " + averageResposnseTime + "ms");
        return urls;
    }
    
    public List<String> searchCommonWords(List<String> words, int page) throws java.rmi.RemoteException {
        List<String> urls = null;
        for (String word : words) {
            List<String> tempUrls = searchUrls(word, -1);
            if (urls == null) {
                urls = tempUrls;
            } else {
                urls.retainAll(tempUrls);
            }
        }
        if (urls == null) {
            return null;
        }
        System.out.println(urls);
        return urls;
    }

    public List<String> searchPageByLink(List<String> links) throws java.rmi.RemoteException {
        List<String> pageTitleStrings = new ArrayList<>();
        for (String link : links) {
            for (Page page : allpages) {
                if (page.getUrl().equals(link)) {
                    pageTitleStrings.add(page.getPageTitle() + "|" + page.getUrl());
                    break;
                }
            }
        }
        return pageTitleStrings;
    }

    public Boolean isNextUrlNull() throws java.rmi.RemoteException {
        return nextUrl == null;
    }

    public void updateBarrelPartition(boolean useA_M, boolean useN_Z) throws java.rmi.RemoteException {
        this.useA_M = useA_M;
        this.useN_Z = useN_Z;
    }

    public void putNew(String url, String title, List<String> referedLinks, String text) throws java.rmi.RemoteException {

        List<String> words = extractWords(text);

        Page newPage = new Page(url, title, referedLinks, words, text);
        
        Page tempUrl = null;
        if ((url.equals("empty")) && nextUrl != null) {
            tempUrl = nextUrl;
            nextUrl = null;
            
            System.out.println("Adding dummy page\n");
        }
        else{
            if (nextUrl == null) {
                nextUrl = newPage;
                return;
            }
            if (nextUrl.getUrl().equals(newPage.getUrl())) {
                return;
            }
            tempUrl = nextUrl;
            nextUrl = newPage;
        }
        

        for (Page page : allpages) {
            if (page.getUrl().equals(tempUrl.getUrl())) {
                page.setPage(tempUrl.getUrl(), tempUrl.getPageTitle(), tempUrl.getReferedLinks(), tempUrl.getContentWords(), tempUrl.getText());
                tempUrl = page;
                break;
            }
        }

        List<String> newWords = tempUrl.getContentWords();

        System.out.println("Adding new page: " + tempUrl.getUrl() + " with title: " + tempUrl.getPageTitle());
        for (String word : newWords) {

            word = word.toLowerCase();
            System.out.println("Word: " + word + " Url: " + tempUrl.getUrl());
            Map<String, List<Page>> indexWords = indexWordsA_M;
            if (word.charAt(0) >= 'N') {
                indexWords = indexWordsN_Z;
            } 

            List<Page> givenPages = indexWords.get(word);
            if (givenPages == null) {
                givenPages = new ArrayList<>();
            }
            boolean flag = false;
            for (Page page : givenPages) {
                if (page.isEquals(tempUrl)) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                continue;
            }
            if (givenPages.contains(tempUrl)) {
                continue;
            }

            List<Page> pages = indexWords.getOrDefault(word, new ArrayList<>());
            flag = false;
            Page tempPage = null;
            for (Page page : pages) {
                if (page.isEquals(tempUrl)) {
                    tempPage = page;
                    flag = true;
                    break;
                }
            }
            if (flag) {
                tempPage.setPage(tempUrl.getUrl(), tempUrl.getPageTitle(), tempUrl.getReferedLinks(), tempUrl.getContentWords(), tempUrl.getText());
                pages.add(tempPage);
                indexWords.put(word, pages);
                continue;
            }
            pages.add(tempUrl);
            indexWords.put(word, pages);
            // System.out.println("Word: " + word + " Url: " + tempUrl.getUrl());
        }
        System.out.println();
        
        for (String currentUrl : tempUrl.getReferedLinks()) {
            boolean foundPage = false;
            for (Page page : allpages) {
                if (page.getUrl().equals(currentUrl) && !page.getUrl().equals(tempUrl.getUrl())) {
                    page.incrementReferedLinksCount();
                    page.addLink(tempUrl.getUrl());
                    foundPage = true;
                    break;
                }
            }
            if (!foundPage) {
                Page newReferedPage = new Page(currentUrl, "", new ArrayList<String>(), new ArrayList<String>(), null);
                newReferedPage.incrementReferedLinksCount();
                newReferedPage.addLink(tempUrl.getUrl());
                allpages.add(newReferedPage);
            }
        }

        for (Map.Entry<String, List<Page>> entry : indexWordsA_M.entrySet()) {
            List<Page> pages = entry.getValue();
            pages.sort((p1, p2) -> Integer.compare(p2.getReferedLinksCount(), p1.getReferedLinksCount()));
            entry.setValue(pages);
        }

        for (Map.Entry<String, List<Page>> entry : indexWordsN_Z.entrySet()) {
            List<Page> pages = entry.getValue();
            pages.sort((p1, p2) -> Integer.compare(p2.getReferedLinksCount(), p1.getReferedLinksCount()));
            entry.setValue(pages);
        }
        
    
    }

    private List<String> extractWords(String text) {
        List<String> words = new ArrayList<>();
        // O padrão abaixo captura letras e números de qualquer idioma
        Pattern pattern = Pattern.compile("[\\p{L}\\p{N}]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return words;
    }

    public void setIndexWords(Map<String, List<Page>> indexWordsA_M, Map<String, List<Page>> indexWordsN_Z) throws java.rmi.RemoteException {
        this.indexWordsA_M = indexWordsA_M;
        this.indexWordsN_Z = indexWordsN_Z;
    }

    public Map<String, List<Page>> getIndexWordsA_M() throws java.rmi.RemoteException {
        return indexWordsA_M;
    }

    public Map<String, List<Page>> getIndexWordsN_Z() throws java.rmi.RemoteException {
        return indexWordsN_Z;
    }

    public List<String> getAllPages() throws java.rmi.RemoteException {
        List<String> pages = new ArrayList<>();
        // List all Pages and the referred links
        for (Map.Entry<String, List<Page>> entry : indexWordsA_M.entrySet()) {
            String tempStr = "Word: " + entry.getKey();
            for (Page page : entry.getValue()) {
                tempStr += " Url: " + page.getUrl() + " Refered count: " + page.getReferedLinksCount() + "\n";
            }
            pages.add(tempStr);
        }
        for (Map.Entry<String, List<Page>> entry : indexWordsN_Z.entrySet()) {
            String tempStr = "Word: " + entry.getKey();
            for (Page page : entry.getValue()) {
                tempStr += " Url: " + page.getUrl() + " Refered count: " + page.getReferedLinksCount() + "\n";
            }
            pages.add(tempStr);
        }
        return pages;
    }

    public List<String> searchLinks(String link, int page) throws java.rmi.RemoteException {
        
        List<String> links = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Procurar a página correspondente ao link
        Page targetPage = null;
        if (allpages != null) {
            for (Page pageObj : allpages) {
                if (pageObj.getUrl().equals(link)) {
                    targetPage = pageObj;
                    break;
                }
            }
        }

        if (targetPage != null) {
            List<String> allLinks = targetPage.getLinks(); // Obter todos os links referenciados pela página
            int startIndex = page * 10; // Índice inicial para a paginação
            int endIndex = Math.min(startIndex + 10, allLinks.size()); // Índice final para a paginação

            // Adicionar apenas os links da página atual

            int i=0;
            for (String linkUrl : allLinks) {
                if (i >= startIndex && i < endIndex) {
                    String tempStr = linkUrl;
                    links.add(tempStr);
                }
                i++;
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        averageResposnseTime = (averageResposnseTime * responseCount + duration) / (responseCount + 1);
        responseCount++;

        return links;
    }

    private <T> T retryRemoteCall(Callable<T> remoteCall, int maxRetries, String errorMessage) throws Exception {
        for (int i = 0; i < maxRetries; i++) {
            try {
                return remoteCall.call();
            } catch (Exception e) {
                System.out.println(errorMessage);
                if (i < maxRetries - 1) {
                    System.out.println("Retrying...");
                    Thread.sleep(3000 * (i+1)); // Wait before retrying
                } else {
                    throw e; // Rethrow the exception if max retries are reached
                }
            }
        }
        return null; // This line will never be reached
    }

    public void notifyDownloaderShutdown() throws RemoteException {
        System.out.println("Downloader has been shut down.");
        System.exit(1);
    }

    public List<Page> getPages() throws java.rmi.RemoteException{
        return allpages;
    }

    public float getAverageResponseTime() throws java.rmi.RemoteException{ 
        return averageResposnseTime;
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

    public float getIndexSize() throws java.rmi.RemoteException {
        
        return indexWordsA_M.size() + indexWordsN_Z.size();
    }

    public String returnPartitionIndex() throws java.rmi.RemoteException {
        if (useA_M && useN_Z) {
            return "A-Z";
        } else if (useA_M) {
            return "A-M";
        } else if (useN_Z) {
            return "N-Z";
        } else {
            return null;
        }
    }

}

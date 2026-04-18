package googol;

import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


public class Downloader extends UnicastRemoteObject implements DownloaderInterface {
    private List<BarrelStructure> barrels;
    private GatewayInterface gateway;
    private int n_barrels = -1;

    private int MAX_RETRIES = 3;

    private int gatewayPort;
    private String gatewayIP;

    private Set<String> urlsSeen;

    public Downloader() throws Exception {
        super();
        barrels = new ArrayList<BarrelStructure>();
        urlsSeen = new HashSet<String>();
    }

    public static void main(String[] args) throws Exception {
        Downloader downloader = new Downloader();
        if (!downloader.setGatewaySettings()) {
            System.out.println("Failed to load gateway settings. Exiting...");
            System.exit(1);
        }
        try {
            downloader.gateway = downloader.retryRemoteCall(
                () -> (GatewayInterface) LocateRegistry.getRegistry(downloader.gatewayIP, downloader.gatewayPort).lookup("gateway"),
                "Error connecting to gateway"
            );
            downloader.gateway.increaseDownloaderCount();
        } catch (Exception e) {
            System.out.println("Failed to connect to the gateway after retries. Exiting...");
            downloader.notifyBarrelsShutdown();
            System.exit(1); // Exit the program with an error code
        }

        if (downloader.gateway == null) {
            System.out.println("Gateway is null. Exiting...");
            downloader.notifyBarrelsShutdown();
            System.exit(1);
        }

        downloader.connectBarrel();
        try {
            downloader.n_barrels = downloader.retryRemoteCall(
                () -> downloader.gateway.getBarrelCount(),
                "Error connecting to gateway"
            );
            System.out.println("Number of barrels: " + downloader.n_barrels);
        } catch (Exception e) {
            downloader.n_barrels = -1; // Assign a default value on failure
        }

        while (downloader.n_barrels == -1) {
            System.out.println("Error connecting to gateway");

            try {
                downloader.gateway = downloader.retryRemoteCall(
                    () -> (GatewayInterface) LocateRegistry.getRegistry(1099).lookup("gateway"),
                    "Error connecting to gateway"
                );
            } catch (Exception e) {
                System.out.println("Failed to reconnect to the gateway. Exiting...");
                System.exit(1);
            }

            if (downloader.gateway == null) {
                System.out.println("Gateway is null. Exiting...");
                System.exit(1);
            }

            downloader.connectBarrel();
            downloader.n_barrels = downloader.retryRemoteCall(
                () -> downloader.gateway.getBarrelCount(),
                "Error connecting to gateway"
            );
        }

        System.out.println("Downloader is running...");
        

        // Add a SIGNINT handler to notify barrels before exiting
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            try {
                downloader.gateway.decreaseDownloaderCount();
            } catch (RemoteException e) {
                System.out.println("Error notifying gateway about shutdown");
            }
        }));
        while (true) {

            int nCurrentBarrel = downloader.retryRemoteCall(() -> downloader.gateway.getBarrelCount(), "Error conecting to gateway");
            while (nCurrentBarrel == 0) {
                System.out.println("No barrels available. Retrying...");
                for (int i=0; i<downloader.MAX_RETRIES; i++) {
                    nCurrentBarrel = downloader.retryRemoteCall(() -> downloader.gateway.getBarrelCount(), "Error conecting to gateway");

                    if (nCurrentBarrel != 0) {
                        break;
                    }
                    Thread.sleep(3000 * (i+1)); // Wait before retrying
                    System.out.println("Retrying in " + 3*(i+1) + " seconds...");

                }
                
                
            }

            if (nCurrentBarrel != downloader.n_barrels) {
                downloader.n_barrels = nCurrentBarrel;
                downloader.connectBarrel();
            }

            String url = null;
            
            for (int i = 0; i < 10 && url == null; i++) {
                try{
                    url = downloader.retryRemoteCall(() -> downloader.gateway.takeNext(), "Error getting next URL from gateway");
                } catch (Exception e) {
                    url = null;
                }

                if (url == null) {
                    System.out.println("No URL received. Retrying...");

                    if (!downloader.isNextUrlNull()) {
                        System.out.println("Sending null URL to barrels...");
                        downloader.putUrlNullBarrel();
                        
                    }

                    Thread.sleep(3000); // Wait before retrying
                }
            }

            if (url == null) {
                for (int i = 0; i < downloader.MAX_RETRIES; i++) {
                    try{
                        downloader.retryRemoteCall(() -> { downloader.gateway.isAlive(); return Boolean.TRUE; }, "Error connecting to gateway");
                        break;
                    }
                    catch (Exception e) {
                        System.out.println("Error connecting to gateway, exiting...");
                        downloader.notifyBarrelsShutdown();
                        System.exit(1);
                    }
                    continue;
                    
                }
                System.out.println("No URL received. Retrying...");
            }
            

            


            List<String> urls = new ArrayList<>();

            // Normaliza a URL antes de tentar fazer a conexão, garantindo a codificação correta
            if (url == null) {
                System.out.println("URL é nula, ignorando...");
                
                continue;
            }
            String normalizedUrl = downloader.normalizeUrl(url);

            // Verifica o tipo de conteúdo antes de tentar fazer parsing com Jsoup
            if (normalizedUrl == null || !downloader.isHtmlContent(normalizedUrl)) {
                System.out.println("URL ignorada (não é HTML): " + normalizedUrl);
                continue;
            }

            org.jsoup.nodes.Document doc = downloader.retryRemoteCall(() -> Jsoup.connect(normalizedUrl).get(), "Error downloading: " + normalizedUrl);


            List<String> currentLinks = new ArrayList<>();
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String linkUrl = link.attr("abs:href");

                if (!downloader.urlsSeen.add(linkUrl)){
                    continue;
                }
                
                
                boolean flag = downloader.retryRemoteCall(() -> { downloader.gateway.putNewUrl(linkUrl); return Boolean.TRUE; }, "Error sending link to gateway");
                for (int i = 0; i < downloader.MAX_RETRIES && !flag; i++) {
                    // Try to reconnect to the gateway
                    
                    try{
                        downloader.gateway = downloader.retryRemoteCall(() -> (GatewayInterface) LocateRegistry.getRegistry(1099).lookup("gateway"), "Error connecting to gateway");
                    } catch (Exception e) {
                        System.out.println("Failed to reconnect to the gateway. Exiting...");
                        downloader.notifyBarrelsShutdown();
                        System.exit(1);
                    }
                    flag = downloader.retryRemoteCall(() -> { downloader.gateway.putNewUrl(linkUrl); return Boolean.TRUE; }, "Error sending link to gateway");
                }
                if (!flag) {
                    System.out.println("Error connecting to gateway, exiting...");
                    downloader.notifyBarrelsShutdown();
                    System.exit(1);
                }
                currentLinks.add(linkUrl);
                urls.add(url);
            }

            String title = doc.title();
            System.out.println("Title: " + title);

            doc.select("[style*=display:none]").remove();
            doc.select("[style*=visibility:hidden]").remove();
            doc.select(".hidden").remove();
            String bodyText = doc.body().text();
            
            // Utiliza o novo método de extração de palavras que lida melhor com caracteres Unicode

            // Envia para os barrels
            downloader.putUrlBarrel(normalizedUrl, title, currentLinks, bodyText);
        }
    }

    private Boolean connectBarrel() throws Exception {
        Set<BarrelStructure> tempBarrels = retryRemoteCall(() -> gateway.getBarrels(), "Error getting barrels from gateway");
        
        if (tempBarrels == null) {
            return false;
        }
        barrels.clear();

        for (BarrelStructure barrel : tempBarrels) {
            try {
                BarrelInterface currentBarrel = retryRemoteCall(
                    () -> (BarrelInterface) LocateRegistry.getRegistry(barrel.getIP_adress(), barrel.getPort()).lookup("barrel"),
                    "Error connecting to barrel: " + barrel.getIP_adress() + ":" + barrel.getPort()
                );
                barrels.add(new BarrelStructure(barrel.getIP_adress(), barrel.getPort(), currentBarrel));
                System.out.println(barrel.getIP_adress() + ":" + barrel.getPort());
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private Boolean connectBarrel(BarrelStructure barrel) throws Exception {
        BarrelStructure currentBarrel = null;
        for (BarrelStructure current : barrels) {
            if (current.getIP_adress().equals(barrel.getIP_adress()) && current.getPort() == barrel.getPort()) {
                currentBarrel = current;
            }
        }
        
        final BarrelStructure finalCurrentBarrel = currentBarrel;
        BarrelInterface barrelInterface;
        try{
            barrelInterface = retryRemoteCall(
                () -> (BarrelInterface) LocateRegistry.getRegistry(finalCurrentBarrel.getIP_adress(), finalCurrentBarrel.getPort()).lookup("barrel"), 
                "Error connecting to barrel: " + barrel.getIP_adress() + ":" + barrel.getPort());
        }
        catch (Exception e) {
            return false;
        }
        if (barrelInterface != null) {
            currentBarrel.setBarrel(barrelInterface);
            return true;
        }
        
        boolean flag = retryRemoteCall(() -> {
            gateway.disconnectBarrel(finalCurrentBarrel);
            return true;
        }, "Error disconnecting barrel from gateway");
        

        while (!flag) {
           // Try to reconnect to the gateway
            gateway = retryRemoteCall(() -> (GatewayInterface) LocateRegistry.getRegistry(1099).lookup("gateway"), "Error connecting to gateway");
            flag = retryRemoteCall(() -> { gateway.disconnectBarrel(finalCurrentBarrel); return Boolean.TRUE; }, "Error disconnecting barrel from gateway");

        }
        return false;


    }

    private void putUrlNullBarrel(){
        for (BarrelStructure currentBarrel : barrels) {
            BarrelInterface barrelInterface = currentBarrel.getBarrel();
            System.out.println("Sending null URL to barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
            try {
                barrelInterface.putNew("empty", "", Collections.emptyList(), "");
                System.out.println("Link sent to barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
            } catch (Exception e) {
                System.out.println("Error sending link to barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
            }
        }


    }


    private void putUrlBarrel(String url, String title, List<String> referedLinks, String text) throws Exception {        
        for (BarrelStructure currentBarrel : barrels) {
            BarrelInterface barrelInterface = currentBarrel.getBarrel();
            try {
                barrelInterface.putNew(url, title, referedLinks, text);
                System.out.println("Link sent to barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
            } catch (Exception e) {
                System.out.println("Error sending link to barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
                if (retryRemoteCall( () -> { connectBarrel(currentBarrel); return Boolean.TRUE; }, "Error connecting to barrel")) {
                    putUrlBarrel(url, title, referedLinks, text);
                
                    try{
                        boolean flag = retryRemoteCall(() -> { connectBarrel(currentBarrel); return Boolean.TRUE; }, "Error connecting to barrel");
                        if (flag) putUrlBarrel(url, title, referedLinks, text);
                    }
                    catch (Exception e2) {
                        System.out.println("Error connecting to barrel: " + currentBarrel.getIP_adress() + ":" + currentBarrel.getPort());
                    }

                   
                }
                return;
            }
        }
    }

    /**
     * Método que normaliza a URL, re-encodificando o caminho e garantindo que caracteres especiais sejam tratados.
     */
    private String normalizeUrl(String url) {
        if (url == null) {
            System.out.println("Erro ao normalizar URL: URL é nula");
            return null;
        }
        try {
            URI uri = new URI(url).normalize();
            String scheme = (uri.getScheme() != null) ? uri.getScheme().toLowerCase() : "";
            String host = (uri.getHost() != null) ? uri.getHost().toLowerCase() : "";
            int port = uri.getPort();
            String path = (uri.getPath() != null && !uri.getPath().isEmpty()) ? uri.getPath() : "/";
            if (path.endsWith("/") && !path.equals("/")) {
                path = path.substring(0, path.length() - 1);
            }
            // Reconstroi a URI para garantir a correta codificação de caracteres especiais no caminho
            URI normalized = new URI(scheme, null, host, port, path, uri.getQuery(), null);
            return normalized.toASCIIString();
        } catch (URISyntaxException | NullPointerException e) {
            System.out.println("Erro ao normalizar URL: " + e.getMessage());
            return url;
        }
    }
    
    /**
     * Extrai as palavras de um texto utilizando um padrão que suporta caracteres Unicode,
     * garantindo que palavras com acentuação e caracteres especiais sejam corretamente capturadas.
     */
    private List<String> extractWords(String text) {
        List<String> words = new ArrayList<>();
        // O padrão abaixo captura letras e números de qualquer idioma
        Pattern pattern = Pattern.compile("[\\p{L}\\p{N}]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            words.add(matcher.group());
        }

        // Display all words found
        System.out.println("Palavras encontradas: " + words.size());
        for (String word : words) {
            System.out.println(word);
        }
        return words;
    }


    private <T> T retryRemoteCall(Callable<T> remoteCall, String errorMessage) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return remoteCall.call();
            } catch (Exception e) {
                System.out.println(errorMessage);
                
                if (i < MAX_RETRIES - 1) {
                    System.out.println("Retrying...");
                    Thread.sleep(3000 * i+1); // Wait before retrying
                } else {
                    throw e; // Rethrow the exception if max retries are reached
                }
            }
        }
        return null; // This line will never be reached
    }

    private void notifyBarrelsShutdown() {
        System.out.println("Informando barrels sobre o desligamento do downloader...");
        System.out.println("Barrels: " + barrels.size());
    
        for (BarrelStructure barrel : barrels) {
            try {
                barrel.getBarrel().notifyDownloaderShutdown();
                System.out.println("Notificado: " + barrel.getIP_adress() + ":" + barrel.getPort());
            } catch (Exception e) {
                System.out.println("Erro ao notificar barrel: " + barrel.getIP_adress() + ":" + barrel.getPort());
            }
        }
    }
    
    private boolean isNextUrlNull(){
        for (BarrelStructure barrel : barrels) {
            try {
                if (barrel.getBarrel().isNextUrlNull()) {
                    return true;
                }
            } catch (RemoteException e) {
                System.out.println("Erro ao verificar se o próximo URL é nulo: " + barrel.getIP_adress() + ":" + barrel.getPort());
            }
        }
        return false;
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

    private boolean isHtmlContent(String url) {
        if (!isValidHttpUrl(url)) {
            System.out.println("URL inválida ou não suportada: " + url);
            return false;
        }

        try {
            Connection.Response response = Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(5000) // opcional, para evitar bloqueios longos
                .execute();
            String contentType = response.contentType();
            return contentType != null && contentType.startsWith("text/html");
        } catch (IOException e) {
            System.out.println("Erro ao verificar tipo de conteúdo da URL: " + url);
            return false;
        }
    }

    private boolean isValidHttpUrl(String url) {
        try {
            URL u = URI.create(url).toURL();
            String protocol = u.getProtocol();
            return protocol.equals("http") || protocol.equals("https");
        } catch (IllegalArgumentException | MalformedURLException e) {
            return false;
        }
    }



}

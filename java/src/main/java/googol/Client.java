package googol;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.concurrent.*;

import java.io.*;
import java.util.*;

public class Client extends UnicastRemoteObject implements ClientInterface {

    private static final int MAX_RETRIES = 3; // Maximum number of retries for remote calls

    public Client() throws RemoteException {
        super();
    }

    private List<String> top10 = new ArrayList<String>();

    private GatewayInterface gateway;
    private int gatewayPort;
    private String gatewayIP;

    private String ip;
    private int port;

    public static void main(String[] args) {
        try {
            System.out.println("Cliente iniciando...");


            
            
            // Cria o cliente
            Client client = new Client();
            client.ip = args[0];
            client.port = Integer.parseInt(args[1]);

            if (args.length != 2) {
                System.err.println("Usage: java Client <ip> <port>");
                System.exit(1);
            }

            if (!client.setGatewaySettings()) {
                System.exit(1);
            }


            try {
                client.gateway = (GatewayInterface) client.retryRemoteCall(
                    () -> (GatewayInterface) LocateRegistry.getRegistry(client.gatewayIP, client.gatewayPort).lookup("gateway"),
                    "Error connecting to gateway");
            }
            catch (Exception e) {
                System.exit(1);
            }

    
            // Cria o cliente e o registra
            // System.setProperty("java.rmi.server.hostname", client.ip);
            Registry registry = LocateRegistry.createRegistry(client.port);
            registry.rebind("client", client);


            // Subscreve o cliente no gateway
            client.gateway.subscribeCliente(client.ip, client.port);
    
            Scanner scanner = new Scanner(System.in);
            boolean executando = true;
            while (executando) {
                System.out.println("\n=== Menu ===");
                System.out.println("1 - Inserir links");
                System.out.println("2 - Pesquisar palavras");
                System.out.println("3 - Pesquisar links");
                System.out.println("4 - Estatísiticas");
                System.out.println("5 - Fechar programa");
                System.out.print("Escolha uma opção: ");
                String opcao = scanner.nextLine();

                int page = 0;
                
                List<String> words = new ArrayList<String>();

                List<String> pageResults = new ArrayList<String>();
                
    
                switch (opcao) {
                    case "1":
                        // Inserir links

                        
                        boolean inserirLink = true;
                        while (inserirLink) {
                            if (!client.verifyConnection()) break;

                            System.out.print("Digite um link (ou '0' para voltar ao menu): ");
                            String link = scanner.nextLine();
                            if (link.equalsIgnoreCase("0")) {
                                inserirLink = false;
                            } else if (link.startsWith("http://") || link.startsWith("https://")) {
                                try {
                                    client.retryRemoteCall(() -> {client.gateway.putNewUrl(link); return Boolean.TRUE;},
                                     "Gateway indisponivel. Nao foi possivel adicionar o link."); 
                                } catch (Exception e) {
                                    System.out.println("Erro: a gateway está down.");   
                                }
                                
                                client.gateway.putNewUrl(link);
                                System.out.println("Link adicionado com sucesso!");
                            } else {
                                System.out.println("Link inválido! O link deve iniciar com http:// ou https://");
                            }
                        }
                        break;
    
                    case "2":
                        // Inserir palavras
                        
                        while (true) {
                            if (!client.verifyConnection()) break;

                            System.out.print("Gigite as palavras (ou precione '0' para voltar ao menu)\n");
                            try {
                                client.retryRemoteCall(() -> {client.gateway.isAlive(); return Boolean.TRUE;},
                                 "Gateway indisponivel. Nao foi possivel pesquisar as palavras.");
                            } catch (Exception e) {
                                System.out.println("Erro: a gateway está down.");
                            }
                            String palavra = scanner.nextLine();
                            if(palavra.equals("0")){
                                break;
                            }
                            page = 0;

                            List<String> palavras = Arrays.asList(palavra.split(" "));
                            System.out.println("Palavras inseridas: " + palavras);
                            
                            if (palavra.isEmpty()) {
                                // System.out.println("Palavras inseridas: " + palavras);
                                if (words.size() == 0) {
                                    System.out.println("Nenhuma palavra inserida! Por favor, insira pelo menos uma palavra.");
                                    continue;
                                }
                                else{
                                    if (words.size() == 1) {
                                        pageResults = client.gateway.searchResults(words.get(0), page++);
                                        for (String result : pageResults) {
                                            List<String> splitedResult = Arrays.asList(result.split(" "));
                                            
                                            for (String word : splitedResult) {
                                                System.out.println(word);
                                            }   
                                        }
                                        continue;                                        
                                    }
                                    
                                    pageResults = client.gateway.searchCommonWords(words, page++);
                                    for (String result : pageResults) {
                                        List<String> splitedResult = Arrays.asList(result.split(" "));
                                        
                                        for (String word : splitedResult) {
                                            System.out.println(word);
                                        }
                                        
                                    }
                                    break;

                                }
                            } else {
                                if (palavras.size() == 1) {
                                    pageResults = client.gateway.searchResults(palavra, page++);
                                    for (String result : pageResults) {
                                        List<String> splitedResult = Arrays.asList(result.split(" "));
                                        
                                        for (String word : splitedResult) {
                                            System.out.println(word);
                                        }
                                        
                                    }
                                    words = Collections.singletonList(palavra);
                                    continue;
                                }
                                pageResults = client.gateway.searchCommonWords(palavras, page++);
                                System.out.println("Searching for results for words: " + palavras);
                                System.out.println(pageResults);
                                words = palavras;
                                // System.out.println("Palavra '" + palavra + "' adicionada! Você pode inserir outra ou apenas pressionar Enter para finalizar.");
                            }
                        }
                        break;
    
                    case "3":
                        if (!client.verifyConnection()) break;

                        // Pesquisa de links através de um link fornecido pelo cliente
                        System.out.print("Digite o link para pesquisa: ");
                        String pesquisaLink = scanner.nextLine();
                        try {
                            client.retryRemoteCall(() -> {client.gateway.searchLinks(pesquisaLink, 0); return Boolean.TRUE;},
                             "Gateway indisponivel. Nao foi possivel pesquisar os links.");
                        } catch (Exception e) {
                            System.out.println("Erro: a gateway está down.");
                        }

                        // Exemplo: executa a pesquisa na primeira página (page 0).
                        System.out.println( client.gateway.searchLinks(pesquisaLink, 0));
                        break;
                        
                    case "4":
                        
                        boolean stayMenu = true;
                        while(stayMenu){
                            if (!client.verifyConnection()) break;
                            boolean isactive = client.retryRemoteCall(() -> {client.gateway.isAlive(); return Boolean.TRUE;},
                            "Gateway indisponivel.");
                            if(isactive == false){
                                System.out.println("Gateway indisponivel. Nao foi possivel exibir o top 10.");
                                break;
                            }
                            System.out.println("\n=== Estatísticas ===");
                            System.out.println("1 - Top 10");
                            System.out.println("2 - Lista de Barrels ativos");
                            System.out.println("3 - Tempo de resposta médio");
                            System.out.println("4 - Voltar ao menu");
                            System.out.print("Escolha uma opção: ");
                            String response = scanner.nextLine();

                            switch(response){
                                case "1":
                                    // Display top 10
                                    if (client.top10.size() == 0) {
                                        System.out.println("Nenhum resultado disponível! Por favor, pesquise palavras antes de visualizar o top 10.");
                                    } else {
                                        System.out.println("Top 10:");
                                        System.out.println(client.top10);
                                    }
                                    break;
                                case "2":
                                    // Display active barrels
                                    List<String> activeBarrels = client.retryRemoteCall(() -> client.gateway.getBarrelsList(),
                                        "Gateway indisponivel. Nao foi possivel exibir a lista de barrels ativos.");
                                    
                                    if (activeBarrels == null) System.out.println("Nenhum barrel ativo encontrado.");
                                    else {
                                        System.out.println("Barrels ativos:");
                                        for (String barrel : activeBarrels) {
                                            System.out.println(barrel);
                                        }
                                    }
                                    break;
                                case "3":

                                    break;
                                case "4":
                                    // Return to main menu
                                    stayMenu = false;
                                    break;
                                default:
                                    System.out.println("Opção inválida! Por favor, tente novamente.");
                                    break;
                            }
                        }
                        break;
                        
                        
                    case "5":
                        // Fechar o programa
                        System.out.println("Encerrando o programa...");
                        executando = false;
                        break;
                        
    
                    default:
                        System.out.println("Opção inválida! Por favor, tente novamente.");
                        break;
                }
            }
            scanner.close();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public void printResults(String results) throws java.rmi.RemoteException {
        System.out.println(results);
    } 

    public void printPage(List<String> page) throws java.rmi.RemoteException {
        //TODO: Print the page in a better way, with a decent UI
        System.out.println(page);
    }


    public void printLinks(List<String> links) throws java.rmi.RemoteException {
        System.out.println(links);
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

    public void setTop10(List<String> top10) throws java.rmi.RemoteException {
        this.top10 = top10;
    }
    
    private boolean verifyConnection() throws Exception{
        boolean isactive = false;
        try{
            isactive = retryRemoteCall(() -> {gateway.isAlive(); return Boolean.TRUE;},
            "Gateway indisponivel.");
            return true;
        }catch(Exception e){
            System.out.println("Erro: a gateway está down.");
            
        }
        if(isactive == false){
            System.out.println("Gateway indisponivel. Nao foi possivel adicionar o link.");
            gateway = (GatewayInterface) retryRemoteCall(
                () -> (GatewayInterface) LocateRegistry.getRegistry(1099).lookup("gateway"),
                "Error connecting to gateway");
            retryRemoteCall(() -> {gateway.subscribeCliente("localhost", 1100); return Boolean.TRUE;},
                "Gateway indisponivel. Nao foi possivel adicionar o link.");
                return false;
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

}

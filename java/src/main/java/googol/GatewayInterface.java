package googol;

import java.rmi.*;
import java.util.*;

/**
 * Interface GatewayInterface que define os métodos remotos para a gateway.
 * Esta interface é utilizada para comunicação entre a gateway, clientes e barrels, 
 * utilizando RMI (Remote Method Invocation).
 */
public interface GatewayInterface extends Remote{
    /**
     * Adiciona um novo URL à fila de URLs a serem processados.
     * 
     * @param url URL a ser adicionada à fila.
     * @throws RemoteException Se houver um erro na chamada remota.
     */

    public void putNewUrl(String url) throws java.rmi.RemoteException;


    public void putNewPriorityUrl(String url) throws java.rmi.RemoteException;
    
    public void clearQueue() throws java.rmi.RemoteException;

    /**
     * Adiciona um novo barrel ao gateway.
     * 
     * @param port Porta do barrel.
     * @param IP_adress Endereço IP do barrel.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public void subscribeBarrel(int port, String IP_adress) throws java.rmi.RemoteException;

    /**
     * Adiciona um novo cliente ao gateway.
     * 
     * @param ip Endereço IP do cliente.
     * @param port Porta do cliente.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public void subscribeCliente(String ip, int port) throws java.rmi.RemoteException;

     /**
     * Retorna o conjunto de barrel conectados ao gateway.
     * 
     * @return Conjunto de barrel conectados.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public Set<BarrelStructure> getBarrels() throws java.rmi.RemoteException;
    
    /**
     * Retorna o próximo URL da fila de URLs a ser processado.
     * 
     * @return Próximo URL ou null se a fila estiver vazia.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public String takeNext() throws java.rmi.RemoteException;

    /**
     * Pesquisa por URLs que contenham a string de pesquisa.
     * 
     * @param results String a ser pesquisada.
     * @param page Página da pesquisa.
     * @return Lista de URLs encontrados.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public List<String> searchResults(String results, int page) throws java.rmi.RemoteException;

    /**
     * Retorna o número de barrel conectados ao gateway.
     * 
     * @return Número de barrel conectados.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public int getBarrelCount() throws java.rmi.RemoteException;

    /**
     * Conecta um barrel ao geteway.
     * 
     * @param barrel Estrutura do barrel a ser conectada
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public void connectBarrel(BarrelStructure barrel) throws java.rmi.RemoteException;

    /**
     * Desconecta um barrel do gateway.
     * 
     * @param barrel Estrutura do barrel a ser desconectada.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public void disconnectBarrel(BarrelStructure barrel) throws java.rmi.RemoteException;

    /**
     * Desconecta um barrel do geteway.
     * 
     * @param ip Endereço IP do barrel a ser desconectado.
     * @param port Porta do barrel a ser desconectado.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public void disconnectBarrel(String ip, int port) throws java.rmi.RemoteException;

    /**
     * Obtém a lista de barrel conectados ao gateway.
     * 
     * @return Lista de barrel no formato "IP:Porta".
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public List<String> getBarrelsList() throws java.rmi.RemoteException;

    /**
     * Obtém o tempo médio de resposta de cada barrel.
     * 
     * @return Lista de tempos médios de resposta.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public List<String> getAverageResponseTime() throws java.rmi.RemoteException;

    /**
     * Obtém todas as páginas armazenadas nos barrels.
     * 
     * @return Lista de páginas.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public List<Page> getAllPages() throws java.rmi.RemoteException;

    /**
     * Obtém o índice de palavras de A a M.
     * 
     * @return Mapa de palavras e as páginas correspondentes.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public Map<String, List<Page>> getIndexWordsA_M() throws java.rmi.RemoteException;

    /**
     * Obtém o índice de palavras de N a Z.
     * 
     * @return Mapa de palavras e as páginas correspondentes.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public Map<String, List<Page>> getIndexWordsN_Z() throws java.rmi.RemoteException;

    /**
     * Realiza uma pesquisa de links em um barrel.
     * 
     * @param link Link para pesquisa.
     * @return Lista de links correspondentes.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public List<String> searchLinks(String link, int page) throws java.rmi.RemoteException;

    /**
    * Realiza uma pesquisa de palavras comuns em uma página específica.
     * 
     * @param words Lista de palavras para pesquisa.
     * @param page Página de resultados.
     * @return Lista de URLs correspondentes.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public List<String> searchCommonWords(List<String> words, int page) throws java.rmi.RemoteException;

    /**
     * Verifica se o gateway está ativo.
     * 
     * @return true se o gateway estiver ativo, false caso contrário.
     * @throws RemoteException Se houver um erro na chamada remota.
     */
    public boolean isAlive() throws java.rmi.RemoteException;

    public boolean setBarrelListToUse(String ip, int port) throws java.rmi.RemoteException;

    // public void notifyClients(String message) throws java.rmi.RemoteException;

    public List<String> getTop10WordCount() throws java.rmi.RemoteException;

    public boolean updateTop10WordCount() throws RemoteException;

    public List<String> getAllStats() throws java.rmi.RemoteException;

    public void increaseDownloaderCount() throws java.rmi.RemoteException;

    public void decreaseDownloaderCount() throws java.rmi.RemoteException;
}

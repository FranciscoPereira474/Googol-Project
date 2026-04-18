package googol;

import java.rmi.*;
import java.util.*;

public interface ClientInterface extends Remote{
    
    /**
     * Imprime os resultados recebidos.
     *
     * @param results os resultados a serem impressos
     * @throws RemoteException se ocorrer um erro na comunicação remota
     */
    public void printResults(String results) throws java.rmi.RemoteException;

    /**
     * Imprime a página de resultados.
     *
     * @param page a lista de resultados da página
     * @throws RemoteException se ocorrer um erro na comunicação remota
     */
    public void printPage(List<String> page) throws java.rmi.RemoteException;

    /**
     * Define a lista top10 com os resultados fornecidos.
     *
     * @param top10 a lista com os top10 resultados
     * @throws RemoteException se ocorrer um erro na comunicação remota
     */
    public void setTop10(List<String> top10) throws java.rmi.RemoteException;
    
}

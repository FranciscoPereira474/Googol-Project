package googol;

import java.rmi.*;
import java.util.*;

public interface BarrelInterface extends Remote{


    public List<String> searchUrls(String word, int page) throws java.rmi.RemoteException;
    
    public void putNew(String url, String title, List<String> referedLinks, String text) throws java.rmi.RemoteException;

    public void setIndexWords(Map<String, List<Page>> indexWordsA_M, Map<String, List<Page>> indexWordsN_Z) throws java.rmi.RemoteException;
    
    public Map<String, List<Page>> getIndexWordsA_M() throws java.rmi.RemoteException;

    public Map<String, List<Page>> getIndexWordsN_Z() throws java.rmi.RemoteException;

    public List<String> getAllPages() throws java.rmi.RemoteException;

    public List<String> searchLinks(String link, int page) throws java.rmi.RemoteException;

    public List<String> searchCommonWords(List<String> words, int page) throws java.rmi.RemoteException;

    public void notifyDownloaderShutdown() throws java.rmi.RemoteException;

    public List<Page> getPages() throws java.rmi.RemoteException;

    public float getAverageResponseTime() throws java.rmi.RemoteException;

    public Boolean isNextUrlNull() throws java.rmi.RemoteException;

    public float getIndexSize() throws java.rmi.RemoteException;

    public List<String> searchPageByLink(List<String> links) throws java.rmi.RemoteException;

    public String returnPartitionIndex() throws java.rmi.RemoteException;

    public void updateBarrelPartition(boolean useA_M, boolean useN_Z) throws java.rmi.RemoteException;
}


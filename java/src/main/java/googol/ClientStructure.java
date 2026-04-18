package googol;

import java.io.Serializable;

public class ClientStructure implements Serializable{

    private static final long serialVersionUID = 1L; // Add a unique ID for serialization

    /**
     * O endereço IP do cliente.
     */
    private String IP_adress;

    /**
     * A porta do cliente.
     */
    private int port;
    
    /**
     * O cliente.
     */
    private ClientInterface client;

    /**
     * Construtor.
     *
     * @param IP_adress o endereço IP do cliente
     * @param port a porta do cliente
     * @param client o cliente
     */
    public ClientStructure(String IP_adress, int port, ClientInterface client) {
        super();
        this.IP_adress = IP_adress;
        this.port = port;
        this.client = client;
    }

    /**
     * Retorna o endereço IP do cliente.
     *
     * @return o endereço IP do cliente
     */
    public String getIP_adress() {
        return IP_adress;
    }

    /**
     * Retorna a porta do cliente.
     *
     * @return a porta do cliente
     */
    public int getPort() {
        return port;
    }

    /**
     * Retorna o cliente.
     *
     * @return o cliente
     */
    public ClientInterface getClient() {
        return client;
    }

    /**
     * Define o endereço IP do cliente.
     *
     * @param iP_adress o endereço IP do cliente
     */
    public void setClient(ClientInterface client) {
        this.client = client;
    }
    
}

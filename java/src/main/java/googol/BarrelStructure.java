package googol;

import java.io.Serializable;

public class BarrelStructure implements Serializable {

    private static final long serialVersionUID = 1L; // Add a unique ID for serialization
    
    private String IP_adress;
    private int port;
    
    private BarrelInterface barrel;

    public BarrelStructure(String IP_adress, int port, BarrelInterface barrel) {
        super();
        this.IP_adress = IP_adress;
        this.port = port;
        this.barrel = barrel;
    }

    public String getIP_adress() {
        return IP_adress;
    }

    public int getPort() {
        return port;
    }

    public BarrelInterface getBarrel() {
        return barrel;
    }

    public void setBarrel(BarrelInterface barrel) {
        this.barrel = barrel;
    }
    
}

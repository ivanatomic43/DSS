package rs.ftn.ingzanja.dto;

import java.util.ArrayList;
import java.util.List;

public class CBRDTO {


    /**
     * @return the simptomi
     */
    public List<String> getSimptomi() {
        return simptomi;
    }

    /**
     * @param simptomi the simptomi to set
     */
    public void setSimptomi(List<String> simptomi) {
        this.simptomi = simptomi;
    }

    private String dijagnoza;

    private String terapija;

    private List<String> simptomi;



    /**
     * @return the dijagnoza
     */
    public String getDijagnoza() {
        return dijagnoza;
    }

    /**
     * @param dijagnoza the dijagnoza to set
     */
    public void setDijagnoza(String dijagnoza) {
        this.dijagnoza = dijagnoza;
    }

    /**
     * @return the terapije
     */
    public String getTerapija() {
        return terapija;
    }

    /**
     * @param terapija the terapije to set
     */
    public void setTerapija(String terapija) {
        this.terapija = terapija;
    }





}

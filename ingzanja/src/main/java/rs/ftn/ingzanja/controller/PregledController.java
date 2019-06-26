package rs.ftn.ingzanja.controller;

import com.ugos.jiprolog.engine.*;
import net.bytebuddy.build.BuildLogger;
import org.hibernate.mapping.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ftn.ingzanja.dto.BolestDTO;
import rs.ftn.ingzanja.dto.PregledDTO;
import rs.ftn.ingzanja.model.*;
import rs.ftn.ingzanja.service.PregledServiceImpl;

import java.util.*;

@RestController
@RequestMapping(value = "api/pregled")
@CrossOrigin(origins = "http://localhost:4200")
public class PregledController {

    @Autowired
    PregledServiceImpl service;

    /**
     * Api za novi pregled prvi put
     * @param id - path variable param of pacient
     * @return id of pregled
     */
    @RequestMapping(
            value = "/new/{id}",
            method = RequestMethod.POST
    )
    public ResponseEntity<Long> noviPregled(@PathVariable("id") Long id)
    {

        Pregled pregled = new Pregled();
        Long retVal = service.savePregled(pregled, id);
        if(retVal == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(retVal, HttpStatus.OK);
    }

    /**
     *
     * @param id - identifier from pregled to save
     * @return -1-fail to save
     *         id -success saved
     */
    @RequestMapping(
            value = "/save/{id}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Long> savePregled(@PathVariable("id") Long id, @RequestBody PregledDTO body)
    {
        Pregled p = service.findPregledById(id);
        if(p == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        if(body.getDijagnoza() != null)
            p.setDijagnoza(body.getDijagnoza());

        if(body.getTerapija() != null)
            p.setTerapija(body.getTerapija());

        service.savePregledOnly(p,body);

        return new ResponseEntity<>(id, HttpStatus.OK);
    }

    /**
     * Metoda za dijagnozu potencijalnih bolesti, pomocu prologa
     * @param id - pregleda za koji se vrsi dijagnoza
     * @return retVal - ArrayList-a potencijalnih bolesti
     */
    @RequestMapping(
            value = "diagnoseProlog/{id}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ArrayList<String>> diagnoseProlog(@PathVariable("id") Long id)
    {

        Pregled p = service.findPregledById(id);
        if(p == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);



        Pacient pacient = p.getPacient();

        //PAZITI NA ENUME
        Race rasa = pacient.getRasa();
        Pol pol = pacient.getPol();
        int godiste = pacient.getGodiste();
        godiste=2019-godiste;
        String rasaS;
        String polS;
        int katGod;

        if(pol.equals(Pol.male)){
            polS="male";
        }else{
            polS="female";
        }

        if(rasa.equals(Race.black)){
            rasaS="black";
        } else if (rasa.equals(Race.white)) {
            rasaS="white";
        }else{
            rasaS="hispanic";
        }

        if(godiste <= 20){
            katGod=0;
        }else if(godiste>20 && godiste<=40){
            katGod=1;
        }else if(godiste>40 && godiste <=60){
            katGod=2;
        }else{
            katGod=3;
        }

//        String rasaS="white";
//        String polS="male";
//        int katGod=3;
        ArrayList<String> simptomi = new ArrayList<>();

        for(Simptom s : p.getSimptoms())
        {
            String a=s.getNaziv();
            a.toLowerCase();
            a=a.replace(' ','_');
            simptomi.add(a);
        }
        //pripremljeni simptomi i sve
//        simptomi.add("sharp_chest_pain");
//        simptomi.add("shortness_of_breath");
        String upit="final_sum("+katGod+", "+polS+", "+rasaS+", [";
        for(int i=0;i<simptomi.size();i++) {
            if (i + 1 < simptomi.size()) {
                upit += simptomi.get(i) + ", ";
            }else{
                upit += simptomi.get(i);
            }
        }
        upit+="],B,SUM)";
        float sum=0;
        System.out.println(upit);
        ArrayList<String> retLista = new ArrayList<>();
        ArrayList<BolestVrednost> bovre=new ArrayList<>();
        //PROLOG LOGIKA DA SE IMPLEMENTIRA ZA DOBIJANJE POTENICIJALNE BOLESTI
        JIPEngine engine=new JIPEngine();

        try {
            engine.consultFile("/src/program.pl");
            JIPTermParser parser=engine.getTermParser();
            JIPTerm term=parser.parseTerm(upit);
            JIPQuery query=engine.openSynchronousQuery(term);

            JIPTerm solution;

            while((solution=query.nextSolution()) !=null) {
                System.out.println(solution.toString());

                //ako ima vise varijabli koje su deo upta
                int i=0;
                String s=null;
                for(JIPVariable var: solution.getVariables()) {
                    if(i==0){
                        s=var.getValue().toString();
                        i++;
                    }else {
                        float broj=Float.parseFloat(var.getValue().toString());
                        bovre.add(new BolestVrednost(s, broj));
                        i = 0;
                        sum+=broj;
                    }
                    System.out.println(var.getName()+ "="+var.getValue());
                }
            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        Collections.sort(bovre, new Comparator<BolestVrednost>() {
            @Override
            public int compare(BolestVrednost u1, BolestVrednost u2) {
                return u2.getVrednost().compareTo(u1.getVrednost());
            }
        });

        for(BolestVrednost bv:bovre){
            retLista.add(bv.getBolest());
        }

        return new ResponseEntity<>(retLista, HttpStatus.OK);
    }


    /**
     * Metoda za dijagnozu potencijalnih DIJAGNOSTIKA na osnovu bolesti, pomocu prologa
     * @param obj - param za koji se daju moguce DIJAGNOSTIKE
     * @return retList - ArrayList-a potencijalnih DIJAGNOSTIKA
     */
    @RequestMapping(
            value = "/diagnoseTerapyProlog/{id}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ArrayList<String>> diagnoseTerapyProlog(@RequestBody BolestDTO obj, @PathVariable("id")Long id)
    {
        Pregled p = service.findPregledById(id);
        p.setDijagnoza(obj.getBolest());
        service.savePregled(p,p.getPacient().getId());

        String bolest = obj.getBolest();
        bolest.toLowerCase();
        bolest=bolest.replace(' ','_');
        //prolog logika vracas mi string mogucih terapija
        ArrayList<String> retList = new ArrayList<>();
        JIPEngine engine=new JIPEngine();

        try {
            engine.consultFile("/src/program.pl");
            JIPTermParser parser=engine.getTermParser();
            JIPTerm term=parser.parseTerm("test_for_disease("+bolest+", X).");
            JIPQuery query=engine.openSynchronousQuery(term);

            JIPTerm solution;

            while((solution=query.nextSolution()) !=null) {

                //ako ima vise varijabli koje su deo upta
                for(JIPVariable var: solution.getVariables()) {
                    System.out.println(var.getName()+ "="+var.getValue());
                    String dijagnostika= String.valueOf(var.getValue());
                    //lek=lek.replace('_',' ');
                    retList.add(dijagnostika);
                }
            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }


        return new ResponseEntity<ArrayList<String>>(retList, HttpStatus.OK);
    }

    /**
     * Metoda za vracanje liste lekova za neku bolest
     * @param obj - param za koji se daju moguce terapije
     * @return retList - ArrayList-a potencijalnih terapija
     */
    @RequestMapping(
            value = "/lek",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ArrayList<String>> lekZaBolest(@RequestBody BolestDTO obj)
    {
        String bolest = obj.getBolest();
        bolest.toLowerCase();
        bolest=bolest.replace(' ','_');
        //prolog logika vracas mi string mogucih lekova
        ArrayList<String> retList = new ArrayList<>();

        JIPEngine engine=new JIPEngine();

        try {
            engine.consultFile("/src/program.pl");
            JIPTermParser parser=engine.getTermParser();
            JIPTerm term=parser.parseTerm("treatment_for_disease("+bolest+", X).");
            JIPQuery query=engine.openSynchronousQuery(term);

            JIPTerm solution;

            while((solution=query.nextSolution()) !=null) {

                //ako ima vise varijabli koje su deo upta
                for(JIPVariable var: solution.getVariables()) {
                    //System.out.println(var.getName()+ "="+var.getValue());
                    String lek= String.valueOf(var.getValue());
                    //lek=lek.replace('_',' ');
                    retList.add(lek);
                }
            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        return new ResponseEntity<ArrayList<String>>(retList, HttpStatus.OK);
    }


    @RequestMapping( value = "/simptoms/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ArrayList<String>> simptoms(@PathVariable("id") Long id)
    {
        ArrayList<String> retLista = new ArrayList<>();
        Pregled p = service.findPregledById(id);
        if( p == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Set<Simptom> simptomi = p.getSimptoms();
        if(simptomi == null)
            return new ResponseEntity<>(retLista,HttpStatus.OK);

        for(Simptom simptom : simptomi)
            retLista.add(simptom.getNaziv());

        return new ResponseEntity<>(retLista,HttpStatus.OK);
    }
}
package simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Cellule {	
	public final int NB_UR = 128;
	
	private List<UR> ur;
	private List<Utilisateur> users;
	private HashMap<Utilisateur, Deque<Paquet>> buffersUsers;
	private int nbPaquetTotal;
	private int numero;
	private int timeslotOffset = 0;
	private int latence=0;
	private int iPaquet;
        
	public Cellule(int numero, int timeslotOffset) {	
		this.numero = numero;
		this.ur = new ArrayList<>();
		for(int i = 0; i < NB_UR; i++) {
			this.ur.add(new UR(i, this));
		}
		
		this.users = new ArrayList<>();
		this.nbPaquetTotal = 0;		
		this.timeslotOffset = timeslotOffset;
		this.buffersUsers = new HashMap<>();
                this.iPaquet = 0;
	}
	
        public List<Utilisateur> getUsers() {
		return this.users;
	}
        
	public List<UR> getUr() {
		return this.ur;
	}
	
	private void createListUR() {		
		for(int i = 0; i < NB_UR; i++) {
			this.ur.get(i).setAffectation(null);
		}
	}
	
	public void addPaquetsFromInternet() {
            for(int i = 0; i < users.size(); i++) {
                int nbBits = ThreadLocalRandom.current().nextInt(10, 1000);
                this.addPaquetsFromInternet(users.get(i), nbBits);
            }		
	}
        
        public void addPaquetsFromInternet(Utilisateur util, int nbBits) {
            if(!buffersUsers.containsKey(util)) {
                    buffersUsers.put(util, new LinkedList<>());
            }
            int temp = nbBits;
            while (temp > 100) {
                buffersUsers.get(util).add(new Paquet(this.iPaquet, 100));
                temp -= 100;
                this.iPaquet++;
            }            
            buffersUsers.get(util).add(new Paquet(this.iPaquet, temp));
            this.iPaquet++;
            buffersUsers.put(util, buffersUsers.get(util));	
	}
	
	private Paquet getPaquetActuel(Utilisateur u) {
            Deque<Paquet> bufferUtil = buffersUsers.get(u);
            if(bufferUtil.isEmpty()) {
                    return null;
            }
            if(bufferUtil.getFirst().getNbBits() == 0) {
                    bufferUtil.removeFirst();
                    if(bufferUtil.isEmpty()) {
                        return null;
                    }
            }
            return bufferUtil.getFirst();
	}
        
        public int getNbBitsAEnvoyer(Utilisateur u) {
            int sum = 0;
            Deque<Paquet> bufferUtil = buffersUsers.get(u);
            for(Paquet p : bufferUtil) {
                sum += p.getNbBits();
            }
            return sum;
        }
	
	// Paquet demande une UR libre
	public UR getURlibre() {
            int i = 0;
            while(i < this.ur.size() && !this.ur.get(i).estLibre()) {
                    i++;
            }
            if(i == this.ur.size()) {
                    return null;
            }
            
            UR ur = this.ur.get(i);
            ur.setNbBits(Helper.rndint(1, 7)); // L'UR peut transporter un nombre aléatoire de bits suivant les conditions radios
            return ur;
	}
	
	public void setUtilisateur(Utilisateur user) {
		this.users.add(user);
	}
	
	public void changeTimeslot() {
		createListUR();
		for(int i = 0; i < users.size(); i++) {
			this.users.get(i).clearURrecues();
		}
	}
	
	
	/*public void sendPaquet(Paquet paquetEnvoye) {
		paquetEnvoye.setDebutEnvoie(Simulation.getTemps());
		// TODO : Latence
		paquetEnvoye.setFinEnvoie(Simulation.getTemps());
		System.out.println("Cellule : Paquet envoyé " + paquetEnvoye.toString());
	}*/

    public void envoyerUR(UR ur) {
        Paquet paquetActuel = this.getPaquetActuel(ur.getUtilisateur());
        if(paquetActuel.getDebutEnvoie() == -1) {
            paquetActuel.setDebutEnvoie();
        }
        
        int nbBitsPaquetActuel = paquetActuel.getNbBits();
        if(nbBitsPaquetActuel < ur.getNbBits()) {
            paquetActuel.subNbBits(nbBitsPaquetActuel);
            Helper.print("    UR" + ur.getId() + " > Util" + ur.getUtilisateur().getId() + " Paquet " + paquetActuel.getId() +" envoyé - Délai : " + paquetActuel.getDelai());
            
            // On passe au paquet suivant et on envoi ses bits pour compléter l'UR
            paquetActuel = this.getPaquetActuel(ur.getUtilisateur());
            if(paquetActuel != null) {
                if(paquetActuel.getDebutEnvoie() == -1) {
                    paquetActuel.setDebutEnvoie();
                }
                paquetActuel.subNbBits(ur.getNbBits() - nbBitsPaquetActuel);
            }
        }
        else {
            paquetActuel.subNbBits(ur.getNbBits());
        }
        ur.setAffectation(null);
    }
}

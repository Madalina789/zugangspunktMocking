package zugang;

import zugang.authinfo.Id;
import zugang.database.PersonenRepository;
import zugang.network.Netzwerk;
import zugang.scanner.Scanner;

import java.util.List;

public class ZugangspunktBuilder {
    private Netzwerk net;
    private Scanner scanner;
    private List<Id> berechtigtePersonen;
    private PersonenRepository db;

    public ZugangspunktBuilder addNetwork(Netzwerk net) {
        this.net = net;
        return this;
    }

    public ZugangspunktBuilder addScanner(Scanner scanner) {
        this.scanner = scanner;
        return this;
    }

    public ZugangspunktBuilder addBerechtigtePersonen(List<Id> berechtigtePersonen) {
       this.berechtigtePersonen = berechtigtePersonen;
        return this;
    }

    public ZugangspunktBuilder addDB(PersonenRepository db) {
        this.db = db;
        return this;
    }

    public Zugangspunkt build() {
        return new Zugangspunkt(net, scanner, berechtigtePersonen, db);
    }
}

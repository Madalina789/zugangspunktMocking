package zugang;

import java.util.List;
import zugang.authinfo.Id;
import zugang.database.PersonenRepository;
import zugang.network.Netzwerk;
import zugang.scanner.Scanner;

public class Zugangskontrolle {

  public static void main(String[] args) {
    // Dieser Code funktioniert nicht, da die Implementierungen von Scanner und Netzwerk
    // von einem anderen Team entwickelt werden und noch nicht fertiggestllt sind

    // Der Code soll nur demonstrieren, wie das Programm benutzt werden soll

    Netzwerk net = null;
    Scanner sc1 = null;
    Scanner sc2 = null;
    PersonenRepository db = null;
    List<Id> berechtigtePersonen = List.of();

    Zugangspunkt zp1 = new Zugangspunkt(net, sc1, berechtigtePersonen, db);
    Zugangspunkt zp2 = new Zugangspunkt(net, sc2, berechtigtePersonen, db);

    zp1.start();
    zp2.start();

  }

}

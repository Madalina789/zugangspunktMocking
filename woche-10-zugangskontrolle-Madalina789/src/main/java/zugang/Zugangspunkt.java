package zugang;

import static zugang.network.Action.ENTER;
import static zugang.network.Action.LEAVE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import zugang.authinfo.Id;
import zugang.authinfo.Person;
import zugang.database.PersonenRepository;
import zugang.network.Netzwerk;
import zugang.network.ZugangspunktSyncEvent;
import zugang.scanner.Scanner;
//Fragen: wie soll ci start genau testen?
//Hätte ich meine Tests einfacher gestallten können? Also hätte ich Mockito irgendwo weg lassen können?
public class Zugangspunkt {

  private final Map<Id, Person> personenImBereich = new HashMap<>();

  private final Netzwerk net;
  private final Scanner scanner;
  private final List<Id> berechtigtePersonen;
  private final PersonenRepository db;

  public Zugangspunkt(Netzwerk net, Scanner scanner, List<Id> berechtigtePersonen, PersonenRepository db) {
    this.net = net;
    this.scanner = scanner;
    this.berechtigtePersonen = berechtigtePersonen;
    this.db = db;
  }

  //Anzeichen für Kopplung durch Überspezifikation, doch wie soll ich sonst testen, ob die run Methode korrekt funktiniert? Die Implementierung wird getestet, kann man es auch einfach sein lassen, also run nicht testen? Grund: es werden nur Opertionen auf der Hashmap genutzt und das funktiniert mit 100 Garantie, dann braucht man das nicht mehr testen oder?
  Map<Id, Person> getPersonenImBereich() {
    return personenImBereich;
  }

  //ist das besser? welche probleme gibt es hierbei noch?
  boolean personBefindetSichImGebaeude(Id id) {
    return personenImBereich.containsKey(id);
  }
  class NetzwerkReceive implements Runnable {

    //Schnittstelle
    @Override
    //Kann es sein das SLAP nicht eingehalten wurde?
    public void run() {
      while (true) {//wie lange ist das true? wann hört es auf?
        if (net.readyToReceive()) { //-> true
          ZugangspunktSyncEvent event = net.receiveMessageBlocking();//ZugangspunktSyncEvent Mocken
          Id id = event.id();
          protocolEvent(event, id);
        }
      }
    }
  }

  void protocolEvent(ZugangspunktSyncEvent event, Id id) {
    if (event.action() == LEAVE) {
      personenImBereich.remove(id);
    } else {
      personenImBereich.put(id, db.getPersonForId(id).get());
    }
  }


  public void start() {
    new Thread(new NetzwerkReceive()).start();

    while (true) {
      scanner.waitForPerson();//void
      initiateProcess();
    }
  }

  void initiateProcess() {
    if (!scanner.personWantsToEnter()) {//false zurückgeben
      processLeave();
    } else {
      processEnter();
    }
  }

  private void processLeave() {
    net.getLock();//void
    try {
      if (scanner.scanId()) {
        Id id = scanner.getId();
        scanner.allowLeave();//weshlab wurde das zweimal in diesem code abschnitt aufgerufen?
        personenImBereich.remove(id);//ist das nicht doppelt enthalten(siehe run)
        net.broadcastEvent(new ZugangspunktSyncEvent(LEAVE, id));//void ,kann ignoriert werden
        scanner.allowLeave();//void
      } else {
        scanner.allowAccess(); // timeout beim Scan, nach innen öffnen,void
      }
    } finally {
      net.releaseLock();//void
    }
  }

  private void processEnter() {
    boolean ausweisGelesen = scanner.scanId();
    if (!ausweisGelesen) {
      scanner.rejectAccess("Kein Ausweis gelesen");//void
      return;
    }
    boolean bildAufgenommen = scanner.takePicture();
    if (!bildAufgenommen) {
      scanner.rejectAccess("Kein Foto aufgenommen");
      return;
    }

    Optional<Person> optinalPerson = db.getPersonForId(scanner.getId());
    if (optinalPerson.isEmpty()) {
      scanner.rejectAccess("Keine Informationen zur Person gespeichert");
      return;
    }

    Person person = optinalPerson.get();

    //Personendaten stimmen nicht mit ausweis zusammen oder person befindet sich schon im Gebäude
    if (!person.biometricMatch(scanner.getScannedBiometricData()) || personenImBereich.containsKey(person.id())) {
      scanner.sendAlert();//void
      return;
    }


    if (berechtigtePersonen.contains(person.id())) {
      zugangGestatten(person);
    } else {
      scanner.sendAlert();//void
    }

  }

  private void zugangGestatten(Person person) {
    net.getLock();
    try {

      personenImBereich.put(person.id(), person);//Zustandsänderung
      scanner.allowAccess();//void
      net.broadcastEvent(new ZugangspunktSyncEvent(ENTER, person.id()));//void

    } finally {
      net.releaseLock();
    }
  }

}

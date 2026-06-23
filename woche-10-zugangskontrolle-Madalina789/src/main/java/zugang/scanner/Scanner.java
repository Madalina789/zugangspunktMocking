package zugang.scanner;

import zugang.authinfo.BiometricData;
import zugang.authinfo.Id;

public interface Scanner {

  // Aufruf blockiert, bis eine Person den Bereich betreten
  // oder verlassen will
  void waitForPerson();

  // Nur aufrufen, wenn eine Person da ist
  // true: Person will rein
  // false: Person will raus
  boolean personWantsToEnter();

  // Nur aufrufen, wenn eine Person den Bereich verlassen will
  // Muss immer gestattet werden!
  void allowLeave();

  // Starte den Scanning Prozess
  // gibt true zurück, wenn die Ausweisdaten gelesen wurden
  // gibt false zurück, wenn die Person im Scanner den
  // Vorgang abgebrochen hat oder ein Timeout aufgetreten ist
  boolean scanId();

  // Lädt die Bilddaten in den Verarbeitungsspeicher des Scanners
  // gibt true zurück, wenn die biometrischen Daten korrekt gelesen werden konnten
  // gibt false zurück, wenn kein Bild korrekt aufgenommen werden konnte
  boolean takePicture();


  // Nur aufrufen, wenn takePicture true war!
  // Wirft ansonsten eine IllegalStateException
  BiometricData getScannedBiometricData();

  // Nur aufrufen, wenn scanId() true war!
  // Wirft ansonsten eine IllegalStateException
  Id getId();


  // Zugang gestatten, öffnet die Schleise nach innen
  void allowAccess();

  // Zugang verweigern und Meldung anzeigen
  // öffnet die Schleuse nach außen
  void rejectAccess(String reason);

  // Alarm auslösen
  void sendAlert();



}
package zugang;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import zugang.authinfo.BiometricData;
import zugang.authinfo.Id;
import zugang.authinfo.Person;
import zugang.database.PersonenRepository;
import zugang.network.Action;
import zugang.network.Netzwerk;
import zugang.network.ZugangspunktSyncEvent;
import zugang.scanner.Scanner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ZugangspunktTest1 {
    private Netzwerk net;
    private Scanner scanner;
    private List<Id> berechtigtePersonen;
    private PersonenRepository db;
    private Person person;
    private Id id ;
    private Zugangspunkt zp;

    @BeforeEach
    void setUp() {
        net = mock(Netzwerk.class);
        scanner = mock(Scanner.class);
        db = mock(PersonenRepository.class);

        id = new Id();
        berechtigtePersonen = List.of(id);
        person = mock(Person.class);
        zp= new Zugangspunkt(net,scanner,berechtigtePersonen,db);


    }
    @Test
    @DisplayName("Personen, die sich im Gebäude befindet werden eingetragen")
    void peronenImBereichEinsetzenTestMeins() throws InterruptedException {
        //run
        ZugangspunktSyncEvent event = mock(ZugangspunktSyncEvent.class);
        when(net.readyToReceive()).thenReturn(true);
        when(net.receiveMessageBlocking()).thenReturn(event);
        when(event.id()).thenReturn(id);
        when(event.action()).thenReturn(Action.ENTER);
        when(db.getPersonForId(id)).thenReturn(Optional.of(person));

        Thread thread = new Thread(zp.new NetzwerkReceive());

        thread.start();

        Thread.sleep(100);

        assertTrue(zp.personBefindetSichImGebaeude(id));
//        assertEquals(person,zp.getPersonenImBereich().get(id));
    }

    @Test
    @DisplayName("Personen,die sich abgemeldet haben werden von der Übersicht über alle sich im Gebäude befindenden Personen gelöscht")
    void peronenImBereichLöschenTestMeins() throws InterruptedException {
        //run
        ZugangspunktSyncEvent event = mock(ZugangspunktSyncEvent.class);
        when(net.readyToReceive()).thenReturn(true);
        when(net.receiveMessageBlocking()).thenReturn(event);
        when(event.id()).thenReturn(id);
        when(event.action()).thenReturn(Action.LEAVE);


        Thread thread = new Thread(zp.new NetzwerkReceive());

        thread.start();

        Thread.sleep(100);

        assertFalse(zp.personBefindetSichImGebaeude(id));
    }


    @Test
    @DisplayName("Person will das Gebäude verlassen und ihre ID konnte gescannt werden")
    void processLeaveTest1() {
        //run
        ZugangspunktSyncEvent event = mock(ZugangspunktSyncEvent.class);
        when(net.readyToReceive()).thenReturn(true);
        when(net.receiveMessageBlocking()).thenReturn(event);
        when(event.id()).thenReturn(id);
        when(event.action()).thenReturn(Action.ENTER);
        when(db.getPersonForId(id)).thenReturn(Optional.of(person));

        //initiateProcess
        when(scanner.personWantsToEnter()).thenReturn(false);

        //processLeave
        when(scanner.scanId()).thenReturn(true);
        when(scanner.getId()).thenReturn(id);


        zp.initiateProcess();

        assertFalse(zp.personBefindetSichImGebaeude(id));
        verify(net).getLock();
        verify(net).broadcastEvent(any(ZugangspunktSyncEvent.class));
        verify(scanner,times(2)).allowLeave();
        verify(net).releaseLock();
    }

    @Test
    @DisplayName("Person will das Gebäude verlassen aber ihre ID kann nicht gescannt werden")
    void processLeaveTest2() {
        when(scanner.personWantsToEnter()).thenReturn(false);
        when(scanner.scanId()).thenReturn(false);

        zp.initiateProcess();

        verify(scanner).allowAccess();
        verify(net).releaseLock();
    }

    @Test
    @DisplayName("Person will das Gebäude betreten")
    void processEnterTest() {
        //initiateProcess
        when(scanner.personWantsToEnter()).thenReturn(true);

        //processLeave
        when(scanner.scanId()).thenReturn(true);
        when(scanner.takePicture()).thenReturn(true);
        when(db.getPersonForId(id)).thenReturn(Optional.of(person));
        when(person.id()).thenReturn(id);
        when(scanner.getId()).thenReturn(id);

        BiometricData biometricData = mock(BiometricData.class);
        when(scanner.getScannedBiometricData()).thenReturn(biometricData);
        when(person.biometricMatch(biometricData)).thenReturn(true);


        zp.initiateProcess();

        assertTrue(zp.personBefindetSichImGebaeude(id));
        verify(net).getLock();
        verify(scanner).allowAccess();
        verify(net).broadcastEvent(any(ZugangspunktSyncEvent.class));
        verify(net).releaseLock();
//        assertEquals(person, zp.getPersonenImBereich().get(id));

    }

//    @Test
//    @DisplayName("Eine Person die das Gebäude betreten möchte wird der Eintritt gewährt und ist jetzt physisch drin")
//    void shouldGrantAccessAndBroadcastEvent() {
//
//
//        when(scanner.personWantsToEnter()).thenReturn(true);
//        when(scanner.scanId()).thenReturn(true);
//        when(scanner.takePicture()).thenReturn(true);
//
//        when(scanner.getId()).thenReturn(id);
//
//        BiometricData bio = mock(BiometricData.class);
//        when(scanner.getScannedBiometricData()).thenReturn(bio);
//
//        when(db.getPersonForId(id)).thenReturn(Optional.of(person));
//        when(person.id()).thenReturn(id);
//        when(person.biometricMatch(bio)).thenReturn(true);
//
//
//        zp.initiateProcess();
//
//        //ist das hier Überspezifikation durch Assertions? Man kennt praktisch alle Methoden
//        verify(net).getLock();
//        verify(scanner).allowAccess();
//        verify(net).broadcastEvent(any(ZugangspunktSyncEvent.class));
//        verify(net).releaseLock();
//
//        Map<Id, Person> map = zp.getPersonenImBereich();
//        assertTrue(map.containsKey(id));
////        assertEquals(person, map.get(id));
//    }

    @Test
    @DisplayName("Der Ausweis konnte nicht gelesen werden oder es gab keinen zum Lesen")
    void ausweisLesenSchlaegtFehlTest() {
        when(scanner.personWantsToEnter()).thenReturn(true);
        when(scanner.scanId()).thenReturn(false);

        zp.initiateProcess();

        verify(scanner).rejectAccess("Kein Ausweis gelesen");
    }

    @Test
    @DisplayName("Es konnte kein Bild korrekt aufgenommen werden")
    void bildaufnahmeSchlaegtFehlTest() {
        when(scanner.personWantsToEnter()).thenReturn(true);
        when(scanner.scanId()).thenReturn(true);

        when(scanner.takePicture()).thenReturn(false);

        zp.initiateProcess();

        verify(scanner).rejectAccess("Kein Foto aufgenommen");
    }

    @Test
    @DisplayName("Die Person wurde nicht in der datenbank gefunden")
    void personAusDBNehmenSchlaegtFehlTest() {
        when(scanner.personWantsToEnter()).thenReturn(true);
        when(scanner.scanId()).thenReturn(true);

        when(scanner.takePicture()).thenReturn(true);

        when(scanner.getId()).thenReturn(id);
        when(db.getPersonForId(id)).thenReturn(Optional.empty());

        zp.initiateProcess();

        verify(scanner).rejectAccess("Keine Informationen zur Person gespeichert");
    }

    @Test
    @DisplayName("Die eingescannte Person stimmt nicht mit den biometrischen Daten aus der Datenbank überein und die Person mit derselben ID befindet sich bereits im Gebäude")
    void biometricMatchnSchlaegtFehlTestMeins() {
        //Arrange
        when(scanner.personWantsToEnter()).thenReturn(true);
        when(scanner.scanId()).thenReturn(true);

        when(scanner.takePicture()).thenReturn(true);

        when(scanner.getId()).thenReturn(id);
        when(db.getPersonForId(id)).thenReturn(Optional.of(person));

        BiometricData bio = mock(BiometricData.class);
        when(scanner.getScannedBiometricData()).thenReturn(bio);
        when(person.biometricMatch(bio)).thenReturn(false);
        when(person.id()).thenReturn(id);

        ZugangspunktSyncEvent event = mock(ZugangspunktSyncEvent.class);
        zp.protocolEvent(event,id);

        //Act
        zp.initiateProcess();

        verify(scanner).sendAlert();
    }

    @Test
    @DisplayName("Alarm wird ausgelöst wenn biometrische Daten nicht übereinstimmen")
    void biometricMatchSchlaegtFehlTest() {
        when(scanner.personWantsToEnter()).thenReturn(true);

        when(scanner.scanId()).thenReturn(true);
        when(scanner.takePicture()).thenReturn(true);

        when(scanner.getId()).thenReturn(id);
        when(db.getPersonForId(id)).thenReturn(Optional.of(person));
        when(person.id()).thenReturn(id);

        BiometricData bio = mock(BiometricData.class);
        when(scanner.getScannedBiometricData()).thenReturn(bio);
        when(person.biometricMatch(bio)).thenReturn(false);


        zp.initiateProcess();


        verify(scanner).sendAlert();
        assertFalse(zp.personBefindetSichImGebaeude(id));
    }

    @Test
    @DisplayName("Alarm wenn eine Person sich mit der Identität einer Person ,die sich bereits im Gebäude bifindet, Eingang zu schaffen")
    void personBereitsImGebaeudeTest() {

        when(scanner.personWantsToEnter()).thenReturn(true);

        when(scanner.scanId()).thenReturn(true);
        when(scanner.takePicture()).thenReturn(true);

        when(scanner.getId()).thenReturn(id);
        when(db.getPersonForId(id)).thenReturn(Optional.of(person));

        when(person.id()).thenReturn(id);

        BiometricData bio = mock(BiometricData.class);
        when(scanner.getScannedBiometricData()).thenReturn(bio);
        when(person.biometricMatch(bio)).thenReturn(true);

        ZugangspunktSyncEvent event = mock(ZugangspunktSyncEvent.class);
        zp.protocolEvent(event,id);
//        zp.getPersonenImBereich().put(id, person);

        // when
        zp.initiateProcess();

        // then
        verify(scanner).sendAlert();
        assertTrue(zp.personBefindetSichImGebaeude(id));
    }

    @Test
    @DisplayName("Person, die erfolgreich identifiziert wurde hat keine Berechtigung ins Gebäude einzutreten")
    void unberechtigtePersonEintrittVerboten() {
        //Arrange
        when(scanner.personWantsToEnter()).thenReturn(true);

        when(scanner.scanId()).thenReturn(true);
        when(scanner.takePicture()).thenReturn(true);

        when(scanner.getId()).thenReturn(id);
        when(db.getPersonForId(id)).thenReturn(Optional.of(person));

        Id id2 = mock(Id.class);
        when(person.id()).thenReturn(id,id2);

        BiometricData bio = mock(BiometricData.class);
        when(scanner.getScannedBiometricData()).thenReturn(bio);
        when(person.biometricMatch(bio)).thenReturn(true);

        zp.initiateProcess();

        verify(scanner).sendAlert();
    }

}

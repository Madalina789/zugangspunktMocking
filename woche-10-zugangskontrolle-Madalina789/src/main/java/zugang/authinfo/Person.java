package zugang.authinfo;

public interface Person {

  Id id();

  boolean biometricMatch(BiometricData scannedBiometricData);
}

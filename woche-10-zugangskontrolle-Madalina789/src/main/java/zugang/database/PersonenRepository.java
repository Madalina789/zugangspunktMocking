package zugang.database;

import java.util.Optional;
import zugang.authinfo.Id;
import zugang.authinfo.Person;

public interface PersonenRepository {

  Optional<Person> getPersonForId(Id id);

}
